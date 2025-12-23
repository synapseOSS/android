package com.synapse.social.studioasinc

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ListView
import android.widget.Toast
import java.io.InputStream
import java.util.Collections
import android.os.Handler
import android.os.Looper

/**
 * Utility functions for Sketchware-generated code and general helpers.
 * Refactored for safety, performance, and code hygiene.
 */
object SketchwareUtil {

    /**
     * Sorts a list of maps securely.
     * Uses generics to avoid raw type casting issues.
     */
    fun sortListMap(
        listMap: ArrayList<HashMap<String, Any>>,
        key: String,
        isNumber: Boolean,
        ascending: Boolean
    ) {
        Collections.sort(listMap) { map1, map2 ->
            try {
                val val1 = map1[key]
                val val2 = map2[key]

                if (val1 == null && val2 == null) return@sort 0
                if (val1 == null) return@sort if (ascending) -1 else 1
                if (val2 == null) return@sort if (ascending) 1 else -1

                if (isNumber) {
                    // Safe parsing with default 0
                    val num1 = val1.toString().toDoubleOrNull() ?: 0.0
                    val num2 = val2.toString().toDoubleOrNull() ?: 0.0
                    if (ascending) num1.compareTo(num2) else num2.compareTo(num1)
                } else {
                    val str1 = val1.toString()
                    val str2 = val2.toString()
                    if (ascending) str1.compareTo(str2) else str2.compareTo(str1)
                }
            } catch (e: Exception) {
                0
            }
        }
    }

    fun isConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Reads InputStream to String efficiently.
     */
    fun copyFromInputStream(inputStream: InputStream): String? {
        return try {
            inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun hideKeyboard(context: Context, view: View?) {
        view?.let {
            val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            inputMethodManager?.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    fun showKeyboard(context: Context, view: View?) {
        view?.let {
            val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            inputMethodManager?.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    /**
     * Shows a toast message safely on the UI thread.
     */
    fun showMessage(context: Context, message: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getLocationX(view: View): Int {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        return location[0]
    }

    fun getLocationY(view: View): Int {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        return location[1]
    }

    fun getRandom(min: Int, max: Int): Int {
        return (min..max).random()
    }

    fun getCheckedItemPositionsToArray(list: ListView): ArrayList<Double> {
        val result = ArrayList<Double>()
        val sparseBooleanArray = list.checkedItemPositions
        for (i in 0 until sparseBooleanArray.size()) {
            if (sparseBooleanArray.valueAt(i)) {
                result.add(sparseBooleanArray.keyAt(i).toDouble())
            }
        }
        return result
    }

    fun getDip(context: Context, input: Int): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            input.toFloat(),
            context.resources.displayMetrics
        )
    }

    fun getDisplayWidthPixels(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    fun getDisplayHeightPixels(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }

    fun getAllKeysFromMap(map: Map<String, Any>?, output: ArrayList<String>?) {
        output?.apply {
            clear()
            map?.keys?.let { addAll(it) }
        }
    }
}

// Extension functions for more idiomatic Kotlin usage
fun Context.showMessage(message: String) {
    SketchwareUtil.showMessage(this, message)
}

fun Context.isConnected(): Boolean {
    return SketchwareUtil.isConnected(this)
}

fun View.hideKeyboard() {
    SketchwareUtil.hideKeyboard(context, this)
}

fun View.showKeyboard() {
    SketchwareUtil.showKeyboard(context, this)
}
