package com.ishmah.musichub;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.ishmah.musichub.model.Track;
import java.util.ArrayList;
import java.util.List;

public class MusicPlayerManager {

    private static MusicPlayerManager instance;
    private static final String TAG = "MusicPlayerManager";

    private String currentTrackName = "";
    private String currentArtistName = "";
    private String currentAlbumArt = "";
    private String currentTrackId = "";
    private String currentPreviewUrl = "";
    private boolean isPlaying = false;
    private int currentProgress = 0;
    private int totalDuration = 30;

    private List<Track> playlist = new ArrayList<>();
    private int currentIndex = -1;

    private MediaPlayer mediaPlayer;
    // Pakai Handler di main thread
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Support multiple listeners
    private final List<OnPlayerStateChangedListener> listeners = new ArrayList<>();

    public interface OnPlayerStateChangedListener {
        void onTrackChanged(String trackName, String artistName,
                            String albumArt, String trackId);
        void onPlayStateChanged(boolean isPlaying);
        void onProgressChanged(int progress, int total);
    }

    // Progress runnable — update setiap 500ms dari MediaPlayer
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null) {
                try {
                    if (mediaPlayer.isPlaying()) {
                        currentProgress = mediaPlayer.getCurrentPosition() / 1000;
                        totalDuration = mediaPlayer.getDuration() > 0
                                ? mediaPlayer.getDuration() / 1000 : 30;
                        notifyProgressChanged(currentProgress, totalDuration);
                        handler.postDelayed(this, 500);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "progressRunnable error: " + e.getMessage());
                }
            }
        }
    };

    // Simulation runnable — kalau tidak ada preview URL
    private final Runnable simulationRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPlaying) {
                if (currentProgress < totalDuration) {
                    currentProgress++;
                    notifyProgressChanged(currentProgress, totalDuration);
                    handler.postDelayed(this, 1000);
                } else {
                    isPlaying = false;
                    currentProgress = 0;
                    notifyPlayStateChanged(false);
                    // Auto next
                    if (hasPlaylist()) playNext();
                }
            }
        }
    };

    private MusicPlayerManager() {}

    public static MusicPlayerManager getInstance() {
        if (instance == null) {
            instance = new MusicPlayerManager();
        }
        return instance;
    }

    // Add listener
    public void addListener(OnPlayerStateChangedListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            // Immediately sync state
            listener.onPlayStateChanged(isPlaying);
            listener.onProgressChanged(currentProgress, totalDuration);
            if (!currentTrackName.isEmpty()) {
                listener.onTrackChanged(currentTrackName, currentArtistName,
                        currentAlbumArt, currentTrackId);
            }
        }
    }

    // Remove listener
    public void removeListener(OnPlayerStateChangedListener listener) {
        listeners.remove(listener);
    }

    // Legacy setListener support
    public void setListener(OnPlayerStateChangedListener listener) {
        if (listener == null) return;
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
        // Immediately sync
        listener.onPlayStateChanged(isPlaying);
        listener.onProgressChanged(currentProgress, totalDuration);
        if (!currentTrackName.isEmpty()) {
            listener.onTrackChanged(currentTrackName, currentArtistName,
                    currentAlbumArt, currentTrackId);
        }
    }

    // Notify all listeners
    private void notifyTrackChanged(String name, String artist,
                                    String art, String id) {
        handler.post(() -> {
            for (OnPlayerStateChangedListener l : new ArrayList<>(listeners)) {
                if (l != null) l.onTrackChanged(name, artist, art, id);
            }
        });
    }

    private void notifyPlayStateChanged(boolean playing) {
        handler.post(() -> {
            for (OnPlayerStateChangedListener l : new ArrayList<>(listeners)) {
                if (l != null) l.onPlayStateChanged(playing);
            }
        });
    }

    private void notifyProgressChanged(int progress, int total) {
        handler.post(() -> {
            for (OnPlayerStateChangedListener l : new ArrayList<>(listeners)) {
                if (l != null) l.onProgressChanged(progress, total);
            }
        });
    }

    public void setPlaylist(List<Track> tracks, int startIndex) {
        this.playlist = new ArrayList<>(tracks);
        this.currentIndex = startIndex;
    }

    public void playTrack(String trackName, String artistName,
                          String albumArt, String trackId,
                          String previewUrl, int duration) {
        this.currentTrackName = trackName;
        this.currentArtistName = artistName;
        this.currentAlbumArt = albumArt;
        this.currentTrackId = trackId;
        this.currentPreviewUrl = previewUrl != null ? previewUrl : "";
        this.totalDuration = duration > 0 ? duration : 30;
        this.currentProgress = 0;

        notifyTrackChanged(trackName, artistName, albumArt, trackId);

        stopMediaPlayer();

        if (this.currentPreviewUrl != null &&
                !this.currentPreviewUrl.isEmpty()) {
            startMediaPlayer(this.currentPreviewUrl);
        } else {
            // Simulasi
            isPlaying = true;
            notifyPlayStateChanged(true);
            handler.post(simulationRunnable);
        }
    }

    private void startMediaPlayer(String url) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build());
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                isPlaying = true;
                totalDuration = mp.getDuration() > 0
                        ? mp.getDuration() / 1000 : 30;
                notifyPlayStateChanged(true);
                notifyProgressChanged(0, totalDuration);
                handler.removeCallbacks(progressRunnable);
                handler.post(progressRunnable);
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                currentProgress = 0;
                handler.removeCallbacks(progressRunnable);
                notifyPlayStateChanged(false);
                notifyProgressChanged(0, totalDuration);
                // Auto next
                if (hasPlaylist()) {
                    handler.postDelayed(() -> playNext(), 500);
                }
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error what=" + what);
                isPlaying = false;
                notifyPlayStateChanged(false);
                // Fallback simulasi
                handler.post(simulationRunnable);
                return true;
            });

        } catch (Exception e) {
            Log.e(TAG, "startMediaPlayer error: " + e.getMessage());
            // Fallback simulasi
            isPlaying = true;
            notifyPlayStateChanged(true);
            handler.post(simulationRunnable);
        }
    }

    public void togglePlayPause() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    isPlaying = false;
                    handler.removeCallbacks(progressRunnable);
                } else {
                    mediaPlayer.start();
                    isPlaying = true;
                    handler.removeCallbacks(progressRunnable);
                    handler.post(progressRunnable);
                }
            } catch (Exception e) {
                Log.e(TAG, "togglePlayPause error: " + e.getMessage());
                isPlaying = !isPlaying;
            }
        } else {
            // Simulasi
            isPlaying = !isPlaying;
            if (isPlaying) {
                handler.removeCallbacks(simulationRunnable);
                handler.post(simulationRunnable);
            } else {
                handler.removeCallbacks(simulationRunnable);
            }
        }
        // Notify SETELAH state berubah
        notifyPlayStateChanged(isPlaying);
    }

    public void playNext() {
        if (playlist.isEmpty()) return;
        currentIndex = (currentIndex + 1) % playlist.size();
        Track next = playlist.get(currentIndex);
        playTrack(
                next.getName(),
                next.getArtist(),
                next.getAlbumArt() != null ? next.getAlbumArt() : "",
                next.getTrackId(),
                next.getPreviewUrl() != null ? next.getPreviewUrl() : "",
                30);
    }

    public void playPrev() {
        if (playlist.isEmpty()) return;
        currentIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
        Track prev = playlist.get(currentIndex);
        playTrack(
                prev.getName(),
                prev.getArtist(),
                prev.getAlbumArt() != null ? prev.getAlbumArt() : "",
                prev.getTrackId(),
                prev.getPreviewUrl() != null ? prev.getPreviewUrl() : "",
                30);
    }

    public void seekTo(int progressSeconds) {
        currentProgress = progressSeconds;
        if (mediaPlayer != null) {
            try {
                mediaPlayer.seekTo(progressSeconds * 1000);
            } catch (Exception e) {
                Log.e(TAG, "seekTo error: " + e.getMessage());
            }
        }
        notifyProgressChanged(currentProgress, totalDuration);
    }

    private void stopMediaPlayer() {
        handler.removeCallbacks(progressRunnable);
        handler.removeCallbacks(simulationRunnable);
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "stop error: " + e.getMessage());
            }
            mediaPlayer = null;
        }
        isPlaying = false;
    }

    // Getters
    public String getCurrentTrackName() { return currentTrackName; }
    public String getCurrentArtistName() { return currentArtistName; }
    public String getCurrentAlbumArt() { return currentAlbumArt; }
    public String getCurrentTrackId() { return currentTrackId; }
    public String getCurrentPreviewUrl() { return currentPreviewUrl; }
    public boolean isPlaying() { return isPlaying; }
    public int getCurrentProgress() { return currentProgress; }
    public int getTotalDuration() { return totalDuration; }
    public boolean hasTrack() { return !currentTrackName.isEmpty(); }
    public boolean hasPlaylist() { return !playlist.isEmpty(); }
    public int getCurrentIndex() { return currentIndex; }

    public String formatTime(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    public void destroy() {
        stopMediaPlayer();
        listeners.clear();
        instance = null;
    }
}