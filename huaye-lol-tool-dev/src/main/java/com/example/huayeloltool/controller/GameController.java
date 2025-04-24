package com.example.huayeloltool.controller;

import com.example.huayeloltool.cache.UserScoreCache;
import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.model.Summoner;
import com.example.huayeloltool.model.rankinfo.RankedInfo;
import com.example.huayeloltool.service.LcuApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;


@Slf4j
@CrossOrigin(origins = "*")
@RestController(value = "game")
public class GameController {

    @Resource
    LcuApiService lcuApiService;

    @GetMapping("/lcu/getAuthInfo")
    public Object getAuthInfo() {
        try {
            return lcuApiService.getCurrSummoner();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/lcu/search/games")
    public Object searchGames() {
        try {
            return lcuApiService.listGameHistory(Summoner.getInstance(), 0, 10);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/lcu/search/rankData")
    public Object searchRankData() {
        try {
            RankedInfo rankData = lcuApiService.getRankData(Summoner.getInstance().getPuuid());
            RankedInfo.HighestRankedEntrySRDto highestRankedEntrySR = rankData.getHighestRankedEntrySR();
            String tier = highestRankedEntrySR.getTier();
            String division = highestRankedEntrySR.getDivision();
            Integer leaguePoints = highestRankedEntrySR.getLeaguePoints();

            String rankName = GameEnums.RankTier.getRankNameMap(tier);

            return String.format("段位：【%s-%s-%d】", rankName, division, leaguePoints);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/lcu/search/ewrwerwerwe")
    public Object searchGamedsds2222s() {
        try {
            return lcuApiService.searchChampionMasteryData(Summoner.getInstance().getPuuid());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/api/scores")
    public Map<String, List<UserScoreCache.ScoreOverview>> searchScores() {
        return UserScoreCache.getScore();
    }


}
