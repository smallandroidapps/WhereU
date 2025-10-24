package com.whereu.whereu.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.whereu.whereu.R;
import com.whereu.whereu.models.TrustedContact;

import java.util.List;

public class TrustedContactAdapter extends RecyclerView.Adapter<TrustedContactAdapter.TrustedContactViewHolder> {

    private List<TrustedContact> trustedContactList;
    private OnActionButtonClickListener listener;

    public interface OnActionButtonClickListener {
        void onActionButtonClick(TrustedContact contact, String action);
    }

    public TrustedContactAdapter(List<TrustedContact> trustedContactList, OnActionButtonClickListener listener) {
        this.trustedContactList = trustedContactList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TrustedContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trusted_contact, parent, false);
        return new TrustedContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrustedContactViewHolder holder, int position) {
        TrustedContact contact = trustedContactList.get(position);
        holder.displayNameTextView.setText(contact.getDisplayName());
        holder.phoneNumberTextView.setText(contact.getPhoneNumber());
        holder.statusTextView.setText("Status: " + contact.getStatus());

        // Set status icon based on status
        holder.statusIconImageView.setVisibility(View.VISIBLE);
        switch (contact.getStatus()) {
            case "pending":
                holder.statusIconImageView.setImageResource(R.drawable.ic_status_pending); // Assuming you have this drawable
                break;
            case "approved":
                holder.statusIconImageView.setImageResource(R.drawable.ic_status_approved); // Assuming you have this drawable
                break;
            case "denied":
                holder.statusIconImageView.setImageResource(R.drawable.ic_status_denied); // Assuming you have this drawable
                break;
            case "expired":
                holder.statusIconImageView.setImageResource(R.drawable.ic_status_expired); // Assuming you have this drawable
                break;
            default:
                holder.statusIconImageView.setVisibility(View.GONE);
                break;
        }

        // Reset button visibility
        holder.requestButton.setVisibility(View.GONE);
        holder.viewDetailsButton.setVisibility(View.GONE);
        holder.approveButton.setVisibility(View.GONE);
        holder.denyButton.setVisibility(View.GONE);
        holder.requestAgainButton.setVisibility(View.GONE);

        // Set button visibility based on status
        switch (contact.getStatus()) {
            case "not_requested":
                holder.requestButton.setVisibility(View.VISIBLE);
                break;
            case "pending":
                holder.viewDetailsButton.setVisibility(View.VISIBLE);
                holder.approveButton.setVisibility(View.VISIBLE);
                holder.denyButton.setVisibility(View.VISIBLE);
                break;
            case "approved":
                holder.viewDetailsButton.setVisibility(View.VISIBLE);
                holder.requestAgainButton.setVisibility(View.VISIBLE);
                break;
            case "denied":
                holder.requestAgainButton.setVisibility(View.VISIBLE);
                break;
            default:
                // Handle unknown status or hide all buttons
                break;
        }

        holder.requestButton.setOnClickListener(v -> listener.onActionButtonClick(contact, "Request"));
        holder.viewDetailsButton.setOnClickListener(v -> listener.onActionButtonClick(contact, "View Details"));
        holder.approveButton.setOnClickListener(v -> listener.onActionButtonClick(contact, "Approve"));
        holder.denyButton.setOnClickListener(v -> listener.onActionButtonClick(contact, "Deny"));
        holder.requestAgainButton.setOnClickListener(v -> listener.onActionButtonClick(contact, "Request Again"));
    }

    @Override
    public int getItemCount() {
        return trustedContactList.size();
    }

    static class TrustedContactViewHolder extends RecyclerView.ViewHolder {
        TextView displayNameTextView;
        TextView phoneNumberTextView;
        TextView statusTextView;
        Button requestButton;
        Button viewDetailsButton;
        Button approveButton;
        Button denyButton;
        Button requestAgainButton;
        ImageView statusIconImageView;

        TrustedContactViewHolder(@NonNull View itemView) {
            super(itemView);
            displayNameTextView = itemView.findViewById(R.id.text_view_display_name);
            phoneNumberTextView = itemView.findViewById(R.id.text_view_phone_number);
            statusTextView = itemView.findViewById(R.id.text_view_status);
            requestButton = itemView.findViewById(R.id.button_request);
            viewDetailsButton = itemView.findViewById(R.id.button_view_details);
            approveButton = itemView.findViewById(R.id.button_approve);
            denyButton = itemView.findViewById(R.id.button_deny);
            requestAgainButton = itemView.findViewById(R.id.button_request_again);
            statusIconImageView = itemView.findViewById(R.id.image_view_status_icon);
        }
    }
}