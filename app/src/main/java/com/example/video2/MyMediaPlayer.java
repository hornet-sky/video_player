package com.example.video2;

import android.media.MediaPlayer;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

public class MyMediaPlayer extends MediaPlayer implements LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        Log.w("myTag", "MyMediaPlayer.onPause");
        this.pause();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        Log.w("myTag", "MyMediaPlayer.onResume");
        this.start();
    }
}
