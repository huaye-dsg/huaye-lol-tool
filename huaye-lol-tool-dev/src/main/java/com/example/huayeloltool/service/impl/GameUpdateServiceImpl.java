package com.example.huayeloltool.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.enums.Heros;
import com.example.huayeloltool.model.GameInfo;
import com.example.huayeloltool.enums.ScoreOption;
import com.example.huayeloltool.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import static com.example.huayeloltool.enums.GameEnums.GameFlow.CHAMPION_SELECT;
import static com.example.huayeloltool.enums.GameEnums.GameFlow.IN_PROGRESS;


@Service
@Slf4j
public class GameUpdateServiceImpl implements GameUpdateService {


    private String state;

    private Double defaultScore = 100.0;


    private DefaultClientConf clientCfg = DefaultClientConf.getInstance();


    @Autowired
    @Qualifier(value = "unsafeOkHttpClient")
    private OkHttpClient client;

    @SneakyThrows
    @Override
    public void onGameFlowUpdate(String gameState) {
        log.info("切换状态：{}", gameState);

        updateGameState(gameState);

        GameEnums.GameFlow gameFlow = GameEnums.GameFlow.getByValue(gameState);

        switch (gameFlow) {
            case MATCHMAKING:
                log.info("匹配中........");
                break;
            case READY_CHECK:
                log.info("等待接受对局");
                new Thread(this::acceptGame).start();
                break;
            case CHAMPION_SELECT:
                log.info("进入英雄选择阶段, 正在计算用户分数");
                new Thread(this::championSelectStart).start();
                break;
            case IN_PROGRESS:
                log.info("游戏进行中, 正在计算敌方队伍分数");
                new Thread(this::calcEnemyTeamScore).start();
                break;
            default:
                log.info("忽略状态：" + gameState);
                break;
        }
    }

    /**
     * 自动接受对局
     */
    public void acceptGame() {
        try {

            String auth = Base64.getEncoder().encodeToString(("riot:" + BaseUrlClient.getInstance().getAuthPwd()).getBytes());
            String URL = BaseUrlClient.assembleUrl("/lol-matchmaking/v1/ready-check/accept");
            Request request = new Request.Builder()
                    .url(URL)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .addHeader("Authorization", "Basic " + auth)
                    .post(RequestBody.create(MediaType.parse("application/json"), "{}"))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.info("自动接受对局失败: {}", com.alibaba.fastjson2.JSON.toJSONString(response));
                }
            }
        } catch (IOException e) {
            log.error("自动接受对局失败!", e);
        }
    }

    /**
     * 计算敌方队伍分数
     */
    public void calcEnemyTeamScore() {
        try {
            CurrSummoner currSummoner = CurrSummoner.getInstance();
            GameFlowSession session = queryGameFlowSession();
            log.info("GameFlowSession ：{}", JSON.toJSONString(session));
            if (session == null || !session.getPhase().equals(IN_PROGRESS)) {
                return;
            }
            if (currSummoner == null) {
                return;
            }

            long selfID = currSummoner.getSummonerId();
            Pair<List<Long>, List<Long>> allUsersFromSession = getAllUsersFromSession(selfID, session);
            List<Long> enemySummonerIDList = new ArrayList<>(allUsersFromSession.getRight());
            log.info("最终解析的敌方SummonerId为：{}", enemySummonerIDList);
            if (CollectionUtils.isEmpty(enemySummonerIDList)) {
                log.error("敌方用户ID为空");
                return;
            }

            List<UserScore> summonerScores = Collections.synchronizedList(new ArrayList<>());

            // 查询所有用户的信息并计算得分
            List<CurrSummoner> summonerList = listSummoner(enemySummonerIDList);
            if (CollectionUtils.isEmpty(summonerList)) {
                log.error("查询召唤师信息失败: {}", enemySummonerIDList);
                return;
            }


            extracted(summonerList);

        } catch (Exception e) {
            log.error("计算敌方队伍得分时发生错误", e);
        }
    }


    public Request createOkHttpRequest(String uri) {
        String auth = Base64.getEncoder().encodeToString(("riot:" + BaseUrlClient.getInstance().getAuthPwd()).getBytes());
        String URL = BaseUrlClient.assembleUrl(uri);
        log.info("url:{}", URL);
        return new Request.Builder()
                .addHeader("User-Agent", "Mozilla/5.0")
                .addHeader("Authorization", "Basic " + auth)
                .url(URL)
                .build();
    }

    public CurrSummoner getCurrSummoner() {
        try {
            Request okHttpRequest = createOkHttpRequest("/lol-summoner/v1/current-summoner");

            // 执行请求
            try (Response response = client.newCall(okHttpRequest).execute()) {
                if (!response.isSuccessful()) {
                    log.error("getCurrSummoner请求失败，response: {}", response);
                    throw new IOException("请求失败，返回码: " + response.code());
                }

                // 解析响应体
                assert response.body() != null;
                byte[] responseBody = response.body().bytes();
                CurrSummoner data = JSON.parseObject(responseBody, CurrSummoner.class);

                // 校验数据
                if (data.getSummonerId() == 0) {
                    log.error("获取当前召唤师失败，召唤师ID为0");
                }

                return data;
            }
        } catch (Exception e) {
            log.error("getCurrSummoner请求错误", e);
            return null;
        }
    }


    public Pair<List<Long>, List<Long>> getAllUsersFromSession(long selfID, GameFlowSession session) {
        log.info("getAllUsersFromSession selfID : {}", selfID);
        List<Long> selfTeamUsers = new ArrayList<>(5);
        List<Long> enemyTeamUsers = new ArrayList<>(5);

        GameEnums.TeamID selfTeamID = GameEnums.TeamID.NONE;

        // 检查 TeamOne
        for (GameFolwSessionTeamUser teamUser : session.getGameData().getTeamOne()) {
            long summonerID = teamUser.getSummonerId();
            if (selfID == summonerID) {
                log.info("检测到当前属于蓝色方！您的位置为：{}。selfID： {}", GameEnums.Position.getDescByValue(teamUser.getSelectedPosition()), summonerID);
                selfTeamID = GameEnums.TeamID.BLUE;
                break;
            }
        }

        // 如果 TeamOne 中没有找到，则检查 TeamTwo
        if (selfTeamID == GameEnums.TeamID.NONE) {
            for (GameFolwSessionTeamUser teamUser : session.getGameData().getTeamTwo()) {
                long summonerID = teamUser.getSummonerId();
                if (selfID == summonerID) {
                    log.info("检测到当前属于蓝色方！您的位置为：{}。selfID： {}", GameEnums.Position.getDescByValue(teamUser.getSelectedPosition()), summonerID);
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
            test(session, selfTeamUsers, enemyTeamUsers);
        } else {
            test(session, enemyTeamUsers, selfTeamUsers);
        }
        return Pair.of(selfTeamUsers, enemyTeamUsers);
    }

    private void test(GameFlowSession session, List<Long> teamUsers1, List<Long> teamUsers2) {
        for (GameFolwSessionTeamUser user : session.getGameData().getTeamOne()) {
            long userID = user.getSummonerId();
            if (userID > 0) {
                log.info("添加SummonerId： {}", userID);
                teamUsers1.add(userID);
            } else {
                log.error("发现SummonerId异常：{}", userID);
                continue;
            }
        }
        for (GameFolwSessionTeamUser user : session.getGameData().getTeamTwo()) {
            long userID = user.getSummonerId();
            if (userID > 0) {
                log.info("添加SummonerId： {}", userID);
                teamUsers2.add(userID);
            } else {
                log.error("发现SummonerId异常：{}", userID);
                continue;
            }
        }
    }

    public GameFlowSession queryGameFlowSession() throws IOException {
        Request request = createOkHttpRequest("/lol-gameflow/v1/session");
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            byte[] responseBody = response.body().bytes();
            GameFlowSession data = JSON.parseObject(responseBody, GameFlowSession.class);

            if (data.getErrorCode() != null && !data.getErrorCode().isEmpty()) {
                throw new IOException("查询游戏会话失败: " + data.getMessage());
            }

            return data;
        }
    }

    private String determineHorse(UserScore score, CalcScoreConf scoreCfg, DefaultClientConf clientCfg) {
        for (int i = 0; i < scoreCfg.getHorse().length; i++) {
            if (score.getScore() >= scoreCfg.getHorse()[i].getScore()) {
                return clientCfg.getHorseNameConf()[i];
            }
        }
        return "未知";
    }

    public void updateGameState(String gameState) {
        log.info("更新游戏状态：{}", gameState);
        state = gameState;
    }

    @Override
    public void onChampSelectSessionUpdate(ChampSelectSessionInfo sessionInfo) {
//        log.info("游戏选择会话变更： {}", JSON.toJSONString(sessionInfo));
        log.info("游戏选择会话变更");
        int userPickActionId = 0;
        int userBanActionId = 0;
        int pickChampionId = 0;
        boolean isSelfPick = false;
        boolean isSelfBan = false;
        boolean pickIsInProgress = false;
        boolean banIsInProgress = false;
        Set<Integer> alloyPrePickSet = new HashSet<>(5);

        // 处理Actions数据
        if (sessionInfo.getActions() != null && !sessionInfo.getActions().isEmpty()) {
            for (List<ChampSelectSessionInfo.Action> actionList : sessionInfo.getActions()) {
                for (ChampSelectSessionInfo.Action action : actionList) {
                          boolean allyAction = action.getIsAllyAction();
                    if(allyAction){
                        if(action.getChampionId() > 0){
                            log.info("友方操作环节：操作英雄：{}, action = {}"，Heros.getNameById(action.getChampionId()), JSON.toJSONString(action));
                        }else{
                            log.info("友方操作环节：不涉及英雄, action = {}"，JSON.toJSONString(action));
                        }
                    }else{
                       if(action.getChampionId() > 0){
                            log.info("友方操作环节：操作英雄：{}, action = {}"，Heros.getNameById(action.getChampionId()), JSON.toJSONString(action));
                        }else{
                            log.info("友方操作环节：不涉及英雄, action = {}"，JSON.toJSONString(action));
                        }                    
                    }

                    // 收集预选英雄
                    if (
                        allyAction
                            && "pick".equalsIgnoreCase(action.getType())
                            && action.getChampionId() > 0) {
                        log.info("添加预选英雄：{}",action.getChampionId());
                        alloyPrePickSet.add(action.getChampionId());
                    }

                    // 检查当前玩家动作
                    if (action.getActorCellId() != sessionInfo.getLocalPlayerCellId()) {
                        continue;
                    }

                    log.info("本人操作环节：{},LocalPlayerCellId: {},ActorCellId: {}",JSON.toJSONString(action),sessionInfo.getLocalPlayerCellId() ,action.getActorCellId());
                    if ("pick".equalsIgnoreCase(action.getType())) {
                        isSelfPick = true;
                        userPickActionId = action.getId();
                        pickChampionId = action.getChampionId();
                        pickIsInProgress = action.getIsInProgress();
                    } else if ("ban".equalsIgnoreCase(action.getType())) {
                        isSelfBan = true;
                        userBanActionId = action.getId();
                        banIsInProgress = action.getIsInProgress();
                    }
                    break;
                }
            }
        }
        List<String> preNames = new ArrayList<>();
        for(Integer id: alloyPrePickSet){
            if(null != id){
                preNames.add(Heros.getNameById(id));
            }
        }
        
        log.info("预选名单为: {}", preNames.toString());

        // 自动选择英雄
        if (clientCfg.getAutoPickChampID() > 0 && isSelfPick) {
            log.info("进入本人操作阶段");
            if (pickIsInProgress) {
                log.info("本人正在选择英雄...");
                pickChampion(clientCfg.getAutoPickChampID(), userPickActionId);
            } else if (pickChampionId == 0) {
                log.info("本人正在预选英雄...");
                prePickChampion(clientCfg.getAutoPickChampID(), userPickActionId);
            }
        }

        // 自动禁用英雄
        if (clientCfg.getAutoBanChampID() > 0 && isSelfBan && banIsInProgress) {
            log.info("本人正在禁用英雄，预选名单为: {}", alloyPrePickSet);
            if (!alloyPrePickSet.contains(clientCfg.getAutoBanChampID())) {
                log.info("预选名单不包含将要禁用的英雄：{}, 可以禁用", clientCfg.getAutoBanChampID());
                banChampion(clientCfg.getAutoBanChampID(), userBanActionId);
            } else {
                log.info("预选名单包含将要禁用的英雄：{}, 取消禁用", clientCfg.getAutoBanChampID());
            }
        }
    }


    /**
     * 通用英雄选择操作
     *
     * @param championId 英雄ID
     * @param actionId   动作ID
     * @param patchType  操作类型（PICK/BAN）
     * @param completed  是否完成操作
     * @return 是否成功
     */
    public boolean champSelectPatchAction(int championId, int actionId,
                                          String patchType,
                                          Boolean completed) {
        Map<String, Object> body = new HashMap<>();
        body.put("championId", championId);
        String auth = Base64.getEncoder().encodeToString(("riot:" + BaseUrlClient.getInstance().getAuthPwd()).getBytes());

        Optional.ofNullable(patchType).ifPresent(t -> body.put("type", t));
        Optional.ofNullable(completed).ifPresent(c -> body.put("completed", c));
        Request request = new Request.Builder()
                .url(BaseUrlClient.assembleUrl("/lol-champ-select/v1/session/actions/") + actionId)
                .addHeader("Authorization", "Basic " + auth)
                .patch(RequestBody.create(
                        JSON.toJSONString(body),
                        MediaType.get("application/json")
                ))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                handleErrorResponse(response);
                return false;
            }
            return true;
        } catch (IOException e) {
            log.info("champSelectPatchActionError", e);
            return false;
        }
    }

    private static void handleErrorResponse(Response response) throws IOException {
        if (response.body() == null) return;

        CommonResp resp = JSON.parseObject(
                response.body().string(),
                CommonResp.class
        );

        if (resp.getErrorCode() != null) {
            throw new RuntimeException("操作失败: " + resp.getMessage());
        }
    }


    // 预选英雄
    public boolean prePickChampion(int championId, int actionId) {
        return champSelectPatchAction(championId, actionId, null, null);
    }

    // 确认选择
    public boolean pickChampion(int championId, int actionId) {
        return champSelectPatchAction(
                championId,
                actionId,
                Constant.CHAMP_SELECT_PATCH_TYPE_PICK,
                true
        );
    }

    // 禁用英雄
    public boolean banChampion(int championId, int actionId) {
        return champSelectPatchAction(
                championId,
                actionId,
                Constant.CHAMP_SELECT_PATCH_TYPE_BAN,
                true
        );
    }


    @SneakyThrows
    public void championSelectStart() {
        List<Long> summonerIDList = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            TimeUnit.SECONDS.sleep(1);
            // 获取队伍所有用户信息
            TeamUsersInfo teamUsersInfo = getTeamUsers();
            summonerIDList = teamUsersInfo.getSummonerIdList();

            if (summonerIDList.size() == 5) {
                break;
            } else {
                log.error("队伍人数不为5");
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
            log.info("查询召唤师信息失败, summconerList为空！ ");
            return;
        }

        extracted(summonerList);

    }

    private void extracted(List<CurrSummoner> summonerList) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        List<UserScore> userScores = Collections.synchronizedList(new ArrayList<>());
        for (CurrSummoner summoner : summonerList) {
//            futures.add(CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(200);
                userScores.add(getUserScore(summoner));
            } catch (Exception e) {
                log.error("计算用户得分失败", e);
            }
//            }));
        }

//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 分数从高到低排序
        userScores.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        CalcScoreConf scoreCfg = new CalcScoreConf();

        for (UserScore scoreInfo : userScores) {
            String horse = "";
            for (int i = 0; i < scoreCfg.getHorse().length; i++) {
                HorseScoreConf[] horse1 = scoreCfg.getHorse();
                HorseScoreConf horseScoreConf = horse1[i];
                if (scoreInfo.getScore() >= horseScoreConf.getScore()) {
                    horse = clientCfg.getHorseNameConf()[i];
                    break;
                }
            }

            StringBuilder currKDASb = new StringBuilder();
            for (int i = 0; i < Math.min(5, scoreInfo.getCurrKDA().size()); i++) {
                currKDASb.append(String.format("[%s-%s]%d/%d/%d   ",
                        scoreInfo.getCurrKDA().get(i).getWin() ? "胜" : "败",
                        scoreInfo.getCurrKDA().get(i).getChampionName(),
                        scoreInfo.getCurrKDA().get(i).getKills(),
                        scoreInfo.getCurrKDA().get(i).getDeaths(),
                        scoreInfo.getCurrKDA().get(i).getAssists()
                ));
            }

            String currKDAMsg = currKDASb.toString().trim();
            String msg = String.format("【%s】【%d分】: %s %s", horse, scoreInfo.getScore().intValue(), scoreInfo.getSummonerName(), currKDAMsg);
            log.info("msg: {}", msg);
        }
    }

    // 获取用户得分
    public UserScore getUserScore(CurrSummoner summoner) {
        long summonerID = summoner.getSummonerId();
        UserScore userScoreInfo = new UserScore(summonerID, defaultScore);
        userScoreInfo.setSummonerName(String.format("%s#%s", summoner.getGameName(), summoner.getTagLine()));

        // 获取战绩列表
        List<GameInfo> gameList;
        try {
            gameList = listGameHistory(summoner.getPuuid());
        } catch (Exception e) {
            log.error("获取游戏战绩列表失败", e);
            return userScoreInfo;
        }

        if (CollectionUtils.isEmpty(gameList)) {
            log.info("用户战绩查询为空！直接返回。summoner： {}", JSON.toJSONString(summoner));
            return userScoreInfo;
        }


        List<GameSummary> gameSummaryList = Collections.synchronizedList(new ArrayList<>());
        CompletableFuture[] futures = gameList.stream().map(info -> CompletableFuture.runAsync(() -> {
            try {
                GameSummary gameSummary = queryGameSummaryWithRetry(info.getGameId());
                gameSummaryList.add(gameSummary);
            } catch (Exception e) {
                log.error("获取游戏对局详细信息失败", e);
            }
        })).toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();

        double totalScore = 0;
        int totalGameCount = 0;
        LocalDateTime nowTime = LocalDateTime.now();
        List<Double> currTimeScoreList = new ArrayList<>();
        List<Double> otherGameScoreList = new ArrayList<>();

        for (GameSummary gameSummary : gameSummaryList) {
            try {
                ScoreWithReason scoreWithReason = calcUserGameScore(summonerID, gameSummary);
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


    // 计算用户游戏得分
    public ScoreWithReason calcUserGameScore(long summonerID, GameSummary gameSummary) throws Exception {
        //CalcScoreConf calcScoreConf = global.getScoreConf();
        CalcScoreConf calcScoreConf = new CalcScoreConf();
        ScoreWithReason gameScore = new ScoreWithReason(defaultScore);
        int userParticipantId = 0;

        // 获取用户参与的ID
        for (GameSummary.ParticipantIdentity identity : gameSummary.getParticipantIdentities()) {
            if (identity.getPlayer().getSummonerId() == summonerID) {
                userParticipantId = identity.getParticipantId();
            }
        }

        if (userParticipantId == 0) {
            throw new Exception("获取用户位置失败");
        }

        Optional<Integer> userTeamID = Optional.empty();
        List<Integer> memberParticipantIDList = new ArrayList<>(4);
        Map<Integer, Participant> idMapParticipant = new HashMap<>(gameSummary.getParticipants().size());

        // 映射参与者ID和获取用户队伍ID
        for (Participant item : gameSummary.getParticipants()) {
            if (item.getParticipantId() == userParticipantId) {
                userTeamID = Optional.of(item.getTeamId());
            }
            idMapParticipant.put(item.getParticipantId(), item);
        }

        if (!userTeamID.isPresent()) {
            throw new Exception("获取用户队伍id失败");
        }

        // 获取同队参与者ID列表
        for (Participant item : gameSummary.getParticipants()) {
            if (item.getTeamId().equals(userTeamID.get())) {
                memberParticipantIDList.add(item.getParticipantId());
            }
        }

        int totalKill = 0;   // 总人头
        int totalDeath = 0;  // 总死亡
        int totalAssist = 0; // 总助攻
        int totalHurt = 0;   // 总伤害
        int totalMoney = 0;  // 总金钱

        for (Participant participant : gameSummary.getParticipants()) {
            if (!participant.getTeamId().equals(userTeamID.get())) {
                continue;
            }
            totalKill += participant.getStats().getKills();
            totalDeath += participant.getStats().getDeaths();
            totalAssist += participant.getStats().getAssists();
            totalHurt += participant.getStats().getTotalDamageDealtToChampions();
            totalMoney += participant.getStats().getGoldEarned();
        }

        Participant userParticipant = idMapParticipant.get(userParticipantId);
        boolean isSupportRole = userParticipant.getTimeline().getLane().equals("BOTTOM") &&
                userParticipant.getTimeline().getRole().equals("SUPPORT");

        // 一血击杀
        if (userParticipant.getStats().isFirstBloodKill()) {
            gameScore.add(calcScoreConf.getFirstBlood()[0], ScoreOption.FIRST_BLOOD_KILL);
        } else if (userParticipant.getStats().isFirstBloodAssist()) {
            gameScore.add(calcScoreConf.getFirstBlood()[1], ScoreOption.FIRST_BLOOD_ASSIST);
        }

        // 五杀、四杀、三杀
        if (userParticipant.getStats().getPentaKills() > 0) {
            gameScore.add(calcScoreConf.getPentaKills()[0], ScoreOption.PENTA_KILLS);
        } else if (userParticipant.getStats().getQuadraKills() > 0) {
            gameScore.add(calcScoreConf.getQuadraKills()[0], ScoreOption.QUADRA_KILLS);
        } else if (userParticipant.getStats().getTripleKills() > 0) {
            gameScore.add(calcScoreConf.getTripleKills()[0], ScoreOption.TRIPLE_KILLS);
        }

        // 参团率
        if (totalKill > 0) {
            int joinTeamRateRank = 1;
            double userJoinTeamKillRate = (double) (userParticipant.getStats().getAssists() + userParticipant.getStats().getKills()) / totalKill;
            List<Double> memberJoinTeamKillRates = listMemberJoinTeamKillRates(gameSummary, totalKill, memberParticipantIDList);
            for (double rate : memberJoinTeamKillRates) {
                if (rate > userJoinTeamKillRate) {
                    joinTeamRateRank++;
                }
            }
            adjustGameScoreForRank(joinTeamRateRank, gameScore, calcScoreConf.getJoinTeamRateRank(), ScoreOption.JOIN_TEAM_RATE_RANK);
        }

        // 获取金钱
        if (totalMoney > 0) {
            int moneyRank = 1;
            int userMoney = userParticipant.getStats().getGoldEarned();
            List<Integer> memberMoneyList = listMemberMoney(gameSummary, memberParticipantIDList);
            for (int v : memberMoneyList) {
                if (v > userMoney) {
                    moneyRank++;
                }
            }
            adjustGameScoreForRank(moneyRank, gameScore, calcScoreConf.getGoldEarnedRank(), ScoreOption.GOLD_EARNED_RANK, !isSupportRole);
        }

        // 伤害占比
        if (totalHurt > 0) {
            int hurtRank = 1;
            int userHurt = userParticipant.getStats().getTotalDamageDealtToChampions();
            List<Integer> memberHurtList = listMemberHurt(gameSummary, memberParticipantIDList);
            for (int v : memberHurtList) {
                if (v > userHurt) {
                    hurtRank++;
                }
            }
            adjustGameScoreForRank(hurtRank, gameScore, calcScoreConf.getHurtRank(), ScoreOption.HURT_RANK);
        }

        // 金钱转换伤害比
        if (totalMoney > 0 && totalHurt > 0) {
            int money2hurtRateRank = 1;
            double userMoney2hurtRate = (double) userParticipant.getStats().getTotalDamageDealtToChampions() / userParticipant.getStats().getGoldEarned();
            List<Double> memberMoney2hurtRateList = listMemberMoney2hurtRate(gameSummary, memberParticipantIDList);
            for (double v : memberMoney2hurtRateList) {
                if (v > userMoney2hurtRate) {
                    money2hurtRateRank++;
                }
            }
            adjustGameScoreForRank(money2hurtRateRank, gameScore, calcScoreConf.getMoney2hurtRateRank(), ScoreOption.MONEY_TO_HURT_RATE_RANK);
        }

        // 视野得分
        {
            int visionScoreRank = 1;
            int userVisionScore = userParticipant.getStats().getVisionScore();
            List<Integer> memberVisionScoreList = listMemberVisionScore(gameSummary, memberParticipantIDList);
            for (int v : memberVisionScoreList) {
                if (v > userVisionScore) {
                    visionScoreRank++;
                }
            }
            adjustGameScoreForRank(visionScoreRank, gameScore, calcScoreConf.getVisionScoreRank(), ScoreOption.VISION_SCORE_RANK);
        }

        // 补兵 每分钟8个刀以上加5分 ,9+10, 10+20
        {
            int totalMinionsKilled = userParticipant.getStats().getTotalMinionsKilled();
            int gameDurationMinute = gameSummary.getGameDuration() / 60;
            int minuteMinionsKilled = totalMinionsKilled / gameDurationMinute;
            for (double[] minionsKilledLimit : calcScoreConf.getMinionsKilled()) {
                if (minuteMinionsKilled >= minionsKilledLimit[0]) {
                    gameScore.add(minionsKilledLimit[1], ScoreOption.MINIONS_KILLED);
                    break;
                }
            }
        }

        // 人头占比
        if (totalKill > 0) {
            double userKillRate = (double) userParticipant.getStats().getKills() / totalKill;
            adjustGameScoreForRate(userKillRate, userParticipant.getStats().getKills(), calcScoreConf.getKillRate(), gameScore, ScoreOption.KILL_RATE);
        }

        // 伤害占比
        if (totalHurt > 0) {
            double userHurtRate = (double) userParticipant.getStats().getTotalDamageDealtToChampions() / totalHurt;
            adjustGameScoreForRate(userHurtRate, userParticipant.getStats().getKills(), calcScoreConf.getHurtRate(), gameScore, ScoreOption.HURT_RATE);
        }

        // 助攻占比
        if (totalAssist > 0) {
            double userAssistRate = (double) userParticipant.getStats().getAssists() / totalAssist;
            adjustGameScoreForRate(userAssistRate, userParticipant.getStats().getKills(), calcScoreConf.getAssistRate(), gameScore, ScoreOption.ASSIST_RATE);
        }

        // KDA调整
        double userJoinTeamKillRate = (double) (userParticipant.getStats().getAssists() + userParticipant.getStats().getKills()) / totalKill;
        int userDeathTimes = userParticipant.getStats().getDeaths() == 0 ? 1 : userParticipant.getStats().getDeaths();
        double adjustVal = ((userParticipant.getStats().getKills() + userParticipant.getStats().getAssists()) / userDeathTimes - calcScoreConf.getAdjustKDA()[0] +
                (userParticipant.getStats().getKills() - userParticipant.getStats().getDeaths()) / calcScoreConf.getAdjustKDA()[1]) * userJoinTeamKillRate;
        gameScore.add(adjustVal, ScoreOption.KDA_ADJUST);

        return gameScore;
    }

    // 调整分数 根据排名
    private void adjustGameScoreForRank(int rank, ScoreWithReason gameScore, double[] scoreConf, ScoreOption option) {
        adjustGameScoreForRank(rank, gameScore, scoreConf, option, true);
    }

    // 调整分数 根据排名
    private void adjustGameScoreForRank(int rank, ScoreWithReason gameScore, double[] scoreConf, ScoreOption option, boolean applyNegative) {
        if (rank == 1) {
            gameScore.add(scoreConf[0], option);
        } else if (rank == 2) {
            gameScore.add(scoreConf[1], option);
        } else if (rank == 4 && applyNegative && scoreConf.length >= 3) {
            gameScore.add(-scoreConf[2], option);
        } else if (rank == 5 && applyNegative && scoreConf.length >= 4) {
            gameScore.add(-scoreConf[3], option);
        }
    }

    // 调整分数 根据rate
    private void adjustGameScoreForRate(double rate, int kills, List<RateItemConf> rateConf, ScoreWithReason gameScore, ScoreOption option) {
        for (RateItemConf confItem : rateConf) {
            if (rate > confItem.getLimit()) {
                for (double[] limitConf : confItem.getScoreConf()) {
                    if (kills > (int) limitConf[0]) {
                        gameScore.add(limitConf[1], option);
                        break;
                    }
                }
            }
        }
    }

    // 获取队员视野得分
    private List<Integer> listMemberVisionScore(GameSummary gameSummary, List<Integer> memberParticipantIDList) {
        List<Integer> res = new ArrayList<>(4);
        for (Participant participant : gameSummary.getParticipants()) {
            if (!memberParticipantIDList.contains(participant.getParticipantId())) {
                continue;
            }
            res.add(participant.getStats().getVisionScore());
        }
        return res;
    }

    // 获取队员金钱转换伤害比
    private List<Double> listMemberMoney2hurtRate(GameSummary gameSummary, List<Integer> memberParticipantIDList) {
        List<Double> res = new ArrayList<>(4);
        for (Participant participant : gameSummary.getParticipants()) {
            if (!memberParticipantIDList.contains(participant.getParticipantId())) {
                continue;
            }
            res.add((double) participant.getStats().getTotalDamageDealtToChampions() / participant.getStats().getGoldEarned());
        }
        return res;
    }

    // 获取队员金钱
    private List<Integer> listMemberMoney(GameSummary gameSummary, List<Integer> memberParticipantIDList) {
        List<Integer> res = new ArrayList<>(4);
        for (Participant participant : gameSummary.getParticipants()) {
            if (!memberParticipantIDList.contains(participant.getParticipantId())) {
                continue;
            }
            res.add(participant.getStats().getGoldEarned());
        }
        return res;
    }

    // 获取队员参团率
    private List<Double> listMemberJoinTeamKillRates(GameSummary gameSummary, int totalKill, List<Integer> memberParticipantIDList) {
        List<Double> res = new ArrayList<>(4);
        for (Participant participant : gameSummary.getParticipants()) {
            if (!memberParticipantIDList.contains(participant.getParticipantId())) {
                continue;
            }
            res.add((double) (participant.getStats().getAssists() + participant.getStats().getKills()) / totalKill);
        }
        return res;
    }

    // 获取队员伤害
    private List<Integer> listMemberHurt(GameSummary gameSummary, List<Integer> memberParticipantIDList) {
        List<Integer> res = new ArrayList<>(4);
        for (Participant participant : gameSummary.getParticipants()) {
            if (!memberParticipantIDList.contains(participant.getParticipantId())) {
                continue;
            }
            res.add(participant.getStats().getTotalDamageDealtToChampions());
        }
        return res;
    }

    /**
     * 根据 PUUID 列出游戏历史记录
     *
     * @param puuid 用户唯一标识符
     * @return 游戏信息列表
     * @throws IOException 查询用户战绩过程中遇到的异常
     */
    public List<GameInfo> listGameHistory(String puuid) throws IOException {
        List<GameInfo> fmtList = new ArrayList<>();
        GameAllData gameAllData = listGamesByPUUID(puuid, 0, 10);

        if (Objects.isNull(gameAllData)) {
            log.error("查询用户战绩失败: puuid={}", puuid);
            return new ArrayList<>();
        }
        List<GameInfo> games = gameAllData.getGames().getGames();
        if (CollectionUtils.isEmpty(games)) {
            log.error("查询用户战绩为空！: puuid={}", puuid);
            return new ArrayList<>();
        }

        // 过滤符合条件的游戏信息
        for (GameInfo gameItem : games) {
            if (!Objects.equals(gameItem.getQueueId(), GameEnums.GameQueueID.NormalQueueID.getValue()) &&
                    !Objects.equals(gameItem.getQueueId(), GameEnums.GameQueueID.RankSoleQueueID.getValue()) &&
                    !Objects.equals(gameItem.getQueueId(), GameEnums.GameQueueID.FastNormalQueueID.getValue()) &&
                    !Objects.equals(gameItem.getQueueId(), GameEnums.GameQueueID.ARAMQueueID.getValue()) &&
                    !Objects.equals(gameItem.getQueueId(), GameEnums.GameQueueID.RankFlexQueueID.getValue())) {
                continue;
            }
            if (gameItem.getGameDuration() < 300) {
                continue;
            }
            fmtList.add(gameItem);
        }
        return fmtList;
    }

    /**
     * 根据 PUUID 获取比赛记录
     *
     * @param puuid 用户唯一标识符
     * @param begin 开始位置
     * @param limit 获取数量
     * @return 比赛记录响应对象
     * @throws IOException 获取比赛记录失败时抛出异常
     */
    public GameAllData listGamesByPUUID(String puuid, int begin, int limit) throws IOException {
        Request request = createOkHttpRequest(String.format("/lol-match-history/v1/products/lol/%s/matches?begIndex=%d&endIndex=%d", puuid, begin, begin + limit));
        GameAllData gameInfos;
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("获取比赛记录失败: puuid={}", puuid);
                throw new IOException("获取比赛记录失败");
            }
            assert response.body() != null;
            String string = response.body().string();
            gameInfos = JSON.parseObject(string, new TypeReference<GameAllData>() {
            });
            log.info("战绩解析结果（最后一条）：: {}", gameInfos.getGames().getGames().get(0));
        }
        return gameInfos;
    }

    /**
     * 根据会话ID获取会话组消息记录
     *
     * @param conversationID 会话组ID
     * @return 会话消息记录列表
     * @throws IOException 获取会话组消息记录失败时抛出异常
     */
    public List<ConversationMsg> listConversationMsg(String conversationID) throws IOException {
        Request request = createOkHttpRequest(String.format("/lol-chat/v1/conversations/%s/messages", conversationID));
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("获取会话组消息记录失败: conversationID={}", conversationID);
                throw new IOException("获取会话组消息记录失败");
            }
            assert response.body() != null;
            return JSON.parseObject(response.body().string(), new TypeReference<List<ConversationMsg>>() {
            });

        }
    }

    /**
     * 从会话消息列表中获取召唤师ID列表
     *
     * @param msgList 会话消息列表
     * @return 召唤师ID列表
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

    /**
     * 获取当前对局聊天组ID
     *
     * @return 当前对局聊天组ID
     * @throws IOException 当前不在英雄选择阶段或获取失败时抛出异常
     */
    public String GetCurrConversationID() throws IOException {
        String url = BaseUrlClient.assembleUrl("/lol-chat/v1/conversations");
        Request request = new Request.Builder()
                .url(url)
                .build();


        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.info("获取当前对局聊天组失败");
                throw new IOException("获取当前对局聊天组失败");
            }

            assert response.body() != null;
            String responseBody = response.body().string();
            List<Conversation> list = JSON.parseArray(responseBody, Conversation.class);
            for (Conversation conversation : list) {
                if (conversation.getType().equals(CHAMPION_SELECT)) {
                    return conversation.getId();
                }
            }
            throw new IOException("当前不在英雄选择阶段");
        }
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
        Request request = createOkHttpRequest(String.format("/lol-match-history/v1/games/%d", gameID));

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.info("查询对局详情失败: gameID= {}", gameID);
                throw new IOException("查询对局详情失败");
            }

            String responseBody = response.body().string();
            GameSummary data = JSON.parseObject(responseBody, GameSummary.class);

            if (data.getErrorCode() != null && !data.getErrorCode().isEmpty()) {
                throw new IOException(String.format("查询对局详情失败 :%s ,gameID: %d", data.getMessage(), gameID));
            }

            return data;
        }
    }


    /**
     * 获取队伍用户
     *
     * @return 对话ID、召唤师ID列表
     * @throws IOException 获取队伍用户过程中遇到的异常
     */
    public TeamUsersInfo getTeamUsers() throws Exception {
        String conversationID = getCurrConversationID();
        if (conversationID == null || conversationID.isEmpty()) {
            throw new IOException("当前不在英雄选择阶段");
        }

        List<ConversationMsg> msgList = listConversationMsg(conversationID);
        if (msgList == null || msgList.isEmpty()) {
            throw new IOException("获取会话组消息记录失败");
        }

        List<Long> summonerIDList = getSummonerIDListFromConversationMsgList(msgList);
        return new TeamUsersInfo(conversationID, summonerIDList);
    }


    public String getCurrConversationID() throws Exception {
        Request request = createOkHttpRequest("/lol-chat/v1/conversations");

        // 执行请求
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("getCurrSummoner请求失败，response: {}", response);
                throw new IOException("请求失败，返回码: " + response.code());
            }

            // 解析响应体
            assert response.body() != null;
            byte[] responseBody = response.body().bytes();
            List<Conversation> conversationList = JSON.parseObject(new String(responseBody), new TypeReference<List<Conversation>>() {
            });

            for (Conversation conversation : conversationList) {
                if (CHAMPION_SELECT.equals(conversation.getType())) { // 换成你实际的类型比较
                    return conversation.getId();
                }
            }

            return null;
        }
    }

    public List<CurrSummoner> listSummoner(List<Long> summonerIDList) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper(); // 用于 JSON 解析

        // 格式化 ID 列表为字符串
        List<String> idStrList = summonerIDList.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());

        // 创建请求 URL
        Request request = createOkHttpRequest(String.format("/lol-summoner/v2/summoners?ids=[%s]", String.join(",", idStrList)));
        try (Response response = client.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                log.error("getCurrSummoner请求失败，response: {}", response);
                throw new IOException("请求失败，返回码: " + response.code());
            }
            assert response.body() != null;
            String body = response.body().string();
            return JSON.parseObject(body, new TypeReference<List<CurrSummoner>>() {
            });
        }
    }


    /**
     * 找到LOL进程并解析端口和token
     */
    public ProcessInfo getLolClientApiInfo(String processName) {
        SystemInfo systemInfo = new SystemInfo();
        OperatingSystem os = systemInfo.getOperatingSystem();

        // 获取所有进程
        List<OSProcess> processes = os.getProcesses();

        String targetProcessName = "LeagueClientUx";
        // 在进程列表中查找LOL进程
        ProcessInfo processInfo = null;
        for (OSProcess process : processes) {
            if (process.getName().equalsIgnoreCase(targetProcessName)) {
                log.info("成功找到进程！ {}", targetProcessName);
                processInfo = new ProcessInfo();
                List<String> arguments = process.getArguments();
                for (String argument : arguments) {
                    if (argument.contains("--app-port")) {
                        String[] split = argument.split("=");
                        log.info("解析的端口：{}", split[1]);
                        processInfo.setPort(Integer.valueOf(split[1]));
                    }
                    if (argument.contains("--remoting-auth-token")) {
                        String[] split = argument.split("=");
                        log.info("解析的token：{}", split[1]);
                        processInfo.setToken(split[1]);
                    }
                }
                break;
            }
        }
        return processInfo;
    }

}
