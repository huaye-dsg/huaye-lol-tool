package com.example.huayeloltool.service;

import com.example.huayeloltool.cache.UserScoreCache;
import com.example.huayeloltool.config.OkHttpUtil;
import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.enums.Heros;
import com.example.huayeloltool.model.*;
import com.example.huayeloltool.model.Conversation.ConversationMsg;
import com.example.huayeloltool.model.base.CalcScoreConf;
import com.example.huayeloltool.model.base.GamePlaySetting;
import com.example.huayeloltool.model.base.HorseScoreConf;
import com.example.huayeloltool.model.game.*;
import com.example.huayeloltool.model.rankinfo.RankedInfo;
import com.example.huayeloltool.model.score.GameScoreCalculator1;
import com.example.huayeloltool.model.score.ScoreService;
import com.example.huayeloltool.model.score.ScoreWithReason;
import com.example.huayeloltool.model.score.UserScore;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.example.huayeloltool.enums.GameEnums.GameFlow.IN_PROGRESS;


@Service
@Slf4j
public class GameStateUpdateService extends CommonRequest {

    private final GamePlaySetting clientCfg = GamePlaySetting.getInstance();

    @Resource
    private LcuApiService lcuApiService;
    @Resource
    private ScoreService scoreService;
    @Resource
    private GameScoreCalculator1 gameScoreCalculator1;

    private static final int MAX_KDA_DISPLAY = 5;
    private static final int SLEEP_TIME = 200;
    private static final String WIN_STR = "胜";
    private static final String LOSE_STR = "败";
    private static final String KDA_FORMAT = "[%s-%s]%d/%d/%d   ";

    public GameStateUpdateService(OkHttpClient client) {
        super(client);
    }


    @SneakyThrows
    public void onGameFlowUpdate(String gameState) {
//        log.info("切换状态：{}", gameState);

        GameEnums.GameFlow gameFlow = GameEnums.GameFlow.getByValue(gameState);

        switch (gameFlow) {
            case MATCHMAKING:
//                log.info("匹配中........");
                break;
            case READY_CHECK:
                lcuApiService.acceptGame();
                break;
            case CHAMPION_SELECT:
                log.info("进入英雄选择阶段, 正在计算我方分数");
                new Thread(this::championSelectStart).start();
                break;
            case IN_PROGRESS:
                log.info("游戏进行中, 正在计算敌方队伍分数");
                new Thread(this::calcEnemyTeamScore).start();
                break;
            case NONE:
                // 初始化数据
                CustomGameSessionDetails.getInstance().init();
            default:
                break;
        }
    }


    /**
     * 查询队友战绩
     */
    @SneakyThrows
    public void championSelectStart() {
        //if (!CustomGameSessionDetails.isSoloRank()) {
        //    log.info("当前不是排位，不计算队友战绩信息");
        //}

        Thread.sleep(1500);

        List<Long> summonerIdList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            TimeUnit.SECONDS.sleep(1);
            // 获取队伍所有用户信息
            summonerIdList = getTeamSummonerIdList();
            if (CollectionUtils.isEmpty(summonerIdList)) {
                log.error("summonerIdList 为空，继续下一次循环");
                continue;
            }
            if (summonerIdList.size() == 5) {
                break;
            }
        }
        if (CollectionUtils.isEmpty(summonerIdList)) {
            log.error("队友召唤师ID查询失败！");
            return;
        }

        if (CustomGameSessionDetails.isSoloRank() && summonerIdList.size() < 5) {
            log.error("队伍人数不为5，size：{}:", summonerIdList.size());
        }


        // 获取队友mate信息
        List<Summoner> summonerList = lcuApiService.listSummoner(summonerIdList);
        if (CollectionUtils.isEmpty(summonerList)) {
            log.info("查询召唤师信息失败, summonerList为空！ ");
            return;
        }

        // 分析战绩并打印
        calcScore(summonerList, true);
    }

    /**
     * 计算敌方队伍分数
     */
    public void calcEnemyTeamScore() {
        try {
            GameFlowSession session = queryGameFlowSession();
            if (session == null || !session.getPhase().equals(IN_PROGRESS)) {
                return;
            }

            Summoner summoner = Summoner.getInstance();
            if (summoner == null) {
                return;
            }

            long selfID = summoner.getSummonerId();
            Pair<List<Long>, List<Long>> allUsersFromSession = getAllUsersFromSession(selfID, session);
            List<Long> enemySummonerIDList = new ArrayList<>(allUsersFromSession.getRight());
            if (CollectionUtils.isEmpty(enemySummonerIDList)) {
                log.error("敌方用户ID为空");
                return;
            }

            // 查询分析敌方用户的信息并计算得分
            List<Summoner> summonerList = lcuApiService.listSummoner(enemySummonerIDList);
            if (CollectionUtils.isEmpty(summonerList)) {
                log.error("查询召唤师信息失败: {}", enemySummonerIDList);
                return;
            }

            calcScore(summonerList, false);

        } catch (Exception e) {
            log.error("计算敌方队伍得分时发生错误", e);
        }
    }


    public Pair<List<Long>, List<Long>> getAllUsersFromSession(long selfID, GameFlowSession session) {
        List<Long> selfTeamUsers = new ArrayList<>(5);
        List<Long> enemyTeamUsers = new ArrayList<>(5);

        GameEnums.TeamID selfTeamID = GameEnums.TeamID.NONE;

        // 检查 TeamOne
        for (GameFlowSession.GameFlowSessionTeamUser teamUser : session.getGameData().getTeamOne()) {
            long summonerID = teamUser.getSummonerId();
            if (selfID == summonerID) {
                selfTeamID = GameEnums.TeamID.BLUE;
                break;
            }
        }

        // 如果 TeamOne 中没有找到，则检查 TeamTwo
        if (selfTeamID == GameEnums.TeamID.NONE) {
            for (GameFlowSession.GameFlowSessionTeamUser teamUser : session.getGameData().getTeamTwo()) {
                long summonerID = teamUser.getSummonerId();
                if (selfID == summonerID) {
                    selfTeamID = GameEnums.TeamID.RED;
                    break;
                }
            }
        }

        if (selfTeamID == GameEnums.TeamID.NONE) {
            log.error("无法分辨是蓝色方还是红色方！");
            return Pair.of(selfTeamUsers, enemyTeamUsers);
        }

        if (selfTeamID == GameEnums.TeamID.BLUE) {
            fillSummonerIds(session, selfTeamUsers, enemyTeamUsers);
        } else {
            fillSummonerIds(session, enemyTeamUsers, selfTeamUsers);
        }
        return Pair.of(selfTeamUsers, enemyTeamUsers);
    }

    private void fillSummonerIds(GameFlowSession session, List<Long> teamUsers1, List<Long> teamUsers2) {
        for (GameFlowSession.GameFlowSessionTeamUser user : session.getGameData().getTeamOne()) {
            long userID = user.getSummonerId();
            if (userID > 0) {
                teamUsers1.add(userID);
            }
        }
        for (GameFlowSession.GameFlowSessionTeamUser user : session.getGameData().getTeamTwo()) {
            long userID = user.getSummonerId();
            if (userID > 0) {
                teamUsers2.add(userID);
            }
        }
    }

    public GameFlowSession queryGameFlowSession() throws IOException {
        Request request = OkHttpUtil.createOkHttpGetRequest("/lol-gameflow/v1/session");
        return sendRequest(request, GameFlowSession.class);
    }


    private String rankData(String puuid) {
        try {
            RankedInfo rankData = lcuApiService.getRankData(puuid);
            RankedInfo.HighestRankedEntrySRDto highestRankedEntrySR = rankData.getHighestRankedEntrySR();
            String tier = highestRankedEntrySR.getTier();
            String division = highestRankedEntrySR.getDivision();
            Integer leaguePoints = highestRankedEntrySR.getLeaguePoints();
            return String.format("【%s-%s-%d】", GameEnums.RankTier.getRankNameMap(tier), division, leaguePoints);
        } catch (Exception e) {
            log.error("查询{}战绩失败！", puuid, e);
        }
        return "";
    }

    private void calcScore(List<Summoner> summonerList, Boolean isSelf) {
        if (CollectionUtils.isEmpty(summonerList)) {
            log.warn("召唤师列表为空");
            return;
        }

        HorseScoreConf[] horseArr = CalcScoreConf.getInstance().getHorse();
        String[] horseNames = clientCfg.getHorseNameConf();

        List<UserScoreCache.ScoreOverview> scoreOverviews = summonerList.parallelStream()
                .map(summoner -> {
                    // 拿到分数
                    UserScore scoreInfo = calculateUserScore(summoner, isSelf);
                    if (scoreInfo == null) return null;

                    double score = scoreInfo.getScore();
                    String horseName = findHorseName(score, horseArr, horseNames);
                    String currKDAMsg = formatKDAInfo(scoreInfo);

                    UserScoreCache.ScoreOverview overview = new UserScoreCache.ScoreOverview();
                    overview.setSummonerName(scoreInfo.getSummonerName());
                    overview.setHouseName(horseName);
                    overview.setScore((int) score);
                    overview.setGameDetail(currKDAMsg);
                    overview.setPuuid(scoreInfo.getPuuid());
                    overview.setExtMag(scoreInfo.getExtMsg());
                    return overview;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(UserScoreCache.ScoreOverview::getScore).reversed())
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(scoreOverviews)) {
            log.error("计算用户得分失败, userScores为空");
            return;
        }

        for (UserScoreCache.ScoreOverview scoreOverview : scoreOverviews) {
            String msg = String.format("【%s】【%d分】%s: %s %s ",
                    scoreOverview.getHouseName(), scoreOverview.getScore(), rankData(scoreOverview.getPuuid()),
                    scoreOverview.getSummonerName(), scoreOverview.getGameDetail());
            log.info("{}\n{}", msg, scoreOverview.getExtMag());
        }
    }


    private UserScore calculateUserScore(Summoner summoner, Boolean isSelf) {
        try {
            Thread.sleep(SLEEP_TIME); // 延迟避免请求过载

            long summonerID = summoner.getSummonerId();
            Double defaultScore = 100.0;
            UserScore userScoreInfo = new UserScore(summonerID, defaultScore); // 创建用户评分对象，默认分数
            userScoreInfo.setSummonerName(String.format("%s#%s", summoner.getGameName(), summoner.getTagLine()));
            userScoreInfo.setPuuid(summoner.getPuuid()); // 设置用户唯一标识

            List<GameHistory.GameInfo> gameList;
            try {
                gameList = lcuApiService.listGameHistory(summoner, 0, 19); // 获取最近20场游戏记录
            } catch (Exception e) {
                log.error("获取游戏战绩列表失败", e);
                return null; // 返回默认对象（包含默认分）
            }

            if (CollectionUtils.isEmpty(gameList)) {
                log.error("用户战绩查询为空！召唤师： {}", summoner.getGameName());
                return null;
            }

            try {
                userScoreInfo.setExtMsg(GameAnalysis.analyzeGameHistory(gameList, summoner.getGameName(), isSelf));
            } catch (Exception e) {
                log.error("分析连败/近期排位情况失败", e);
            }
            // 改用线程安全的Map存储得分和时间，避免存储完整GameSummary对象
            List<CompletableFuture<AbstractMap.SimpleEntry<Double, LocalDateTime>>> futures = gameList.stream()
                    .map(info -> CompletableFuture.supplyAsync(() -> {
                        try {
                            GameSummary gameSummary = queryGameSummaryWithRetry(info.getGameId());
                            ScoreWithReason score = scoreService.calcUserGameScore(summonerID, gameSummary);

//                            ScoreWithReason scoreWithReason = gameScoreCalculator1.calcUserGameScore(summonerID, gameSummary);

                            return new AbstractMap.SimpleEntry<>(score.getScore(), gameSummary.getGameCreationDate());
                        } catch (Exception e) {
                            log.error("获取或计算游戏数据失败", e);
                            return null; // 异常情况返回null，后续过滤
                        }
                    })).collect(Collectors.toList());

            // 合并所有结果并过滤无效值
            List<AbstractMap.SimpleEntry<Double, LocalDateTime>> validScores = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 计算加权总分
            LocalDateTime nowTime = LocalDateTime.now();
            List<Double> currTimeScores = new ArrayList<>(validScores.size()); // 预分配容量
            List<Double> otherTimeScores = new ArrayList<>(validScores.size());

            double totalScore = 0;
            int totalGameCount = validScores.size(); // 直接使用有效游戏数
            for (AbstractMap.SimpleEntry<Double, LocalDateTime> entry : validScores) {
                double score = entry.getKey();
                LocalDateTime gameTime = entry.getValue();
                totalScore += score;

                if (nowTime.isBefore(gameTime.plusHours(5))) { // 5小时内游戏为当前时段
                    currTimeScores.add(score);
                } else {
                    otherTimeScores.add(score);
                }
            }

            // 计算加权分数（若有效游戏数为0则使用默认分）
            double weightTotalScore = totalGameCount > 0 ?
                    calculateWeightedScore(currTimeScores, otherTimeScores, totalGameCount, totalScore) : defaultScore;

            // 处理KDA数据：分析每一场游戏的KDA，这里发现 participant列表中只有一个元素
            List<UserScore.Kda> kdaList = new ArrayList<>(gameList.size()); // 预分配容量
            for (GameHistory.GameInfo gameInfo : gameList) {
                Participant participant = gameInfo.getParticipants().get(0);
                Participant.Stats stats = participant.getStats();
                UserScore.Kda kda = new UserScore.Kda();
                kda.setKills(stats.getKills());
                kda.setDeaths(stats.getDeaths());
                kda.setAssists(stats.getAssists());
                kda.setWin(stats.getWin());
                kda.setChampionId(participant.getChampionId());
                kda.setChampionName(Heros.getNameById(participant.getChampionId()));
                kdaList.add(kda);
            }

            userScoreInfo.setCurrKDA(kdaList);
            userScoreInfo.setScore(weightTotalScore);
            return userScoreInfo;
        } catch (Exception e) {
            log.error("计算用户得分失败, summoner: {}", summoner.getGameName(), e);
            return null; // 顶层异常返回null（上层需处理）
        }
    }


    private String findHorseName(double score, HorseScoreConf[] horseArr, String[] horseNames) {
        for (int i = 0; i < horseArr.length; i++) {
            if (score >= horseArr[i].getScore()) {
                return horseNames[i];
            }
        }
        return "";
    }

    private String formatKDAInfo(UserScore scoreInfo) {
        return scoreInfo.getCurrKDA().stream()
                .limit(MAX_KDA_DISPLAY)
                .map(kda -> String.format(KDA_FORMAT,
                        kda.getWin() ? WIN_STR : LOSE_STR,
                        kda.getChampionName(),
                        kda.getKills(),
                        kda.getDeaths(),
                        kda.getAssists()))
                .collect(Collectors.joining())
                .trim();
    }

    private static final int MAX_LOSING_STREAK = 3; // 最大连跪场次
    private static final int RECENT_GAMES_COUNT = 5; // 近期比赛场次
    private static final int MIN_RANKED_GAMES = 3; // 最少排位场次


    // 模拟的算法: 计算带权重的总得分
    private double calculateWeightedScore(List<Double> currTimeScoreList, List<Double> otherGameScoreList, int totalGameCount, double totalScore) {
        double totalTimeScore = currTimeScoreList.stream().mapToDouble(Double::doubleValue).sum();
        double totalOtherGameScore = otherGameScoreList.stream().mapToDouble(Double::doubleValue).sum();

        double totalGameAvgScore = totalGameCount > 0 ? totalScore / totalGameCount : 0.0;

        double weightTotalScore = 0.0;
        double avgTimeScore = !currTimeScoreList.isEmpty() ? totalTimeScore / currTimeScoreList.size() : 0;
        double avgOtherGameScore = !otherGameScoreList.isEmpty() ? totalOtherGameScore / otherGameScoreList.size() : 0;

        weightTotalScore += !currTimeScoreList.isEmpty() ? 0.8 * avgTimeScore : 0.8 * totalGameAvgScore;
        weightTotalScore += !otherGameScoreList.isEmpty() ? 0.2 * avgOtherGameScore : 0.2 * totalGameAvgScore;

        return weightTotalScore;
    }

    // 伪代码: 有重试机制的查询游戏详情方法
    private GameSummary queryGameSummaryWithRetry(long gameId) throws Exception {
        int attempts = 5;
        int delay = 10; // 毫秒
        Exception lastException = null;

        for (int i = 0; i < attempts; i++) {
            try {
                return lcuApiService.queryGameSummary(gameId);
            } catch (Exception e) {
                lastException = e;
                Thread.sleep(delay);
            }
        }
        throw lastException;
    }


    /**
     * 从会话消息列表中获取召唤师ID列表
     */
    public List<Long> getTeamSummonerIdList() {
        String conversationID = lcuApiService.getCurrConversationID();
        if (conversationID == null || conversationID.isEmpty()) {
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


}
