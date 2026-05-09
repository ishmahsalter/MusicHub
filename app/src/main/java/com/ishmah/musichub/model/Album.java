package com.ishmah.musichub.model;

public class Album {
    private String name, artist, coverUrl, year, trackCount;

    public Album() {}

    public Album(String name, String artist, String coverUrl,
                 String year, String trackCount) {
        this.name = name;
        this.artist = artist;
        this.coverUrl = coverUrl;
        this.year = year;
        this.trackCount = trackCount;
    }

    public String getName() { return name; }
    public String getArtist() { return artist; }
    public String getCoverUrl() { return coverUrl; }
    public String getYear() { return year; }
    public String getTrackCount() { return trackCount; }

    public void setName(String name) { this.name = name; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public void setYear(String year) { this.year = year; }
    public void setTrackCount(String trackCount) { this.trackCount = trackCount; }
}