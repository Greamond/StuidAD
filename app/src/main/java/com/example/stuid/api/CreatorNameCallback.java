package com.example.stuid.api;

public interface CreatorNameCallback {
    void onNameLoaded(String creatorName);
    void onFailure(String error);
}
