package com.example.huayeloltool.service;

import com.alibaba.fastjson2.JSON;
import com.example.huayeloltool.common.CommonRequest;
import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.enums.Heros;
import com.example.huayeloltool.model.base.CalcScoreConf;
import com.example.huayeloltool.model.cache.CustomGameCache;
import com.example.huayeloltool.model.conversation.ConversationMsg;
import com.example.huayeloltool.model.game.*;
import com.example.huayeloltool.model.score.ScoreService;
import com.example.huayeloltool.model.score.ScoreWithReason;
import com.example.huayeloltool.model.score.UserScore;
import com.example.huayeloltool.model.summoner.RankedInfo;
import com.example.huayeloltool.model.summoner.Summoner;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.example.huayeloltool.enums.GameEnums.GameFlow.IN_PROGRESS;

@Slf4j
@Service
public class GameFlowHandler extends CommonRequest {

    @Autowired
    private LcuApiService lcuApiService;
    @Autowired
    private ScoreService scoreService;

    // 注入线程池
    @Resource(name = "gameEventExecutor")
    private ExecutorService gameEventExecutor;

    @Resource(name = "scheduledExecutor")
    private ScheduledExecutorService scheduledExecutor;

    public void onGameFlowUpdate(String gameState) {
        switch (GameEnums.GameFlow.getByValue(gameState)) {
            case READY_CHECK -> this.acceptGame();
            case CHAMPION_SELECT -> gameEventExecutor.submit(this::championSelectStart);
            case IN_PROGRESS -> gameEventExecutor.submit(this::calcEnemyTeamScore);
            case END_OF_GAME -> gameEventExecutor.submit(() -> CustomGameSession.getInstance().reset());
        }
    }


    private void acceptGame() {
        // 使用线程池异步延迟执行，替代Thread.sleep
        scheduledExecutor.schedule(() -> {
            lcuApiService.acceptGame();
        }, 1500, TimeUnit.MILLISECONDS);
    }

    /**
     * 查询队友战绩
     */
    public void championSelectStart() {
        try {
            // 使用异步延迟替代Thread.sleep
            scheduledExecutor.schedule(() -> {
                try {
                    List<Long> summonerIdList = fetchTeamSummonerIdsWithRetry(3);
                    if (CollectionUtils.isEmpty(summonerIdList)) {
                        log.error("队友召唤师ID查询失败！");
                        return;
                    }

                    if (CustomGameSession.isSoloRank() && summonerIdList.size() < 5) {
                        log.error("队伍人数不为5，size：{}:", summonerIdList.size());
                    }

                    // 获取队友mate信息
                    List<Summoner> summonerList = lcuApiService.listSummoner(summonerIdList);
                    if (CollectionUtils.isEmpty(summonerList)) {
                        log.info("查询召唤师信息失败, summonerList为空！ ");
                        return;
                    }

                    // 分析战绩并打印
                    calculateScore(summonerList, true);
                } catch (Exception e) {
                    log.error("查询队友战绩异常", e);
                }
            }, 1500, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            log.error("查询队友战绩异常", e);
        }
    }


    /**
     * 尝试获取当前团队中的召唤师ID列表
     *
     * @param attemptCount 当前尝试次数
     * @return 召唤师ID列表
     */
    private List<Long> fetchTeamSummonerIdsWithRetry(int attemptCount) {
        List<Long> summonerIdList = getTeamSummonerIdList();

        // 如果获取成功且数量为5，直接返回
        if (CollectionUtils.isNotEmpty(summonerIdList) && summonerIdList.size() == 5) {
            return summonerIdList;
        }

        // 如果已经尝试了3次，返回当前结果
        if (attemptCount >= 2) {
            return summonerIdList;
        }

        // 使用线程池延迟执行下一次尝试
        try {
            return scheduledExecutor.schedule(() -> fetchTeamSummonerIdsWithRetry(attemptCount + 1),
                    200, TimeUnit.MILLISECONDS).get();
        } catch (Exception e) {
            log.error("获取召唤师ID列表时发生异常", e);
            return summonerIdList;
        }
    }

    /**
     * 计算敌方队伍分数
     */
    public void calcEnemyTeamScore() {
        try {
            GameFlowSession session = lcuApiService.queryGameFlowSession();
            if (session == null || !session.getPhase().equals(IN_PROGRESS)) {
                return;
            }

            Summoner summoner = Summoner.getInstance();
            if (summoner == null) {
                return;
            }

            long selfID = summoner.getSummonerId();
            // 这里会拿到敌我双方所有人的id
            Pair<List<Long>, List<Long>> allUsersFromSession = getAllUsersSummonerIdFromSession(selfID, session);
            List<Long> enemySummonerIDList = allUsersFromSession.getRight();
            if (CollectionUtils.isEmpty(enemySummonerIDList)) {
                log.error("敌方用户ID查询为空");
                return;
            }

            // 查询分析敌方用户的信息并计算得分
            List<Summoner> summonerList = lcuApiService.listSummoner(enemySummonerIDList);
            if (CollectionUtils.isEmpty(summonerList)) {
                log.error("查询召唤师信息失败: {}", enemySummonerIDList);
                return;
            }

            calculateScore(summonerList, Boolean.FALSE);
        } catch (Exception e) {
            log.error("计算敌方队伍得分时发生错误", e);
        }
    }

    public Pair<List<Long>, List<Long>> getAllUsersSummonerIdFromSession(long selfID, GameFlowSession session) {
        List<Long> selfTeamUsers = new ArrayList<>(5);
        List<Long> enemyTeamUsers = new ArrayList<>(5);

        // 找到 我方 所属的队伍ID
        GameEnums.TeamID selfTeamID = findSelfTeamID(selfID, session);

        if (selfTeamID == GameEnums.TeamID.NONE) {
            log.error("无法分辨是蓝色方还是红色方！");
            return Pair.of(selfTeamUsers, enemyTeamUsers);
        }

        if (selfTeamID == GameEnums.TeamID.BLUE) {
            fillUserIds(session.getGameData().getTeamOne(), selfTeamUsers);
            fillUserIds(session.getGameData().getTeamTwo(), enemyTeamUsers);
        } else {
            fillUserIds(session.getGameData().getTeamTwo(), selfTeamUsers);
            fillUserIds(session.getGameData().getTeamOne(), enemyTeamUsers);
        }
        return Pair.of(selfTeamUsers, enemyTeamUsers);
    }

    private GameEnums.TeamID findSelfTeamID(long selfID, GameFlowSession session) {
        for (GameFlowSession.GameFlowSessionTeamUser teamUser : session.getGameData().getTeamOne()) {
            if (selfID == teamUser.getSummonerId()) {
                return GameEnums.TeamID.BLUE;
            }
        }

        for (GameFlowSession.GameFlowSessionTeamUser teamUser : session.getGameData().getTeamTwo()) {
            if (selfID == teamUser.getSummonerId()) {
                return GameEnums.TeamID.RED;
            }
        }

        return GameEnums.TeamID.NONE;
    }

    private void fillUserIds(List<GameFlowSession.GameFlowSessionTeamUser> teamUsers, List<Long> targetList) {
        teamUsers.stream()
                .map(GameFlowSession.GameFlowSessionTeamUser::getSummonerId)
                .filter(Objects::nonNull)
                .filter(userId -> userId > 0)
                .forEach(targetList::add);
    }

    private String rankData(String puuid) {
        try {
            RankedInfo rankData = lcuApiService.getRankData(puuid);
            RankedInfo.QueueMapDto.RANKEDSOLO5x5Dto rankedSoloInfo = rankData.getQueueMap().getRankedSolo5x5();
            String tier = rankedSoloInfo.getTier();
            String division = rankedSoloInfo.getDivision();
            Integer leaguePoints = rankedSoloInfo.getLeaguePoints();
            return String.format("【%s-%s-%d】", GameEnums.RankTier.getRankNameMap(tier), division, leaguePoints);
        } catch (Exception e) {
            log.error("查询{}战绩失败！", puuid, e);
        }
        return "";
    }

    /**
     * 计算召唤师得分
     */
    private void calculateScore(List<Summoner> summonerList, Boolean isSelf) {
        if (CollectionUtils.isEmpty(summonerList)) {
            log.warn("召唤师列表为空");
            return;
        }

        CalcScoreConf.HorseScoreConf[] horseArr = CalcScoreConf.getInstance().getHorse();
        // 存储到缓存
        summonerList.stream()
                .map(summoner -> calculateUserScore(summoner, isSelf))
                .filter(Objects::nonNull) // 过滤无效值
                .sorted(Comparator.comparingDouble(UserScore::getScore).reversed()) // 先按照分数排序
                .forEach(scoreInfo -> {
                    CustomGameCache.Item item = new CustomGameCache.Item();
                    item.setHorse(findHorseName(scoreInfo.getScore(), horseArr));
                    item.setScore(scoreInfo.getScore().intValue());
                    item.setRank(rankData(scoreInfo.getPuuid()));
                    item.setSummonerName(scoreInfo.getSummonerName());
                    List<UserScore.Kda> currKDA = scoreInfo.getCurrKDA();

                    List<CustomGameCache.KdaDetail> kdaDetails = currKDA.stream().limit(5).map(kda -> {
                        CustomGameCache.KdaDetail kdaDetail = new CustomGameCache.KdaDetail();
                        kdaDetail.setQueueGame(kda.getQueueGame());
                        kdaDetail.setWin(kda.getWin());
                        kdaDetail.setChampionId(kda.getChampionId());
                        kdaDetail.setImageUrl(Heros.getImageById(kda.getChampionId()));
                        kdaDetail.setKills(kda.getKills());
                        kdaDetail.setDeaths(kda.getDeaths());
                        kdaDetail.setAssists(kda.getAssists());
                        return kdaDetail;
                    }).toList();

                    item.setCurrKDA(kdaDetails);
                    // 存储到缓存
                    if (isSelf) {
                        CustomGameCache.getInstance().getTeamList().add(item);
                    } else {
                        CustomGameCache.getInstance().getEnemyList().add(item);
                    }
                });
        // 格式化控制台输出
        if (isSelf) {
            log.info(formatTeamInfo("【我方队友】", CustomGameCache.getInstance().getTeamList()));
        } else {
            log.info(formatTeamInfo("【敌方对手】", CustomGameCache.getInstance().getEnemyList()));
        }
    }

    /**
     * 格式化控制台队伍信息输出
     */
    private static String formatTeamInfo(String title, List<CustomGameCache.Item> teamList) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append("=".repeat(60)).append("\n");
        sb.append(String.format("【%s 队伍信息】", title)).append("\n");

        for (int i = 0; i < teamList.size(); i++) {
            CustomGameCache.Item item = teamList.get(i);
            sb.append(String.format("%d. %s [%s] %s分 %s\n",
                    i + 1,
                    item.getHorse(),
                    item.getScore(),
                    item.getRank(),
                    item.getSummonerName()
            ));

            // 输出KDA信息
            List<CustomGameCache.KdaDetail> kdaList = item.getCurrKDA();
            int size = Math.min(kdaList.size(), 3);
            List<CustomGameCache.KdaDetail> kdaDetails = kdaList.subList(0, size);
            for (CustomGameCache.KdaDetail kdaDetail : kdaDetails) {
                sb.append(String.format("%s-%s-%s-%s  ",
                        kdaDetail.getQueueGame(),
                        kdaDetail.getWin() ? "胜" : "负",
                        Heros.getNameById(kdaDetail.getChampionId()),
                        kdaDetail.getKills() + "/" + kdaDetail.getDeaths() + "/" + kdaDetail.getAssists()
                ));
            }
            sb.append("\n\n");
        }
        sb.append("=".repeat(60)).append("\n");
        return sb.toString();
    }

    private UserScore calculateUserScore(Summoner summoner, boolean isSelf) {
        try {

            long summonerID = summoner.getSummonerId();
            UserScore userScoreInfo = new UserScore(summonerID, Constant.DEFAULT_SCORE); // 创建用户评分对象，默认分数
            userScoreInfo.setSummonerName(String.format("%s#%s", summoner.getGameName(), summoner.getTagLine()));
            userScoreInfo.setPuuid(summoner.getPuuid()); // 设置用户唯一标识

            List<GameHistory.GameInfo> gameList;
            try {
                gameList = lcuApiService.listGameHistory(summoner, 0, 19); // 获取最近20场对局记录
                if (CollectionUtils.isEmpty(gameList)) {
                    log.error("【{}】战绩查询为空！", summoner.getGameName());
                    return null;
                }
            } catch (Exception e) {
                log.error("【{}】战绩列表获取失败", summoner.getGameName(), e);
                return null;
            }

            // 计算分数
            List<Long> gameIdList = gameList.stream().map(GameHistory.GameInfo::getGameId).toList();

            double finalScore = getFinalScore(gameIdList, summonerID);

            userScoreInfo.setCurrKDA(getKdas(gameList));
            userScoreInfo.setScore(finalScore);
            userScoreInfo.setExtMsg(analyzeGameHistory(gameList, summoner.getGameName(), isSelf));

            return userScoreInfo;
        } catch (Exception e) {
            log.error("【{}】计算用户得分失败", summoner.getGameName(), e);
            return null; // 顶层异常返回null（上层需处理）
        }
    }

    private double getFinalScore(List<Long> gameIdList, long summonerID) {
        List<AbstractMap.SimpleEntry<Double, LocalDateTime>> validScores = gameIdList.stream()
                .map(gameId -> {
                    try {
                        GameSummary gameSummary = lcuApiService.queryGameSummaryWithRetry(gameId);
                        ScoreWithReason score = scoreService.calcUserGameScore(summonerID, gameSummary);
                        return new AbstractMap.SimpleEntry<>(score.getScore(), gameSummary.getGameCreationDate());
                    } catch (Exception e) {
                        log.error("获取或计算对局数据失败", e);
                        return null; // 异常情况返回null，后续过滤
                    }
                }).filter(Objects::nonNull).toList();

        // 计算加权总分
        LocalDateTime nowTime = LocalDateTime.now();
        // 近期对局分数
        List<Double> currTimeScores = new ArrayList<>(validScores.size());
        // 其他时段对局分数
        List<Double> otherTimeScores = new ArrayList<>(validScores.size());

        double totalScore = 0;
        int totalGameCount = validScores.size(); // 直接使用有效对局数
        for (AbstractMap.SimpleEntry<Double, LocalDateTime> entry : validScores) {
            double score = entry.getKey();
            totalScore += score;
            if (nowTime.isBefore(entry.getValue().plusHours(24))) { // 24小时内对局为当前时段
                currTimeScores.add(score);
            } else {
                otherTimeScores.add(score);
            }
        }

        // 计算加权分数（若有效对局数为0则使用默认分）
        return totalGameCount > 0 ?
                calculateWeightedScore(currTimeScores, otherTimeScores, totalGameCount, totalScore) : Constant.DEFAULT_SCORE;
    }

    /**
     * 分析每一场对局的KDA和使用英雄
     */
    public List<UserScore.Kda> getKdas(List<GameHistory.GameInfo> gameList) {
        return gameList.stream().map(gameInfo -> {
            Participant participant = gameInfo.getParticipants().get(0);
            Participant.Stats stats = participant.getStats();
            UserScore.Kda kda = new UserScore.Kda();
            kda.setKills(stats.getKills());
            kda.setDeaths(stats.getDeaths());
            kda.setAssists(stats.getAssists());
            kda.setWin(stats.getWin());
            kda.setQueueGame(GameEnums.GameQueueID.getGameNameMap(gameInfo.getQueueId()));
            kda.setChampionName(Heros.getNameById(participant.getChampionId()));
            kda.setChampionId(participant.getChampionId());
            kda.setPosition(getPositionFromLaneAndRole(participant.getTimeline().getLane(), participant.getTimeline().getRole()));
            return kda;
        }).toList();
    }

    public static String getPositionFromLaneAndRole(String lane, String role) {
        if (lane == null || role == null) return "";
        lane = lane.toUpperCase();
        role = role.toUpperCase();

        switch (lane) {
            case "TOP":
                if ("SOLO".equals(role)) return "上单";
                break;
            case "JUNGLE":
                // role字段通常为"NONE"或空
                return "打野";
            case "MIDDLE":
                if ("SOLO".equals(role)) return "中单";
                break;
            case "BOTTOM":
                if ("DUO_CARRY".equals(role) || "SOLO".equals(role)) return "ADC";
                if ("DUO_SUPPORT".equals(role)) return "辅助";
                break;
            case "NONE":
                // 兼容部分对局，通常不能断定，返回UNKNOWN
                return "";
        }
        // 未匹配到的组合，返回UNKNOWN
        return "";
    }

    private String findHorseName(double score, CalcScoreConf.HorseScoreConf[] horseArr) {
        for (int i = 0; i < horseArr.length; i++) {
            if (score >= horseArr[i].getScore()) {
                return Constant.HORSE_NAME_CONF[i];
            }
        }
        return "";
    }

    /**
     * 自动开启下一场对局
     */
    private void autoStartNextGame() {
        // 使用异步延迟替代Thread.sleep
        scheduledExecutor.schedule(() -> {
            boolean result = lcuApiService.playAgain();
            // TODO 扩展，检查自己是不是房主
            if (result) {
                scheduledExecutor.schedule(() -> {
                    lcuApiService.autoStartMatch();
                }, 1500, TimeUnit.MILLISECONDS);
            }
        }, 1500, TimeUnit.MILLISECONDS);
    }


    /**
     * 计算加权后的总得分。
     *
     * @param currTimeScoreList  当前时间内的得分列表
     * @param otherGameScoreList 其他对局的得分列表
     * @param totalGameCount     总的对局场次数量
     * @param totalScore         所有对局的总得分
     * @return 加权后的总得分
     */
    private double calculateWeightedScore(List<Double> currTimeScoreList, List<Double> otherGameScoreList, int totalGameCount, double totalScore) {
        // 计算当前时间内所有得分之和
        double totalTimeScore = currTimeScoreList.stream().mapToDouble(Double::doubleValue).sum();
        // 计算其他对局中所有得分之和
        double totalOtherGameScore = otherGameScoreList.stream().mapToDouble(Double::doubleValue).sum();

        // 如果对局场次大于零，计算平均得分；否则设为0
        double totalGameAvgScore = totalGameCount > 0 ? totalScore / totalGameCount : 0.0;

        // 初始化加权总分
        double weightTotalScore = 0.0;
        // 计算当前时间内得分的平均值；如果列表为空，则设为0
        double avgTimeScore = !currTimeScoreList.isEmpty() ? totalTimeScore / currTimeScoreList.size() : 0;
        // 计算其他对局中得分的平均值；如果列表为空，则设为0
        double avgOtherGameScore = !otherGameScoreList.isEmpty() ? totalOtherGameScore / otherGameScoreList.size() : 0;

        // 将当前时间和其他对局中的得分按比例加入到加权总分中
        weightTotalScore += !currTimeScoreList.isEmpty() ? 0.7 * avgTimeScore : 0.7 * totalGameAvgScore;
        weightTotalScore += !otherGameScoreList.isEmpty() ? 0.3 * avgOtherGameScore : 0.3 * totalGameAvgScore;

        // 返回最终的加权总分
        return weightTotalScore;
    }

    /**
     * 从会话消息列表中获取召唤师ID列表
     */
    public List<Long> getTeamSummonerIdList() {
        String conversationID = lcuApiService.getCurrConversationID();
        if (StringUtils.isBlank(conversationID)) {
            log.error("当前不在英雄选择阶段");
            return Collections.emptyList();
        }

        List<ConversationMsg> msgList = lcuApiService.listConversationMsg(conversationID);
        if (msgList == null || msgList.isEmpty()) {
            log.error("获取会话组消息记录失败");
            return Collections.emptyList();
        }

        List<Long> summonerIDList = new ArrayList<>(5); // 初始化容量为 5 的列表
        for (ConversationMsg msg : msgList) {
            if (Constant.CONVERSATION_MSG_TYPE_SYSTEM.equals(msg.getType()) &&
                    Constant.JOINED_ROOM_MSG.equals(msg.getBody()) &&
                    msg.getFromSummonerId() > 0) {
                summonerIDList.add(msg.getFromSummonerId());
            }
        }
        return summonerIDList;
    }

    public static String analyzeGameHistory(List<GameHistory.GameInfo> gameInfoList, String gameName, boolean isTeammate) {
        if (CollectionUtils.isEmpty(gameInfoList)) {
            return "";
        }
        if (gameInfoList.size() < 3) {
            return "";
        }

        List<GameHistory.GameInfo> gameInfos = gameInfoList.subList(0, 3);
        boolean isAllSoloQueue = true;
        for (GameHistory.GameInfo gameInfo : gameInfos) {
            if (!GameEnums.GameQueueID.isNormalGameMode(gameInfo.getQueueId())) {
                isAllSoloQueue = false;
                break;
            }
        }
        if (!isAllSoloQueue) {
            return "";
        }

        boolean allTrue = gameInfoList.stream().allMatch(item -> item.getParticipants().get(0).getStats().getWin());
        boolean allFalse = gameInfoList.stream().noneMatch(item -> item.getParticipants().get(0).getStats().getWin());
        if (allTrue || allFalse) {
            if (isTeammate) {
                if (allTrue) {
                    return "恭喜！队友：" + gameName + "三连胜，请积极对局";
                } else {
                    return "警告！队友：" + gameName + "三连跪，请谨慎对局";
                }
            } else {
                if (allTrue) {
                    return "警告！对手：" + gameName + "三连胜，请注意针对";
                } else {
                    return "恭喜！对手：" + gameName + "三连跪，请注意针对";
                }
            }
        }
        return "";
    }


    /**
     * 处理游戏模式数据（改为非静态方法，提供更好的错误处理）
     *
     * @param data 包含游戏模式信息的数据字符串
     */
    public void handleGameMode(String data) {
        // 如果传入的数据为空，则直接返回
        if (data == null) {
            log.debug("游戏模式数据为空，跳过处理");
            return;
        }

        try {
            // 将JSON格式的数据解析成Matchmaking对象
            Matchmaking matchmaking = JSON.parseObject(data, Matchmaking.class);
            if (matchmaking == null) {
                log.warn("无法解析游戏模式数据: {}", data);
                return;
            }

            // 判断是否正在排队
            boolean isInQueue = BooleanUtils.isTrue(matchmaking.getIsCurrentlyInQueue());

            // 获取队列ID
            Integer queueId = matchmaking.getQueueId();

            // 如果正在排队并且队列ID有效（大于0）
            if (isInQueue && queueId != null && queueId > 0) {
                try {
                    // 根据队列ID获取对应的游戏模式名称
                    String modeName = GameEnums.GameQueueID.getGameNameMap(queueId);

                    // 设置当前队列ID到自定义游戏会话实例中
                    CustomGameSession.getInstance().setQueueId(queueId);

                    // 记录日志，显示当前游戏模式
                    log.info("当前模式：{}，queueId：{}", modeName, queueId);
                } catch (Exception e) {
                    log.error("处理队列ID {}时发生错误", queueId, e);
                }
            }

        } catch (Exception e) {
            // 更详细的异常处理
            log.error("处理游戏模式数据时发生错误，数据: {}", data, e);
        }
    }
}