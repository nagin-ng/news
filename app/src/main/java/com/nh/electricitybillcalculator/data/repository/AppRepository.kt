package com.nh.electricitybillcalculator.data.repository

import com.nh.electricitybillcalculator.data.local.dao.ApplianceDao
import com.nh.electricitybillcalculator.data.local.dao.BillCalculationDao
import com.nh.electricitybillcalculator.data.local.dao.MeterReadingDao
import com.nh.electricitybillcalculator.data.local.dao.TariffSlabDao
import com.nh.electricitybillcalculator.data.local.entity.ApplianceEntity
import com.nh.electricitybillcalculator.data.local.entity.BillCalculationEntity
import com.nh.electricitybillcalculator.data.local.entity.MeterReadingEntity
import com.nh.electricitybillcalculator.data.local.entity.TariffSlabEntity

class AppRepository(
    private val billDao: BillCalculationDao,
    private val meterDao: MeterReadingDao,
    private val applianceDao: ApplianceDao,
    private val tariffDao: TariffSlabDao
) {
    val allBills = billDao.getAllBills()
    val allReadings = meterDao.getAllReadings()
    val allAppliances = applianceDao.getAllAppliances()
    val allSlabs = tariffDao.getAllSlabs()

    suspend fun insertBill(bill: BillCalculationEntity): Long = billDao.insertBill(bill)
    suspend fun deleteBill(bill: BillCalculationEntity) = billDao.deleteBill(bill)
    suspend fun getBillById(id: Long) = billDao.getBillById(id)
    suspend fun clearBills() = billDao.deleteAll()

    suspend fun insertReading(reading: MeterReadingEntity) = meterDao.insertReading(reading)
    suspend fun deleteReading(reading: MeterReadingEntity) = meterDao.deleteReading(reading)
    suspend fun clearReadings() = meterDao.deleteAll()

    suspend fun insertAppliance(appliance: ApplianceEntity) = applianceDao.insertAppliance(appliance)
    suspend fun updateAppliance(appliance: ApplianceEntity) = applianceDao.updateAppliance(appliance)
    suspend fun deleteAppliance(appliance: ApplianceEntity) = applianceDao.deleteAppliance(appliance)
    suspend fun clearAppliances() = applianceDao.deleteAll()

    suspend fun insertSlab(slab: TariffSlabEntity) = tariffDao.insertSlab(slab)
    suspend fun updateSlab(slab: TariffSlabEntity) = tariffDao.updateSlab(slab)
    suspend fun deleteSlab(slab: TariffSlabEntity) = tariffDao.deleteSlab(slab)
    suspend fun insertAllSlabs(slabs: List<TariffSlabEntity>) = tariffDao.insertAll(slabs)
    
    suspend fun clearAllData() {
        billDao.deleteAll()
        meterDao.deleteAll()
        applianceDao.deleteAll()
        tariffDao.deleteAll()
    }
}
