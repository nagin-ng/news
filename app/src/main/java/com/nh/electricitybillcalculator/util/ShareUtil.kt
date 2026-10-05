package com.nh.electricitybillcalculator.util

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
        val text = """
            Electricity Bill Estimate
            
            Units: ${bill.unitsConsumed} kWh
            Energy Charge: ₹${String.format("%.2f", bill.energyCharge)}
            Fixed Charge: ₹${String.format("%.2f", bill.fixedCharge)}
            Duty: ₹${String.format("%.2f", bill.electricityDuty)}
            Discount: ₹${String.format("%.2f", bill.discount)}
            
            Estimated Total: ₹${String.format("%.2f", bill.totalAmount)}
        """.trimIndent()
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
