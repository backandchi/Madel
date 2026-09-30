package com.example.kotib.domain.messages

import com.example.kotib.data.api.RetrofitClient
import com.example.kotib.data.local.dao.AgentActionLogDao
import com.example.kotib.data.local.dao.NotificationMessageDao
import com.example.kotib.data.local.dao.VipContactDao
import com.example.kotib.data.local.entity.AgentActionLogEntity
import com.example.kotib.data.local.entity.NotificationMessageEntity
import com.example.kotib.data.repository.ApiKeyRepository
import com.example.kotib.domain.agent.SensitiveDataMasker
import com.example.kotib.domain.call.KotibCallManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class MessageAnalysisEngine(
    private val notificationMessageDao: NotificationMessageDao,
    private val vipContactDao: VipContactDao,
    private val apiKeyRepository: ApiKeyRepository,
    private val callManager: KotibCallManager,
    private val quietHoursManager: QuietHoursManager,
    private val actionLogDao: AgentActionLogDao,
    private val scope: CoroutineScope
) {

    /**
     * Yangi kiruvchi bildirishnoma yoki SMS xabarni qabul qilish va AI tahlilini boshlash
     */
    fun onNewMessageReceived(
        packageName: String,
        appName: String,
        sender: String,
        title: String,
        content: String,
        messageType: String = NotificationMessageEntity.TYPE_NOTIFICATION
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                // 1. Maxfiy ma'lumotlarni (OTP, karta raqami, parol) maskalash
                val maskResult = SensitiveDataMasker.mask(content)
                val cleanContent = maskResult.maskedText

                // 2. VIP kontakt ekanligini tekshirish
                val isVip = vipContactDao.countMatchingVip(sender) > 0 ||
                        vipContactDao.countMatchingVip(title) > 0

                val initialUrgency = if (isVip) 4 else 1

                val entity = NotificationMessageEntity(
                    packageName = packageName,
                    appName = appName,
                    sender = sender.ifBlank { "Noma'lum" },
                    title = title,
                    rawContent = content,
                    cleanContent = cleanContent,
                    timestamp = System.currentTimeMillis(),
                    urgencyLevel = initialUrgency,
                    summary = "Tahlil qilinmoqda...",
                    isImportant = isVip,
                    isProcessed = false,
                    messageType = messageType
                )

                val messageId = notificationMessageDao.insertMessage(entity)

                // 3. Gemini orqali tahlil qilish
                analyzeWithGemini(messageId, entity.copy(id = messageId), isVip)
            } catch (_: Exception) {}
        }
    }

    private suspend fun analyzeWithGemini(
        id: Long,
        message: NotificationMessageEntity,
        isVip: Boolean
    ) {
        val activeKey = apiKeyRepository.getActiveKey() ?: return

        val prompt = """
Quyidagi kiruvchi xabarni tahlil qil va O'zbek tilida faqat toza JSON formatida javob ber:
Ilova: ${message.appName}
Yuboruvchi: ${message.sender}
Sarlavha: ${message.title}
Matn: ${message.cleanContent}
VIP Kontakt: ${if (isVip) "Ha" else "Yo'q"}

Kutilayotgan JSON formati (hech qanday markdown yoki qo'shimcha so'zsiz):
{
  "urgencyLevel": 1 dan 5 gacha butun son (5 - o'ta favqulodda/shoshilinch, 4 - muhim shaxsiy/ish, 3 - oddiy suhbat, 2 - guruh xabari, 1 - reklama/spam),
  "summary": "Mavzu bo'yicha 1 jumlalik aniq xulosa",
  "suggestedAction": "Qisqa tavsiya etilgan amal (masalan: 'Darhol javob yozish', 'Qo'ng'iroq qilish', 'Eslatma qo'yish', 'E'tiborsiz qoldirish')"
}
""".trimIndent()

        val jsonRequest = JSONObject().apply {
            val contentsArr = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            }
            put("contents", contentsArr)
            put("generationConfig", JSONObject().put("temperature", 0.1))
        }

        val body = jsonRequest.toString().toRequestBody("application/json".toMediaType())

        try {
            val response = RetrofitClient.api.generateContent(
                model = "gemini-3.5-flash",
                apiKey = activeKey.key,
                requestBody = body
            )

            if (response.isSuccessful) {
                apiKeyRepository.recordKeyUsage(activeKey.id)
                val rawResp = response.body()?.string() ?: "{}"
                val respJson = JSONObject(rawResp)
                val candidates = respJson.optJSONArray("candidates")
                val text = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: ""

                // JSON'ni tozalash
                val cleanJsonStr = text.replace("```json", "").replace("```", "").trim()
                val parsed = try {
                    JSONObject(cleanJsonStr)
                } catch (_: Exception) {
                    JSONObject().apply {
                        put("urgencyLevel", if (isVip) 4 else 2)
                        put("summary", "${message.sender}dan yangi xabar")
                        put("suggestedAction", "Xabarni o'qish")
                    }
                }

                var urgency = parsed.optInt("urgencyLevel", if (isVip) 4 else 2)
                if (isVip && urgency < 4) urgency = 4
                val summary = parsed.optString("summary", "${message.sender}: ${message.cleanContent.take(40)}")
                val action = parsed.optString("suggestedAction", "O'qish")

                val updatedEntity = message.copy(
                    urgencyLevel = urgency,
                    summary = summary,
                    suggestedAction = action,
                    isImportant = urgency >= 4 || isVip,
                    isProcessed = true
                )

                notificationMessageDao.updateMessage(updatedEntity)

                actionLogDao.insertLog(
                    AgentActionLogEntity(
                        actionType = AgentActionLogEntity.TYPE_TOOL_EXECUTION,
                        description = "Xabarlar markazi: '${message.sender}' xabari tahlil qilindi (Muhimlik: $urgency/5). Xulosa: $summary",
                        isSuccess = true
                    )
                )

                // 4. KOTIB QO'NG'IROQ REJIMI (Daraja 4-5 hodisada telefon haqiqiy qo'ng'iroqdek jiringlaydi!)
                if (urgency >= 4) {
                    val inQuietHours = quietHoursManager.isQuietHoursNow()
                    val allowInQuiet = isVip && quietHoursManager.allowVipInQuietHours

                    if (!inQuietHours || allowInQuiet) {
                        callManager.triggerIncomingCall(
                            summaryText = "${message.sender}dan shoshilinch xabar: $summary",
                            urgencyLevel = urgency,
                            attempt = 1
                        )
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
