package com.example

import com.example.platform.RomScannerRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RomScannerRulesTest {

    @Test
    fun `extracts cue referenced bin filenames with quotes and paths`() {
        val cueText = """
            FILE "SubFolder/Track 01.bin" BINARY
              TRACK 01 MODE2/2352
                INDEX 01 00:00:00
            FILE Track02.BIN BINARY
              TRACK 02 AUDIO
                INDEX 00 00:00:00
        """.trimIndent()

        val refs = RomScannerRules.extractCueReferencedFileNames(cueText)
        assertEquals(setOf("track 01.bin", "track02.bin"), refs)
    }

    @Test
    fun `extracts cue referenced paths even with UTF-8 BOM and deduplicates`() {
        val cueText = "\uFEFF" + """
            FILE "Silent Hill (USA).bin" BINARY
              TRACK 01 MODE2/2352
                INDEX 01 00:00:00
            FILE "Silent Hill (USA).bin" BINARY
              TRACK 02 AUDIO
                INDEX 00 00:00:00
        """.trimIndent()

        val paths = RomScannerRules.extractCueReferencedPaths(cueText)
        assertEquals(listOf("Silent Hill (USA).bin"), paths)
    }

    @Test
    fun `marks bin associated with cue by reference or track stem`() {
        val refs = setOf("alpha_game (track 1).bin")
        val stems = setOf("alpha_game", "beta_game")

        assertTrue(
            RomScannerRules.isBinAssociatedWithCue(
                binFileName = "Alpha_Game (Track 1).bin",
                cueReferencedFilesLower = refs,
                cueStemsLower = stems
            )
        )
        assertTrue(
            RomScannerRules.isBinAssociatedWithCue(
                binFileName = "Beta_Game (Track 02).bin",
                cueReferencedFilesLower = refs,
                cueStemsLower = stems
            )
        )
        assertFalse(
            RomScannerRules.isBinAssociatedWithCue(
                binFileName = "Orphan_Game.bin",
                cueReferencedFilesLower = refs,
                cueStemsLower = stems
            )
        )
    }

    @Test
    fun `derives clean display title from filename`() {
        assertEquals(
            "Chrono Cross (Disc 1)",
            RomScannerRules.deriveDisplayTitle("Chrono_Cross_(Disc_1).chd")
        )
    }
}
