package com.example.data.repository

import com.example.data.local.dao.HoldingDao
import com.example.data.local.entity.HoldingRecordEntity
import com.example.domain.analyzer.FinancialSummary
import com.example.domain.analyzer.RevenueAnalysisEngine
import com.example.domain.sample.SampleDataGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RevenueRepository(private val holdingDao: HoldingDao) {

    val allHoldings: Flow<List<HoldingRecordEntity>> = holdingDao.getAllHoldings()

    val inconsistentHoldings: Flow<List<HoldingRecordEntity>> = holdingDao.getInconsistentHoldings()

    val financialSummary: Flow<FinancialSummary> = allHoldings.map { holdings ->
        RevenueAnalysisEngine.calculateSummary(holdings)
    }

    suspend fun getHoldingById(holdingId: String): HoldingRecordEntity? {
        return holdingDao.getHoldingById(holdingId)
    }

    suspend fun insertHoldings(holdings: List<HoldingRecordEntity>) {
        holdingDao.deleteAll()
        holdingDao.insertAll(holdings)
    }

    suspend fun appendHoldings(holdings: List<HoldingRecordEntity>) {
        holdingDao.insertAll(holdings)
    }

    suspend fun updateHolding(holding: HoldingRecordEntity) {
        holdingDao.update(holding)
    }

    suspend fun clearAllHoldings() {
        holdingDao.deleteAll()
    }

    suspend fun loadSyntheticSampleData() {
        val samples = SampleDataGenerator.getSampleHoldings()
        holdingDao.deleteAll()
        holdingDao.insertAll(samples)
    }

    suspend fun getHoldingsCount(): Int {
        return holdingDao.getCount()
    }
}
