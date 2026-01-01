package com.synapse.social.studioasinc.attachments

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.synapse.social.studioasinc.AsyncUploadService
import com.synapse.social.studioasinc.FileUtil
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.UploadFiles
import java.util.ArrayList
import java.util.HashMap

class Rv_attacmentListAdapter(
    private val context: Context,
    private val data: ArrayList<HashMap<String, Any>>,
    private val attachmentLayoutListHolder: View
) : RecyclerView.Adapter<Rv_attacmentListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.chat_attactment, parent, false)
        val layoutParams = RecyclerView.LayoutParams(dpToPx(100), dpToPx(100))
        view.layoutParams = layoutParams
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemView = holder.itemView

        // val cardMediaItem = itemView.findViewById<androidx.cardview.widget.CardView>(R.id.cardMediaItem)
        // val imageWrapperRL = itemView.findViewById<RelativeLayout>(R.id.imageWrapperRL)
        val previewIV = itemView.findViewById<ImageView>(R.id.previewIV)
        val overlayLL = itemView.findViewById<LinearLayout>(R.id.overlayLL)
        val uploadProgressCPI = itemView.findViewById<CircularProgressIndicator>(R.id.uploadProgressCPI)
        val closeIV = itemView.findViewById<ImageView>(R.id.closeIV)

        if (position < 0 || position >= data.size) {
            Log.w("RvAttachment", "Invalid position in attachment adapter: $position")
            itemView.visibility = View.GONE
            return
        }

        val itemData = data[position]
        // In Kotlin itemData can't be null if data is list of non-null HashMaps.
        // But if data can contain nulls, then check. Assuming data not null.

        if (!itemData.containsKey("localPath") || itemData["localPath"] == null) {
            itemView.visibility = View.GONE
            itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
            return
        }

        itemView.visibility = View.VISIBLE
        itemView.layoutParams = RecyclerView.LayoutParams(dpToPx(100), dpToPx(100))

        val localPath = itemData["localPath"].toString()
        try {
            previewIV.setImageDrawable(null)
            previewIV.setImageBitmap(FileUtil.decodeSampleBitmapFromPath(localPath, 1024, 1024))
        } catch (e: Exception) {
            Log.e("RvAttachment", "Error loading image preview: " + e.message)
            previewIV.setImageResource(R.drawable.ph_imgbluredsqure)
        }

        val uploadState = itemData.getOrDefault("uploadState", "pending").toString()
        var progress = 0
        if (itemData.containsKey("uploadProgress")) {
            try {
                progress = itemData["uploadProgress"].toString().toDouble().toInt()
            } catch (e: NumberFormatException) {
                Log.w("RvAttachment", "Invalid upload progress value: " + itemData["uploadProgress"])
                progress = 0
            }
        }

        when (uploadState) {
            "uploading" -> {
                overlayLL.visibility = View.VISIBLE
                overlayLL.setBackgroundColor(-0x7fffffff) // 0x80000000
                uploadProgressCPI.visibility = View.VISIBLE
                uploadProgressCPI.progress = progress
                closeIV.visibility = View.GONE
            }
            "success" -> {
                overlayLL.visibility = View.GONE
                uploadProgressCPI.visibility = View.GONE
                closeIV.visibility = View.VISIBLE
            }
            "failed" -> {
                overlayLL.visibility = View.VISIBLE
                overlayLL.setBackgroundColor(-0x7f2c0000 - 1 + 0x2C0000 - 0xD0D0) // Manual calc for 0x80D32F2F...
                // Color.parseColor or resource is better but sticking to literal conversion
                // 0x80D32F2F is -2133643473
                overlayLL.setBackgroundColor(-0x7f2cd0d1)
                uploadProgressCPI.visibility = View.GONE
                closeIV.visibility = View.VISIBLE
            }
            else -> {
                overlayLL.visibility = View.GONE
                uploadProgressCPI.visibility = View.GONE
                closeIV.visibility = View.VISIBLE
            }
        }

        closeIV.setOnClickListener {
            val adapterPosition = holder.bindingAdapterPosition
            if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= data.size) {
                Log.w("RvAttachment", "Invalid position for removal: $adapterPosition")
                return@setOnClickListener
            }

            val currentItemData = data[adapterPosition]

            if ("uploading" == currentItemData["uploadState"]) {
                val localPathVal = currentItemData["localPath"].toString()
                AsyncUploadService.cancelUpload(context, localPathVal)
            }

            data.removeAt(adapterPosition)
            notifyItemRemoved(adapterPosition)
            notifyItemRangeChanged(adapterPosition, data.size - adapterPosition)

            if (currentItemData.containsKey("publicId")) {
                val publicId = currentItemData["publicId"].toString()
                if (publicId.isNotEmpty()) {
                    UploadFiles.deleteByPublicId(publicId, object : UploadFiles.DeleteCallback {
                        override fun onSuccess() {
                            Log.d("RvAttachment", "Successfully deleted attachment: $publicId")
                        }

                        override fun onFailure(error: String) {
                            Log.e("RvAttachment", "Failed to delete attachment: $error")
                        }
                    })
                }
            }

            if (data.isEmpty()) {
                attachmentLayoutListHolder.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v)

    private fun dpToPx(dp: Int): Int {
        try {
            if (context.resources != null && context.resources.displayMetrics != null) {
                return (dp * context.resources.displayMetrics.density).toInt()
            }
        } catch (e: Exception) {
            Log.e("RvAttachment", "Error converting dp to px: " + e.message)
        }
        return dp
    }
}
