package com.example.ui.screens

import android.app.ApplicationExitInfo
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DomDeckCrashDiagnosticsTest {

    @Test
    fun reportsNativeCrashAsNativeFailure() {
        assertEquals(
            "Falha nativa (C/C++/GPU)",
            exitReasonLabel(ApplicationExitInfo.REASON_CRASH_NATIVE)
        )
    }

    @Test
    fun distinguishesJavaAndAnrFromNativeCrash() {
        assertEquals(
            "Exceção Java/Kotlin",
            exitReasonLabel(ApplicationExitInfo.REASON_CRASH)
        )
        assertEquals(
            "Sem resposta (ANR)",
            exitReasonLabel(ApplicationExitInfo.REASON_ANR)
        )
    }
}
