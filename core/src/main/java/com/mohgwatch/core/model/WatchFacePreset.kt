package com.mohgwatch.core.model

/**
 * Preset tarczy zegarka — 15 wbudowanych w 3 kategoriach.
 */
enum class WatchFacePreset(
    val displayName: String,
    val category: PresetCategory,
    val description: String
) {
    // Cyfrowe
    D1_CLASSIC("MoghWatch Classic", PresetCategory.DIGITAL, "Ciemne tło, duży cyfrowy zegar, kolorowy pasek glukozy"),
    D2_NEON_PULSE("Neon Pulse", PresetCategory.DIGITAL, "Cyberpunk — neonowy cyan/magenta, AMOLED"),
    D3_MINIMAL_WHITE("Minimal White", PresetCategory.DIGITAL, "Skandynawski minimalizm, dużo przestrzeni"),
    D4_DASHBOARD("Dashboard", PresetCategory.DIGITAL, "Kokpit — wiele kafelków z danymi"),
    D5_NIGHT_MODE("Night Mode", PresetCategory.DIGITAL, "Ultra-ciemny, przyciemniona czerwień"),

    // Analogowe
    A1_SWISS_HERITAGE("Swiss Heritage", PresetCategory.ANALOG, "Klasyczny elegancki zegarek szwajcarski"),
    A2_DIVER_PRO("Diver Pro", PresetCategory.ANALOG, "Styl nurkowy z lunetą"),
    A3_CHRONOGRAPH("Chronograph", PresetCategory.ANALOG, "Chronograf wyścigowy z sub-dialami"),
    A4_FIELD_WATCH("Field Watch", PresetCategory.ANALOG, "Zegarek polowy, podwójna skala 12/24h"),
    A5_MOONPHASE("Moonphase", PresetCategory.ANALOG, "Elegancki dress watch z fazą księżyca"),

    // Hybrydowe
    H1_TECH_FUSION("Tech Fusion", PresetCategory.HYBRID, "Sci-fi HUD z analogowymi wskazówkami"),
    H2_VITALS_MONITOR("Vitals Monitor", PresetCategory.HYBRID, "Monitor medyczny z linią EKG"),
    H3_RETRO_DIGITAL("Retro Digital", PresetCategory.HYBRID, "Styl Casio z LCD panelem"),
    H4_SPORT_TRACKER("Sport Tracker", PresetCategory.HYBRID, "Sportowy z metrykami aktywności"),
    H5_AURORA("Aurora", PresetCategory.HYBRID, "Artystyczny gradient aurora borealis")
}

enum class PresetCategory(val displayName: String) {
    DIGITAL("Cyfrowe"),
    ANALOG("Analogowe"),
    HYBRID("Hybrydowe")
}
