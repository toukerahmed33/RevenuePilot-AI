package com.example.domain.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import java.io.File
import java.io.FileOutputStream

object PdfReportExporter {

    private const val PAGE_WIDTH = 595 // A4 width in PostScript points (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 height
    private const val MARGIN = 40f

    /**
     * Generates a multi-page A4 PDF using Noto Sans Bengali typeface and returns a FileProvider Uri.
     */
    fun generatePdfToCache(context: Context, report: GeneratedReport): Uri {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        // Clean up cached PDFs older than 24 hours so temporary exports do not accumulate
        val cutoffMs = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        reportsDir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoffMs) {
                file.delete()
            }
        }

        val safeDate = report.date.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "report" }
        val safeName = "RevenuePilot_${report.type.name}_$safeDate.pdf"
        val outFile = File(reportsDir, safeName)
        val reportsCanonical = reportsDir.canonicalPath
        val outCanonical = outFile.canonicalPath
        require(outCanonical.startsWith("$reportsCanonical${File.separator}")) {
            "অবৈধ PDF ফাইল পাথ"
        }

        writePdfToStream(context, report) { document ->
            FileOutputStream(outFile).use { fos ->
                document.writeTo(fos)
            }
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
    }

    /**
     * Writes the generated PDF directly to a user-selected Document URI (Save As).
     */
    fun writePdfToUri(context: Context, report: GeneratedReport, targetUri: Uri) {
        writePdfToStream(context, report) { document ->
            val stream = context.contentResolver.openOutputStream(targetUri)
                ?: throw IllegalStateException("লক্ষ্য ফাইলে লেখার অনুমতি পাওয়া যায়নি")
            stream.use { os ->
                document.writeTo(os)
            }
        }
    }

    private fun writePdfToStream(
        context: Context,
        report: GeneratedReport,
        writer: (PdfDocument) -> Unit
    ) {
        val bengaliTypeface: Typeface = ResourcesCompat.getFont(context, R.font.noto_sans_bengali)
            ?: Typeface.DEFAULT

        val document = PdfDocument()
        val contentWidth = (PAGE_WIDTH - 2 * MARGIN).toInt()

        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#0B5A38")
            style = Paint.Style.FILL
        }
        val headerTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(bengaliTypeface, Typeface.BOLD)
        }
        val headerSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4F5E1")
            textSize = 10f
            typeface = bengaliTypeface
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#141A16")
            textSize = 10.5f
            typeface = bengaliTypeface
        }
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5F6B63")
            textSize = 8.5f
            typeface = bengaliTypeface
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#DCE5DF")
            strokeWidth = 1f
        }

        val fullLayout = StaticLayout.Builder.obtain(
            report.contentBangla,
            0,
            report.contentBangla.length,
            bodyPaint,
            contentWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(3f, 1.15f)
            .setIncludePad(true)
            .build()

        val totalLines = fullLayout.lineCount
        var currentLine = 0
        var pageNumber = 1

        while (currentLine < totalLines || pageNumber == 1) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            var yCursor = MARGIN

            if (pageNumber == 1) {
                // Header Banner on Page 1
                val headerHeight = 68f
                canvas.drawRoundRect(
                    MARGIN,
                    yCursor,
                    PAGE_WIDTH - MARGIN,
                    yCursor + headerHeight,
                    8f,
                    8f,
                    headerBgPaint
                )
                canvas.drawText(
                    "রেভিনিউ পাইলট (RevenuePilot AI) — ${report.title}",
                    MARGIN + 14f,
                    yCursor + 26f,
                    headerTitlePaint
                )
                canvas.drawText(
                    "তারিখ: ${report.date}  |  কর্মকর্তা: ${report.supervisorName}  |  এলাকা: ${report.wardInfo}",
                    MARGIN + 14f,
                    yCursor + 48f,
                    headerSubPaint
                )
                yCursor += headerHeight + 16f
            } else {
                canvas.drawText(
                    "${report.title} (${report.date})",
                    MARGIN,
                    yCursor + 12f,
                    footerPaint
                )
                yCursor += 20f
                canvas.drawLine(MARGIN, yCursor, PAGE_WIDTH - MARGIN, yCursor, dividerPaint)
                yCursor += 12f
            }

            val maxContentY = PAGE_HEIGHT - MARGIN - 28f
            val availableHeight = maxContentY - yCursor

            // Determine how many lines fit cleanly on this page
            val startY = fullLayout.getLineTop(currentLine)
            var endLine = currentLine
            while (endLine < totalLines) {
                val lineBottom = fullLayout.getLineBottom(endLine) - startY
                if (lineBottom > availableHeight && endLine > currentLine) {
                    break
                }
                endLine++
            }

            // Draw slice of lines
            canvas.save()
            canvas.translate(MARGIN, yCursor - startY)
            canvas.clipRect(0, startY, contentWidth, fullLayout.getLineBottom(endLine - 1))
            fullLayout.draw(canvas)
            canvas.restore()

            // Footer
            val footerY = PAGE_HEIGHT - MARGIN + 6f
            canvas.drawLine(MARGIN, footerY - 14f, PAGE_WIDTH - MARGIN, footerY - 14f, dividerPaint)
            canvas.drawText(
                "স্বাধীন উৎপাদনশীলতা প্রতিবেদন (সরকারি নথি নয়)  •  পৃষ্ঠা $pageNumber",
                MARGIN,
                footerY,
                footerPaint
            )

            document.finishPage(page)
            currentLine = endLine
            pageNumber++
        }

        try {
            writer(document)
        } finally {
            document.close()
        }
    }
}
