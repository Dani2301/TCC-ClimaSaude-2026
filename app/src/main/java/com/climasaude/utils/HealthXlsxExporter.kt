package com.climasaude.utils

import android.content.Context
import com.climasaude.data.database.entities.Medication
import com.climasaude.data.database.entities.Symptom
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    // Senior Fix: Implementação de exportação funcional. Modificado por: Daniel
    suspend fun exportHealthData(
        userName: String,
        medications: List<Medication>,
        symptoms: List<Symptom>
    ): Resource<File> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Relatorio_Saude_$timestamp.csv"
            val file = File(context.cacheDir, fileName)
            
            val writer = FileOutputStream(file).bufferedWriter()
            
            // Cabeçalho do Relatório
            writer.write("RELATORIO DE SAUDE - $userName\n")
            writer.write("Gerado em: ${Date()}\n\n")
            
            // Seção de Medicamentos
            writer.write("MEDICAMENTOS ATIVOS\n")
            writer.write("Nome;Dosagem;Frequencia;Horarios\n")
            medications.forEach { med ->
                writer.write("${med.name};${med.dosage};${med.frequency};${med.times.joinToString(",")}\n")
            }
            
            writer.write("\nHISTORICO DE SINTOMAS\n")
            writer.write("Data;Sintoma;Intensidade;Notas\n")
            symptoms.forEach { s ->
                writer.write("${s.timestamp};${s.name};${s.intensity};${s.notes ?: ""}\n")
            }
            
            writer.close()
            Resource.Success(file)
        } catch (e: Exception) {
            Resource.Error("Falha ao gerar arquivo: ${e.message}")
        }
    }

    suspend fun exportHealthDataTo(
        targetFile: File,
        userName: String,
        medications: List<Medication>,
        symptoms: List<Symptom>
    ): Resource<File> = withContext(Dispatchers.IO) {
        try {
            val writer = FileOutputStream(targetFile).bufferedWriter()
            
            writer.write("RELATORIO DE SAUDE - $userName\n")
            writer.write("Gerado em: ${Date()}\n\n")
            
            writer.write("MEDICAMENTOS ATIVOS\n")
            writer.write("Nome;Dosagem;Frequencia;Horarios\n")
            medications.forEach { med ->
                writer.write("${med.name};${med.dosage};${med.frequency};${med.times.joinToString(",")}\n")
            }
            
            writer.write("\nHISTORICO DE SINTOMAS\n")
            writer.write("Data;Sintoma;Intensidade;Notas\n")
            symptoms.forEach { s ->
                writer.write("${s.timestamp};${s.name};${s.intensity};${s.notes ?: ""}\n")
            }
            
            writer.close()
            Resource.Success(targetFile)
        } catch (e: Exception) {
            Resource.Error("Falha ao salvar arquivo: ${e.message}")
        }
    }
}
