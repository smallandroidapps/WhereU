package com.whereu.whereu.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.whereu.whereu.R;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private List<Contact> contactList;
    private OnActionButtonClickListener listener;

    public ContactAdapter(List<Contact> contactList, OnActionButtonClickListener listener) {
        this.contactList = contactList;
        this.listener = listener;
    }

    public interface OnActionButtonClickListener {
        void onActionButtonClick(Contact contact, String action);
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contactList.get(position);
        holder.contactName.setText(contact.getName());

        // Handle button and status text based on contact status
        switch (contact.getStatus()) {
            case "not_requested":
                holder.requestStatusText.setVisibility(View.GONE);
                holder.actionButton.setVisibility(View.VISIBLE);
                holder.actionButton.setText("Request");
                holder.actionButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onActionButtonClick(contact, "request");
                    }
                });
                break;
            case "pending":
                holder.requestStatusText.setVisibility(View.VISIBLE);
                holder.requestStatusText.setText("Pending");
                holder.actionButton.setVisibility(View.GONE);
                break;
            case "approved":
                holder.requestStatusText.setVisibility(View.GONE);
                holder.actionButton.setVisibility(View.VISIBLE);
                holder.actionButton.setText("View Details");
                holder.actionButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onActionButtonClick(contact, "view_details");
                    }
                });
                break;
            case "denied":
                holder.requestStatusText.setVisibility(View.VISIBLE);
                holder.requestStatusText.setText("Denied");
                holder.actionButton.setVisibility(View.GONE);
                break;
            case "expired":
                holder.requestStatusText.setVisibility(View.VISIBLE);
                holder.requestStatusText.setText("Expired");
                holder.actionButton.setVisibility(View.VISIBLE);
                holder.actionButton.setText("Request Again");
                holder.actionButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onActionButtonClick(contact, "request_again");
                    }
                });
                break;
            default:
                holder.requestStatusText.setVisibility(View.GONE);
                holder.actionButton.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        CircleImageView contactAvatar;
        TextView contactName;
        TextView requestStatusText;
        Button actionButton;

        ContactViewHolder(View itemView) {
            super(itemView);
            contactAvatar = itemView.findViewById(R.id.contact_avatar);
            contactName = itemView.findViewById(R.id.contact_name);
            requestStatusText = itemView.findViewById(R.id.request_status_text);
            actionButton = itemView.findViewById(R.id.action_button);
        }
    }
}