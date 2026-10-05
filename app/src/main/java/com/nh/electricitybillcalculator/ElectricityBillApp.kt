package com.nh.electricitybillcalculator

import android.app.Application
import com.nh.electricitybillcalculator.data.local.AppDatabase
import com.nh.electricitybillcalculator.data.repository.AppRepository

class ElectricityBillApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { 
        AppRepository(
            database.billDao(),
            database.meterDao(),
            database.applianceDao(),
            database.tariffDao()
        )
    }
}
