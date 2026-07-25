package com.mohgwatch.phone.ui.screens.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohgwatch.phone.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Import / Eksport") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Column(modifier = Modifier.padding(24.dp)) {
            // Import
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FileDownload, null, tint = Teal500, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Importuj tarczę", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Importuj tarczę z pliku WFF XML. Możesz pobrać tarcze ze społeczności lub stworzyć własne.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { /* TODO: File picker */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.FolderOpen, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Wybierz plik WFF", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Export
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FileUpload, null, tint = Indigo500, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Eksportuj tarczę", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Eksportuj tarczę jako plik WFF XML, aby udostępnić ją innym lub zrobić backup.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { /* TODO: Export */ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Indigo500)
                    ) {
                        Icon(Icons.Filled.Share, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Wybierz i eksportuj", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
