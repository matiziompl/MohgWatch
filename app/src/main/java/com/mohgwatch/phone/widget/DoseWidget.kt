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
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.phone.MainActivity
import com.mohgwatch.phone.data.SettingsStore
import kotlinx.coroutines.flow.first

class DoseWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = SettingsStore(context).settingsFlow.first()
        provideContent {
            WidgetContent(settings)
        }
    }

    @Composable
    private fun WidgetContent(settings: UserSettings) {
        val surfaceColor = WidgetThemeHelper.surface(settings.widgetBgColor)
        val primaryColor = WidgetThemeHelper.primary(settings.widgetPrimaryTheme, null, settings)

        val b = settings.doseBreakfast.ifBlank { "—" }
        val l = settings.doseLunch.ifBlank { "—" }
        val d = settings.doseDinner.ifBlank { "—" }
        val n = settings.doseNight.ifBlank { "—" }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surfaceColor)
                .padding(12.dp)
                .clickable(actionStartActivity(android.content.Intent(androidx.glance.LocalContext.current, MainActivity::class.java))),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val lines = listOf("Ś: $b", "O: $l", "K: $d", "N: $n")
            Spacer(modifier = GlanceModifier.defaultWeight())
            lines.forEach { line ->
                Text(
                    text = line,
                    style = TextStyle(
                        color = primaryColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
            }
        }
    }
}

class DoseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DoseWidget()
}
