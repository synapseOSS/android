package com.synapse.social.studioasinc.lab;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class FallbackFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        TextView tv = new TextView(getContext());
        tv.setText("Problem with onFragmentAdded or ViewPager\n\nThere seems to be an issue with the return logic in onFragmentAdded. Please verify that each case for the position has a corresponding return statement, and ensure there is a fallback return at the end of the method. Also double-check that setFragmentAdapter and TabCount in your ViewPager setup are correctly configured.");
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(14);
        tv.setBackgroundColor(0xFF000000);
        return tv;
    }
}