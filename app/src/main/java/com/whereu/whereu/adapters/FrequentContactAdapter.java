package com.whereu.whereu.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.whereu.whereu.R;
import com.whereu.whereu.activities.SearchResultAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class FrequentContactAdapter extends RecyclerView.Adapter<FrequentContactAdapter.FrequentContactViewHolder> {

    private List<SearchResultAdapter.SearchResult> frequentContacts;
    private OnFrequentContactClickListener listener;

    public interface OnFrequentContactClickListener {
        void onFrequentContactClick(SearchResultAdapter.SearchResult contact);
        void onClearFrequentContact(SearchResultAdapter.SearchResult contact);
    }

    public FrequentContactAdapter(List<SearchResultAdapter.SearchResult> frequentContacts, OnFrequentContactClickListener listener) {
        this.frequentContacts = frequentContacts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FrequentContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_frequent_contact, parent, false);
        return new FrequentContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FrequentContactViewHolder holder, int position) {
        SearchResultAdapter.SearchResult contact = frequentContacts.get(position);
        holder.contactName.setText(contact.getDisplayName());

        String status = contact.getRequestStatus();
        if (status == null) status = "not_requested";
        switch (status) {
            case "pending":
                holder.statusText.setText("Request sent");
                holder.actionButton.setText("Request sent");
                holder.actionButton.setEnabled(false);
                holder.actionButton.setVisibility(View.VISIBLE);
                break;
            case "approved":
                holder.statusText.setText("Approved");
                holder.actionButton.setText("View details");
                holder.actionButton.setEnabled(true);
                holder.actionButton.setVisibility(View.VISIBLE);
                break;
            case "expired":
                holder.statusText.setText("Expired");
                holder.actionButton.setText("Request again");
                holder.actionButton.setEnabled(true);
                holder.actionButton.setVisibility(View.VISIBLE);
                break;
            case "rejected":
                holder.statusText.setText("Rejected");
                holder.actionButton.setText("Request again");
                holder.actionButton.setEnabled(true);
                holder.actionButton.setVisibility(View.VISIBLE);
                break;
            default:
                holder.statusText.setText("Not requested");
                holder.actionButton.setText("Request location");
                holder.actionButton.setEnabled(true);
                holder.actionButton.setVisibility(View.VISIBLE);
                break;
        }

        long ts = contact.getLastRequestTimestamp();
        if (ts > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            String time = sdf.format(new Date(ts));
            holder.statusText.setText(holder.statusText.getText() + " • " + time);
        }

        holder.actionButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFrequentContactClick(contact);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFrequentContactClick(contact);
            }
        });

        View clearButton = holder.itemView.findViewById(R.id.clear_button);
        if (clearButton != null) {
            clearButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClearFrequentContact(contact);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return frequentContacts.size();
    }

    public void updateData(List<SearchResultAdapter.SearchResult> newFrequentContacts) {
        this.frequentContacts.clear();
        this.frequentContacts.addAll(newFrequentContacts);
        notifyDataSetChanged();
    }

    static class FrequentContactViewHolder extends RecyclerView.ViewHolder {
        CircleImageView contactAvatar;
        TextView contactName;
        TextView statusText;
        TextView actionButton;

        FrequentContactViewHolder(@NonNull View itemView) {
            super(itemView);
            contactAvatar = itemView.findViewById(R.id.contact_avatar);
            contactName = itemView.findViewById(R.id.contact_name);
            statusText = itemView.findViewById(R.id.status_text);
            actionButton = itemView.findViewById(R.id.frequent_action_button);
        }
    }
}
