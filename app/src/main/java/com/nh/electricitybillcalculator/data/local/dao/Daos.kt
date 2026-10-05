package com.nh.electricitybillcalculator.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nh.electricitybillcalculator.data.local.entity.ApplianceEntity
import com.nh.electricitybillcalculator.data.local.entity.BillCalculationEntity
import com.nh.electricitybillcalculator.data.local.entity.MeterReadingEntity
import com.nh.electricitybillcalculator.data.local.entity.TariffSlabEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillCalculationDao {
    @Query("SELECT * FROM bill_calculations ORDER BY dateMillis DESC")
    fun getAllBills(): Flow<List<BillCalculationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillCalculationEntity): Long

    @Delete
    suspend fun deleteBill(bill: BillCalculationEntity)

    @Query("DELETE FROM bill_calculations")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM bill_calculations WHERE id = :id")
    suspend fun getBillById(id: Long): BillCalculationEntity?
}

@Dao
interface MeterReadingDao {
    @Query("SELECT * FROM meter_readings ORDER BY dateMillis DESC")
    fun getAllReadings(): Flow<List<MeterReadingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: MeterReadingEntity)

    @Delete
    suspend fun deleteReading(reading: MeterReadingEntity)
    
    @Query("DELETE FROM meter_readings")
    suspend fun deleteAll()
}

@Dao
interface ApplianceDao {
    @Query("SELECT * FROM appliances ORDER BY name ASC")
    fun getAllAppliances(): Flow<List<ApplianceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppliance(appliance: ApplianceEntity)

    @Update
    suspend fun updateAppliance(appliance: ApplianceEntity)

    @Delete
    suspend fun deleteAppliance(appliance: ApplianceEntity)
    
    @Query("DELETE FROM appliances")
    suspend fun deleteAll()
}

@Dao
interface TariffSlabDao {
    @Query("SELECT * FROM tariff_slabs ORDER BY minUnits ASC")
    fun getAllSlabs(): Flow<List<TariffSlabEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlab(slab: TariffSlabEntity)

    @Update
    suspend fun updateSlab(slab: TariffSlabEntity)

    @Delete
    suspend fun deleteSlab(slab: TariffSlabEntity)
    
    @Query("DELETE FROM tariff_slabs")
    suspend fun deleteAll()
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(slabs: List<TariffSlabEntity>)
}
