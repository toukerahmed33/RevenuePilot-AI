package com.example.ui.reports

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiSummaryUiState
import com.example.domain.report.GeneratedReport
import com.example.domain.report.PdfReportExporter
import com.example.domain.report.ReportType
import com.example.ui.components.MunicipalDisclaimerBanner
import com.example.ui.theme.StatusDanger

@Composable
fun ReportsScreen(
    generatedReport: GeneratedReport?,
    selectedReportType: ReportType,
    customRemarks: String,
    aiSummaryState: AiSummaryUiState,
    onSelectReportType: (ReportType) -> Unit,
    onCustomRemarksChange: (String) -> Unit,
    onGenerateReport: () -> Unit,
    onRequestAiSummary: () -> Unit,
    onInsertAiSummary: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var showAiConsentDialog by remember { mutableStateOf(false) }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri: Uri? ->
        if (targetUri != null && generatedReport != null) {
            try {
                PdfReportExporter.writePdfToUri(context, generatedReport, targetUri)
                onShowMessage("PDF প্রতিবেদন সফলভাবে সংরক্ষিত হয়েছে")
            } catch (e: Exception) {
                onShowMessage("PDF সংরক্ষণে ত্রুটি: ${e.localizedMessage}")
            }
        }
    }

    LaunchedEffect(Unit) {
        onGenerateReport()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            MunicipalDisclaimerBanner()
        }

        // Report Type Selector
        item {
            Text(
                text = "প্রতিবেদনের ধরণ:",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ReportType.entries.forEach { rType ->
                    val isSelected = selectedReportType == rType
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        onClick = { onSelectReportType(rType) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = rType.titleBn,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = rType.subtitleBn,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Gemini AI Executive Summary Assistance Card (Explicit User Trigger Only)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemini AI নির্বাহী সারসংক্ষেপ",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ডিটারমিনিস্টিক প্রতিবেদনের তথ্য থেকে স্বয়ংক্রিয় বাংলা সারসংক্ষেপ তৈরি করুন (কেবল আপনার অনুমতিতে)।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    when (aiSummaryState) {
                        is AiSummaryUiState.Idle -> {
                            Button(
                                onClick = { showAiConsentDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_summary_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI সারসংক্ষেপ তৈরি করুন")
                            }
                        }
                        is AiSummaryUiState.Loading -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Gemini AI সারসংক্ষেপ প্রস্তুত করছে...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        is AiSummaryUiState.Streaming -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = aiSummaryState.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        is AiSummaryUiState.Success -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = aiSummaryState.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onInsertAiSummary(aiSummaryState.text) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("insert_ai_summary_button")
                                ) {
                                    Text("মন্তব্যে যোগ করুন")
                                }
                                OutlinedButton(
                                    onClick = { showAiConsentDialog = true },
                                    modifier = Modifier.weight(0.7f)
                                ) {
                                    Text("পুনরায় তৈরি")
                                }
                            }
                        }
                        is AiSummaryUiState.Error -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StatusDanger.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = aiSummaryState.message,
                                    style = MaterialTheme.typography.bodySmall.copy(color = StatusDanger),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showAiConsentDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("আবার চেষ্টা করুন")
                            }
                        }
                    }
                }
            }
        }

        // Editable Remarks Before Export
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "রপ্তানির পূর্বে মন্তব্য সম্পাদনা:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customRemarks,
                        onValueChange = onCustomRemarksChange,
                        placeholder = { Text("এখানে আপনার পর্যবেক্ষণ বা দাপ্তরিক সুপারিশ লিখুন...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_remarks_input"),
                        minLines = 2,
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onGenerateReport,
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("refresh_report_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("প্রিভিউ আপডেট")
                    }
                }
            }
        }

        // Report Export Actions & Preview
        if (generatedReport != null) {
            item {
                Text(
                    text = "প্রতিবেদন রপ্তানি (PDF / CSV):",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val pdfUri = PdfReportExporter.generatePdfToCache(context, generatedReport)
                                sharePdf(context, pdfUri, generatedReport.title, onShowMessage)
                            } catch (e: Exception) {
                                onShowMessage("PDF তৈরিতে ত্রুটি: ${e.localizedMessage}")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF শেয়ার", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            val fileName = "RevenuePilot_${generatedReport.type.name}_${generatedReport.date}.pdf"
                            savePdfLauncher.launch(fileName)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_pdf_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF সেভ", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            shareText(context, generatedReport.exportableCsv, "revenue_data.csv", onShowMessage)
                        },
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("export_csv_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CSV", maxLines = 1)
                    }
                }
            }

            if (generatedReport.keyMetrics.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        generatedReport.keyMetrics.entries.take(4).forEach { (k, v) ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = k,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = v,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
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
                                text = "বাংলা প্রতিবেদন প্রিভিউ",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            TextButton(onClick = { shareText(context, generatedReport.contentBangla, generatedReport.title, onShowMessage) }) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("টেক্সট শেয়ার")
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = generatedReport.contentBangla,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    if (showAiConsentDialog) {
        AlertDialog(
            onDismissRequest = { showAiConsentDialog = false },
            title = {
                Text(
                    text = "AI সারসংক্ষেপ অনুমতি নিশ্চিতকরণ",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "আপনার অনুমতি সাপেক্ষে কেবল বর্তমানে নির্বাচিত প্রতিবেদনের সারাংশ Firebase AI Logic (Gemini)-তে পাঠানো হবে যাতে একটি সংক্ষিপ্ত বাংলা নির্বাহী সারসংক্ষেপ তৈরি করা যায়। প্রকৃত নাগরিক তথ্য না পাঠাতে সর্বদা সতর্ক থাকুন।",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAiConsentDialog = false
                        onRequestAiSummary()
                    },
                    modifier = Modifier.testTag("confirm_ai_summary_button")
                ) {
                    Text("নিশ্চিত ও তৈরি করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiConsentDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

private fun sharePdf(context: Context, pdfUri: Uri, title: String, onShowMessage: (String) -> Unit) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, pdfUri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(shareIntent, "PDF প্রতিবেদন শেয়ার বা প্রিন্ট করুন"))
    } catch (_: ActivityNotFoundException) {
        onShowMessage("PDF শেয়ার করার জন্য কোনো উপযুক্ত অ্যাপ পাওয়া যায়নি")
    }
}

private fun shareText(context: Context, text: String, title: String, onShowMessage: (String) -> Unit) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_TITLE, title)
        type = "text/plain"
    }
    try {
        context.startActivity(Intent.createChooser(sendIntent, "প্রতিবেদন শেয়ার বা সংরক্ষণ করুন"))
    } catch (_: ActivityNotFoundException) {
        onShowMessage("প্রতিবেদন শেয়ার করার জন্য কোনো উপযুক্ত অ্যাপ পাওয়া যায়নি")
    }
}
