package com.wheru.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.whereu.whereu.models.LocationRequest;
import com.whereu.whereu.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private List<LocationRequest> requestList;
    private OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onRequestApproved(int position, LocationRequest request);
        void onRequestRejected(int position, LocationRequest request);
        void onRequestItemClicked(int position, LocationRequest request);
    }

    public RequestAdapter(List<LocationRequest> requestList, OnRequestActionListener listener) {
        this.requestList = requestList;
        this.listener = listener;
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
        holder.senderNameTextView.setText(request.getSenderId());
        holder.requestStatusTextView.setText(request.getStatus());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        if (request.getCreatedAt() != null) {
            holder.timestampTextView.setText(sdf.format(request.getCreatedAt()));
        }

        if ("pending".equals(request.getStatus())) {
            holder.approveButton.setVisibility(View.VISIBLE);
            holder.rejectButton.setVisibility(View.VISIBLE);
        } else {
            holder.approveButton.setVisibility(View.GONE);
            holder.rejectButton.setVisibility(View.GONE);
        }

        holder.approveButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRequestApproved(position, request);
            }
        });

        holder.rejectButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRequestRejected(position, request);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRequestItemClicked(position, request);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView senderNameTextView;
        TextView requestStatusTextView;
        TextView timestampTextView;
        Button approveButton;
        Button rejectButton;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            senderNameTextView = itemView.findViewById(R.id.text_sender_name);
            requestStatusTextView = itemView.findViewById(R.id.text_request_status);
            timestampTextView = itemView.findViewById(R.id.text_timestamp);
            approveButton = itemView.findViewById(R.id.button_approve);
            rejectButton = itemView.findViewById(R.id.button_reject);
        }
    }
}
