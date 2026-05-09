package com.ishmah.musichub.model;

public class Track {
    private String name, artist, duration, albumArt, trackId, streamCount, url;

    public Track() {}

    public Track(String name, String artist, String duration,
                 String albumArt, String trackId, String streamCount, String url) {
        this.name = name;
        this.artist = artist;
        this.duration = duration;
        this.albumArt = albumArt;
        this.trackId = trackId;
        this.streamCount = streamCount;
        this.url = url;
    }

    public String getName() { return name; }
    public String getArtist() { return artist; }
    public String getDuration() { return duration; }
    public String getAlbumArt() { return albumArt; }
    public String getTrackId() { return trackId; }
    public String getStreamCount() { return streamCount; }
    public String getUrl() { return url; }

    public void setName(String name) { this.name = name; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setAlbumArt(String albumArt) { this.albumArt = albumArt; }
    public void setTrackId(String trackId) { this.trackId = trackId; }
    public void setStreamCount(String streamCount) { this.streamCount = streamCount; }
    public void setUrl(String url) { this.url = url; }
}