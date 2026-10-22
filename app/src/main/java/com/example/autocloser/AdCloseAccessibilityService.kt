package com.example.autocloser

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AdCloseAccessibilityService : AccessibilityService() {

    private val closeKeywords = listOf(
        "閉じる", "スキップ", "×", "✕", "✖", "close", "skip"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return
        findAndClick(rootNode)
    }

    private fun findAndClick(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.trim()?.lowercase()
        val desc = node.contentDescription?.toString()?.trim()?.lowercase()

        for (keyword in closeKeywords) {
            val kw = keyword.lowercase()
            val matchText = text != null && (text == kw || text == kw.uppercase())
            val matchDesc = desc != null && desc.contains(kw)

            if (matchText || matchDesc) {
                if (node.isClickable) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) {
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return true
                    }
                    parent = parent.parent
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClick(child)) {
                return true
            }
        }
        return false
    }

    override fun onInterrupt() {}
}
