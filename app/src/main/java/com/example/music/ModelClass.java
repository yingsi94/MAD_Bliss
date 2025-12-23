package com.example.music;

import java.io.Serializable;

public class ModelClass implements Serializable {

    private String song_name;
    private String singer;
    private String duration;
    private int img;
    private String imgFileName;
    private String audioUrl;
    private String mood;

    // 1. 默认构造函数
    public ModelClass() {
    }

    // 2. 复制构造函数
    public ModelClass(ModelClass source) {
        this.song_name = source.song_name;
        this.singer = source.singer;
        this.duration = source.duration;
        this.img = source.img;
        this.imgFileName = source.imgFileName;
        this.audioUrl = source.audioUrl;
        this.mood = source.mood;
    }

    // =========================================================
    // Getters and Setters
    // =========================================================

    public String getSong_name() {
        return song_name;
    }

    public void setSong_name(String song_name) {
        this.song_name = song_name;
    }

    public String getSinger() {
        return singer;
    }

    public void setSinger(String singer) {
        this.singer = singer;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setImg(int img) {
        this.img = img;
    }

    public int getImg() {
        return img;
    }

    public String getImgFileName() {
        return imgFileName;
    }

    public void setImgFileName(String imgFileName) {
        this.imgFileName = imgFileName;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }
}