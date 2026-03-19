package edu.hitsz.application;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// 引入你的具体类，确保包名正确
import edu.hitsz.R;
import edu.hitsz.aircraft.*;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.bullet.HeroBullet;
import edu.hitsz.props.FireSupplyPlusProp;
import edu.hitsz.props.HpProp;
import edu.hitsz.props.FireSupplyProp;
import edu.hitsz.props.BombSupplyProp;

/**
 * 综合管理图片的加载、访问与释放 (Android 优化版)
 *
 * @author hitsz
 */
public class ImageManager {

    private static final String TAG = "ImageManager";

    /**
     * 类名 - 图片 映射，存储各基类的图片
     */
    private static final Map<String, Bitmap> CLASSNAME_IMAGE_MAP = new HashMap<>();

    // ==================== 静态图片引用 ====================
    public static Bitmap BACKGROUND_IMAGE;
    public static Bitmap HERO_IMAGE;
    public static Bitmap HERO_BULLET_IMAGE;
    public static Bitmap ENEMY_BULLET_IMAGE;
    public static Bitmap MOB_ENEMY_IMAGE;
    public static Bitmap ELITE_ENEMY_IMAGE;
    public static Bitmap ELITE_ENEMY_PLUS_IMAGE; // 修正拼写错误
    public static Bitmap BOSS_ENEMY_IMAGE;
    public static Bitmap HP_PROP_IMAGE;
    public static Bitmap FIRE_SUPPLY_PROP_IMAGE;
    public static Bitmap FIRE_SUPPLY_PLUS_PROP_IMAGE;
    public static Bitmap BOMB_SUPPLY_PROP_IMAGE;

    // 标记是否已初始化
    private static boolean isInitialized = false;

    // 图片加载配置 (核心优化：降低内存占用)
    private static final BitmapFactory.Options OPTIONS = new BitmapFactory.Options();

    static {
        // 使用 RGB_565 代替 ARGB_8888，内存占用减少一半 (2字节/像素 vs 4字节/像素)
        // 对于游戏素材，通常不需要 Alpha 通道的精细渐变，RGB_565 足够且性能更好
        OPTIONS.inPreferredConfig = Bitmap.Config.RGB_565;
        OPTIONS.inSampleSize = 1;
        OPTIONS.inJustDecodeBounds = false;
    }

    /**
     * 必须在 Application 或 MainActivity 的 onCreate 中调用此方法进行初始化
     * @param context 上下文对象
     */
    public static void init(Context context) {
        if (isInitialized) {
            Log.w(TAG, "ImageManager already initialized, skipping...");
            return;
        }

        Log.d(TAG, "Start loading images...");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 加载背景
            BACKGROUND_IMAGE = decodeResource(context, R.drawable.bg);

            // 2. 加载英雄与子弹
            HERO_IMAGE = decodeResource(context, R.drawable.hero);
            HERO_BULLET_IMAGE = decodeResource(context, R.drawable.bullet_hero);
            ENEMY_BULLET_IMAGE = decodeResource(context, R.drawable.bullet_enemy);

            // 3. 加载敌机
            MOB_ENEMY_IMAGE = decodeResource(context, R.drawable.mob);
            ELITE_ENEMY_IMAGE = decodeResource(context, R.drawable.elite);
            ELITE_ENEMY_PLUS_IMAGE = decodeResource(context, R.drawable.eliteplus);
            BOSS_ENEMY_IMAGE = decodeResource(context, R.drawable.boss);

            // 4. 加载道具
            HP_PROP_IMAGE = decodeResource(context, R.drawable.prop_blood);
            FIRE_SUPPLY_PROP_IMAGE = decodeResource(context, R.drawable.prop_bullet);
            BOMB_SUPPLY_PROP_IMAGE = decodeResource(context, R.drawable.prop_bomb);
            FIRE_SUPPLY_PLUS_PROP_IMAGE = decodeResource(context, R.drawable.prop_bulletplus);

            // 5. 填充映射表 (用于通过 Class 自动获取图片)
            putToMap(HeroAircraft.class, HERO_IMAGE);
            putToMap(MobEnemy.class, MOB_ENEMY_IMAGE);
            putToMap(EliteEnemyAircraft.class, ELITE_ENEMY_IMAGE);
            putToMap(EliteEnemyPlusAircraft.class, ELITE_ENEMY_PLUS_IMAGE);
            putToMap(BossEnemyAircraft.class, BOSS_ENEMY_IMAGE);
            putToMap(HeroBullet.class, HERO_BULLET_IMAGE);
            putToMap(EnemyBullet.class, ENEMY_BULLET_IMAGE);
            putToMap(HpProp.class, HP_PROP_IMAGE);
            putToMap(FireSupplyProp.class, FIRE_SUPPLY_PROP_IMAGE);
            putToMap(BombSupplyProp.class, BOMB_SUPPLY_PROP_IMAGE);
            putToMap(FireSupplyPlusProp.class, FIRE_SUPPLY_PLUS_PROP_IMAGE);

            isInitialized = true;

            long duration = System.currentTimeMillis() - startTime;
            Log.d(TAG, "Images loaded successfully in " + duration + "ms");
            logMemoryUsage();

        } catch (Exception e) {
            Log.e(TAG, "Critical error: Image loading failed!", e);
            throw new RuntimeException("Image loading failed: " + e.getMessage(), e);
        }
    }

    /**
     * 辅助方法：带配置的资源解码
     */
    private static Bitmap decodeResource(Context context, int resId) {
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId, OPTIONS);
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode resource ID: " + resId);
            }
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error decoding resource ID: " + resId, e);
            return null;
        }
    }

    /**
     * 辅助方法：放入映射表
     */
    private static void putToMap(Class<?> clazz, Bitmap bitmap) {
        if (clazz != null && bitmap != null) {
            CLASSNAME_IMAGE_MAP.put(clazz.getName(), bitmap);
        }
    }

    /**
     * 通过类名获取图片
     */
    public static Bitmap get(String className) {
        if (!isInitialized) {
            Log.w(TAG, "ImageManager not initialized yet!");
            return null;
        }
        return CLASSNAME_IMAGE_MAP.get(className);
    }

    /**
     * 通过对象实例获取图片 (推荐用法)
     * 示例：ImageManager.get(myHeroAircraft);
     */
    public static Bitmap get(Object obj) {
        if (!isInitialized) {
            return null;
        }
        if (obj == null) {
            return null;
        }
        return get(obj.getClass().getName());
    }

    /**
     * 动态设置背景图
     * @param context 上下文
     * @param resId 资源ID (R.drawable.xxx)
     */
    public static void setBackGround(Context context, int resId) {
        Bitmap oldBg = BACKGROUND_IMAGE;
        // 如果旧背景存在且未被回收，先回收以释放内存
        if (oldBg != null && !oldBg.isRecycled()) {
            oldBg.recycle();
        }

        try {
            BACKGROUND_IMAGE = decodeResource(context, resId);
            // 更新映射表中的背景 (如果有类映射的话，通常背景不映射类)
        } catch (Exception e) {
            Log.e(TAG, "Failed to load new background", e);
        }
    }

    /**
     * 【核心】清理所有图片资源，释放内存
     * 必须在 Activity.onDestroy() 或游戏彻底结束时调用
     */
    public static void clear() {
        Log.d(TAG, "Clearing all image resources...");

        // 1. 回收并清空映射表
        Set<Map.Entry<String, Bitmap>> entries = CLASSNAME_IMAGE_MAP.entrySet();
        for (Map.Entry<String, Bitmap> entry : entries) {
            recycleBitmap(entry.getValue(), entry.getKey());
        }
        CLASSNAME_IMAGE_MAP.clear();

        // 2. 回收并清空静态引用
        recycleBitmap(BACKGROUND_IMAGE, "BACKGROUND_IMAGE");
        BACKGROUND_IMAGE = null;

        recycleBitmap(HERO_IMAGE, "HERO_IMAGE");
        HERO_IMAGE = null;

        recycleBitmap(HERO_BULLET_IMAGE, "HERO_BULLET_IMAGE");
        HERO_BULLET_IMAGE = null;

        recycleBitmap(ENEMY_BULLET_IMAGE, "ENEMY_BULLET_IMAGE");
        ENEMY_BULLET_IMAGE = null;

        recycleBitmap(MOB_ENEMY_IMAGE, "MOB_ENEMY_IMAGE");
        MOB_ENEMY_IMAGE = null;

        recycleBitmap(ELITE_ENEMY_IMAGE, "ELITE_ENEMY_IMAGE");
        ELITE_ENEMY_IMAGE = null;

        recycleBitmap(ELITE_ENEMY_PLUS_IMAGE, "ELITE_ENEMY_PLUS_IMAGE");
        ELITE_ENEMY_PLUS_IMAGE = null;

        recycleBitmap(BOSS_ENEMY_IMAGE, "BOSS_ENEMY_IMAGE");
        BOSS_ENEMY_IMAGE = null;

        recycleBitmap(HP_PROP_IMAGE, "HP_PROP_IMAGE");
        HP_PROP_IMAGE = null;

        recycleBitmap(FIRE_SUPPLY_PROP_IMAGE, "FIRE_SUPPLY_PROP_IMAGE");
        FIRE_SUPPLY_PROP_IMAGE = null;

        recycleBitmap(FIRE_SUPPLY_PLUS_PROP_IMAGE, "FIRE_SUPPLY_PLUS_PROP_IMAGE");
        FIRE_SUPPLY_PLUS_PROP_IMAGE = null;

        recycleBitmap(BOMB_SUPPLY_PROP_IMAGE, "BOMB_SUPPLY_PROP_IMAGE");
        BOMB_SUPPLY_PROP_IMAGE = null;

        // 3. 重置状态
        isInitialized = false;

        // 4. 建议系统回收内存
        System.gc();

        Log.d(TAG, "All image resources cleared.");
    }

    /**
     * 辅助回收方法
     */
    private static void recycleBitmap(Bitmap bitmap, String name) {
        if (bitmap != null && !bitmap.isRecycled()) {
            int size = bitmap.getByteCount();
            bitmap.recycle();
            Log.d(TAG, "Recycled: " + name + " (" + formatSize(size) + ")");
        }
    }

    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return isInitialized;
    }

    /**
     * 打印当前图片内存占用 (调试用)
     */
    public static void logMemoryUsage() {
        long totalBytes = 0;

        for (Bitmap bmp : CLASSNAME_IMAGE_MAP.values()) {
            if (bmp != null && !bmp.isRecycled()) {
                totalBytes += bmp.getByteCount();
            }
        }
        // 加上静态引用的
        for (Bitmap bmp : new Bitmap[]{
                BACKGROUND_IMAGE, HERO_IMAGE, HERO_BULLET_IMAGE, ENEMY_BULLET_IMAGE,
                MOB_ENEMY_IMAGE, ELITE_ENEMY_IMAGE, ELITE_ENEMY_PLUS_IMAGE, BOSS_ENEMY_IMAGE,
                HP_PROP_IMAGE, FIRE_SUPPLY_PROP_IMAGE, FIRE_SUPPLY_PLUS_PROP_IMAGE, BOMB_SUPPLY_PROP_IMAGE
        }) {
            if (bmp != null && !bmp.isRecycled()) {
                // 避免重复计算已经在 map 里的 (实际上 map 里就是这些引用，这里只是为了保险起见统计所有非空引用)
                // 由于 map 和静态变量指向同一对象，上面循环已经统计了 map 里的。
                // 这里简单处理：只统计 map 里的即可，因为所有加载的图片都放入了 map。
            }
        }

        // 修正：直接遍历 map 统计即可，因为静态变量和 map 指向同一内存地址
        // 重新计算一次确保准确
        totalBytes = 0;
        for (Bitmap bmp : CLASSNAME_IMAGE_MAP.values()) {
            if (bmp != null && !bmp.isRecycled()) {
                totalBytes += bmp.getByteCount();
            }
        }

        Log.d(TAG, "Current Image Memory Usage: " + formatSize(totalBytes));
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}