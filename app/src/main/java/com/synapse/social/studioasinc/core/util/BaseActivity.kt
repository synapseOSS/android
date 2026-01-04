package com.synapse.social.studioasinc

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applyWindowInsetsToContent()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        applyWindowInsetsToContent()
    }

    private fun applyWindowInsetsToContent() {
        val rootView = findViewById<View>(android.R.id.content)?.let {
            (it as? android.view.ViewGroup)?.getChildAt(0)
        } ?: return
        
        applyWindowInsets(rootView)
    }

    protected fun applyWindowInsets(rootView: View) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
