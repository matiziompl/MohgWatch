package com.mohgwatch.phone.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.core.util.InjectionSiteHelper
import com.mohgwatch.phone.MainActivity
import com.mohgwatch.phone.data.SettingsStore
import kotlinx.coroutines.flow.first

class InjectionSitesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = SettingsStore(context).settingsFlow.first()
        val todayIndex = InjectionSiteHelper.getTodayCycleIndex()
        val reading = com.mohgwatch.phone.service.GlucoseSyncState.latestReading.value

        provideContent {
            WidgetContent(reading, todayIndex, settings)
        }
    }

    @Composable
    private fun WidgetContent(reading: com.mohgwatch.core.model.GlucoseReading?, todayIndex: Int, settings: UserSettings) {
        val surfaceColor = WidgetThemeHelper.surface(settings.widgetBgColor)
        val primaryColor = WidgetThemeHelper.primary(settings.widgetPrimaryTheme, reading, settings)
        val inactiveColor = WidgetThemeHelper.inactiveSlot(settings.widgetBgColor)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surfaceColor)
                .padding(4.dp)
                .clickable(actionStartActivity(android.content.Intent(androidx.glance.LocalContext.current, MainActivity::class.java))),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Inverted 2x3 grid:
            // Row 0: Slots 0 & 1 (Lower body - top in field of view)
            // Row 1: Slots 2 & 3 (Middle body)
            // Row 2: Slots 4 & 5 (Upper body - bottom in field of view)
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
        }
    }
}

class InjectionSitesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = InjectionSitesWidget()
}
