package com.synapse.social.studioasinc

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.synapse.social.studioasinc.animations.ShimmerFrameLayout

open class BaseMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val body: LinearLayout? = view.findViewById(R.id.body)
    val messageLayout: LinearLayout? = view.findViewById(R.id.message_layout)
    val messageBG: LinearLayout? = view.findViewById(R.id.messageBG)
    val myMessageInfo: LinearLayout? = view.findViewById(R.id.my_message_info)
    val mProfileCard: CardView? = view.findViewById(R.id.mProfileCard)
    val mProfileImage: ImageView? = view.findViewById(R.id.mProfileImage)
    val date: TextView? = view.findViewById(R.id.date)
    val messageState: ImageView? = view.findViewById(R.id.message_state)
    val messageText: TextView? = view.findViewById(R.id.message_text)
    val senderUsername: TextView? = view.findViewById(R.id.senderUsername)
    val shimmerContainer: ShimmerFrameLayout? = view.findViewById(R.id.shimmer_container)
    
    val mRepliedMessageLayout: MaterialCardView? = view.findViewById(R.id.mRepliedMessageLayout)
    val mRepliedMessageLayoutUsername: TextView?
    val mRepliedMessageLayoutMessage: TextView?
    val mRepliedMessageLayoutLeftBar: LinearLayout?
    val mRepliedMessageLayoutImage: ImageView?
    
    init {
        if (mRepliedMessageLayout != null) {
            mRepliedMessageLayoutUsername = mRepliedMessageLayout.findViewById(R.id.mRepliedMessageLayoutUsername)
            mRepliedMessageLayoutMessage = mRepliedMessageLayout.findViewById(R.id.mRepliedMessageLayoutMessage)
            mRepliedMessageLayoutLeftBar = mRepliedMessageLayout.findViewById(R.id.mRepliedMessageLayoutLeftBar)
            mRepliedMessageLayoutImage = mRepliedMessageLayout.findViewById(R.id.mRepliedMessageLayoutImage)
        } else {
            mRepliedMessageLayoutUsername = null
            mRepliedMessageLayoutMessage = null
            mRepliedMessageLayoutLeftBar = null
            mRepliedMessageLayoutImage = null
        }
    }
    
    fun startShimmer() {
        shimmerContainer?.startShimmer()
    }
    
    fun stopShimmer() {
        shimmerContainer?.stopShimmer()
    }
}
