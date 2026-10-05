package com.cy.languagereader.mobile.data

import android.content.Context

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("language_reader_settings", Context.MODE_PRIVATE)

    init {
        // V2.8 removes the low-quality bundled pronunciation pack and returns
        // to Google pronunciation. Re-enable network pronunciation once so
        // upgrades from V2.5/V2.6 do not remain silently muted by an old false preference.
        if (!prefs.getBoolean("google_pronunciation_v28_migrated", false)) {
            prefs.edit()
                .putBoolean("network_pronunciation_enabled", true)
                .putBoolean("google_pronunciation_v28_migrated", true)
                .apply()
        }
        // V4.2 removes the optional Ollama integration completely. Clear stale
        // endpoint/model values left by older builds so backups and diagnostics
        // no longer carry unused local-AI configuration.
        if (prefs.contains("ollama_url") || prefs.contains("ollama_model")) {
            prefs.edit().remove("ollama_url").remove("ollama_model").apply()
        }

        // V4.5 stops assuming that Google pronunciation is the best first route.
        // Existing installs that still carry the old default are moved once to AUTO;
        // users can still explicitly choose Mainland / Google / Offline afterwards.
        if (!prefs.getBoolean("pronunciation_auto_v45_migrated", false)) {
            val oldRoute = prefs.getString("pronunciation_route", null)
            prefs.edit().apply {
                if (oldRoute == null || oldRoute.equals("GOOGLE", ignoreCase = true)) {
                    putString("pronunciation_route", "AUTO")
                }
                putBoolean("pronunciation_auto_v45_migrated", true)
            }.apply()
        }

        // The original Readium defaults were intentionally roomy while the renderer
        // was being stabilized. V4.5 tightens only untouched defaults to a more
        // book-like reading density; customized values are preserved.
        if (!prefs.getBoolean("reader_typography_v45_migrated", false)) {
            val edit = prefs.edit()
            if (!prefs.contains("reader_margin_dp") || prefs.getInt("reader_margin_dp", 20) == 20) {
                edit.putInt("reader_margin_dp", 16)
            }
            if (!prefs.contains("reader_line_spacing_percent") || prefs.getInt("reader_line_spacing_percent", 150) == 150) {
                edit.putInt("reader_line_spacing_percent", 142)
            }
            edit.putBoolean("reader_typography_v45_migrated", true).apply()
        }

        // V5.0 widens the reading magnifier so the enlarged line is not clipped
        // at the left/right edges on high-density emulators. Preserve deliberate
        // wider custom values, but migrate the old 90% default once.
        if (!prefs.getBoolean("reader_magnifier_v50_migrated", false)) {
            val oldWidth = prefs.getInt("reader_magnifier_width_percent", 90)
            val edit = prefs.edit()
            if (!prefs.contains("reader_magnifier_width_percent") || oldWidth <= 90) {
                edit.putInt("reader_magnifier_width_percent", 98)
            }
            edit.putBoolean("reader_magnifier_v50_migrated", true).apply()
        }
    }

    var readerFontFamily: String
        get() = prefs.getString("reader_font_family", "DEFAULT") ?: "DEFAULT"
        set(value) {
            val normalized = value.uppercase()
            val allowed = setOf(
                "DEFAULT", "SERIF", "SANS_SERIF", "CURSIVE", "MONOSPACE",
                "OPEN_DYSLEXIC", "ACCESSIBLE_DFA", "IA_WRITER_DUOSPACE"
            )
            prefs.edit().putString("reader_font_family", if (normalized in allowed) normalized else "DEFAULT").apply()
        }

    var readerFontSize: Int
        get() = prefs.getInt("reader_font_size", 20).coerceIn(14, 64)
        set(value) { prefs.edit().putInt("reader_font_size", value.coerceIn(14, 64)).apply() }

    var readerLineSpacingPercent: Int
        get() = prefs.getInt("reader_line_spacing_percent", 142).coerceIn(90, 260)
        set(value) { prefs.edit().putInt("reader_line_spacing_percent", value.coerceIn(90, 260)).apply() }

    var readerMarginDp: Int
        get() = prefs.getInt("reader_margin_dp", 16)
        set(value) { prefs.edit().putInt("reader_margin_dp", value.coerceIn(8, 40)).apply() }

    var readerTheme: String
        get() = prefs.getString("reader_theme", "PAPER") ?: "PAPER"
        set(value) { prefs.edit().putString("reader_theme", value).apply() }

    var readerMode: String
        get() = prefs.getString("reader_mode", "PAGE") ?: "PAGE"
        set(value) { prefs.edit().putString("reader_mode", value).apply() }

    var readerPageChars: Int
        get() = prefs.getInt("reader_page_chars", 650)
        set(value) { prefs.edit().putInt("reader_page_chars", value.coerceIn(350, 1800)).apply() }

    var readerAutoScrollSpeed: Int
        get() = prefs.getInt("reader_auto_scroll_speed", 4)
        set(value) { prefs.edit().putInt("reader_auto_scroll_speed", value.coerceIn(1, 10)).apply() }

    // Legacy V3.4 preference kept only for upgrade compatibility.
    // V3.5 no longer overrides window brightness; reading follows Android system brightness.
    var readerBrightnessPercent: Int
        get() = prefs.getInt("reader_brightness_percent", 65)
        set(value) { prefs.edit().putInt("reader_brightness_percent", value.coerceIn(10, 100)).apply() }

    var readerKeepScreenOn: Boolean
        get() = prefs.getBoolean("reader_keep_screen_on", true)
        set(value) { prefs.edit().putBoolean("reader_keep_screen_on", value).apply() }

    var readerTwoPageLandscape: Boolean
        get() = prefs.getBoolean("reader_two_page_landscape", true)
        set(value) { prefs.edit().putBoolean("reader_two_page_landscape", value).apply() }

    var libraryGridMode: Boolean
        get() = prefs.getBoolean("library_grid_mode", false)
        set(value) { prefs.edit().putBoolean("library_grid_mode", value).apply() }
    var readerVolumeKeyTurnsPage: Boolean
        get() = prefs.getBoolean("reader_volume_key_turns_page", true)
        set(value) = prefs.edit().putBoolean("reader_volume_key_turns_page", value).apply()

    var readerTapZonesEnabled: Boolean
        get() = prefs.getBoolean("reader_tap_zones_enabled", false)
        set(value) = prefs.edit().putBoolean("reader_tap_zones_enabled", value).apply()

    var readerReadingRuler: Boolean
        get() = prefs.getBoolean("reader_reading_ruler", false)
        set(value) = prefs.edit().putBoolean("reader_reading_ruler", value).apply()

    var readerBlueLightPercent: Int
        get() = prefs.getInt("reader_blue_light_percent", 0)
        set(value) = prefs.edit().putInt("reader_blue_light_percent", value.coerceIn(0, 55)).apply()

    var readerPageAnimation: Boolean
        get() = prefs.getBoolean("reader_page_animation", true)
        set(value) = prefs.edit().putBoolean("reader_page_animation", value).apply()

    var readerSessionSeconds: Long
        get() = prefs.getLong("reader_session_seconds", 0L)
        set(value) = prefs.edit().putLong("reader_session_seconds", value.coerceAtLeast(0L)).apply()

    var readerTapLeftAction: String
        get() = prefs.getString("reader_tap_left_action", "PREVIOUS") ?: "PREVIOUS"
        set(value) = prefs.edit().putString("reader_tap_left_action", value).apply()

    var readerTapCenterAction: String
        get() = prefs.getString("reader_tap_center_action", "TOGGLE_CHROME") ?: "TOGGLE_CHROME"
        set(value) = prefs.edit().putString("reader_tap_center_action", value).apply()

    var readerTapRightAction: String
        get() = prefs.getString("reader_tap_right_action", "NEXT") ?: "NEXT"
        set(value) = prefs.edit().putString("reader_tap_right_action", value).apply()

    var readerRulerStyle: String
        get() = prefs.getString("reader_ruler_style", "BAND") ?: "BAND"
        set(value) = prefs.edit().putString("reader_ruler_style", value).apply()

    var readerRulerPositionPercent: Int
        get() = prefs.getInt("reader_ruler_position_percent", 50).coerceIn(10, 90)
        set(value) = prefs.edit().putInt("reader_ruler_position_percent", value.coerceIn(10, 90)).apply()

    // V4.9: the old reading-ruler size key is intentionally reused as the
    // magnifier lens height so existing installations keep a sensible preference.
    var readerRulerSizeDp: Int
        get() = prefs.getInt("reader_ruler_size_dp", 72).coerceIn(56, 160)
        set(value) = prefs.edit().putInt("reader_ruler_size_dp", value.coerceIn(56, 160)).apply()

    var readerMagnifierWidthPercent: Int
        get() = prefs.getInt("reader_magnifier_width_percent", 98).coerceIn(65, 100)
        set(value) = prefs.edit().putInt("reader_magnifier_width_percent", value.coerceIn(65, 100)).apply()

    var readerMagnifierZoomPercent: Int
        get() = prefs.getInt("reader_magnifier_zoom_percent", 145).coerceIn(110, 220)
        set(value) = prefs.edit().putInt("reader_magnifier_zoom_percent", value.coerceIn(110, 220)).apply()

    var readerLineGuideEnabled: Boolean
        get() = prefs.getBoolean("reader_line_guide_enabled", false)
        set(value) = prefs.edit().putBoolean("reader_line_guide_enabled", value).apply()

    var readerLineGuidePositionPercent: Int
        get() = prefs.getInt("reader_line_guide_position_percent", 52).coerceIn(10, 90)
        set(value) = prefs.edit().putInt("reader_line_guide_position_percent", value.coerceIn(10, 90)).apply()

    var readerLineGuideHeightDp: Int
        get() = prefs.getInt("reader_line_guide_height_dp", 44).coerceIn(24, 120)
        set(value) = prefs.edit().putInt("reader_line_guide_height_dp", value.coerceIn(24, 120)).apply()

    var readerLineGuideTopColor: String
        get() = normalizeGuideColor(prefs.getString("reader_line_guide_top_color", "BLUE").orEmpty(), "BLUE")
        set(value) = prefs.edit().putString("reader_line_guide_top_color", normalizeGuideColor(value, "BLUE")).apply()

    var readerLineGuideBottomColor: String
        get() = normalizeGuideColor(prefs.getString("reader_line_guide_bottom_color", "ORANGE").orEmpty(), "ORANGE")
        set(value) = prefs.edit().putString("reader_line_guide_bottom_color", normalizeGuideColor(value, "ORANGE")).apply()

    var vocabReviewAutoPronounce: Boolean
        get() = prefs.getBoolean("vocab_review_auto_pronounce", false)
        set(value) = prefs.edit().putBoolean("vocab_review_auto_pronounce", value).apply()

    var dailyReadingGoalMinutes: Int
        get() = prefs.getInt("daily_reading_goal_minutes", 30)
        set(value) = prefs.edit().putInt("daily_reading_goal_minutes", value.coerceIn(5, 240)).apply()

    var dailyVocabReviewGoal: Int
        get() = prefs.getInt("daily_vocab_review_goal", 20)
        set(value) = prefs.edit().putInt("daily_vocab_review_goal", value.coerceIn(5, 200)).apply()

    var safeMode: Boolean
        get() = prefs.getBoolean("safe_mode", false)
        set(value) = prefs.edit().putBoolean("safe_mode", value).apply()

    var networkPronunciationEnabled: Boolean
        get() = prefs.getBoolean("network_pronunciation_enabled", true)
        set(value) = prefs.edit().putBoolean("network_pronunciation_enabled", value).apply()

    var pronunciationCacheEnabled: Boolean
        get() = prefs.getBoolean("pronunciation_cache_enabled", true)
        set(value) = prefs.edit().putBoolean("pronunciation_cache_enabled", value).apply()

    var pronunciationRoute: String
        get() = prefs.getString("pronunciation_route", "AUTO") ?: "AUTO"
        set(value) = prefs.edit().putString(
            "pronunciation_route",
            value.uppercase().takeIf { it in setOf("AUTO", "MAINLAND", "GOOGLE", "OFFLINE") } ?: "AUTO",
        ).apply()

    var pronunciationFallbackEnabled: Boolean
        get() = prefs.getBoolean("pronunciation_failure_fallback_enabled", true)
        set(value) = prefs.edit().putBoolean("pronunciation_failure_fallback_enabled", value).apply()

    private fun normalizeGuideColor(value: String, fallback: String): String {
        val normalized = value.trim().uppercase()
        return normalized.takeIf { it in setOf("BLUE", "ORANGE", "GREEN", "RED", "PURPLE", "GRAY") } ?: fallback
    }

}
