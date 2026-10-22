package com.example.autocloser

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.Button

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rootView = findViewById<ViewGroup>(android.R.id.content)
        findButton(rootView)?.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun findButton(group: ViewGroup): Button? {
        for (i in 0 until group.childCount) {
            val child: View = group.getChildAt(i)
            if (child is Button) return child
            if (child is ViewGroup) {
                val found = findButton(child)
                if (found != null) return found
            }
        }
        return null
    }
}
