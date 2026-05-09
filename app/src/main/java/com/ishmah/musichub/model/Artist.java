package com.ishmah.musichub.model;

public class Artist {
    private String name, bio, photo, monthlyListeners, totalStreams, genre;

    public Artist() {}

    public Artist(String name, String bio, String photo,
                  String monthlyListeners, String totalStreams, String genre) {
        this.name = name;
        this.bio = bio;
        this.photo = photo;
        this.monthlyListeners = monthlyListeners;
        this.totalStreams = totalStreams;
        this.genre = genre;
    }

    public String getName() { return name; }
    public String getBio() { return bio; }
    public String getPhoto() { return photo; }
    public String getMonthlyListeners() { return monthlyListeners; }
    public String getTotalStreams() { return totalStreams; }
    public String getGenre() { return genre; }

    public void setName(String name) { this.name = name; }
    public void setBio(String bio) { this.bio = bio; }
    public void setPhoto(String photo) { this.photo = photo; }
    public void setMonthlyListeners(String monthlyListeners) { this.monthlyListeners = monthlyListeners; }
    public void setTotalStreams(String totalStreams) { this.totalStreams = totalStreams; }
    public void setGenre(String genre) { this.genre = genre; }
}