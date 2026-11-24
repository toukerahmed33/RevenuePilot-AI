package com.example.ui.analyzer

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.HoldingRecordEntity
import com.example.domain.analyzer.FinancialSummary
import com.example.domain.analyzer.RevenueAnalysisEngine
import com.example.ui.components.MunicipalDisclaimerBanner
import com.example.ui.theme.AuditFlagColor
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

enum class HoldingFilter {
    ALL,
    INCONSISTENT_ONLY,
    DEFICIT_ONLY,
    CLEARED_ONLY
}

@Composable
fun RevenueAnalyzerScreen(
    holdings: List<HoldingRecordEntity>,
    financialSummary: FinancialSummary,
    isParsingFile: Boolean,
    onPickFileClick: () -> Unit,
    onOpenManualImportDialog: () -> Unit,
    onSelectHoldingToInspect: (HoldingRecordEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(HoldingFilter.ALL) }

    val filteredHoldings = remember(holdings, searchQuery, selectedFilter) {
        holdings.filter { h ->
            val matchesSearch = searchQuery.isBlank() ||
                h.holdingId.contains(searchQuery, ignoreCase = true) ||
                h.ownerName.contains(searchQuery, ignoreCase = true) ||
                h.ward.contains(searchQuery, ignoreCase = true) ||
                h.area.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                HoldingFilter.ALL -> true
                HoldingFilter.INCONSISTENT_ONLY -> h.hasCalculationMismatch || h.hasMissingValues || h.isDuplicate
                HoldingFilter.DEFICIT_ONLY -> h.remainingBalance > 0.0
                HoldingFilter.CLEARED_ONLY -> h.remainingBalance <= 0.0
            }

            matchesSearch && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("revenue_analyzer_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            MunicipalDisclaimerBanner()
        }

        // File Picker & Import CTA Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "রেভিনিউ অ্যানালাইজার",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "ডিভাইস থেকে .csv বা .xlsx Excel ফাইল নিয়ে ডিটারমিনিস্টিক গাণিতিক বিশ্লেষণ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPickFileClick,
                            enabled = !isParsingFile,
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("pick_spreadsheet_button")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ফাইল নির্বাচন (CSV/Excel)")
                        }
                        OutlinedButton(
                            onClick = onOpenManualImportDialog,
                            enabled = !isParsingFile,
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("import_csv_button")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("পেস্ট / নমুনা")
                        }
                    }
                    if (isParsingFile) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // Financial Aggregates Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "গাণিতিক সারসংক্ষেপ (মোট ${holdings.size} টি হোল্ডিং)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem("মোট দাবি", RevenueAnalysisEngine.formatTaka(financialSummary.totalDemand), MaterialTheme.colorScheme.primary)
                        MetricItem("মোট আদায়", RevenueAnalysisEngine.formatTaka(financialSummary.totalCollected), StatusSuccess)
                        MetricItem("অবশিষ্ট বকেয়া", RevenueAnalysisEngine.formatTaka(financialSummary.totalRemainingBalance), StatusDanger)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem("আদায় হার", "%.1f%%".format(financialSummary.collectionRatePercentage), MaterialTheme.colorScheme.secondary)
                        MetricItem("গাণিতিক অমিল", "${financialSummary.anomalySummary.calculationMismatchesCount} টি", AuditFlagColor)
                        MetricItem("দ্বৈত / অসম্পূর্ণ", "${financialSummary.anomalySummary.duplicateIdsCount + financialSummary.anomalySummary.missingValuesCount} টি", StatusWarning)
                    }
                }
            }
        }

        if (financialSummary.anomalySummary.totalFlaggedRecords > 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = AuditFlagColor.copy(alpha = 0.06f)
                    ),
                    border = BorderStroke(1.dp, AuditFlagColor.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = AuditFlagColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "পেশাদার নিরীক্ষা নোটিশ: ${financialSummary.anomalySummary.totalFlaggedRecords}টি সারিতে গাণিতিক বা তথ্য অমিল পাওয়া গেছে। এটি কেবল ডাটা এন্ট্রি ত্রুটি হতে পারে; যেকোনো সারিতে ট্যাপ করে উৎস ডাটা দেখুন।",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Search & Filter Chips
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("হোল্ডিং নং বা মালিকের নাম খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("analyzer_search_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == HoldingFilter.ALL,
                        onClick = { selectedFilter = HoldingFilter.ALL },
                        label = { Text("সকল (${holdings.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == HoldingFilter.INCONSISTENT_ONLY,
                        onClick = { selectedFilter = HoldingFilter.INCONSISTENT_ONLY },
                        label = { Text("অডিট অমিল (${financialSummary.anomalySummary.totalFlaggedRecords})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == HoldingFilter.DEFICIT_ONLY,
                        onClick = { selectedFilter = HoldingFilter.DEFICIT_ONLY },
                        label = { Text("বকেয়া আছে") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == HoldingFilter.CLEARED_ONLY,
                        onClick = { selectedFilter = HoldingFilter.CLEARED_ONLY },
                        label = { Text("পরিশোধিত") }
                    )
                }
            }
        }

        item {
            Text(
                text = "হোল্ডিং তালিকা (${filteredHoldings.size} টি):",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (filteredHoldings.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "কোনো রেকর্ড পাওয়া যায়নি। অনুসন্ধান বা ফিল্টার পরিবর্তন করুন।",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(filteredHoldings, key = { it.id }) { holding ->
                HoldingItemCard(
                    holding = holding,
                    onClick = { onSelectHoldingToInspect(holding) }
                )
            }
        }
    }
}

@Composable
private fun HoldingItemCard(
    holding: HoldingRecordEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = holding.holdingId,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "সারি #${holding.sourceRowNumber}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (holding.remainingBalance <= 0.0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StatusSuccess.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "পরিশোধিত",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = StatusSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StatusDanger.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "বকেয়া: ${RevenueAnalysisEngine.formatTaka(holding.remainingBalance)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = StatusDanger,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "মালিক: ${holding.ownerName.ifEmpty { "(নাম নেই)" }} | ${holding.ward}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (holding.hasCalculationMismatch || holding.hasMissingValues || holding.isDuplicate) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AuditFlagColor.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = AuditFlagColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = holding.mismatchNotes,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AuditFlagColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FinancialMiniColumn("পূর্বের বকেয়া", RevenueAnalysisEngine.formatTaka(holding.previousArrears))
                FinancialMiniColumn("চলতি দাবি", RevenueAnalysisEngine.formatTaka(holding.currentDemand))
                FinancialMiniColumn("মোট দাবি", RevenueAnalysisEngine.formatTaka(holding.totalPayable))
                FinancialMiniColumn("আদায়", RevenueAnalysisEngine.formatTaka(holding.amountCollected), color = StatusSuccess)
            }
        }
    }
}

@Composable
private fun FinancialMiniColumn(label: String, value: String, color: Color = Color.Unspecified) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun MetricItem(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
    }
}
