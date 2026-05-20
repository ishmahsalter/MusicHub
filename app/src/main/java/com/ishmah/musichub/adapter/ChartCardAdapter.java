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
import com.ishmah.musichub.MusicPlayerManager;
import com.ishmah.musichub.R;
import com.ishmah.musichub.activity.DetailActivity;
import com.ishmah.musichub.model.Track;
import java.util.List;

public class ChartCardAdapter extends RecyclerView.Adapter<ChartCardAdapter.VH> {

    private final Context context;
    private final List<Track> tracks;

    public ChartCardAdapter(Context context, List<Track> tracks) {
        this.context = context;
        this.tracks = tracks;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_chart_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Track track = tracks.get(position);
        holder.tvRank.setText("#" + (position + 1));
        holder.tvName.setText(track.getName());
        holder.tvArtist.setText(track.getArtist());

        String sc = track.getStreamCount();
        if (sc != null && !sc.isEmpty()) {
            holder.tvPlays.setText(formatCount(sc) + " plays");
        } else {
            holder.tvPlays.setText("");
        }

        String art = track.getAlbumArt();
        if (art != null && !art.isEmpty()) {
            Glide.with(context).load(art).centerCrop()
                    .placeholder(R.color.purple_deeper)
                    .into(holder.ivArt);
        } else {
            holder.ivArt.setBackgroundResource(R.color.purple_deeper);
        }

        holder.itemView.setOnClickListener(v -> {
            MusicPlayerManager.getInstance().setPlaylist(tracks, position);
            MusicPlayerManager.getInstance().playTrack(
                    track.getName(), track.getArtist(),
                    track.getAlbumArt() != null ? track.getAlbumArt() : "",
                    track.getTrackId(),
                    track.getPreviewUrl() != null ? track.getPreviewUrl() : "",
                    30);
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("trackName", track.getName());
            intent.putExtra("artistName", track.getArtist());
            intent.putExtra("trackId", track.getTrackId());
            intent.putExtra("albumArt", track.getAlbumArt() != null ? track.getAlbumArt() : "");
            intent.putExtra("previewUrl", track.getPreviewUrl() != null ? track.getPreviewUrl() : "");
            context.startActivity(intent);
        });
    }

    private String formatCount(String raw) {
        try {
            long n = Long.parseLong(raw);
            if (n >= 1_000_000L) return String.format("%.1fM", n / 1_000_000.0);
            if (n >= 1_000L) return String.format("%.1fK", n / 1_000.0);
            return raw;
        } catch (Exception e) { return raw; }
    }

    @Override
    public int getItemCount() { return tracks.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivArt;
        TextView tvRank, tvName, tvArtist, tvPlays;

        VH(@NonNull View itemView) {
            super(itemView);
            ivArt = itemView.findViewById(R.id.iv_chart_art);
            tvRank = itemView.findViewById(R.id.tv_chart_rank);
            tvName = itemView.findViewById(R.id.tv_chart_name);
            tvArtist = itemView.findViewById(R.id.tv_chart_artist);
            tvPlays = itemView.findViewById(R.id.tv_chart_plays);
        }
    }
}
