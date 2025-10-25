package com.wheru.adapters;

import android.graphics.drawable.GradientDrawable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.whereu.whereu.R;
import com.whereu.whereu.models.LocationRequest;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private List<LocationRequest> requestList;
    private OnRequestActionListener listener;
    private FirebaseUser currentUser;

    public RequestAdapter(List<LocationRequest> requestList, OnRequestActionListener listener) {
        this.requestList = requestList;
        this.listener = listener;
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImageView;
        TextView senderReceiverTextView;
        TextView timestampTextView;
        // TextView statusTextView; // Removed as status_text_view is removed from layout
        MaterialButton viewDetailsButton;
        MaterialButton requestAgainButton;
        LinearLayout actionButtonsLayout;
        MaterialButton approveButton;
        MaterialButton rejectButton;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.avatar_image_view);
            senderReceiverTextView = itemView.findViewById(R.id.sender_receiver_text_view);
            timestampTextView = itemView.findViewById(R.id.timestamp_text_view);
            // statusTextView = itemView.findViewById(R.id.status_text_view); // Removed as status_text_view is removed from layout
            viewDetailsButton = itemView.findViewById(R.id.view_details_button);
            requestAgainButton = itemView.findViewById(R.id.request_again_button);
            actionButtonsLayout = itemView.findViewById(R.id.action_buttons_layout);
            approveButton = itemView.findViewById(R.id.approve_button);
            rejectButton = itemView.findViewById(R.id.reject_button);
        }
    }

    public interface OnRequestActionListener {
        void onApproveClicked(LocationRequest request);
        void onRejectClicked(LocationRequest request);
        void onRequestAgainClicked(LocationRequest request);
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
        if (currentUser == null) return;

        String otherUserName = request.getUserName() != null ? request.getUserName() : "Unknown";

        if (request.getFromUserId().equals(currentUser.getUid())) {
            holder.senderReceiverTextView.setText("To: " + otherUserName);
        } else {
            holder.senderReceiverTextView.setText("From: " + otherUserName);
        }

        holder.timestampTextView.setText(formatTimestamp(request.getTimestamp()));

        String status = request.getStatus();
        // holder.statusTextView.setText(status); // Removed as status_text_view is removed from layout
        int statusColor;
        switch (status) {
            case "pending":
                statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_pending);
                break;
            case "approved":
                statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_approved);
                break;
            case "rejected":
                statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_rejected);
                break;
            default:
                statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_default);
                break;
        }
        // ((GradientDrawable) holder.statusTextView.getBackground()).setColor(statusColor); // Removed as status_text_view is removed from layout // Removed as status_text_view is removed from layout

        // Control button visibility
        boolean isReceiver = request.getToUserId().equals(currentUser.getUid());

        if (isReceiver && "pending".equals(status)) {
            holder.actionButtonsLayout.setVisibility(View.VISIBLE);
            holder.viewDetailsButton.setVisibility(View.GONE);
            holder.requestAgainButton.setVisibility(View.GONE);
        } else if ("approved".equals(status)) {
            holder.actionButtonsLayout.setVisibility(View.GONE);
            holder.viewDetailsButton.setVisibility(View.VISIBLE);
            holder.requestAgainButton.setVisibility(View.VISIBLE);
        } else {
            holder.actionButtonsLayout.setVisibility(View.GONE);
            holder.viewDetailsButton.setVisibility(View.GONE);
            holder.requestAgainButton.setVisibility(View.VISIBLE);
        }

        holder.approveButton.setOnClickListener(v -> listener.onApproveClicked(request));
        holder.rejectButton.setOnClickListener(v -> listener.onRejectClicked(request));
        holder.requestAgainButton.setOnClickListener(v -> listener.onRequestAgainClicked(request));

        holder.viewDetailsButton.setOnClickListener(v -> {
            // Optional: Implement details view logic
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    private String formatTimestamp(long timestamp) {
        return DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString();
    }
}
