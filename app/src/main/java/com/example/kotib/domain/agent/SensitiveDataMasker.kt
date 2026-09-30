package com.example.kotib.domain.agent

/**
 * Xavfsizlik filtri:
 * OTP, bank karta raqamlari, parollar va maxfiy ma'lumotlarni
 * tashqi sun'iy intellekt (Gemini)ga yuborilishidan oldin avtomatik maskalaydi.
 */
object SensitiveDataMasker {

    // Bank kartalari (Uzcard, Humo, Visa, Mastercard - 16 yoki 19 raqamli)
    private val CARD_PATTERN = Regex("""\b(?:\d[ -]*?){13,19}\b""")

    // 4-8 raqamli tasdiqlash kodlari (OTP/SMS code)
    private val OTP_PATTERN = Regex(
        """(?i)\b(?:kod|kodi|code|otp|tasdiqlash|sms|parol)\s*[:=–-]?\s*([0-9]{4,8})\b"""
    )

    // Oddiy 6 raqamli mustaqil kodlar
    private val STANDALONE_OTP_PATTERN = Regex("""\b\d{6}\b""")

    // Parol va PIN kodlar
    private val PASSWORD_PATTERN = Regex(
        """(?i)\b(?:parol|password|pass|pin|pin-kod|pincode)\s*[:=–-]?\s*([^\s,;]{3,30})"""
    )

    data class MaskResult(
        val maskedText: String,
        val wasMasked: Boolean,
        val details: List<String>
    )

    fun mask(input: String): MaskResult {
        var text = input
        var wasMasked = false
        val details = mutableListOf<String>()

        // 1. Bank kartalarini maskalash (faqat birinchi 4 va oxirgi 4 raqam qoladi)
        text = CARD_PATTERN.replace(text) { match ->
            val raw = match.value.filter { it.isDigit() }
            if (raw.length in 13..19) {
                wasMasked = true
                details.add("Bank karta raqami maskalandi")
                "${raw.take(4)} **** **** ${raw.takeLast(4)}"
            } else {
                match.value
            }
        }

        // 2. Maxfiy so'z bilan kelgan OTP kodlarni maskalash
        text = OTP_PATTERN.replace(text) { match ->
            wasMasked = true
            details.add("SMS/OTP tasdiqlash kodi maskalandi")
            val full = match.value
            val otpCode = match.groupValues[1]
            full.replace(otpCode, "[MAXFIY-OTP]")
        }

        // 3. Parollarni maskalash
        text = PASSWORD_PATTERN.replace(text) { match ->
            wasMasked = true
            details.add("Parol yoki PIN kod maskalandi")
            val full = match.value
            val secret = match.groupValues[1]
            full.replace(secret, "[MAXFIY-PAROL]")
        }

        return MaskResult(
            maskedText = text,
            wasMasked = wasMasked,
            details = details
        )
    }
}
