package com.synapse.social.studioasinc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.synapse.social.studioasinc.animations.textview.TVeffects
import com.synapse.social.studioasinc.styling.MarkdownRenderer

class ContentDisplayBottomSheetDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_content_display, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleTextView = view.findViewById<TextView>(R.id.summary_title)
        val contentTextView = view.findViewById<TVeffects>(R.id.summary_text)

        if (arguments != null) {
            val title = requireArguments().getString(ARG_CONTENT_TITLE)
            val text = requireArguments().getString(ARG_CONTENT_TEXT)

            if (title != null) {
                titleTextView.text = title
            }

            if (text != null) {
                // Use MarkdownRenderer for consistent styling
                MarkdownRenderer.get(requireContext()).render(contentTextView, text)
            }
        }

        val scrollView = view.findViewById<ScrollView>(R.id.scroll_view)
        scrollView.setOnTouchListener(object : View.OnTouchListener {
            private var startY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> startY = event.y
                    MotionEvent.ACTION_MOVE -> {
                        val y = event.y
                        val dy = y - startY

                        // Scrolling up
                        if (dy < 0 && scrollView.canScrollVertically(1)) {
                            v.parent.requestDisallowInterceptTouchEvent(true)
                        } else if (dy > 0 && scrollView.canScrollVertically(-1)) {
                            v.parent.requestDisallowInterceptTouchEvent(true)
                        }
                    }
                }
                return false
            }
        })
    }

    companion object {
        private const val ARG_CONTENT_TEXT = "ARG_CONTENT_TEXT"
        private const val ARG_CONTENT_TITLE = "ARG_CONTENT_TITLE"

        fun newInstance(text: String?, title: String?): ContentDisplayBottomSheetDialogFragment {
            val fragment = ContentDisplayBottomSheetDialogFragment()
            val args = Bundle()
            args.putString(ARG_CONTENT_TEXT, text)
            args.putString(ARG_CONTENT_TITLE, title)
            fragment.arguments = args
            return fragment
        }
    }
}
