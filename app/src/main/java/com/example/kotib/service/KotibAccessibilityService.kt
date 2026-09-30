package com.example.kotib.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Haqiqiy Android Accessibility Service (UI Avtomatlashtirish):
 * Ekranni o'qish, tugmalarni bosish, orqaga/bosh ekranga qaytish,
 * bildirishnomalar panelini tushirish va skrinshot olish imkonini beradi.
 * Root talab qilmaydi!
 */
class KotibAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Kerak bo'lganda ekran o'zgarishlarini kuzatish
    }

    override fun onInterrupt() {
        // Xizmat to'xtatilganda
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    companion object {
        @Volatile
        var instance: KotibAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null

        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }

        /**
         * Global harakatlar: Bosh ekran, Orqaga, Bildirishnomalar paneli, Ekranni qulflash
         */
        fun performGlobalAction(action: Int): Boolean {
            val service = instance ?: return false
            return service.performGlobalAction(action)
        }

        /**
         * Ekranda ko'rinib turgan matnlar va tugmalarni o'qib, AI uchun matn xulosasini tuzadi
         */
        fun dumpVisibleScreenTexts(): String {
            val service = instance ?: return "Accessibility xizmati yoqilmagan"
            val rootNode = service.rootInActiveWindow ?: return "Faol ekran oynasi topilmadi"

            val texts = mutableListOf<String>()
            extractTextsRecursive(rootNode, texts)
            return if (texts.isEmpty()) "Ekranda matn topilmadi" else texts.joinToString("\n• ", prefix = "• ")
        }

        private fun extractTextsRecursive(node: AccessibilityNodeInfo?, result: MutableList<String>) {
            if (node == null) return
            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            val isClickable = node.isClickable

            val item = when {
                !text.isNullOrBlank() && !desc.isNullOrBlank() && text != desc -> "$text ($desc)"
                !text.isNullOrBlank() -> text
                !desc.isNullOrBlank() -> desc
                else -> null
            }

            if (!item.isNullOrBlank() && !result.contains(item)) {
                val tag = if (isClickable) "[Tugma] " else ""
                result.add("$tag$item")
            }

            for (i in 0 until node.childCount) {
                extractTextsRecursive(node.getChild(i), result)
            }
        }

        /**
         * Ko'rsatilgan matnli tugmani ekranda topib bosish
         */
        fun clickNodeByText(targetText: String): Boolean {
            val service = instance ?: return false
            val rootNode = service.rootInActiveWindow ?: return false

            val nodes = rootNode.findAccessibilityNodeInfosByText(targetText)
            for (node in nodes) {
                if (node.isClickable) {
                    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) {
                        return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                    parent = parent.parent
                }
            }
            return false
        }

        /**
         * Pastga yoki yuqoriga aylantirish (Scroll)
         */
        fun scroll(forward: Boolean): Boolean {
            val service = instance ?: return false
            val rootNode = service.rootInActiveWindow ?: return false

            val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            return rootNode.performAction(action)
        }
    }
}
