package com.climasaude.data.network.responses

import java.util.Date

data class SymptomAnalysisResponse(
    val riskLevel: String,
    val likelyConditions: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val generatedAt: Date = Date()
)

data class MedicationInteractionResponse(
    val hasInteractions: Boolean,
    val interactions: List<MedicationInteraction> = emptyList(),
    val generatedAt: Date = Date()
)

data class MedicationInteraction(
    val medicationA: String,
    val medicationB: String,
    val severity: String,
    val description: String
)

data class HealthInsightResponse(
    val insights: List<HealthInsight> = emptyList(),
    val generatedAt: Date = Date()
)

data class HealthInsight(
    val title: String,
    val message: String,
    val severity: String = "info"
)

