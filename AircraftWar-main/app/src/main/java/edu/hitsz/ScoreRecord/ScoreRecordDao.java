package edu.hitsz.ScoreRecord;

import java.util.List;

public interface ScoreRecordDao {
    public List<PlayerScore> getAllPlayerScoreRecords();
    public void addPlayerScoreRecords(PlayerScore playerScoreRecord);
    public void savePlayerScore(List<PlayerScore> playerScoreRecords);
    public void deletePlayerScoreRecords(String playerName);
    public void printAllRecords();

}
