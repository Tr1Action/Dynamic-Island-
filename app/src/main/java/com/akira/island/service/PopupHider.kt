package com.akira.island.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.IBinder
import android.service.notification.StatusBarNotification
import android.telecom.TelecomManager
import android.util.Log
import com.akira.island.data.SettingsRepo
import java.util.concurrent.Executors

/**
 * Çift bildirim olmasın (root): uygulamanın kendi bildirimi kapatılmaz — sesi, titreşimi çalar ve bildirim
 * panelinde kalır. Sadece ekranın üstüne düşen açılır kutusu kapanır, yerine ada görünür. Bunun için o
 * bildirim kanalının önemi «Yüksek»ten «Varsayılan»a iner (Ayarlar'da «Yüzen bildirimler»i kapatmakla aynı).
 * Kanal başına bir kez yapılır: o kanalın yalnızca ilk bildirimi iki kez görünür. Aramalar ve alarmlar hariç.
 */
object PopupHider {
    private const val TAG = "PopupHider"
    private const val PREFS = "popup_hider"
    private const val KEY_CHANGED = "changed" // "pkg|uid|channelId"

    private val worker = Executors.newSingleThreadExecutor()
    private val tried = HashSet<String>()

    /** Ada bu bildirimi gösterdi ve bildirimin kendi açılır kutusu da var. */
    fun onShown(context: Context, sbn: StatusBarNotification) {
        if (!SettingsRepo.current.hidePopups) return
        val n = sbn.notification ?: return
        val channel = n.channelId ?: return
        if (n.category == Notification.CATEGORY_CALL || n.category == Notification.CATEGORY_ALARM || n.fullScreenIntent != null) return
        val pkg = sbn.packageName
        val dialer = runCatching { context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage }.getOrNull()
        if (pkg == dialer || pkg == context.packageName || pkg == SystemBanners.SYSTEM_UI || pkg == SystemBanners.ANDROID) return
        val entry = "$pkg|${sbn.uid}|$channel"
        synchronized(tried) { if (!tried.add(entry)) return }
        val app = context.applicationContext
        worker.execute {
            if (run(app, "lower", pkg, sbn.uid.toString(), channel)) {
                val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                p.edit().putStringSet(KEY_CHANGED, HashSet(p.getStringSet(KEY_CHANGED, emptySet())!!) + entry).apply()
            }
        }
    }

    /** Ayar kapatıldı: kapatılan açılır kutuları geri aç. */
    fun restoreAll(context: Context) {
        val app = context.applicationContext
        worker.execute {
            val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            p.getStringSet(KEY_CHANGED, emptySet())!!.forEach { entry ->
                val parts = entry.split("|", limit = 3)
                if (parts.size == 3) run(app, "raise", parts[0], parts[1], parts[2])
            }
            p.edit().remove(KEY_CHANGED).apply()
            synchronized(tried) { tried.clear() }
        }
    }

    /** Root ile sistem yetkisinde [ChannelTool] çalıştırır. */
    private fun run(context: Context, vararg args: String): Boolean = try {
        val apk = context.applicationInfo.sourceDir
        val quoted = args.joinToString(" ") { "'" + it.replace("'", "'\\''") + "'" }
        val cmd = "CLASSPATH='$apk' app_process /system/bin ${ChannelTool::class.java.name} $quoted"
        val proc = ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start()
        val out = proc.inputStream.bufferedReader().readText()
        val ok = proc.waitFor() == 0 && out.contains("OK")
        if (!ok) Log.w(TAG, "başarısız: $out")
        ok
    } catch (e: Exception) {
        Log.w(TAG, "su yok", e)
        false
    }
}

/**
 * Root altında `app_process` ile çalışır (uid 0 = sistem yetkisi): başka bir uygulamanın bildirim kanalının
 * önemini değiştirir. `lower` → Varsayılan (açılır kutu yok, ses var), `raise` → Yüksek.
 */
object ChannelTool {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val action = args[0]
            val pkg = args[1]
            val uid = args[2].toInt()
            val channelId = args[3]
            val binder = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java).invoke(null, "notification") as IBinder
            val nm = Class.forName("android.app.INotificationManager\$Stub")
                .getMethod("asInterface", IBinder::class.java).invoke(null, binder)!!
            val slice = nm.javaClass.getMethod(
                "getNotificationChannelsForPackage",
                String::class.java, Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType,
            ).invoke(nm, pkg, uid, false)!!
            @Suppress("UNCHECKED_CAST")
            val channels = slice.javaClass.getMethod("getList").invoke(slice) as List<NotificationChannel>
            val ch = channels.firstOrNull { it.id == channelId }
            if (ch == null) {
                println("NO_CHANNEL")
                System.exit(2)
                return
            }
            if (action == "lower" && ch.importance < NotificationManager.IMPORTANCE_HIGH) {
                println("OK zaten")
                System.exit(0)
                return
            }
            ch.importance = if (action == "raise") NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            // Ayarlar uygulaması gibi «kullanıcı değiştirdi» diye kilitle
            runCatching {
                val lock = NotificationChannel::class.java.getField("USER_LOCKED_IMPORTANCE").getInt(null)
                NotificationChannel::class.java.getMethod("lockFields", Int::class.javaPrimitiveType).invoke(ch, lock)
            }
            nm.javaClass.getMethod(
                "updateNotificationChannelForPackage",
                String::class.java, Int::class.javaPrimitiveType, NotificationChannel::class.java,
            ).invoke(nm, pkg, uid, ch)
            println("OK")
            System.exit(0)
        } catch (t: Throwable) {
            println("ERR ${t.cause ?: t}")
            System.exit(1)
        }
    }
}
