package com.example.huayeloltool.service;

import com.alibaba.fastjson2.JSON;
import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.enums.Heros;
import com.example.huayeloltool.model.base.CalcScoreConf;
import com.example.huayeloltool.model.base.GameGlobalSetting;
import com.example.huayeloltool.model.cache.CustomGameCache;
import com.example.huayeloltool.model.game.*;
import com.example.huayeloltool.model.score.UserScore;
import com.example.huayeloltool.model.summoner.RankedInfo;
import com.example.huayeloltool.model.summoner.Summoner;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

import static com.example.huayeloltool.enums.GameEnums.GameFlow.IN_PROGRESS;

/**
 * 游戏流程处理器 - 重构版本
 * 
 * <p>负责处理LOL游戏的各个阶段，包括：
 * - 自动接受对局
 * - 英雄选择阶段的队友分析
 * - 游戏进行中的敌方分析
 * - 游戏结束后的状态重置
 * 
 * <p>重构说明：
 * 原来的单一类承担了太多职责，现在拆分为多个专门的服务类：
 * - {@link TeamAnalyzer} 负责队伍分析
 * - {@link ScoreCalculationService} 负责分数计算
 * - {@link GameHistoryAnalyzer} 负责游戏历史分析
 * - {@link ResultFormatter} 负责结果格式化
 */
@Slf4j
@Service
public class GameFlowHandler {

    @Autowired
    private LcuApiService lcuApiService;
    
    @Autowired
    private TeamAnalyzer teamAnalyzer;
    
    @Autowired
    private ScoreCalculationService scoreCalculationService;
    
    @Autowired
    private GameGlobalSetting gameGlobalSetting;

    @Resource(name = "scheduledExecutor")
    private ScheduledExecutorService scheduledExecutor;

    /**
     * 游戏流程状态更新处理入口
     * 
     * <p>根据不同的游戏状态执行相应的处理逻辑：
     * - READY_CHECK: 自动接受对局
     * - CHAMPION_SELECT: 开始队友分析
     * - IN_PROGRESS: 计算敌方队伍得分
     * - END_OF_GAME/LOBBY: 重置游戏状态
     * 
     * @param gameState 游戏状态字符串
     */
    public void onGameFlowUpdate(String gameState) {
        GameEnums.GameFlow gameFlow = GameEnums.GameFlow.getByValue(gameState);
        
        switch (gameFlow) {
            case READY_CHECK -> handleReadyCheck();
            case CHAMPION_SELECT -> handleChampionSelect();
            case IN_PROGRESS -> handleGameInProgress();
            case END_OF_GAME, LOBBY -> handleGameEnd();
            default -> log.debug("未处理的游戏状态: {}", gameState);
        }
    }

    /**
     * 处理准备检查阶段 - 自动接受对局
     */
    private void handleReadyCheck() {
        if (!Boolean.TRUE.equals(gameGlobalSetting.getAutoAcceptGame())) {
            log.debug("自动接受对局功能已关闭");
            return;
        }
        
        // 使用虚拟线程异步处理，避免阻塞
        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(Constant.ACTION_DELAY_MS);
                lcuApiService.acceptGame();
                log.info("已自动接受对局");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("自动接受对局被中断");
            } catch (Exception e) {
                log.error("自动接受对局失败", e);
            }
        });
    }

    /**
     * 处理英雄选择阶段 - 分析队友战绩
     */
    private void handleChampionSelect() {
        Thread.startVirtualThread(() -> {
            try {
                // 等待界面加载完成
                Thread.sleep(Constant.ACTION_DELAY_MS);
                
                // 获取队友信息并分析
                analyzeTeammates();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("队友战绩分析被中断");
            } catch (Exception e) {
                log.error("队友战绩分析异常", e);
            }
        });
    }

    /**
     * 处理游戏进行阶段 - 分析敌方队伍
     */
    private void handleGameInProgress() {
        Thread.startVirtualThread(() -> {
            try {
                analyzeEnemyTeam();
            } catch (Exception e) {
                log.error("敌方队伍分析失败", e);
            }
        });
    }

    /**
     * 处理游戏结束 - 重置状态
     */
    private void handleGameEnd() {
        try {
            CustomGameSession.getInstance().reset();
            log.debug("游戏状态已重置");
        } catch (Exception e) {
            log.error("重置游戏状态失败", e);
        }
    }

    /**
     * 分析队友战绩
     * 
     * <p>分析流程：
     * 1. 获取队友召唤师ID列表（带重试机制）
     * 2. 验证队伍人数
     * 3. 查询召唤师详细信息
     * 4. 计算并展示分析结果
     */
    private void analyzeTeammates() {
        // 获取队友召唤师ID列表
        List<Long> summonerIdList = teamAnalyzer.getTeamSummonerIds(3);
        
        // 验证队伍人数
        teamAnalyzer.validateTeamSize(summonerIdList, CustomGameSession.isSoloRank());
        
        if (CollectionUtils.isEmpty(summonerIdList)) {
            log.error("队友召唤师ID查询失败！");
            return;
        }

        // 获取召唤师详细信息
        List<Summoner> summonerList = teamAnalyzer.getSummonerDetails(summonerIdList);
        if (CollectionUtils.isEmpty(summonerList)) {
            log.error("查询召唤师信息失败");
            return;
        }

        // 计算并展示分析结果
        analyzeAndDisplayTeam(summonerList, true, "我方队友");
    }

    /**
     * 分析敌方队伍得分
     */
    private void analyzeEnemyTeam() {
        GameFlowSession session = lcuApiService.queryGameFlowSession();
        if (session == null || !session.getPhase().equals(IN_PROGRESS)) {
            log.debug("游戏未在进行中，跳过敌方分析");
            return;
        }

        Summoner summoner = Summoner.getInstance();
        if (summoner == null) {
            log.warn("当前召唤师信息为空，无法分析敌方");
            return;
        }

        // 获取敌方召唤师ID列表
        long selfID = summoner.getSummonerId();
        Pair<List<Long>, List<Long>> allUsers = getAllUsersSummonerIdFromSession(selfID, session);
        List<Long> enemySummonerIDList = allUsers.getRight();
        
        if (CollectionUtils.isEmpty(enemySummonerIDList)) {
            log.error("敌方用户ID查询为空");
            return;
        }

        // 查询敌方召唤师信息
        List<Summoner> enemySummonerList = teamAnalyzer.getSummonerDetails(enemySummonerIDList);
        if (CollectionUtils.isEmpty(enemySummonerList)) {
            log.error("查询敌方召唤师信息失败");
            return;
        }

        // 分析并展示敌方信息
        analyzeAndDisplayTeam(enemySummonerList, false, "敌方对手");
    }


    /**
     * 分析并展示队伍信息
     * 
     * @param summonerList 召唤师列表
     * @param isSelf 是否为己方队伍
     * @param teamTitle 队伍标题
     */
    private void analyzeAndDisplayTeam(List<Summoner> summonerList, boolean isSelf, String teamTitle) {
        if (CollectionUtils.isEmpty(summonerList)) {
            log.warn("{}召唤师列表为空", teamTitle);
            return;
        }

        CalcScoreConf.HorseScoreConf[] horseArr = CalcScoreConf.getInstance().getHorse();
        
        // 计算每个召唤师的得分并排序
        List<UserScore> userScores = summonerList.stream()
                .map(summoner -> scoreCalculationService.calculateUserScore(summoner, isSelf))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(UserScore::getScore).reversed())
                .toList();

        // 存储到缓存并格式化输出
        storeAndDisplayResults(userScores, horseArr, isSelf, teamTitle);
    }

    /**
     * 存储分析结果到缓存并格式化输出
     * 
     * @param userScores 用户得分列表
     * @param horseArr 马力配置数组
     * @param isSelf 是否为己方
     * @param teamTitle 队伍标题
     */
    private void storeAndDisplayResults(List<UserScore> userScores, CalcScoreConf.HorseScoreConf[] horseArr, 
                                      boolean isSelf, String teamTitle) {
        List<CustomGameCache.Item> cacheItems = new ArrayList<>();
        
        for (UserScore scoreInfo : userScores) {
            CustomGameCache.Item item = createCacheItem(scoreInfo, horseArr);
            cacheItems.add(item);
            
            // 存储到对应的缓存列表
            if (isSelf) {
                CustomGameCache.getInstance().getTeamList().add(item);
            } else {
                CustomGameCache.getInstance().getEnemyList().add(item);
            }
        }
        
        // 格式化并输出结果
        String formattedResult = ResultFormatter.formatTeamInfo(teamTitle, cacheItems);
        log.info(formattedResult);
        
        ResultFormatter.logTeamAnalysisComplete(teamTitle, cacheItems);
    }

    /**
     * 创建缓存项
     * 
     * @param scoreInfo 用户得分信息
     * @param horseArr 马力配置数组
     * @return 缓存项
     */
    private CustomGameCache.Item createCacheItem(UserScore scoreInfo, CalcScoreConf.HorseScoreConf[] horseArr) {
        CustomGameCache.Item item = new CustomGameCache.Item();
        item.setHorse(findHorseName(scoreInfo.getScore(), horseArr));
        item.setScore(scoreInfo.getScore().intValue());
        item.setRank(getRankData(scoreInfo.getPuuid()));
        item.setSummonerName(scoreInfo.getSummonerName());
        
        // 转换KDA详情
        List<CustomGameCache.KdaDetail> kdaDetails = scoreInfo.getCurrKDA().stream()
                .limit(Constant.MAX_KDA_DISPLAY)
                .map(this::convertToKdaDetail)
                .toList();
        
        item.setCurrKDA(kdaDetails);
        return item;
    }

    /**
     * 转换KDA信息为缓存格式
     * 
     * @param kda 原始KDA信息
     * @return 缓存格式的KDA详情
     */
    private CustomGameCache.KdaDetail convertToKdaDetail(UserScore.Kda kda) {
        CustomGameCache.KdaDetail kdaDetail = new CustomGameCache.KdaDetail();
        kdaDetail.setQueueGame(kda.getQueueGame());
        kdaDetail.setWin(kda.getWin());
        kdaDetail.setChampionId(kda.getChampionId());
        kdaDetail.setImageUrl(Heros.getImageById(kda.getChampionId()));
        kdaDetail.setKills(kda.getKills());
        kdaDetail.setDeaths(kda.getDeaths());
        kdaDetail.setAssists(kda.getAssists());
        return kdaDetail;
    }

    /**
     * 根据分数查找对应的马力等级名称
     * 
     * @param score 得分
     * @param horseArr 马力配置数组
     * @return 马力等级名称
     */
    private String findHorseName(double score, CalcScoreConf.HorseScoreConf[] horseArr) {
        for (int i = 0; i < horseArr.length; i++) {
            if (score >= horseArr[i].getScore()) {
                return Constant.HORSE_NAME_CONF[i];
            }
        }
        return "";
    }

    /**
     * 获取召唤师段位信息
     * 
     * @param puuid 召唤师PUUID
     * @return 格式化的段位信息
     */
    private String getRankData(String puuid) {
        try {
            RankedInfo rankData = lcuApiService.getRankData(puuid);
            if (rankData == null || rankData.getQueueMap() == null) {
                return "";
            }
            
            RankedInfo.QueueMapDto.RANKEDSOLO5x5Dto rankedSoloInfo = rankData.getQueueMap().getRankedSolo5x5();
            if (rankedSoloInfo == null) {
                return "";
            }
            
            String tier = rankedSoloInfo.getTier();
            String division = rankedSoloInfo.getDivision();
            Integer leaguePoints = rankedSoloInfo.getLeaguePoints();
            
            return String.format("【%s-%s-%d】", 
                    GameEnums.RankTier.getRankNameMap(tier), division, leaguePoints);
        } catch (Exception e) {
            log.warn("查询{}段位失败", puuid, e);
            return "";
        }
    }

    /**
     * 从游戏会话中获取所有用户的召唤师ID
     * 
     * @param selfID 自己的召唤师ID
     * @param session 游戏会话信息
     * @return 己方和敌方召唤师ID列表的键值对
     */
    public Pair<List<Long>, List<Long>> getAllUsersSummonerIdFromSession(long selfID, GameFlowSession session) {
        List<Long> selfTeamUsers = new ArrayList<>(Constant.STANDARD_TEAM_SIZE);
        List<Long> enemyTeamUsers = new ArrayList<>(Constant.STANDARD_TEAM_SIZE);

        // 找到自己所属的队伍ID
        GameEnums.TeamID selfTeamID = findSelfTeamID(selfID, session);

        if (selfTeamID == GameEnums.TeamID.NONE) {
            log.error("无法分辨是蓝色方还是红色方！");
            return Pair.of(selfTeamUsers, enemyTeamUsers);
        }

        // 根据队伍ID分配用户
        if (selfTeamID == GameEnums.TeamID.BLUE) {
            fillUserIds(session.getGameData().getTeamOne(), selfTeamUsers);
            fillUserIds(session.getGameData().getTeamTwo(), enemyTeamUsers);
        } else {
            fillUserIds(session.getGameData().getTeamTwo(), selfTeamUsers);
            fillUserIds(session.getGameData().getTeamOne(), enemyTeamUsers);
        }
        
        return Pair.of(selfTeamUsers, enemyTeamUsers);
    }

    /**
     * 查找自己所属的队伍ID
     * 
     * @param selfID 自己的召唤师ID
     * @param session 游戏会话信息
     * @return 队伍ID
     */
    private GameEnums.TeamID findSelfTeamID(long selfID, GameFlowSession session) {
        // 检查蓝色方
        for (GameFlowSession.GameFlowSessionTeamUser teamUser : session.getGameData().getTeamOne()) {
            if (selfID == teamUser.getSummonerId()) {
                return GameEnums.TeamID.BLUE;
            }
        }

        // 检查红色方
        for (GameFlowSession.GameFlowSessionTeamUser teamUser : session.getGameData().getTeamTwo()) {
            if (selfID == teamUser.getSummonerId()) {
                return GameEnums.TeamID.RED;
            }
        }

        return GameEnums.TeamID.NONE;
    }

    /**
     * 填充用户ID列表
     * 
     * @param teamUsers 队伍用户列表
     * @param targetList 目标ID列表
     */
    private void fillUserIds(List<GameFlowSession.GameFlowSessionTeamUser> teamUsers, List<Long> targetList) {
        teamUsers.stream()
                .map(GameFlowSession.GameFlowSessionTeamUser::getSummonerId)
                .filter(Objects::nonNull)
                .filter(userId -> userId > 0)
                .forEach(targetList::add);
    }

    /**
     * 处理游戏模式数据
     * 
     * @param data 包含游戏模式信息的数据字符串
     */
    public void handleGameMode(String data) {
        if (data == null) {
            log.debug("游戏模式数据为空，跳过处理");
            return;
        }

        try {
            Matchmaking matchmaking = JSON.parseObject(data, Matchmaking.class);
            if (matchmaking == null) {
                log.warn("无法解析游戏模式数据: {}", data);
                return;
            }

            boolean isInQueue = BooleanUtils.isTrue(matchmaking.getIsCurrentlyInQueue());
            Integer queueId = matchmaking.getQueueId();

            if (isInQueue && queueId != null && queueId > 0) {
                try {
                    String modeName = GameEnums.GameQueueID.getGameNameMap(queueId);
                    CustomGameSession.getInstance().setQueueId(queueId);
                    log.info("当前模式：{}，queueId：{}", modeName, queueId);
                } catch (Exception e) {
                    log.error("处理队列ID {}时发生错误", queueId, e);
                }
            }

        } catch (Exception e) {
            log.error("处理游戏模式数据时发生错误，数据: {}", data, e);
        }
    }
}