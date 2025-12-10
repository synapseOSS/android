package com.synapse.social.studioasinc.util

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Enhanced keyboard helper with smooth animations
 * Handles keyboard visibility and adjusts UI accordingly
 */
object KeyboardHelper {

    /**
     * Show keyboard with smooth animation
     */
    fun showKeyboard(editText: EditText) {
        editText.requestFocus()
        editText.postDelayed({
            val imm = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    /**
     * Hide keyboard smoothly
     */
    fun hideKeyboard(activity: Activity) {
        val view = activity.currentFocus ?: activity.window.decorView
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Hide keyboard from specific view
     */
    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Check if keyboard is visible
     */
    fun isKeyboardVisible(rootView: View): Boolean {
        val insets = ViewCompat.getRootWindowInsets(rootView)
        return insets?.isVisible(WindowInsetsCompat.Type.ime()) ?: false
    }

    /**
     * Setup keyboard visibility listener
     */
    fun setupKeyboardListener(
        rootView: View,
        onKeyboardShown: ((height: Int) -> Unit)? = null,
        onKeyboardHidden: (() -> Unit)? = null
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isKeyboardVisible = imeInsets.bottom > 0
            
            if (isKeyboardVisible) {
                onKeyboardShown?.invoke(imeInsets.bottom)
            } else {
                onKeyboardHidden?.invoke()
            }
            
            insets
        }
    }

    /**
     * Adjust view when keyboard appears
     * Smoothly scrolls content to keep focused view visible
     */
    fun adjustViewForKeyboard(
        rootView: View,
        targetView: View,
        onAdjust: ((keyboardHeight: Int) -> Unit)? = null
    ) {
        rootView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            private var wasKeyboardVisible = false
            
            override fun onGlobalLayout() {
                val insets = ViewCompat.getRootWindowInsets(rootView)
                val imeInsets = insets?.getInsets(WindowInsetsCompat.Type.ime())
                val isKeyboardVisible = (imeInsets?.bottom ?: 0) > 0
                
                if (isKeyboardVisible != wasKeyboardVisible) {
                    wasKeyboardVisible = isKeyboardVisible
                    
                    if (isKeyboardVisible) {
                        val keyboardHeight = imeInsets?.bottom ?: 0
                        onAdjust?.invoke(keyboardHeight)
                        
                        // Scroll to keep target view visible
                        targetView.postDelayed({
                            targetView.requestFocus()
                        }, 100)
                    }
                }
            }
        })
    }

    /**
     * Toggle keyboard visibility
     */
    fun toggleKeyboard(context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    /**
     * Clear focus and hide keyboard
     */
    fun clearFocusAndHideKeyboard(view: View) {
        view.clearFocus()
        hideKeyboard(view)
    }

    /**
     * Setup smooth keyboard handling for EditText
     * Automatically adjusts padding when keyboard appears
     */
    fun setupSmoothKeyboardHandling(
        editText: EditText,
        containerView: View,
        onKeyboardStateChanged: ((isVisible: Boolean, height: Int) -> Unit)? = null
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(containerView) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isKeyboardVisible = imeInsets.bottom > 0
            
            // Animate padding change
            view.animate()
                .translationY(-imeInsets.bottom.toFloat())
                .setDuration(250)
                .start()
            
            onKeyboardStateChanged?.invoke(isKeyboardVisible, imeInsets.bottom)
            
            insets
        }
    }
}
