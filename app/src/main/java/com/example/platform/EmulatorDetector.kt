package com.example.platform

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.model.ConsoleCatalog

data class EmulatorStatus(
    val packageName: String,
    val displayName: String,
    val isInstalled: Boolean,
    val versionName: String? = null,
    val canLaunchMainApp: Boolean = false
)

class EmulatorDetector(private val context: Context) {

    fun detectDuckStation(): EmulatorStatus {
        val packageManager = context.packageManager
        val pkg = ConsoleCatalog.DUCKSTATION_PACKAGE
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(pkg, 0)
            }
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            EmulatorStatus(
                packageName = pkg,
                displayName = "DuckStation",
                isInstalled = true,
                versionName = packageInfo.versionName,
                canLaunchMainApp = launchIntent != null
            )
        } catch (_: PackageManager.NameNotFoundException) {
            EmulatorStatus(
                packageName = pkg,
                displayName = "DuckStation",
                isInstalled = false,
                versionName = null,
                canLaunchMainApp = false
            )
        } catch (_: Exception) {
            EmulatorStatus(
                packageName = pkg,
                displayName = "DuckStation",
                isInstalled = false,
                versionName = null,
                canLaunchMainApp = false
            )
        }
    }
}
