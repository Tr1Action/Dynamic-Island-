package com.akira.island.core

import android.content.Context
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Türkçe metinler. Kodda metinler çift olarak yazılır: tr("Зарядка", "Charging"). Türkçe karşılığı
 * İngilizce metne göre assets/tr_strings.json'dan bulunur, böylece yüzlerce çağrıya dokunmak gerekmez.
 *
 * İçinde değişken olan metinler («Version 1.7 is available») kalıp olarak saklanır: İngilizce kalıpta
 * her değişkenin yerinde \u0000 durur, Türkçede {0}, {1}… Bulunamayan metin İngilizce kalır.
 */
object TurkishStrings {
    @Volatile private var exact: Map<String, String> = emptyMap()
    @Volatile private var templates: List<Pair<Regex, String>> = emptyList()
    private val cache = ConcurrentHashMap<String, String>()

    fun init(context: Context) {
        runCatching {
            val text = context.assets.open("tr_strings.json").bufferedReader().use { it.readText() }
            val root = JSONObject(text)

            val ex = root.getJSONObject("exact")
            val map = HashMap<String, String>(ex.length() * 2)
            val keys = ex.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = ex.getString(k)
            }

            val arr = root.getJSONArray("templates")
            val list = ArrayList<Pair<Regex, String>>(arr.length())
            for (i in 0 until arr.length()) {
                val pair = arr.getJSONArray(i)
                val pattern = pair.getString(0).split('\u0000').joinToString("(.*?)") { Regex.escape(it) }
                list += Regex("^" + pattern + "\$", RegexOption.DOT_MATCHES_ALL) to pair.getString(1)
            }

            exact = map
            templates = list
            cache.clear()
        }
    }

    fun of(en: String): String {
        exact[en]?.let { return it }
        cache[en]?.let { return it }
        var out = en
        for ((re, tpl) in templates) {
            val match = re.matchEntire(en) ?: continue
            var s = tpl
            match.groupValues.drop(1).forEachIndexed { i, v -> s = s.replace("{$i}", v) }
            out = s
            break
        }
        // Geri sayım gibi her saniye değişen metinler önbelleği şişirmesin
        if (cache.size > 300) cache.clear()
        cache[en] = out
        return out
    }
}
