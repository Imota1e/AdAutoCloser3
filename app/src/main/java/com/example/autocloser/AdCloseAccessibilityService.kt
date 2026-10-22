package com.example.autocloser

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AdCloseAccessibilityService : AccessibilityService() {

    private var lastClickTime = 0L

    private val targetTexts = listOf(
        "閉じる", "スキップ", "広告をスキップ",
        "Close", "Skip", "Skip Ad", "Dismiss",
        "×", "✕", "✖", "⨯", "X"
    )

    private val targetIdKeywords = listOf(
        "close", "skip", "dismiss", "btn_close", "ad_close", "close_button"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime < 800) return

        val rootNode = rootInActiveWindow ?: return
        if (rootNode.packageName?.toString() == packageName) return

        if (searchAndClick(rootNode)) {
            lastClickTime = now
        }
    }

    private fun searchAndClick(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        if (node.isVisibleToUser && isTargetNode(node)) {
            if (performClick(node)) {
                return true
            }
        }

        for (i in 0 until node.childCount) {
            if (searchAndClick(node.getChild(i))) {
                return true
            }
        }
        return false
    }

    private fun isTargetNode(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.trim() ?: ""
        val desc = node.contentDescription?.toString()?.trim() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        if (targetTexts.any { text.equals(it, ignoreCase = true) || desc.equals(it, ignoreCase = true) }) {
            return true
        }

        if (targetIdKeywords.any { viewId.contains(it) }) {
            return true
        }

        return false
    }

    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 4) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
            depth++
        }
        return false
    }

    override fun onInterrupt() {}
}
