package com.example.autocloser

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AdCloseAccessibilityService : AccessibilityService() {

    private var lastClickTime = 0L

    private val targetKeywords = listOf(
        "閉じる", "スキップ", "広告をスキップ", "広告",
        "Close", "Skip", "Skip Ad", "Dismiss",
        "×", "✕", "✖", "⨯", "X", "報酬"
    )

    private val targetIdKeywords = listOf(
        "close", "skip", "dismiss", "btn_close", "ad_close", "close_button", "count_down"
    )

    private val ignoredPackages = listOf(
        "launcher", "trebuchet", "miui", "home", "settings",
        "packageinstaller", "systemui", "pixel", "inputmethod"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val now = System.currentTimeMillis()
        if (now - lastClickTime < 1500) return

        val rootNode = rootInActiveWindow ?: return
        val pkg = rootNode.packageName?.toString()?.lowercase() ?: ""

        if (pkg.isEmpty() || pkg == packageName.lowercase() || ignoredPackages.any { pkg.contains(it) }) {
            return
        }

        // 1. ノード探索によるクリック
        if (searchAndClick(rootNode)) {
            lastClickTime = now
            return
        }

        // 2. 画面内に広告特有の要素（「報酬」や「Google Play」等）が存在する場合の右上座標フォールバックタップ
        if (containsAdIndicator(rootNode)) {
            clickTopRightCorner()
            lastClickTime = now
        }
    }

    private fun searchAndClick(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        if (isTargetNode(node)) {
            val rect = Rect()
            node.getBoundsInScreen(rect)

            // 通常のクリックアクションを試行
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }

            // クリック不可でも座標が存在すればジェスチャータップ
            if (rect.width() > 0 && rect.height() > 0) {
                clickScreenCoordinates(rect.centerX().toFloat(), rect.centerY().toFloat())
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

        if (text.length > 35 || desc.length > 35) return false

        if (targetKeywords.any { text.contains(it, ignoreCase = true) || desc.contains(it, ignoreCase = true) }) {
            return true
        }

        if (targetIdKeywords.any { viewId.contains(it) }) {
            return true
        }

        return false
    }

    private fun containsAdIndicator(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""

        if (text.contains("報酬") || text.contains("インストール") || desc.contains("報酬")) {
            return true
        }

        for (i in 0 until node.childCount) {
            if (containsAdIndicator(node.getChild(i))) return true
        }
        return false
    }

    // 画面右上の「✕」ボタン位置（上部約4%〜6%、右端から約6%〜10%）を直接タップ
    private fun clickTopRightCorner() {
        val metrics = resources.displayMetrics
        val x = metrics.widthPixels * 0.92f
        val y = metrics.heightPixels * 0.05f
        clickScreenCoordinates(x, y)
    }

    private fun clickScreenCoordinates(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
                .build()
            dispatchGesture(gesture, null, null)
        }
    }

    override fun onInterrupt() {}
}
