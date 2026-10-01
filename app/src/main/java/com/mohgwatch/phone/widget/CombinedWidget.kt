package com.mohgwatch.phone.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.core.util.GlucoseFormatter
import com.mohgwatch.core.util.InjectionSiteHelper
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.service.GlucoseSyncState
import kotlinx.coroutines.flow.first

class CombinedWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition

    companion object {
        val SHOW_SITES_KEY = booleanPreferencesKey("combined_show_sites")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val reading = GlucoseSyncState.latestReading.value
        val settings = SettingsStore(context).settingsFlow.first()
        val todayIndex = InjectionSiteHelper.getTodayCycleIndex()

        provideContent {
            val prefs = currentState<Preferences>()
            val showSites = prefs[SHOW_SITES_KEY] ?: false
            WidgetContent(reading, todayIndex, settings, showSites)
        }
    }

    @Composable
    private fun WidgetContent(
        reading: GlucoseReading?,
        todayIndex: Int,
        settings: UserSettings,
        showSites: Boolean
    ) {
        val surfaceColor = WidgetThemeHelper.surface(settings.widgetBgColor)
        val primaryColor = WidgetThemeHelper.primary(settings.widgetPrimaryTheme, reading, settings)
        val inactiveColor = WidgetThemeHelper.inactiveSlot(settings.widgetBgColor)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surfaceColor)
                .padding(if (showSites) 4.dp else 10.dp)
                .clickable(actionRunCallback<ToggleCombinedModeAction>()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showSites) {
                // Widok 1: Odwrócona siatka miejsc wkłuć 2x3 (30dp, brak ucinania)
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (row in InjectionSiteHelper.GRID_SLOTS) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val slot0 = row[0]
                            val slot1 = row[1]
                            Spacer(
                                modifier = GlanceModifier
                                    .size(30.dp)
                                    .cornerRadius(7.dp)
                                    .background(if (slot0 == todayIndex) primaryColor else inactiveColor)
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Spacer(
                                modifier = GlanceModifier
                                    .size(30.dp)
                                    .cornerRadius(7.dp)
                                    .background(if (slot1 == todayIndex) primaryColor else inactiveColor)
                            )
                        }
                    }
                }
            } else {
                // Widok 2: Czysty odczyt glukozy
                if (reading != null) {
                    val gluColor = WidgetThemeHelper.glucoseColor(reading, settings)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = GlanceModifier.defaultWeight()
                    ) {
                        Text(
                            text = "${reading.value.toInt()}",
                            style = TextStyle(
                                color = gluColor,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = reading.trendArrow.symbol,
                            style = TextStyle(
                                color = primaryColor,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Text(
                        text = GlucoseFormatter.formatMinutesAgo(reading.getMinutesAgo()),
                        style = TextStyle(color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    )
                } else {
                    Text(
                        text = "Brak danych",
                        style = TextStyle(color = primaryColor, fontSize = 14.sp)
                    )
                }
            }
        }
    }
}

class ToggleCombinedModeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            val current = prefs[CombinedWidget.SHOW_SITES_KEY] ?: false
            prefs.toMutablePreferences().apply {
                this[CombinedWidget.SHOW_SITES_KEY] = !current
            }
        }
        CombinedWidget().update(context, glanceId)
    }
}

class CombinedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CombinedWidget()
}
