package com.whereu.whereu.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.whereu.whereu.databinding.ItemRequestBinding;
import com.whereu.whereu.models.LocationRequest;
import com.wheru.models.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private final List<LocationRequest> requestList;
    private final OnRequestActionListener listener;
    private final Context context;

    public interface OnRequestActionListener {
        void onApproveClicked(LocationRequest request);
        void onRejectClicked(LocationRequest request);
        void onRequestAgainClicked(LocationRequest request);
        void onViewLocationClicked(LocationRequest request);
    }

    public RequestAdapter(Context context, List<LocationRequest> requestList, OnRequestActionListener listener) {
        this.context = context;
        this.requestList = requestList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRequestBinding binding = ItemRequestBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new RequestViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        LocationRequest request = requestList.get(position);
        holder.bind(request);
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    class RequestViewHolder extends RecyclerView.ViewHolder {

        private final ItemRequestBinding binding;

        public RequestViewHolder(ItemRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(LocationRequest request) {
            String userId = request.isSentByCurrentUser() ? request.getToUserId() : request.getFromUserId();

            FirebaseFirestore.getInstance().collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        binding.senderReceiverTextView.setText(user.getDisplayName());
                        // Hydrate request with userName for downstream UI (details sheet)
                        request.setUserName(user.getDisplayName());
                        if (user.getProfilePhotoUrl() != null && !user.getProfilePhotoUrl().isEmpty()) {
                            Glide.with(context)
                                    .load(user.getProfilePhotoUrl())
                                    .into(binding.avatarImageView);
                        }
                    }
                }
            });

            // Format and display the date and time
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            String formattedDate = sdf.format(new Date(request.getTimestamp()));
            binding.timestampTextView.setText(formattedDate);

            if (request.isSentByCurrentUser()) {
                binding.actionButtonsLayout.setVisibility(View.GONE);
                if ("approved".equals(request.getStatus())) {
                    binding.viewDetailsButton.setVisibility(View.VISIBLE);
                    binding.requestAgainButton.setVisibility(View.GONE);
                } else {
                    binding.viewDetailsButton.setVisibility(View.GONE);
                    binding.requestAgainButton.setVisibility(View.VISIBLE);
                }
            } else {
                binding.actionButtonsLayout.setVisibility(View.VISIBLE);
                binding.viewDetailsButton.setVisibility(View.GONE);
                binding.requestAgainButton.setVisibility(View.GONE);
            }

            binding.approveButton.setOnClickListener(v -> listener.onApproveClicked(request));
            binding.rejectButton.setOnClickListener(v -> listener.onRejectClicked(request));
            binding.requestAgainButton.setOnClickListener(v -> listener.onRequestAgainClicked(request));
            binding.viewDetailsButton.setOnClickListener(v -> listener.onViewLocationClicked(request));
        }
    }
}
