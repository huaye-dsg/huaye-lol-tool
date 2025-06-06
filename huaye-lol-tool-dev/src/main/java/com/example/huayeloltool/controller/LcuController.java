package com.example.huayeloltool.controller;

import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.model.cache.CustomGameCache;
import com.example.huayeloltool.model.game.GameHistory;
import com.example.huayeloltool.model.score.UserScore;
import com.example.huayeloltool.model.summoner.Summoner;
import com.example.huayeloltool.service.GameStateUpdateService;
import com.example.huayeloltool.service.LcuApiService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * DESCRIPTION
 */
@RestController(value = "/lcu")
public class LcuController {

    static LcuApiService lcuApiService = LcuApiService.getInstance();

    static GameStateUpdateService gameStateUpdateService = GameStateUpdateService.getInstance();

    public record GameBriefInfo(String queueGame, String championName, String win, String kda) {
    }

    @GetMapping("/summoner/info")
    public Summoner getSummonerInfo(@RequestParam("name") String name, @RequestParam("tagLine") String tagLine) {
        return lcuApiService.getSummonerByNickName(name, tagLine);
    }

    @GetMapping("/summoner/game/history")
    public List<GameBriefInfo> getSummonerGameHistory(@RequestParam("name") String name,
                                                      @RequestParam("tagLine") String tagLine,
                                                      @RequestParam(value = "size", required = false, defaultValue = "5") Integer size) {
        Summoner summoner = lcuApiService.getSummonerByNickName(name, tagLine);
        List<GameHistory.GameInfo> gameInfos = lcuApiService.listGameHistory(summoner, 0, size);
        if (CollectionUtils.isEmpty(gameInfos)) {
            return new ArrayList<>();
        }
        List<UserScore.Kda> kdas = gameStateUpdateService.getKdas(gameInfos);
        if (CollectionUtils.isEmpty(kdas)) {
            return new ArrayList<>();
        }
        return getGameBriefInfos(kdas);
    }

    @GetMapping("/game/overview")
    public List<CustomGameCache.Item> getSummonerGameHistory(@RequestParam("type") Integer type) {
        if (type == 1) {
            return CustomGameCache.getInstance().getTeamList();
        } else {
            return CustomGameCache.getInstance().getEnemyList();
        }
    }


    private static List<GameBriefInfo> getGameBriefInfos(List<UserScore.Kda> kdas) {
        List<GameBriefInfo> gameBriefInfos = new ArrayList<>();
        for (UserScore.Kda kda : kdas) {
            GameBriefInfo gameBriefInfo = new GameBriefInfo(kda.getQueueGame(),
                    kda.getChampionName(),
                    kda.getWin() ? Constant.WIN_STR : Constant.LOSE_STR,
                    String.format("%s/%s/%s", kda.getKills(), kda.getDeaths(), kda.getAssists()));
            gameBriefInfos.add(gameBriefInfo);
        }
        return gameBriefInfos;
    }

}
