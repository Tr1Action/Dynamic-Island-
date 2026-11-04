package com.akira.island.island

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.Method

/**
 * Gerçek arka plan bulanıklığı — HyperOS 3'ün bildirim panelinde ve kontrol merkezinde kullandığı yöntem.
 *
 * Android 12'den beri SurfaceFlinger bir pencerenin ARKASINDA kalan her şeyi (başka uygulamalar dahil)
 * istenen bölgede bulanıklaştırabiliyor. SystemUI bunu gizli `BackgroundBlurDrawable` ile yapar:
 * çizildiği yerde, verilen köşe yarıçapıyla bir bulanıklık bölgesi açar ve bölge, adanın büyüyüp
 * küçülmesini her karede takip eder. Ada da aynı drawable'ı kullanır.
 *
 * Çalışması için telefonda pencereler arası bulanıklığın açık olması gerekir (HyperOS'ta
 * “Gelişmiş materyal/dokular”; pil tasarrufu bunu kapatabilir). Herhangi bir adım başarısız olursa
 * bulanıklık sessizce devre dışı kalır, ada eskisi gibi boyanmış camla görünür.
 */
internal object CrossBlur {
    private var prepared: Boolean? = null

    /** Gizli API'lere erişimi bir kez açar. Android 12'den eskide ya da hata olursa false. */
    fun prepare(): Boolean {
        prepared?.let { return it }
        val ok = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && runCatching {
            HiddenApiBypass.addHiddenApiExemptions(
                "Landroid/view/View;",
                "Landroid/view/ViewRootImpl;",
                "Lcom/android/internal/graphics/drawable/BackgroundBlurDrawable;",
            )
        }.getOrDefault(false)
        prepared = ok
        return ok
    }
}

/**
 * Arkasını bulanıklaştıran görünmez görünüm. Kendisi hiçbir piksel boyamaz (rengi saydam):
 * camın rengi ve parlamaları [glassSurface] tarafından üstüne çizilir.
 */
internal class BlurBackdropView(context: Context) : View(context) {
    private var blur: Drawable? = null
    private var setRadius: Method? = null
    private var setCorners: Method? = null
    private var setCorner: Method? = null

    private var radius = 0
    private var corners = FloatArray(4)

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attach()
    }

    override fun onDetachedFromWindow() {
        // Bölge, drawable görünümden ayrılınca kapanır
        background = null
        blur = null
        super.onDetachedFromWindow()
    }

    // Erişim HiddenApiBypass ile açılıyor (CrossBlur.prepare), bu yüzden lint'in yasağı burada geçerli değil
    @SuppressLint("BlockedPrivateApi", "SoonBlockedPrivateApi", "DiscouragedPrivateApi", "PrivateApi")
    private fun attach() {
        if (blur != null || !CrossBlur.prepare()) return
        runCatching {
            val root = View::class.java.getDeclaredMethod("getViewRootImpl").invoke(this) ?: return
            val create = Class.forName("android.view.ViewRootImpl").getDeclaredMethod("createBackgroundBlurDrawable")
            val d = create.invoke(root) as Drawable
            val c = d.javaClass
            setRadius = c.getMethod("setBlurRadius", Int::class.javaPrimitiveType)
            setCorners = runCatching {
                val f = Float::class.javaPrimitiveType
                c.getMethod("setCornerRadius", f, f, f, f)
            }.getOrNull()
            setCorner = runCatching { c.getMethod("setCornerRadius", Float::class.javaPrimitiveType) }.getOrNull()
            runCatching { c.getMethod("setColor", Int::class.javaPrimitiveType).invoke(d, 0) }
            blur = d
            background = d
            push()
        }.onFailure {
            blur = null
            background = null
        }
    }

    /** Bulanıklık yarıçapı (px) ve köşeler (px): sol üst, sağ üst, sol alt, sağ alt. 0 — bulanıklık yok. */
    fun update(radiusPx: Int, tl: Float, tr: Float, bl: Float, br: Float) {
        if (radiusPx == radius && corners[0] == tl && corners[1] == tr && corners[2] == bl && corners[3] == br) return
        radius = radiusPx
        corners[0] = tl
        corners[1] = tr
        corners[2] = bl
        corners[3] = br
        push()
    }

    private fun push() {
        val d = blur ?: return
        runCatching {
            setRadius?.invoke(d, radius)
            val sc = setCorners
            if (sc != null) {
                sc.invoke(d, corners[0], corners[1], corners[2], corners[3])
            } else {
                setCorner?.invoke(d, corners.max())
            }
        }.onFailure {
            // Bir şey ters giderse bulanıklığı tamamen bırak: ada boyanmış camla kalır
            background = null
            blur = null
        }
        invalidate()
    }
}

/** Telefonda pencereler arası bulanıklık şu an açık mı (pil tasarrufu ya da ayar değişince güncellenir). */
@Composable
fun rememberCrossWindowBlur(): Boolean {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val stop = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) watchCrossBlur(context) { enabled = it } else null
        onDispose { stop?.invoke() }
    }
    return enabled
}

/** Bulanıklığın açık olup olmadığını izler; dönen işlev izlemeyi bırakır. */
@RequiresApi(Build.VERSION_CODES.S)
private fun watchCrossBlur(context: Context, onChange: (Boolean) -> Unit): (() -> Unit)? {
    val wm = context.getSystemService(WindowManager::class.java) ?: return null
    onChange(runCatching { wm.isCrossWindowBlurEnabled }.getOrDefault(false))
    val listener = java.util.function.Consumer<Boolean> { onChange(it) }
    return runCatching {
        wm.addCrossWindowBlurEnabledListener(context.mainExecutor, listener)
        val stop: () -> Unit = { runCatching { wm.removeCrossWindowBlurEnabledListener(listener) } }
        stop
    }.getOrNull()
}

/**
 * Adanın arkasındaki gerçek bulanıklık. Yarıçap ve köşeler her karede okunur, böylece bulanıklık
 * bölgesi adanın açılma animasyonunu ve köşe değişimini birebir izler.
 */
@Composable
fun BlurBackdrop(
    modifier: Modifier,
    radiusPx: () -> Float,
    cornerPx: () -> Float,
    notch: Boolean,
) {
    AndroidView(
        factory = { BlurBackdropView(it) },
        modifier = modifier,
        update = { v ->
            val c = cornerPx().coerceAtLeast(0f)
            val top = if (notch) 0f else c
            v.update(radiusPx().toInt().coerceAtLeast(0), top, top, c, c)
        },
    )
}
