package com.whereu.whereu.adapters;

import android.content.Context;
import android.os.CountDownTimer;
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

            // Format and display timestamp (relative if < 24h)
            long refTs = request.getApprovedTimestamp() > 0 ? request.getApprovedTimestamp() : request.getTimestamp();
            String label = request.getApprovedTimestamp() > 0 ? "Shared At: " : "Requested At: ";
            binding.timestampTextView.setText(label + formatRelativeOrAbsolute(refTs));

            if (request.isSentByCurrentUser()) {
                binding.actionButtonsLayout.setVisibility(View.GONE);
                if ("approved".equals(request.getStatus())) {
                    binding.viewDetailsButton.setVisibility(View.VISIBLE);
                    binding.requestAgainButton.setVisibility(View.GONE);
                } else {
                    binding.viewDetailsButton.setVisibility(View.GONE);
                    binding.requestAgainButton.setVisibility(View.VISIBLE);

                    // Cooldown: prevent re-request within 60 seconds of last request timestamp
                    long cooldownMs = 60_000L;
                    long lastReqTs = request.getTimestamp();
                    long now = System.currentTimeMillis();
                    long remaining = (lastReqTs + cooldownMs) - now;

                    // Cancel any existing countdown associated with this button to avoid leaks
                    Object tag = binding.requestAgainButton.getTag();
                    if (tag instanceof CountDownTimer) {
                        ((CountDownTimer) tag).cancel();
                        binding.requestAgainButton.setTag(null);
                    }

                    if (remaining > 0) {
                        binding.requestAgainButton.setEnabled(false);
                        binding.requestAgainButton.setAlpha(0.6f);

                        CountDownTimer timer = new CountDownTimer(remaining, 1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                long seconds = (millisUntilFinished + 999) / 1000; // ceil
                                long mins = seconds / 60;
                                long secs = seconds % 60;
                                String mmss = String.format(Locale.getDefault(), "%d:%02d", mins, secs);
                                binding.requestAgainButton.setText("Request Again (" + mmss + ")");
                            }

                            @Override
                            public void onFinish() {
                                binding.requestAgainButton.setText("Request Again");
                                binding.requestAgainButton.setEnabled(true);
                                binding.requestAgainButton.setAlpha(1.0f);
                                binding.requestAgainButton.setTag(null);
                            }
                        };
                        binding.requestAgainButton.setTag(timer);
                        timer.start();
                    } else {
                        binding.requestAgainButton.setText("Request Again");
                        binding.requestAgainButton.setEnabled(true);
                        binding.requestAgainButton.setAlpha(1.0f);
                    }
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

        private String formatRelativeOrAbsolute(long ts) {
            if (ts <= 0) return "N/A";
            long now = System.currentTimeMillis();
            long diff = Math.abs(now - ts);

            long oneDayMs = 24L * 60L * 60L * 1000L;
            if (diff < oneDayMs) {
                long minutes = diff / (60L * 1000L);
                if (minutes < 60) {
                    if (minutes <= 0) return "just now";
                    return minutes + " min ago";
                } else {
                    long hours = minutes / 60L;
                    return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
                }
            }
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            return sdf.format(new Date(ts));
        }
    }
}
