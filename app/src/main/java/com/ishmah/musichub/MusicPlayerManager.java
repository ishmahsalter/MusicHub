package com.ishmah.musichub;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.ishmah.musichub.db.UserProfileDao;
import com.ishmah.musichub.model.Track;
import java.util.ArrayList;
import java.util.List;

public class MusicPlayerManager {

    private static MusicPlayerManager instance;
    private static final String TAG = "MusicPlayerManager";
    private static final int MAX_PREVIEW_SECONDS = 30;

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
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Listening time tracking
    private Context appContext;
    private UserProfileDao profileDao;
    private int secondsPlayedInSession = 0;

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
                    stopSecondsCounter();
                    saveListeningTime();
                    isPlaying = false;
                    currentProgress = 0;
                    notifyPlayStateChanged(false);
                    // Auto next
                    if (hasPlaylist()) playNext();
                }
            }
        }
    };

    // Seconds counter runnable — increments once per second while playing
    private final Runnable secondsCountRunnable = new Runnable() {
        @Override
        public void run() {
            if (secondsPlayedInSession < MAX_PREVIEW_SECONDS) {
                secondsPlayedInSession++;
            }
            if (isPlaying) {
                handler.postDelayed(this, 1000);
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

    public void init(Context context) {
        if (appContext == null) {
            appContext = context.getApplicationContext();
            profileDao = new UserProfileDao(appContext);
        }
    }

    // ── Listening time helpers ─────────────────────────────────────────────

    private void startSecondsCounter() {
        handler.removeCallbacks(secondsCountRunnable);
        handler.postDelayed(secondsCountRunnable, 1000);
    }

    private void stopSecondsCounter() {
        handler.removeCallbacks(secondsCountRunnable);
    }

    private void saveListeningTime() {
        if (secondsPlayedInSession > 0 && profileDao != null) {
            profileDao.addListeningTime(secondsPlayedInSession);
        }
        secondsPlayedInSession = 0;
    }

    // ── Listener management ───────────────────────────────────────────────

    public void addListener(OnPlayerStateChangedListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            listener.onPlayStateChanged(isPlaying);
            listener.onProgressChanged(currentProgress, totalDuration);
            if (!currentTrackName.isEmpty()) {
                listener.onTrackChanged(currentTrackName, currentArtistName,
                        currentAlbumArt, currentTrackId);
            }
        }
    }

    public void removeListener(OnPlayerStateChangedListener listener) {
        listeners.remove(listener);
    }

    public void setListener(OnPlayerStateChangedListener listener) {
        if (listener == null) return;
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
        listener.onPlayStateChanged(isPlaying);
        listener.onProgressChanged(currentProgress, totalDuration);
        if (!currentTrackName.isEmpty()) {
            listener.onTrackChanged(currentTrackName, currentArtistName,
                    currentAlbumArt, currentTrackId);
        }
    }

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

    // ── Playback ──────────────────────────────────────────────────────────

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

        // saves time from any previous track before starting new one
        stopMediaPlayer();

        if (this.currentPreviewUrl != null && !this.currentPreviewUrl.isEmpty()) {
            startMediaPlayer(this.currentPreviewUrl);
        } else {
            isPlaying = true;
            notifyPlayStateChanged(true);
            handler.post(simulationRunnable);
            startSecondsCounter();
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
                startSecondsCounter();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                stopSecondsCounter();
                saveListeningTime();
                isPlaying = false;
                currentProgress = 0;
                handler.removeCallbacks(progressRunnable);
                notifyPlayStateChanged(false);
                notifyProgressChanged(0, totalDuration);
                if (hasPlaylist()) {
                    handler.postDelayed(this::playNext, 500);
                }
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error what=" + what);
                stopSecondsCounter();
                isPlaying = false;
                notifyPlayStateChanged(false);
                handler.post(simulationRunnable);
                return true;
            });

        } catch (Exception e) {
            Log.e(TAG, "startMediaPlayer error: " + e.getMessage());
            isPlaying = true;
            notifyPlayStateChanged(true);
            handler.post(simulationRunnable);
            startSecondsCounter();
        }
    }

    public void togglePlayPause() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    isPlaying = false;
                    handler.removeCallbacks(progressRunnable);
                    stopSecondsCounter();
                    saveListeningTime();
                } else {
                    mediaPlayer.start();
                    isPlaying = true;
                    handler.removeCallbacks(progressRunnable);
                    handler.post(progressRunnable);
                    startSecondsCounter();
                }
            } catch (Exception e) {
                Log.e(TAG, "togglePlayPause error: " + e.getMessage());
                isPlaying = !isPlaying;
            }
        } else {
            isPlaying = !isPlaying;
            if (isPlaying) {
                handler.removeCallbacks(simulationRunnable);
                handler.post(simulationRunnable);
                startSecondsCounter();
            } else {
                handler.removeCallbacks(simulationRunnable);
                stopSecondsCounter();
                saveListeningTime();
            }
        }
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
        stopSecondsCounter();
        saveListeningTime();
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

    // ── Getters ───────────────────────────────────────────────────────────

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
