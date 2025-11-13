package com.example.brightbuds_app.models;

public class MemoryCard {

    private int id;
    private int imageResId;
    private boolean flipped;
    private boolean matched;

    public MemoryCard(int id, int imageResId) {
        this.id = id;
        this.imageResId = imageResId;
        this.flipped = false;
        this.matched = false;
    }

    public int getId() {
        return id;
    }

    public int getImageResId() {
        return imageResId;
    }

    public boolean isFlipped() {
        return flipped;
    }

    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
    }

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }
}
