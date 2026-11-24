package com.example.domain.report

import com.example.data.local.entity.HoldingRecordEntity
import com.example.data.local.entity.InspectionNoteEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.analyzer.FinancialSummary
import com.example.domain.analyzer.RevenueAnalysisEngine

enum class ReportType(val titleBn: String, val subtitleBn: String) {
    DAILY_ACTIVITY("দৈনিক কার্যক্রম প্রতিবেদন", "আজকের সম্পাদিত কাজ ও মাঠ পরিদর্শন নোটের বিবরণ"),
    REVENUE_SUMMARY("রাজস্ব আদায় ও সারসংক্ষেপ প্রতিবেদন", "ওয়ার্ডভিত্তিক মোট দাবি, আদায় ও অবশিষ্ট বকেয়া বিশ্লেষণ"),
    ARREARS_AUDIT("বকেয়া ও অডিট অমিল প্রতিবেদন", "গাণিতিক অমিল, দ্বৈত হোল্ডিং ও অসম্পূর্ণ তথ্যের নিরীক্ষা তালিকা"),
    FIELD_INSPECTION("মাঠ পরিদর্শন ও সত্যতা যাচাই প্রতিবেদন", "মাঠ পরিদর্শনের ফলাফল, পর্যবেক্ষণ ও প্রস্তাবিত পরবর্তী পদক্ষেপ")
}

data class GeneratedReport(
    val type: ReportType,
    val title: String,
    val date: String,
    val supervisorName: String,
    val wardInfo: String,
    val contentBangla: String,
    val exportableCsv: String,
    val keyMetrics: Map<String, String>
)

object ReportGeneratorEngine {

    fun generateReport(
        type: ReportType,
        date: String,
        supervisorName: String,
        wardInfo: String,
        customRemarks: String,
        holdings: List<HoldingRecordEntity>,
        tasks: List<TaskEntity>,
        inspectionNotes: List<InspectionNoteEntity>,
        financialSummary: FinancialSummary
    ): GeneratedReport {
        val disclaimerHeader = """
            ===================================================================
            রেভিনিউ পাইলট (RevenuePilot AI) — পৌর রাজস্ব ব্যবস্থাপনা সহায়ক
            * স্বাধীন ব্যক্তিগত উৎপাদনশীলতা প্রতিবেদন (দাপ্তরিক নথির বিকল্প নয়) *
            ===================================================================
            প্রতিবেদন প্রকার : ${type.titleBn}
            তারিখ : $date | কর্মকর্তা : $supervisorName | এলাকা : $wardInfo
            -------------------------------------------------------------------
        """.trimIndent()

        val sb = StringBuilder()
        sb.appendLine(disclaimerHeader)
        sb.appendLine()

        var exportableCsv = ""
        val metrics = mutableMapOf<String, String>()

        when (type) {
            ReportType.DAILY_ACTIVITY -> {
                val completedToday = tasks.filter { it.status == "COMPLETED" }
                val pendingTasks = tasks.filter { it.status != "COMPLETED" }
                val todayInspections = inspectionNotes.filter { it.inspectionDate == date || !it.isDraft }

                metrics["মোট টাস্ক"] = tasks.size.toString()
                metrics["সম্পন্ন টাস্ক"] = completedToday.size.toString()
                metrics["মুলতুবি টাস্ক"] = pendingTasks.size.toString()
                metrics["পরিদর্শন সংখ্যা"] = todayInspections.size.toString()

                sb.appendLine("১. দৈনিক কর্মসম্পাদনের সারসংক্ষেপ:")
                sb.appendLine("   - মোট নির্ধারিত টাস্ক : ${tasks.size} টি")
                sb.appendLine("   - সফলভাবে সম্পন্ন : ${completedToday.size} টি")
                sb.appendLine("   - চলমান বা অপেক্ষমাণ : ${pendingTasks.size} টি")
                sb.appendLine("   - মাঠ পরিদর্শন সম্পন্ন : ${todayInspections.size} টি")
                sb.appendLine()

                sb.appendLine("২. সম্পাদিত গুরুত্বপূর্ণ কার্যক্রম:")
                if (completedToday.isEmpty()) {
                    sb.appendLine("   - (আজ কোনো টাস্ক সম্পন্ন হিসেবে চিহ্নিত হয়নি)")
                } else {
                    completedToday.forEachIndexed { i, t ->
                        sb.appendLine("   ${i + 1}. [${t.priority}] ${t.title} (হোল্ডিং: ${t.holdingId.ifEmpty { "প্রযোজ্য নয়" }})")
                        if (t.notes.isNotEmpty()) sb.appendLine("      নোট: ${t.notes}")
                    }
                }
                sb.appendLine()

                sb.appendLine("৩. অপেক্ষমাণ ও ফলো-আপের জন্য নির্ধারিত কাজ:")
                if (pendingTasks.isEmpty()) {
                    sb.appendLine("   - (কোনো কাজ অপেক্ষমাণ নেই)")
                } else {
                    pendingTasks.forEachIndexed { i, t ->
                        sb.appendLine("   ${i + 1}. [${t.priority}] ${t.title} | শেষ সময়: ${t.dueDate}")
                    }
                }
                sb.appendLine()

                sb.appendLine("৪. আজকের মাঠ পরিদর্শনের খতিয়ান:")
                if (todayInspections.isEmpty()) {
                    sb.appendLine("   - (আজ কোনো মাঠ পরিদর্শন রেকর্ড নেই)")
                } else {
                    todayInspections.forEachIndexed { i, n ->
                        sb.appendLine("   ${i + 1}. রেফারেন্স: ${n.inspectionRef} | হোল্ডিং: ${n.holdingRef} (${n.assignedArea})")
                        sb.appendLine("      অবস্থা: ${translateVerificationStatus(n.verificationStatus)}")
                        sb.appendLine("      পর্যবেক্ষণ: ${n.observations}")
                        sb.appendLine("      পরবর্তী পদক্ষেপ: ${n.followUpAction}")
                    }
                }

                // CSV Exportable
                exportableCsv = buildString {
                    appendLine("Task_ID,Title,Holding_Ref,Due_Date,Priority,Status,Notes")
                    tasks.forEach { t ->
                        appendLine(
                            "${t.id},${formatCsvCell(t.title)},${formatCsvCell(t.holdingId)},${formatCsvCell(t.dueDate)},${formatCsvCell(t.priority)},${formatCsvCell(t.status)},${formatCsvCell(t.notes)}"
                        )
                    }
                }
            }

            ReportType.REVENUE_SUMMARY -> {
                metrics["মোট হোল্ডিং"] = holdings.size.toString()
                metrics["মোট দাবি"] = RevenueAnalysisEngine.formatTaka(financialSummary.totalDemand)
                metrics["মোট আদায়"] = RevenueAnalysisEngine.formatTaka(financialSummary.totalCollected)
                metrics["অবশিষ্ট বকেয়া"] = RevenueAnalysisEngine.formatTaka(financialSummary.totalRemainingBalance)
                metrics["আদায় হার"] = "%.1f%%".format(financialSummary.collectionRatePercentage)

                sb.appendLine("১. রাজস্ব আদায় পরিসংখ্যান সারসংক্ষেপ:")
                sb.appendLine("   - মোট পর্যালোচিত হোল্ডিং : ${holdings.size} টি")
                sb.appendLine("   - সর্বমোট দাবি (পূর্বের বকেয়া + চলতি) : ${RevenueAnalysisEngine.formatTaka(financialSummary.totalDemand)}")
                sb.appendLine("   - সংগৃহীত মোট আদায় : ${RevenueAnalysisEngine.formatTaka(financialSummary.totalCollected)}")
                sb.appendLine("   - আদায়ের হার : %.1f%%".format(financialSummary.collectionRatePercentage))
                sb.appendLine("   - অনাদায়ী অবশিষ্ট বকেয়া : ${RevenueAnalysisEngine.formatTaka(financialSummary.totalRemainingBalance)}")
                sb.appendLine()

                sb.appendLine("২. শীর্ষ বকেয়া হোল্ডিং তালিকা (অডিট ও ফলো-আপের জন্য):")
                val topArrears = holdings.sortedByDescending { it.remainingBalance }.take(10)
                if (topArrears.isEmpty()) {
                    sb.appendLine("   - কোনো হোল্ডিং রেকর্ড পাওয়া যায়নি।")
                } else {
                    topArrears.forEachIndexed { i, h ->
                        sb.appendLine("   ${i + 1}. হোল্ডিং: ${h.holdingId} | মালিক: ${h.ownerName}")
                        sb.appendLine("      মোট দাবি: ৳${RevenueAnalysisEngine.formatNumber(h.totalPayable)} | আদায়: ৳${RevenueAnalysisEngine.formatNumber(h.amountCollected)} | অবশিষ্ট বকেয়া: ৳${RevenueAnalysisEngine.formatNumber(h.remainingBalance)}")
                    }
                }

                // CSV Exportable
                exportableCsv = buildString {
                    appendLine("Holding_ID,Owner_Name,Ward,Previous_Arrears,Current_Demand,Total_Payable,Amount_Collected,Remaining_Balance")
                    holdings.forEach { h ->
                        appendLine(
                            "${formatCsvCell(h.holdingId)},${formatCsvCell(h.ownerName)},${formatCsvCell(h.ward)},${h.previousArrears},${h.currentDemand},${h.totalPayable},${h.amountCollected},${h.remainingBalance}"
                        )
                    }
                }
            }

            ReportType.ARREARS_AUDIT -> {
                val flagged = holdings.filter { it.hasCalculationMismatch || it.hasMissingValues || it.isDuplicate }
                metrics["অডিট চিহ্নিত হোল্ডিং"] = flagged.size.toString()
                metrics["গাণিতিক অমিল"] = financialSummary.anomalySummary.calculationMismatchesCount.toString()
                metrics["দ্বৈত হোল্ডিং"] = financialSummary.anomalySummary.duplicateIdsCount.toString()
                metrics["অসম্পূর্ণ তথ্য"] = financialSummary.anomalySummary.missingValuesCount.toString()

                sb.appendLine("১. অডিট ও ডাটা অসঙ্গতি সারসংক্ষেপ:")
                sb.appendLine("   - মোট চিহ্নিত রেকর্ড : ${flagged.size} টি")
                sb.appendLine("   - গাণিতিক যোগফলে অমিল : ${financialSummary.anomalySummary.calculationMismatchesCount} টি")
                sb.appendLine("   - দ্বৈত হোল্ডিং উপস্থিতি : ${financialSummary.anomalySummary.duplicateIdsCount} টি")
                sb.appendLine("   - অসম্পূর্ণ মান বা ফাঁকা ক্ষেত্র : ${financialSummary.anomalySummary.missingValuesCount} টি")
                sb.appendLine("   * বিশেষ সতর্কতা: এই অমিলগুলো ডাটা এন্ট্রি টাইপো বা অসম্পূর্ণ রেজিস্টার নির্দেশ করে। কোনো অসদাচরণ বিবেচনা না করে খতিয়ান বইয়ের সাথে পুনঃনিরীক্ষা করার অনুরোধ করা হচ্ছে।")
                sb.appendLine()

                sb.appendLine("২. নিরীক্ষার জন্য ফ্ল্যাগকৃত রেকর্ডের তালিকা (উৎস সারিসহ):")
                if (flagged.isEmpty()) {
                    sb.appendLine("   - কোনো অসঙ্গতি বা অমিল পাওয়া যায়নি। সকল রেকর্ড গাণিতিকভাবে সঠিক।")
                } else {
                    flagged.forEachIndexed { i, h ->
                        sb.appendLine("   ${i + 1}. সারি #${h.sourceRowNumber} | হোল্ডিং: ${h.holdingId} | নাম: ${h.ownerName}")
                        sb.appendLine("      চিহ্নিত কারণ: ${h.mismatchNotes}")
                        sb.appendLine("      উৎস ডাটা: বকেয়া=৳${RevenueAnalysisEngine.formatNumber(h.previousArrears)}, চলতি=৳${RevenueAnalysisEngine.formatNumber(h.currentDemand)}, প্রদেয়=৳${RevenueAnalysisEngine.formatNumber(h.totalPayable)}, আদায়=৳${RevenueAnalysisEngine.formatNumber(h.amountCollected)}")
                    }
                }

                exportableCsv = buildString {
                    appendLine("Source_Row,Holding_ID,Owner_Name,Flag_Reason,Total_Payable,Amount_Collected,Balance")
                    flagged.forEach { h ->
                        appendLine(
                            "${h.sourceRowNumber},${formatCsvCell(h.holdingId)},${formatCsvCell(h.ownerName)},${formatCsvCell(h.mismatchNotes)},${h.totalPayable},${h.amountCollected},${h.remainingBalance}"
                        )
                    }
                }
            }

            ReportType.FIELD_INSPECTION -> {
                metrics["মোট পরিদর্শন"] = inspectionNotes.size.toString()
                metrics["চূড়ান্ত রিপোর্ট"] = inspectionNotes.count { !it.isDraft }.toString()
                metrics["খসড়া নোট"] = inspectionNotes.count { it.isDraft }.toString()

                sb.appendLine("১. মাঠ পরিদর্শনের খতিয়ান ও সত্যতা যাচাই:")
                sb.appendLine("   - সর্বমোট পরিদর্শন নোট : ${inspectionNotes.size} টি")
                sb.appendLine("   - চূড়ান্ত রিপোর্ট : ${inspectionNotes.count { !it.isDraft }} টি")
                sb.appendLine("   - চলমান খসড়া : ${inspectionNotes.count { it.isDraft }} টি")
                sb.appendLine()

                sb.appendLine("২. পরিদর্শনভিত্তিক পর্যবেক্ষণ ও সুপারিশ:")
                if (inspectionNotes.isEmpty()) {
                    sb.appendLine("   - কোনো পরিদর্শন নোট সংরক্ষিত নেই।")
                } else {
                    inspectionNotes.forEachIndexed { i, n ->
                        val draftTag = if (n.isDraft) "[খসড়া] " else ""
                        sb.appendLine("   ${i + 1}. ${draftTag}রেফারেন্স: ${n.inspectionRef} | হোল্ডিং: ${n.holdingRef}")
                        sb.appendLine("      এলাকা: ${n.assignedArea} | তারিখ: ${n.inspectionDate}")
                        sb.appendLine("      স্ট্যাটাস: ${translateVerificationStatus(n.verificationStatus)}")
                        sb.appendLine("      মাঠ পর্যবেক্ষণ: ${n.observations}")
                        sb.appendLine("      প্রস্তাবিত পদক্ষেপ: ${n.followUpAction}")
                        if (n.notes.isNotEmpty()) sb.appendLine("      অতিরিক্ত নোট: ${n.notes}")
                        sb.appendLine()
                    }
                }

                exportableCsv = buildString {
                    appendLine("Inspection_Ref,Holding_Ref,Area,Date,Status,Observations,Follow_Up_Action,Is_Draft")
                    inspectionNotes.forEach { n ->
                        appendLine(
                            "${formatCsvCell(n.inspectionRef)},${formatCsvCell(n.holdingRef)},${formatCsvCell(n.assignedArea)},${formatCsvCell(n.inspectionDate)},${formatCsvCell(n.verificationStatus)},${formatCsvCell(n.observations)},${formatCsvCell(n.followUpAction)},${n.isDraft}"
                        )
                    }
                }
            }
        }

        if (customRemarks.isNotBlank()) {
            sb.appendLine("-------------------------------------------------------------------")
            sb.appendLine("কর্মকর্তার বিশেষ মন্তব্য / নোট:")
            sb.appendLine(customRemarks)
        }

        sb.appendLine()
        sb.appendLine("-------------------------------------------------------------------")
        sb.appendLine("প্রতিবেদন প্রস্তুতকারী: $supervisorName (রেভিনিউ সুপারভাইজার)")
        sb.appendLine("প্রস্তুতের সময়: $date | রেভিনিউ পাইলট দ্বারা সিস্টেম জেনারেটেড")
        sb.appendLine("===================================================================")

        return GeneratedReport(
            type = type,
            title = type.titleBn,
            date = date,
            supervisorName = supervisorName,
            wardInfo = wardInfo,
            contentBangla = sb.toString(),
            exportableCsv = exportableCsv,
            keyMetrics = metrics
        )
    }

    /**
     * Escapes quotes and neutralizes leading spreadsheet formula characters (=, +, -, @, tab, CR)
     * before wrapping the cell value in double quotes.
     */
    fun formatCsvCell(rawValue: String): String {
        val singleLine = rawValue.replace("\r\n", " ").replace("\n", " ").replace("\r", " ")
        val trimmedStart = singleLine.trimStart()
        val formulaSafe = if (
            trimmedStart.isNotEmpty() &&
            trimmedStart[0] in charArrayOf('=', '+', '-', '@', '\t', '\r')
        ) {
            "'$singleLine"
        } else {
            singleLine
        }
        val escapedQuotes = formulaSafe.replace("\"", "\"\"")
        return "\"$escapedQuotes\""
    }

    private fun translateVerificationStatus(status: String): String {
        return when (status) {
            "VERIFIED" -> "যাচাইকৃত ও সঠিক"
            "MISMATCH_MEASUREMENT" -> "পরিমাপে অমিল পাওয়া গেছে"
            "UNAUTHORIZED_COMMERCIAL" -> "অননুমোদিত বাণিজ্যিক ব্যবহার"
            "EXTENSION_FOUND" -> "অতিরিক্ত তলা / স্থাপনা দৃশ্যমান"
            "INCOMPLETE" -> "অসম্পূর্ণ পরিদর্শন (পুনঃপরিদর্শন প্রয়োজন)"
            else -> status
        }
    }
}
