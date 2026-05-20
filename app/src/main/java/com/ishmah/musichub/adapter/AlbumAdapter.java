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

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.VH> {

    private final Context context;
    private final List<Map<String, String>> albums;

    public AlbumAdapter(Context context, List<Map<String, String>> albums) {
        this.context = context;
        this.albums = albums;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_album, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Map<String, String> album = albums.get(position);
        holder.tvName.setText(album.get("name"));

        String plays = album.get("playcount");
        if (plays != null && !plays.isEmpty()) {
            try {
                long count = Long.parseLong(plays);
                if (count >= 1_000_000) {
                    holder.tvPlays.setText(String.format("%.1fM plays", count / 1_000_000.0));
                } else if (count >= 1_000) {
                    holder.tvPlays.setText(String.format("%.1fK plays", count / 1_000.0));
                } else {
                    holder.tvPlays.setText(plays + " plays");
                }
            } catch (NumberFormatException e) {
                holder.tvPlays.setText(plays + " plays");
            }
        } else {
            holder.tvPlays.setText("");
        }

        String imageUrl = album.get("image");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.color.purple_deeper)
                    .into(holder.ivCover);
        } else {
            holder.ivCover.setImageResource(android.R.color.transparent);
            holder.ivCover.setBackgroundResource(R.color.purple_deeper);
        }
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvName, tvPlays;

        VH(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_album_cover);
            tvName = itemView.findViewById(R.id.tv_album_name);
            tvPlays = itemView.findViewById(R.id.tv_album_plays);
        }
    }
}
