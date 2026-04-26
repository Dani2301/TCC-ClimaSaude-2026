package com.climasaude.utils

import android.content.Context
import com.climasaude.data.database.entities.Medication
import com.climasaude.data.database.entities.Symptom
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthXlsxExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun exportHealthDataTo(
        targetFile: File,
        userName: String,
        medications: List<Medication>,
        symptoms: List<Symptom>
    ): Resource<File> = withContext(Dispatchers.IO) {
        try {
            val workbook = XSSFWorkbook()
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            // Estilo do Cabeçalho
            val headerStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                val font = workbook.createFont().apply {
                    setBold(true)
                }
                setFont(font)
            }

            // Aba de Medicamentos
            val medSheet = workbook.createSheet("Medicamentos")
            val medHeader = medSheet.createRow(0)
            listOf("Nome", "Dosagem", "Frequência", "Horários").forEachIndexed { i, title ->
                medHeader.createCell(i).apply {
                    setCellValue(title)
                    cellStyle = headerStyle
                }
            }

            medications.forEachIndexed { index, med ->
                val row = medSheet.createRow(index + 1)
                row.createCell(0).setCellValue(med.name)
                row.createCell(1).setCellValue(med.dosage)
                row.createCell(2).setCellValue(med.frequency)
                row.createCell(3).setCellValue(med.times.joinToString(", "))
            }
            medSheet.setColumnWidth(0, 6000)
            medSheet.setColumnWidth(3, 8000)

            // Aba de Sintomas
            val symSheet = workbook.createSheet("Sintomas")
            val symHeader = symSheet.createRow(0)
            listOf("Data", "Sintoma", "Intensidade", "Notas").forEachIndexed { i, title ->
                symHeader.createCell(i).apply {
                    setCellValue(title)
                    cellStyle = headerStyle
                }
            }

            symptoms.forEachIndexed { index, s ->
                val row = symSheet.createRow(index + 1)
                row.createCell(0).setCellValue(sdf.format(s.timestamp))
                row.createCell(1).setCellValue(s.name)
                row.createCell(2).setCellValue(s.intensity.toDouble())
                row.createCell(3).setCellValue(s.notes ?: "")
            }
            symSheet.setColumnWidth(0, 5000)
            symSheet.setColumnWidth(1, 6000)
            symSheet.setColumnWidth(3, 10000)

            // Salvar o arquivo
            FileOutputStream(targetFile).use { 
                workbook.write(it)
            }
            workbook.close()

            Resource.Success(targetFile)
        } catch (e: Exception) {
            Resource.Error("Falha ao gerar arquivo XLSX: ${e.message}")
        }
    }
}
