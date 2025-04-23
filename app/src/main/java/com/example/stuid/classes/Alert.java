package com.example.stuid.classes;

import android.app.AlertDialog;
import android.content.Context;

public class Alert {
    public static void showAlert(String title, String message, Context context) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
