package com.ishmah.musichub.model;

public class FeaturedCard {
    private String badge, title, subtitle, meta,
            artUrl, previewUrl, trackId;

    public FeaturedCard(String badge, String title, String subtitle,
                        String meta, String artUrl,
                        String previewUrl, String trackId) {
        this.badge = badge;
        this.title = title;
        this.subtitle = subtitle;
        this.meta = meta;
        this.artUrl = artUrl;
        this.previewUrl = previewUrl;
        this.trackId = trackId;
    }

    public String getBadge() { return badge; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getMeta() { return meta; }
    public String getArtUrl() { return artUrl; }
    public String getPreviewUrl() { return previewUrl; }
    public String getTrackId() { return trackId; }

    public void setArtUrl(String artUrl) { this.artUrl = artUrl; }
    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }
}