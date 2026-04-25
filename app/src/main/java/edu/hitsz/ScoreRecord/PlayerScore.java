package edu.hitsz.ScoreRecord;

import edu.hitsz.Game.Game;
import edu.hitsz.aircraft.HeroAircraft;

import java.io.Serializable;
import java.time.LocalDateTime;

/**class:玩家分数统计*/
public class PlayerScore implements Serializable, Comparable<PlayerScore> {
    private String playerName;
    private int score;
    private LocalDateTime recordTime;

    public PlayerScore(String playerName, int score, LocalDateTime recordTime) {
        this.playerName = playerName;
        this.score = score;
        this.recordTime = recordTime;
    }
    public String getPlayerName() {
        return playerName;
    }
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    public int getScore() {
        return score;
    }
    public void setScore(int score) {
        this.score = score;
    }
    public LocalDateTime getRecordTime() {
        return recordTime;
    }
    public void setRecordTime(LocalDateTime recordTime) {
        this.recordTime = recordTime;
    }

    @Override
    public int compareTo(PlayerScore other) {
        // 按分数降序排列 (分数高的排在前面)
        return Integer.compare(other.score, this.score);
    }


    public String getTime() {
        return  recordTime.toString();
    }
}
