package com.nh.electricitybillcalculator.domain.calculator

import com.nh.electricitybillcalculator.data.local.entity.TariffSlabEntity

object CalculationEngine {
    fun calculateUnits(previousReading: Double, currentReading: Double): Double {
        if (currentReading < previousReading) return 0.0
        return currentReading - previousReading
    }

    fun calculateSlabEnergyCharge(units: Double, slabs: List<TariffSlabEntity>): Double {
        if (units <= 0 || slabs.isEmpty()) return 0.0
        
        var remainingUnits = units
        var totalCharge = 0.0
        val sortedSlabs = slabs.sortedBy { it.minUnits }

        for (slab in sortedSlabs) {
            if (remainingUnits <= 0) break
            
            val slabRange = if (slab.maxUnits == -1.0) {
                Double.MAX_VALUE
            } else {
                slab.maxUnits - slab.minUnits + 1
            }
            
            val unitsInSlab = if (remainingUnits > slabRange) slabRange else remainingUnits
            totalCharge += unitsInSlab * slab.rate
            remainingUnits -= unitsInSlab
        }
        
        return totalCharge
    }

    fun calculateTotal(
        energyCharge: Double,
        fixedCharge: Double,
        electricityDuty: Double,
        otherCharges: Double,
        discount: Double
    ): Double {
        return energyCharge + fixedCharge + electricityDuty + otherCharges - discount
    }

    fun calculateApplianceDailyKwh(watts: Double, quantity: Int, hours: Double): Double {
        return (watts * quantity * hours) / 1000.0
    }

    fun calculateApplianceMonthlyKwh(dailyKwh: Double, days: Int): Double {
        return dailyKwh * days
    }
    
    fun calculateApplianceMonthlyCost(monthlyKwh: Double, rate: Double): Double {
        return monthlyKwh * rate
    }
}
