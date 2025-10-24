package com.wheru.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.whereu.whereu.models.LocationRequest;
import com.whereu.whereu.R;

import android.text.format.DateUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private List<LocationRequest> requestList;
    private OnRequestActionListener listener;
    private String currentUserId;

    public interface OnRequestActionListener {
        void onRequestApproved(int position, LocationRequest request);
        void onRequestRejected(int position, LocationRequest request);
        void onRequestItemClicked(int position, LocationRequest request);
    }

    public RequestAdapter(List<LocationRequest> requestList, OnRequestActionListener listener) {
        this.requestList = requestList;
        this.listener = listener;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        LocationRequest request = requestList.get(position);
        boolean isSender = currentUserId.equals(request.getSenderId());

        // Set the name to the other user in the request
        // In a real app, you would fetch the user's name from the ID
        holder.senderReceiverTextView.setText(isSender ? "To: " + request.getReceiverName() : "From: " + request.getSenderName());

        // Set the status
        holder.statusTextView.setText(request.getStatus());

        // Use approvedAt for the timestamp if the request is approved, otherwise use createdAt
        Date timestampToShow = "approved".equals(request.getStatus()) ? request.getApprovedAt() : request.getCreatedAt();
        if (timestampToShow != null) {
            holder.timestampTextView.setText(getRelativeTimeSpanString(timestampToShow.getTime()));
        }

        // Handle status background color
        int statusColor;
        switch (request.getStatus()) {
            case "pending":
                statusColor = 0xFFFFA000; // Amber
                break;
            case "approved":
                statusColor = 0xFF388E3C; // Green
                break;
            case "rejected":
                statusColor = 0xFFD32F2F; // Red
                break;
            default:
                statusColor = 0xFF757575; // Grey
                break;
        }
        holder.statusTextView.getBackground().setColorFilter(statusColor, android.graphics.PorterDuff.Mode.SRC_IN);

        // Hide/Show View Details button based on status
        if ("approved".equals(request.getStatus())) {
            holder.viewDetailsButton.setVisibility(View.VISIBLE);
        } else {
            holder.viewDetailsButton.setVisibility(View.GONE);
        }

        holder.viewDetailsButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRequestItemClicked(position, request);
            }
        });

        // Remove old approve/reject button logic
        // The new design uses a bottom sheet for actions
    }

    private CharSequence getRelativeTimeSpanString(long time) {
        long now = System.currentTimeMillis();
        return DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS);
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImageView;
        TextView senderReceiverTextView;
        TextView timestampTextView;
        TextView statusTextView;
        Button viewDetailsButton;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.avatar_image_view);
            senderReceiverTextView = itemView.findViewById(R.id.sender_receiver_text_view);
            timestampTextView = itemView.findViewById(R.id.timestamp_text_view);
            statusTextView = itemView.findViewById(R.id.status_text_view);
            viewDetailsButton = itemView.findViewById(R.id.view_details_button);
        }
    }
}