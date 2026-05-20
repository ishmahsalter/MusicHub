package com.ishmah.musichub.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import de.hdodenhof.circleimageview.CircleImageView;
import com.ishmah.musichub.R;
import com.ishmah.musichub.activity.ArtistActivity;
import java.util.List;
import java.util.Map;

public class ArtistChipAdapter extends RecyclerView.Adapter<ArtistChipAdapter.VH> {

    private final Context context;
    private final List<Map<String, String>> artists;

    public ArtistChipAdapter(Context context, List<Map<String, String>> artists) {
        this.context = context;
        this.artists = artists;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_artist_chip, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Map<String, String> artist = artists.get(position);
        String name = artist.get("name");
        String photo = artist.get("photo");

        holder.tvName.setText(name);

        if (photo != null && !photo.isEmpty()) {
            Glide.with(context).load(photo)
                    .placeholder(R.drawable.ic_profile)
                    .into(holder.ivArtist);
        } else {
            holder.ivArtist.setImageResource(R.drawable.ic_profile);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ArtistActivity.class);
            intent.putExtra("artistName", name);
            intent.putExtra("artistId", name);
            intent.putExtra("artistPhoto", photo != null ? photo : "");
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return artists.size(); }

    static class VH extends RecyclerView.ViewHolder {
        CircleImageView ivArtist;
        TextView tvName;

        VH(@NonNull View itemView) {
            super(itemView);
            ivArtist = itemView.findViewById(R.id.iv_artist);
            tvName = itemView.findViewById(R.id.tv_artist_name);
        }
    }
}
