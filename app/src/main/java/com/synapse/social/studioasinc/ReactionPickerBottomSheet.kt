package com.synapse.social.studioasinc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.synapse.social.studioasinc.model.ReactionType

/**
 * Bottom sheet dialog for selecting reactions on a post
 */
class ReactionPickerBottomSheet : BottomSheetDialogFragment() {

    private var onReactionSelected: ((ReactionType) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.reaction_picker_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupReactionButtons(view)
        animateEntrance(view)
    }

    private fun setupReactionButtons(view: View) {
        val reactionViews = listOf(
            view.findViewById<View>(R.id.reactionLike) to ReactionType.LIKE,
            view.findViewById<View>(R.id.reactionLove) to ReactionType.LOVE,
            view.findViewById<View>(R.id.reactionHaha) to ReactionType.HAHA,
            view.findViewById<View>(R.id.reactionWow) to ReactionType.WOW,
            view.findViewById<View>(R.id.reactionSad) to ReactionType.SAD,
            view.findViewById<View>(R.id.reactionAngry) to ReactionType.ANGRY
        )

        reactionViews.forEach { (reactionView, reactionType) ->
            reactionView.setOnClickListener {
                animateSelection(it)
                onReactionSelected?.invoke(reactionType)
                dismiss()
            }

            // Add hover effect
            reactionView.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        v.animate()
                            .scaleX(1.2f)
                            .scaleY(1.2f)
                            .setDuration(100)
                            .start()
                    }
                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()
                    }
                }
                false
            }
        }
    }

    private fun animateEntrance(view: View) {
        val container = view.findViewById<ViewGroup>(R.id.reactionsContainer)
        
        container.post {
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                child.alpha = 0f
                child.scaleX = 0.5f
                child.scaleY = 0.5f
                
                child.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay((i * 50).toLong())
                    .setDuration(200)
                    .setInterpolator(OvershootInterpolator())
                    .start()
            }
        }
    }

    private fun animateSelection(view: View) {
        view.animate()
            .scaleX(1.5f)
            .scaleY(1.5f)
            .setDuration(150)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    fun setOnReactionSelectedListener(listener: (ReactionType) -> Unit) {
        onReactionSelected = listener
    }

    companion object {
        fun newInstance(onReactionSelected: (ReactionType) -> Unit): ReactionPickerBottomSheet {
            return ReactionPickerBottomSheet().apply {
                this.onReactionSelected = onReactionSelected
            }
        }
    }
}
