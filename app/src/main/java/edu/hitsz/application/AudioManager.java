package edu.hitsz.application;

import android.content.Context;
import android.media.AudioAttributes;

import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * 单例模式的 AudioManager 类。
 * 该类将同时管理 SoundPool (音效) 和 MediaPlayer (背景音乐)。
 * */
public class AudioManager {
    private static final String TAG = "AudioManager";
    private static volatile AudioManager instance;

    private Context context;

    // --- 音效管理 (SoundPool) ---
    private SoundPool soundPool;
    private final Map<String, Integer> soundIdMap = new HashMap<>();

    // --- 背景音乐管理 (MediaPlayer) ---
    private MediaPlayer bgmMediaPlayer;
    private String currentBgmResName; // 记录当前播放的 BGM 资源名，避免重复加载

    // 音量控制 (0.0f - 1.0f)
    private float bgmVolume = 0.5f;
    private float effectVolume = 0.8f;

    private AudioManager(){
        //构造函数私有，防止外部实例化
    }
    /**
     * 获取单例实例
     * 必须在调用其他方法前先在 Application 或 MainActivity 中调用 init()
     */
    public static AudioManager getInstance() {
        if (instance == null) {
            synchronized (AudioManager.class) {
                if (instance == null) {
                    instance = new AudioManager();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化音频管理器
     * @param context 应用上下文
     */
    public void init(Context context) {
        this.context = context.getApplicationContext();
        initSoundPool();
        Log.d(TAG, "AudioManager initialized.");
    }

    /**
     * 初始化 SoundPool
     * 适配不同 Android 版本
     */
    private void initSoundPool() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(10) // 最大同时播放音效数
                .setAudioAttributes(attributes)
                .build();
    }

    /**
     * 预加载音效资源
     * 建议在游戏启动时调用，避免播放时延迟
     * @param resIdMap 键为自定义名称 (如 "bullet_hit")，值为 R.raw.xxx
     */
    public void loadSounds(Map<String, Integer> resIdMap) {
        if (soundPool == null) return;

        for (Map.Entry<String, Integer> entry : resIdMap.entrySet()) {
            int soundId = soundPool.load(context, entry.getValue(), 1);
            soundIdMap.put(entry.getKey(), soundId);
            Log.d(TAG, "Loaded sound: " + entry.getKey() + " -> ID: " + soundId);
        }
    }

    /**
     * 播放音效
     * @param soundName 音效名称 (需在 loadSounds 中注册)
     */
    public void playSound(String soundName) {
        if (soundPool == null || !soundIdMap.containsKey(soundName)) {
            Log.w(TAG, "Sound not found or pool not initialized: " + soundName);
            return;
        }
        Integer soundId = soundIdMap.get(soundName);
        if (soundId != null) {
            soundPool.play(soundId, effectVolume, effectVolume, 1, 0, 1.0f);
        }
    }

    /**
     * 播放背景音乐
     * 支持循环播放，如果当前正在播放同名音乐则忽略
     * @param resId 音乐资源 ID (R.raw.bgm_xxx)
     * @param isLooping 是否循环，由音乐开关按键决定
     */
    public void playBgm(int resId, boolean isLooping) {
        // 简单优化：如果正在播放同一个资源，则不重新加载

        if (bgmMediaPlayer != null && bgmMediaPlayer.isPlaying()) {
            // 如果需要切换音乐，先停止当前音乐
            // 如果不想频繁切换，可以在此处添加逻辑判断是否真的是同一首
        }

        stopBgm(); // 停止当前正在播放的 BGM

        try {
            bgmMediaPlayer = MediaPlayer.create(context, resId);
            if (bgmMediaPlayer != null) {
                bgmMediaPlayer.setLooping(isLooping);
                bgmMediaPlayer.setVolume(bgmVolume, bgmVolume);
                bgmMediaPlayer.start();
                Log.d(TAG, "Started BGM: " + resId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing BGM", e);
        }
    }

    /**
     * 暂停背景音乐
     */
    public void pauseBgm() {
        if (bgmMediaPlayer != null && bgmMediaPlayer.isPlaying()) {
            bgmMediaPlayer.pause();
        }
    }

    /**
     * 恢复背景音乐
     */
    public void resumeBgm() {
        if (bgmMediaPlayer != null && !bgmMediaPlayer.isPlaying()) {
            bgmMediaPlayer.start();
        }
    }

    /**
     * 停止并释放背景音乐
     */
    public void stopBgm() {
        if (bgmMediaPlayer != null) {
            if (bgmMediaPlayer.isPlaying()) {
                bgmMediaPlayer.stop();
            }
            bgmMediaPlayer.release();
            bgmMediaPlayer = null;
        }
    }

    /**
     * 设置背景音乐音量
     * @param volume 0.0f - 1.0f
     */
    public void setBgmVolume(float volume) {
        this.bgmVolume = Math.max(0.0f, Math.min(1.0f, volume));
        if (bgmMediaPlayer != null) {
            bgmMediaPlayer.setVolume(this.bgmVolume, this.bgmVolume);
        }
    }

    /**
     * 设置音效音量
     * @param volume 0.0f - 1.0f
     */
    public void setEffectVolume(float volume) {
        this.effectVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    /**
     * 释放所有音频资源
     * 在游戏退出或 onDestroy 时调用
     */
    public void release() {
        Log.d(TAG, "Releasing audio resources...");

        stopBgm();

        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
            soundIdMap.clear();
        }
    }
}
