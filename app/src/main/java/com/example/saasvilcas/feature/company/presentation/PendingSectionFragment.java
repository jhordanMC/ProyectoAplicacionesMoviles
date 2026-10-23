package com.example.saasvilcas.feature.company.presentation;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.saasvilcas.R;

public final class PendingSectionFragment extends Fragment {
    public PendingSectionFragment() { super(R.layout.company_fragment_pending); }
    public static PendingSectionFragment create(int title) {
        PendingSectionFragment fragment = new PendingSectionFragment();
        Bundle args = new Bundle(); args.putInt("title",title); fragment.setArguments(args);
        return fragment;
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        ((TextView)view.findViewById(R.id.pendingTitle)).setText(requireArguments().getInt("title"));
    }
}
