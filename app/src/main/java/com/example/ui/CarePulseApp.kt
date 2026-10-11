package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.auth.AuthScreen
import com.example.auth.GoogleAuthManager
import com.example.ui.components.EmergencySosDialog
import com.example.ui.screens.AiConsultantScreen
import com.example.ui.screens.CarePlanScreen
import com.example.ui.screens.HealthMonitorScreen
import com.example.ui.screens.PatientProfileScreen
import com.example.ui.screens.SymptomAnalyzerScreen
import com.example.ui.screens.TeleconsultationScreen
import com.example.ui.theme.TriageEmergencyRed
import com.example.ui.theme.TriageSafeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarePulseApp(
    viewModel: CarePulseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    // Auth gate: if not signed in, show AuthScreen with Google Sign-In button
    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = { /* AuthStateListener updates currentUser */ },
            modifier = modifier
        )
        return
    }

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val liveVitals by viewModel.liveVitals.collectAsStateWithLifecycle()
    val ecgWavePoints by viewModel.ecgWavePoints.collectAsStateWithLifecycle()
    val allVitals by viewModel.allVitals.collectAsStateWithLifecycle()
    val allSymptomReports by viewModel.allSymptomReports.collectAsStateWithLifecycle()
    val allConsultations by viewModel.allConsultations.collectAsStateWithLifecycle()
    val allMedications by viewModel.allMedications.collectAsStateWithLifecycle()
    val symptomInput by viewModel.symptomInput.collectAsStateWithLifecycle()
    val videoCallState by viewModel.videoCallState.collectAsStateWithLifecycle()
    val showSosDialog by viewModel.showSosDialog.collectAsStateWithLifecycle()

    val chatMessages by viewModel.displayChatMessages.collectAsStateWithLifecycle()
    val selectedAiRole by viewModel.selectedAiRole.collectAsStateWithLifecycle()
    val isAiGenerating by viewModel.isAiGenerating.collectAsStateWithLifecycle()

    var showUserMenu by remember { mutableStateOf(false) }

    // Handle back button
    BackHandler(enabled = currentScreen != ScreenDestination.HEALTH_MONITOR) {
        if (videoCallState.isActive) {
            viewModel.endConsultation()
        } else {
            viewModel.navigateTo(ScreenDestination.HEALTH_MONITOR)
        }
    }

    if (showSosDialog) {
        EmergencySosDialog(
            currentHeartRate = liveVitals.heartRate,
            currentSpo2 = liveVitals.spo2,
            currentBp = "${liveVitals.systolicBp}/${liveVitals.diastolicBp}",
            onDismiss = { viewModel.dismissSosDialog() }
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            topBar = {
                if (!videoCallState.isActive) {
                    CenterAlignedTopAppBar(
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_app_icon),
                                    contentDescription = "CarePulse",
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CarePulse",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "TABLET STATION",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            Row(
                                modifier = Modifier.padding(start = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (liveVitals.isAllSensorsOnline) TriageSafeGreen else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (liveVitals.isAllSensorsOnline) "Sensors 4/4" else "Sensors Off",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            // User account dropdown
                            Box {
                                IconButton(onClick = { showUserMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Account",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                DropdownMenu(
                                    expanded = showUserMenu,
                                    onDismissRequest = { showUserMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = currentUser?.displayName ?: "Patient User",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = currentUser?.email ?: "Google Account",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {}
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.Logout, contentDescription = null, tint = TriageEmergencyRed)
                                        },
                                        text = { Text("Sign Out", color = TriageEmergencyRed) },
                                        onClick = {
                                            showUserMenu = false
                                            GoogleAuthManager.signOut(
                                                context = context,
                                                credentialManager = credentialManager,
                                                onSignOutComplete = {},
                                                scope = coroutineScope
                                            )
                                        }
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.openSosDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = TriageEmergencyRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .testTag("top_bar_sos_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Emergency,
                                    contentDescription = "SOS",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "SOS", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!isWideScreen && !videoCallState.isActive) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.testTag("mobile_bottom_nav")
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == ScreenDestination.HEALTH_MONITOR,
                            onClick = { viewModel.navigateTo(ScreenDestination.HEALTH_MONITOR) },
                            icon = { Icon(imageVector = Icons.Default.MonitorHeart, contentDescription = "Monitor") },
                            label = { Text("Monitor", fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_item_monitor")
                        )
                        NavigationBarItem(
                            selected = currentScreen == ScreenDestination.SYMPTOM_ANALYSIS,
                            onClick = { viewModel.navigateTo(ScreenDestination.SYMPTOM_ANALYSIS) },
                            icon = { Icon(imageVector = Icons.Default.Healing, contentDescription = "Symptoms") },
                            label = { Text("Symptoms", fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_item_symptoms")
                        )
                        NavigationBarItem(
                            selected = currentScreen == ScreenDestination.AI_ASSISTANT,
                            onClick = { viewModel.navigateTo(ScreenDestination.AI_ASSISTANT) },
                            icon = { Icon(imageVector = Icons.Default.SmartToy, contentDescription = "AI Doctor") },
                            label = { Text("AI Doctor", fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_item_ai_doctor")
                        )
                        NavigationBarItem(
                            selected = currentScreen == ScreenDestination.TELECONSULTATION,
                            onClick = { viewModel.navigateTo(ScreenDestination.TELECONSULTATION) },
                            icon = { Icon(imageVector = Icons.Default.VideoCall, contentDescription = "Consult") },
                            label = { Text("Consult", fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_item_consult")
                        )
                        NavigationBarItem(
                            selected = currentScreen == ScreenDestination.CARE_PLAN,
                            onClick = { viewModel.navigateTo(ScreenDestination.CARE_PLAN) },
                            icon = { Icon(imageVector = Icons.Outlined.Medication, contentDescription = "Care Plan") },
                            label = { Text("Care Plan", fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_item_care_plan")
                        )
                        NavigationBarItem(
                            selected = currentScreen == ScreenDestination.PATIENT_PROFILE,
                            onClick = { viewModel.navigateTo(ScreenDestination.PATIENT_PROFILE) },
                            icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Profile") },
                            label = { Text("Profile", fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_item_profile")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Wide Screen / Tablet Navigation Rail
                if (isWideScreen && !videoCallState.isActive) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("tablet_navigation_rail"),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        NavigationRailItem(
                            selected = currentScreen == ScreenDestination.HEALTH_MONITOR,
                            onClick = { viewModel.navigateTo(ScreenDestination.HEALTH_MONITOR) },
                            icon = { Icon(imageVector = Icons.Default.MonitorHeart, contentDescription = "Monitor") },
                            label = { Text("Monitor") }
                        )
                        NavigationRailItem(
                            selected = currentScreen == ScreenDestination.SYMPTOM_ANALYSIS,
                            onClick = { viewModel.navigateTo(ScreenDestination.SYMPTOM_ANALYSIS) },
                            icon = { Icon(imageVector = Icons.Default.Healing, contentDescription = "Symptoms") },
                            label = { Text("Symptoms") }
                        )
                        NavigationRailItem(
                            selected = currentScreen == ScreenDestination.AI_ASSISTANT,
                            onClick = { viewModel.navigateTo(ScreenDestination.AI_ASSISTANT) },
                            icon = { Icon(imageVector = Icons.Default.SmartToy, contentDescription = "AI Doctor") },
                            label = { Text("AI Doctor") }
                        )
                        NavigationRailItem(
                            selected = currentScreen == ScreenDestination.TELECONSULTATION,
                            onClick = { viewModel.navigateTo(ScreenDestination.TELECONSULTATION) },
                            icon = { Icon(imageVector = Icons.Default.VideoCall, contentDescription = "Consult") },
                            label = { Text("Consult") }
                        )
                        NavigationRailItem(
                            selected = currentScreen == ScreenDestination.CARE_PLAN,
                            onClick = { viewModel.navigateTo(ScreenDestination.CARE_PLAN) },
                            icon = { Icon(imageVector = Icons.Outlined.Medication, contentDescription = "Care Plan") },
                            label = { Text("Care Plan") }
                        )
                        NavigationRailItem(
                            selected = currentScreen == ScreenDestination.PATIENT_PROFILE,
                            onClick = { viewModel.navigateTo(ScreenDestination.PATIENT_PROFILE) },
                            icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Profile") },
                            label = { Text("Profile") }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen) {
                        ScreenDestination.HEALTH_MONITOR -> {
                            HealthMonitorScreen(
                                liveVitals = liveVitals,
                                ecgPoints = ecgWavePoints,
                                vitalsHistory = allVitals,
                                onScanNow = { viewModel.performVitalsScan() },
                                onSimulationProfileChange = { viewModel.setSimulationProfile(it) },
                                onConnectDoctor = { viewModel.navigateTo(ScreenDestination.TELECONSULTATION) }
                            )
                        }

                        ScreenDestination.SYMPTOM_ANALYSIS -> {
                            SymptomAnalyzerScreen(
                                inputState = symptomInput,
                                liveVitals = liveVitals,
                                pastReports = allSymptomReports,
                                onRegionChange = { viewModel.updateSelectedRegion(it) },
                                onSymptomChange = { viewModel.updateSelectedSymptom(it) },
                                onSeverityChange = { viewModel.updateSeverity(it) },
                                onDurationChange = { viewModel.updateDuration(it) },
                                onNotesChange = { viewModel.updateAdditionalNotes(it) },
                                onSaveReport = { viewModel.saveCurrentSymptomReport() },
                                onStartTeleconsult = { viewModel.navigateTo(ScreenDestination.TELECONSULTATION) },
                                onEmergencySos = { viewModel.openSosDialog() }
                            )
                        }

                        ScreenDestination.AI_ASSISTANT -> {
                            AiConsultantScreen(
                                messages = chatMessages,
                                selectedRole = selectedAiRole,
                                onRoleSelected = { viewModel.selectAiRole(it) },
                                isGenerating = isAiGenerating,
                                liveVitals = liveVitals,
                                onSendMessage = { text, attachVitals ->
                                    viewModel.sendAiUserMessage(text, attachVitals)
                                }
                            )
                        }

                        ScreenDestination.TELECONSULTATION -> {
                            TeleconsultationScreen(
                                videoCallState = videoCallState,
                                availableDoctors = viewModel.availableDoctors,
                                pastConsultations = allConsultations,
                                liveVitals = liveVitals,
                                onStartCall = { viewModel.startConsultation(it) },
                                onEndCall = { viewModel.endConsultation() },
                                onToggleMute = { viewModel.toggleMute() },
                                onToggleCamera = { viewModel.toggleCamera() },
                                onSendMessage = { viewModel.sendPatientMessage(it) },
                                onDismissCompletedSummary = {
                                    viewModel.resetConsultationCompletedState()
                                    viewModel.navigateTo(ScreenDestination.CARE_PLAN)
                                }
                            )
                        }

                        ScreenDestination.CARE_PLAN -> {
                            CarePlanScreen(
                                medications = allMedications,
                                onToggleMedication = { id, taken -> viewModel.toggleMedicationTaken(id, taken) }
                            )
                        }

                        ScreenDestination.PATIENT_PROFILE -> {
                            PatientProfileScreen(
                                onTriggerSos = { viewModel.openSosDialog() }
                            )
                        }
                    }
                }
            }
        }
    }
}
