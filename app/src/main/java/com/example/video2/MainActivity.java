package com.example.video2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Pair;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private SurfaceView surfaceView;
    private FrameLayout surfaceFrame;
    private ImageView playerStatusImageView;
    private SeekBar playerProgressSeekBar;
    private TextView playRemainingTimeTextView;
    private ConstraintLayout controllerLayout;
    private MyViewModel vm;
    private Handler handler = new Handler();
    private Runnable hideControllerRunnable = new Runnable() {
        @Override
        public void run() {
            controllerLayout.setVisibility(View.INVISIBLE);
        }
    };

    public MainActivity() {
        Log.w("myTag", "MainActivity.constructor"); // 每次旋转手机屏幕Activity都会重新构建
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.w("myTag", "MainActivity.onCreate");
        progressBar = findViewById(R.id.progressBar);
        surfaceView = findViewById(R.id.surfaceView);
        surfaceFrame = findViewById(R.id.surfaceFrame);
        playerStatusImageView = findViewById(R.id.playerStatusImageView);
        playerProgressSeekBar = findViewById(R.id.playerProgressSeekBar);
        playRemainingTimeTextView = findViewById(R.id.playRemainingTimeTextView);
        controllerLayout = findViewById(R.id.controllerLayout);

        vm = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication()))
                .get(MyViewModel.class);
        vm.getProgressBarVisibility().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer visibility) {
                Log.w("myTag", "ProgressBarVisibility.Observer.onChanged - " + visibility);
                progressBar.setVisibility(visibility);
            }
        });

        vm.getVideoResolution().observe(this, new Observer<Pair>() {
            @Override
            public void onChanged(Pair pair) {
                if(pair == null) return;
                int videoWidth = (Integer) pair.first;
                int videoHeight = (Integer) pair.second;
                Log.w("myTag", "VideoResolution.Observer.onChanged [videoWidth=" + videoWidth + ", videoHeight=" + videoHeight + "]");
                surfaceResize(videoWidth, videoHeight);
            }
        });

        vm.getVideoDuration().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer currDuration) {
                Log.w("myTag2", "VideoDuration.Observer.onChanged [currDuration=" + currDuration + "]");
                playerProgressSeekBar.setMax(currDuration);
            }
        });
        vm.getVideoBufferingPercent().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer currPercent) {
                Log.w("myTag2", "VideoBufferingPercent.Observer.onChanged [currPercent=" + currPercent + "]");
                playerProgressSeekBar.setSecondaryProgress((int) (playerProgressSeekBar.getMax() * currPercent / 100.0));
            }
        });

        vm.getVideoCurrentPosition().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer currPosition) {
                Log.w("myTag2", "VideoCurrentPosition.Observer.onChanged [currPosition=" + currPosition + "]");
                playerProgressSeekBar.setProgress(currPosition);
                playRemainingTimeTextView.setText(calcTime(playerProgressSeekBar.getMax() - currPosition));
            }
        });

        vm.getPlayerStatus().observe(this, new Observer<PlayerStatus>() {
            @Override
            public void onChanged(PlayerStatus playerStatus) {
                Log.w("myTag4", "PlayerStatus.Observer.onChanged [playerStatus=" + playerStatus + "]");
                switch(playerStatus) {
                    case PLAYING:
                        playerStatusImageView.setImageResource(R.drawable.ic_baseline_pause);
                        break;
                    case COMPLETED:
                        playerStatusImageView.setImageResource(R.drawable.ic_baseline_replay);
                        break;
                    default:
                        playerStatusImageView.setImageResource(R.drawable.ic_baseline_play);
                }
            }
        });

        this.getLifecycle().addObserver(vm); // 切换到后台时 视频停止播放；切回前台时 视频恢复播放

        surfaceFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(controllerLayout.getVisibility() == View.VISIBLE) {
                    controllerLayout.setVisibility(View.INVISIBLE);
                } else {
                    controllerLayout.setVisibility(View.VISIBLE);
                    hideControllerAfter(3);
                }
            }
        });

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
            }
            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
                Log.w("myTag", "SurfaceHolder.Callback.surfaceChanged [f=" + format + ", w=" + width + ", h=" + height + "]");
                // 当画布（surfaceView）尺寸发生改变时通知 mediaPlayer
                vm.getMediaPlayer().setDisplay(surfaceHolder);
                vm.getMediaPlayer().setScreenOnWhilePlaying(true); // 在播放的时候保证不息屏
            }
            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            }
        });

        playerProgressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    Log.w("myTag3", "playerProgressSeekBar.onProgressChanged [progress=" + progress + "]");
                    vm.seekTo(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.w("myTag3", "playerProgressSeekBar.onStartTrackingTouch");
                vm.playerPause();
                handler.removeCallbacks(hideControllerRunnable);
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.w("myTag3", "playerProgressSeekBar.onStopTrackingTouch");
                vm.playerStart();
                handler.postDelayed(hideControllerRunnable, 3 * 1000);
            }
        });

        playerStatusImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayerStatus playerStatus = vm.getPlayerStatus().getValue();
                switch(playerStatus) {
                    case PLAYING:
                        vm.playerPause();
                        break;
                    default:
                        vm.playerStart();
                }
                hideControllerAfter(3);
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) { // onWindowFocusChanged 这个监听器方法也行
        super.onWindowFocusChanged(hasFocus);
        int currOri = getResources().getConfiguration().orientation;
        Log.w("myTag", "MainActivity.onConfigurationChanged - currOri " + currOri);
        if(currOri == Configuration.ORIENTATION_LANDSCAPE) {
            hideSystemUI(); // 实现“全屏”
            vm.emmitVideoResolution(); // 隐藏顶部状态栏后surface会在高度上有拉伸，因此需要重新触发调整宽度
        }
    }

    private void hideControllerAfter(int seconds) {
        handler.removeCallbacks(hideControllerRunnable);
        handler.postDelayed(hideControllerRunnable, seconds * 1000);
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void surfaceResize(final int videoWidth, final int videoHeight) {
        if(videoWidth == 0 || videoHeight == 0) {
            return;
        }
        surfaceFrame.post(new Runnable() { // 等surfaceFrame布局好了之后再执行，此时能获得surfaceFrame的高度
            @Override
            public void run() {
                int frameHeight = surfaceFrame.getHeight();
                Log.w("myTag", "surfaceResize[videoWidth=" + videoWidth + ", videoHeight=" + videoHeight + ", frameHeight=" + frameHeight + "]");
                FrameLayout.LayoutParams surfaceLayoutParams = new FrameLayout.LayoutParams(
                        frameHeight * videoWidth / videoHeight,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER
                );
                surfaceView.setLayoutParams(surfaceLayoutParams);
            }
        });

    }

    private String calcTime(Integer progress) {
        if(progress == null) {
            return "";
        }
        return progress / 1000 / 60 + ":" + progress / 1000 % 60;
    }
}