package com.ishmah.musichub.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ishmah.musichub.R;
import java.util.List;
import java.util.Map;

public class PlaylistDialogAdapter extends
        RecyclerView.Adapter<PlaylistDialogAdapter.ViewHolder> {

    private Context context;
    private List<Map<String, String>> playlists;
    private int selectedPosition = -1;
    private OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(int position, Map<String, String> playlist);
    }

    public PlaylistDialogAdapter(Context context,
                                 List<Map<String, String>> playlists,
                                 OnPlaylistClickListener listener) {
        this.context = context;
        this.playlists = playlists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_playlist_dialog, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> playlist = playlists.get(position);
        holder.tvPlaylistName.setText(playlist.get("name"));
        holder.tvTrackCount.setText(playlist.get("track_count") + " songs");

        if (selectedPosition == position) {
            holder.ivCheck.setVisibility(View.VISIBLE);
        } else {
            holder.ivCheck.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_ID) return;
            selectedPosition = adapterPosition;
            notifyDataSetChanged();
            if (listener != null) {
                listener.onPlaylistClick(adapterPosition, playlists.get(adapterPosition));
            }
        });
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public Map<String, String> getSelectedPlaylist() {
        if (selectedPosition >= 0 && selectedPosition < playlists.size()) {
            return playlists.get(selectedPosition);
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlaylistName, tvTrackCount;
        ImageView ivCheck;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlaylistName = itemView.findViewById(R.id.tv_playlist_name);
            tvTrackCount = itemView.findViewById(R.id.tv_track_count);
            ivCheck = itemView.findViewById(R.id.iv_check);
        }
    }
}