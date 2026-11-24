package com.example.domain.sample

import com.example.data.local.entity.HoldingRecordEntity
import com.example.data.local.entity.InspectionNoteEntity
import com.example.data.local.entity.TaskEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SampleDataGenerator {

    private fun dateWithOffset(daysOffset: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(Date(System.currentTimeMillis() + daysOffset * 24 * 60 * 60 * 1000L))
    }

    /**
     * Generates 20 realistic municipal holding records.
     * Contains controlled audit anomalies for demonstration:
     * - Record #4 has duplicate holding ID (W07-H1002) with Record #2
     * - Record #8 has source calculation discrepancy (Recorded total 45,000 instead of 15,000 + 40,000 = 55,000)
     * - Record #14 has missing previous arrears data
     */
    fun getSampleHoldings(): List<HoldingRecordEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            HoldingRecordEntity(
                id = 1,
                holdingId = "W07-H1001",
                ownerName = "মুহাম্মদ রফিকুল ইসলাম",
                ward = "ওয়ার্ড-০৭",
                area = "মিরপুর-১১, ব্লক-সি",
                previousArrears = 12000.0,
                currentDemand = 24000.0,
                totalPayable = 36000.0,
                amountCollected = 36000.0,
                remainingBalance = 0.0,
                sourceRowNumber = 2,
                rawCsvRow = "W07-H1001,মুহাম্মদ রফিকুল ইসলাম,ওয়ার্ড-০৭,12000,24000,36000,36000,0",
                hasCalculationMismatch = false,
                hasMissingValues = false,
                isDuplicate = false,
                mismatchNotes = "পরিশোধিত ও নিয়মিত হোল্ডিং",
                updatedAt = now
            ),
            HoldingRecordEntity(
                id = 2,
                holdingId = "W07-H1002",
                ownerName = "আনোয়ারা বেগম",
                ward = "ওয়ার্ড-০৭",
                area = "মিরপুর-১০, রোড-৪",
                previousArrears = 35000.0,
                currentDemand = 18000.0,
                totalPayable = 53000.0,
                amountCollected = 20000.0,
                remainingBalance = 33000.0,
                sourceRowNumber = 3,
                rawCsvRow = "W07-H1002,আনোয়ারা বেগম,ওয়ার্ড-০৭,35000,18000,53000,20000,33000",
                hasCalculationMismatch = false,
                hasMissingValues = false,
                isDuplicate = true, // Duplicate ID with record 4
                mismatchNotes = "দ্বৈত হোল্ডিং: একই হোল্ডিং আইডি একাধিক সারিতে পাওয়া গেছে",
                updatedAt = now
            ),
            HoldingRecordEntity(
                id = 3,
                holdingId = "W07-H1003",
                ownerName = "মেসার্স ঢাকা ফেব্রিক্স",
                ward = "ওয়ার্ড-০৭",
                area = "পল্লবী বাণিজ্যিক এলাকা",
                previousArrears = 85000.0,
                currentDemand = 45000.0,
                totalPayable = 130000.0,
                amountCollected = 75000.0,
                remainingBalance = 55000.0,
                sourceRowNumber = 4,
                rawCsvRow = "W07-H1003,মেসার্স ঢাকা ফেব্রিক্স,ওয়ার্ড-০৭,85000,45000,130000,75000,55000",
                hasCalculationMismatch = false,
                hasMissingValues = false,
                isDuplicate = false,
                mismatchNotes = "",
                updatedAt = now
            ),
            HoldingRecordEntity(
                id = 4,
                holdingId = "W07-H1002", // Intentional duplicate ID for testing
                ownerName = "আনোয়ারা বেগম (দ্বিতীয় অংশ)",
                ward = "ওয়ার্ড-০৭",
                area = "মিরপুর-১০, রোড-৪",
                previousArrears = 10000.0,
                currentDemand = 12000.0,
                totalPayable = 22000.0,
                amountCollected = 10000.0,
                remainingBalance = 12000.0,
                sourceRowNumber = 5,
                rawCsvRow = "W07-H1002,আনোয়ারা বেগম (দ্বিতীয় অংশ),ওয়ার্ড-০৭,10000,12000,22000,10000,12000",
                hasCalculationMismatch = false,
                hasMissingValues = false,
                isDuplicate = true,
                mismatchNotes = "দ্বৈত হোল্ডিং: একই হোল্ডিং আইডি একাধিক সারিতে পাওয়া গেছে",
                updatedAt = now
            ),
            HoldingRecordEntity(
                id = 5,
                holdingId = "W07-H1005",
                ownerName = "হাজী দেলোয়ার হোসেন",
                ward = "ওয়ার্ড-০৭",
                area = "মিরপুর-১১, অ্যাভিনিউ-১",
                previousArrears = 0.0,
                currentDemand = 28000.0,
                totalPayable = 28000.0,
                amountCollected = 28000.0,
                remainingBalance = 0.0,
                sourceRowNumber = 6,
                rawCsvRow = "W07-H1005,হাজী দেলোয়ার হোসেন,ওয়ার্ড-০৭,0,28000,28000,28000,0",
                hasCalculationMismatch = false,
                hasMissingValues = false,
                isDuplicate = false,
                mismatchNotes = "সম্পূর্ণ পরিশোধিত",
                updatedAt = now
            ),
            HoldingRecordEntity(
                id = 6,
                holdingId = "W07-H1006",
                ownerName = "ডাঃ সিরাজুল ইসলাম",
                ward = "ওয়ার্ড-০৭",
                area = "কালশী মেইন রোড",
                previousArrears = 22000.0,
                currentDemand = 32000.0,
                totalPayable = 54000.0,
                amountCollected = 15000.0,
                remainingBalance = 39000.0,
                sourceRowNumber = 7,
                rawCsvRow = "W07-H1006,ডাঃ সিরাজুল ইসলাম,ওয়ার্ড-০৭,22000,32000,54000,15000,39000",
                hasCalculationMismatch = false,
                hasMissingValues = false,
                isDuplicate = false,
                mismatchNotes = "",
                updatedAt = now
            ),
            HoldingRecordEntity(
                id = 7,
                holdingId = "W07-H1007",
                ownerName = "সুলতানা কামাল",
                ward = "ওয়ার্ড-০৭",
                area = "মিরপুর-১১, ব্লক-ডি",
                previousArrears = 48000.0,
                currentDemand = 22000.0,
                totalPayable = 70000.0,
                amountCollected = 35000.0,
                remainingBalance = 35000.0,
                sourceRowNumber = 8,
                rawCsvRow = "W07-H1007,সুলতানা কামাল,ওয়ার্ড-০৭,48000,22000,70000,35000,35000",
                hasCalculationMismatch = false,
                hasMissingValues = false,
                isDuplicate = false,
                mismatchNotes = "",
                updatedAt = now
            ),
            HoldingRecordEntity(
                id = 8,
                holdingId = "W07-H1008", // Intentional calculation discrepancy in source sheet
                ownerName = "কবীর আহমদ",
                ward = "ওয়ার্ড-০৭",
                area = "মিরপুর সেকশন-৬",
                previousArrears = 15000.0,
                currentDemand = 40000.0,
                totalPayable = 45000.0, // Source typo: should be 55,000 (15,000 + 40,000)
                amountCollected = 20000.0,
                remainingBalance = 25000.0,
                sourceRowNumber = 9,
                rawCsvRow = "W07-H1008,কবীর আহমদ,ওয়ার্ড-০৭,15000,40000,45000,20000,25000",
                hasCalculationMismatch = true,
                hasMissingValues = false,
                isDuplicate = false,
                mismatchNotes = "দাবি অমিল: উৎসে ৳45,000, কিন্তু গাণিতিক যোগফল (বকেয়া+চলতি) = ৳55,000",
                updatedAt = now
            ),
            HoldingRecordEntity(
                id = 9,
                holdingId = "W07-H1009",
                ownerName = "নিউ গ্রীন ফার্মাসিউটিক্যালস",
                ward = "ওয়ার্ড-০৭",
                area = "মিরপুর শিল্পাঞ্চল",
                previousArrears = 110000.0,
                currentDemand = 65000.0,
                totalPayable = 175000.0,
                amountCollected = 100000.0,
                remainingBalance = 75000.0,
                sourceRowNumber = 10,
                rawCsvRow = "W07-H1009,নিউ গ্রীন ফার্মাসিউটিক্যালস,ওয়ার্ড-০৭,110000,65000,175000,100000,75000",
                hasCalculationMismatch = false,
                hasMissingValues = false,
                isDuplicate = false,
                mismatchNotes = "",
                updatedAt = now
            ),
            HoldingRecordEntity(
                id = 10,
                holdingId = "W07-H1010",
                ownerName = "ফারহানা ইয়াসমিন",
                ward = "ওয়ার্ড-০৭",
                area = "পল্লবী আবাসিক এলাকা",
                previousArrears = 6000.0,
                currentDemand = 16000.0,
                totalPayable = 22000.0,
                amountCollected = 22000.0,
                remainingBalance = 0.0,
                sourceRowNumber = 11,
                rawCsvRow = "W07-H1010,ফারহানা ইয়াসমিন,ওয়ার্ড-০৭,6000,16000,22000,22000,0",
                hasCalculationMismatch = false,
                hasMissingValues = false,
                isDuplicate = false,
                mismatchNotes = "পরিশোধিত",
                updatedAt = now
            ),
            HoldingRecordEntity(
                id = 11,
                holdingId = "W07-H1011",
                ownerName = "জাহিদুল আলম",
                ward = "ওয়ার্ড-০৭",
                area = "মিরপুর-১১, ব্লক-এ",
                previousArrears = 25000.0,
                currentDemand = 20000.0,
                totalPayable = 45000.0,
                amountCollected = 15000.0,
                remainingBalance = 30000.0,
                sourceRowNumber = 12,
                rawCsvRow = "W07-H1011,জাহিদুল আলম,ওয়ার্ড-০৭,25000,20000,45000,15000,30000",
                hasCalculationMismatch = false,
                hasMissingValues = false,
                isDuplicate = false,
                mismatchNotes = "",
                updatedAt = now
            ),
            HoldingRecordEntity(
                id = 12,
                holdingId = "W07-H1012",
                ownerName = "বেঙ্গল প্রিন্টিং প্রেস",
                ward = "ওয়ার্ড-০৭",
                area = "মিরপুর মেইন রোড",
                previousArrears = 92000.0,
                currentDemand = 38000.0,
                totalPayable = 130000.0,
                amountCollected = 50000.0,
                remainingBalance = 80000.0,
                sourceRowNumber = 13,
                rawCsvRow = "W07-H1012,বেঙ্গল প্রিন্টিং প্রেস,ওয়ার্ড-০৭,92000,38000,130000,50000,80000",
                hasCalculationMismatch = false,
                hasMissingValues = false,
                isDuplicate = false,
                mismatchNotes = "",
                updatedAt = now
            ),
            HoldingRecordEntity(
                id = 13,
                holdingId = "W07-H1013",
                ownerName = "মোস্তাফিজুর রহমান",
                ward = "ওয়ার্ড-০৭",
                area = "কালশী রোড-৩",
                previousArrears = 14000.0,
                currentDemand = 19000.0,
                totalPayable = 33000.0,
                amountCollected = 33000.0,
                remainingBalance = 0.0,
                sourceRowNumber = 14,
                rawCsvRow = "W07-H1013,মোস্তাফিজুর রহমান,ওয়ার্ড-০৭,14000,19000,33000,33000,0",
                hasCalculationMismatch = false,
                hasMissingValues = false,
                isDuplicate = false,
                mismatchNotes = "পরিশোধিত",
                updatedAt = now
            ),
            HoldingRecordEntity(
                id = 14,
                holdingId = "W07-H1014", // Intentional missing previous arrears
                ownerName = "আনোয়ার সাদাত",
                ward = "ওয়ার্ড-০৭",
                area = "মিরপুর-১১, ব্লক-ই",
                previousArrears = 0.0,
                currentDemand = 25000.0,
                totalPayable = 25000.0,
                amountCollected = 0.0,
                remainingBalance = 25000.0,
                sourceRowNumber = 15,
                rawCsvRow = "W07-H1014,আনোয়ার সাদাত,ওয়ার্ড-০৭,,25000,25000,0,25000",
                hasCalculationMismatch = false,
                hasMissingValues = true,
                isDuplicate = false,
                mismatchNotes = "অসম্পূর্ণ মান: পূর্বের বকেয়া অনুদ্ঘাটিত",
                updatedAt = now
            ),
            HoldingRecordEntity(
                id = 15,
                holdingId = "W07-H1015",
                ownerName = "মেসার্স হক ট্রেডার্স",
                ward = "ওয়ার্ড-০৭",
                area = "পল্লবী শপিং কমপ্লেক্স",
                previousArrears = 64000.0,
                currentDemand = 42000.0,
                totalPayable = 106000.0,
                amountCollected = 50000.0,
                remainingBalance = 56000.0,
                sourceRowNumber = 16,
                rawCsvRow = "W07-H1015,মেসার্স হক ট্রেডার্স,ওয়ার্ড-০৭,64000,42000,106000,50000,56000",
                hasCalculationMismatch = false,
                hasMissingValues = false,
                isDuplicate = false,
                mismatchNotes = "",
                updatedAt = now
            ),
            HoldingRecordEntity(
                id = 16,
                holdingId = "W07-H1016",
                ownerName = "শাহীন আক্তার",
                ward = "ওয়ার্ড-০৭",
                area = "মিরপুর-১০, সেকশন-সি",
                previousArrears = 8000.0,
                currentDemand = 15000.0,
                totalPayable = 23000.0,
                amountCollected = 15000.0,
                remainingBalance = 8000.0,
                sourceRowNumber = 17,
                rawCsvRow = "W07-H1016,শাহীন আক্তার,ওয়ার্ড-০৭,8000,15000,23000,15000,8000",
                hasCalculationMismatch = false,
                hasMissingValues = false,
                isDuplicate = false,
                mismatchNotes = "",
                updatedAt = now
            ),
            HoldingRecordEntity(
                id = 17,
                holdingId = "W07-H1017",
                ownerName = "আলহাজ্ব আকরাম হোসেন",
                ward = "ওয়ার্ড-০৭",
                area = "মিরপুর-১১, রোড-৭",
                previousArrears = 40000.0,
                currentDemand = 30000.0,
                totalPayable = 70000.0,
                amountCollected = 70000.0,
                remainingBalance = 0.0,
                sourceRowNumber = 18,
                rawCsvRow = "W07-H1017,আলহাজ্ব আকরাম হোসেন,ওয়ার্ড-০৭,40000,30000,70000,70000,0",
                hasCalculationMismatch = false,
                hasMissingValues = false,
                isDuplicate = false,
                mismatchNotes = "সম্পূর্ণ পরিশোধিত",
                updatedAt = now
            ),
            HoldingRecordEntity(
                id = 18,
                holdingId = "W07-H1018",
                ownerName = "রোকসানা পারভীন",
                ward = "ওয়ার্ড-০৭",
                area = "কালশী হাউজিং",
                previousArrears = 18000.0,
                currentDemand = 17000.0,
                totalPayable = 35000.0,
                amountCollected = 20000.0,
                remainingBalance = 15000.0,
                sourceRowNumber = 19,
                rawCsvRow = "W07-H1018,রোকসানা পারভীন,ওয়ার্ড-০৭,18000,17000,35000,20000,15000",
                hasCalculationMismatch = false,
                hasMissingValues = false,
                isDuplicate = false,
                mismatchNotes = "",
                updatedAt = now
            ),
            HoldingRecordEntity(
                id = 19,
                holdingId = "W07-H1019",
                ownerName = "গোল্ডেন প্লাজা মার্কেট কমিটি",
                ward = "ওয়ার্ড-০৭",
                area = "মিরপুর-১১ গোলচত্বর",
                previousArrears = 150000.0,
                currentDemand = 80000.0,
                totalPayable = 230000.0,
                amountCollected = 120000.0,
                remainingBalance = 110000.0,
                sourceRowNumber = 20,
                rawCsvRow = "W07-H1019,গোল্ডেন প্লাজা মার্কেট কমিটি,ওয়ার্ড-০৭,150000,80000,230000,120000,110000",
                hasCalculationMismatch = false,
                hasMissingValues = false,
                isDuplicate = false,
                mismatchNotes = "",
                updatedAt = now
            ),
            HoldingRecordEntity(
                id = 20,
                holdingId = "W07-H1020",
                ownerName = "মাহমুদ হাসান",
                ward = "ওয়ার্ড-০৭",
                area = "পল্লবী ব্লক-এফ",
                previousArrears = 10000.0,
                currentDemand = 18000.0,
                totalPayable = 28000.0,
                amountCollected = 28000.0,
                remainingBalance = 0.0,
                sourceRowNumber = 21,
                rawCsvRow = "W07-H1020,মাহমুদ হাসান,ওয়ার্ড-০৭,10000,18000,28000,28000,0",
                hasCalculationMismatch = false,
                hasMissingValues = false,
                isDuplicate = false,
                mismatchNotes = "সম্পূর্ণ পরিশোধিত",
                updatedAt = now
            )
        )
    }

    /**
     * Generates 10 realistic municipal supervisory tasks.
     */
    fun getSampleTasks(): List<TaskEntity> {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        val todayStr = dateWithOffset(0)
        val tomorrowStr = dateWithOffset(1)
        val yesterdayStr = dateWithOffset(-1)
        val dayAfterThree = dateWithOffset(3)
        val dayAfterFour = dateWithOffset(4)

        return listOf(
            TaskEntity(
                id = 1,
                title = "W07-H1003 বাণিজ্যিক ইউনিটের অতিরিক্ত সম্প্রসারণ যাচাই",
                description = "মেসার্স ঢাকা ফেব্রিক্সের ট্রেড লাইসেন্স এলাকা ও ভবনের ফ্লোর এলাকা পরিদর্শন।",
                holdingId = "W07-H1003",
                dueDate = todayStr,
                priority = "HIGH",
                category = "INSPECTION",
                status = "PENDING",
                notes = "সহকারী রাজস্ব কর্মকর্তার সাথে সকাল ১১টায় পরিদর্শন করতে হবে।",
                createdAt = now - (2 * dayMs)
            ),
            TaskEntity(
                id = 2,
                title = "W07-H1008 এর গাণিতিক দাবির অমিল পুনরায় নিরীক্ষা",
                description = "উৎসের স্প্রেডশিটে মোট প্রদেয় ৪৫,০০০ লেখা আছে কিন্তু বকেয়া ও চলতি মিলিয়ে ৫৫,০০০ টাকা হয়।",
                holdingId = "W07-H1008",
                dueDate = todayStr,
                priority = "HIGH",
                category = "DEMAND_NOTICE",
                status = "IN_PROGRESS",
                notes = "হোল্ডিং খতিয়ান রেজিস্টার যাচাই করে সঠিক দাবি নোটিশ প্রস্তুতকরণ।",
                createdAt = now - dayMs
            ),
            TaskEntity(
                id = 3,
                title = "W07-H1002 এর দ্বৈত হোল্ডিং অ্যান্ট্রি নিরসন",
                description = "একই হোল্ডিং নম্বরে দুটি পৃথক অংশ লিপিবদ্ধ হয়েছে কিনা বা ভুল এন্ট্রি হয়েছে কিনা যাচাই।",
                holdingId = "W07-H1002",
                dueDate = todayStr,
                priority = "MEDIUM",
                category = "HEARING",
                status = "PENDING",
                notes = "মালিক আনোয়ারা বেগমের আবেদনপত্র খতিয়ান শাখা থেকে সংগ্রহ।",
                createdAt = now - (3 * dayMs)
            ),
            TaskEntity(
                id = 4,
                title = "W07-H1014 এর শূন্য বকেয়া রেকর্ডের পূর্ববর্তী বছর চেক",
                description = "২০২৪-২৫ অর্থবছরের লেজার বই থেকে পূর্ববর্তী বকেয়া রেকর্ড খুঁজে বের করা।",
                holdingId = "W07-H1014",
                dueDate = tomorrowStr,
                priority = "MEDIUM",
                category = "ASSESSMENT",
                status = "PENDING",
                notes = "রেকর্ড রুমের ক্লার্কের সাথে সমন্বয় করে এন্ট্রি আপডেট।",
                createdAt = now - dayMs
            ),
            TaskEntity(
                id = 5,
                title = "W07-H1019 গোল্ডেন প্লাজা মার্কেটের দ্বিতীয় কিস্তির নোটিশ",
                description = "মার্কেট কমিটির কাছে অবশিষ্ট ১,১০,০০০ টাকা পরিশোধের স্মারক পত্র প্রস্তুত।",
                holdingId = "W07-H1019",
                dueDate = tomorrowStr,
                priority = "HIGH",
                category = "DEMAND_NOTICE",
                status = "PENDING",
                notes = "মার্কেট কমিটির সভাপতির সাথে ফোনে প্রাথমিক আলোচনা সম্পন্ন।",
                createdAt = now - (4 * dayMs)
            ),
            TaskEntity(
                id = 6,
                title = "W07-H1001 ও W07-H1005 এর পূর্ণ আদায় রশিদ অনুমোদন",
                description = "অনলাইন ব্যাংক স্টেটমেন্টের সাথে ক্যাশ রশিদের টাকা নিশ্চিতকরণ।",
                holdingId = "W07-H1001",
                dueDate = yesterdayStr,
                priority = "LOW",
                category = "GENERAL",
                status = "COMPLETED",
                notes = "সকল চালান ও ব্যাংক ক্রেডিট ভাউচার মিলিয়ে সম্পন্ন করা হয়েছে।",
                completedAt = now - (12 * 60 * 60 * 1000L),
                createdAt = now - (3 * dayMs)
            ),
            TaskEntity(
                id = 7,
                title = "মিরপুর-১১ ব্লকের বকেয়া হোল্ডিংগুলোর তালিকা প্রিন্ট",
                description = "মাঠ কর্মীদের জন্য সাপ্তাহিক ফলো-আপ শিট তৈরি ও বিতরণ।",
                holdingId = "",
                dueDate = todayStr,
                priority = "LOW",
                category = "FOLLOW_UP",
                status = "COMPLETED",
                notes = "৩ জন সহকারী কর্মকর্তার কাছে প্রিন্ট কপি হস্তান্তর।",
                completedAt = now - (4 * 60 * 60 * 1000L),
                createdAt = now - (2 * dayMs)
            ),
            TaskEntity(
                id = 8,
                title = "W07-H1012 বেঙ্গল প্রিন্টিং প্রেসের শুনানি বৈঠক",
                description = "মালিকের আবেদনের প্রেক্ষিতে বাণিজ্যিক রেট সম্পর্কিত আপত্তি শুনানি।",
                holdingId = "W07-H1012",
                dueDate = dayAfterThree,
                priority = "MEDIUM",
                category = "HEARING",
                status = "PENDING",
                notes = "শুনানি কক্ষ-৩ এ দুপুর আড়াইটায়।",
                createdAt = now - dayMs
            ),
            TaskEntity(
                id = 9,
                title = "ওয়ার্ড-০৭ এর মাসিক আদায় অগ্রগতি রিপোর্ট প্রণয়ন",
                description = "মাসিক টার্গেটের বিপরীতে আদায়ের শতাংশ হিসাব করে সংক্ষিপ্ত সারসংক্ষেপ তৈরি।",
                holdingId = "",
                dueDate = dayAfterFour,
                priority = "HIGH",
                category = "GENERAL",
                status = "PENDING",
                notes = "জোনাল এক্সিকিউটিভ অফিসারের নিকট উপস্থাপনের জন্য।",
                createdAt = now
            ),
            TaskEntity(
                id = 10,
                title = "দৈনিক কার্যবিবরণী ও পরিদর্শন খসড়া সংরক্ষণ",
                description = "আজকের পরিদর্শন করা হোল্ডিংগুলোর খসড়া নোট চূড়ান্ত করা।",
                holdingId = "",
                dueDate = todayStr,
                priority = "MEDIUM",
                category = "INSPECTION",
                status = "IN_PROGRESS",
                notes = "সফটওয়্যারের রিপোর্ট জেনারেটর ব্যবহার করে রপ্তানি করা হবে।",
                createdAt = now
            )
        )
    }

    /**
     * Generates 5 realistic municipal field inspection notes.
     */
    fun getSampleInspectionNotes(): List<InspectionNoteEntity> {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        val todayStr = dateWithOffset(0)
        val yesterdayStr = dateWithOffset(-1)
        val twoDaysAgoStr = dateWithOffset(-2)

        return listOf(
            InspectionNoteEntity(
                id = 1,
                inspectionRef = "INSP-2026-081",
                holdingRef = "W07-H1003",
                assignedArea = "ওয়ার্ড-০৭, পল্লবী বাণিজ্যিক এলাকা",
                inspectionDate = todayStr,
                verificationStatus = "UNAUTHORIZED_COMMERCIAL",
                observations = "ভবনের ৩য় তলা আবাসিক হিসেবে অনুমোদিত থাকলেও সেখানে মেসার্স ঢাকা ফেব্রিক্সের গুদাম ও কাটিং সেকশন পরিচালিত হচ্ছে। বাণিজ্যিক কর হার প্রয়োগের যৌক্তিকতা দৃশ্যমান।",
                followUpAction = "বাণিজ্যিক হার পুনর্নির্ধারণ নোটিশ জারি ও মালিককে শুনানিতে ডাকার সুপারিশ।",
                notes = "সাইট ফটোগ্রাফ ও ট্রেড লাইসেন্স কপি সংগৃহীত।",
                isDraft = false,
                updatedAt = now,
                createdAt = now - dayMs
            ),
            InspectionNoteEntity(
                id = 2,
                inspectionRef = "INSP-2026-082",
                holdingRef = "W07-H1009",
                assignedArea = "ওয়ার্ড-০৭, মিরপুর শিল্পাঞ্চল",
                inspectionDate = yesterdayStr,
                verificationStatus = "EXTENSION_FOUND",
                observations = "মূল কারখানার উত্তর পাশে নতুন দুই তলা আরসিসি শেড নির্মাণ করা হয়েছে যা পূর্বের মূল্যায়ন নথিতে অন্তর্ভুক্ত নেই (প্রায় ২,২০০ বর্গফুট)।",
                followUpAction = "অতিরিক্ত পরিমাপের ভিত্তিতে সংশোধিত মূল্যায়ন প্রস্তাব দাখিল।",
                notes = "বিল্ডিং প্ল্যান যাচাইয়ের জন্য ইঞ্জিনিয়ারিং শাখায় চিঠি পাঠানো হয়েছে।",
                isDraft = false,
                updatedAt = now - dayMs,
                createdAt = now - (2 * dayMs)
            ),
            InspectionNoteEntity(
                id = 3,
                inspectionRef = "INSP-2026-083",
                holdingRef = "W07-H1005",
                assignedArea = "ওয়ার্ড-০৭, মিরপুর-১১, অ্যাভিনিউ-১",
                inspectionDate = twoDaysAgoStr,
                verificationStatus = "VERIFIED",
                observations = "ভবনের ব্যবহার সম্পূর্ণ আবাসিক। মূল্যায়নে উল্লেখিত পরিমাপ ও ফ্লোর সংখ্যার সাথে মাঠের বাস্তবতার পূর্ণ মিল পাওয়া গেছে। কর সম্পূর্ণ পরিশোধিত।",
                followUpAction = "নথি চূড়ান্ত ও হালনাগাদ সংরক্ষণ।",
                notes = "নিয়মিত করদাতা হিসেবে প্রশংসনীয় রেকর্ড।",
                isDraft = false,
                updatedAt = now - (2 * dayMs),
                createdAt = now - (3 * dayMs)
            ),
            InspectionNoteEntity(
                id = 4,
                inspectionRef = "INSP-2026-084",
                holdingRef = "W07-H1012",
                assignedArea = "ওয়ার্ড-০৭, মিরপুর মেইন রোড",
                inspectionDate = todayStr,
                verificationStatus = "MISMATCH_MEASUREMENT",
                observations = "প্রেসের নিচতলার আয়তন নথিতে ১,২০০ বর্গফুট উল্লেখ থাকলেও বাস্তবে সামনের অংশ সম্প্রসারণ করে ১,৬৫০ বর্গফুট ব্যবহৃত হচ্ছে।",
                followUpAction = "যৌথ পুনঃপরিমাপের জন্য সার্ভেয়ার নিয়োগের সুপারিশ।",
                notes = "মালিকের প্রতিনিধি উপস্থিতিতে স্বাক্ষর গ্রহণ করা হয়েছে।",
                isDraft = true, // Draft note for offline editing demo
                updatedAt = now,
                createdAt = now
            ),
            InspectionNoteEntity(
                id = 5,
                inspectionRef = "INSP-2026-085",
                holdingRef = "W07-H1008",
                assignedArea = "ওয়ার্ড-০৭, মিরপুর সেকশন-৬",
                inspectionDate = todayStr,
                verificationStatus = "INCOMPLETE",
                observations = "হোল্ডিং গেট তালাবদ্ধ থাকায় অভ্যন্তরীণ পরিমাপ সম্ভব হয়নি। তত্ত্বাবধায়কের সাথে ফোনে যোগাযোগ করা হয়েছে।",
                followUpAction = "আগামী বৃহস্পতিবার সকাল ১০টায় পুনঃপরিদর্শনের সময় নির্ধারণ।",
                notes = "স্থানীয় প্রতিবেশীদের সাথে প্রাথমিক কথা বলে খসড়া রাখা হলো।",
                isDraft = true,
                updatedAt = now,
                createdAt = now
            )
        )
    }

    /**
     * Pre-packaged raw CSV text that users can view, copy, or reload in the CSV Analyzer!
     */
    fun getSampleCsvRaw(): String {
        return """
Holding ID,Owner Name,Ward,Previous Arrears,Current Demand,Total Payable,Amount Collected,Remaining Balance
W07-H1001,মুহাম্মদ রফিকুল ইসলাম,ওয়ার্ড-০৭,12000,24000,36000,36000,0
W07-H1002,আনোয়ারা বেগম,ওয়ার্ড-০৭,35000,18000,53000,20000,33000
W07-H1003,মেসার্স ঢাকা ফেব্রিক্স,ওয়ার্ড-০৭,85000,45000,130000,75000,55000
W07-H1002,আনোয়ারা বেগম (দ্বিতীয় অংশ),ওয়ার্ড-০৭,10000,12000,22000,10000,12000
W07-H1005,হাজী দেলোয়ার হোসেন,ওয়ার্ড-০৭,0,28000,28000,28000,0
W07-H1006,ডাঃ সিরাজুল ইসলাম,ওয়ার্ড-০৭,22000,32000,54000,15000,39000
W07-H1007,সুলতানা কামাল,ওয়ার্ড-০৭,48000,22000,70000,35000,35000
W07-H1008,কবীর আহমদ,ওয়ার্ড-০৭,15000,40000,45000,20000,25000
W07-H1009,নিউ গ্রীন ফার্মাসিউটিক্যালস,ওয়ার্ড-০৭,110000,65000,175000,100000,75000
W07-H1010,ফারহানা ইয়াসমিন,ওয়ার্ড-০৭,6000,16000,22000,22000,0
W07-H1011,জাহিদুল আলম,ওয়ার্ড-০৭,25000,20000,45000,15000,30000
W07-H1012,বেঙ্গল প্রিন্টিং প্রেস,ওয়ার্ড-০৭,92000,38000,130000,50000,80000
W07-H1013,মোস্তাফিজুর রহমান,ওয়ার্ড-০৭,14000,19000,33000,33000,0
W07-H1014,আনোয়ার সাদাত,ওয়ার্ড-০৭,,25000,25000,0,25000
W07-H1015,মেসার্স হক ট্রেডার্স,ওয়ার্ড-০৭,64000,42000,106000,50000,56000
W07-H1016,শাহীন আক্তার,ওয়ার্ড-০৭,8000,15000,23000,15000,8000
W07-H1017,আলহাজ্ব আকরাম হোসেন,ওয়ার্ড-০৭,40000,30000,70000,70000,0
W07-H1018,রোকসানা পারভীন,ওয়ার্ড-০৭,18000,17000,35000,20000,15000
W07-H1019,গোল্ডেন প্লাজা মার্কেট কমিটি,ওয়ার্ড-০৭,150000,80000,230000,120000,110000
W07-H1020,মাহমুদ হাসান,ওয়ার্ড-০৭,10000,18000,28000,28000,0
        """.trimIndent()
    }
}
