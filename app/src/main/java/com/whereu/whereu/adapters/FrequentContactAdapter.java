package com.whereu.whereu.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.whereu.whereu.R;
import com.whereu.whereu.activities.SearchResultAdapter;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class FrequentContactAdapter extends RecyclerView.Adapter<FrequentContactAdapter.FrequentContactViewHolder> {

    private List<SearchResultAdapter.SearchResult> frequentContacts;
    private OnFrequentContactClickListener listener;

    public interface OnFrequentContactClickListener {
        void onFrequentContactClick(SearchResultAdapter.SearchResult contact);
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
        // Set avatar if available
        // if (contact.getProfilePhotoUrl() != null && !contact.getProfilePhotoUrl().isEmpty()) {
        //     Glide.with(holder.itemView.getContext()).load(contact.getProfilePhotoUrl()).into(holder.contactAvatar);
        // } else {
        //     holder.contactAvatar.setImageResource(R.drawable.ic_profile_placeholder);
        // }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFrequentContactClick(contact);
            }
        });
    }

    @Override
    public int getItemCount() {
        return frequentContacts.size();
    }

    static class FrequentContactViewHolder extends RecyclerView.ViewHolder {
        CircleImageView contactAvatar;
        TextView contactName;

        FrequentContactViewHolder(@NonNull View itemView) {
            super(itemView);
            contactAvatar = itemView.findViewById(R.id.contact_avatar);
            contactName = itemView.findViewById(R.id.contact_name);
        }
    }
}