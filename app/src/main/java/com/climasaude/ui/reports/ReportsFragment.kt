package com.climasaude.ui.reports

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.climasaude.databinding.FragmentReportsBinding
import com.climasaude.presentation.viewmodels.ReportsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReportsViewModel by viewModels()

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                saveFileToUri(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.buttonExportReport.setOnClickListener {
            openDirectoryPicker()
        }

        binding.buttonShareReport.setOnClickListener {
            val filePath = viewModel.uiState.value.exportedFilePath
            if (filePath.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Gere o relatório primeiro.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            shareReportFile(filePath)
        }
    }

    private fun openDirectoryPicker() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Relatorio_Saude_$timestamp.csv" // Mantido CSV para compatibilidade com o writer atual

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/comma-separated-values"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        createDocumentLauncher.launch(intent)
    }

    private fun saveFileToUri(uri: Uri) {
        val tempFile = File(requireContext().cacheDir, "temp_report.csv")
        viewModel.exportHealthReport(tempFile)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    // Só copia se o estado mudar para sucesso e o arquivo for o temporário
                    if (!uiState.isExporting && uiState.exportedFilePath == tempFile.absolutePath) {
                        try {
                            requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                                tempFile.inputStream().use { inputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                            // Não removemos o tempFile aqui para permitir o compartilhamento posterior se desejado
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro ao salvar arquivo: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    binding.progressExportReport.isVisible = uiState.isExporting
                    binding.buttonExportReport.isEnabled = !uiState.isExporting
                    binding.buttonShareReport.isEnabled = !uiState.isExporting && !uiState.exportedFilePath.isNullOrBlank()
                    binding.textExportStatus.text = uiState.statusMessage
                    binding.textExportPath.text = uiState.exportedFilePath ?: ""
                }
            }
        }
    }

    private fun shareReportFile(filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(requireContext(), "Arquivo temporário não encontrado.", Toast.LENGTH_SHORT).show()
                return
            }

            val fileUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/comma-separated-values"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Compartilhar Relatório"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Erro ao compartilhar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
