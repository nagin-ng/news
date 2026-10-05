package com.nh.electricitybillcalculator.util

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
