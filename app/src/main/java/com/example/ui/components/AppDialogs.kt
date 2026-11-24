package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.local.entity.HoldingRecordEntity
import com.example.data.local.entity.InspectionNoteEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.analyzer.ColumnMapping
import com.example.domain.analyzer.RevenueAnalysisEngine
import com.example.domain.inspection.FieldMediaHelper
import com.example.domain.sample.SampleDataGenerator
import com.example.ui.theme.AuditFlagColor
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CsvImportDialog(
    isOpen: Boolean,
    initialCsv: String,
    importedFileName: String?,
    currentMapping: ColumnMapping,
    onPickFileClick: () -> Unit,
    onDismiss: () -> Unit,
    onImport: (csvText: String, mapping: ColumnMapping, hasHeader: Boolean) -> Unit
) {
    if (!isOpen) return

    var csvText by remember(initialCsv) { mutableStateOf(initialCsv) }
    var hasHeader by remember { mutableStateOf(true) }
    var showMappingConfig by remember { mutableStateOf(false) }

    var holdingIdCol by remember(currentMapping) { mutableStateOf(currentMapping.holdingIdIndex.toString()) }
    var ownerNameCol by remember(currentMapping) { mutableStateOf(currentMapping.ownerNameIndex.toString()) }
    var wardCol by remember(currentMapping) { mutableStateOf(currentMapping.wardIndex.toString()) }
    var arrearsCol by remember(currentMapping) { mutableStateOf(currentMapping.previousArrearsIndex.toString()) }
    var demandCol by remember(currentMapping) { mutableStateOf(currentMapping.currentDemandIndex.toString()) }
    var payableCol by remember(currentMapping) { mutableStateOf(currentMapping.totalPayableIndex.toString()) }
    var collectedCol by remember(currentMapping) { mutableStateOf(currentMapping.amountCollectedIndex.toString()) }
    var balanceCol by remember(currentMapping) { mutableStateOf(currentMapping.remainingBalanceIndex.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CSV / Excel (.xlsx) ডাটা আমদানি",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (!importedFileName.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = "নির্বাচিত ফাইল: $importedFileName (কলাম স্বয়ংক্রিয়ভাবে চিহ্নিত হয়েছে)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    Text(
                        text = "ডিভাইস থেকে .csv বা .xlsx ফাইল বাছাই করুন অথবা নিচে টেক্সট পেস্ট করুন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onPickFileClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_pick_file_button")
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ডিভাইস থেকে ফাইল বাছুন (.csv / .xlsx)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { csvText = SampleDataGenerator.getSampleCsvRaw() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("load_sample_csv_button")
                    ) {
                        Text("নমুনা CSV", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { showMappingConfig = !showMappingConfig },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (showMappingConfig) "ম্যাপিং লুকান" else "কলাম ম্যাপিং", maxLines = 1)
                    }
                }

                if (showMappingConfig) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "কলাম ইনডেক্স (০ থেকে শুরু):",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = holdingIdCol,
                                    onValueChange = { holdingIdCol = it },
                                    label = { Text("হোল্ডিং ID") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = ownerNameCol,
                                    onValueChange = { ownerNameCol = it },
                                    label = { Text("মালিকের নাম") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = arrearsCol,
                                    onValueChange = { arrearsCol = it },
                                    label = { Text("বকেয়া Col") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = demandCol,
                                    onValueChange = { demandCol = it },
                                    label = { Text("চলতি দাবি Col") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = payableCol,
                                    onValueChange = { payableCol = it },
                                    label = { Text("মোট প্রদেয় Col") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = collectedCol,
                                    onValueChange = { collectedCol = it },
                                    label = { Text("আদায়কৃত Col") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = wardCol,
                                    onValueChange = { wardCol = it },
                                    label = { Text("ওয়ার্ড Col") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = balanceCol,
                                    onValueChange = { balanceCol = it },
                                    label = { Text("ব্যালেন্স Col") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = csvText,
                    onValueChange = { csvText = it },
                    label = { Text("স্প্রেডশিট ডাটা প্রিভিউ") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .testTag("csv_input_field"),
                    maxLines = 10
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = hasHeader,
                        onCheckedChange = { hasHeader = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "প্রথম সারিতে কলামের শিরোনাম (Header) রয়েছে",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("বাতিল")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val mapping = ColumnMapping(
                                holdingIdIndex = (holdingIdCol.toIntOrNull() ?: 0).coerceIn(0, 255),
                                ownerNameIndex = (ownerNameCol.toIntOrNull() ?: 1).coerceIn(0, 255),
                                wardIndex = (wardCol.toIntOrNull() ?: 2).coerceIn(0, 255),
                                previousArrearsIndex = (arrearsCol.toIntOrNull() ?: 3).coerceIn(0, 255),
                                currentDemandIndex = (demandCol.toIntOrNull() ?: 4).coerceIn(0, 255),
                                totalPayableIndex = (payableCol.toIntOrNull() ?: 5).coerceIn(0, 255),
                                amountCollectedIndex = (collectedCol.toIntOrNull() ?: 6).coerceIn(0, 255),
                                remainingBalanceIndex = (balanceCol.toIntOrNull() ?: 7).coerceIn(0, 255)
                            )
                            onImport(csvText, mapping, hasHeader)
                        },
                        modifier = Modifier.testTag("confirm_csv_import_button")
                    ) {
                        Text("যাচাই ও আমদানি")
                    }
                }
            }
        }
    }
}

@Composable
fun SourceRowInspectorDialog(
    holding: HoldingRecordEntity?,
    onDismiss: () -> Unit
) {
    if (holding == null) return

    val calculatedTotalDemand = holding.previousArrears + holding.currentDemand
    val calculatedRemainingBalance = holding.totalPayable - holding.amountCollected

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "উৎস ডাটা ও নিরীক্ষা খতিয়ান",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "হোল্ডিং: ${holding.holdingId}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "মালিক: ${holding.ownerName} | ${holding.ward}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "স্প্রেডশিট উৎস সারি নং: #${holding.sourceRowNumber}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (holding.hasCalculationMismatch || holding.hasMissingValues || holding.isDuplicate) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = AuditFlagColor.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = AuditFlagColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "নিরীক্ষা অসঙ্গতি সতর্কতা",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AuditFlagColor
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = holding.mismatchNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "* মন্তব্য: এটি কেবল গাণিতিক বা ডেটা এন্ট্রি সংক্রান্ত অসঙ্গতি। খতিয়ান রেজিস্টার দেখে মিলিয়ে নেয়ার পরামর্শ দেওয়া হচ্ছে।",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = "গাণিতিক হিসাব তুলনা:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                InspectorRow("পূর্বের বকেয়া", RevenueAnalysisEngine.formatTaka(holding.previousArrears))
                InspectorRow("চলতি অর্থবছরের দাবি", RevenueAnalysisEngine.formatTaka(holding.currentDemand))
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                InspectorRow(
                    "উৎস নথিতে মোট দাবি",
                    RevenueAnalysisEngine.formatTaka(holding.totalPayable),
                    isMismatch = holding.hasCalculationMismatch
                )
                InspectorRow(
                    "গাণিতিক যোগফল (বকেয়া+চলতি)",
                    RevenueAnalysisEngine.formatTaka(calculatedTotalDemand),
                    highlight = true
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                InspectorRow("আদায়কৃত টাকা", RevenueAnalysisEngine.formatTaka(holding.amountCollected))
                InspectorRow("অবশিষ্ট বকেয়া", RevenueAnalysisEngine.formatTaka(holding.remainingBalance))
                InspectorRow(
                    "গাণিতিক অবশিষ্ট (মোট-আদায়)",
                    RevenueAnalysisEngine.formatTaka(calculatedRemainingBalance),
                    highlight = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "মূল স্প্রেডশিট কাঁচা ডাটা (Raw Row):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = holding.rawCsvRow.ifEmpty { "(উৎস সারি ডাটা উপলব্ধ নেই)" },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("ঠিক আছে")
            }
        }
    )
}

@Composable
private fun InspectorRow(
    label: String,
    value: String,
    isMismatch: Boolean = false,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isMismatch) AuditFlagColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (highlight || isMismatch) FontWeight.Bold else FontWeight.Normal,
                color = if (isMismatch) AuditFlagColor else if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
fun TaskEditorDialog(
    isOpen: Boolean,
    taskToEdit: TaskEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String, holdingId: String, dueDate: String, priority: String, category: String, notes: String) -> Unit
) {
    if (!isOpen) return

    val defaultToday = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var title by remember(taskToEdit) { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember(taskToEdit) { mutableStateOf(taskToEdit?.description ?: "") }
    var holdingId by remember(taskToEdit) { mutableStateOf(taskToEdit?.holdingId ?: "") }
    var dueDate by remember(taskToEdit) { mutableStateOf(taskToEdit?.dueDate ?: defaultToday) }
    var priority by remember(taskToEdit) { mutableStateOf(taskToEdit?.priority ?: "MEDIUM") }
    var category by remember(taskToEdit) { mutableStateOf(taskToEdit?.category ?: "INSPECTION") }
    var notes by remember(taskToEdit) { mutableStateOf(taskToEdit?.notes ?: "") }

    val priorities = listOf("HIGH" to "জরুরি", "MEDIUM" to "মাঝারি", "LOW" to "সাধারণ")
    val categories = listOf(
        "INSPECTION" to "মাঠ পরিদর্শন",
        "DEMAND_NOTICE" to "দাবি নোটিশ",
        "HEARING" to "শুনানি বৈঠক"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (taskToEdit == null) "নতুন টাস্ক তৈরি করুন" else "টাস্ক সম্পাদনা",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("টাস্কের শিরোনাম *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = holdingId,
                        onValueChange = { holdingId = it },
                        label = { Text("হোল্ডিং নং") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("তারিখ (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("অগ্রাধিকার:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    priorities.forEach { (key, label) ->
                        val isSelected = priority == key
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { priority = key },
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                Text("ক্যাটাগরি:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { (catKey, catLabel) ->
                        val isSelected = category == catKey
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { category = catKey },
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = catLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("বিস্তারিত বিবরণ") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("নোট / মন্তব্য") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(title, description, holdingId, dueDate, priority, category, notes)
                },
                modifier = Modifier.testTag("save_task_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun InspectionNoteEditorDialog(
    isOpen: Boolean,
    noteToEdit: InspectionNoteEntity?,
    onDismiss: () -> Unit,
    onSave: (
        ref: String,
        holding: String,
        area: String,
        date: String,
        status: String,
        obs: String,
        action: String,
        notes: String,
        isDraft: Boolean,
        photoUri: String,
        latitude: Double?,
        longitude: Double?,
        locationLabel: String
    ) -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val defaultToday = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var ref by remember(noteToEdit) { mutableStateOf(noteToEdit?.inspectionRef ?: "") }
    var holding by remember(noteToEdit) { mutableStateOf(noteToEdit?.holdingRef ?: "") }
    var area by remember(noteToEdit) { mutableStateOf(noteToEdit?.assignedArea ?: "ওয়ার্ড-০৭, মিরপুর") }
    var date by remember(noteToEdit) { mutableStateOf(noteToEdit?.inspectionDate ?: defaultToday) }
    var status by remember(noteToEdit) { mutableStateOf(noteToEdit?.verificationStatus ?: "VERIFIED") }
    var observations by remember(noteToEdit) { mutableStateOf(noteToEdit?.observations ?: "") }
    var followUpAction by remember(noteToEdit) { mutableStateOf(noteToEdit?.followUpAction ?: "") }
    var notes by remember(noteToEdit) { mutableStateOf(noteToEdit?.notes ?: "") }
    var isDraft by remember(noteToEdit) { mutableStateOf(noteToEdit?.isDraft ?: false) }

    var photoUri by remember(noteToEdit) { mutableStateOf(noteToEdit?.photoUri ?: "") }
    var latitude by remember(noteToEdit) { mutableStateOf(noteToEdit?.latitude) }
    var longitude by remember(noteToEdit) { mutableStateOf(noteToEdit?.longitude) }
    var locationLabel by remember(noteToEdit) { mutableStateOf(noteToEdit?.locationLabel ?: "") }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var locationStatusText by remember { mutableStateOf<String?>(null) }

    // Android Photo Picker (Zero-permission per Play Policy)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = FieldMediaHelper.copyPhotoToInternalStorage(context, uri)
            if (savedPath != null) {
                photoUri = savedPath
            }
        }
    }

    val fetchLocationAction = {
        isFetchingLocation = true
        locationStatusText = null
        scope.launch {
            val loc = FieldMediaHelper.getCurrentLocation(context)
            isFetchingLocation = false
            if (loc != null) {
                latitude = loc.latitude
                longitude = loc.longitude
                locationLabel = "%.5f, %.5f".format(loc.latitude, loc.longitude)
                locationStatusText = "লোকেশন সংযুক্ত হয়েছে"
            } else {
                locationStatusText = "বর্তমান জিপিএস অবস্থান পাওয়া যায়নি; ডিভাইসের লোকেশন চালু আছে কিনা দেখুন।"
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchLocationAction()
        } else {
            locationStatusText = "লোকেশন অনুমতি দেওয়া হয়নি — কেবল ম্যানুয়াল এলাকা সংরক্ষণ হবে।"
        }
    }

    val statusOptions = listOf(
        "VERIFIED" to "যাচাইকৃত ও সঠিক",
        "MISMATCH_MEASUREMENT" to "পরিমাপে অমিল",
        "UNAUTHORIZED_COMMERCIAL" to "অননুমোদিত বাণিজ্যিক ব্যবহার",
        "EXTENSION_FOUND" to "অতিরিক্ত তলা / স্থাপনা দৃশ্যমান",
        "INCOMPLETE" to "অসম্পূর্ণ পরিদর্শন"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NoteAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (noteToEdit == null) "নতুন মাঠ পরিদর্শন নোট" else "পরিদর্শন নোট সম্পাদনা",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = holding,
                        onValueChange = { holding = it },
                        label = { Text("হোল্ডিং নং *") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inspection_holding_input")
                    )
                    OutlinedTextField(
                        value = ref,
                        onValueChange = { ref = it },
                        label = { Text("রেফারেন্স নং") },
                        placeholder = { Text("INSP-XXXX") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = area,
                        onValueChange = { area = it },
                        label = { Text("ওয়ার্ড / এলাকা") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("তারিখ (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Photo & Location Attachment Row
                Text("মাঠ প্রমাণ (ছবি ও অবস্থান ট্যাগ):", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("attach_photo_button")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (photoUri.isEmpty()) "ছবি যুক্ত" else "ছবি বদলান", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            val hasFine = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasFine || hasCoarse) {
                                fetchLocationAction()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            }
                        },
                        enabled = !isFetchingLocation,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("attach_location_button")
                    ) {
                        if (isFetchingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("লোকেশন ট্যাগ", maxLines = 1)
                    }
                }

                if (photoUri.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "পরিদর্শনের সংযুক্ত ছবি",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            IconButton(
                                onClick = { photoUri = "" },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(50))
                                    .size(48.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "ছবি মুছুন", tint = StatusDanger, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                if (locationLabel.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusSuccess.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "জিপিএস স্থানাঙ্ক: $locationLabel",
                                style = MaterialTheme.typography.labelSmall.copy(color = StatusSuccess, fontWeight = FontWeight.Bold)
                            )
                            IconButton(
                                onClick = {
                                    latitude = null
                                    longitude = null
                                    locationLabel = ""
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "মুছুন", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                if (locationStatusText != null) {
                    Text(
                        text = locationStatusText!!,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text("যাচাইকৃত অবস্থা (Verification Status):", style = MaterialTheme.typography.labelSmall)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    statusOptions.forEach { (key, label) ->
                        val isSelected = status == key
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            onClick = { status = key },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = observations,
                    onValueChange = { observations = it },
                    label = { Text("মাঠ পর্যায়ের পর্যবেক্ষণ *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inspection_observations_input"),
                    minLines = 3,
                    maxLines = 5
                )

                OutlinedTextField(
                    value = followUpAction,
                    onValueChange = { followUpAction = it },
                    label = { Text("প্রস্তাবিত পরবর্তী পদক্ষেপ") },
                    placeholder = { Text("যেমন: শুনানি নোটিশ প্রদান বা পুনর্পরিমাপ") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("অতিরিক্ত মন্তব্য / নোট") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDraft) "খসড়া হিসেবে চিহ্নিত (Draft)" else "চূড়ান্ত প্রস্তুত (Final)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDraft) StatusWarning else StatusSuccess
                        )
                    )
                    Switch(
                        checked = isDraft,
                        onCheckedChange = { isDraft = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        ref, holding, area, date, status, observations, followUpAction,
                        notes, isDraft, photoUri, latitude, longitude, locationLabel
                    )
                },
                modifier = Modifier.testTag("save_inspection_button")
            ) {
                Text(if (isDraft) "খসড়া সংরক্ষণ" else "সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun EndOfDaySummaryDialog(
    isOpen: Boolean,
    tasks: List<TaskEntity>,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val completed = tasks.filter { it.status == "COMPLETED" }
    val pending = tasks.filter { it.status != "COMPLETED" }
    val completionRate = if (tasks.isNotEmpty()) (completed.size.toDouble() / tasks.size * 100) else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "আজকের কাজের সারসংক্ষেপ",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("মোট নির্ধারিত কাজ:", style = MaterialTheme.typography.bodyMedium)
                            Text("${tasks.size} টি", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("সম্পন্ন হয়েছে:", style = MaterialTheme.typography.bodyMedium)
                            Text("${completed.size} টি", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = StatusSuccess))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("অবশিষ্ট / মুলতুবি:", style = MaterialTheme.typography.bodyMedium)
                            Text("${pending.size} টি", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = StatusDanger))
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("দৈনিক সাফল্য হার:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("%.1f%%".format(completionRate), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                        }
                    }
                }

                if (pending.isNotEmpty()) {
                    Text(
                        text = "আগামীকালের জন্য স্থগিত কাজ:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = StatusDanger)
                    )
                    pending.forEach { t ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "• [${t.priority}] ${t.title}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StatusSuccess.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "অভিনন্দন! আজকের সকল অর্পিত কার্যক্রম সফলভাবে সম্পন্ন হয়েছে।",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = StatusSuccess),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("বন্ধ করুন")
            }
        }
    )
}
