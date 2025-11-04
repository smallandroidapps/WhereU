package com.whereu.whereu.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.whereu.whereu.databinding.ItemRequestBinding;
import com.whereu.whereu.models.LocationRequest;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private final List<LocationRequest> requestList;
    private final OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onApproveClicked(LocationRequest request);
        void onRejectClicked(LocationRequest request);
        void onRequestAgainClicked(LocationRequest request);
    }

    public RequestAdapter(List<LocationRequest> requestList, OnRequestActionListener listener) {
        this.requestList = requestList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRequestBinding binding = ItemRequestBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new RequestViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        LocationRequest request = requestList.get(position);
        holder.bind(request);
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    class RequestViewHolder extends RecyclerView.ViewHolder {

        private final ItemRequestBinding binding;

        public RequestViewHolder(ItemRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(LocationRequest request) {
            binding.senderReceiverTextView.setText(request.getUserName());
            binding.timestampTextView.setText(request.getStatus());

            binding.approveButton.setOnClickListener(v -> listener.onApproveClicked(request));
            binding.rejectButton.setOnClickListener(v -> listener.onRejectClicked(request));
            binding.requestAgainButton.setOnClickListener(v -> listener.onRequestAgainClicked(request));
        }
    }
}
