package com.synapse.social.studioasinc.util

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ScrollView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView

/**
 * Utility for managing keyboard visibility and behavior
 * Improves UX by providing smooth keyboard transitions
 */
object KeyboardUtil {

    /**
     * Show keyboard and focus on EditText
     * 
     * @param editText The EditText to focus
     */
    fun showKeyboard(editText: EditText) {
        editText.requestFocus()
        editText.postDelayed({
            val imm = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    /**
     * Hide keyboard from view
     * 
     * @param view The view to hide keyboard from
     */
    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Hide keyboard from activity
     * 
     * @param activity The activity to hide keyboard from
     */
    fun hideKeyboard(activity: Activity) {
        val view = activity.currentFocus ?: activity.window.decorView
        hideKeyboard(view)
    }

    /**
     * Check if keyboard is currently visible
     * 
     * @param view The view to check keyboard visibility for
     * @return True if keyboard is visible
     */
    fun isKeyboardVisible(view: View): Boolean {
        val insets = ViewCompat.getRootWindowInsets(view)
        return insets?.isVisible(WindowInsetsCompat.Type.ime()) ?: false
    }

    /**
     * Toggle keyboard visibility
     * 
     * @param view The view to toggle keyboard for
     */
    fun toggleKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    /**
     * Setup keyboard listener for view
     * 
     * @param view The view to listen for keyboard changes
     * @param onKeyboardVisible Callback when keyboard becomes visible
     * @param onKeyboardHidden Callback when keyboard is hidden
     */
    fun setupKeyboardListener(
        view: View,
        onKeyboardVisible: ((height: Int) -> Unit)? = null,
        onKeyboardHidden: (() -> Unit)? = null
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isKeyboardVisible = imeInsets.bottom > 0

            if (isKeyboardVisible) {
                onKeyboardVisible?.invoke(imeInsets.bottom)
            } else {
                onKeyboardHidden?.invoke()
            }

            insets
        }
    }

    /**
     * Adjust view padding when keyboard appears
     * Useful for keeping input fields visible above keyboard
     * 
     * @param view The view to adjust padding for
     * @param originalBottomPadding The original bottom padding to restore when keyboard hides
     */
    fun adjustViewForKeyboard(view: View, originalBottomPadding: Int = 0) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomPadding = if (imeInsets.bottom > 0) {
                imeInsets.bottom
            } else {
                originalBottomPadding
            }
            
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                bottomPadding
            )
            
            insets
        }
    }

    /**
     * Smooth scroll to view when keyboard appears
     * Ensures the focused view is visible above the keyboard
     * 
     * @param scrollView The parent scroll view (ScrollView or NestedScrollView)
     * @param targetView The view to scroll to
     */
    fun scrollToViewWhenKeyboardAppears(scrollView: View, targetView: View) {
        setupKeyboardListener(scrollView, onKeyboardVisible = { keyboardHeight ->
            scrollView.post {
                val location = IntArray(2)
                targetView.getLocationInWindow(location)
                val targetY = location[1]
                val scrollY = scrollView.scrollY
                val screenHeight = scrollView.height
                val targetBottom = targetY + targetView.height

                // Check if target view is hidden by keyboard
                if (targetBottom > screenHeight - keyboardHeight) {
                    val scrollAmount = targetBottom - (screenHeight - keyboardHeight) + 50 // 50dp extra padding
                    when (scrollView) {
                        is NestedScrollView -> scrollView.smoothScrollBy(0, scrollAmount)
                        is ScrollView -> scrollView.smoothScrollBy(0, scrollAmount)
                    }
                }
            }
        })
    }
}
