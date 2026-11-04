package com.akira.island.service

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.akira.island.R
import com.akira.island.core.AlarmInfo
import com.akira.island.core.IslandBus
import com.akira.island.core.Notice
import com.akira.island.core.NoticeStyle
import com.akira.island.core.Tints
import com.akira.island.core.tr
import com.akira.island.data.SettingsRepo
import com.akira.island.island.Ios
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Ближайший будильник. Его знает сама система (AlarmManager.getNextAlarmClock) — уведомления «Часов»
 * для этого не нужны. Поставили новый будильник — остров показывает плашку со временем подъёма;
 * следующий по расписанию (прошлый прозвенел или отложен) — молча.
 * Будильником системы пользуются и календари, напоминалки, трекеры сна — их «будильник» человек не ставил,
 * поэтому берём только поставленные приложением-будильником («Часы», Alarmy и т. п.).
 */
class AlarmMonitor(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) = refresh(announce = true)
    }

    fun start() {
        AlarmFix.init(context)
        AlarmFix.onChange = { refresh(announce = false) }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED), ContextCompat.RECEIVER_EXPORTED,
        )
        refresh(announce = false)
    }

    /**
     * Saat uygulamasının sisteme yazdığı alarm saati («Çar 07:40»), varsa: sistem alarmına en yakın o saat.
     * Yalnızca dakikası aynı ve tam saatlerle kaymışsa işe yarar — başka bir alarmla karışmaz.
     */
    private fun formattedAlarm(raw: Long): Long? {
        @Suppress("DEPRECATION")
        val text = runCatching { Settings.System.getString(context.contentResolver, Settings.System.NEXT_ALARM_FORMATTED) }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val m = Regex("""(\d{1,2})[:.](\d{2})""").find(text) ?: return null
        var h = m.groupValues[1].toInt()
        val min = m.groupValues[2].toInt()
        val t = text.lowercase()
        if (h in 1..11 && ("pm" in t || "ös" in t)) h += 12
        if (h == 12 && ("am" in t || "öö" in t)) h = 0
        if (h > 23 || min > 59) return null
        return (-1..1).map { d ->
            Calendar.getInstance().apply {
                timeInMillis = raw
                add(Calendar.DAY_OF_YEAR, d)
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.minByOrNull { abs(it - raw) }
    }

    fun stop() {
        AlarmFix.onChange = null
        runCatching { context.unregisterReceiver(receiver) }
        IslandBus.nextAlarm.value = null
    }

    private fun refresh(announce: Boolean) {
        val info = runCatching { alarms?.nextAlarmClock }.getOrNull()
        val alarm = info?.takeIf { a -> a.showIntent?.creatorPackage?.let { ClockApps.isAlarmApp(context, it) } == true }
        if (alarm != null) {
            AlarmFix.raw = alarm.triggerTime
            // Yalnızca kaymayı bildirirse: ada şu anda zaten yenileniyor, tekrar çağırmaya gerek yok
            formattedAlarm(alarm.triggerTime)?.let { AlarmFix.learn(alarm.triggerTime, it, 60_000, notify = false, allowZero = false) }
        }
        val next = alarm?.let { AlarmInfo(AlarmFix.apply(it.triggerTime), it.showIntent?.creatorPackage, it.showIntent) }
        val prev = IslandBus.nextAlarm.value
        IslandBus.nextAlarm.value = next
        if (!announce || next == null || prev?.triggerAt == next.triggerAt) return
        val now = System.currentTimeMillis()
        if (next.triggerAt <= now) return
        // Прошлый будильник уже прозвенел или его отложили — это не новый, а очередной по расписанию
        if (prev != null && prev.triggerAt <= now + 60_000) return
        val s = SettingsRepo.current
        if (!s.enabled || !s.evAlarmSet) return
        IslandBus.post(
            Notice(
                title = tr("Будильник", "Alarm"),
                icon = Ios.load(context, R.drawable.ios_alarm),
                tint = Tints.Orange,
                style = NoticeStyle.ALARM,
                alarm = next,
                extraMs = 2_000,
            ),
        )
    }
}

/**
 * Xiaomi Saat, sistemin «sıradaki alarm» bilgisini gerçek alarmdan tam 1–3 saat kayık yazabiliyor
 * (07:40'lık alarm 06:40 görünüyordu). Gerçek zaman Saat'in kendi yazısından öğrenilir
 * («Alarm 4 saat 49 dakika içinde» ya da sistemdeki biçimli alarm saati). Kayma her seferinde aynı
 * olduğundan saklanır ve sonraki alarmlara da uygulanır; Saat'te kayma görünmezse sıfırlanır.
 */
object AlarmFix {
    /** Sistemin bildirdiği son alarm zamanı */
    @Volatile var raw: Long = 0L
    @Volatile private var offset: Long = 0L
    @Volatile var onChange: (() -> Unit)? = null
    private var prefs: android.content.SharedPreferences? = null

    private const val HOUR = 3_600_000L
    private const val KEY = "alarm_fix_offset"

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences("alarm_fix", Context.MODE_PRIVATE)
        offset = prefs?.getLong(KEY, 0L) ?: 0L
    }

    fun apply(raw: Long): Long = raw + offset

    /** Gerçek alarm yaklaşık [real] zamanında, [toleranceMs] payla. Düzeltme değiştiyse ada yenilenir. */
    fun learn(raw: Long, real: Long, toleranceMs: Long, notify: Boolean = true, allowZero: Boolean = true) {
        if (raw <= 0L) return
        val diff = real - raw
        val hours = (diff / HOUR.toDouble()).roundToLong()
        val newOffset = when {
            abs(diff) <= toleranceMs -> if (allowZero) 0L else return
            hours != 0L && abs(hours) <= 3 && abs(diff - hours * HOUR) <= toleranceMs -> hours * HOUR
            else -> return
        }
        if (offset == newOffset) return
        offset = newOffset
        prefs?.edit()?.putLong(KEY, newOffset)?.apply()
        if (notify) onChange?.invoke()
    }

    /** Saat ekranında görünen «… içinde» geri sayımı: alarma kalan süre, ms */
    fun learnFromCountdown(leftMs: Long) {
        val r = raw.takeIf { it > 0L } ?: return
        // Saat dakikayı aşağı yuvarlar: gerçek zaman [şimdi + kalan, şimdi + kalan + 1 dk) aralığında
        learn(r, System.currentTimeMillis() + leftMs + 30_000, 90_000)
    }
}

/** «Alarm 4 saat 49 dakika içinde», «Alarm in 4 hours 49 minutes», «Будильник через 4 ч 49 мин» */
object AlarmCountdownParser {
    private val alarmWord = Regex("""alarm|будильник|réveil""", RegexOption.IGNORE_CASE)
    private val inWord = Regex("""içinde|sonra|\bin\b|через|dans""", RegexOption.IGNORE_CASE)
    private val days = Regex("""(\d{1,2})\s*(gün|days?|дн|д\.|jours?)""", RegexOption.IGNORE_CASE)
    private val hours = Regex("""(\d{1,2})\s*(saat|sa\b|hours?|hrs?|h\b|час|ч\b|heures?)""", RegexOption.IGNORE_CASE)
    private val minutes = Regex("""(\d{1,2})\s*(dakika|dk|minutes?|mins?|мин|min)""", RegexOption.IGNORE_CASE)

    fun parse(text: String): Long? {
        if (text.length > 80 || !alarmWord.containsMatchIn(text) || !inWord.containsMatchIn(text)) return null
        val d = days.find(text)?.groupValues?.get(1)?.toLong() ?: 0L
        val h = hours.find(text)?.groupValues?.get(1)?.toLong() ?: 0L
        val m = minutes.find(text)?.groupValues?.get(1)?.toLong()
        if (m == null && h == 0L && d == 0L) return null
        return ((d * 24 + h) * 60 + (m ?: 0L)) * 60_000L
    }
}
