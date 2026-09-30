package com.example.kotib.domain.automation

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class ShellResult(
    val exitCode: Int,
    val output: String,
    val error: String,
    val isSuccess: Boolean = exitCode == 0
)

class RootShellManager(private val context: Context) {

    private val rootBinaries = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su"
    )

    /**
     * Qurilmada haqiqiy Root (su) huquqlari mavjudligini tekshiradi
     */
    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        val binaryExists = rootBinaries.any { File(it).exists() }
        if (!binaryExists) {
            // "which su" buyrug'i orqali tekshirish
            val whichResult = executeCommand("which su", asRoot = false)
            if (whichResult.isSuccess && whichResult.output.isNotBlank()) {
                return@withContext true
            }
        }

        // Haqiqiy su sinovi
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Haqiqiy Shell (sh yoki su) buyrug'ini bajaradi
     */
    suspend fun executeCommand(command: String, asRoot: Boolean = false): ShellResult = withContext(Dispatchers.IO) {
        try {
            val shell = if (asRoot) "su" else "sh"
            val process = Runtime.getRuntime().exec(arrayOf(shell, "-c", command))

            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            val stdout = stdoutReader.readText().trim()
            val stderr = stderrReader.readText().trim()

            val exitCode = process.waitFor()

            ShellResult(
                exitCode = exitCode,
                output = stdout,
                error = stderr,
                isSuccess = exitCode == 0
            )
        } catch (e: Exception) {
            ShellResult(
                exitCode = -1,
                output = "",
                error = e.message ?: "Buyruqni bajarib bo'lmadi",
                isSuccess = false
            )
        }
    }

    /**
     * UI Avtomatlashtirish: Ekrandagi (x, y) koordinatasiga bosish
     */
    suspend fun tap(x: Int, y: Int, useRoot: Boolean = true): ShellResult {
        return executeCommand("input tap $x $y", asRoot = useRoot)
    }

    /**
     * Klaviaturadan matn kiritish
     */
    suspend fun typeText(text: String, useRoot: Boolean = true): ShellResult {
        val sanitized = text.replace(" ", "%s").replace("\"", "\\\"")
        return executeCommand("input text \"$sanitized\"", asRoot = useRoot)
    }

    /**
     * Tugma bosish (3 = Home, 4 = Back, 24 = Volume Up, 25 = Volume Down, 26 = Power)
     */
    suspend fun pressKeyEvent(keyCode: Int, useRoot: Boolean = true): ShellResult {
        return executeCommand("input keyevent $keyCode", asRoot = useRoot)
    }

    /**
     * Ilovani to'xtatish (Force stop)
     */
    suspend fun forceStopPackage(packageName: String, useRoot: Boolean = true): ShellResult {
        return executeCommand("am force-stop $packageName", asRoot = useRoot)
    }

    /**
     * Ilova keshini tozalash
     */
    suspend fun clearPackageData(packageName: String, useRoot: Boolean = true): ShellResult {
        return executeCommand("pm clear $packageName", asRoot = useRoot)
    }

    /**
     * Ekran yorug'ligini o'zgartirish (0 dan 255 gacha)
     */
    suspend fun setBrightness(brightness: Int, useRoot: Boolean = true): ShellResult {
        val clamped = brightness.coerceIn(0, 255)
        return executeCommand("settings put system screen_brightness $clamped", asRoot = useRoot)
    }

    /**
     * Wi-Fi yoqish / o'chirish
     */
    suspend fun toggleWifi(enable: Boolean, useRoot: Boolean = true): ShellResult {
        val cmd = if (enable) "svc wifi enable" else "svc wifi disable"
        return executeCommand(cmd, asRoot = useRoot)
    }

    /**
     * Bluetooth yoqish / o'chirish
     */
    suspend fun toggleBluetooth(enable: Boolean, useRoot: Boolean = true): ShellResult {
        val cmd = if (enable) "svc bluetooth enable" else "svc bluetooth disable"
        return executeCommand(cmd, asRoot = useRoot)
    }
}
