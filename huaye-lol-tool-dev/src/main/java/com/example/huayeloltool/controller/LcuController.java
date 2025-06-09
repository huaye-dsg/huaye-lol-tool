package com.example.huayeloltool.controller;

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
    public List<GameBriefInfo> getSummonerGameHistory(@RequestParam("name") String name, @RequestParam("tagLine") String tagLine, @RequestParam(value = "size", required = false, defaultValue = "5") Integer size) {
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
//            List<CustomGameCache.Item> items = new ArrayList<>();
//
//            items.add(new CustomGameCache.Item("中等马", 115.7, "【黄金-I-54】", "或许是我太极端吧#83491", Arrays.asList("单排排位-胜-莫甘娜-4/5/16", "单排排位-胜-潘森-12/6/11", "单排排位-败-稻草人-5/13/4", "单排排位-胜-潘森-1/4/7", "单排排位-胜-卡莎-6/1/6")));
//
//            items.add(new CustomGameCache.Item("下等马", 98.9, "【黄金-IV-30】", "平平安安#54365", Arrays.asList("单排排位-败-小火龙-0/7/10", "单排排位-败-鳄鱼-6/6/2", "灵活排位-败-赵信-3/12/11", "灵活排位-败-卢锡安-9/10/9", "灵活排位-胜-剑圣-9/3/7")));
//
//            items.add(new CustomGameCache.Item("牛 马", 89.3, "【黄金-IV-74】", "半拉大馒头#13040", Arrays.asList("单排排位-胜-球女-4/5/11", "单排排位-败-球女-4/6/4", "单排排位-败-球女-1/2/1", "单排排位-胜-球女-2/2/3", "单排排位-败-球女-3/7/3")));
//
//            items.add(new CustomGameCache.Item("下等马", 100.3, "【未知-NA-0】", "青铜6#57060", Arrays.asList("单排排位-胜-厄斐琉斯-7/5/1", "单排排位-胜-厄斐琉斯-8/9/7", "单排排位-败-厄斐琉斯-2/9/2", "单排排位-败-电耗子-1/5/3", "单排排位-败-劫-7/10/13")));
//            items.add(new CustomGameCache.Item("下等马", 100.3, "【未知-NA-0】", "青铜6#57060", Arrays.asList("单排排位-胜-厄斐琉斯-7/5/1", "单排排位-胜-厄斐琉斯-8/9/7", "单排排位-败-厄斐琉斯-2/9/2", "单排排位-败-电耗子-1/5/3", "单排排位-败-劫-7/10/13")));
//
//            return items;
            return CustomGameCache.getInstance().getTeamList();
        } else {
//            List<CustomGameCache.Item> items = new ArrayList<>();
//
//            items.add(new CustomGameCache.Item("中等马", 115.7, "【黄金-I-54】", "或许是我太极端吧#83491", Arrays.asList("单排排位-胜-莫甘娜-4/5/16", "单排排位-胜-潘森-12/6/11", "单排排位-败-稻草人-5/13/4", "单排排位-胜-潘森-1/4/7", "单排排位-胜-卡莎-6/1/6")));
//
//            items.add(new CustomGameCache.Item("下等马", 98.9, "【黄金-IV-30】", "平平安安#54365", Arrays.asList("单排排位-败-小火龙-0/7/10", "单排排位-败-鳄鱼-6/6/2", "灵活排位-败-赵信-3/12/11", "灵活排位-败-卢锡安-9/10/9", "灵活排位-胜-剑圣-9/3/7")));
//
//            items.add(new CustomGameCache.Item("牛 马", 89.3, "【黄金-IV-74】", "半拉大馒头#13040", Arrays.asList("单排排位-胜-球女-4/5/11", "单排排位-败-球女-4/6/4", "单排排位-败-球女-1/2/1", "单排排位-胜-球女-2/2/3", "单排排位-败-球女-3/7/3")));
//
//            items.add(new CustomGameCache.Item("下等马", 100.3, "【未知-NA-0】", "青铜6#57060", Arrays.asList("单排排位-胜-厄斐琉斯-7/5/1", "单排排位-胜-厄斐琉斯-8/9/7", "单排排位-败-厄斐琉斯-2/9/2", "单排排位-败-电耗子-1/5/3", "单排排位-败-劫-7/10/13")));
//            items.add(new CustomGameCache.Item("下等马", 100.3, "【未知-NA-0】", "青铜6#57060", Arrays.asList("单排排位-胜-厄斐琉斯-7/5/1", "单排排位-胜-厄斐琉斯-8/9/7", "单排排位-败-厄斐琉斯-2/9/2", "单排排位-败-电耗子-1/5/3", "单排排位-败-劫-7/10/13")));
//
//            return items;
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

}
