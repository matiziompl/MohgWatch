package com.mohgwatch.phone.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohgwatch.core.api.LibreLinkUpClient
import com.mohgwatch.phone.R
import com.mohgwatch.phone.data.CredentialStore
import com.mohgwatch.phone.data.Diagnostics
import com.mohgwatch.phone.service.GlucoseSyncService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialStore = remember { CredentialStore(context) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("global") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var regionExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val creds = credentialStore.getCredentials()
        if (creds != null) {
            email = creds.email.orEmpty()
            selectedRegion = creds.region
        }
    }

    val regions = listOf(
        "global" to "Automatyczny",
        "eu" to "Europa (EU)",
        "eu2" to "Europa 2 (EU2)",
        "us" to "USA",
        "de" to "Niemcy (DE)",
        "fr" to "Francja (FR)",
        "jp" to "Japonia (JP)",
        "ap" to "Azja-Pacyfik (AP)",
        "au" to "Australia (AU)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .size(128.dp)
                .clip(RoundedCornerShape(20.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "MohgWatch",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Monitor glukozy na Twoim zegarku",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Login Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Zaloguj się do LibreLinkUp",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = { Icon(Icons.Filled.Email, "Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Hasło") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    leadingIcon = { Icon(Icons.Filled.Lock, "Hasło") },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.VisibilityOff
                                else Icons.Filled.Visibility,
                                "Pokaż/ukryj hasło"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Region
                ExposedDropdownMenuBox(
                    expanded = regionExpanded,
                    onExpandedChange = { regionExpanded = it }
                ) {
                    OutlinedTextField(
                        value = regions.find { it.first == selectedRegion }?.second ?: "Automatyczny",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Region") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionExpanded) },
                        leadingIcon = { Icon(Icons.Filled.Public, "Region") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = regionExpanded,
                        onDismissRequest = { regionExpanded = false }
                    ) {
                        regions.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedRegion = code
                                    regionExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Error
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warning, "Błąd", tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Login Button
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Wypełnij email i hasło"
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        val loginEmail = email.trim()
                        val loginPassword = password
                        val region = if (selectedRegion == "global") null else selectedRegion

                        scope.launch {
                            try {
                                Diagnostics.record(context, "LOGIN_START", "region=${region ?: "automatic"}; ${Diagnostics.snapshot(context)}")
                                val client = LibreLinkUpClient { event, details -> Diagnostics.record(context, event, details) }
                                when (val result = client.login(loginEmail, loginPassword, region)) {
                                    is LibreLinkUpClient.ApiResult.Success -> {
                                        val session = result.data
                                        credentialStore.saveCredentials(
                                            token = session.token,
                                            userId = session.userId,
                                            region = session.region,
                                            expires = session.expires,
                                            email = loginEmail,
                                            password = loginPassword
                                        )
                                        Diagnostics.record(context, "LOGIN_SUCCESS")
                                        GlucoseSyncService.start(context)
                                        onLoginSuccess()
                                    }
                                    is LibreLinkUpClient.ApiResult.Error -> {
                                        Diagnostics.record(context, "LOGIN_ERROR", "type=api code=${result.code}")
                                        errorMessage = result.message
                                    }
                                    is LibreLinkUpClient.ApiResult.NetworkError -> {
                                        Diagnostics.record(context, "LOGIN_ERROR", "type=network kind=${result.kind}")
                                        errorMessage = result.message
                                    }
                                    is LibreLinkUpClient.ApiResult.LoginRequired -> {
                                        Diagnostics.record(context, "LOGIN_ERROR", "type=credentials_rejected")
                                        errorMessage = "LibreLinkUp odrzucił logowanie. Sprawdź email, hasło i możliwość logowania w oficjalnej aplikacji LibreLinkUp."
                                    }
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                Diagnostics.record(context, "LOGIN_LOCAL_ERROR", Diagnostics.error(failure))
                                errorMessage = "Nie udało się zakończyć logowania w aplikacji. Skopiuj raport diagnostyczny z ustawień."
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Zaloguj", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Follower Info
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp)) {
                Icon(
                    Icons.Filled.Info,
                    "Info",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Upewnij się, że w aplikacji LibreLink dodałeś swoje konto jako " +
                            "\"opiekuna\" (follower) w ustawieniach LibreLinkUp.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
