package com.example.pictobeads;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Fragment containing the main two-button menu (Bracelet and Picture).
 */
public class StartFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_start, container, false);

        view.findViewById(R.id.btn_bracelet).setOnClickListener(v -> {
            startActivity(new Intent(getContext(), BraceletActivity.class));
        });

        view.findViewById(R.id.btn_picture).setOnClickListener(v -> {
            startActivity(new Intent(getContext(), PictureActivity.class));
        });

        return view;
    }
}
