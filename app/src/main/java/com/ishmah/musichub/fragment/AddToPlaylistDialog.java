package com.ishmah.musichub.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ishmah.musichub.R;
import com.ishmah.musichub.adapter.PlaylistDialogAdapter;
import com.ishmah.musichub.db.PlaylistDao;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddToPlaylistDialog extends DialogFragment {

    private String trackId, trackName, artistName;
    private PlaylistDao playlistDao;
    private PlaylistDialogAdapter adapter;
    private List<Map<String, String>> playlists = new ArrayList<>();

    public static AddToPlaylistDialog newInstance(String trackId,
                                                  String trackName,
                                                  String artistName) {
        AddToPlaylistDialog dialog = new AddToPlaylistDialog();
        Bundle args = new Bundle();
        args.putString("trackId", trackId);
        args.putString("trackName", trackName);
        args.putString("artistName", artistName);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            trackId = getArguments().getString("trackId");
            trackName = getArguments().getString("trackName");
            artistName = getArguments().getString("artistName");
        }
        playlistDao = new PlaylistDao(requireContext());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_to_playlist, null);

        // Setup track info
        TextView tvTrackInfo = view.findViewById(R.id.tv_track_info);
        tvTrackInfo.setText(trackName + " — " + artistName);

        // Load playlists
        loadPlaylists();

        // Setup RecyclerView
        RecyclerView rvPlaylists = view.findViewById(R.id.rv_playlists);
        adapter = new PlaylistDialogAdapter(requireContext(), playlists,
                (position, playlist) -> {});
        rvPlaylists.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPlaylists.setAdapter(adapter);

        // Create new playlist
        LinearLayout llCreateNew = view.findViewById(R.id.ll_create_new);
        llCreateNew.setOnClickListener(v -> showCreatePlaylistDialog());

        // Cancel button
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(v -> dismiss());

        // Save button
        Button btnSave = view.findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> {
            Map<String, String> selected = adapter.getSelectedPlaylist();
            if (selected == null) {
                Toast.makeText(requireContext(),
                        "Pilih playlist dulu!", Toast.LENGTH_SHORT).show();
                return;
            }
            int playlistId = Integer.parseInt(selected.get("playlist_id"));
            playlistDao.addTrackToPlaylist(playlistId, trackId, () -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(),
                                "Ditambahkan ke " + selected.get("name"),
                                Toast.LENGTH_SHORT).show();
                        dismiss();
                    });
                }
            });
        });

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();
    }

    private void loadPlaylists() {
        playlists.clear();
        List<Map<String, String>> raw = playlistDao.getAllPlaylists();
        for (Map<String, String> p : raw) {
            Map<String, String> item = new HashMap<>(p);
            int count = playlistDao.getTrackCount(
                    Integer.parseInt(p.get("playlist_id")));
            item.put("track_count", String.valueOf(count));
            playlists.add(item);
        }

        // Kalau belum ada playlist, buat default
        if (playlists.isEmpty()) {
            playlistDao.createPlaylist("My Vibes", "gradient", "purple", () -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        loadPlaylists();
                        if (adapter != null) adapter.notifyDataSetChanged();
                    });
                }
            });
        }
    }

    private void showCreatePlaylistDialog() {
        EditText etName = new EditText(requireContext());
        etName.setHint("Nama playlist...");
        etName.setTextColor(requireContext().getResources().getColor(R.color.text_primary));
        etName.setHintTextColor(requireContext().getResources().getColor(R.color.text_hint));

        new AlertDialog.Builder(requireContext())
                .setTitle("New Playlist")
                .setView(etName)
                .setPositiveButton("Create", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        playlistDao.createPlaylist(name, "gradient", "purple", () -> {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    loadPlaylists();
                                    adapter.notifyDataSetChanged();
                                });
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}