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

object SketchwareUtil {

    fun sortListMap(
        listMap: ArrayList<HashMap<String, Any>>,
        key: String,
        isNumber: Boolean,
        ascending: Boolean
    ) {
        listMap.sortWith { map1, map2 ->
            try {
                if (isNumber) {
                    val count1 = map1[key].toString().toInt()
                    val count2 = map2[key].toString().toInt()
                    if (ascending) count1.compareTo(count2) else count2.compareTo(count1)
                } else {
                    val str1 = map1[key].toString()
                    val str2 = map2[key].toString()
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

    fun copyFromInputStream(inputStream: InputStream): String? {
        return try {
            inputStream.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }
        } catch (e: Exception) {
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

    fun showMessage(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
        return ArrayList<Double>().apply {
            for (i in 0 until list.count) {
                if (list.isItemChecked(i)) {
                    add(i.toDouble())
                }
            }
        }
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
