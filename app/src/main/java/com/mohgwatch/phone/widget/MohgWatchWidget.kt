package com.mohgwatch.phone.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.layout.Alignment
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.mohgwatch.phone.MainActivity
import com.mohgwatch.phone.service.GlucoseSyncState
import com.mohgwatch.core.util.GlucoseFormatter
import com.mohgwatch.core.model.GlucoseReading
import kotlinx.coroutines.flow.first

class MohgWatchWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val reading = GlucoseSyncState.latestReading.value
        val settingsStore = com.mohgwatch.phone.data.SettingsStore(context)
        val settings = settingsStore.settingsFlow.first()
        
        provideContent {
            GlanceTheme {
                WidgetContent(reading, settings)
            }
        }
    }

    @Composable
    private fun WidgetContent(reading: GlucoseReading?, settings: com.mohgwatch.core.model.UserSettings) {
        val surfaceColor = WidgetThemeHelper.surface(settings.widgetBgColor)
        val primaryColor = WidgetThemeHelper.primary(settings.widgetPrimaryTheme, reading, settings)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surfaceColor)
                .padding(10.dp)
                .clickable(actionStartActivity(android.content.Intent(androidx.glance.LocalContext.current, MainActivity::class.java))),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    style = TextStyle(
                        color = primaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            } else {
                Text(
                    text = "Brak danych",
                    style = TextStyle(
                        color = primaryColor,
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}

class MohgWatchWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MohgWatchWidget()
}
