package com.nh.electricitybillcalculator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bill_calculations")
data class BillCalculationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long,
    val previousReading: Double,
    val currentReading: Double,
    val unitsConsumed: Double,
    val energyCharge: Double,
    val fixedCharge: Double,
    val electricityDuty: Double,
    val otherCharges: Double,
    val discount: Double,
    val totalAmount: Double
)

@Entity(tableName = "meter_readings")
data class MeterReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long,
    val reading: Double,
    val note: String
)

@Entity(tableName = "appliances")
data class ApplianceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val watts: Double,
    val quantity: Int,
    val hoursPerDay: Double,
    val daysPerMonth: Int,
    val electricityRate: Double = 0.0
)

@Entity(tableName = "tariff_slabs")
data class TariffSlabEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val minUnits: Double,
    val maxUnits: Double, // -1 means infinity
    val rate: Double
)
