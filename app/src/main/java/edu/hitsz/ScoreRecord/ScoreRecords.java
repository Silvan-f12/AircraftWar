package edu.hitsz.ScoreRecord;

import android.content.Context;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

/**
 * 分数记录管理 (Android 版本)
 */
public class ScoreRecords {

    private static final String FILE_NAME = "score_records.dat";  // 改为 .dat 表示二进制文件
    private List<PlayerScore> scores;
    private Context context;
    private File scoreFile;

    public ScoreRecords(Context context) {
        this.context = context;
        this.scores = new ArrayList<>();

        // 【关键修改】使用应用内部存储目录
        scoreFile = new File(context.getFilesDir(), FILE_NAME);

        // 如果文件不存在，创建它
        if (!scoreFile.exists()) {
            try {
                // 确保父目录存在
                File parentDir = scoreFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                scoreFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create score file: " + e.getMessage(), e);
            }
        }

        // 加载已有记录
        loadScores();
    }

    /**
     * 加载分数记录 (使用对象流反序列化)
     */
    @SuppressWarnings("unchecked")
    private void loadScores() {
        if (!scoreFile.exists() || scoreFile.length() == 0) {
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(scoreFile))) {
            Object obj = ois.readObject();
            if (obj instanceof List) {
                scores = (List<PlayerScore>) obj;
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            // 如果读取失败，初始化为空列表
            scores = new ArrayList<>();
        }
    }

    /**
     * 保存分数记录 (使用对象流序列化)
     */
    private void saveScores() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(scoreFile))) {
            oos.writeObject(scores);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 添加新分数记录
     * @param score 分数值
     */
    public void addPlayerScoreRecordsScore(int score) {
        // 创建 PlayerScore 对象（玩家名默认为 "Player"）
        PlayerScore playerScore = new PlayerScore("Player", score, LocalDateTime.now());
        scores.add(playerScore);
        saveScores();
    }

    /**
     * 添加新分数记录（可指定玩家名）
     * @param playerName 玩家名
     * @param score 分数值
     */
    public void addPlayerScoreRecordsScore(String playerName, int score) {
        PlayerScore playerScore = new PlayerScore(playerName, score, LocalDateTime.now());
        scores.add(playerScore);
        saveScores();
    }

    /**
     * 获取所有分数记录
     */
    public List<PlayerScore> getScores() {
        return new ArrayList<>(scores);
    }

    /**
     * 获取所有分数值（兼容旧接口）
     */
    public List<Integer> getScoreValues() {
        List<Integer> scoreValues = new ArrayList<>();
        for (PlayerScore ps : scores) {
            scoreValues.add(ps.getScore());
        }
        return scoreValues;
    }

    /**
     * 获取最高分
     */
    public int getHighestScore() {
        if (scores.isEmpty()) {
            return 0;
        }
        int max = scores.get(0).getScore();
        for (PlayerScore ps : scores) {
            if (ps.getScore() > max) {
                max = ps.getScore();
            }
        }
        return max;
    }

    /**
     * 获取最新分数
     */
    public int getLatestScore() {
        if (scores.isEmpty()) {
            return 0;
        }
        return scores.get(scores.size() - 1).getScore();
    }

    /**
     * 清空记录
     */
    public void clear() {
        scores.clear();
        saveScores();
    }

    /**
     * 获取记录数量
     */
    public int getCount() {
        return scores.size();
    }
}