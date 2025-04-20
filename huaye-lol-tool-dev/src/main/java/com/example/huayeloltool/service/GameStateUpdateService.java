package com.example.huayeloltool.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.example.huayeloltool.cache.UserScoreCache;
import com.example.huayeloltool.config.OkHttpUtil;
import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.enums.Heros;
import com.example.huayeloltool.model.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.example.huayeloltool.enums.GameEnums.GameFlow.CHAMPION_SELECT;
import static com.example.huayeloltool.enums.GameEnums.GameFlow.IN_PROGRESS;


@Service
@Slf4j
public class GameStateUpdateService extends CommonRequest {


    private String state;


    private DefaultClientConf clientCfg = DefaultClientConf.getInstance();

    @Autowired
    @Qualifier(value = "unsafeOkHttpClient")
    private OkHttpClient client;

    @Autowired
    private LcuService lcuService;
    @Autowired
    private ScoreService scoreService;

    private static final int MAX_KDA_DISPLAY = 5;
    private static final int SLEEP_TIME = 200;
    private static final String WIN_STR = "胜";
    private static final String LOSE_STR = "败";
    private static final String KDA_FORMAT = "[%s-%s]%d/%d/%d   ";
    private Double defaultScore = 100.0;

    @SneakyThrows
    public void onGameFlowUpdate(String gameState) {
//        log.info("切换状态：{}", gameState);

        GameEnums.GameFlow gameFlow = GameEnums.GameFlow.getByValue(gameState);

        switch (gameFlow) {
            case MATCHMAKING:
//                log.info("匹配中........");
                break;
            case READY_CHECK:
//                log.info("等待接受对局");
                lcuService.acceptGame();
                break;
            case CHAMPION_SELECT:
//                if (SelfGameSession.getQueueId() == GameEnums.GameQueueID.RANK_SOLO.getId()) {
                log.info("进入英雄选择阶段, 正在计算我方分数");
                new Thread(this::championSelectStart).start();
//                }
                break;
            case IN_PROGRESS:
//                if (SelfGameSession.getQueueId() == GameEnums.GameQueueID.RANK_SOLO.getId()) {
                log.info("游戏进行中, 正在计算敌方队伍分数");
                new Thread(this::calcEnemyTeamScore).start();
//                }
                break;
            case NONE:
                // 初始化数据
                SelfGameSession.init();
//                log.info("初始化数据：{}", JSON.toJSONString(SelfGameSession.getInstance()));
            default:
//                log.info("忽略状态：{}", gameState);
                break;
        }
    }


    /**
     * 查询队友战绩
     */
    @SneakyThrows
    public void championSelectStart() {
//        if (!SelfGameSession.isSoloRank()) {
//            log.info("当前不是排位，不计算队友战绩信息");
//        }

        Thread.sleep(1500);

        List<Long> summonerIDList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            TimeUnit.SECONDS.sleep(1);
            // 获取队伍所有用户信息
            TeamUsersInfo teamUsersInfo = getTeamUsers();
            if (Objects.isNull(teamUsersInfo)) {
                log.error("teamUsersInfo 为null，继续下一次循环");
                continue;
            }
            // 拿到SummonerId
            summonerIDList = teamUsersInfo.getSummonerIdList();
            if (summonerIDList.size() == 5) {
                break;
            }

        }

        if (SelfGameSession.isSoloRank()) {
            if (summonerIDList.size() != 5) {
                log.error("队伍人数不为5，size：{}:", summonerIDList.size());
            }
        }

        if (summonerIDList.isEmpty()) {
            log.info("summonerIDList is empty");
            return;
        }

        log.info("队伍人员列表:{}", summonerIDList);

        // 查询所有用户的信息并计算得分
        List<CurrSummoner> summonerList = listSummoner(summonerIDList);
        if (CollectionUtils.isEmpty(summonerList)) {
            log.info("查询召唤师信息失败, summonerList为空！ ");
            return;
        }

        // 战绩信息
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

            CurrSummoner currSummoner = CurrSummoner.getInstance();
            if (currSummoner == null) {
                return;
            }

            long selfID = currSummoner.getSummonerId();
            Pair<List<Long>, List<Long>> allUsersFromSession = getAllUsersFromSession(selfID, session);
            List<Long> enemySummonerIDList = new ArrayList<>(allUsersFromSession.getRight());
//            log.info("最终解析的敌方SummonerId为：{}", enemySummonerIDList);
            if (CollectionUtils.isEmpty(enemySummonerIDList)) {
                log.error("敌方用户ID为空");
                return;
            }

            // 查询所有用户的信息并计算得分
            List<CurrSummoner> summonerList = listSummoner(enemySummonerIDList);
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
//        log.info("getAllUsersFromSession selfID : {}", selfID);
        List<Long> selfTeamUsers = new ArrayList<>(5);
        List<Long> enemyTeamUsers = new ArrayList<>(5);

        GameEnums.TeamID selfTeamID = GameEnums.TeamID.NONE;

        // 检查 TeamOne
        for (GameFolwSessionTeamUser teamUser : session.getGameData().getTeamOne()) {
            long summonerID = teamUser.getSummonerId();
            if (selfID == summonerID) {
//                log.info("检测到当前属于蓝色方！您的位置为：{}。selfID： {}", GameEnums.Position.getDescByValue(teamUser.getSelectedPosition()), summonerID);
                selfTeamID = GameEnums.TeamID.BLUE;
                break;
            }
        }

        // 如果 TeamOne 中没有找到，则检查 TeamTwo
        if (selfTeamID == GameEnums.TeamID.NONE) {
            for (GameFolwSessionTeamUser teamUser : session.getGameData().getTeamTwo()) {
                long summonerID = teamUser.getSummonerId();
                if (selfID == summonerID) {
//                    log.info("检测到当前属于蓝色方！您的位置为：{}。selfID： {}", GameEnums.Position.getDescByValue(teamUser.getSelectedPosition()), summonerID);
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
            fillTeamSummonerIds(session, selfTeamUsers, enemyTeamUsers);
        } else {
            fillTeamSummonerIds(session, enemyTeamUsers, selfTeamUsers);
        }
        return Pair.of(selfTeamUsers, enemyTeamUsers);
    }

    private void fillTeamSummonerIds(GameFlowSession session, List<Long> teamUsers1, List<Long> teamUsers2) {
        for (GameFolwSessionTeamUser user : session.getGameData().getTeamOne()) {
            long userID = user.getSummonerId();
            if (userID > 0) {
//                log.info("添加SummonerId： {}", userID);
                teamUsers1.add(userID);
            } else {
                log.error("发现SummonerId异常：{}", userID);
            }
        }
        for (GameFolwSessionTeamUser user : session.getGameData().getTeamTwo()) {
            long userID = user.getSummonerId();
            if (userID > 0) {
//                log.info("添加SummonerId： {}", userID);
                teamUsers2.add(userID);
            } else {
                log.error("发现SummonerId异常：{}", userID);
            }
        }
    }

    public GameFlowSession queryGameFlowSession() throws IOException {
        Request request = OkHttpUtil.createOkHttpGetRequest("/lol-gameflow/v1/session");
        return sendRequest(request, GameFlowSession.class);
    }


    private String rankData(String puuid) {
        try {
            RankedInfo rankData = lcuService.getRankData(puuid);
            RankedInfo.HighestRankedEntrySRDto highestRankedEntrySR = rankData.getHighestRankedEntrySR();
            String tier = highestRankedEntrySR.getTier();
            String division = highestRankedEntrySR.getDivision();
            Integer leaguePoints = highestRankedEntrySR.getLeaguePoints();
            String rankName = GameEnums.RankTier.getRankNameMap(tier);
            return String.format("【%s-%s-%d】", rankName, division, leaguePoints);
        } catch (Exception e) {
            log.error("查询{}战绩失败！", puuid, e);
        }
        return "";
    }


    private void calcScore(List<CurrSummoner> summonerList, Boolean isSelf) {
        if (CollectionUtils.isEmpty(summonerList)) {
            log.warn("召唤师列表为空");
            return;
        }

        List<UserScore> userScores = summonerList.parallelStream()
                .map(this::calculateUserScore)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(userScores)) {
            log.error("计算用户得分失败, userScores为空");
            return;
        }

        // 分数从高到低排序
        userScores.sort(Comparator.comparingDouble(UserScore::getScore).reversed());

        HorseScoreConf[] horseArr = CalcScoreConf.getInstance().getHorse();
        String[] horseNames = clientCfg.getHorseNameConf();

        userScores.forEach(scoreInfo -> {
            String horseName = findHorseName(scoreInfo.getScore(), horseArr, horseNames);
            String currKDAMsg = formatKDAInfo(scoreInfo);
            String msg = String.format("【%s】【%d分】%s: %s %s ",
                    horseName,
                    scoreInfo.getScore().intValue(),
                    rankData(scoreInfo.getPuuid()),
                    scoreInfo.getSummonerName(),
                    currKDAMsg
            );
            UserScoreCache.ScoreOverview userScoreCache = new UserScoreCache.ScoreOverview();
            userScoreCache.setSummonerName(scoreInfo.getSummonerName());
            userScoreCache.setHouseName(horseName);
            userScoreCache.setScore(scoreInfo.getScore().intValue());
            userScoreCache.setGameDetail(currKDAMsg);
            if (isSelf) {
                UserScoreCache.addSelfTeamScore(userScoreCache);
            } else {
                UserScoreCache.addEnemyTeamScore(userScoreCache);
            }
            log.info("msg: {}", msg);
        });
    }

    private UserScore calculateUserScore(CurrSummoner summoner) {
        try {
            Thread.sleep(SLEEP_TIME);
            return getUserScore(summoner);
        } catch (Exception e) {
            log.error("计算用户得分失败, summoner: {}", summoner.getGameName(), e);
            return null;
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

    // 获取用户得分
    public UserScore getUserScore(CurrSummoner summoner) {
        long summonerID = summoner.getSummonerId();
        UserScore userScoreInfo = new UserScore(summonerID, defaultScore);
        userScoreInfo.setSummonerName(String.format("%s#%s", summoner.getGameName(), summoner.getTagLine()));
        userScoreInfo.setPuuid(summoner.getPuuid());
        // 获取战绩列表
        List<GameInfo> gameList;
        try {
            gameList = lcuService.listGameHistory(summoner, 0, 19);
        } catch (Exception e) {
            log.error("获取游戏战绩列表失败", e);
            return userScoreInfo;
        }

        if (CollectionUtils.isEmpty(gameList)) {
//            log.error("用户战绩查询为空！召唤师： {}", summoner.getGameName());
            return null;
        }


        // 临时分析是不是连败或者近期没打过排位
        analyzeGameHistory(gameList, summoner.getGameName());

        List<GameSummary> gameSummaryList = Collections.synchronizedList(new ArrayList<>());

        CompletableFuture.allOf(gameList.stream().map(info -> CompletableFuture.runAsync(() -> {
            try {
                GameSummary gameSummary = queryGameSummaryWithRetry(info.getGameId());
                gameSummaryList.add(gameSummary);
            } catch (Exception e) {
                log.error("获取游戏对局详细信息失败", e);
            }
        })).toArray(CompletableFuture[]::new)).join();


        double totalScore = 0;
        int totalGameCount = 0;
        LocalDateTime nowTime = LocalDateTime.now();
        List<Double> currTimeScoreList = new ArrayList<>();
        List<Double> otherGameScoreList = new ArrayList<>();

        for (GameSummary gameSummary : gameSummaryList) {
            try {
                ScoreWithReason scoreWithReason = scoreService.calcUserGameScore(summonerID, gameSummary);
                double scoreValue = scoreWithReason.getScore();
                totalGameCount++;
                totalScore += scoreValue;

                if (nowTime.isBefore(gameSummary.getGameCreationDate().plusHours(5))) {
                    currTimeScoreList.add(scoreValue);
                } else {
                    otherGameScoreList.add(scoreValue);
                }
            } catch (Exception e) {
                log.error("游戏战绩计算用户得分失败", e);
            }
        }

        double weightTotalScore = calculateWeightedScore(currTimeScoreList, otherGameScoreList, totalGameCount, totalScore);

        if (gameSummaryList.isEmpty()) {
            weightTotalScore = defaultScore;
        }

        List<UserScore.Kda> kdaList = new ArrayList<>();
        for (GameInfo gameInfo : gameList) {
            // 这里一定要用gameInfo去判断，不能用gameSummaryList
            List<GameInfo.Participant> participants = gameInfo.getParticipants();
            for (GameInfo.Participant participant : participants) {
                GameInfo.Stats stats = participant.getStats();
                UserScore.Kda kda = new UserScore.Kda();
                kda.setKills(stats.getKills());
                kda.setDeaths(stats.getDeaths());
                kda.setAssists(stats.getAssists());
                kda.setWin(stats.getWin());
                kda.setChampionId(participant.getChampionId());
                kda.setChampionName(Heros.getNameById(participant.getChampionId()));
                kdaList.add(kda);
            }
        }

        userScoreInfo.setCurrKDA(kdaList);
        userScoreInfo.setScore(weightTotalScore);

        return userScoreInfo;
    }


    private static final int MAX_LOSING_STREAK = 3; // 最大连跪场次
    private static final int RECENT_GAMES_COUNT = 5; // 近期比赛场次
    private static final int MIN_RANKED_GAMES = 3; // 最少排位场次

    public static void analyzeGameHistory(List<GameInfo> gameInfoList, String gameName) {
        int losingStreak = 0;
        int recentRankedGames = 0;

        // 倒序遍历，从最近的比赛开始
        for (int i = 0; i < gameInfoList.size(); i++) {
            GameInfo gameInfo = gameInfoList.get(i);

            // 判断是否是排位赛
            boolean isRankedGame = (gameInfo.getQueueId() == GameEnums.GameQueueID.RANK_SOLO.getId());

            if (isRankedGame) {
                recentRankedGames++; // 累加排位赛场次
                GameInfo.Stats stats = gameInfo.getParticipants().get(0).getStats();
                // 判断是否输了
                if (!stats.getWin()) {
                    losingStreak++; // 连跪场次+1
                } else {
                    losingStreak = 0; // 不是连跪，重置连跪计数
                }
            }

            // 只检查最近的几场比赛
            if (i >= RECENT_GAMES_COUNT - 1) {
                break; // 退出循环
            }
        }

        // 判断是否达到连跪阈值
        if (losingStreak >= MAX_LOSING_STREAK) {
            log.error("【{}】检测到连跪！已连跪 {} 场排位赛。", gameName, losingStreak);
        }

        // 判断近期排位赛场次
        if (recentRankedGames < MIN_RANKED_GAMES) {
            log.error("【{}】近期排位赛场次过少！最近 20 场比赛中，只有 {} 场排位赛。", gameName, recentRankedGames);
        }
    }


    /**
     * 从会话消息列表中获取召唤师ID列表
     */
    public List<Long> getSummonerIDListFromConversationMsgList(List<ConversationMsg> msgList) {
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
                return queryGameSummary(gameId); // 假设QueryGameSummary是一个远程调用方法
            } catch (Exception e) {
                lastException = e;
                Thread.sleep(delay);
            }
        }
        throw lastException;
    }


    /**
     * 查询对局详情
     *
     * @param gameID 对局ID
     * @return 对局详情信息对象
     * @throws IOException 查询对局详情过程中遇到的异常
     */
    public GameSummary queryGameSummary(long gameID) throws IOException {
        Request request = OkHttpUtil.createOkHttpGetRequest(String.format("/lol-match-history/v1/games/%d", gameID));
        return sendRequest(request, GameSummary.class);
    }


    /**
     * 获取队伍用户
     *
     * @return 对话ID、召唤师ID列表
     * @throws IOException 获取队伍用户过程中遇到的异常
     */
    public TeamUsersInfo getTeamUsers() throws Exception {
        String conversationID = lcuService.getCurrConversationID();
        if (conversationID == null || conversationID.isEmpty()) {
            log.error("当前不在英雄选择阶段");
            return null;
        }

        List<ConversationMsg> msgList = lcuService.listConversationMsg(conversationID);
        if (msgList == null || msgList.isEmpty()) {
            log.error("获取会话组消息记录失败");
            return null;
        }

        List<Long> summonerIDList = getSummonerIDListFromConversationMsgList(msgList);
        return new TeamUsersInfo(conversationID, summonerIDList);
    }


    public List<CurrSummoner> listSummoner(List<Long> summonerIDList) throws Exception {
        // 格式化 ID 列表为字符串
        List<String> idStrList = summonerIDList.stream().map(String::valueOf).collect(Collectors.toList());

        // 创建请求 URL
        Request request = OkHttpUtil.createOkHttpGetRequest(String.format("/lol-summoner/v2/summoners?ids=[%s]", String.join(",", idStrList)));

        return sendTypeRequest(request, new TypeReference<List<CurrSummoner>>() {
        });
    }


}
