package com.nh.electricitybillcalculator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nh.electricitybillcalculator.data.local.entity.ApplianceEntity
import com.nh.electricitybillcalculator.data.local.entity.BillCalculationEntity
import com.nh.electricitybillcalculator.data.local.entity.MeterReadingEntity
import com.nh.electricitybillcalculator.data.local.entity.TariffSlabEntity
import com.nh.electricitybillcalculator.data.repository.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    val allBills = repository.allBills.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allReadings = repository.allReadings.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allAppliances = repository.allAppliances.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allSlabs = repository.allSlabs.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Initial dummy data if no slabs exist
    init {
        viewModelScope.launch {
            repository.allSlabs.collect { slabs ->
                if (slabs.isEmpty()) {
                    val defaultSlabs = listOf(
                        TariffSlabEntity(minUnits = 0.0, maxUnits = 50.0, rate = 3.0),
                        TariffSlabEntity(minUnits = 51.0, maxUnits = 100.0, rate = 4.0),
                        TariffSlabEntity(minUnits = 101.0, maxUnits = 200.0, rate = 5.5),
                        TariffSlabEntity(minUnits = 201.0, maxUnits = 300.0, rate = 6.5),
                        TariffSlabEntity(minUnits = 301.0, maxUnits = -1.0, rate = 7.5)
                    )
                    repository.insertAllSlabs(defaultSlabs)
                }
            }
        }
    }

    fun insertBill(bill: BillCalculationEntity) {
        viewModelScope.launch { repository.insertBill(bill) }
    }
    fun deleteBill(bill: BillCalculationEntity) {
        viewModelScope.launch { repository.deleteBill(bill) }
    }

    fun insertReading(reading: MeterReadingEntity) {
        viewModelScope.launch { repository.insertReading(reading) }
    }
    fun deleteReading(reading: MeterReadingEntity) {
        viewModelScope.launch { repository.deleteReading(reading) }
    }

    fun insertAppliance(appliance: ApplianceEntity) {
        viewModelScope.launch { repository.insertAppliance(appliance) }
    }
    fun deleteAppliance(appliance: ApplianceEntity) {
        viewModelScope.launch { repository.deleteAppliance(appliance) }
    }

    fun insertSlab(slab: TariffSlabEntity) {
        viewModelScope.launch { repository.insertSlab(slab) }
    }
    fun deleteSlab(slab: TariffSlabEntity) {
        viewModelScope.launch { repository.deleteSlab(slab) }
    }
    
    fun clearAllData() {
        viewModelScope.launch { repository.clearAllData() }
    }
}

class MainViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
