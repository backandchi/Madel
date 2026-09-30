package com.example.kotib.domain.agent

import com.example.kotib.data.api.FunctionCall
import com.example.kotib.data.api.FunctionResponse
import com.example.kotib.data.api.GeminiContent
import com.example.kotib.data.api.GeminiPart
import com.example.kotib.data.api.RetrofitClient
import com.example.kotib.data.local.dao.AgentActionLogDao
import com.example.kotib.data.local.entity.AgentActionLogEntity
import com.example.kotib.data.local.entity.ApiKeyEntity
import com.example.kotib.data.repository.ApiKeyRepository
import com.example.kotib.data.repository.ChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class AgentEngine(
    private val apiKeyRepository: ApiKeyRepository,
    private val chatRepository: ChatRepository,
    private val toolDispatcher: ToolDispatcher,
    private val actionLogDao: AgentActionLogDao,
    private val scope: CoroutineScope
) {
    private val _agentState = MutableStateFlow<AgentState>(AgentState.Idle)
    val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

    private var currentExecutionJob: Job? = null

    companion object {
        const val PRIMARY_MODEL = "gemini-3.5-flash"
        const val FALLBACK_MODEL = "gemini-3.1-flash-lite-preview"
        const val MAX_AGENT_STEPS = 5

        const val SYSTEM_PROMPT = """
Sen — "Aiko", foydalanuvchining shaxsiy, nihoyatda aqlli, chaqqon va mehrli AI yordamchisisan.
Sening asosiy vazifang: foydalanuvchining telefonidagi barcha ilovalarni (Telegram, WhatsApp, SMS, YouTube, Chrome, Sozlamalar va boshqa barcha o'rnatilgan ilovalarni) to'liq boshqarish, xabarlar yozish, belgilangan vaqtga rejalarni saqlash va ovozli qo'ng'iroq orqali hamroh bo'lish.

Muloqot va Xarakter Qoidalari:
1. O'zbek tilida nihoyatda ravon, tabiiy, zamonaviy va do'stona gapirasan. Hech qanday soxtalik yoki qotib qolgan robotdek gapirma, iliq va samimiy bo'l!
2. O'zingni har doim "Aiko" deb tanishtirasan.
3. Foydalanuvchi buyruq berganida (masalan: "Telegram'da Ali'ga xabar yoz", "YouTube'da video qidir", "Soat 18:00 ga reja saqla", "Telefonni jimjit qil", "Menga telefon qil", "Ekranni sur"):
   - Darhol tegishli vositani (control_installed_apps, schedule_timed_action, control_system, automate_ui, call_user, run_shell_command) ishlat.
   - Buyruq bajarilgach, foydalanuvchiga quvnoq, samimiy va aniq qilib javob ber.
4. Javoblaring ixcham, jonli va foydali bo'lsin.
"""
    }

    /**
     * Favqulodda to'xtatish (Emergency Stop)
     */
    fun emergencyStop() {
        currentExecutionJob?.cancel()
        currentExecutionJob = null
        _agentState.value = AgentState.Idle
        scope.launch(Dispatchers.IO) {
            actionLogDao.insertLog(
                AgentActionLogEntity(
                    actionType = AgentActionLogEntity.TYPE_EMERGENCY_STOP,
                    description = "Foydalanuvchi tomonidan favqulodda to'xtatish (Emergency Stop) bosildi",
                    isSuccess = true
                )
            )
        }
    }

    /**
     * Yangi xabar/buyruqni qabul qilish va agent tsiklini ishga tushirish
     */
    fun processPrompt(
        prompt: String,
        onSpeechRequested: ((String) -> Unit)? = null
    ) {
        currentExecutionJob?.cancel()
        currentExecutionJob = scope.launch(Dispatchers.IO) {
            try {
                _agentState.value = AgentState.Thinking("Xavfsizlik tekshiruvi o'tkazilmoqda...")

                // 1. Maxfiy ma'lumotlarni (OTP, karta, parol) Gemini yuborilishidan oldin maskalash
                val maskResult = SensitiveDataMasker.mask(prompt)
                if (maskResult.wasMasked) {
                    actionLogDao.insertLog(
                        AgentActionLogEntity(
                            actionType = AgentActionLogEntity.TYPE_SECURITY_MASK,
                            description = "Maxfiy ma'lumotlar maskalandi: ${maskResult.details.joinToString(", ")}",
                            isSuccess = true
                        )
                    )
                }

                val safePrompt = maskResult.maskedText

                // 2. Chat bazasiga foydalanuvchi xabarini saqlash
                chatRepository.addUserMessage(safePrompt)

                // 3. Faol kalitni aniqlash
                var currentKey = apiKeyRepository.getActiveKey()
                if (currentKey == null) {
                    val errMsg = "Hech qanday faol Gemini API kalit topilmadi. Iltimos, sozlamalar (Kalitlar) bo'limidan kalit qo'shing."
                    chatRepository.addModelMessage(errMsg, null, null)
                    _agentState.value = AgentState.Error(errMsg)
                    return@launch
                }

                var currentModel = PRIMARY_MODEL
                val conversationContents = mutableListOf<GeminiContent>()

                // O'tmishdagi oxirgi kontekstni olish
                val recentHistory = chatRepository.getRecentMessages(6)
                recentHistory.forEach { msg ->
                    if (msg.role == "user") {
                        conversationContents.add(GeminiContent.user(msg.content))
                    } else if (msg.role == "model") {
                        conversationContents.add(GeminiContent.model(msg.content))
                    }
                }

                // Agar kontekstda joriy xabar bo'lmasa, qo'shamiz
                if (conversationContents.none { it.parts.any { p -> p.text == safePrompt } }) {
                    conversationContents.add(GeminiContent.user(safePrompt))
                }

                var loopCount = 0
                var isCompleted = false

                _agentState.value = AgentState.Thinking("Kotib javob tayyorlamoqda...")

                while (loopCount < MAX_AGENT_STEPS && !isCompleted) {
                    loopCount++

                    val requestJson = buildRequestBody(conversationContents)
                    val body = requestJson.toString().toRequestBody("application/json".toMediaType())

                    var response = try {
                        RetrofitClient.api.generateContent(
                            model = currentModel,
                            apiKey = currentKey!!.key,
                            requestBody = body
                        )
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        null
                    }

                    // 429 Rate Limit tekshiruvi va avtomatik rotatsiya
                    if (response == null || response.code() == 429) {
                        val oldLabel = currentKey!!.label
                        _agentState.value = AgentState.Thinking("429 limiti: keyingi kalitga o'tilmoqda...")
                        val rotatedKey = apiKeyRepository.rotateOnRateLimit(
                            currentKey!!.id,
                            "HTTP 429 Too Many Requests"
                        )

                        if (rotatedKey != null) {
                            _agentState.value = AgentState.RotatingKey(
                                oldLabel = oldLabel,
                                newLabel = rotatedKey.label,
                                reason = "429 Limit bo'yicha rotatsiya"
                            )
                            currentKey = rotatedKey

                            // Yangi kalit bilan so'rovni qayta yuborish
                            response = RetrofitClient.api.generateContent(
                                model = currentModel,
                                apiKey = currentKey.key,
                                requestBody = body
                            )
                        } else {
                            // Agar barcha kalitlar limitga yetgan bo'lsa, arzon zaxira modelga (Flash-Lite) o'tish
                            if (currentModel != FALLBACK_MODEL) {
                                currentModel = FALLBACK_MODEL
                                _agentState.value = AgentState.Thinking("Zaxira Flash-Lite modeliga o'tildi...")
                                response = RetrofitClient.api.generateContent(
                                    model = currentModel,
                                    apiKey = currentKey!!.key,
                                    requestBody = body
                                )
                            }
                        }
                    }

                    if (response == null || !response.isSuccessful) {
                        val errBody = response?.errorBody()?.string() ?: "Tarmoq xatosi"
                        val errText = "Gemini so'rovida xatolik (${response?.code() ?: 0}): $errBody"
                        chatRepository.addModelMessage(errText, currentModel, currentKey?.label)
                        _agentState.value = AgentState.Error(errText)
                        return@launch
                    }

                    // Kalit ishlatilganligini qayd etish
                    currentKey?.let { apiKeyRepository.recordKeyUsage(it.id) }

                    val rawResponseString = response.body()?.string() ?: "{}"
                    val responseJson = JSONObject(rawResponseString)

                    val candidates = responseJson.optJSONArray("candidates")
                    if (candidates == null || candidates.length() == 0) {
                        val noResp = "Modeldan javob olinmadi."
                        chatRepository.addModelMessage(noResp, currentModel, currentKey?.label)
                        _agentState.value = AgentState.Idle
                        return@launch
                    }

                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    val partsArray = contentObj?.optJSONArray("parts")

                    var functionCallFound: FunctionCall? = null
                    var textResponseFound: String? = null

                    if (partsArray != null) {
                        for (i in 0 until partsArray.length()) {
                            val part = partsArray.getJSONObject(i)
                            if (part.has("functionCall")) {
                                functionCallFound = FunctionCall.fromJson(part.getJSONObject("functionCall"))
                                break
                            } else if (part.has("text")) {
                                textResponseFound = part.optString("text")
                            }
                        }
                    }

                    // A) Agar model VOSITA (functionCall) talab qilsa:
                    if (functionCallFound != null) {
                        val toolName = functionCallFound.name
                        val toolArgs = functionCallFound.args

                        _agentState.value = AgentState.ExecutingTool(toolName, toolArgs)

                        // 1. Tool chaqiruvini chatga va logga saqlash
                        chatRepository.addToolCallMessage(toolName, JSONObject(toolArgs).toString())
                        actionLogDao.insertLog(
                            AgentActionLogEntity(
                                actionType = AgentActionLogEntity.TYPE_TOOL_EXECUTION,
                                description = "Vosita chaqirildi: $toolName, argumentlar: $toolArgs",
                                isSuccess = true
                            )
                        )

                        // 2. Vositani bajarish
                        val toolResult = toolDispatcher.executeTool(toolName, toolArgs)

                        // 3. Natijani chatga va logga saqlash
                        chatRepository.addToolResultMessage(
                            toolName = toolName,
                            resultSummary = toolResult.userSummary,
                            rawResultJson = JSONObject(toolResult.output).toString()
                        )

                        // 4. Modelga qaytarish uchun parts tayyorlash
                        conversationContents.add(
                            GeminiContent(
                                role = "model",
                                parts = listOf(
                                    GeminiPart(functionCall = functionCallFound)
                                )
                            )
                        )
                        conversationContents.add(
                            GeminiContent(
                                role = "function",
                                parts = listOf(
                                    GeminiPart(
                                        functionResponse = FunctionResponse(
                                            name = toolName,
                                            response = toolResult.output
                                        )
                                    )
                                )
                            )
                        )

                        _agentState.value = AgentState.Thinking("Vosita natijasi tahlil qilinmoqda...")
                        // Tsikl davom etadi va natija Gemini'ga boradi
                    } else {
                        // B) Final matnli javob olindi
                        val finalText = textResponseFound ?: "Javob mavjud emas."
                        chatRepository.addModelMessage(finalText, currentModel, currentKey?.label)

                        isCompleted = true
                        _agentState.value = AgentState.Idle

                        // Ovoz chiqarish (TTS) so'rovi bo'lsa
                        onSpeechRequested?.invoke(finalText)
                    }
                }

                if (!isCompleted) {
                    val limitMsg = "Kotib vazifani yakunlash uchun maksimal qadamlar limitiga (5 ta) yetdi."
                    chatRepository.addModelMessage(limitMsg, currentModel, currentKey?.label)
                    _agentState.value = AgentState.Idle
                }

            } catch (e: CancellationException) {
                _agentState.value = AgentState.Idle
            } catch (e: Exception) {
                val err = "Xatolik yuz berdi: ${e.message}"
                chatRepository.addModelMessage(err, null, null)
                _agentState.value = AgentState.Error(err)
            }
        }
    }

    private fun buildRequestBody(contents: List<GeminiContent>): JSONObject {
        val root = JSONObject()

        // 1. Contents massivi
        val contentsArr = JSONArray()
        contents.forEach { contentsArr.put(it.toJson()) }
        root.put("contents", contentsArr)

        // 2. Tools
        root.put("tools", toolDispatcher.buildGeminiToolsJson())

        // 3. System instruction
        val sysInstructionObj = JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().put("text", SYSTEM_PROMPT))
            })
        }
        root.put("systemInstruction", sysInstructionObj)

        // 4. Generation config
        val genConfig = JSONObject().apply {
            put("temperature", 0.3)
        }
        root.put("generationConfig", genConfig)

        return root
    }
}
