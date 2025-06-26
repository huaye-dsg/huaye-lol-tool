package com.example.huayeloltool.service;

import com.example.huayeloltool.common.CommonRequest;
import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.enums.Heros;
import com.example.huayeloltool.enums.NewHeros;
import com.example.huayeloltool.model.Conversation.ConversationMsg;
import com.example.huayeloltool.model.cache.CustomGameCache;
import com.example.huayeloltool.model.game.CustomGameSession;
import com.example.huayeloltool.model.summoner.Summoner;
import com.example.huayeloltool.model.base.CalcScoreConf;
import com.example.huayeloltool.model.base.GameGlobalSetting;
import com.example.huayeloltool.model.game.GameFlowSession;
import com.example.huayeloltool.model.game.GameHistory;
import com.example.huayeloltool.model.game.GameSummary;
import com.example.huayeloltool.model.game.Participant;
import com.example.huayeloltool.model.summoner.RankedInfo;
import com.example.huayeloltool.model.score.ScoreService;
import com.example.huayeloltool.model.score.ScoreWithReason;
import com.example.huayeloltool.model.score.UserScore;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.example.huayeloltool.enums.GameEnums.GameFlow.IN_PROGRESS;


@Slf4j
public class GameStateUpdateService extends CommonRequest {

    private final GameGlobalSetting clientCfg = GameGlobalSetting.getInstance();

    private final LcuApiService lcuApiService = LcuApiService.getInstance();
    private final ScoreService scoreService = ScoreService.getInstance();

    private static final String SCORE_RESULT = "【%s】【%d分】%s: %s %s ";
    private static final String KDA_FORMAT = "[%s-%s-%s-%d/%d/%d]";
    private static final Double defaultScore = 100.0;

    private static GameStateUpdateService instance;

    public static GameStateUpdateService getInstance() {
        if (instance == null) {
            instance = new GameStateUpdateService();
        }
        return instance;
    }


    @SneakyThrows
    public void onGameFlowUpdate(String gameState) {
        switch (GameEnums.GameFlow.getByValue(gameState)) {
//            case MATCHMAKING -> log.info("开始匹配");
            case READY_CHECK -> this.acceptGame();
            case CHAMPION_SELECT -> new Thread(this::championSelectStart).start();
            case IN_PROGRESS -> new Thread(this::calcEnemyTeamScore).start();
//            case END_OF_GAME -> new Thread(this::autoStartNextGame).start();
            case LOBBY -> AudioPlayer.inputLobby();

        }
    }

    /**
     * 自动开启下一场对局
     */
    @SneakyThrows
    private void autoStartNextGame() {
        Thread.sleep(1500);
        boolean result = lcuApiService.playAgain();
        // TODO 扩展，检查自己是不是房主
        if (result) {
            Thread.sleep(1500);
            lcuApiService.autoStartMatch();
        }
    }

    @SneakyThrows
    private void acceptGame() {
        Thread.sleep(1500);
        AudioPlayer.findGame();
        lcuApiService.acceptGame();
    }

    /**
     * 查询队友战绩
     */
    @SneakyThrows
    public void championSelectStart() {
//        if (!GameEnums.GameQueueID.isNormalGameMode(CustomGameSession.getInstance().getQueueId())) {
//            // 不存在选人界面的模式，直接返回
//            return;
//        }
        AudioPlayer.championSelectStart();

        Thread.sleep(1500);
        List<Long> summonerIdList = fetchTeamSummonerIds();
        if (CollectionUtils.isEmpty(summonerIdList)) {
            log.error("队友召唤师ID查询失败！");
            return;
        }

        if (CustomGameSession.isSoloRank() && summonerIdList.size() < 5) {
            log.error("队伍人数不为5，size：{}:", summonerIdList.size());
        }

        // 不计算本人
        //summonerIdList.remove(Summoner.getInstance().getSummonerId());

        // 获取队友mate信息
        List<Summoner> summonerList = lcuApiService.listSummoner(summonerIdList);
        if (CollectionUtils.isEmpty(summonerList)) {
            log.info("查询召唤师信息失败, summonerList为空！ ");
            return;
        }

        // 分析战绩并打印
        calculateScore(summonerList, true);
    }


    /**
     * 尝试获取当前团队中的召唤师ID列表（最多尝试3次）
     *
     * @return 召唤师ID列表
     */
    private List<Long> fetchTeamSummonerIds() throws InterruptedException {
        List<Long> summonerIdList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            TimeUnit.SECONDS.sleep(1);
            summonerIdList = getTeamSummonerIdList();
            if (CollectionUtils.isNotEmpty(summonerIdList) && summonerIdList.size() == 5) {
                break;
            }
        }
        return summonerIdList;
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
        // 打印日志
        //summonerList.stream()
        //        .map(summoner -> calculateUserScore(summoner, isSelf))
        //        .filter(Objects::nonNull) // 过滤无效值
        //        .sorted(Comparator.comparingDouble(UserScore::getScore).reversed()) // 先按照分数排序
        //        .forEach(scoreInfo -> {
        //            double score = scoreInfo.getScore();
        //            String msg = String.format(SCORE_RESULT,
        //                    findHorseName(score, horseArr),  // 马匹信息
        //                    (int) score, // 分数
        //                    rankData(scoreInfo.getPuuid()), // 段位
        //                    scoreInfo.getSummonerName(), // 召唤师名称
        //                    formatKDAInfo(scoreInfo.getCurrKDA(), 5));  // 前几局KDA
        //            log.info("{}\n{}", msg, scoreInfo.getExtMsg());
        //        });

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

                    List<String> collect = currKDA.stream().limit(5).map(kda -> String.format(KDA_FORMAT,
                            kda.getQueueGame(),
                            kda.getWin() ? Constant.WIN_STR : Constant.LOSE_STR,
                            "http://game.gtimg.cn/images/lol/act/img/champion/" + NewHeros.getAliasById(kda.getChampionId()) + ".png",
                            kda.getKills(),
                            kda.getDeaths(),
                            kda.getAssists())).toList();
                    item.setCurrKDA(collect);
                    if (isSelf) {
                        CustomGameCache.getInstance().getTeamList().add(item);
                    } else {
                        CustomGameCache.getInstance().getEnemyList().add(item);
                    }
                });
        if (isSelf) {
            log.info("【信息缓存成功】我方：{}", CustomGameCache.getInstance().getTeamList());
        } else {
            log.info("【信息缓存成功】敌方：{}", CustomGameCache.getInstance().getEnemyList());
        }
    }


    private UserScore calculateUserScore(Summoner summoner, boolean isSelf) {
        try {
            Thread.sleep(200); // 延迟避免请求过载

            long summonerID = summoner.getSummonerId();
            UserScore userScoreInfo = new UserScore(summonerID, defaultScore); // 创建用户评分对象，默认分数
            userScoreInfo.setSummonerName(String.format("%s#%s", summoner.getGameName(), summoner.getTagLine()));
            userScoreInfo.setPuuid(summoner.getPuuid()); // 设置用户唯一标识

            List<GameHistory.GameInfo> gameList;
            try {
                gameList = lcuApiService.listGameHistory(summoner, 0, 19); // 获取最近20场游戏记录
                // 过滤指定的对局模式
                gameList = gameList.stream().filter(game -> GameEnums.GameQueueID.isNormalGameMode(game.getQueueId())).toList();
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
        // 近期游戏分数
        List<Double> currTimeScores = new ArrayList<>(validScores.size());
        // 其他时段游戏分数
        List<Double> otherTimeScores = new ArrayList<>(validScores.size());

        double totalScore = 0;
        int totalGameCount = validScores.size(); // 直接使用有效游戏数
        for (AbstractMap.SimpleEntry<Double, LocalDateTime> entry : validScores) {
            double score = entry.getKey();
            totalScore += score;
            if (nowTime.isBefore(entry.getValue().plusHours(24))) { // 24小时内游戏为当前时段
                currTimeScores.add(score);
            } else {
                otherTimeScores.add(score);
            }
        }

        // 计算加权分数（若有效游戏数为0则使用默认分）
        return totalGameCount > 0 ?
                calculateWeightedScore(currTimeScores, otherTimeScores, totalGameCount, totalScore) : defaultScore;
    }

    /**
     * 分析每一场游戏的KDA和使用英雄
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
            kda.setPosition(guessPosition(participant.getTimeline().getLane(), participant.getTimeline().getRole()));
            return kda;
        }).toList();
    }

    public static String guessPosition(String lane, String role) {
        if ("JUNGLE".equalsIgnoreCase(lane)) {
            return "打野";
        } else if ("TOP".equalsIgnoreCase(lane)) {
            return "上单";
        } else if ("MIDDLE".equalsIgnoreCase(lane) || "MID".equalsIgnoreCase(lane)) {
            return "中单";
        } else if ("BOTTOM".equalsIgnoreCase(lane)) {
            if ("CARRY".equalsIgnoreCase(role)) {
                return "ADC";
            } else if ("SUPPORT".equalsIgnoreCase(role)) {
                return "辅助";
            }
        }
        return "未知";
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
     * 格式化对局kda详情
     */
    public String formatKDAInfo(List<UserScore.Kda> kdaList, int limit) {
        return kdaList.stream()
                .limit(limit)
                .map(kda -> String.format(KDA_FORMAT,
                        kda.getQueueGame(),
                        kda.getWin() ? Constant.WIN_STR : Constant.LOSE_STR,
                        kda.getChampionName(),
                        kda.getKills(),
                        kda.getDeaths(),
                        kda.getAssists()))
                .collect(Collectors.joining())
                .trim();
    }


    /**
     * 计算加权后的总得分。
     *
     * @param currTimeScoreList  当前时间内的得分列表
     * @param otherGameScoreList 其他游戏的得分列表
     * @param totalGameCount     总的游戏场次数量
     * @param totalScore         所有游戏的总得分
     * @return 加权后的总得分
     */
    private double calculateWeightedScore(List<Double> currTimeScoreList, List<Double> otherGameScoreList, int totalGameCount, double totalScore) {
        // 计算当前时间内所有得分之和
        double totalTimeScore = currTimeScoreList.stream().mapToDouble(Double::doubleValue).sum();
        // 计算其他游戏中所有得分之和
        double totalOtherGameScore = otherGameScoreList.stream().mapToDouble(Double::doubleValue).sum();

        // 如果游戏场次大于零，计算平均得分；否则设为0
        double totalGameAvgScore = totalGameCount > 0 ? totalScore / totalGameCount : 0.0;

        // 初始化加权总分
        double weightTotalScore = 0.0;
        // 计算当前时间内得分的平均值；如果列表为空，则设为0
        double avgTimeScore = !currTimeScoreList.isEmpty() ? totalTimeScore / currTimeScoreList.size() : 0;
        // 计算其他游戏中得分的平均值；如果列表为空，则设为0
        double avgOtherGameScore = !otherGameScoreList.isEmpty() ? totalOtherGameScore / otherGameScoreList.size() : 0;

        // 将当前时间和其他游戏中的得分按比例加入到加权总分中
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


}
