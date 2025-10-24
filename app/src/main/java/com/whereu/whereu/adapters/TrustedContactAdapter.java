package com.whereu.whereu.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.whereu.whereu.R;
import com.whereu.whereu.models.TrustedContact;

import java.util.List;

public class TrustedContactAdapter extends RecyclerView.Adapter<TrustedContactAdapter.TrustedContactViewHolder> {

    private List<TrustedContact> trustedContactList;

    public TrustedContactAdapter(List<TrustedContact> trustedContactList) {
        this.trustedContactList = trustedContactList;
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
    }

    @Override
    public int getItemCount() {
        return trustedContactList.size();
    }

    static class TrustedContactViewHolder extends RecyclerView.ViewHolder {
        TextView displayNameTextView;
        TextView phoneNumberTextView;

        TrustedContactViewHolder(@NonNull View itemView) {
            super(itemView);
            displayNameTextView = itemView.findViewById(R.id.text_view_display_name);
            phoneNumberTextView = itemView.findViewById(R.id.text_view_phone_number);
        }
    }
}