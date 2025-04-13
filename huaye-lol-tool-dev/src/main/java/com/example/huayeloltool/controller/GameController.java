package com.example.huayeloltool.controller;

import com.example.huayeloltool.cache.UserScoreCache;
import com.example.huayeloltool.model.CurrSummoner;
import com.example.huayeloltool.service.LcuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


//@Slf4j
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
            String puuid1 = CurrSummoner.getInstance().getPuuid();
            return lcuService.listGameHistory(puuid1, 0, 10);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/lcu/search/sld")
    public Object searchGame2222s() {
        try {
            return lcuService.getRankData(CurrSummoner.getInstance().getPuuid());
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
