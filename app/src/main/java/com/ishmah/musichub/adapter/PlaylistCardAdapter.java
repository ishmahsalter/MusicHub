package com.ishmah.musichub.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.ishmah.musichub.R;
import java.util.List;
import java.util.Map;

public class PlaylistCardAdapter extends RecyclerView.Adapter<PlaylistCardAdapter.VH> {

    public interface OnPlaylistClickListener {
        void onPlaylistClick(int playlistId, String name);
    }

    private final Context context;
    private final List<Map<String, String>> playlists;
    private final OnPlaylistClickListener listener;

    public PlaylistCardAdapter(Context context, List<Map<String, String>> playlists,
                               OnPlaylistClickListener listener) {
        this.context = context;
        this.playlists = playlists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_playlist_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Map<String, String> p = playlists.get(position);
        String name = p.get("name");
        String coverType = p.get("cover_type");
        String coverValue = p.get("cover_value");
        String trackCountStr = p.get("track_count");
        int trackCount = 0;
        try { trackCount = Integer.parseInt(trackCountStr); } catch (Exception ignored) {}
        int playlistId = 0;
        try { playlistId = Integer.parseInt(p.get("playlist_id")); } catch (Exception ignored) {}

        holder.tvName.setText(name != null ? name : "Playlist");
        holder.tvCount.setText(trackCount + (trackCount == 1 ? " track" : " tracks"));

        if ("image".equals(coverType) && coverValue != null && !coverValue.isEmpty()) {
            holder.ivCover.setBackground(null);
            Glide.with(context).load(coverValue).centerCrop()
                    .placeholder(R.drawable.bg_playlist_purple)
                    .into(holder.ivCover);
        } else {
            holder.ivCover.setImageResource(0);
            holder.ivCover.setBackgroundResource(getGradientRes(coverValue));
        }

        int finalPlaylistId = playlistId;
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPlaylistClick(finalPlaylistId, name != null ? name : "Playlist");
        });
    }

    private int getGradientRes(String colorName) {
        if (colorName == null) return R.drawable.bg_playlist_purple;
        switch (colorName) {
            case "gold":  return R.drawable.bg_playlist_gold;
            case "teal":  return R.drawable.bg_playlist_teal;
            case "pink":  return R.drawable.bg_playlist_pink;
            case "blue":  return R.drawable.bg_playlist_blue;
            case "green": return R.drawable.bg_playlist_green;
            default:      return R.drawable.bg_playlist_purple;
        }
    }

    @Override
    public int getItemCount() { return playlists.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvName, tvCount;

        VH(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvName  = itemView.findViewById(R.id.tv_playlist_name);
            tvCount = itemView.findViewById(R.id.tv_track_count);
        }
    }
}
