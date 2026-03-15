package com.lockin.app.data.repository

import com.lockin.app.data.local.database.dao.EnergyScoreDao
import com.lockin.app.data.local.models.EnergyScoreEntity
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class EnergyScoreRepository @Inject constructor(
    private val energyScoreDao: EnergyScoreDao
) {
    suspend fun getEnergyScoreForDate(userId: String, date: LocalDate = LocalDate.now()): EnergyScoreEntity? {
        return energyScoreDao.getByDate(userId, date)
    }

    suspend fun saveEnergyScore(entity: EnergyScoreEntity) = withContext(Dispatchers.IO) {
        energyScoreDao.upsert(entity)
    }
}
