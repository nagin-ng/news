import os

files = {
"app/src/main/java/com/nh/electricitybillcalculator/data/local/AppDatabase.kt": """package com.nh.electricitybillcalculator.data.local

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
""",

"app/src/main/java/com/nh/electricitybillcalculator/data/repository/AppRepository.kt": """package com.nh.electricitybillcalculator.data.repository

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
""",

"app/src/main/java/com/nh/electricitybillcalculator/domain/calculator/CalculationEngine.kt": """package com.nh.electricitybillcalculator.domain.calculator

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
""",

"app/src/main/java/com/nh/electricitybillcalculator/ElectricityBillApp.kt": """package com.nh.electricitybillcalculator

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
""",

"app/src/main/java/com/nh/electricitybillcalculator/util/PdfGenerator.kt": """package com.nh.electricitybillcalculator.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.nh.electricitybillcalculator.data.local.entity.BillCalculationEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {
    fun generatePdf(context: Context, bill: BillCalculationEntity): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        // Title
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 24f
        paint.color = Color.BLACK
        canvas.drawText("Electricity Bill Calculator Pro", 50f, 60f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 14f
        paint.color = Color.GRAY
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        canvas.drawText("Date: ${sdf.format(Date(bill.dateMillis))}", 50f, 90f, paint)

        // Divider
        paint.color = Color.LTGRAY
        paint.strokeWidth = 2f
        canvas.drawLine(50f, 110f, 545f, 110f, paint)

        // Details
        paint.color = Color.BLACK
        paint.textSize = 16f
        var y = 140f
        
        canvas.drawText("Meter Readings", 50f, y, paint)
        y += 25f
        paint.textSize = 14f
        canvas.drawText("Previous Reading:", 50f, y, paint)
        canvas.drawText("${bill.previousReading}", 400f, y, paint)
        y += 25f
        canvas.drawText("Current Reading:", 50f, y, paint)
        canvas.drawText("${bill.currentReading}", 400f, y, paint)
        y += 25f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Units Consumed:", 50f, y, paint)
        canvas.drawText("${bill.unitsConsumed} Units", 400f, y, paint)
        
        y += 40f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 16f
        canvas.drawText("Charges Breakdown", 50f, y, paint)
        y += 25f
        
        paint.textSize = 14f
        canvas.drawText("Energy Charge:", 50f, y, paint)
        canvas.drawText(String.format("₹%.2f", bill.energyCharge), 400f, y, paint)
        y += 25f
        canvas.drawText("Fixed Charge:", 50f, y, paint)
        canvas.drawText(String.format("₹%.2f", bill.fixedCharge), 400f, y, paint)
        y += 25f
        canvas.drawText("Electricity Duty:", 50f, y, paint)
        canvas.drawText(String.format("₹%.2f", bill.electricityDuty), 400f, y, paint)
        y += 25f
        canvas.drawText("Other Charges:", 50f, y, paint)
        canvas.drawText(String.format("₹%.2f", bill.otherCharges), 400f, y, paint)
        y += 25f
        canvas.drawText("Discount:", 50f, y, paint)
        canvas.drawText(String.format("-₹%.2f", bill.discount), 400f, y, paint)
        
        y += 25f
        paint.color = Color.LTGRAY
        canvas.drawLine(50f, y, 545f, y, paint)
        y += 25f
        
        paint.color = Color.BLACK
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Estimated Total:", 50f, y, paint)
        canvas.drawText(String.format("₹%.2f", bill.totalAmount), 400f, y, paint)

        pdfDocument.finishPage(page)

        return try {
            val cacheDir = File(context.cacheDir, "pdfs")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "Bill_Estimate_${System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }
}
""",

"app/src/main/java/com/nh/electricitybillcalculator/util/ShareUtil.kt": """package com.nh.electricitybillcalculator.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.nh.electricitybillcalculator.data.local.entity.BillCalculationEntity
import java.io.File

object ShareUtil {
    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }
    
    fun shareBillText(context: Context, bill: BillCalculationEntity) {
        val text = \"\"\"
            Electricity Bill Estimate
            
            Units: ${bill.unitsConsumed} kWh
            Energy Charge: ₹${String.format("%.2f", bill.energyCharge)}
            Fixed Charge: ₹${String.format("%.2f", bill.fixedCharge)}
            Duty: ₹${String.format("%.2f", bill.electricityDuty)}
            Discount: ₹${String.format("%.2f", bill.discount)}
            
            Estimated Total: ₹${String.format("%.2f", bill.totalAmount)}
        \"\"\".trimIndent()
        shareText(context, text)
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF"))
    }
}
"""
}

for path, content in files.items():
    full_path = os.path.join('/data/data/com.termux/files/home/projects/ElectricityBillCalculatorPro', path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, 'w', encoding='utf-8') as f:
        f.write(content)

print("Part 2 created successfully!")
