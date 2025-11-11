package com.whereu.whereu.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.whereu.whereu.R;
import com.whereu.whereu.models.SavedCard;

import java.util.List;

public class SavedCardsAdapter extends RecyclerView.Adapter<SavedCardsAdapter.ViewHolder> {

    public interface OnSavedCardSelectedListener {
        void onSavedCardSelected(SavedCard card);
    }

    private final List<SavedCard> cards;
    private final OnSavedCardSelectedListener listener;

    public SavedCardsAdapter(List<SavedCard> cards, OnSavedCardSelectedListener listener) {
        this.cards = cards;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedCard card = cards.get(position);
        holder.title.setText(card.getBrand() + " • " + card.getMasked());
        holder.subtitle.setText("Exp " + safe(card.getExpMonth()) + "/" + safe(card.getExpYear()));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSavedCardSelected(card);
        });
    }

    @Override
    public int getItemCount() {
        return cards != null ? cards.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView subtitle;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.card_title);
            subtitle = itemView.findViewById(R.id.card_subtitle);
        }
    }

    private String safe(Integer v) {
        return v == null ? "--" : String.valueOf(v);
    }
}

