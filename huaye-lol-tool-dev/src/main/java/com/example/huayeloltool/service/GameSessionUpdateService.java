package com.example.huayeloltool.service;

import com.alibaba.fastjson2.JSON;
import com.example.huayeloltool.config.OkHttpUtil;
import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.enums.Heros;
import com.example.huayeloltool.model.ChampSelectSessionInfo;
import com.example.huayeloltool.model.DefaultClientConf;
import com.example.huayeloltool.model.SelfGameSession;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GameSessionUpdateService {

    private final DefaultClientConf clientCfg = DefaultClientConf.getInstance();

    @Autowired
    @Qualifier(value = "unsafeOkHttpClient")
    private OkHttpClient client;

    public void onChampSelectSessionUpdate(ChampSelectSessionInfo sessionInfo) {

//        if (sessionInfo.getIsCustomGame()) {
//            log.info("自定义模式，不查询战绩");
//        }

//        log.info("游戏选择会话变更： {}", JSON.toJSONString(sessionInfo));
        analyzeSession(sessionInfo);
////        log.info("游戏选择会话变更");
//
//        Integer localPlayerCellId = sessionInfo.getLocalPlayerCellId();
//        SelfGameSession.setFloor(localPlayerCellId);
//
//        List<ChampSelectSessionInfo.Player> myTeams = sessionInfo.getMyTeam();
//        if (CollectionUtils.isNotEmpty(myTeams)) {
//            for (ChampSelectSessionInfo.Player player : myTeams) {
//                if (Objects.equals(player.getCellId(), localPlayerCellId)) {
//                    log.info("解析到当前位置：{}", player.getAssignedPosition());
//                    SelfGameSession.setPosition(GameEnums.Position.getDescByValue(player.getAssignedPosition()));
//                    break;
//                }
//            }
//        }
//
//        if (SelfGameSession.getPosition() == null) {
//            log.error("未解析成功当前位置");
//        }
//
//
//        List<ChampSelectSessionInfo.Player> myTeam = sessionInfo.getMyTeam();
//        List<Integer> collect = myTeam.stream().filter(item -> item.getChampionId() != null && item.getChampionId() > 0).map(item -> item.getChampionId()).collect(Collectors.toList());
//        List<String> selfHeroNames = new ArrayList<>();
//        if (CollectionUtils.isNotEmpty(collect)) {
//            selfHeroNames = collect.stream().map(Heros::getNameById).collect(Collectors.toList());
//        }
//
//
////        List<ChampSelectSessionInfo.Player> selfPreHeroNames = getPreHeros(myTeam);
//
//
//        List<ChampSelectSessionInfo.Player> theirTeam = sessionInfo.getTheirTeam();
//        List<Integer> collec2t = theirTeam.stream().filter(item -> item.getChampionId() != null && item.getChampionId() > 0).map(item -> item.getChampionId()).collect(Collectors.toList());
//        List<String> enmtyHeroNames = new ArrayList<>();
//        if (CollectionUtils.isNotEmpty(collec2t)) {
//            enmtyHeroNames = collec2t.stream().map(Heros::getNameById).collect(Collectors.toList());
//        }
//
////        List<ChampSelectSessionInfo.Player> theirPreHeroNames = getPreHeros(theirTeam);
//
//        log.info("敌方puuid：{}", theirTeam.stream().map(ChampSelectSessionInfo.Player::getPuuid).collect(Collectors.toList()));
//
//
//        List<String> selfBansNames = sessionInfo.getBans().getMyTeamBans().stream().map(Heros::getNameById).collect(Collectors.toList());
//        List<String> theirBansNames = sessionInfo.getBans().getTheirTeamBans().stream().map(Heros::getNameById).collect(Collectors.toList());
//        log.info("【游戏信息】游戏模式：{}，位置：{}，楼层：{}楼。\n " +
//                        "我方阵容英雄为：{} \n " +
//                        "我方禁用英雄为：{} \n " +
////                        "我方预选英雄为：{} \n " +
//                        "敌方阵容英雄为：{} \n " +
//                        "敌方禁用英雄为：{} \n ",
////                        "敌方预选英雄为：{} \n ",
//                GameEnums.GameQueueID.getGameNameMap(SelfGameSession.getQueueId()),
//                GameEnums.Position.getDescByValue(SelfGameSession.getPosition()),
//                SelfGameSession.getFloor() + 1,
//                selfHeroNames,
//                selfBansNames,
////                selfPreHeroNames,
//                enmtyHeroNames,
//                theirBansNames
////                theirPreHeroNames
//        );
//
//
//        if (SelfGameSession.isBaned() && SelfGameSession.isSelected()) {
//            log.info("已经选择完并且仅用了。跳过");
//            return;
//        }
//
//
//        // 先查找当前进行中的动作组
//        List<ChampSelectSessionInfo.Action> currentActionGroup = null;
//        if (sessionInfo.getActions() != null && !sessionInfo.getActions().isEmpty()) {
//            for (List<ChampSelectSessionInfo.Action> actionList : sessionInfo.getActions()) {
//                for (ChampSelectSessionInfo.Action action : actionList) {
//                    if (action.getIsInProgress()) {
//                        currentActionGroup = actionList;
//                        break;
//                    }
//                }
//                if (currentActionGroup != null) {
//                    break;
//                }
//            }
//        }
//
//
//        // 如果找到了正在进行中的动作组，只处理这一组的动作
//        if (currentActionGroup != null) {
//            for (ChampSelectSessionInfo.Action action : currentActionGroup) {
//                // 检查是否为当前玩家的操作
//                if (!action.getIsInProgress() || !action.getCompleted() || !Objects.equals(action.getActorCellId(), sessionInfo.getLocalPlayerCellId())) {
//                    continue;
//                }
//
//                // 如果该动作已处理，则跳过
//                if (SelfGameSession.getProcessedActionIds().contains(action.getId())) {
//                    continue;
//                }
//
//                if ("pick".equalsIgnoreCase(action.getType()) && !SelfGameSession.isSelected()) {
//                    log.info("本人选择英雄, actionId: {}", action.getId());
//                    SelfGameSession.addProcessedActionIds(action.getId());
//                    if (clientCfg.getAutoPickChampID() != null && clientCfg.getAutoPickChampID() > 0) {
//                        pickChampion(clientCfg.getAutoPickChampID(), action.getId());
//                        SelfGameSession.setIsSelected(true);
//                        // 找到操作后，跳出循环，防止重复处理
//                        break;
//                    }
//                } else if ("ban".equalsIgnoreCase(action.getType()) && !SelfGameSession.isBaned()) {
//                    log.info("本人禁用英雄, actionId: {}", action.getId());
//                    if (clientCfg.getAutoBanChampID() != null && clientCfg.getAutoBanChampID() > 0) {
//                        banChampion(clientCfg.getAutoBanChampID(), action.getId());
//                        SelfGameSession.setIsBanned(true);
//                        SelfGameSession.addProcessedActionIds(action.getId());
//                        // 找到操作后，跳出循环，防止重复处理
//                        break;
//                    }
//                }
//                if (SelfGameSession.isBaned() && SelfGameSession.isSelected()) {
//                    log.info("已经选择完并且仅用了。跳过");
//                    return;
//                }
//
//            }
//        }
    }

    // 用于记录已处理过的动作，避免重复打印日志
    private Set<String> processedActions = new HashSet<>();

    public void analyzeSession(ChampSelectSessionInfo session) {
        int localPlayerCellId = session.getLocalPlayerCellId();
        List<List<ChampSelectSessionInfo.Action>> actions = session.getActions();


        List<ChampSelectSessionInfo.Player> myTeam = session.getMyTeam();
        Map<Integer, String> positionMap = myTeam.stream().collect(Collectors.toMap(ChampSelectSessionInfo.Player::getCellId, ChampSelectSessionInfo.Player::getAssignedPosition));

        for (List<ChampSelectSessionInfo.Action> actions1 : actions) {
            for (ChampSelectSessionInfo.Action action : actions1) {
                String type = action.getType();
                int actorCellId = action.getActorCellId();
                int championId = action.getChampionId();
                boolean completed = action.getCompleted();
                Boolean isAllyAction = action.getIsAllyAction();
                Integer id = action.getId();

                String actionKey = actorCellId + "_" + id + "_" + type + completed + action.getIsInProgress();

                if (completed && !processedActions.contains(actionKey)) {
                    processedActions.add(actionKey);
                    String actionType = "pick".equals(type) ? "选择（锁定）英雄" : "禁用英雄";
                    String logMessage = String.format(
                            "【%s】位置：%s, 动作: %s, 英雄: %s, 是否本人: %s",
                            isAllyAction ? "我方" : "敌方",
                            GameEnums.Position.getDescByValue(positionMap.get(actorCellId)),
                            actionType, Heros.getNameById(championId), (actorCellId == localPlayerCellId) ? "是" : "否"
                    );
                    System.out.println(logMessage);
                } else if (!completed && actorCellId == localPlayerCellId) {
                    if ("ban".equals(type)) {
//                        System.out.println("本人环节禁用英雄: " + championId);
                        banChampion(clientCfg.getAutoBanChampID(), action.getId());
                        SelfGameSession.setIsBanned(true);
                        SelfGameSession.addProcessedActionIds(action.getId());
                        action.setCompleted(true);
                    } else if ("pick".equals(type)) {
//                        System.out.println("本人环节选择英雄: " + championId);
                        action.setCompleted(true);
                    }
                }
            }
        }
    }

    // 获取动作类型的描述
    private static String getActionType(String type, boolean completed) {
        if ("pick".equals(type)) {
            return completed ? "选择（锁定）英雄" : "预选英雄";
        } else if ("ban".equals(type)) {
            return completed ? "禁用英雄" : "预选禁用英雄";
        } else {
            return "未知动作";
        }
    }

    // 自动禁用英雄的方法（示例实现）
    private static void banChampion(int championId) {
        // 这里应实现与游戏客户端或API的交互，自动禁用英雄
        System.out.println("自动禁用英雄: " + championId);
    }

    // 自动选择英雄的方法（示例实现）
    private static void pickChampion(int championId) {
        // 这里应实现与游戏客户端或API的交互，自动选择英雄
        System.out.println("自动选择英雄: " + championId);
    }

    /**
     * 解析预选英雄
     */
    private static List<ChampSelectSessionInfo.Player> getPreHeros(List<ChampSelectSessionInfo.Player> myTeam) {
        return myTeam.stream().filter(item -> item.getChampionPickIntent() == 0 && item.getChampionId() == 0).collect(Collectors.toList());
    }


    // 预选英雄
    public void prePickChampion(int championId, int actionId) {
        champSelectPatchAction(championId, actionId, null, null);
    }

    // 确认选择
    public void pickChampion(int championId, int actionId) {
        champSelectPatchAction(
                championId,
                actionId,
                Constant.CHAMP_SELECT_PATCH_TYPE_PICK,
                true
        );
    }

    // 禁用英雄
    public void banChampion(int championId, int actionId) {
        champSelectPatchAction(
                championId,
                actionId,
                Constant.CHAMP_SELECT_PATCH_TYPE_BAN,
                true
        );
    }


    /**
     * 通用英雄选择操作
     */
    public void champSelectPatchAction(int championId, int actionId, String patchType, Boolean completed) {
        Map<String, Object> body = new HashMap<>();
        body.put("championId", championId);
        Optional.ofNullable(patchType).ifPresent(t -> body.put("type", t));
        Optional.ofNullable(completed).ifPresent(c -> body.put("completed", c));
        Request request = OkHttpUtil.createOkHttpPatchRequest("/lol-champ-select/v1/session/actions/" + actionId, body);

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("champSelectPatchActionError: {}", response);
            }
        } catch (Exception e) {
            log.error("champSelectPatchActionIOError", e);
        }
    }


}
