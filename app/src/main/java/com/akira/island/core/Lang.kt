package com.akira.island.core

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalConfiguration
import com.akira.island.data.AppLanguage
import com.akira.island.data.SettingsRepo
import java.util.Locale

/**
 * Язык приложения и острова: русский или английский. «Как в системе» — русский, если телефон на русском,
 * иначе английский. Строки пишутся парой прямо в коде: tr("Зарядка", "Charging").
 */
object Lang {
    /** Код языка: "ru", "en" или "tr". «Как в системе» — русский или турецкий, если телефон на нём, иначе английский. */
    val code: String
        get() = when (SettingsRepo.current.language) {
            AppLanguage.EN -> "en"
            AppLanguage.RU -> "ru"
            AppLanguage.TR -> "tr"
            AppLanguage.SYSTEM -> when (systemLanguage()) {
                "ru" -> "ru"
                "tr" -> "tr"
                else -> "en"
            }
        }

    /** Не русский: английский или турецкий (форматы и единицы — не русские) */
    val en: Boolean get() = code != "ru"

    /** Турецкий интерфейс */
    val turkish: Boolean get() = code == "tr"

    /** Для дат: «пятница, 24 октября», «Friday, October 24» или «4 Kasım Çarşamba» */
    val locale: Locale
        get() = when (code) {
            "ru" -> Locale.forLanguageTag("ru")
            "tr" -> Locale.forLanguageTag("tr")
            else -> Locale.ENGLISH
        }

    private fun systemLanguage(): String =
        runCatching { Resources.getSystem().configuration.locales[0].language }.getOrNull() ?: Locale.getDefault().language
}

/** Строка на текущем языке. */
fun tr(ru: String, en: String): String = when (Lang.code) {
    "ru" -> ru
    "tr" -> TurkishStrings.of(en)
    else -> en
}

/**
 * Сменили язык (в настройках или в системе) — всё внутри рисуется заново уже на новом языке.
 * Состояние снаружи (открытая вкладка и экран) остаётся.
 */
@Composable
fun LangScope(content: @Composable () -> Unit) {
    val s by SettingsRepo.state.collectAsState()
    // Язык системы меняется вместе с конфигурацией — чтение подписывает на неё
    LocalConfiguration.current
    key(s.language, Lang.code) { content() }
}
