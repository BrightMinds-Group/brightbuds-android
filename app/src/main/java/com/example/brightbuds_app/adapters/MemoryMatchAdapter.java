package com.example.brightbuds_app.adapters;

import android.media.SoundPool;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brightbuds_app.R;
import com.example.brightbuds_app.models.MemoryCard;

import java.util.List;

public class MemoryMatchAdapter extends RecyclerView.Adapter<MemoryMatchAdapter.CardViewHolder> {

    public interface OnCardClickListener {
        void onCardClick(int position);
    }

    private final List<MemoryCard> cards;
    private final OnCardClickListener listener;

    private final SoundPool soundPool;
    private final int flipSoundId;

    public MemoryMatchAdapter(List<MemoryCard> cards,
                              OnCardClickListener listener,
                              SoundPool soundPool,
                              int flipSoundId) {
        this.cards = cards;
        this.listener = listener;
        this.soundPool = soundPool;
        this.flipSoundId = flipSoundId;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_memory_card, parent, false);

        int spanCount = 2;
        int size = parent.getMeasuredWidth() / spanCount - 24;
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            params.width = size;
            params.height = size;
            view.setLayoutParams(params);
        }

        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        MemoryCard card = cards.get(position);

        if (card.isFlipped() || card.isMatched()) {
            holder.imageViewCard.setImageResource(card.getImageResId());
        } else {
            holder.imageViewCard.setImageResource(R.drawable.memory_card);
        }

        holder.itemView.setOnClickListener(v -> {
            if (soundPool != null) {
                soundPool.play(flipSoundId, 1f, 1f, 1, 0, 1f);
            }
            if (listener != null) {
                listener.onCardClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewCard;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewCard = itemView.findViewById(R.id.imageViewCard);
        }
    }
}
