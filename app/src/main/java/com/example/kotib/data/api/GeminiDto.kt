package com.example.kotib.data.api

import org.json.JSONArray
import org.json.JSONObject

/**
 * Gemini Function Calling ma'lumotlar modellari
 */

data class GeminiPart(
    val text: String? = null,
    val functionCall: FunctionCall? = null,
    val functionResponse: FunctionResponse? = null
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        text?.let { json.put("text", it) }
        functionCall?.let { json.put("functionCall", it.toJson()) }
        functionResponse?.let { json.put("functionResponse", it.toJson()) }
        return json
    }

    companion object {
        fun fromText(text: String) = GeminiPart(text = text)

        fun fromFunctionCall(name: String, args: Map<String, Any?>) =
            GeminiPart(functionCall = FunctionCall(name = name, args = args))

        fun fromFunctionResponse(name: String, response: Map<String, Any?>) =
            GeminiPart(functionResponse = FunctionResponse(name = name, response = response))

        fun fromJson(json: JSONObject): GeminiPart {
            val text = if (json.has("text")) json.optString("text") else null
            val functionCall = if (json.has("functionCall")) {
                FunctionCall.fromJson(json.getJSONObject("functionCall"))
            } else null
            val functionResponse = if (json.has("functionResponse")) {
                FunctionResponse.fromJson(json.getJSONObject("functionResponse"))
            } else null
            return GeminiPart(text, functionCall, functionResponse)
        }
    }
}

data class FunctionCall(
    val name: String,
    val args: Map<String, Any?>
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("name", name)
        val argsJson = JSONObject()
        args.forEach { (k, v) -> argsJson.put(k, v) }
        json.put("args", argsJson)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): FunctionCall {
            val name = json.optString("name")
            val argsMap = mutableMapOf<String, Any?>()
            if (json.has("args")) {
                val argsObj = json.optJSONObject("args")
                argsObj?.keys()?.forEach { key ->
                    argsMap[key] = argsObj.opt(key)
                }
            }
            return FunctionCall(name, argsMap)
        }
    }
}

data class FunctionResponse(
    val name: String,
    val response: Map<String, Any?>
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("name", name)
        val respJson = JSONObject()
        response.forEach { (k, v) -> respJson.put(k, v) }
        json.put("response", respJson)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): FunctionResponse {
            val name = json.optString("name")
            val respMap = mutableMapOf<String, Any?>()
            if (json.has("response")) {
                val respObj = json.optJSONObject("response")
                respObj?.keys()?.forEach { key ->
                    respMap[key] = respObj.opt(key)
                }
            }
            return FunctionResponse(name, respMap)
        }
    }
}

data class GeminiContent(
    val role: String, // "user", "model", "function"
    val parts: List<GeminiPart>
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("role", role)
        val partsArray = JSONArray()
        parts.forEach { partsArray.put(it.toJson()) }
        json.put("parts", partsArray)
        return json
    }

    companion object {
        fun user(text: String) = GeminiContent("user", listOf(GeminiPart.fromText(text)))
        fun model(text: String) = GeminiContent("model", listOf(GeminiPart.fromText(text)))
        fun function(name: String, response: Map<String, Any?>) =
            GeminiContent("function", listOf(GeminiPart.fromFunctionResponse(name, response)))

        fun fromJson(json: JSONObject): GeminiContent {
            val role = json.optString("role", "model")
            val partsList = mutableListOf<GeminiPart>()
            val partsArr = json.optJSONArray("parts")
            if (partsArr != null) {
                for (i in 0 until partsArr.length()) {
                    partsArr.optJSONObject(i)?.let { partObj ->
                        partsList.add(GeminiPart.fromJson(partObj))
                    }
                }
            }
            return GeminiContent(role, partsList)
        }
    }
}

data class ToolDeclaration(
    val name: String,
    val description: String,
    val parameters: Map<String, Any?>
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("name", name)
        json.put("description", description)
        val paramsJson = JSONObject(parameters)
        json.put("parameters", paramsJson)
        return json
    }
}

data class GeminiResponseResult(
    val text: String?,
    val functionCalls: List<FunctionCall>,
    val rawJson: String
)
