package com.example.stuid.api;

public interface ProfilePhotoDownloadCallback {
    void onSuccess(String base64Image);
    void onFailure(String error);
}
