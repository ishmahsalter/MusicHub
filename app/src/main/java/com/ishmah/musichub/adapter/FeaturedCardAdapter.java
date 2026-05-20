package com.ishmah.musichub.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.ishmah.musichub.MusicPlayerManager;
import com.ishmah.musichub.R;
import com.ishmah.musichub.activity.DetailActivity;
import com.ishmah.musichub.model.FeaturedCard;
import java.util.List;

public class FeaturedCardAdapter extends
        RecyclerView.Adapter<FeaturedCardAdapter.CardViewHolder> {

    private Context context;
    private List<FeaturedCard> cards;

    public FeaturedCardAdapter(Context context, List<FeaturedCard> cards) {
        this.context = context;
        this.cards = cards;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                             int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_featured_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder,
                                 int position) {
        FeaturedCard card = cards.get(position);

        holder.tvBadge.setText(card.getBadge());
        holder.tvTitle.setText(card.getTitle());
        holder.tvSubtitle.setText(card.getSubtitle());
        holder.tvMeta.setText(card.getMeta());

        // Load art
        if (card.getArtUrl() != null && !card.getArtUrl().isEmpty()) {
            Glide.with(context)
                    .load(card.getArtUrl())
                    .apply(RequestOptions.bitmapTransform(
                            new RoundedCorners(24)))
                    .placeholder(R.color.purple_deeper)
                    .into(holder.ivArt);
        }

        // Play button click
        holder.btnPlay.setOnClickListener(v -> {
            if (card.getPreviewUrl() != null &&
                    !card.getPreviewUrl().isEmpty()) {
                MusicPlayerManager.getInstance().playTrack(
                        card.getTitle(),
                        card.getSubtitle(),
                        card.getArtUrl() != null ?
                                card.getArtUrl() : "",
                        card.getTrackId(),
                        card.getPreviewUrl(),
                        30);
            }
            Intent intent = new Intent(context, DetailActivity.class);
            // FIX: tambah FLAG_ACTIVITY_NEW_TASK
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("trackName", card.getTitle());
            intent.putExtra("artistName", card.getSubtitle());
            intent.putExtra("trackId", card.getTrackId());
            intent.putExtra("albumArt",
                    card.getArtUrl() != null ? card.getArtUrl() : "");
            intent.putExtra("previewUrl",
                    card.getPreviewUrl() != null ?
                            card.getPreviewUrl() : "");
            context.startActivity(intent);
        });

        // Card click
        holder.itemView.setOnClickListener(v ->
                holder.btnPlay.performClick());
    }

    public void updateCards(List<FeaturedCard> newCards) {
        this.cards = newCards;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() { return cards.size(); }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView tvBadge, tvTitle, tvSubtitle, tvMeta;
        ImageView ivArt, btnPlay;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBadge = itemView.findViewById(R.id.tv_card_badge);
            tvTitle = itemView.findViewById(R.id.tv_card_title);
            tvSubtitle = itemView.findViewById(R.id.tv_card_subtitle);
            tvMeta = itemView.findViewById(R.id.tv_card_meta);
            ivArt = itemView.findViewById(R.id.iv_card_art);
            btnPlay = itemView.findViewById(R.id.btn_card_play);
        }
    }
}