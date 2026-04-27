package edu.hitsz.ScoreRecord;

import android.content.Context;
import android.util.Log;

import java.io.*;
import java.util.*;

/**
 * 分数记录管理 (最终修复版)
 * 增加了文件目录检查和空文件处理
 */
public class ScoreRecords {

    private static final String FILE_PREFIX = "score_records_";
    private static final String FILE_SUFFIX = ".dat";

    private List<PlayerScore> scores;
    private Context context;
    private File scoreFile;

    /**
     * 构造函数：指定难度构造
     * @param context
     * @param difficultyTag 难度标签，如 "simple", "medium", "difficult"
     */
    public ScoreRecords(Context context, String difficultyTag) {
        this.context = context.getApplicationContext();
        this.scores = new ArrayList<>();

        // 1. 生成文件名：score_records_simple.dat
        String fileName = FILE_PREFIX + difficultyTag + FILE_SUFFIX;

        // 2. 获取内部存储目录
        File filesDir = context.getFilesDir();
        this.scoreFile = new File(filesDir, fileName);

        // 3. 【关键修复】确保目录存在
        if (!filesDir.exists()) {
            filesDir.mkdirs();
        }

        // 4. 如果文件不存在，创建它
        if (!scoreFile.exists()) {
            try {
                scoreFile.createNewFile();
                System.out.println("【ScoreRecords】创建新文件: " + fileName);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("【ScoreRecords】创建文件失败: " + e.getMessage());
            }
        }

        // 5. 加载数据
        loadScores();
    }

    /**
     * 读取文件 (增加了空文件检查)
     */
    private void loadScores() {
        // 如果文件不存在或大小为0，直接返回空列表
        if (!scoreFile.exists() || scoreFile.length() == 0) {
            System.out.println("【ScoreRecords】文件为空或不存在，初始化空列表");
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(scoreFile))) {
            Object obj = ois.readObject();
            if (obj instanceof List) {
                scores = (List<PlayerScore>) obj;
                System.out.println("【ScoreRecords】成功读取 " + scores.size() + " 条记录");
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 读取失败（比如格式错误），重置为空列表，防止崩溃
            scores = new ArrayList<>();
        }
    }

    /**
     * 保存文件
     */
    private void saveScores() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(scoreFile))) {
            oos.writeObject(scores);
            oos.flush();
            System.out.println("【ScoreRecords】保存成功: " + scores.size() + " 条记录");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("【ScoreRecords】保存失败: " + e.getMessage());
        }
    }

    // --- 公共方法 ---

    /**
     * 添加分数
     */
    public void addPlayerScoreRecordsScore(String playerName, int score) {
        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = "游客";
        }
        PlayerScore ps = new PlayerScore(playerName, score, java.time.LocalDateTime.now());
        scores.add(ps);
        saveScores();
    }

    /**
     * 获取排序后的分数列表 (最高分在前)
     */
    public List<PlayerScore> getSortedScores() {
        List<PlayerScore> sortedList = new ArrayList<>(scores);
        Collections.sort(sortedList);
        return sortedList;
    }
    /**
     * 获取分数对应的排名
     * @param score 待查询的分数
     * @return 排名 (1代表最高，如果未上榜返回 -1 或 列表长度+1)
     */
    public int getRank(int score) {
        // 1. 获取排序后的列表（降序，最高分在最前）
        List<PlayerScore> sortedScores = getSortedScores();

        // 2. 如果列表为空，直接返回 1,这是第一个分数
        if (sortedScores == null || sortedScores.isEmpty()) {
            return 1;
        }

        // 3. 遍历列表，查找分数应该插入的位置

        for (int i = 0; i < sortedScores.size(); i++) {
            PlayerScore ps = sortedScores.get(i);
            // 如果当前分数大于等于列表中的分数，说明新分数应该排在 i 的位置
            // +1 是因为排名从 1 开始，而数组索引从 0 开始
            if (score >= ps.getScore()) {
                return i + 1;
            }
        }

        // 4. 如果分数比列表里所有人都低，排在最后
        return sortedScores.size() + 1;
    }

    /**
     * 删除指定的分数记录
     * 【修复版】不再依赖对象引用，而是根据数据内容查找删除
     */
    public boolean deleteScore(PlayerScore targetScore) {
        // 1. 遍历内部原始列表
        for (PlayerScore ps : scores) {
            // 2. 找到匹配的记录
            // 关键修改：使用 Objects.equals() 来安全地比较字符串
            // Objects.equals 会自动处理 null 的情况，不会报错
            if (ps.getScore() == targetScore.getScore() &&
                    Objects.equals(ps.getPlayerName(), targetScore.getPlayerName())) {

                // 3. 从原始列表中移除
                scores.remove(ps);
                saveScores(); // 保存文件
                System.out.println("【ScoreRecords】删除成功: " + ps.getPlayerName() + " - " + ps.getScore());
                return true;
            }
        }

        System.err.println("【ScoreRecords】删除失败，未找到匹配记录");
        return false;
    }

    /**
     * 获取所有记录（供界面刷新用）
     */
    public List<PlayerScore> getAllScores() {
        return new ArrayList<>(scores); // 返回副本，防止外部直接修改内部数据
    }
    /**
     * 【新增方法】删除所有记录
     * 功能：清空当前难度下的所有分数记录
     * @return boolean 是否删除成功
     */
    public boolean clearAllScores() {
        try {
            // 1. 清空内存中的列表
            scores.clear();

            // 2. 保存（这会写入一个空列表到文件）
            saveScores();

            System.out.println("【ScoreRecords】所有记录已清空");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("【ScoreRecords】清空记录失败: " + e.getMessage());
            return false;
        }
    }
}