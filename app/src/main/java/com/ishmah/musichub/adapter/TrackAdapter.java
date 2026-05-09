package com.ishmah.musichub.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.ishmah.musichub.R;
import com.ishmah.musichub.activity.DetailActivity;
import com.ishmah.musichub.db.FavoriteDao;
import com.ishmah.musichub.fragment.AddToPlaylistDialog;
import com.ishmah.musichub.model.Track;
import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    private Context context;
    private List<Track> trackList;
    private FavoriteDao favoriteDao;

    public TrackAdapter(Context context, List<Track> trackList) {
        this.context = context;
        this.trackList = trackList;
        this.favoriteDao = new FavoriteDao(context);
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        Track track = trackList.get(position);

        holder.tvTrackName.setText(track.getName());
        holder.tvArtistName.setText(track.getArtist());
        holder.tvDuration.setText(track.getDuration());
        holder.tvNumber.setText(String.valueOf(position + 1));

        // Load album art pakai Glide
        if (track.getAlbumArt() != null && !track.getAlbumArt().isEmpty()) {
            Glide.with(context)
                    .load(track.getAlbumArt())
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(16)))
                    .placeholder(R.color.bg_card)
                    .error(R.color.bg_card)
                    .into(holder.ivAlbumArt);
        } else {
            holder.ivAlbumArt.setImageResource(R.color.bg_card);
        }

        // Cek status like
        boolean isLiked = favoriteDao.isFavorite(track.getTrackId());
        updateLikeIcon(holder.ivLike, isLiked);

        // Like button click
        holder.ivLike.setOnClickListener(v -> {
            boolean currentlyLiked = favoriteDao.isFavorite(track.getTrackId());
            if (currentlyLiked) {
                favoriteDao.removeFavorite(track.getTrackId(), null);
                updateLikeIcon(holder.ivLike, false);
            } else {
                favoriteDao.addFavorite(
                        track.getTrackId(),
                        track.getName(),
                        track.getArtist(),
                        track.getAlbumArt(),
                        track.getDuration(),
                        null
                );
                updateLikeIcon(holder.ivLike, true);
            }
        });

        // Click → DetailActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("trackName", track.getName());
            intent.putExtra("artistName", track.getArtist());
            intent.putExtra("trackId", track.getTrackId());
            intent.putExtra("albumArt", track.getAlbumArt());
            context.startActivity(intent);
        });

        // Long click → Add to Playlist
        holder.itemView.setOnLongClickListener(v -> {
            AddToPlaylistDialog dialog = AddToPlaylistDialog.newInstance(
                    track.getTrackId(),
                    track.getName(),
                    track.getArtist()
            );
            if (context instanceof FragmentActivity) {
                dialog.show(
                        ((FragmentActivity) context).getSupportFragmentManager(),
                        "AddToPlaylist"
                );
            }
            return true;
        });
    }

    private void updateLikeIcon(ImageView ivLike, boolean isLiked) {
        if (isLiked) {
            ivLike.setImageResource(R.drawable.ic_favorite);
            ivLike.setColorFilter(context.getResources().getColor(R.color.gold_primary));
            ivLike.setAlpha(1f);
        } else {
            ivLike.setImageResource(R.drawable.ic_favorite);
            ivLike.setColorFilter(context.getResources().getColor(R.color.text_muted));
            ivLike.setAlpha(0.4f);
        }
    }

    @Override
    public int getItemCount() {
        return trackList.size();
    }

    public void updateList(List<Track> newList) {
        this.trackList = newList;
        notifyDataSetChanged();
    }

    public static class TrackViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAlbumArt, ivLike;
        TextView tvTrackName, tvArtistName, tvDuration, tvNumber;

        public TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAlbumArt = itemView.findViewById(R.id.iv_album_art);
            ivLike = itemView.findViewById(R.id.iv_like);
            tvTrackName = itemView.findViewById(R.id.tv_track_name);
            tvArtistName = itemView.findViewById(R.id.tv_artist_name);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvNumber = itemView.findViewById(R.id.tv_number);
        }
    }
}