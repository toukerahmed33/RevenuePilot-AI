package com.example.domain.analyzer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

data class ParsedSpreadsheet(
    val fileName: String,
    val rows: List<List<String>>,
    val suggestedMapping: ColumnMapping
)

object SpreadsheetFileParser {

    private const val MAX_CSV_BYTES = 10 * 1024 * 1024 // 10 MB
    private const val MAX_XLSX_ENTRY_BYTES = 10 * 1024 * 1024 // 10 MB per XML entry
    private const val MAX_XLSX_TOTAL_BYTES = 20 * 1024 * 1024 // 20 MB total extracted XML
    private const val MAX_ZIP_ENTRIES = 256
    private const val MAX_COLUMN_INDEX = 255

    /**
     * Reads either a CSV/TSV text file or a native .xlsx Excel workbook from a Document URI.
     */
    fun parseFromUri(context: Context, uri: Uri): ParsedSpreadsheet {
        val fileName = queryFileName(context, uri) ?: "imported_sheet"
        val mimeType = context.contentResolver.getType(uri).orEmpty()
        val isXlsx = fileName.endsWith(".xlsx", ignoreCase = true) ||
            mimeType.contains("spreadsheetml", ignoreCase = true)

        val rows = context.contentResolver.openInputStream(uri)?.use { stream ->
            if (isXlsx) {
                parseXlsxStream(stream)
            } else {
                val bytes = readBoundedBytes(stream, MAX_CSV_BYTES, "CSV ফাইল")
                val text = bytes.toString(Charsets.UTF_8)
                RevenueAnalysisEngine.parseCsv(text)
            }
        } ?: emptyList()

        val suggestedMapping = if (rows.isNotEmpty()) {
            detectColumnMapping(rows.first())
        } else {
            ColumnMapping()
        }

        return ParsedSpreadsheet(
            fileName = fileName,
            rows = rows,
            suggestedMapping = suggestedMapping
        )
    }

    /**
     * Pure Kotlin + hardened SAXParser reader for .xlsx workbooks (xl/sharedStrings.xml + sheet1.xml).
     * Enforces XXE protections and bounded ZIP decompression limits to prevent zip-bomb OOM/DoS.
     */
    fun parseXlsxStream(inputStream: InputStream): List<List<String>> {
        val zipEntries = mutableMapOf<String, ByteArray>()
        var totalExtractedBytes = 0
        var entryCount = 0

        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entryCount++
                if (entryCount > MAX_ZIP_ENTRIES) {
                    throw IllegalArgumentException("অননুমোদিত বা অতিরিক্ত বড় স্প্রেডশিট আর্কাইভ (অতিরিক্ত এন্ট্রি সংখ্যা)।")
                }
                if (!entry.isDirectory) {
                    val name = entry.name
                    if (name == "xl/sharedStrings.xml" || name.startsWith("xl/worksheets/sheet")) {
                        val remainingBudget = (MAX_XLSX_TOTAL_BYTES - totalExtractedBytes).coerceAtLeast(0)
                        val entryLimit = minOf(MAX_XLSX_ENTRY_BYTES, remainingBudget)
                        val bytes = readBoundedBytes(zis, entryLimit, name)
                        totalExtractedBytes += bytes.size
                        zipEntries[name] = bytes
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val sharedStrings = zipEntries["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()
        val sheetBytes = zipEntries["xl/worksheets/sheet1.xml"]
            ?: zipEntries.entries.firstOrNull { it.key.startsWith("xl/worksheets/sheet") }?.value
            ?: return emptyList()

        return parseWorksheetXml(sheetBytes, sharedStrings)
    }

    private fun readBoundedBytes(stream: InputStream, maxBytes: Int, label: String): ByteArray {
        val buffer = ByteArray(8192)
        val out = ByteArrayOutputStream()
        var total = 0
        while (true) {
            val read = stream.read(buffer)
            if (read == -1) break
            total += read
            if (total > maxBytes) {
                throw IllegalArgumentException("$label নির্ধারিত সর্বোচ্চ সীমা (${maxBytes / (1024 * 1024)} MB) অতিক্রম করেছে।")
            }
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun newSecureSaxParser(): SAXParser {
        val factory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = true
            try {
                isXIncludeAware = false
            } catch (_: UnsupportedOperationException) {
                // Ignored on minimal Android SAX implementations
            }
        }
        val secureFeatures = listOf(
            XMLConstants.FEATURE_SECURE_PROCESSING to true,
            "http://apache.org/xml/features/disallow-doctype-decl" to true,
            "http://xml.org/sax/features/external-general-entities" to false,
            "http://xml.org/sax/features/external-parameter-entities" to false,
            "http://apache.org/xml/features/nonvalidating/load-external-dtd" to false
        )
        for ((feature, value) in secureFeatures) {
            try {
                factory.setFeature(feature, value)
            } catch (_: Exception) {
                // Some Android XML parsers don't recognize all Xerces URIs; apply every supported guard.
            }
        }
        return factory.newSAXParser()
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        val saxParser = newSecureSaxParser()

        val handler = object : DefaultHandler() {
            private var inSi = false
            private var inText = false
            private val buffer = StringBuilder()

            override fun resolveEntity(publicId: String?, systemId: String?): InputSource {
                return InputSource(StringReader(""))
            }

            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                val tag = localName?.ifEmpty { qName } ?: qName ?: ""
                when (tag) {
                    "si" -> {
                        inSi = true
                        buffer.clear()
                    }
                    "t" -> if (inSi) inText = true
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (inSi && inText) {
                    buffer.append(ch, start, length)
                }
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                val tag = localName?.ifEmpty { qName } ?: qName ?: ""
                when (tag) {
                    "t" -> inText = false
                    "si" -> {
                        inSi = false
                        strings.add(buffer.toString().trim())
                    }
                }
            }
        }

        saxParser.parse(ByteArrayInputStream(bytes), handler)
        return strings
    }

    private fun parseWorksheetXml(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val saxParser = newSecureSaxParser()

        val handler = object : DefaultHandler() {
            private var currentRow = mutableListOf<String>()
            private var currentColIndex = -1
            private var currentCellType = ""
            private var inCellValue = false
            private var inInlineString = false
            private val cellBuffer = StringBuilder()

            override fun resolveEntity(publicId: String?, systemId: String?): InputSource {
                return InputSource(StringReader(""))
            }

            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                val tag = localName?.ifEmpty { qName } ?: qName ?: ""
                when (tag) {
                    "row" -> currentRow = mutableListOf()
                    "c" -> {
                        val cellRef = attributes?.getValue("r") ?: ""
                        currentColIndex = columnReferenceToIndex(cellRef)
                        currentCellType = attributes?.getValue("t") ?: ""
                        cellBuffer.clear()
                    }
                    "v" -> inCellValue = true
                    "t" -> if (currentCellType == "inlineStr") inInlineString = true
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (inCellValue || inInlineString) {
                    cellBuffer.append(ch, start, length)
                }
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                val tag = localName?.ifEmpty { qName } ?: qName ?: ""
                when (tag) {
                    "v" -> inCellValue = false
                    "t" -> inInlineString = false
                    "c" -> {
                        val rawValue = cellBuffer.toString().trim()
                        val resolvedValue = when (currentCellType) {
                            "s" -> rawValue.toIntOrNull()?.let { sharedStrings.getOrNull(it) } ?: ""
                            else -> rawValue
                        }
                        if (currentColIndex in 0..MAX_COLUMN_INDEX) {
                            while (currentRow.size <= currentColIndex) {
                                currentRow.add("")
                            }
                            currentRow[currentColIndex] = resolvedValue
                        } else if (currentColIndex < 0 && currentRow.size <= MAX_COLUMN_INDEX) {
                            currentRow.add(resolvedValue)
                        }
                    }
                    "row" -> {
                        if (currentRow.any { it.isNotBlank() }) {
                            rows.add(currentRow)
                        }
                    }
                }
            }
        }

        saxParser.parse(ByteArrayInputStream(bytes), handler)
        return rows
    }

    /**
     * Converts Excel cell coordinate like "C12" -> 0-based column index 2.
     */
    fun columnReferenceToIndex(cellRef: String): Int {
        if (cellRef.isBlank()) return -1
        var col = 0
        for (ch in cellRef) {
            if (ch in 'A'..'Z') {
                col = col * 26 + (ch - 'A' + 1)
            } else if (ch in 'a'..'z') {
                col = col * 26 + (ch - 'a' + 1)
            } else {
                break
            }
            if (col > MAX_COLUMN_INDEX + 1) return -1
        }
        return if (col > 0) col - 1 else -1
    }

    /**
     * Heuristically detects column mapping from Bangla or English header keywords.
     */
    fun detectColumnMapping(headers: List<String>): ColumnMapping {
        var holdingId = 0
        var ownerName = 1
        var ward = 2
        var arrears = 3
        var demand = 4
        var payable = 5
        var collected = 6
        var balance = 7

        headers.forEachIndexed { idx, rawHeader ->
            val h = rawHeader.lowercase().trim()
            when {
                h.contains("holding") || h.contains("হোল্ডিং") -> holdingId = idx
                h.contains("owner") || h.contains("name") || h.contains("মালিক") || h.contains("নাম") -> ownerName = idx
                h.contains("ward") || h.contains("ওয়ার্ড") || h.contains("এলাকা") -> ward = idx
                h.contains("arrear") || h.contains("previous") || h.contains("বকেয়া") -> arrears = idx
                h.contains("current") || h.contains("demand") || h.contains("চলতি") || h.contains("দাবি") -> demand = idx
                h.contains("total") || h.contains("payable") || h.contains("মোট") || h.contains("প্রদেয়") -> payable = idx
                h.contains("collect") || h.contains("paid") || h.contains("আদায়") || h.contains("পরিশোধ") -> collected = idx
                h.contains("balance") || h.contains("remain") || h.contains("অবশিষ্ট") || h.contains("ব্যালেন্স") -> balance = idx
            }
        }

        return ColumnMapping(
            holdingIdIndex = holdingId,
            ownerNameIndex = ownerName,
            wardIndex = ward,
            previousArrearsIndex = arrears,
            currentDemandIndex = demand,
            totalPayableIndex = payable,
            amountCollectedIndex = collected,
            remainingBalanceIndex = balance
        )
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
