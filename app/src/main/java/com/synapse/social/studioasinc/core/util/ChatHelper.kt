package com.synapse.social.studioasinc.core.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class ChatHelper(private val context: Context) {

    fun onBackPressed() {
        val activity = context as? Activity ?: return
        val intent = activity.intent
        if (intent.hasExtra("ORIGIN_KEY")) {
            val originSimpleName = intent.getStringExtra("ORIGIN_KEY")
            if (!originSimpleName.isNullOrEmpty() && originSimpleName != "null") {
                try {
                    val packageName = "com.synapse.social.studioasinc"
                    // Handle activities that were moved to sub-packages
                    val className = originSimpleName.trim()
                    val fullClassName = when (className) {
                        "MainActivity" -> "$packageName.ui.main.MainActivity"
                        "ChatActivity" -> "$packageName.ui.chat.ChatActivity"
                        else -> "$packageName.$className"
                    }
                    val clazz = Class.forName(fullClassName)

                    val newIntent = Intent(context, clazz)
                    if ("ProfileActivity" == originSimpleName.trim()) {
                        if (intent.hasExtra("uid")) {
                            newIntent.putExtra("uid", intent.getStringExtra("uid"))
                        } else {
                            Toast.makeText(context, "Error: UID is required for ProfileActivity", Toast.LENGTH_SHORT).show()
                            activity.finish()
                            return
                        }
                    }
                    context.startActivity(newIntent)
                    activity.finish()
                    return
                } catch (e: ClassNotFoundException) {
                    Log.e("ChatActivity", "Activity class not found: $originSimpleName", e)
                    Toast.makeText(context, "Error: Activity not found", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("ChatActivity", "Failed to start activity: $originSimpleName", e)
                    Toast.makeText(context, "Error: Failed to start activity", Toast.LENGTH_SHORT).show()
                }
            }
        }
        activity.finish()
    }
}
