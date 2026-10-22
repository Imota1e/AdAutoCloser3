package com.example.autocloser

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AdCloseAccessibilityService : AccessibilityService() {

    private var lastClickTime = 0L

    // 検出対象のテキスト・説明ラベル
    private val targetTexts = listOf(
        "閉じる", "スキップ", "広告をスキップ",
        "Close", "Skip", "Skip Ad", "Dismiss",
        "×", "✕", "✖", "⨯", "X"
    )

    // 検出対象のボタンIDキーワード
    private val targetIdKeywords = listOf(
        "close", "skip", "dismiss", "btn_close", "ad_close", "close_button"
    )

    // 動作させないアプリ（ホーム画面やシステム）のパッケージ名キーワード
    private val ignoredPackages = listOf(
        "launcher", "trebuchet", "miui", "home", "settings", 
        "packageinstaller", "systemui", "pixel"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 画面の変更やウィンドウ状態の切り替わり時のみ反応させる
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val now = System.currentTimeMillis()
        // 連続クリックによる暴走を防ぐため、間隔を2秒に広げる
        if (now - lastClickTime < 2000) return

        val rootNode = rootInActiveWindow ?: return
        val pkg = rootNode.packageName?.toString()?.lowercase() ?: ""

        // 自アプリやホーム画面、システム画面では絶対に動作させない
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

        // 長すぎるテキストは誤爆の元なので除外
        if (text.length > 15 || desc.length > 15) return false

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
