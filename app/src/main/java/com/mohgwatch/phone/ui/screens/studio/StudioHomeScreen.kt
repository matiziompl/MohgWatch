package com.mohgwatch.phone.ui.screens.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohgwatch.phone.ui.theme.*

@Composable
fun StudioHomeScreen(
    onNavigateToPresets: () -> Unit,
    onNavigateToEditor: () -> Unit,
    onNavigateToImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Text(
            "Studio Tarcz",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Twórz, modyfikuj i zarządzaj tarczami zegarka",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action Cards Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Preset Gallery
            item {
                StudioActionCard(
                    icon = Icons.Filled.Collections,
                    title = "Galeria\nPresetów",
                    subtitle = "15 wbudowanych",
                    gradientColors = listOf(Teal500, Teal700),
                    onClick = onNavigateToPresets
                )
            }

            // Create New
            item {
                StudioActionCard(
                    icon = Icons.Filled.Add,
                    title = "Stwórz\nNową",
                    subtitle = "Od podstaw",
                    gradientColors = listOf(Indigo500, Indigo600),
                    onClick = onNavigateToEditor
                )
            }

            // Import
            item {
                StudioActionCard(
                    icon = Icons.Filled.FileDownload,
                    title = "Importuj",
                    subtitle = "Z pliku WFF",
                    gradientColors = listOf(WarningAmber, GlucoseHigh),
                    onClick = onNavigateToImport
                )
            }

            // My Faces
            item {
                StudioActionCard(
                    icon = Icons.Filled.Folder,
                    title = "Moje\nTarcze",
                    subtitle = "0 zapisanych",
                    gradientColors = listOf(GlucoseInRange, Teal600),
                    onClick = { /* TODO */ }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudioActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    gradientColors: List<androidx.compose.ui.graphics.Color>,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient accent at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Brush.horizontalGradient(gradientColors))
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    icon, null,
                    tint = gradientColors.first(),
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
