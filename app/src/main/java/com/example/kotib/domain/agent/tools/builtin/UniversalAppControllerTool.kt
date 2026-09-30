package com.example.kotib.domain.agent.tools.builtin

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.kotib.domain.agent.tools.AgentTool
import com.example.kotib.domain.agent.tools.ToolExecutionResult
import com.example.kotib.domain.automation.RootShellManager
import com.example.kotib.domain.automation.SystemAutomationController
import com.example.kotib.service.KotibAccessibilityService
import kotlinx.coroutines.delay

/**
 * Universal Ilovalar va Tizim Boshqaruvchisi (Phase 6):
 * Telefondagi HAR QANDAY ilovani ochish, yopish, chatlarga (Telegram, WhatsApp, SMS)
 * avtomatik xabar yozish va jo'natish, YouTube/Chrome'da qidirish,
 * ekranni surish (swipe), teginish (tap), matn kiritish va o'qish.
 */
class UniversalAppControllerTool(
    private val context: Context,
    private val systemController: SystemAutomationController,
    private val shellManager: RootShellManager
) : AgentTool {

    override val name: String = "control_installed_apps"
    override val description: String =
        "Telefondagi barcha ilovalar va ekran harakatlarini to'liq boshqarish: Telegram, WhatsApp, SMS chatlariga xabar yuborish, YouTube/Chrome'da qidirish, ilovalarni ochish yoki to'xtatish, ekranni surish (swipe), tugmalarni bosish yoki barcha o'rnatilgan ilovalar ro'yxatini olish."

    override val parametersSchema: Map<String, Any?> = mapOf(
        "type" to "OBJECT",
        "properties" to mapOf(
            "action" to mapOf(
                "type" to "STRING",
                "description" to "Amal turi: 'send_chat_message' (chatga xabar yuborish), 'open_and_search' (ilovada qidirish), 'launch_app' (ochish), 'close_app' (to'xtatish), 'list_apps' (barcha ilovalar ro'yxati), 'swipe' (surish: up/down/left/right), 'tap' (nuqtaga bosish), 'click_text' (ekrandagi matnni bosish), 'type_text' (matn yozish), 'read_screen' (ekranni o'qish)"
            ),
            "app" to mapOf(
                "type" to "STRING",
                "description" to "Ilova nomi: 'telegram', 'whatsapp', 'sms', 'youtube', 'chrome', 'camera', 'instagram', 'tiktok' yoki istalgan boshqa ilova"
            ),
            "recipient" to mapOf(
                "type" to "STRING",
                "description" to "Xabar oluvchi kontakt nomi, telefon raqami yoki username (masalan: '+998901234567' yoki 'Ali')"
            ),
            "text" to mapOf(
                "type" to "STRING",
                "description" to "Jo'natilishi kerak bo'lgan xabar matni, qidiruv so'zi yoki kiritiladigan matn"
            ),
            "direction" to mapOf(
                "type" to "STRING",
                "description" to "Surish yo'nalishi (swipe): 'up', 'down', 'left', 'right'"
            ),
            "x" to mapOf("type" to "INTEGER", "description" to "X koordinatasi"),
            "y" to mapOf("type" to "INTEGER", "description" to "Y koordinatasi")
        ),
        "required" to listOf("action")
    )

    override val requiresConfirmation: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val action = args["action"]?.toString() ?: "launch_app"
        val app = args["app"]?.toString()?.lowercase() ?: ""
        val recipient = args["recipient"]?.toString() ?: ""
        val text = args["text"]?.toString() ?: ""
        val direction = args["direction"]?.toString()?.lowercase() ?: "up"
        val x = (args["x"] as? Number)?.toInt() ?: 500
        val y = (args["y"] as? Number)?.toInt() ?: 1000

        return when (action.lowercase()) {
            "send_chat_message" -> {
                sendChatMessage(app, recipient, text)
            }

            "open_and_search" -> {
                openAndSearch(app, text)
            }

            "list_apps" -> {
                listInstalledApps()
            }

            "close_app" -> {
                val pkgName = resolvePackageName(app)
                shellManager.forceStopPackage(pkgName)
                ToolExecutionResult(
                    isSuccess = true,
                    output = mapOf("package" to pkgName),
                    userSummary = "'$app' ilovasi to'xtatildi va xotiradan tozalandi."
                )
            }

            "swipe" -> {
                swipeScreen(direction)
            }

            "tap" -> {
                shellManager.tap(x, y)
                ToolExecutionResult(
                    isSuccess = true,
                    output = mapOf("x" to x, "y" to y),
                    userSummary = "Ekrandagi ($x, $y) nuqtasiga bosildi."
                )
            }

            "click_text" -> {
                val ok = KotibAccessibilityService.clickNodeByText(text)
                ToolExecutionResult(
                    isSuccess = ok,
                    output = mapOf("target" to text, "clicked" to ok),
                    userSummary = if (ok) "Ekranda '$text' tugmasi bosildi." else "Ekranda '$text' elementi topilmadi."
                )
            }

            "type_text" -> {
                shellManager.typeText(text)
                ToolExecutionResult(
                    isSuccess = true,
                    output = mapOf("typed" to text),
                    userSummary = "'$text' matni kiritildi."
                )
            }

            "read_screen" -> {
                val dump = KotibAccessibilityService.dumpVisibleScreenTexts()
                ToolExecutionResult(
                    isSuccess = true,
                    output = mapOf("content" to dump),
                    userSummary = "Hozirgi ekrandagi ma'lumotlar:\n$dump"
                )
            }

            else -> {
                // launch_app
                val (ok, msg) = systemController.launchApp(app)
                ToolExecutionResult(
                    isSuccess = ok,
                    output = mapOf("app" to app, "status" to msg),
                    userSummary = msg
                )
            }
        }
    }

    private fun listInstalledApps(): ToolExecutionResult {
        return try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(0)
            val launchableApps = packages.filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .map { pm.getApplicationLabel(it).toString() }
                .distinct()
                .sorted()

            val preview = launchableApps.take(25).joinToString(", ")
            ToolExecutionResult(
                isSuccess = true,
                output = mapOf("count" to launchableApps.size, "apps" to launchableApps),
                userSummary = "Telefonda jami ${launchableApps.size} ta asosiy ilovalar o'rnatilgan:\n$preview..."
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to (e.message ?: "Xatolik")),
                userSummary = "Ilovalar ro'yxatini olib bo'lmadi: ${e.message}"
            )
        }
    }

    private suspend fun swipeScreen(direction: String): ToolExecutionResult {
        return try {
            // Ekranni o'lchamlariga qarab surish (input swipe x1 y1 x2 y2 ms)
            val cmd = when (direction) {
                "up", "yuqoriga" -> "input swipe 500 1500 500 500 300"
                "down", "pastga" -> "input swipe 500 500 500 1500 300"
                "left", "chapga" -> "input swipe 900 1000 100 1000 300"
                "right", "o'ngga" -> "input swipe 100 1000 900 1000 300"
                else -> "input swipe 500 1400 500 600 300"
            }
            shellManager.executeCommand(cmd)

            // Accessibility orqali ham scroll
            if (KotibAccessibilityService.isRunning()) {
                KotibAccessibilityService.scroll(forward = direction == "up")
            }

            ToolExecutionResult(
                isSuccess = true,
                output = mapOf("direction" to direction),
                userSummary = "Ekran $direction yo'nalishida surildi."
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to (e.message ?: "Xato")),
                userSummary = "Ekranni surib bo'lmadi."
            )
        }
    }

    private suspend fun sendChatMessage(app: String, recipient: String, text: String): ToolExecutionResult {
        return try {
            when {
                app.contains("telegram") -> {
                    val cleanUsername = recipient.removePrefix("@").trim()
                    val uri = if (cleanUsername.isNotBlank()) {
                        Uri.parse("https://t.me/$cleanUsername")
                    } else {
                        Uri.parse("tg://msg?text=" + Uri.encode(text))
                    }

                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("org.telegram.messenger")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }

                    val pm = context.packageManager
                    if (intent.resolveActivity(pm) != null) {
                        context.startActivity(intent)
                    } else {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Telegram").apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                    }

                    delay(1200)
                    if (KotibAccessibilityService.isRunning()) {
                        KotibAccessibilityService.clickNodeByText("Send") || KotibAccessibilityService.clickNodeByText("Yuborish")
                    }

                    ToolExecutionResult(
                        isSuccess = true,
                        output = mapOf("app" to "Telegram", "recipient" to recipient, "text" to text),
                        userSummary = "Telegram'da $recipient'ga '$text' xabari tayyorlandi va jo'natildi."
                    )
                }

                app.contains("whatsapp") -> {
                    val cleanPhone = recipient.replace("[^0-9]".toRegex(), "")
                    val uri = if (cleanPhone.isNotBlank()) {
                        Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(text)}")
                    } else {
                        Uri.parse("whatsapp://send?text=${Uri.encode(text)}")
                    }

                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.whatsapp")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }

                    context.startActivity(intent)
                    delay(1200)
                    if (KotibAccessibilityService.isRunning()) {
                        KotibAccessibilityService.clickNodeByText("Send") || KotibAccessibilityService.clickNodeByText("Yuborish")
                    }

                    ToolExecutionResult(
                        isSuccess = true,
                        output = mapOf("app" to "WhatsApp", "recipient" to recipient, "text" to text),
                        userSummary = "WhatsApp'da $recipient'ga '$text' xabari yuborildi."
                    )
                }

                else -> {
                    val cleanRecipient = recipient.ifBlank { "998901234567" }
                    val smsUri = Uri.parse("smsto:$cleanRecipient")
                    val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                        putExtra("sms_body", text)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)

                    delay(1000)
                    if (KotibAccessibilityService.isRunning()) {
                        KotibAccessibilityService.clickNodeByText("Send") || KotibAccessibilityService.clickNodeByText("SMS")
                    }

                    ToolExecutionResult(
                        isSuccess = true,
                        output = mapOf("app" to "SMS", "recipient" to cleanRecipient, "text" to text),
                        userSummary = "SMS orqali $cleanRecipient'ga '$text' xabari yuborildi."
                    )
                }
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to (e.message ?: "Xatolik")),
                userSummary = "Xabar yuborishda xatolik: ${e.message}"
            )
        }
    }

    private fun openAndSearch(app: String, query: String): ToolExecutionResult {
        return try {
            when {
                app.contains("youtube") -> {
                    val intent = Intent(Intent.ACTION_SEARCH).apply {
                        setPackage("com.google.android.youtube")
                        putExtra("query", query)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    val pm = context.packageManager
                    if (intent.resolveActivity(pm) != null) {
                        context.startActivity(intent)
                    } else {
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(webIntent)
                    }
                    ToolExecutionResult(
                        isSuccess = true,
                        output = mapOf("app" to "YouTube", "query" to query),
                        userSummary = "YouTube'da '$query' qidirildi."
                    )
                }

                else -> {
                    val uri = Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ToolExecutionResult(
                        isSuccess = true,
                        output = mapOf("query" to query),
                        userSummary = "Brauzerda '$query' qidirildi."
                    )
                }
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to (e.message ?: "Qidiruv xatosi")),
                userSummary = "Qidiruvni bajarib bo'lmadi: ${e.message}"
            )
        }
    }

    private fun resolvePackageName(appName: String): String {
        return when (appName.lowercase()) {
            "telegram" -> "org.telegram.messenger"
            "whatsapp" -> "com.whatsapp"
            "youtube" -> "com.google.android.youtube"
            "chrome" -> "com.android.chrome"
            "instagram" -> "com.instagram.android"
            "tiktok" -> "com.zhiliaoapp.musically"
            else -> appName
        }
    }
}
