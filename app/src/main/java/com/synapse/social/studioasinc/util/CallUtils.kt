package com.synapse.social.studioasinc.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object CallUtils {
    fun initiateCall(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        context.startActivity(intent)
    }
}
