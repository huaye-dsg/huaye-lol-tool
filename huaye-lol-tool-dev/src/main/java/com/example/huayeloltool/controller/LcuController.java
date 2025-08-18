package com.example.huayeloltool.controller;

import com.example.huayeloltool.StartLauncher;
import com.example.huayeloltool.common.BusinessException;
import com.example.huayeloltool.model.request.AutoAcceptGameRequest;
import com.example.huayeloltool.model.request.BanChampionRequest;
import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.enums.Heros;
import com.example.huayeloltool.model.base.GameGlobalSetting;
import com.example.huayeloltool.model.cache.CustomGameCache;
import com.example.huayeloltool.model.game.GameHistory;
import com.example.huayeloltool.model.game.GameTimeLine;
import com.example.huayeloltool.model.score.UserScore;
import com.example.huayeloltool.model.summoner.Summoner;
import com.example.huayeloltool.service.GameStateUpdateService;
import com.example.huayeloltool.service.LcuApiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
public class LcuController {

    @Autowired
    LcuApiService lcuApiService;
    @Autowired
    GameStateUpdateService gameStateUpdateService;
    @Autowired
    GameGlobalSetting gameGlobalSetting;
    @Autowired
    StartLauncher startLauncher;

    public record GameBriefInfo(String queueGame, String championImage, String win, String kda, String position) {
    }

    /**
     * 根据名字获取召唤师信息
     */
    @GetMapping("/summoner/info")
    public Summoner getSummonerInfo(@RequestParam("name") String name, @RequestParam("tagLine") String tagLine) {
        return lcuApiService.getSummonerByNickName(name, tagLine);
    }

    @GetMapping("/custom/info")
    public Summoner getCustomSummonerInfo() {
        return lcuApiService.getCurrSummoner();
    }


    /**
     * 根据名字获取召唤师对局记录
     */
    @GetMapping("/summoner/game/history")
    public List<GameBriefInfo> getSummonerGameHistory(@RequestParam("name") String name,
                                                      @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                                      @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize
    ) {
        String[] split = name.split("#");
        Summoner summoner = lcuApiService.getSummonerByNickName(split[0], split[1]);
        List<GameHistory.GameInfo> gameInfos = lcuApiService.listGameHistory(summoner, (pageNum - 1) * pageSize, pageSize);
        if (CollectionUtils.isEmpty(gameInfos)) {
            return new ArrayList<>();
        }
        List<UserScore.Kda> kdas = gameStateUpdateService.getKdas(gameInfos);
        if (CollectionUtils.isEmpty(kdas)) {
            return new ArrayList<>();
        }
        return getGameBriefInfos(kdas);
    }

    /**
     * 设置自动ban英雄
     */
    @PostMapping("/set/ban/champion")
    public void setBanChampion(@RequestBody BanChampionRequest request) {
        if (!request.getAutoBanChamp()) {
            gameGlobalSetting.setAutoBanChamp(false);
        }
        if (request.getAutoBanChamp() && request.getChampionId() != null && request.getChampionId() > 0) {
            gameGlobalSetting.setAutoBanChampID(request.getChampionId());
            gameGlobalSetting.setAutoBanChamp(request.getAutoBanChamp());
        }

        log.info("set ban champion id: {}", request.getChampionId());
    }

    /**
     * 设置自动接受对局
     */
    @PostMapping("/set/auto/accept/game")
    public void autoAcceptGame(@RequestBody AutoAcceptGameRequest request) {
        gameGlobalSetting.setAutoAcceptGame(request.getAutoAcceptGame());

        log.info("auto accept game id: {}", request.getAutoAcceptGame());
    }

    /**
     * 获取全局配置
     */
    @GetMapping("/global/config")
    public GameGlobalSetting getConfig() {
        return gameGlobalSetting;
    }

    /**
     * 获取我方/敌方战绩情况
     */
    @GetMapping("/game/overview")
    public List<CustomGameCache.Item> gameOverview(@RequestParam("type") Integer type) {
        if (type == 1) {
            return CustomGameCache.getInstance().getTeamList();
        } else {
            return CustomGameCache.getInstance().getEnemyList();
        }
    }

    /**
     * 格式化对局kda
     */
    private static List<GameBriefInfo> getGameBriefInfos(List<UserScore.Kda> kdas) {
        List<GameBriefInfo> gameBriefInfos = new ArrayList<>();
        for (UserScore.Kda kda : kdas) {
            GameBriefInfo gameBriefInfo = new GameBriefInfo(kda.getQueueGame(),
                    Heros.getImageById(kda.getChampionId()),
                    kda.getWin() ? Constant.WIN_STR : Constant.LOSE_STR,
                    String.format("%s/%s/%s", kda.getKills(), kda.getDeaths(), kda.getAssists()),
                    kda.getPosition()
            );
            gameBriefInfos.add(gameBriefInfo);
        }
        return gameBriefInfos;
    }

    /**
     * 重新连接客户端
     */
    @PostMapping("/reconnect")
    public boolean reconnect() {
        startLauncher.run(null);
        return true;
    }


    /**
     * 对局时间线详情
     */
    @GetMapping("/game/info/detail")
    public GameTimeLine getSummonerInfo(@RequestParam("gameId") Long gameId) {
        return lcuApiService.getGameTimelines(gameId);
    }
}
