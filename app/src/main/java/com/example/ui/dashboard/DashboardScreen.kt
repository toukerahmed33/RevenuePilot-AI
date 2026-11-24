package com.example.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.HoldingRecordEntity
import com.example.data.local.entity.InspectionNoteEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.analyzer.FinancialSummary
import com.example.domain.analyzer.RevenueAnalysisEngine
import com.example.ui.AppScreen
import com.example.ui.WorkspaceTab
import com.example.ui.components.AuditAlertBanner
import com.example.ui.components.MunicipalDisclaimerBanner
import com.example.ui.components.PriorityBadge
import com.example.ui.components.RevenueDonutChart
import com.example.ui.components.VerificationStatusBadge
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun DashboardScreen(
    supervisorName: String,
    wardInfo: String,
    tasks: List<TaskEntity>,
    holdings: List<HoldingRecordEntity>,
    inspectionNotes: List<InspectionNoteEntity>,
    financialSummary: FinancialSummary,
    onNavigate: (AppScreen, WorkspaceTab?) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onOpenTaskDialog: (TaskEntity?) -> Unit,
    onOpenInspectionDialog: (InspectionNoteEntity?) -> Unit,
    onPickSpreadsheetClick: () -> Unit,
    onLoadSampleData: () -> Unit,
    onOpenEndOfDaySummary: () -> Unit
) {
    val completedTasks = tasks.filter { it.status == "COMPLETED" }
    val pendingTasks = tasks.filter { it.status != "COMPLETED" }
    val completionPercentage = if (tasks.isNotEmpty()) {
        (completedTasks.size.toFloat() / tasks.size.toFloat())
    } else 0f

    val flaggedHoldingsCount = financialSummary.anomalySummary.totalFlaggedRecords

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            MunicipalDisclaimerBanner()
        }

        // Minimalist Supervisor Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = supervisorName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            Text(
                                text = "$wardInfo • রাজস্ব সুপারভাইজার",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                )
                            )
                        }
                    }
                }
            }
        }

        if (flaggedHoldingsCount > 0) {
            item {
                AuditAlertBanner(
                    mismatchesCount = financialSummary.anomalySummary.calculationMismatchesCount,
                    duplicatesCount = financialSummary.anomalySummary.duplicateIdsCount,
                    missingCount = financialSummary.anomalySummary.missingValuesCount,
                    onClick = { onNavigate(AppScreen.ANALYZER, null) }
                )
            }
        }

        // Visual Revenue Chart + KPI Summary Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "রাজস্ব আদায় ও বকেয়া চিত্র",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "মোট ${holdings.size} টি হোল্ডিংয়ের হিসাব",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onNavigate(AppScreen.ANALYZER, null) }) {
                            Text("বিস্তারিত")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (holdings.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "কোনো তথ্য পাওয়া যায়নি (No data available)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = onLoadSampleData,
                                    modifier = Modifier.testTag("load_sample_data_empty_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("নমুনা ডাটা লোড করুন")
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RevenueDonutChart(
                                collected = financialSummary.totalCollected,
                                remaining = financialSummary.totalRemainingBalance,
                                collectionPercentage = financialSummary.collectionRatePercentage
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ChartLegendRow(
                                    label = "মোট দাবি",
                                    amount = RevenueAnalysisEngine.formatTaka(financialSummary.totalDemand),
                                    dotColor = MaterialTheme.colorScheme.primary
                                )
                                ChartLegendRow(
                                    label = "সংগৃহীত আদায়",
                                    amount = RevenueAnalysisEngine.formatTaka(financialSummary.totalCollected),
                                    dotColor = StatusSuccess
                                )
                                ChartLegendRow(
                                    label = "অবশিষ্ট বকেয়া",
                                    amount = RevenueAnalysisEngine.formatTaka(financialSummary.totalRemainingBalance),
                                    dotColor = StatusDanger
                                )
                            }
                        }
                    }
                }
            }
        }

        // Daily Tasks Progress Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "আজকের অর্পিত কার্যক্রম",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        TextButton(onClick = onOpenEndOfDaySummary) {
                            Text("সারসংক্ষেপ")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TaskKpiColumn("মোট টাস্ক", "${tasks.size} টি", MaterialTheme.colorScheme.primary)
                        TaskKpiColumn("সম্পন্ন", "${completedTasks.size} টি", StatusSuccess)
                        TaskKpiColumn("মুলতুবি", "${pendingTasks.size} টি", StatusDanger)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { completionPercentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = StatusSuccess,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "দৈনিক অগ্রগতি: ${(completionPercentage * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Quick Actions Row (48dp+ touch targets)
        item {
            Text(
                text = "দ্রুত পদক্ষেপ",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.Add,
                    label = "নতুন টাস্ক",
                    onClick = { onOpenTaskDialog(null) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Default.NoteAlt,
                    label = "পরিদর্শন",
                    onClick = { onOpenInspectionDialog(null) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Default.FileUpload,
                    label = "ফাইল ইমপোর্ট",
                    onClick = onPickSpreadsheetClick,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Default.Description,
                    label = "রিপোর্ট",
                    onClick = { onNavigate(AppScreen.REPORTS, null) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Pending Tasks Preview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "মুলতুবি কাজসমূহ",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = { onNavigate(AppScreen.WORKSPACE, WorkspaceTab.TASKS) }) {
                    Text("সকল (${tasks.size})")
                }
            }
        }

        if (pendingTasks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "কোনো মুলতুবি কাজ নেই। সব কাজ সম্পন্ন হয়েছে।",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(pendingTasks.take(3)) { task ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onToggleTask(task) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (task.status == "COMPLETED") Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = "Toggle Complete",
                                tint = if (task.status == "COMPLETED") StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            if (task.holdingId.isNotEmpty()) {
                                Text(
                                    text = "হোল্ডিং: ${task.holdingId}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        PriorityBadge(task.priority)
                    }
                }
            }
        }

        // Recent Inspection Notes Preview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "সাম্প্রতিক মাঠ পরিদর্শন",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = { onNavigate(AppScreen.WORKSPACE, WorkspaceTab.INSPECTIONS) }) {
                    Text("সকল (${inspectionNotes.size})")
                }
            }
        }

        if (inspectionNotes.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "কোনো পরিদর্শন নোট সংরক্ষিত নেই।",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(inspectionNotes.take(2)) { note ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "হোল্ডিং: ${note.holdingRef}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            VerificationStatusBadge(note.verificationStatus)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = note.observations,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (note.isDraft) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "[খসড়া নোট]",
                                style = MaterialTheme.typography.labelSmall.copy(color = StatusWarning, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendRow(
    label: String,
    amount: String,
    dotColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = amount,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun TaskKpiColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
        }
    }
}
