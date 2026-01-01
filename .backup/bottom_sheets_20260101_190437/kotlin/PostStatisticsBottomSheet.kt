package com.synapse.social.studioasinc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.synapse.social.studioasinc.model.Post

class PostStatisticsBottomSheet : BottomSheetDialogFragment() {

    private lateinit var reactionsCount: TextView
    private lateinit var commentsCount: TextView
    private lateinit var viewsCount: TextView
    private lateinit var resharesCount: TextView

    private var post: Post? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_post_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        displayStatistics()
    }

    private fun initializeViews(view: View) {
        reactionsCount = view.findViewById(R.id.reactionsCount)
        commentsCount = view.findViewById(R.id.commentsCount)
        viewsCount = view.findViewById(R.id.viewsCount)
        resharesCount = view.findViewById(R.id.resharesCount)
    }

    private fun displayStatistics() {
        post?.let { post ->
            reactionsCount.text = post.getTotalReactionsCount().toString()
            commentsCount.text = post.commentsCount.toString()
            viewsCount.text = post.viewsCount.toString()
            resharesCount.text = post.resharesCount.toString()
        }
    }

    companion object {
        fun newInstance(post: Post): PostStatisticsBottomSheet {
            return PostStatisticsBottomSheet().apply {
                this.post = post
            }
        }
    }
}
