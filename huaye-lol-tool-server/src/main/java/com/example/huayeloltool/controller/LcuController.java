package com.example.huayeloltool.controller;

import com.example.huayeloltool.common.BusinessException;
import com.example.huayeloltool.common.CommonResponse;
import com.example.huayeloltool.model.base.BaseUrlClient;
import com.example.huayeloltool.model.request.AutoAcceptGameRequest;
import com.example.huayeloltool.model.request.BanChampionRequest;
import com.example.huayeloltool.enums.Heros;
import com.example.huayeloltool.model.base.GameGlobalSetting;
import com.example.huayeloltool.model.cache.CustomGameCache;
import com.example.huayeloltool.model.game.GameHistory;
import com.example.huayeloltool.model.game.GameTimeLine;
import com.example.huayeloltool.model.score.UserScore;
import com.example.huayeloltool.model.summoner.Summoner;
import com.example.huayeloltool.service.ClientMonitorService;
import com.example.huayeloltool.service.GameStateUpdateService;
import com.example.huayeloltool.service.LcuApiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
    ClientMonitorService clientMonitorService;

    public record GameBriefInfo(String queueGame, String imageUrl, Boolean win, Integer kills,
                                Integer deaths, Integer assists) {
    }

    /**
     * 根据名字获取召唤师信息
     */
    @GetMapping("/summoner/info")
    public CommonResponse<Summoner> getSummonerInfo(@RequestParam("name") String name, @RequestParam("tagLine") String tagLine) {
        return CommonResponse.success(lcuApiService.getSummonerByNickName(name, tagLine));
    }

    /**
     * 获取当前召唤师信息
     */
    @GetMapping("/custom/info")
    public CommonResponse<Summoner> getCustomSummonerInfo() {
        Summoner currSummoner = lcuApiService.getCurrSummoner();
        if (Objects.nonNull(currSummoner)) {
            return CommonResponse.success(currSummoner);
        }
        Summoner customSummoner = new Summoner();
        customSummoner.setPrivacy("private");
        customSummoner.setPuuid("123456");
        customSummoner.setGameName("Demo数据");
        return CommonResponse.success(customSummoner);
    }

    /**
     * 重新连接客户端
     */
    @PostMapping("/reconnect")
    public CommonResponse<String> reconnect() {
        log.info("收到手动重连请求");

        boolean success = clientMonitorService.manualReconnect();
        String message = clientMonitorService.getConnectionInfo();

        if (success) {
            return CommonResponse.success(message);
        } else {
            return CommonResponse.fail(500, "重连失败: " + message);
        }
    }

    /**
     * 获取连接状态
     */
    @GetMapping("/connection/status")
    public CommonResponse<ConnectionStatus> getConnectionStatus() {
        boolean clientConnected = clientMonitorService.isClientConnected();
        boolean webSocketConnected = clientMonitorService.isWebSocketConnected();
        String info = clientMonitorService.getConnectionInfo();

        return CommonResponse.success(new ConnectionStatus(clientConnected, webSocketConnected, info));
    }

    /**
     * 连接状态响应对象
     */
    public record ConnectionStatus(boolean clientConnected, boolean webSocketConnected, String info) {
    }

    /**
     * 根据名字获取召唤师对局记录
     */
    @GetMapping("/summoner/game/history")
    public CommonResponse<List<GameBriefInfo>> getSummonerGameHistory(@RequestParam("name") String name,
                                                                      @RequestParam(value = "page", required = false, defaultValue = "1") Integer pageNum,
                                                                      @RequestParam(value = "size", required = false, defaultValue = "10") Integer pageSize
    ) {
        String[] split = name.split("#");
        if (split.length != 2) {
            throw new BusinessException(500, "召唤师名称不符合规范");
        }
        if (BaseUrlClient.getInstance().getPort() <= 0) {
            // mock数据
            List<GameBriefInfo> gameBriefInfos = new ArrayList<>();
            gameBriefInfos.add(new GameBriefInfo("单排排位", Heros.getImageById(1), true, 4, 0, 17));
            gameBriefInfos.add(new GameBriefInfo("单排排位", Heros.getImageById(2), false, 4, 0, 17));
            gameBriefInfos.add(new GameBriefInfo("灵活排位", Heros.getImageById(3), true, 4, 0, 17));
            gameBriefInfos.add(new GameBriefInfo("单排排位", Heros.getImageById(4), true, 4, 0, 17));
            gameBriefInfos.add(new GameBriefInfo("灵活排位", Heros.getImageById(5), false, 4, 0, 17));
            return CommonResponse.success(gameBriefInfos);
        }

        Summoner summoner = lcuApiService.getSummonerByNickName(split[0], split[1]);
        List<GameHistory.GameInfo> gameInfos = lcuApiService.listGameHistory(summoner, (pageNum - 1) * pageSize, pageSize);
        if (CollectionUtils.isEmpty(gameInfos)) {
            return CommonResponse.success(new ArrayList<>());
        }
        List<UserScore.Kda> kdas = gameStateUpdateService.getKdas(gameInfos);
        if (CollectionUtils.isEmpty(kdas)) {
            return CommonResponse.success(new ArrayList<>());
        }
        return CommonResponse.success(getGameBriefInfos(kdas));
    }

    private List<GameBriefInfo> getGameBriefInfos(List<UserScore.Kda> kdas) {
        List<GameBriefInfo> gameBriefInfos = new ArrayList<>();
        for (UserScore.Kda kda : kdas) {
            gameBriefInfos.add(new GameBriefInfo(kda.getQueueGame(), Heros.getImageById(kda.getChampionId()), kda.getWin(), kda.getKills(), kda.getDeaths(), kda.getAssists()));
        }
        return gameBriefInfos;
    }

    /**
     * 设置自动ban英雄
     */
    @PostMapping("/set/ban/champion")
    public CommonResponse<Boolean> setBanChampion(@RequestBody BanChampionRequest request) {
        if (!request.getAutoBanChamp()) {
            gameGlobalSetting.setAutoBanChamp(false);
        }
        if (request.getAutoBanChamp() && request.getChampionId() != null && request.getChampionId() > 0) {
            gameGlobalSetting.setAutoBanChampID(request.getChampionId());
            gameGlobalSetting.setAutoBanChamp(request.getAutoBanChamp());
        }

        log.info("set ban champion id: {}", request.getChampionId());
        return CommonResponse.success(true);
    }

    /**
     * 设置自动接受对局
     */
    @PostMapping("/set/auto/accept/game")
    public CommonResponse<Boolean> autoAcceptGame(@RequestBody AutoAcceptGameRequest request) {
        gameGlobalSetting.setAutoAcceptGame(request.getAutoAcceptGame());
        log.info("auto accept game id: {}", request.getAutoAcceptGame());
        return CommonResponse.success(true);
    }

    /**
     * 获取全局配置
     */
    @GetMapping("/global/config")
    public CommonResponse<GameGlobalSetting> getConfig() {
        return CommonResponse.success(gameGlobalSetting);
    }

    /**
     * 获取我方/敌方战绩情况
     */
    @GetMapping("/game/overview")
    public CommonResponse<List<CustomGameCache.Item>> gameOverview(@RequestParam("type") Integer type) {
        List<CustomGameCache.Item> response;
        if (type == 1) {
            response = CustomGameCache.getInstance().getTeamList();
        } else {
            response = CustomGameCache.getInstance().getEnemyList();
        }
        if (CollectionUtils.isNotEmpty(response)) {
            return CommonResponse.success(response);
        }

        // mock数据
        response = new ArrayList<>();
        // 创建5个对手的数据
// 对手1：钻石选手，高分数
        List<CustomGameCache.KdaDetail> kda1 = Arrays.asList(
                new CustomGameCache.KdaDetail("单排排位", true, Heros.getImageById(1), 15, 3, 8),
                new CustomGameCache.KdaDetail("单排排位", true, Heros.getImageById(2), 12, 4, 6)
        );
        response.add(new CustomGameCache.Item("钻石", 95, "S+", "影流之主#32423", kda1));

// 对手2：铂金选手，中等分数
        List<CustomGameCache.KdaDetail> kda2 = Arrays.asList(
                new CustomGameCache.KdaDetail("单排排位", false, Heros.getImageById(3), 2, 5, 12),
                new CustomGameCache.KdaDetail("灵活排位", true, Heros.getImageById(4), 8, 4, 10)
        );
        response.add(new CustomGameCache.Item("铂金", 75, "A", "光辉女郎#32423", kda2));

// 对手3：黄金选手，一般分数
        List<CustomGameCache.KdaDetail> kda3 = Arrays.asList(
                new CustomGameCache.KdaDetail("单排排位", true, Heros.getImageById(4), 6, 6, 8),
                new CustomGameCache.KdaDetail("单排排位", false, Heros.getImageById(5), 4, 7, 5)
        );
        response.add(new CustomGameCache.Item("黄金", 65, "B+", "盲僧大师#32423", kda3));

// 对手4：白银选手，较低分数
        List<CustomGameCache.KdaDetail> kda4 = Arrays.asList(
                new CustomGameCache.KdaDetail("灵活排位", false, Heros.getImageById(6), 3, 8, 4),
                new CustomGameCache.KdaDetail("单排排位", false, Heros.getImageById(7), 2, 6, 3)
        );
        response.add(new CustomGameCache.Item("白银", 45, "C", "提莫队长#32423", kda4));

// 对手5：青铜选手，低分数
        List<CustomGameCache.KdaDetail> kda5 = Arrays.asList(
                new CustomGameCache.KdaDetail("单排排位", false, Heros.getImageById(8), 1, 10, 2),
                new CustomGameCache.KdaDetail("灵活排位", false, Heros.getImageById(9), 3, 9, 4)
        );
        response.add(new CustomGameCache.Item("青铜", 35, "D", "寒冰射手#32423", kda5));

        return CommonResponse.success(response);
    }

    /**
     * 对局时间线详情
     */
    @GetMapping("/game/info/detail")
    public GameTimeLine getSummonerInfo(@RequestParam("gameId") Long gameId) {
        return lcuApiService.getGameTimelines(gameId);
    }
}