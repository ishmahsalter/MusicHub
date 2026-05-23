package com.ishmah.musichub.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.ishmah.musichub.R;
import com.ishmah.musichub.activity.ArtistActivity;
import com.ishmah.musichub.db.ArtistDao;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FollowingArtistAdapter extends RecyclerView.Adapter<FollowingArtistAdapter.ViewHolder> {

    private final Context context;
    private final List<Map<String, String>> list;
    private final ArtistDao artistDao;

    public FollowingArtistAdapter(Context context, ArtistDao artistDao) {
        this.context = context;
        this.list = new ArrayList<>();
        this.artistDao = artistDao;
    }

    public void updateList(List<Map<String, String>> newList) {
        list.clear();
        list.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_following_artist, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> item = list.get(position);
        String artistId    = item.get("artist_id");
        String artistName  = item.get("artist_name");
        String artistPhoto = item.get("artist_photo");
        String genre       = item.get("genre");

        holder.tvName.setText(artistName != null ? artistName : "");
        holder.tvGenre.setText(genre != null ? genre : "");

        Glide.with(context)
                .load(artistPhoto)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.ivPhoto);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ArtistActivity.class);
            intent.putExtra("artistId", artistId);
            intent.putExtra("artistName", artistName);
            intent.putExtra("artistPhoto", artistPhoto);
            intent.putExtra("genre", genre);
            context.startActivity(intent);
        });

        holder.btnUnfollow.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            list.remove(pos);
            notifyItemRemoved(pos);
            artistDao.unfollowArtist(artistId, null);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final CircleImageView ivPhoto;
        final TextView tvName, tvGenre;
        final Button btnUnfollow;

        ViewHolder(View v) {
            super(v);
            ivPhoto     = v.findViewById(R.id.iv_artist_photo);
            tvName      = v.findViewById(R.id.tv_artist_name);
            tvGenre     = v.findViewById(R.id.tv_genre);
            btnUnfollow = v.findViewById(R.id.btn_unfollow);
        }
    }
}
