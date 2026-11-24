package com.example.domain.analyzer

import com.example.data.local.entity.HoldingRecordEntity
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

data class ColumnMapping(
    val holdingIdIndex: Int = 0,
    val ownerNameIndex: Int = 1,
    val wardIndex: Int = 2,
    val previousArrearsIndex: Int = 3,
    val currentDemandIndex: Int = 4,
    val totalPayableIndex: Int = 5,
    val amountCollectedIndex: Int = 6,
    val remainingBalanceIndex: Int = 7
)

data class AnomalySummary(
    val missingValuesCount: Int,
    val duplicateIdsCount: Int,
    val calculationMismatchesCount: Int,
    val totalFlaggedRecords: Int
)

data class FinancialSummary(
    val totalRecords: Int,
    val totalDemand: Double,
    val totalCollected: Double,
    val totalRemainingBalance: Double,
    val collectionRatePercentage: Double,
    val anomalySummary: AnomalySummary
)

object RevenueAnalysisEngine {

    /**
     * Deterministic CSV parser supporting quoted strings with commas and escaped quotes.
     */
    fun parseCsv(csvText: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val lines = csvText.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val tokens = mutableListOf<String>()
            val current = StringBuilder()
            var inQuotes = false
            var i = 0
            while (i < trimmed.length) {
                val c = trimmed[i]
                if (c == '\"') {
                    if (inQuotes && i + 1 < trimmed.length && trimmed[i + 1] == '\"') {
                        current.append('\"')
                        i++ // Skip escaped quote
                    } else {
                        inQuotes = !inQuotes
                    }
                } else if (c == ',' && !inQuotes) {
                    tokens.add(current.toString().trim())
                    current.clear()
                } else {
                    current.append(c)
                }
                i++
            }
            tokens.add(current.toString().trim())
            rows.add(tokens)
        }
        return rows
    }

    /**
     * Map parsed CSV rows into HoldingRecordEntity with deterministic validation and audit checks.
     */
    fun processRows(
        rows: List<List<String>>,
        mapping: ColumnMapping,
        hasHeader: Boolean = true
    ): List<HoldingRecordEntity> {
        val dataRows = if (hasHeader && rows.isNotEmpty()) rows.drop(1) else rows
        val holdingIdCounts = mutableMapOf<String, Int>()

        // First pass: count holding IDs for duplicate detection
        dataRows.forEach { row ->
            val id = row.getOrNull(mapping.holdingIdIndex)?.trim() ?: ""
            if (id.isNotEmpty()) {
                val normalizedId = id.uppercase()
                holdingIdCounts[normalizedId] = (holdingIdCounts[normalizedId] ?: 0) + 1
            }
        }

        val result = mutableListOf<HoldingRecordEntity>()

        dataRows.forEachIndexed { index, row ->
            val rowNumber = if (hasHeader) index + 2 else index + 1
            val rawHoldingId = row.getOrNull(mapping.holdingIdIndex)?.trim() ?: ""
            val ownerName = row.getOrNull(mapping.ownerNameIndex)?.trim() ?: ""
            val ward = row.getOrNull(mapping.wardIndex)?.trim() ?: ""

            val rawArrears = row.getOrNull(mapping.previousArrearsIndex)?.trim() ?: ""
            val rawDemand = row.getOrNull(mapping.currentDemandIndex)?.trim() ?: ""
            val rawTotalPayable = row.getOrNull(mapping.totalPayableIndex)?.trim() ?: ""
            val rawCollected = row.getOrNull(mapping.amountCollectedIndex)?.trim() ?: ""
            val rawBalance = row.getOrNull(mapping.remainingBalanceIndex)?.trim() ?: ""

            val parsedArrears = parseAmount(rawArrears)
            val parsedDemand = parseAmount(rawDemand)
            val parsedTotalPayable = parseAmount(rawTotalPayable)
            val parsedCollected = parseAmount(rawCollected)
            val parsedBalance = parseAmount(rawBalance)

            // Missing values check
            var hasMissing = false
            val missingFields = mutableListOf<String>()
            if (rawHoldingId.isEmpty()) {
                hasMissing = true
                missingFields.add("হোল্ডিং আইডি নেই")
            }
            if (rawArrears.isEmpty()) missingFields.add("পূর্বের বকেয়া অনুদ্ঘাটিত")
            if (rawDemand.isEmpty()) missingFields.add("চলতি দাবি অনুদ্ঘাটিত")

            // Duplicate identifier check
            val isDuplicate = rawHoldingId.isNotEmpty() && (holdingIdCounts[rawHoldingId.uppercase()] ?: 0) > 1

            // Deterministic mathematical calculation
            val calculatedTotalDemand = parsedArrears + parsedDemand
            val effectiveTotalPayable = if (rawTotalPayable.isNotEmpty()) parsedTotalPayable else calculatedTotalDemand
            val calculatedRemainingBalance = effectiveTotalPayable - parsedCollected
            val effectiveBalance = if (rawBalance.isNotEmpty()) parsedBalance else calculatedRemainingBalance

            // Inconsistency checks (Strictly deterministic arithmetic checks)
            var hasMismatch = false
            val mismatchNotes = mutableListOf<String>()

            // 1. Total demand mismatch
            if (rawTotalPayable.isNotEmpty() && abs(parsedTotalPayable - calculatedTotalDemand) > 1.0) {
                hasMismatch = true
                mismatchNotes.add(
                    "দাবি অমিল: উৎসে ৳${formatNumber(parsedTotalPayable)}, কিন্তু গাণিতিক যোগফল (বকেয়া+চলতি) = ৳${formatNumber(calculatedTotalDemand)}"
                )
            }

            // 2. Balance mismatch
            if (rawBalance.isNotEmpty() && abs(parsedBalance - calculatedRemainingBalance) > 1.0) {
                hasMismatch = true
                mismatchNotes.add(
                    "ব্যালেন্স অমিল: উৎসে ৳${formatNumber(parsedBalance)}, কিন্তু গাণিতিক ব্যালেন্স (মোট-আদায়) = ৳${formatNumber(calculatedRemainingBalance)}"
                )
            }

            if (isDuplicate) {
                mismatchNotes.add("দ্বৈত হোল্ডিং: একই হোল্ডিং আইডি একাধিক সারিতে উপস্থিত")
            }

            if (hasMissing) {
                mismatchNotes.add("অসম্পূর্ণ মান: ${missingFields.joinToString(", ")}")
            }

            val rawCsv = row.joinToString(", ")

            result.add(
                HoldingRecordEntity(
                    holdingId = if (rawHoldingId.isEmpty()) "W00-MISSING" else rawHoldingId,
                    ownerName = ownerName,
                    ward = if (ward.isEmpty()) "সাধারণ ওয়ার্ড" else ward,
                    area = "",
                    previousArrears = parsedArrears,
                    currentDemand = parsedDemand,
                    totalPayable = effectiveTotalPayable,
                    amountCollected = parsedCollected,
                    remainingBalance = effectiveBalance,
                    sourceRowNumber = rowNumber,
                    rawCsvRow = rawCsv,
                    hasCalculationMismatch = hasMismatch,
                    hasMissingValues = hasMissing,
                    isDuplicate = isDuplicate,
                    mismatchNotes = mismatchNotes.joinToString(" | ")
                )
            )
        }

        return result
    }

    /**
     * Parse numeric amounts, safely handling commas, currency signs, and blanks.
     */
    fun parseAmount(value: String): Double {
        if (value.isBlank()) return 0.0
        val sanitized = value.replace("৳", "")
            .replace("BDT", "", ignoreCase = true)
            .replace(",", "")
            .replace(" ", "")
            .trim()
        return sanitized.toDoubleOrNull() ?: 0.0
    }

    /**
     * Compute deterministic financial aggregates for loaded records.
     */
    fun calculateSummary(records: List<HoldingRecordEntity>): FinancialSummary {
        if (records.isEmpty()) {
            return FinancialSummary(
                totalRecords = 0,
                totalDemand = 0.0,
                totalCollected = 0.0,
                totalRemainingBalance = 0.0,
                collectionRatePercentage = 0.0,
                anomalySummary = AnomalySummary(0, 0, 0, 0)
            )
        }

        var totalDemand = 0.0
        var totalCollected = 0.0
        var totalBalance = 0.0

        var missingCount = 0
        var duplicateCount = 0
        var mismatchCount = 0
        var totalFlagged = 0

        records.forEach { record ->
            totalDemand += record.totalPayable
            totalCollected += record.amountCollected
            totalBalance += record.remainingBalance

            var flagged = false
            if (record.hasMissingValues) {
                missingCount++
                flagged = true
            }
            if (record.isDuplicate) {
                duplicateCount++
                flagged = true
            }
            if (record.hasCalculationMismatch) {
                mismatchCount++
                flagged = true
            }
            if (flagged) {
                totalFlagged++
            }
        }

        val rate = if (totalDemand > 0.0) (totalCollected / totalDemand) * 100.0 else 0.0

        return FinancialSummary(
            totalRecords = records.size,
            totalDemand = totalDemand,
            totalCollected = totalCollected,
            totalRemainingBalance = totalBalance,
            collectionRatePercentage = rate,
            anomalySummary = AnomalySummary(
                missingValuesCount = missingCount,
                duplicateIdsCount = duplicateCount,
                calculationMismatchesCount = mismatchCount,
                totalFlaggedRecords = totalFlagged
            )
        )
    }

    /**
     * Currency formatting in Bangladeshi Taka (BDT).
     * Example: ৳ ১,২৫,০০ অথবা ৳ 1,25,000
     */
    fun formatTaka(amount: Double, useBanglaNumerals: Boolean = false): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 0
        val formatted = formatter.format(amount)
        val text = if (useBanglaNumerals) toBanglaNumerals(formatted) else formatted
        return "৳ $text"
    }

    fun formatNumber(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 0
        return formatter.format(amount)
    }

    fun toBanglaNumerals(englishNumberStr: String): String {
        val banglaDigits = mapOf(
            '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
            '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
        )
        val sb = StringBuilder()
        for (c in englishNumberStr) {
            sb.append(banglaDigits[c] ?: c)
        }
        return sb.toString()
    }
}
