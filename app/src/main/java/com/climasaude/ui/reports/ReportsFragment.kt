package com.climasaude.ui.reports

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

@AndroidEntryPoint
class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReportsViewModel by viewModels()

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
            viewModel.exportHealthReport()
        }

        binding.buttonShareReport.setOnClickListener {
            val filePath = viewModel.uiState.value.exportedFilePath
            if (filePath.isNullOrBlank()) {
                Toast.makeText(
                    requireContext(),
                    "Gere o arquivo XLSX antes de compartilhar.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            shareReportFile(filePath)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    binding.progressExportReport.isVisible = uiState.isExporting
                    binding.buttonExportReport.isEnabled = !uiState.isExporting
                    binding.buttonShareReport.isEnabled =
                        !uiState.isExporting && !uiState.exportedFilePath.isNullOrBlank()

                    binding.textExportStatus.text = uiState.statusMessage
                    binding.textExportPath.text = uiState.exportedFilePath
                        ?: getString(com.climasaude.R.string.no_reports_generated)
                }
            }
        }
    }

    private fun shareReportFile(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(
                requireContext(),
                "Arquivo nao encontrado. Gere novamente o relatorio.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val fileUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = XLSX_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "Relatorio de medicamentos e sintomas")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, getString(com.climasaude.R.string.share_report)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val XLSX_MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    }
}
