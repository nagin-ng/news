package com.nh.electricitybillcalculator.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nh.electricitybillcalculator.data.local.dao.ApplianceDao
import com.nh.electricitybillcalculator.data.local.dao.BillCalculationDao
import com.nh.electricitybillcalculator.data.local.dao.MeterReadingDao
import com.nh.electricitybillcalculator.data.local.dao.TariffSlabDao
import com.nh.electricitybillcalculator.data.local.entity.ApplianceEntity
import com.nh.electricitybillcalculator.data.local.entity.BillCalculationEntity
import com.nh.electricitybillcalculator.data.local.entity.MeterReadingEntity
import com.nh.electricitybillcalculator.data.local.entity.TariffSlabEntity

@Database(
    entities = [
        BillCalculationEntity::class,
        MeterReadingEntity::class,
        ApplianceEntity::class,
        TariffSlabEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun billDao(): BillCalculationDao
    abstract fun meterDao(): MeterReadingDao
    abstract fun applianceDao(): ApplianceDao
    abstract fun tariffDao(): TariffSlabDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "electricity_bill_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
