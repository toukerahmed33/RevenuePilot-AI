package com.example

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ai.configureAppCheck
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.analyzer.RevenueAnalyzerScreen
import com.example.ui.components.BiometricLockScreen
import com.example.ui.components.CsvImportDialog
import com.example.ui.components.EndOfDaySummaryDialog
import com.example.ui.components.InspectionNoteEditorDialog
import com.example.ui.components.SourceRowInspectorDialog
import com.example.ui.components.TaskEditorDialog
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.RevenuePilotTheme
import com.example.ui.theme.StatusSuccess
import com.example.ui.workspace.WorkspaceScreen

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            RevenuePilotTheme {
                RevenuePilotApp(
                    onBiometricFlagChange = { enabled ->
                        if (enabled) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    },
                    onRequestBiometricUnlock = { onSuccess, onError ->
                        launchBiometricPrompt(onSuccess, onError)
                    }
                )
            }
        }
    }

    private fun launchBiometricPrompt(
        onSuccess: () -> Unit,
        onError: (String?) -> Unit
    ) {
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

        val canAuth = BiometricManager.from(this).canAuthenticate(authenticators)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            onError("ডিভাইসে বায়োমেট্রিক বা পিন লক সক্রিয় নেই।")
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    val isUserCancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    if (isUserCancelled) {
                        onError(null)
                    } else {
                        onError(errString.toString().ifBlank { "প্রমাণীকরণ সম্পন্ন হয়নি।" })
                    }
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("রেভিনিউ পাইলট আনলক করুন")
            .setSubtitle("নিরাপত্তার জন্য বায়োমেট্রিক বা ডিভাইস পিন যাচাই করুন")
            .setAllowedAuthenticators(authenticators)
            .build()

        prompt.authenticate(info)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevenuePilotApp(
    viewModel: MainViewModel = viewModel(),
    onBiometricFlagChange: (Boolean) -> Unit = {},
    onRequestBiometricUnlock: (onSuccess: () -> Unit, onError: (String?) -> Unit) -> Unit = { onSuccess, _ -> onSuccess() }
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val workspaceTab by viewModel.workspaceTab.collectAsStateWithLifecycle()
    val supervisorName by viewModel.supervisorName.collectAsStateWithLifecycle()
    val wardInfo by viewModel.wardInfo.collectAsStateWithLifecycle()
    val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsStateWithLifecycle()
    val isAuthStateLoaded by viewModel.isAuthStateLoaded.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()

    val holdings by viewModel.holdings.collectAsStateWithLifecycle()
    val financialSummary by viewModel.financialSummary.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val inspectionNotes by viewModel.inspectionNotes.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    // Import & File Picker states
    val isImportDialogOpen by viewModel.isImportDialogOpen.collectAsStateWithLifecycle()
    val importedFileName by viewModel.importedFileName.collectAsStateWithLifecycle()
    val isParsingFile by viewModel.isParsingFile.collectAsStateWithLifecycle()
    val csvInputText by viewModel.csvInputText.collectAsStateWithLifecycle()
    val columnMapping by viewModel.columnMapping.collectAsStateWithLifecycle()
    val inspectedHolding by viewModel.inspectedHolding.collectAsStateWithLifecycle()

    // Modals
    val isTaskDialogOpen by viewModel.isTaskDialogOpen.collectAsStateWithLifecycle()
    val editingTask by viewModel.editingTask.collectAsStateWithLifecycle()
    val isInspectionDialogOpen by viewModel.isInspectionDialogOpen.collectAsStateWithLifecycle()
    val editingInspection by viewModel.editingInspection.collectAsStateWithLifecycle()
    val isEndOfDayDialogOpen by viewModel.isEndOfDayDialogOpen.collectAsStateWithLifecycle()

    // Reports & AI
    val selectedReportType by viewModel.selectedReportType.collectAsStateWithLifecycle()
    val customReportRemarks by viewModel.customReportRemarks.collectAsStateWithLifecycle()
    val generatedReport by viewModel.generatedReport.collectAsStateWithLifecycle()
    val aiSummaryState by viewModel.aiSummaryState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Storage Access Framework file picker for CSV & Excel (.xlsx)
    val spreadsheetPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importSpreadsheetFromUri(uri)
        }
    }

    val launchFilePicker = {
        spreadsheetPickerLauncher.launch(
            arrayOf(
                "text/csv",
                "text/comma-separated-values",
                "text/plain",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "*/*"
            )
        )
    }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(biometricLockEnabled) {
        onBiometricFlagChange(biometricLockEnabled)
    }

    // Hold sensitive financial UI until initial lock preference finishes loading from DataStore
    if (!isAuthStateLoaded) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {}
        return
    }

    // Trigger Biometric prompt automatically once if locked
    LaunchedEffect(isAuthStateLoaded, isUnlocked, biometricLockEnabled) {
        if (isAuthStateLoaded && !isUnlocked && biometricLockEnabled) {
            onRequestBiometricUnlock(
                { viewModel.unlockApp() },
                { err -> if (!err.isNullOrBlank()) viewModel.showMessage(err) }
            )
        }
    }

    if (!isUnlocked && biometricLockEnabled) {
        BiometricLockScreen(
            onUnlockClick = {
                onRequestBiometricUnlock(
                    { viewModel.unlockApp() },
                    { err -> if (!err.isNullOrBlank()) viewModel.showMessage(err) }
                )
            }
        )
        return
    }

    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        viewModel.handleBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_revenue_pilot_logo),
                            contentDescription = "রেভিনিউ পাইলট লোগো",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .testTag("app_brand_logo")
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "রেভিনিউ পাইলট",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "RevenuePilot AI",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StatusSuccess.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(StatusSuccess)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "অফলাইন",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = StatusSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        modifier = Modifier.testTag("nav_settings_topbar")
                    ) {
                        Icon(
                            imageVector = if (currentScreen == AppScreen.SETTINGS) Icons.Default.Settings else Icons.Outlined.Settings,
                            contentDescription = "সেটিংস",
                            tint = if (currentScreen == AppScreen.SETTINGS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.DASHBOARD,
                    onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                    icon = {
                        Icon(
                            if (currentScreen == AppScreen.DASHBOARD) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "ড্যাশবোর্ড"
                        )
                    },
                    label = { Text("ড্যাশবোর্ড", maxLines = 1) },
                    modifier = Modifier.testTag("nav_dashboard")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.ANALYZER,
                    onClick = { viewModel.navigateTo(AppScreen.ANALYZER) },
                    icon = {
                        Icon(
                            if (currentScreen == AppScreen.ANALYZER) Icons.Default.PieChart else Icons.Outlined.PieChart,
                            contentDescription = "অ্যানালাইজার"
                        )
                    },
                    label = { Text("অ্যানালাইজার", maxLines = 1) },
                    modifier = Modifier.testTag("nav_analyzer")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.WORKSPACE,
                    onClick = { viewModel.navigateTo(AppScreen.WORKSPACE) },
                    icon = {
                        Icon(
                            if (currentScreen == AppScreen.WORKSPACE) Icons.AutoMirrored.Filled.Assignment else Icons.AutoMirrored.Outlined.Assignment,
                            contentDescription = "কাজ ও নোট"
                        )
                    },
                    label = { Text("কাজ ও নোট", maxLines = 1) },
                    modifier = Modifier.testTag("nav_workspace")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.REPORTS,
                    onClick = { viewModel.navigateTo(AppScreen.REPORTS) },
                    icon = {
                        Icon(
                            if (currentScreen == AppScreen.REPORTS) Icons.Default.Description else Icons.Outlined.Description,
                            contentDescription = "রিপোর্ট"
                        )
                    },
                    label = { Text("রিপোর্ট", maxLines = 1) },
                    modifier = Modifier.testTag("nav_reports")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> DashboardScreen(
                    supervisorName = supervisorName,
                    wardInfo = wardInfo,
                    tasks = tasks,
                    holdings = holdings,
                    inspectionNotes = inspectionNotes,
                    financialSummary = financialSummary,
                    onNavigate = { screen, tab -> viewModel.navigateTo(screen, tab) },
                    onToggleTask = { viewModel.toggleTaskCompletion(it) },
                    onOpenTaskDialog = { viewModel.openTaskDialog(it) },
                    onOpenInspectionDialog = { viewModel.openInspectionDialog(it) },
                    onPickSpreadsheetClick = launchFilePicker,
                    onLoadSampleData = { viewModel.loadSyntheticData() },
                    onOpenEndOfDaySummary = { viewModel.isEndOfDayDialogOpen.value = true }
                )

                AppScreen.ANALYZER -> RevenueAnalyzerScreen(
                    holdings = holdings,
                    financialSummary = financialSummary,
                    isParsingFile = isParsingFile,
                    onPickFileClick = launchFilePicker,
                    onOpenManualImportDialog = { viewModel.openImportDialog() },
                    onSelectHoldingToInspect = { viewModel.inspectHoldingRecord(it) }
                )

                AppScreen.WORKSPACE -> WorkspaceScreen(
                    activeTab = workspaceTab,
                    tasks = tasks,
                    inspectionNotes = inspectionNotes,
                    onSelectTab = { viewModel.setWorkspaceTab(it) },
                    onToggleTask = { viewModel.toggleTaskCompletion(it) },
                    onOpenTaskDialog = { viewModel.openTaskDialog(it) },
                    onDeleteTask = { viewModel.deleteTask(it) },
                    onOpenEndOfDaySummary = { viewModel.isEndOfDayDialogOpen.value = true },
                    onOpenInspectionDialog = { viewModel.openInspectionDialog(it) },
                    onDeleteInspectionNote = { viewModel.deleteInspectionNote(it) }
                )

                AppScreen.REPORTS -> ReportsScreen(
                    generatedReport = generatedReport,
                    selectedReportType = selectedReportType,
                    customRemarks = customReportRemarks,
                    aiSummaryState = aiSummaryState,
                    onSelectReportType = { viewModel.setReportType(it) },
                    onCustomRemarksChange = { viewModel.setReportCustomRemarks(it) },
                    onGenerateReport = { viewModel.generateActiveReport() },
                    onRequestAiSummary = { viewModel.generateAiExecutiveSummary() },
                    onInsertAiSummary = { viewModel.insertAiSummaryIntoRemarks(it) },
                    onShowMessage = { viewModel.showMessage(it) }
                )

                AppScreen.SETTINGS -> SettingsScreen(
                    currentSupervisorName = supervisorName,
                    currentWardInfo = wardInfo,
                    biometricLockEnabled = biometricLockEnabled,
                    onToggleBiometricLock = { viewModel.setBiometricLockEnabled(it) },
                    onSaveSupervisorInfo = { name, ward -> viewModel.setSupervisorInfo(name, ward) },
                    onReloadSampleData = { viewModel.loadSyntheticData() },
                    onClearAllData = { viewModel.clearAllData() },
                    onShowMessage = { viewModel.showMessage(it) }
                )
            }
        }

        // --- Dialogs ---
        CsvImportDialog(
            isOpen = isImportDialogOpen,
            initialCsv = csvInputText,
            importedFileName = importedFileName,
            currentMapping = columnMapping,
            onPickFileClick = launchFilePicker,
            onDismiss = { viewModel.closeImportDialog() },
            onImport = { text, mapping, header ->
                viewModel.setCsvInputText(text)
                viewModel.setColumnMapping(mapping)
                viewModel.processAndImportCsv(header)
            }
        )

        SourceRowInspectorDialog(
            holding = inspectedHolding,
            onDismiss = { viewModel.inspectHoldingRecord(null) }
        )

        TaskEditorDialog(
            isOpen = isTaskDialogOpen,
            taskToEdit = editingTask,
            onDismiss = { viewModel.closeTaskDialog() },
            onSave = { title, desc, hId, date, prio, cat, notes ->
                viewModel.saveTask(title, desc, hId, date, prio, cat, notes)
            }
        )

        InspectionNoteEditorDialog(
            isOpen = isInspectionDialogOpen,
            noteToEdit = editingInspection,
            onDismiss = { viewModel.closeInspectionDialog() },
            onSave = { ref, holding, area, date, status, obs, act, notes, isDraft, photoUri, lat, lng, locLabel ->
                viewModel.saveInspectionNote(
                    ref, holding, area, date, status, obs, act, notes, isDraft,
                    photoUri, lat, lng, locLabel
                )
            }
        )

        EndOfDaySummaryDialog(
            isOpen = isEndOfDayDialogOpen,
            tasks = tasks,
            onDismiss = { viewModel.isEndOfDayDialogOpen.value = false }
        )
    }
}
