package com.synapse.social.studioasinc.lab

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class FallbackFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val tv = TextView(context)
        tv.text = "Problem with onFragmentAdded or ViewPager\n\nThere seems to be an issue with the return logic in onFragmentAdded. Please verify that each case for the position has a corresponding return statement, and ensure there is a fallback return at the end of the method. Also double-check that setFragmentAdapter and TabCount in your ViewPager setup are correctly configured."
        tv.setTextColor(Color.WHITE)
        tv.gravity = Gravity.CENTER
        tv.textSize = 14f
        tv.setBackgroundColor(-0x1000000)
        return tv
    }
}
