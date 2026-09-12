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
import com.mohgwatch.core.model.GlucoseUnit

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
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(16.dp)
                .clickable(actionStartActivity(android.content.Intent(androidx.glance.LocalContext.current, MainActivity::class.java))),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (reading != null) {
                Text(
                    text = "Glukoza",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${reading.value.toInt()}",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = reading.trendArrow.symbol,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = GlucoseFormatter.formatMinutesAgo(reading.getMinutesAgo()),
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                )
            } else {
                Text(
                    text = "Brak danych",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                )
            }
        }
    }
}
