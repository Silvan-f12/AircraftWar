package edu.hitsz.application;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * 单例模式的 AudioManager 类。
 * 【最终版】严格遵循老师要求：
 * - 背景音乐 (BGM) 使用 MediaPlayer
 * - 音效 (SFX) 使用 SoundPool
 */
public class AudioManager {
    private static final String TAG = "AudioManager";
    private static volatile AudioManager instance;

    private Context context;

    // --- 1. 音效管理 (SoundPool) ---
    private SoundPool soundPool;
    private final Map<String, Integer> soundIdMap = new HashMap<>();

    // --- 2. 背景音乐管理 (MediaPlayer) ---
    private MediaPlayer bgmMediaPlayer;
    private int currentBgmResId = -1; // 记录当前加载的资源 ID

    private boolean isAudioEnabled = true; // 总开关

    // 音量控制 (0.0f - 1.0f)
    private float bgmVolume = 0.5f;
    private float effectVolume = 0.8f;

    private AudioManager() {
        // 构造函数私有
    }

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
     */
    public void init(Context context) {
        this.context = context.getApplicationContext();
        initSoundPool();
        Log.d(TAG, "AudioManager initialized. Strategy: BGM=MediaPlayer, SFX=SoundPool.");
    }

    /**
     * 初始化 SoundPool (仅用于音效)
     */
    private void initSoundPool() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(10) // 音效最大并发数
                .setAudioAttributes(attributes)
                .build();
    }

    /**
     * 预加载音效资源
     */
    public void loadSounds(Map<String, Integer> resIdMap) {
        if (soundPool == null) return;

        for (Map.Entry<String, Integer> entry : resIdMap.entrySet()) {
            int soundId = soundPool.load(context, entry.getValue(), 1);
            soundIdMap.put(entry.getKey(), soundId);
            Log.d(TAG, "Loaded SFX: " + entry.getKey() + " -> ID: " + soundId);
        }
    }

    /**
     * 播放音效 (使用 SoundPool)
     */
    public void playSound(String soundName) {
        if (!isAudioEnabled || soundPool == null || !soundIdMap.containsKey(soundName) || effectVolume <= 0.001f) {
            return;
        }
        Integer soundId = soundIdMap.get(soundName);
        if (soundId != null) {
            soundPool.play(soundId, effectVolume, effectVolume, 1, 0, 1.0f);
        }
    }

    /**
     * 播放背景音乐 (使用 MediaPlayer + setDataSource + prepareAsync)
     * 【优化版】使用异步加载，避免阻塞主线程，支持更灵活的源。
     */
    public void playBgm(int resId) {
        Log.d(TAG, "[playBgm] Requested ResID: " + resId + ", Enabled: " + isAudioEnabled);

        // 1. 检查总开关
        if (!isAudioEnabled) {
            Log.w(TAG, "[playBgm] Blocked: Audio is DISABLED.");
            stopBgm();
            return;
        }

        // 2. 检查音量
        if (bgmVolume <= 0.001f) {
            Log.w(TAG, "[playBgm] Blocked: Volume is too low.");
            stopBgm();
            return;
        }

        // 3. 如果资源相同且正在播放，仅更新音量
        if (currentBgmResId == resId && bgmMediaPlayer != null && bgmMediaPlayer.isPlaying()) {
            bgmMediaPlayer.setVolume(bgmVolume, bgmVolume);
            Log.d(TAG, "[playBgm] Already playing same BGM, updated volume.");
            return;
        }

        // 4. 停止并释放旧的 MediaPlayer
        stopBgm();

        try {
            Log.d(TAG, "[playBgm] Creating new MediaPlayer instance...");
            bgmMediaPlayer = new MediaPlayer(); // 手动创建实例

            // 5. 设置数据源 (从 raw 资源获取 FileDescriptor)
            // 这是比 create() 更底层的做法，允许我们在 prepare 前后做更多操作
            android.content.res.AssetFileDescriptor afd = context.getResources().openRawResourceFd(resId);
            if (afd == null) {
                Log.e(TAG, "[playBgm] Failed to open resource FD: " + resId);
                return;
            }

            bgmMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close(); // 关闭描述符，但 MediaPlayer 已持有引用

            // 6. 配置播放器
            bgmMediaPlayer.setLooping(true);
            bgmMediaPlayer.setVolume(bgmVolume, bgmVolume);

            // 7. 设置监听器
            bgmMediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "[playBgm] Prepared successfully. Starting playback...");
                mp.start();
                if (mp.isPlaying()) {
                    Log.d(TAG, "[playBgm] SUCCESS! BGM is playing.");
                } else {
                    Log.e(TAG, "[playBgm] FAILED! start() called but not playing.");
                }
            });

            bgmMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "[playBgm] MediaPlayer Error: what=" + what + ", extra=" + extra);
                String errorMsg = "";
                if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN) errorMsg = "Unknown error";
                if (extra == -1004) errorMsg = "(Code -1004: File format/codec unsupported)";
                Log.e(TAG, "[playBgm] Detail: " + errorMsg);

                // 出错后清理
                stopBgm();
                return true; // 表示已处理
            });

            currentBgmResId = resId;

            // 8. 异步准备 (不会阻塞主线程)
            // 如果是极小的文件，也可以用 prepare() 同步，但 prepareAsync() 更安全
            bgmMediaPlayer.prepareAsync();

        } catch (Exception e) {
            Log.e(TAG, "[playBgm] Exception during setup", e);
            stopBgm();
        }
    }


    /**
     * 暂停背景音乐
     */
    public void pauseBgm() {
        if (bgmMediaPlayer != null && bgmMediaPlayer.isPlaying()) {
            bgmMediaPlayer.pause();
            Log.d(TAG, "[pauseBgm] BGM paused.");
        }
    }
    /**
     * 【新增】暂停指定资源 ID 的背景音乐
     *
     * @param resId 要暂停的音乐资源 ID (例如 R.raw.game_bgm)
     * @return true 如果成功暂停了对应的音乐;
     *         false 如果当前没播放音乐，正在播放的不是这首，或者音乐本身就没在播放。
     */
    public boolean pauseBgm(int resId) {
        if (bgmMediaPlayer == null) {
            Log.w(TAG, "[pauseBgm:0x" + Integer.toHexString(resId) + "] Failed: MediaPlayer is null.");
            return false;
        }

        // 1. 检查是否是当前正在播放的音乐
        if (currentBgmResId != resId) {
            Log.w(TAG, "[pauseBgm:0x" + Integer.toHexString(resId) + "] Ignored: Currently playing ResID=0x"
                    + Integer.toHexString(currentBgmResId) + ".");
            return false;
        }

        // 2. 检查是否已经在暂停状态
        if (!bgmMediaPlayer.isPlaying()) {
            Log.d(TAG, "[pauseBgm:0x" + Integer.toHexString(resId) + "] Already paused.");
            return false; // 或者返回 true 表示状态已达预期，视业务逻辑而定，这里返回 false 表示未执行动作
        }

        // 3. 执行暂停
        try {
            bgmMediaPlayer.pause();
            Log.d(TAG, "[pauseBgm:0x" + Integer.toHexString(resId) + "] SUCCESS. BGM paused.");
            return true;
        } catch (IllegalStateException e) {
            Log.e(TAG, "[pauseBgm] IllegalStateException", e);
            return false;
        }
    }

    public void resumeBgm() {
        if (bgmMediaPlayer != null) {
            // 如果是因为 pause 而停止，直接 start
            if (!bgmMediaPlayer.isPlaying()) {
                // 注意：如果之前还没 prepare 完，这里可能需要重新 prepare，
                // 但通常 pause/resume 流程中 prepare 早已完成。
                // 为了安全，检查一下状态，或者直接尝试 start (如果状态不对会抛异常，需捕获)
                try {
                    bgmMediaPlayer.start();
                    Log.d(TAG, "[resumeBgm] BGM resumed.");
                } catch (IllegalStateException e) {
                    Log.w(TAG, "[resumeBgm] Cannot resume, MediaPlayer in wrong state. Re-triggering playBgm.");
                    // 如果状态乱了，重新播放当前曲目
                    if (currentBgmResId != -1) {
                        playBgm(currentBgmResId);
                    }
                }
            }
        }
    }
    /**
     * 【可选新增】恢复指定 ID 的音乐
     * 只有当当前挂起的音乐 ID 匹配时才恢复。
     */
    public boolean resumeBgm(int resId) {
        if (bgmMediaPlayer == null) {
            return false;
        }
        if (currentBgmResId != resId) {
            Log.w(TAG, "[resumeBgm:0x" + Integer.toHexString(resId) + "] Ignored: ID mismatch.");
            return false;
        }

        try {
            if (!bgmMediaPlayer.isPlaying()) {
                bgmMediaPlayer.start();
                return true;
            }
            return false; // 已经在播放
        } catch (IllegalStateException e) {
            Log.e(TAG, "[resumeBgm] Error", e);
            return false;
        }
    }

    /**
     * 停止并释放背景音乐
     */
    public void stopBgm() {
        if (bgmMediaPlayer != null) {
            try {
                if (bgmMediaPlayer.isPlaying()) {
                    bgmMediaPlayer.stop();
                }
                bgmMediaPlayer.reset(); // 重置状态，比直接 release 更稳妥地复用
                bgmMediaPlayer.release(); // 彻底释放底层资源
            } catch (IllegalStateException e) {
                Log.e(TAG, "[stopBgm] IllegalState while stopping", e);
            } finally {
                bgmMediaPlayer = null;
                currentBgmResId = -1;
                Log.d(TAG, "[stopBgm] BGM stopped and released.");
            }
        }
    }

    /**
     * 【新增】停止指定资源 ID 的背景音乐
     *
     * @param resId 要停止的音乐资源 ID (例如 R.raw.menu_bgm)
     * @return true 如果成功停止了对应的音乐;
     *         false 如果当前没播放音乐，或者正在播放的不是这首音乐。
     */
    public boolean stopBgm(int resId) {
        if (bgmMediaPlayer == null) {
            Log.w(TAG, "[stopBgm:0x" + Integer.toHexString(resId) + "] Failed: MediaPlayer is null.");
            return false;
        }

        // 核心逻辑：检查当前播放的是否是指定的资源 ID
        if (currentBgmResId != resId) {
            Log.w(TAG, "[stopBgm:0x" + Integer.toHexString(resId) + "] Ignored: Currently playing ResID=0x"
                    + Integer.toHexString(currentBgmResId) + ".");
            return false;
        }

        Log.d(TAG, "[stopBgm:0x" + Integer.toHexString(resId) + "] Match found. Stopping and releasing...");
        performStopAndRelease();
        return true;
    }
    /**
     * 【私有辅助】执行实际的停止和释放操作
     * 统一处理状态重置、资源释放和变量清零，避免代码重复。
     */
    private void performStopAndRelease() {
        if (bgmMediaPlayer == null) return;

        try {
            if (bgmMediaPlayer.isPlaying()) {
                bgmMediaPlayer.stop();
            }
            // reset() 将播放器置于 Idle 状态，允许重新设置数据源
            bgmMediaPlayer.reset();
            // release() 释放底层解码器资源
            bgmMediaPlayer.release();
        } catch (IllegalStateException e) {
            Log.e(TAG, "[performStopAndRelease] IllegalState while stopping", e);
        } finally {
            // 无论是否出错，都要清空引用和状态
            bgmMediaPlayer = null;
            currentBgmResId = -1;
            Log.d(TAG, "[performStopAndRelease] BGM resources fully released.");
        }
    }

    public void setAudioEnabled(boolean isAudioEnabled) {
        this.isAudioEnabled = isAudioEnabled;
        if (!isAudioEnabled) {
            pauseBgm(); // 关闭时暂停
        } else {
            // 开启时，如果之前有音乐，可以选择自动恢复，这里由外部控制
        }
    }

    /**
     * 设置背景音乐音量
     */
    public void setBgmVolume(float volume) {
        this.bgmVolume = Math.max(0.0f, Math.min(1.0f, volume));
        if (bgmMediaPlayer != null) {
            bgmMediaPlayer.setVolume(this.bgmVolume, this.bgmVolume);
        }
    }

    /**
     * 设置音效音量
     */
    public void setSoundVolume(float volume) {
        this.effectVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    /**
     * 释放所有资源
     */
    public void release() {
        Log.d(TAG, "Releasing all audio resources...");
        stopBgm();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
            soundIdMap.clear();
        }
        instance = null;
    }
}