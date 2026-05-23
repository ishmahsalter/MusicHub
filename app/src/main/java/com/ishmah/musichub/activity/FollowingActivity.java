package com.ishmah.musichub.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ishmah.musichub.R;
import com.ishmah.musichub.ThemeHelper;
import com.ishmah.musichub.adapter.FollowingArtistAdapter;
import com.ishmah.musichub.api.DeezerApi;
import com.ishmah.musichub.api.RetrofitClient;
import com.ishmah.musichub.db.ArtistDao;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Response;

public class FollowingActivity extends AppCompatActivity {

    private static final String TAG = "FollowingActivity";

    private RecyclerView rvFollowing;
    private TextView tvEmpty;
    private FollowingArtistAdapter adapter;
    private ArtistDao artistDao;
    private DeezerApi deezerApi;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_following);

        artistDao  = new ArtistDao(this);
        deezerApi  = RetrofitClient.getDeezerInstance().create(DeezerApi.class);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        rvFollowing = findViewById(R.id.rv_following);
        tvEmpty     = findViewById(R.id.tv_empty);

        adapter = new FollowingArtistAdapter(this, artistDao);
        rvFollowing.setLayoutManager(new LinearLayoutManager(this));
        rvFollowing.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFollowing();
    }

    private void loadFollowing() {
        executor.execute(() -> {
            List<Map<String, String>> result = artistDao.getAllFollowing();

            for (Map<String, String> artist : result) {
                String photo = artist.get("artist_photo");
                if (photo == null || photo.isEmpty()) {
                    String name = artist.get("artist_name");
                    String id   = artist.get("artist_id");
                    if (name == null || name.isEmpty()) continue;
                    try {
                        Response<JsonObject> response =
                                deezerApi.searchArtist(name).execute();
                        if (response.isSuccessful() && response.body() != null) {
                            JsonArray data = response.body().getAsJsonArray("data");
                            if (data != null && data.size() > 0) {
                                JsonObject first = data.get(0).getAsJsonObject();
                                String picUrl = first.has("picture_xl")
                                        ? first.get("picture_xl").getAsString()
                                        : "";
                                if (!picUrl.isEmpty()) {
                                    artist.put("artist_photo", picUrl);
                                    artistDao.updateArtistPhoto(id, picUrl);
                                    Log.d(TAG, "Fetched photo for " + name + ": " + picUrl);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to fetch photo for " + name, e);
                    }
                }
            }

            mainHandler.post(() -> {
                adapter.updateList(result);
                boolean empty = result.isEmpty();
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvFollowing.setVisibility(empty ? View.GONE : View.VISIBLE);
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
