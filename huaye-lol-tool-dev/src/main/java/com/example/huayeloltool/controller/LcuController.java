package com.example.huayeloltool.controller;

import com.example.huayeloltool.Main;
import com.example.huayeloltool.common.AutoAcceptGameRequest;
import com.example.huayeloltool.common.BanChampionRequest;
import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.model.base.GameGlobalSetting;
import com.example.huayeloltool.model.cache.CustomGameCache;
import com.example.huayeloltool.model.game.GameHistory;
import com.example.huayeloltool.model.score.UserScore;
import com.example.huayeloltool.model.summoner.Summoner;
import com.example.huayeloltool.service.GameStateUpdateService;
import com.example.huayeloltool.service.LcuApiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DESCRIPTION
 */
@Slf4j
@RestController
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
    public List<GameBriefInfo> getSummonerGameHistory(@RequestParam("name") String name) {
        String[] split = name.split("#");
        Summoner summoner = lcuApiService.getSummonerByNickName(split[0], split[1]);
        List<GameHistory.GameInfo> gameInfos = lcuApiService.listGameHistory(summoner, 0, 5);
        if (CollectionUtils.isEmpty(gameInfos)) {
            return new ArrayList<>();
        }
        List<UserScore.Kda> kdas = gameStateUpdateService.getKdas(gameInfos);
        if (CollectionUtils.isEmpty(kdas)) {
            return new ArrayList<>();
        }
        return getGameBriefInfos(kdas);
    }


    @PostMapping("/set/ban/champion")
    public void setBanChampion(@RequestBody BanChampionRequest request) {
        GameGlobalSetting.getInstance().setAutoBanChampID(request.getChampionId());
        GameGlobalSetting.getInstance().setAutoBanChamp(request.getAutoBanChamp());

        log.info("set ban champion id: {}", request.getChampionId());
    }

    @PostMapping("/set/auto/accept/game")
    public void autoAcceptGame(@RequestBody AutoAcceptGameRequest request) {
        GameGlobalSetting.getInstance().setAutoAcceptGame(request.getAutoAcceptGame());

        log.info("auto accept game id: {}", request.getAutoAcceptGame());
    }


    @GetMapping("/global/config")
    public GameGlobalSetting getConfig() {
        return GameGlobalSetting.getInstance();
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
            GameBriefInfo gameBriefInfo = new GameBriefInfo(kda.getQueueGame(), kda.getChampionName(), kda.getWin() ? Constant.WIN_STR : Constant.LOSE_STR, String.format("%s/%s/%s", kda.getKills(), kda.getDeaths(), kda.getAssists()));
            gameBriefInfos.add(gameBriefInfo);
        }
        return gameBriefInfos;
    }

    @PostMapping("/reconnect")
    public boolean reconnect() {
        Main.main(null);
        return true;
    }

}
