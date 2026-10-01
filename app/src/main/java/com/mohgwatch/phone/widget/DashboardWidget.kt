package com.mohgwatch.phone.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.core.util.GlucoseFormatter
import com.mohgwatch.core.util.InjectionSiteHelper
import com.mohgwatch.phone.MainActivity
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.service.GlucoseSyncState
import kotlinx.coroutines.flow.first

class DashboardWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

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
        val size = LocalSize.current
        val surfaceColor = WidgetThemeHelper.surface(settings.widgetBgColor)
        val primaryColor = WidgetThemeHelper.primary(settings.widgetPrimaryTheme, reading, settings)
        val inactiveColor = WidgetThemeHelper.inactiveSlot(settings.widgetBgColor)
        val isHorizontal = size.width >= 200.dp && size.height < 170.dp

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surfaceColor)
                .padding(8.dp)
                .clickable(actionStartActivity(android.content.Intent(androidx.glance.LocalContext.current, MainActivity::class.java))),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isHorizontal) {
                // Układ poziomy (4x2): Glukoza po lewej, miejsca wkłuć po prawej
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lewa strona: Glukoza
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.defaultWeight()
                    ) {
                        if (reading != null) {
                            val gluColor = WidgetThemeHelper.glucoseColor(reading, settings)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${reading.value.toInt()}",
                                    style = TextStyle(color = gluColor, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = GlanceModifier.width(8.dp))
                                Text(
                                    text = reading.trendArrow.symbol,
                                    style = TextStyle(color = primaryColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = GlucoseFormatter.formatMinutesAgo(reading.getMinutesAgo()),
                                style = TextStyle(color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            )
                        } else {
                            Text(text = "Brak danych", style = TextStyle(color = primaryColor, fontSize = 12.sp))
                        }
                    }

                    // Prawa strona: Siatka wkłuć
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.defaultWeight()
                    ) {
                        SitesGrid(todayIndex, primaryColor, inactiveColor, boxSize = 22.dp)
                    }
                }
            } else {
                // Układ pionowy/kwadratowy (2x2, 3x3, 4x4)
                val fontSize = if (size.width >= 240.dp) 44.sp else 32.sp
                val arrowSize = if (size.width >= 240.dp) 38.sp else 26.sp
                val boxSize = if (size.width >= 240.dp) 24.dp else 16.dp

                // Góra: Glukoza
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                ) {
                    if (reading != null) {
                        val gluColor = WidgetThemeHelper.glucoseColor(reading, settings)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${reading.value.toInt()}",
                                style = TextStyle(color = gluColor, fontSize = fontSize, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(
                                text = reading.trendArrow.symbol,
                                style = TextStyle(color = primaryColor, fontSize = arrowSize, fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = GlucoseFormatter.formatMinutesAgo(reading.getMinutesAgo()),
                            style = TextStyle(color = primaryColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        )
                    } else {
                        Text(text = "Brak danych", style = TextStyle(color = primaryColor, fontSize = 12.sp))
                    }
                }

                // Dół: Siatka 6 wkłuć (lekko obniżona, dopasowana bez obcinania)
                Spacer(modifier = GlanceModifier.height(4.dp))
                SitesGrid(todayIndex, primaryColor, inactiveColor, boxSize = boxSize)
                Spacer(modifier = GlanceModifier.height(2.dp))
            }
        }
    }

    @Composable
    private fun SitesGrid(
        todayIndex: Int,
        activeColor: androidx.glance.unit.ColorProvider,
        inactiveColor: androidx.glance.unit.ColorProvider,
        boxSize: androidx.compose.ui.unit.Dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (row in InjectionSiteHelper.GRID_SLOTS) {
                Row(
                    modifier = GlanceModifier.padding(vertical = 1.5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val slot0 = row[0]
                    val slot1 = row[1]
                    Spacer(
                        modifier = GlanceModifier
                            .size(boxSize)
                            .cornerRadius(5.dp)
                            .background(if (slot0 == todayIndex) activeColor else inactiveColor)
                    )
                    Spacer(modifier = GlanceModifier.width(3.dp))
                    Spacer(
                        modifier = GlanceModifier
                            .size(boxSize)
                            .cornerRadius(5.dp)
                            .background(if (slot1 == todayIndex) activeColor else inactiveColor)
                    )
                }
            }
        }
    }
}

class DashboardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DashboardWidget()
}
