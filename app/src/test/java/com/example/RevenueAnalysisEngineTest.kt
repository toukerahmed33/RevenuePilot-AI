package com.example

import com.example.data.local.entity.HoldingRecordEntity
import com.example.domain.analyzer.ColumnMapping
import com.example.domain.analyzer.RevenueAnalysisEngine
import com.example.domain.analyzer.SpreadsheetFileParser
import com.example.domain.report.ReportGeneratorEngine
import com.example.domain.report.ReportType
import com.example.domain.sample.SampleDataGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class RevenueAnalysisEngineTest {

    @Test
    fun testDeterministicCsvParsing() {
        val sampleCsv = """
            Holding ID,Owner Name,Ward,Previous Arrears,Current Demand,Total Payable,Amount Collected,Remaining Balance
            W07-H1001,"মুহাম্মদ রফিকুল ইসলাম",ওয়ার্ড-০৭,12000,24000,36000,36000,0
            W07-H1002,"আনোয়ারা বেগম",ওয়ার্ড-০৭,35000,18000,53000,20000,33000
        """.trimIndent()

        val parsed = RevenueAnalysisEngine.parseCsv(sampleCsv)
        assertEquals(3, parsed.size)
        assertEquals("Holding ID", parsed[0][0])
        assertEquals("W07-H1001", parsed[1][0])
        assertEquals("মুহাম্মদ রফিকুল ইসলাম", parsed[1][1])
    }

    @Test
    fun testXlsxNativeParser() {
        val sharedStringsXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="4" uniqueCount="4">
                <si><t>Holding ID</t></si>
                <si><t>Owner</t></si>
                <si><t>W07-H900</t></si>
                <si><t>করিম মিয়া</t></si>
            </sst>
        """.trimIndent()

        val sheet1Xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <sheetData>
                    <row r="1">
                        <c r="A1" t="s"><v>0</v></c>
                        <c r="B1" t="s"><v>1</v></c>
                        <c r="C1" t="inlineStr"><is><t>Arrears</t></is></c>
                    </row>
                    <row r="2">
                        <c r="A2" t="s"><v>2</v></c>
                        <c r="B2" t="s"><v>3</v></c>
                        <c r="C2"><v>15000</v></c>
                    </row>
                </sheetData>
            </worksheet>
        """.trimIndent()

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zos.write(sharedStringsXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zos.write(sheet1Xml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        val rows = SpreadsheetFileParser.parseXlsxStream(ByteArrayInputStream(baos.toByteArray()))
        assertEquals(2, rows.size)
        assertEquals("Holding ID", rows[0][0])
        assertEquals("Owner", rows[0][1])
        assertEquals("Arrears", rows[0][2])
        assertEquals("W07-H900", rows[1][0])
        assertEquals("করিম মিয়া", rows[1][1])
        assertEquals("15000", rows[1][2])
    }

    @Test
    fun testXlsxParserBlocksXxeDoctype() {
        val maliciousSharedStrings = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE sst [
              <!ENTITY xxe "LEAKED_EXTERNAL_CONTENT">
            ]>
            <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <si><t>&xxe;</t></si>
            </sst>
        """.trimIndent()

        val sheet1Xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <sheetData>
                    <row r="1"><c r="A1" t="s"><v>0</v></c></row>
                </sheetData>
            </worksheet>
        """.trimIndent()

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zos.write(maliciousSharedStrings.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zos.write(sheet1Xml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        var blocked = false
        try {
            val rows = SpreadsheetFileParser.parseXlsxStream(ByteArrayInputStream(baos.toByteArray()))
            // Even if a parser tolerates DOCTYPE, entity resolution must never inject content
            blocked = rows.flatten().none { it.contains("LEAKED_EXTERNAL_CONTENT") }
        } catch (_: Exception) {
            blocked = true
        }
        assertTrue("XXE / DOCTYPE declaration must be blocked", blocked)
    }

    @Test
    fun testXlsxParserRejectsExcessiveZipEntries() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            for (i in 0..260) {
                zos.putNextEntry(ZipEntry("xl/dummy_$i.xml"))
                zos.write("<x/>".toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }

        var rejected = false
        try {
            SpreadsheetFileParser.parseXlsxStream(ByteArrayInputStream(baos.toByteArray()))
        } catch (e: IllegalArgumentException) {
            rejected = true
        }
        assertTrue("Archive exceeding MAX_ZIP_ENTRIES should be rejected", rejected)
    }

    @Test
    fun testColumnReferenceAndAutoDetection() {
        assertEquals(0, SpreadsheetFileParser.columnReferenceToIndex("A1"))
        assertEquals(2, SpreadsheetFileParser.columnReferenceToIndex("C12"))
        assertEquals(26, SpreadsheetFileParser.columnReferenceToIndex("AA5"))
        assertEquals(-1, SpreadsheetFileParser.columnReferenceToIndex("XFD1"))

        val headers = listOf("Owner Name", "হোল্ডিং নং", "Ward", "বকেয়া", "Current Demand", "Total Payable", "আদায়", "Remaining Balance")
        val mapping = SpreadsheetFileParser.detectColumnMapping(headers)
        assertEquals(1, mapping.holdingIdIndex)
        assertEquals(0, mapping.ownerNameIndex)
        assertEquals(2, mapping.wardIndex)
        assertEquals(3, mapping.previousArrearsIndex)
        assertEquals(4, mapping.currentDemandIndex)
        assertEquals(5, mapping.totalPayableIndex)
        assertEquals(6, mapping.amountCollectedIndex)
        assertEquals(7, mapping.remainingBalanceIndex)
    }

    @Test
    fun testSyntheticSampleDataIntegrity() {
        val holdings = SampleDataGenerator.getSampleHoldings()
        val tasks = SampleDataGenerator.getSampleTasks()
        val notes = SampleDataGenerator.getSampleInspectionNotes()

        assertEquals(20, holdings.size)
        assertEquals(10, tasks.size)
        assertEquals(5, notes.size)

        val duplicates = holdings.filter { it.isDuplicate }
        assertTrue("Should detect duplicate holding IDs", duplicates.isNotEmpty())

        val calculationMismatches = holdings.filter { it.hasCalculationMismatch }
        assertTrue("Should detect calculation mismatch in Record 8", calculationMismatches.isNotEmpty())
        assertEquals("W07-H1008", calculationMismatches[0].holdingId)

        val missingValues = holdings.filter { it.hasMissingValues }
        assertTrue("Should detect missing value in Record 14", missingValues.isNotEmpty())
        assertEquals("W07-H1014", missingValues[0].holdingId)
    }

    @Test
    fun testDeterministicFinancialCalculation() {
        val rawCsv = """
            ID,Name,Ward,Arrears,Demand,Payable,Collected,Balance
            H-01,Alpha,W-1,10000,15000,25000,20000,5000
            H-02,Beta,W-1,5000,10000,15000,15000,0
        """.trimIndent()

        val rows = RevenueAnalysisEngine.parseCsv(rawCsv)
        val entities = RevenueAnalysisEngine.processRows(rows, ColumnMapping(), hasHeader = true)
        val summary = RevenueAnalysisEngine.calculateSummary(entities)

        assertEquals(2, summary.totalRecords)
        assertEquals(40000.0, summary.totalDemand, 0.01)
        assertEquals(35000.0, summary.totalCollected, 0.01)
        assertEquals(5000.0, summary.totalRemainingBalance, 0.01)
        assertEquals(87.5, summary.collectionRatePercentage, 0.01)
        assertEquals(0, summary.anomalySummary.totalFlaggedRecords)
    }

    @Test
    fun testArithmeticInconsistencyDetectionIncludingZeroPayable() {
        val rawCsv = """
            ID,Name,Ward,Arrears,Demand,Payable,Collected,Balance
            H-ERROR,Mismatched,W-1,10000,10000,25000,10000,15000
            H-ZERO,ZeroPayable,W-1,10000,15000,0,0,0
        """.trimIndent()

        val rows = RevenueAnalysisEngine.parseCsv(rawCsv)
        val entities = RevenueAnalysisEngine.processRows(rows, ColumnMapping(), hasHeader = true)

        assertEquals(2, entities.size)
        assertTrue("Should flag calculation mismatch when 10k + 10k != 25k", entities[0].hasCalculationMismatch)
        assertTrue("Should flag calculation mismatch when explicit 0 payable != 25k demand", entities[1].hasCalculationMismatch)
    }

    @Test
    fun testDuplicateIdDetection() {
        val rawCsv = """
            ID,Name,Ward,Arrears,Demand,Payable,Collected,Balance
            H-DUP,Owner 1,W-1,5000,5000,10000,10000,0
            H-DUP,Owner 2,W-1,5000,5000,10000,10000,0
        """.trimIndent()

        val rows = RevenueAnalysisEngine.parseCsv(rawCsv)
        val entities = RevenueAnalysisEngine.processRows(rows, ColumnMapping(), hasHeader = true)

        assertEquals(2, entities.size)
        assertTrue("First duplicate should be flagged", entities[0].isDuplicate)
        assertTrue("Second duplicate should be flagged", entities[1].isDuplicate)
    }

    @Test
    fun testCsvFormulaNeutralizationInReports() {
        val maliciousHoldings = listOf(
            HoldingRecordEntity(
                id = 1,
                holdingId = "=CMD|' /C calc'!A0",
                ownerName = "+SUM(1,2) \"Quoted\"",
                ward = "@HYPERLINK(\"http://evil\")",
                previousArrears = 1000.0,
                currentDemand = 1000.0,
                totalPayable = 2000.0,
                amountCollected = 1000.0,
                remainingBalance = 1000.0
            )
        )
        val summary = RevenueAnalysisEngine.calculateSummary(maliciousHoldings)
        val report = ReportGeneratorEngine.generateReport(
            type = ReportType.REVENUE_SUMMARY,
            date = "2026-11-23",
            supervisorName = "Test Supervisor",
            wardInfo = "Ward 7",
            customRemarks = "",
            holdings = maliciousHoldings,
            tasks = emptyList(),
            inspectionNotes = emptyList(),
            financialSummary = summary
        )

        assertTrue(report.exportableCsv.contains("\"'=CMD|' /C calc'!A0\""))
        assertTrue(report.exportableCsv.contains("\"'+SUM(1,2) \"\"Quoted\"\"\""))
        assertTrue(report.exportableCsv.contains("\"'@HYPERLINK(\"\"http://evil\"\")\""))
        assertFalse(report.exportableCsv.contains("\",=CMD"))
    }

    @Test
    fun testReportGeneratorEngine() {
        val holdings = SampleDataGenerator.getSampleHoldings()
        val tasks = SampleDataGenerator.getSampleTasks()
        val notes = SampleDataGenerator.getSampleInspectionNotes()
        val summary = RevenueAnalysisEngine.calculateSummary(holdings)

        val report = ReportGeneratorEngine.generateReport(
            type = ReportType.REVENUE_SUMMARY,
            date = "2026-11-23",
            supervisorName = "মোহাম্মদ জাহিদ হাসান",
            wardInfo = "ওয়ার্ড-০৭, মিরপুর",
            customRemarks = "মাঠ পর্যায়ে আদায় কার্যক্রম সন্তোষজনক।",
            holdings = holdings,
            tasks = tasks,
            inspectionNotes = notes,
            financialSummary = summary
        )

        assertNotNull(report)
        assertTrue(report.contentBangla.contains("রেভিনিউ পাইলট"))
        assertTrue(report.contentBangla.contains("রাজস্ব আদায় ও সারসংক্ষেপ প্রতিবেদন"))
        assertTrue(report.contentBangla.contains("মোহাম্মদ জাহিদ হাসান"))
        assertTrue(report.exportableCsv.contains("Holding_ID"))
    }
}
