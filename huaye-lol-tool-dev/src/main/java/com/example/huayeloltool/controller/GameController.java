package com.example.huayeloltool.controller;

import com.example.huayeloltool.cache.UserScoreCache;
import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.model.CurrSummoner;
import com.example.huayeloltool.model.RankedInfo;
import com.example.huayeloltool.service.LcuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


//@Slf4j
@Slf4j
@CrossOrigin(origins = "*")
@RestController(value = "game")
public class GameController {

    @Autowired
    LcuService lcuService;

    @GetMapping("/lcu/getAuthInfo")
    public Object getAuthInfo() {
        try {
            return lcuService.getCurrSummoner();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/lcu/search/games")
    public Object searchGames() {
        try {
            return lcuService.listGameHistory(CurrSummoner.getInstance(), 0, 10);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/lcu/search/rankData")
    public Object searchRankData() {
        try {
            RankedInfo rankData = lcuService.getRankData(CurrSummoner.getInstance().getPuuid());
            RankedInfo.HighestRankedEntrySRDto highestRankedEntrySR = rankData.getHighestRankedEntrySR();
            String tier = highestRankedEntrySR.getTier();
            String division = highestRankedEntrySR.getDivision();
            Integer leaguePoints = highestRankedEntrySR.getLeaguePoints();

            String rankName = GameEnums.RankTier.getRankNameMap(tier);

            String logMessage = String.format("段位：【%s-%s-%d】", rankName, division, leaguePoints);
            return logMessage;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/lcu/search/ewrwerwerwe")
    public Object searchGamedsds2222s() {
        try {
            return lcuService.searchChampionMasteryData(CurrSummoner.getInstance().getPuuid());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/api/scores")
    public Map<String, List<UserScoreCache.ScoreOverview>> searchScores() {
        return UserScoreCache.getScore();
    }


}
