package com.mohgwatch.phone.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mohgwatch.phone.data.NightReadinessPolicy
import com.mohgwatch.phone.data.NightReadinessStore
import com.mohgwatch.phone.data.OemChecklist
import com.mohgwatch.phone.data.ReadinessInputs
import com.mohgwatch.phone.data.ReadinessIssue
import com.mohgwatch.phone.util.tr
import java.text.DateFormat
import java.util.Date

data class NightReadinessView(
    val batteryExempt: Boolean = true,
    val exactAlarmsAllowed: Boolean = true,
    val notificationsAllowed: Boolean = true,
    val oemFamily: Boolean = false,
    val checklist: OemChecklist = OemChecklist(),
    val issues: List<ReadinessIssue> = emptyList()
)

/** Current readiness; Android permissions are re-read whenever the user returns to the app. */
@Composable
fun rememberNightReadiness(): NightReadinessView {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val stored by remember(context) { NightReadinessStore.checklist(context) }.collectAsState()
    var resumeCount by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) resumeCount++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return remember(stored, resumeCount) {
        val fingerprint = NightReadinessStore.currentFingerprint
        val checklist = (stored ?: OemChecklist()).validFor(fingerprint)
        val battery = context.getSystemService(android.os.PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(context.packageName) == true
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(android.app.AlarmManager::class.java)?.canScheduleExactAlarms() == true
        val notifications = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val oem = NightReadinessStore.isOemFamily
        NightReadinessView(battery, exact, notifications, oem, checklist,
            NightReadinessPolicy.issues(ReadinessInputs(battery, exact, notifications, oem, checklist, fingerprint)))
    }
}

@Composable
fun NightReadinessCard(view: NightReadinessView) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tr("Gotowość na noc", "Night readiness"), style = MaterialTheme.typography.titleMedium)
            ReadinessRow(view.batteryExempt, tr("Wyjątek optymalizacji baterii", "Battery optimization exemption"),
                tr("Bez niego Android może odciąć internet w uśpieniu.", "Without it Android may cut internet while asleep.")) {
                openFirst(context,
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")),
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
            ReadinessRow(view.exactAlarmsAllowed, tr("Dokładne wybudzenia", "Exact wakeups"),
                tr("Potrzebne do pobierania co kilka minut.", "Needed to fetch every few minutes.")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) openFirst(context,
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
            }
            ReadinessRow(view.notificationsAllowed, tr("Powiadomienia", "Notifications"),
                tr("Bez nich alarmy nie zostaną pokazane.", "Without them alarms cannot be shown.")) {
                openFirst(context, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }

            view.checklist.outage?.let { outage ->
                Text(
                    tr("Tej nocy telefon był bez internetu: ${outageTime(outage.startMillis)} – ${outageTime(outage.endMillis)}. " +
                        if (view.oemFamily) "Sprawdź „Optymalizację gotowości podczas snu” poniżej." else "Sprawdź ustawienia oszczędzania energii.",
                        "Last night the phone had no internet: ${outageTime(outage.startMillis)} – ${outageTime(outage.endMillis)}. " +
                        if (view.oemFamily) "Check “Sleep standby optimization” below." else "Check the power saving settings."),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = { NightReadinessStore.dismissOutage(context) }) { Text(tr("Rozumiem", "Got it")) }
            }

            if (view.oemFamily) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Text(tr("OnePlus / OPPO — sprawdź ręcznie", "OnePlus / OPPO — check manually"), style = MaterialTheme.typography.titleSmall)
                Text(
                    tr("Te ustawienia są niewidoczne dla aplikacji. Zaznacz po sprawdzeniu; po aktualizacji systemu trzeba sprawdzić ponownie.",
                        "Apps cannot read these settings. Tick them after checking; recheck after a system update."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ChecklistRow(view.checklist.sleepStandbyOff,
                    tr("„Optymalizacja gotowości podczas snu” jest wyłączona", "“Sleep standby optimization” is off"),
                    tr("Ta opcja wyłącza Wi-Fi i dane w nocy.", "This option turns off Wi-Fi and data at night."),
                    { NightReadinessStore.setSleepStandbyOff(context, it) })
                OutlinedButton(onClick = {
                    openFirst(context, Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS), Intent(Intent.ACTION_POWER_USAGE_SUMMARY))
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Otwórz ustawienia oszczędzania energii", "Open power saving settings"))
                }
                ChecklistRow(view.checklist.backgroundActivityAllowed,
                    tr("MohgWatch: „Zezwalaj na aktywność w tle”", "MohgWatch: “Allow background activity”"),
                    tr("Ustawienia aplikacji → Wykorzystanie baterii.", "App settings → Battery usage."),
                    { NightReadinessStore.setBackgroundActivityAllowed(context, it) })
                OutlinedButton(onClick = { openFirst(context, appDetails(context)) }, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Otwórz ustawienia aplikacji", "Open app settings"))
                }
            }
        }
    }
}

/** Thin warning strip for the Status screen; tapping it opens the screen that can fix the problem. */
@Composable
fun StatusStrip(text: String, action: String, warning: Boolean, onClick: () -> Unit) {
    val color = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .clickable(onClickLabel = action, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (warning) {
            Icon(Icons.Filled.Warning, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(action, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun ReadinessRow(ok: Boolean, title: String, hint: String, onFix: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning, null,
            tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (!ok) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (!ok) TextButton(onClick = onFix) { Text(tr("Napraw", "Fix")) }
    }
}

@Composable
private fun ChecklistRow(checked: Boolean, title: String, hint: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium,
                color = if (checked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun appDetails(context: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

/** Opens the first screen the system can handle, falling back to the app's own settings page. */
fun openFirst(context: Context, vararg intents: Intent) {
    for (intent in listOf(*intents, appDetails(context))) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: Exception) {
        }
    }
}

private fun outageTime(millis: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))

