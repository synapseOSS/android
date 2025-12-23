package com.synapse.social.studioasinc.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.synapse.social.studioasinc.R

class ReelsFragment : Fragment() {

    private lateinit var loadedBody: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reels, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialize(view)
    }

    private fun initialize(view: View) {
        loadedBody = view.findViewById(R.id.loadedBody)
        
        // Show placeholder message
        val placeholderText = TextView(context)
        placeholderText.text = "Reels feature temporarily unavailable"
        placeholderText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        loadedBody.removeAllViews()
        loadedBody.addView(placeholderText)
        loadedBody.visibility = View.VISIBLE
    }
}
