package com.example.video2;

import android.app.Application;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;

public class MyViewModel extends AndroidViewModel {
    private MyMediaPlayer mediaPlayer = new MyMediaPlayer();
    private MutableLiveData<Integer> progressBarVisibility = new MutableLiveData<>();
    private MutableLiveData<Pair> videoResolution = new MutableLiveData<>();
    private MutableLiveData<Integer> videoBufferingPercent = new MutableLiveData<>();
    private MutableLiveData<Integer> videoDuration = new MutableLiveData<>();
    private MutableLiveData<Integer> videoCurrentPosition = new MutableLiveData<>();
    private MutableLiveData<PlayerStatus> playerStatus = new MutableLiveData<>();
    private Handler handler = new android.os.Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if(mediaPlayer.isPlaying()) {
                videoCurrentPosition.setValue(mediaPlayer.getCurrentPosition());
                Integer currBuffPercent = videoBufferingPercent.getValue();
                double currBuffSize = currBuffPercent == null ? 0 : currBuffPercent * mediaPlayer.getDuration() / 100.0;
                if(mediaPlayer.getCurrentPosition() > currBuffSize) {
                    progressBarVisibility.setValue(View.VISIBLE);
                } else {
                    progressBarVisibility.setValue(View.INVISIBLE);
                }
                handler.postDelayed(this, 500);
            }
        }
    };
    public MyViewModel(@NonNull Application application) throws IOException {
        super(application);
        Log.w("myTag", "MyViewModel.constructor");
        progressBarVisibility.setValue(View.VISIBLE);
        // String path = "android.resource://com.example.video2/" + R.raw.news;
        // mediaPlayer.setDataSource(application, Uri.parse(path));
        String path = "https://cdn3.vvc.intersee.cn/c1f5f0e4-8ba8-4f6f-83b2-790f0d0e26d8%40news.mp4";
        mediaPlayer.setDataSource(path);
        mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {
                Log.w("myTag2", "mediaPlayer.onVideoSizeChanged [w=" + width + ", h=" + height + "]");
                videoResolution.setValue(new Pair(width, height));
            }
        });
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                int duration = mediaPlayer.getDuration();
                Log.w("myTag", "mediaPlayer.onPrepared - duration " + duration);
                videoDuration.setValue(duration);
                // mediaPlayer.setLooping(true);
                playerStart();
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                Log.e("myTag", "mediaPlayer.onError [i=" + i + ", i1=" + i1 + "]");
                // playerStop();
                return true; // 表示已经解决，不再触发OnCompletionListener监听
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.e("myTag4", "mediaPlayer.onCompletion");
                playerStatus.setValue(PlayerStatus.COMPLETED);
            }
        });

        mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                Log.e("myTag2", "mediaPlayer.onBufferingUpdate [percent=" + percent + "]");
                videoBufferingPercent.setValue(percent);
            }
        });

        mediaPlayer.prepareAsync();
    }

    @Override
    protected void onCleared() {
        Log.e("myTag", "MyViewModel.onCleared");
        super.onCleared();
        mediaPlayer.release();
    }

    public void playerStart() {
        mediaPlayer.start();
        playerStatus.setValue(PlayerStatus.PLAYING);
        handler.postDelayed(runnable, 0);
    }

    public void playerPause() {
        mediaPlayer.pause();
        playerStatus.setValue(PlayerStatus.PAUSE);
        handler.removeCallbacks(runnable);
    }

    public void playerStop() {
        mediaPlayer.stop();
        progressBarVisibility.setValue(View.INVISIBLE);
    }

    public void seekTo(int progress) {
        mediaPlayer.seekTo(progress); // 有时候seek到的位置和实际播放的位置有偏差，那是因为视频的关键帧太稀疏了，实际播放时只能找到就近关键帧的位置开始播放。
    }

    public MyMediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public LiveData<Integer> getProgressBarVisibility() {
        return progressBarVisibility;
    }

    public LiveData<Pair> getVideoResolution() {
        return videoResolution;
    }

    public void emmitVideoResolution() {
        videoResolution.setValue(videoResolution.getValue());
    }

    public LiveData<Integer> getVideoBufferingPercent() {
        return videoBufferingPercent;
    }

    public LiveData<Integer> getVideoDuration() {
        return videoDuration;
    }

    public LiveData<Integer> getVideoCurrentPosition() {
        return videoCurrentPosition;
    }

    public LiveData<PlayerStatus> getPlayerStatus() {
        return playerStatus;
    }
}

enum PlayerStatus {
    PLAYING, PAUSE, COMPLETED
}