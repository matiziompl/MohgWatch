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
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.core.util.GlucoseFormatter
import com.mohgwatch.core.util.InjectionSiteHelper
import com.mohgwatch.phone.MainActivity
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.service.GlucoseSyncState
import kotlinx.coroutines.flow.first

class CompactBarWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val reading = GlucoseSyncState.latestReading.value
        val settings = SettingsStore(context).settingsFlow.first()
        val todayIndex = InjectionSiteHelper.getTodayCycleIndex()

        provideContent {
            WidgetContent(reading, todayIndex, settings)
        }
    }

    @Composable
    private fun WidgetContent(
        reading: GlucoseReading?,
        todayIndex: Int,
        settings: UserSettings
    ) {
        val surfaceColor = WidgetThemeHelper.surface(settings.widgetBgColor)
        val primaryColor = WidgetThemeHelper.primary(settings.widgetPrimaryTheme, reading, settings)

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surfaceColor)
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .clickable(actionStartActivity(android.content.Intent(androidx.glance.LocalContext.current, MainActivity::class.java))),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (reading != null) {
                val gluColor = WidgetThemeHelper.glucoseColor(reading, settings)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${reading.value.toInt()}",
                        style = TextStyle(
                            color = gluColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = reading.trendArrow.symbol,
                        style = TextStyle(
                            color = primaryColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(10.dp))

                Text(
                    text = InjectionSiteHelper.getShortSiteName(todayIndex),
                    style = TextStyle(
                        color = primaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                Spacer(modifier = GlanceModifier.width(6.dp))

                Text(
                    text = GlucoseFormatter.formatMinutesAgo(reading.getMinutesAgo()),
                    style = TextStyle(color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Normal)
                )
            } else {
                Text(
                    text = "Brak danych glukozy",
                    style = TextStyle(color = primaryColor, fontSize = 12.sp)
                )
            }
        }
    }
}

class CompactBarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactBarWidget()
}
