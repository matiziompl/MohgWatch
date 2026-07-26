package com.mohgwatch.phone.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

val LocalAppLanguage = compositionLocalOf { "system" }

@Composable
fun tr(pl: String, en: String): String {
    val lang = LocalAppLanguage.current
    if (lang == "en") return en
    if (lang == "pl") return pl
    return if (java.util.Locale.getDefault().language.startsWith("en")) en else pl
}

fun trStr(lang: String, pl: String, en: String): String {
    if (lang == "en") return en
    if (lang == "pl") return pl
    return if (java.util.Locale.getDefault().language.startsWith("en")) en else pl
}
