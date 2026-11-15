package com.example.model

enum class ConsoleIntegrationStatus {
    AVAILABLE,
    IN_DEVELOPMENT
}

data class ConsoleSystem(
    val id: String,
    val name: String,
    val shortName: String,
    val mediaFormatLabel: String,
    val integrationStatus: ConsoleIntegrationStatus,
    val supportedExtensions: List<String>,
    val targetEmulatorName: String? = null,
    val targetEmulatorPackage: String? = null
)

object ConsoleCatalog {
    const val PS1_ID = "ps1"
    const val DUCKSTATION_PACKAGE = "com.github.stenzek.duckstation"

    val allConsoles: List<ConsoleSystem> = listOf(
        ConsoleSystem(
            id = PS1_ID,
            name = "PlayStation 1",
            shortName = "PS1",
            mediaFormatLabel = "Disco óptico • 32-bit",
            integrationStatus = ConsoleIntegrationStatus.AVAILABLE,
            supportedExtensions = listOf("chd", "cue", "pbp", "iso", "m3u"),
            targetEmulatorName = "DuckStation",
            targetEmulatorPackage = DUCKSTATION_PACKAGE
        ),
        ConsoleSystem(
            id = "n64",
            name = "Nintendo 64",
            shortName = "N64",
            mediaFormatLabel = "Cartucho • 64-bit",
            integrationStatus = ConsoleIntegrationStatus.IN_DEVELOPMENT,
            supportedExtensions = emptyList()
        ),
        ConsoleSystem(
            id = "gamecube",
            name = "GameCube",
            shortName = "GC",
            mediaFormatLabel = "Mini-disco • 128-bit",
            integrationStatus = ConsoleIntegrationStatus.IN_DEVELOPMENT,
            supportedExtensions = emptyList()
        ),
        ConsoleSystem(
            id = "snes",
            name = "SNES",
            shortName = "SNES",
            mediaFormatLabel = "Cartucho • 16-bit",
            integrationStatus = ConsoleIntegrationStatus.IN_DEVELOPMENT,
            supportedExtensions = emptyList()
        ),
        ConsoleSystem(
            id = "dreamcast",
            name = "Dreamcast",
            shortName = "DC",
            mediaFormatLabel = "GD-ROM • 128-bit",
            integrationStatus = ConsoleIntegrationStatus.IN_DEVELOPMENT,
            supportedExtensions = emptyList()
        ),
        ConsoleSystem(
            id = "ps2",
            name = "PlayStation 2",
            shortName = "PS2",
            mediaFormatLabel = "DVD • 128-bit",
            integrationStatus = ConsoleIntegrationStatus.IN_DEVELOPMENT,
            supportedExtensions = emptyList()
        )
    )

    fun findById(id: String): ConsoleSystem? = allConsoles.firstOrNull { it.id == id }
}
