package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiSummaryUiState
import com.example.data.ai.ReportAiRepository
import com.example.data.local.AppDatabase
import com.example.data.local.entity.HoldingRecordEntity
import com.example.data.local.entity.InspectionNoteEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.repository.InspectionRepository
import com.example.data.repository.RevenueRepository
import com.example.data.repository.TaskRepository
import com.example.data.repository.UserPreferences
import com.example.data.repository.UserPreferencesRepository
import com.example.domain.analyzer.ColumnMapping
import com.example.domain.analyzer.FinancialSummary
import com.example.domain.analyzer.RevenueAnalysisEngine
import com.example.domain.analyzer.SpreadsheetFileParser
import com.example.domain.inspection.FieldMediaHelper
import com.example.domain.report.GeneratedReport
import com.example.domain.report.ReportGeneratorEngine
import com.example.domain.report.ReportType
import com.example.domain.sample.SampleDataGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    DASHBOARD,
    ANALYZER,
    WORKSPACE,
    REPORTS,
    SETTINGS
}

enum class WorkspaceTab {
    TASKS,
    INSPECTIONS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val revenueRepository = RevenueRepository(db.holdingDao())
    val taskRepository = TaskRepository(db.taskDao())
    val inspectionRepository = InspectionRepository(db.inspectionNoteDao())
    private val preferencesRepository = UserPreferencesRepository(application)
    private val reportAiRepository = ReportAiRepository()

    // Navigation state
    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _workspaceTab = MutableStateFlow(WorkspaceTab.TASKS)
    val workspaceTab: StateFlow<WorkspaceTab> = _workspaceTab.asStateFlow()

    private val screenBackStack = mutableListOf<AppScreen>()

    // Preferences & Biometric Gate
    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val supervisorName: StateFlow<String> = userPreferences
        .map { it.supervisorName }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "মোহাম্মদ জাহিদ হাসান")

    val wardInfo: StateFlow<String> = userPreferences
        .map { it.wardInfo }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ওয়ার্ড-০৭, মিরপুর জোন")

    val biometricLockEnabled: StateFlow<Boolean> = userPreferences
        .map { it.biometricLockEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isAuthStateLoaded = MutableStateFlow(false)
    val isAuthStateLoaded: StateFlow<Boolean> = _isAuthStateLoaded.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    // Holdings Data
    val holdings: StateFlow<List<HoldingRecordEntity>> = revenueRepository.allHoldings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val financialSummary: StateFlow<FinancialSummary> = revenueRepository.financialSummary
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            RevenueAnalysisEngine.calculateSummary(emptyList())
        )

    // Tasks Data
    val tasks: StateFlow<List<TaskEntity>> = taskRepository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Inspection Notes Data
    val inspectionNotes: StateFlow<List<InspectionNoteEntity>> = inspectionRepository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Snackbars / Alerts
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // CSV / Excel Import State
    private val _isImportDialogOpen = MutableStateFlow(false)
    val isImportDialogOpen: StateFlow<Boolean> = _isImportDialogOpen.asStateFlow()

    private val _importedFileName = MutableStateFlow<String?>(null)
    val importedFileName: StateFlow<String?> = _importedFileName.asStateFlow()

    private val _csvInputText = MutableStateFlow("")
    val csvInputText: StateFlow<String> = _csvInputText.asStateFlow()

    private val _parsedSheetRows = MutableStateFlow<List<List<String>>>(emptyList())
    val parsedSheetRows: StateFlow<List<List<String>>> = _parsedSheetRows.asStateFlow()

    private val _isParsingFile = MutableStateFlow(false)
    val isParsingFile: StateFlow<Boolean> = _isParsingFile.asStateFlow()

    private val _columnMapping = MutableStateFlow(ColumnMapping())
    val columnMapping: StateFlow<ColumnMapping> = _columnMapping.asStateFlow()

    // Active Task Editor Modal
    private val _editingTask = MutableStateFlow<TaskEntity?>(null)
    val editingTask: StateFlow<TaskEntity?> = _editingTask.asStateFlow()
    val isTaskDialogOpen = MutableStateFlow(false)

    // Active Inspection Note Editor Modal
    private val _editingInspection = MutableStateFlow<InspectionNoteEntity?>(null)
    val editingInspection: StateFlow<InspectionNoteEntity?> = _editingInspection.asStateFlow()
    val isInspectionDialogOpen = MutableStateFlow(false)

    // End-of-Day Summary Dialog
    val isEndOfDayDialogOpen = MutableStateFlow(false)

    // Report Generator & AI State
    private val _selectedReportType = MutableStateFlow(ReportType.DAILY_ACTIVITY)
    val selectedReportType: StateFlow<ReportType> = _selectedReportType.asStateFlow()

    private val _customReportRemarks = MutableStateFlow("")
    val customReportRemarks: StateFlow<String> = _customReportRemarks.asStateFlow()

    private val _generatedReport = MutableStateFlow<GeneratedReport?>(null)
    val generatedReport: StateFlow<GeneratedReport?> = _generatedReport.asStateFlow()

    private val _aiSummaryState = MutableStateFlow<AiSummaryUiState>(AiSummaryUiState.Idle)
    val aiSummaryState: StateFlow<AiSummaryUiState> = _aiSummaryState.asStateFlow()

    private var aiSummaryJob: Job? = null

    // Selected Holding for Source Row Inspector
    private val _inspectedHolding = MutableStateFlow<HoldingRecordEntity?>(null)
    val inspectedHolding: StateFlow<HoldingRecordEntity?> = _inspectedHolding.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = preferencesRepository.userPreferences.first()
            _isUnlocked.value = !prefs.biometricLockEnabled
            _isAuthStateLoaded.value = true
            if (revenueRepository.getHoldingsCount() == 0) {
                loadSyntheticData()
            }
        }
    }

    fun unlockApp() {
        _isUnlocked.value = true
    }

    fun lockApp() {
        _isUnlocked.value = false
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBiometricLockEnabled(enabled)
            showMessage(if (enabled) "বায়োমেট্রিক নিরাপত্তা লক চালু করা হয়েছে" else "বায়োমেট্রিক লক বন্ধ করা হয়েছে")
        }
    }

    fun navigateTo(screen: AppScreen, initialWorkspaceTab: WorkspaceTab? = null) {
        if (initialWorkspaceTab != null) {
            _workspaceTab.value = initialWorkspaceTab
        }
        if (_currentScreen.value != screen) {
            screenBackStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun setWorkspaceTab(tab: WorkspaceTab) {
        _workspaceTab.value = tab
    }

    fun handleBack(): Boolean {
        return if (screenBackStack.isNotEmpty()) {
            _currentScreen.value = screenBackStack.removeAt(screenBackStack.size - 1)
            true
        } else if (_currentScreen.value != AppScreen.DASHBOARD) {
            _currentScreen.value = AppScreen.DASHBOARD
            true
        } else {
            false
        }
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    fun setSupervisorInfo(name: String, ward: String) {
        viewModelScope.launch {
            preferencesRepository.updateSupervisorProfile(name, ward)
            showMessage("তত্ত্বাবধায়ক ও ওয়ার্ড তথ্য সংরক্ষিত হয়েছে")
        }
    }

    // --- Synthetic Sample Data ---
    fun loadSyntheticData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                FieldMediaHelper.clearAllInspectionPhotos(getApplication())
            }
            revenueRepository.loadSyntheticSampleData()
            taskRepository.loadSyntheticSampleData()
            inspectionRepository.loadSyntheticSampleData()
            showMessage("২০টি হোল্ডিং, ১০টি টাস্ক এবং ৫টি পরিদর্শন নোটের সিনথেটিক নমুনা ডাটা লোড হয়েছে")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                FieldMediaHelper.clearAllInspectionPhotos(getApplication())
            }
            revenueRepository.clearAllHoldings()
            taskRepository.clearAll()
            inspectionRepository.clearAll()
            _generatedReport.value = null
            _aiSummaryState.value = AiSummaryUiState.Idle
            showMessage("সকল স্থানীয় ডাটা মুছে ফেলা হয়েছে")
        }
    }

    // --- CSV & Excel (.xlsx) Import & Deterministic Analyzer ---
    fun openImportDialog() {
        _importedFileName.value = null
        _parsedSheetRows.value = emptyList()
        _csvInputText.value = SampleDataGenerator.getSampleCsvRaw()
        _columnMapping.value = ColumnMapping()
        _isImportDialogOpen.value = true
    }

    fun importSpreadsheetFromUri(uri: Uri) {
        viewModelScope.launch {
            _isParsingFile.value = true
            try {
                val parsed = withContext(Dispatchers.IO) {
                    SpreadsheetFileParser.parseFromUri(getApplication(), uri)
                }
                if (parsed.rows.isEmpty()) {
                    showMessage("নির্বাচিত ফাইলে কোনো বৈধ সারি পাওয়া যায়নি")
                } else {
                    _importedFileName.value = parsed.fileName
                    _parsedSheetRows.value = parsed.rows
                    _columnMapping.value = parsed.suggestedMapping
                    _csvInputText.value = parsed.rows.joinToString("\n") { row ->
                        row.joinToString(",") { cell ->
                            val singleLine = cell.replace("\r\n", " ").replace("\n", " ").replace("\r", " ")
                            if (singleLine.contains(",") || singleLine.contains("\"")) {
                                "\"${singleLine.replace("\"", "\"\"")}\""
                            } else {
                                singleLine
                            }
                        }
                    }
                    _isImportDialogOpen.value = true
                    showMessage("${parsed.fileName} লোড হয়েছে (${parsed.rows.size} সারি)। কলাম যাচাই করে আমদানি করুন।")
                }
            } catch (e: Exception) {
                showMessage("ফাইল পড়তে ত্রুটি: ${e.localizedMessage ?: "অসমর্থিত ফরম্যাট"}")
            } finally {
                _isParsingFile.value = false
            }
        }
    }

    fun closeImportDialog() {
        _isImportDialogOpen.value = false
    }

    fun setCsvInputText(text: String) {
        _csvInputText.value = text
    }

    fun setColumnMapping(mapping: ColumnMapping) {
        _columnMapping.value = mapping
    }

    fun processAndImportCsv(hasHeader: Boolean = true) {
        val text = _csvInputText.value.trim()
        val preParsed = _parsedSheetRows.value
        if (text.isEmpty() && preParsed.isEmpty()) {
            showMessage("অনুগ্রহ করে বৈধ CSV বা Excel ফাইল প্রদান করুন")
            return
        }
        viewModelScope.launch {
            try {
                val rows = if (text.isNotEmpty()) {
                    RevenueAnalysisEngine.parseCsv(text)
                } else {
                    preParsed
                }
                if (rows.isEmpty()) {
                    showMessage("কোনো বৈধ সারি পাওয়া যায়নি")
                    return@launch
                }
                val entities = RevenueAnalysisEngine.processRows(
                    rows = rows,
                    mapping = _columnMapping.value,
                    hasHeader = hasHeader
                )
                revenueRepository.insertHoldings(entities)
                _isImportDialogOpen.value = false
                val flagCount = entities.count { it.hasCalculationMismatch || it.hasMissingValues || it.isDuplicate }
                showMessage("সফলভাবে ${entities.size}টি হোল্ডিং ডাটা আমদানি হয়েছে! (অডিট ফ্ল্যাগ: $flagCount)")
            } catch (e: Exception) {
                showMessage("ডাটা প্রক্রিয়াকরণে ত্রুটি: ${e.localizedMessage}")
            }
        }
    }

    fun inspectHoldingRecord(holding: HoldingRecordEntity?) {
        _inspectedHolding.value = holding
    }

    // --- Task Actions ---
    fun openTaskDialog(task: TaskEntity? = null) {
        _editingTask.value = task
        isTaskDialogOpen.value = true
    }

    fun closeTaskDialog() {
        _editingTask.value = null
        isTaskDialogOpen.value = false
    }

    fun saveTask(
        title: String,
        description: String,
        holdingId: String,
        dueDate: String,
        priority: String,
        category: String,
        notes: String
    ) {
        if (title.isBlank()) {
            showMessage("টাস্কের শিরোনাম আবশ্যক")
            return
        }
        viewModelScope.launch {
            val existing = _editingTask.value
            if (existing != null) {
                val updated = existing.copy(
                    title = title.trim(),
                    description = description.trim(),
                    holdingId = holdingId.trim(),
                    dueDate = dueDate.trim(),
                    priority = priority,
                    category = category,
                    notes = notes.trim()
                )
                taskRepository.updateTask(updated)
                showMessage("টাস্ক হালনাগাদ করা হয়েছে")
            } else {
                val newTask = TaskEntity(
                    title = title.trim(),
                    description = description.trim(),
                    holdingId = holdingId.trim(),
                    dueDate = dueDate.trim(),
                    priority = priority,
                    category = category,
                    status = "PENDING",
                    notes = notes.trim()
                )
                taskRepository.insertTask(newTask)
                showMessage("নতুন টাস্ক সংরক্ষিত হয়েছে")
            }
            closeTaskDialog()
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(task)
            val stateText = if (task.status == "COMPLETED") "অপেক্ষমাণ করা হয়েছে" else "সম্পন্ন হিসেবে চিহ্নিত"
            showMessage("টাস্ক $stateText")
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
            showMessage("টাস্ক মুছে ফেলা হয়েছে")
        }
    }

    // --- Field Inspection Actions ---
    fun openInspectionDialog(note: InspectionNoteEntity? = null) {
        _editingInspection.value = note
        isInspectionDialogOpen.value = true
    }

    fun closeInspectionDialog() {
        _editingInspection.value = null
        isInspectionDialogOpen.value = false
    }

    fun saveInspectionNote(
        inspectionRef: String,
        holdingRef: String,
        assignedArea: String,
        inspectionDate: String,
        verificationStatus: String,
        observations: String,
        followUpAction: String,
        notes: String,
        isDraft: Boolean,
        photoUri: String,
        latitude: Double?,
        longitude: Double?,
        locationLabel: String
    ) {
        if (holdingRef.isBlank() || observations.isBlank()) {
            showMessage("হোল্ডিং রেফারেন্স এবং মাঠ পর্যবেক্ষণ আবশ্যক")
            return
        }
        viewModelScope.launch {
            val existing = _editingInspection.value
            val now = System.currentTimeMillis()
            val ref = if (inspectionRef.isBlank()) {
                val datePart = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                "INSP-$datePart-${(100..999).random()}"
            } else inspectionRef.trim()

            if (existing != null) {
                val previousPhotoUri = existing.photoUri
                val updated = existing.copy(
                    inspectionRef = ref,
                    holdingRef = holdingRef.trim(),
                    assignedArea = assignedArea.trim(),
                    inspectionDate = inspectionDate.trim(),
                    verificationStatus = verificationStatus,
                    observations = observations.trim(),
                    followUpAction = followUpAction.trim(),
                    notes = notes.trim(),
                    isDraft = isDraft,
                    photoUri = photoUri,
                    latitude = latitude,
                    longitude = longitude,
                    locationLabel = locationLabel,
                    updatedAt = now
                )
                inspectionRepository.updateNote(updated)
                if (previousPhotoUri.isNotBlank() && previousPhotoUri != photoUri) {
                    withContext(Dispatchers.IO) {
                        FieldMediaHelper.deletePhotoFileIfInternal(getApplication(), previousPhotoUri)
                    }
                }
                showMessage(if (isDraft) "খসড়া সংরক্ষিত হয়েছে" else "চূড়ান্ত পরিদর্শন নোট সংরক্ষিত হয়েছে")
            } else {
                val newNote = InspectionNoteEntity(
                    inspectionRef = ref,
                    holdingRef = holdingRef.trim(),
                    assignedArea = assignedArea.trim(),
                    inspectionDate = inspectionDate.trim(),
                    verificationStatus = verificationStatus,
                    observations = observations.trim(),
                    followUpAction = followUpAction.trim(),
                    notes = notes.trim(),
                    isDraft = isDraft,
                    photoUri = photoUri,
                    latitude = latitude,
                    longitude = longitude,
                    locationLabel = locationLabel,
                    updatedAt = now,
                    createdAt = now
                )
                inspectionRepository.insertNote(newNote)
                showMessage(if (isDraft) "নতুন খসড়া সংরক্ষিত হয়েছে" else "নতুন পরিদর্শন নোট সংরক্ষিত হয়েছে")
            }
            closeInspectionDialog()
        }
    }

    fun deleteInspectionNote(note: InspectionNoteEntity) {
        viewModelScope.launch {
            inspectionRepository.deleteNote(note)
            if (note.photoUri.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    FieldMediaHelper.deletePhotoFileIfInternal(getApplication(), note.photoUri)
                }
            }
            showMessage("পরিদর্শন নোট মুছে ফেলা হয়েছে")
        }
    }

    // --- Report Generator & Gemini AI Summary Actions ---
    fun setReportType(type: ReportType) {
        _selectedReportType.value = type
        _aiSummaryState.value = AiSummaryUiState.Idle
        generateActiveReport()
    }

    fun setReportCustomRemarks(remarks: String) {
        _customReportRemarks.value = remarks
        generateActiveReport()
    }

    fun generateActiveReport() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val report = ReportGeneratorEngine.generateReport(
            type = _selectedReportType.value,
            date = todayStr,
            supervisorName = supervisorName.value,
            wardInfo = wardInfo.value,
            customRemarks = _customReportRemarks.value,
            holdings = holdings.value,
            tasks = tasks.value,
            inspectionNotes = inspectionNotes.value,
            financialSummary = financialSummary.value
        )
        _generatedReport.value = report
    }

    fun generateAiExecutiveSummary() {
        val report = _generatedReport.value ?: run {
            generateActiveReport()
            _generatedReport.value
        } ?: return

        aiSummaryJob?.cancel()
        aiSummaryJob = viewModelScope.launch {
            reportAiRepository.streamExecutiveSummary(
                reportTitle = report.title,
                deterministicReportBody = report.contentBangla
            ).collect { state ->
                _aiSummaryState.value = state
            }
        }
    }

    fun insertAiSummaryIntoRemarks(aiText: String) {
        val existing = _customReportRemarks.value.trim()
        val combined = if (existing.isEmpty()) {
            "AI নির্বাহী সারসংক্ষেপ:\n$aiText"
        } else {
            "$existing\n\nAI নির্বাহী সারসংক্ষেপ:\n$aiText"
        }
        setReportCustomRemarks(combined)
        showMessage("AI সারসংক্ষেপ প্রতিবেদনের মন্তব্যে যোগ করা হয়েছে")
    }

    fun resetAiSummaryState() {
        aiSummaryJob?.cancel()
        _aiSummaryState.value = AiSummaryUiState.Idle
    }
}
