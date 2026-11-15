package com.example.model

sealed interface LaunchResult {
    data object Idle : LaunchResult

    data class IntentDispatched(
        val message: String,
        val emulatorPackage: String
    ) : LaunchResult

    data class Failed(
        val reason: String,
        val emulatorPackage: String?,
        val canOpenMainApp: Boolean
    ) : LaunchResult
}
