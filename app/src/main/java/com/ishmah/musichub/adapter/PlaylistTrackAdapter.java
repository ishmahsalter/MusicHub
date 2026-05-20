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

public class PlaylistTrackAdapter extends RecyclerView.Adapter<PlaylistTrackAdapter.VH> {

    public interface OnTrackActionListener {
        void onTrackClick(int position, Map<String, String> track);
        void onRemoveClick(int position, Map<String, String> track);
    }

    private final Context context;
    private final List<Map<String, String>> tracks;
    private final OnTrackActionListener listener;

    public PlaylistTrackAdapter(Context context, List<Map<String, String>> tracks,
                                OnTrackActionListener listener) {
        this.context = context;
        this.tracks = tracks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_playlist_track, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Map<String, String> track = tracks.get(position);
        holder.tvNum.setText(String.valueOf(position + 1));
        holder.tvName.setText(track.get("name") != null ? track.get("name") : "");
        holder.tvArtist.setText(track.get("artist") != null ? track.get("artist") : "");
        holder.tvDuration.setText(track.get("duration") != null ? track.get("duration") : "");

        String art = track.get("albumArt");
        if (art != null && !art.isEmpty()) {
            Glide.with(context).load(art).centerCrop()
                    .placeholder(R.color.purple_deeper)
                    .into(holder.ivArt);
        } else {
            holder.ivArt.setImageResource(0);
        }

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID && listener != null) {
                listener.onTrackClick(pos, tracks.get(pos));
            }
        });

        holder.btnRemove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID && listener != null) {
                listener.onRemoveClick(pos, tracks.get(pos));
            }
        });
    }

    @Override
    public int getItemCount() { return tracks.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvNum, tvName, tvArtist, tvDuration;
        ImageView ivArt, btnRemove;

        VH(@NonNull View itemView) {
            super(itemView);
            tvNum      = itemView.findViewById(R.id.tv_track_num);
            tvName     = itemView.findViewById(R.id.tv_track_name);
            tvArtist   = itemView.findViewById(R.id.tv_track_artist);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            ivArt      = itemView.findViewById(R.id.iv_album_art);
            btnRemove  = itemView.findViewById(R.id.btn_remove);
        }
    }
}
