package com.whereu.whereu.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.whereu.whereu.R;

public class FullScreenImageDialogFragment extends DialogFragment {

    private static final String ARG_IMAGE_URL = "image_url";

    public static FullScreenImageDialogFragment newInstance(String imageUrl) {
        FullScreenImageDialogFragment fragment = new FullScreenImageDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_URL, imageUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_fullscreen_image, container, false);
        ImageView imageView = view.findViewById(R.id.image_fullscreen);

        String imageUrl = getArguments() != null ? getArguments().getString(ARG_IMAGE_URL) : null;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(imageView);
        }

        view.setOnClickListener(v -> dismiss());
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        return view;
    }
}
