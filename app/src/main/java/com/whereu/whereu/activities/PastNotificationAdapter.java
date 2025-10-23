package com.whereu.whereu.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.whereu.whereu.R;

import java.util.List;

public class PastNotificationAdapter extends RecyclerView.Adapter<PastNotificationAdapter.PastNotificationViewHolder> {

    private List<NotificationItem> notificationList;

    public PastNotificationAdapter(List<NotificationItem> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public PastNotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_past_notification, parent, false);
        return new PastNotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PastNotificationViewHolder holder, int position) {
        NotificationItem notificationItem = notificationList.get(position);
        holder.notificationText.setText(notificationItem.getMessage());

        // Set status icon based on status
        switch (notificationItem.getStatus()) {
            case "Approved":
                holder.statusIcon.setImageResource(R.drawable.ic_status_approved);
                break;
            case "Rejected":
                holder.statusIcon.setImageResource(R.drawable.ic_status_rejected);
                break;
            case "Pending":
            default:
                holder.statusIcon.setImageResource(R.drawable.ic_status_pending);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class PastNotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView statusIcon;
        TextView notificationText;

        PastNotificationViewHolder(View itemView) {
            super(itemView);
            statusIcon = itemView.findViewById(R.id.status_icon);
            notificationText = itemView.findViewById(R.id.notification_text);
        }
    }
}