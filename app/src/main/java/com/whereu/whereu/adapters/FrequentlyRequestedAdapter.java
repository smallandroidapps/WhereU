package com.whereu.whereu.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.whereu.whereu.R;
import com.whereu.whereu.models.LocationRequest;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.wheru.models.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class FrequentlyRequestedAdapter extends RecyclerView.Adapter<FrequentlyRequestedAdapter.ViewHolder> {

    private List<LocationRequest> frequentlyRequestedList;
    private OnRequestAgainListener listener;

    public FrequentlyRequestedAdapter(List<LocationRequest> frequentlyRequestedList, OnRequestAgainListener listener) {
        this.frequentlyRequestedList = frequentlyRequestedList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_frequently_requested_location, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocationRequest request = frequentlyRequestedList.get(position);

        holder.userName.setText(request.getUserName());

        // Load avatar for the other user in the request
        String otherUserId = request.isSentByCurrentUser() ? request.getToUserId() : request.getFromUserId();
        if (otherUserId != null && !otherUserId.isEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(otherUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                // Update display name if not set
                                if (request.getUserName() == null || request.getUserName().isEmpty()) {
                                    holder.userName.setText(user.getDisplayName());
                                }
                                Glide.with(holder.itemView)
                                        .load(user.getProfilePhotoUrl())
                                        .placeholder(R.drawable.ic_profile_placeholder)
                                        .error(R.drawable.ic_profile_placeholder)
                                        .into(holder.avatar);
                            }
                        }
                    });
        }

        // Set timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        holder.timestamp.setText(sdf.format(new Date(request.getTimestamp())));

        // Set area name and coordinates
        if (request.getAreaName() != null && !request.getAreaName().isEmpty()) {
            holder.areaName.setText(request.getAreaName());
        } else {
            holder.areaName.setText("Unknown Area");
        }
        holder.coordinates.setText(String.format(Locale.getDefault(), "%.4f, %.4f", request.getLatitude(), request.getLongitude()));

        // Calculate and set distance (if needed, currently not in model)
        // holder.distance.setText("X km away"); // Placeholder

        // Handle status chip
        long twentyFourHoursInMillis = TimeUnit.HOURS.toMillis(24);
        boolean isExpired = (System.currentTimeMillis() - request.getApprovedTimestamp()) > twentyFourHoursInMillis;

        if (request.getStatus().equals("approved") && !isExpired) {
            holder.statusChip.setText("Approved");
            holder.statusChip.setBackgroundResource(R.drawable.chip_background_approved);
            holder.statusChip.setVisibility(View.VISIBLE);
            holder.requestAgainButton.setVisibility(View.GONE);
        } else if (request.getStatus().equals("approved") && isExpired) {
            holder.statusChip.setText("Expired");
            holder.statusChip.setBackgroundResource(R.drawable.chip_background_expired);
            holder.statusChip.setVisibility(View.VISIBLE);
            holder.requestAgainButton.setVisibility(View.VISIBLE);
            holder.requestAgainButton.setText("Request Again");
        } else {
            holder.statusChip.setVisibility(View.GONE);
            holder.requestAgainButton.setVisibility(View.GONE);
        }

        holder.requestAgainButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRequestAgain(request);
            }
        });
    }

    @Override
    public int getItemCount() {
        return frequentlyRequestedList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView userName;
        TextView timestamp;
        TextView statusChip;
        TextView areaName;
        TextView coordinates;
        TextView distance;
        Button requestAgainButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.image_view_avatar);
            userName = itemView.findViewById(R.id.text_view_user_name);
            timestamp = itemView.findViewById(R.id.text_view_timestamp);
            statusChip = itemView.findViewById(R.id.text_view_status_chip);
            areaName = itemView.findViewById(R.id.text_view_area_name);
            coordinates = itemView.findViewById(R.id.text_view_coordinates);
            distance = itemView.findViewById(R.id.text_view_distance);
            requestAgainButton = itemView.findViewById(R.id.button_request_again);
        }
    }

    public interface OnRequestAgainListener {
        void onRequestAgain(LocationRequest request);
    }
}
