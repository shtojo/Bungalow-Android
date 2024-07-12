package com.smj.bungalow;

import android.app.Activity;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

class StoredSettings {

    String homeMac;
    String localServer;
    String remoteServer;
    String password;
    int serverPort;

    private Activity activity;

    StoredSettings(Activity activity){
        this.activity = activity;
    }

    // To save preferences, update the member variables then call Save
    void Save(){
        SharedPreferences prefs = activity.getSharedPreferences("com.smj.bungalow.prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.putString("homeMac", homeMac);
        editor.putString("localServer", localServer);
        editor.putString("remoteServer", remoteServer);
        editor.putInt("serverPort", serverPort);
        editor.putString("password", password);
        editor.apply();  // commit=immediate, apply=background
    }

    // To retrieve preferences, call Retrieve then access the member variables
    void Retrieve() {
        SharedPreferences prefs = activity.getSharedPreferences("com.smj.bungalow.prefs", MODE_PRIVATE);
        localServer = prefs.getString("localServer", "");
        remoteServer = prefs.getString("remoteServer", "");
        homeMac = prefs.getString("homeMac", "");
        serverPort = prefs.getInt("serverPort", 1000);
        password = prefs.getString("password", "");
    }
}
