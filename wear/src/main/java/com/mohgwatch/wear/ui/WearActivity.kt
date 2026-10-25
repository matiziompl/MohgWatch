package com.mohgwatch.wear.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.mohgwatch.core.util.GlucoseFormatter
import com.mohgwatch.wear.BuildConfig
import com.mohgwatch.wear.data.GlucoseRepository
import com.mohgwatch.wear.service.WristDetectionManager
import kotlinx.coroutines.delay

class WearActivity : ComponentActivity() {

    private val repository by lazy { GlucoseRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        
        setContent {
            WearAppScreen(repository)
        }
    }
}

@Composable
fun WearAppScreen(repository: GlucoseRepository) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsStore = remember { com.mohgwatch.wear.data.WearSettingsStore(context) }
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle(initialValue = com.mohgwatch.core.model.UserSettings())
    
    com.mohgwatch.wear.ui.theme.WearMohgWatchTheme {
        val readingState by remember(repository) { repository.observeLatest() }.collectAsStateWithLifecycle(initialValue = null)
        val lifecycleOwner = LocalLifecycleOwner.current
        val now by produceState(System.currentTimeMillis(), readingState?.timestamp, lifecycleOwner, settings.staleAfterMillis) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    value = System.currentTimeMillis()
                    val elapsed = (value - (readingState?.timestamp ?: value)).coerceAtLeast(0L)
                    delay(60_000L - elapsed % 60_000L)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "MohgWatch",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            val reading = readingState
            if (reading != null) {
                val isFresh = reading.isFreshAt(now, settings.staleAfterMillis)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFresh) GlucoseFormatter.format(reading.value, settings.unit) else "- -",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isFresh) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = reading.trendArrow.symbol,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = if (isFresh) "${settings.unit.shortLabel} • ${GlucoseFormatter.formatMinutesAgo((now - reading.timestamp) / 60_000L)}"
                        else "Dane przeterminowane (${settings.staleAfterMillis / 60_000L} min)",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            } else {
                Text(
                    text = "- -",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "Oczekiwanie na dane z telefonu...",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val isWorn by WristDetectionManager.isWornState.collectAsStateWithLifecycle()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (isWorn) Color(0xFF10B981) else Color(0xFFEF4444),
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isWorn) "Na ręku" else "Zdjęty z ręki",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isWorn) Color(0xFF10B981) else Color(0xFFEF4444),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
}
