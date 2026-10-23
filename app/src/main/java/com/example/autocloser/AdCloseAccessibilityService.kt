package com.example.autocloser

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AdCloseAccessibilityService : AccessibilityService() {

    private var lastClickTime = 0L

    // 検出したいキーワード（一部に含まれていればヒットするようにします）
    private val targetKeywords = listOf(
        "閉じる", "スキップ", "広告をスキップ",
        "Close", "Skip", "Skip Ad", "Dismiss",
        "×", "✕", "✖", "⨯", "X", "報酬"
    )

    private val targetIdKeywords = listOf(
        "close", "skip", "dismiss", "btn_close", "ad_close", "close_button"
    )

    private val ignoredPackages = listOf(
        "launcher", "trebuchet", "miui", "home", "settings", 
        "packageinstaller", "systemui", "pixel"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastClickTime < 1500) return

        val rootNode = rootInActiveWindow ?: return
        val pkg = rootNode.packageName?.toString()?.lowercase() ?: ""

        if (pkg == packageName.lowercase() || ignoredPackages.any { pkg.contains(it) }) {
            return
        }

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

        // 長すぎる文章は除外（ただし「報酬を獲得しました ✕」などを拾うため少し長さを許容）
        if (text.length > 30 || desc.length > 30) return false

        // 部分一致（contains）に変更し、文字の中にキーワードが含まれていれば検知
        if (targetKeywords.any { text.contains(it, ignoreCase = true) || desc.contains(it, ignoreCase = true) }) {
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
        while (current != null && depth < 5) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
            depth++
        }
        // もし親階層にクリック可能な要素がない場合でも、ノード自体に直接クリックを試みる
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    override fun onInterrupt() {}
}
