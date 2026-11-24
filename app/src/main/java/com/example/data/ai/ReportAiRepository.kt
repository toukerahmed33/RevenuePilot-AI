package com.example.data.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "ReportAiRepository"

sealed interface AiSummaryUiState {
    data object Idle : AiSummaryUiState
    data object Loading : AiSummaryUiState
    data class Streaming(val text: String) : AiSummaryUiState
    data class Success(val text: String) : AiSummaryUiState
    data class Error(val message: String) : AiSummaryUiState
}

class ReportAiRepository {

    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    /**
     * Streams a concise Bangla executive summary from already-computed deterministic report content.
     * Never alters or fabricates financial figures.
     */
    fun streamExecutiveSummary(
        reportTitle: String,
        deterministicReportBody: String
    ): Flow<AiSummaryUiState> = flow {
        emit(AiSummaryUiState.Loading)
        val prompt = """
            আপনি একজন বাংলাদেশি পৌর রাজস্ব ব্যবস্থাপনা সহায়ক।
            নিচের প্রতিবেদনটি সম্পূর্ণ প্রোগ্রামিং লজিকে গণনা করা হয়েছে। কোনো সংখ্যা বা আর্থিক হিসাব পরিবর্তন বা অনুমান করবেন না।
            প্রদত্ত তথ্যের ভিত্তিতে সুপারভাইজারের জন্য ৩ থেকে ৫টি স্পষ্ট বাংলা বুলেট পয়েন্টে একটি সংক্ষিপ্ত ও পেশাদার নির্বাহী সারসংক্ষেপ (Executive Summary) ও করণীয় সুপারিশ লিখুন।
            নিরীক্ষা অমিলগুলোকে সর্বদা 'ডেটা এন্ট্রি বা খতিয়ান যাচাইয়ের প্রয়োজন' হিসেবে উল্লেখ করবেন, কখনোই অসদাচরণ বা জালিয়াতি বলবেন না।

            প্রতিবেদনের ধরন: $reportTitle
            প্রতিবেদনের উপাত্ত:
            $deterministicReportBody
        """.trimIndent()

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                model.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(AiSummaryUiState.Streaming(accumulated.toString()))
                    }
                }
            }
            val finalOutput = accumulated.toString().trim()
            if (finalOutput.isNotEmpty()) {
                emit(AiSummaryUiState.Success(finalOutput))
            } else {
                emit(AiSummaryUiState.Error("মডেল থেকে কোনো সারসংক্ষেপ পাওয়া যায়নি। অনুগ্রহ করে আবার চেষ্টা করুন।"))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI summary timed out", e)
            emit(AiSummaryUiState.Error("সময়সীমা শেষ হয়েছে (৬০ সেকেন্ড)। ইন্টারনেট সংযোগ যাচাই করে পুনরায় চেষ্টা করুন।"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI summary generation failed", e)
            emit(AiSummaryUiState.Error(e.localizedMessage ?: "AI সারসংক্ষেপ তৈরিতে ত্রুটি হয়েছে।"))
        }
    }
}
