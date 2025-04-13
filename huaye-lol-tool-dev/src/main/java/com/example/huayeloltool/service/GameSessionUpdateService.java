package com.example.huayeloltool.service;

import com.example.huayeloltool.config.OkHttpUtil;
import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.enums.Heros;
import com.example.huayeloltool.model.ChampSelectSessionInfo;
import com.example.huayeloltool.model.DefaultClientConf;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class GameSessionUpdateService {

    private final DefaultClientConf clientCfg = DefaultClientConf.getInstance();

    @Autowired
    @Qualifier(value = "unsafeOkHttpClient")
    private OkHttpClient client;

    public void onChampSelectSessionUpdate(ChampSelectSessionInfo sessionInfo) {
//        log.info("游戏选择会话变更： {}", JSON.toJSONString(sessionInfo));
//        log.info("游戏选择会话变更");
        int userPickActionId = 0;
        int userBanActionId = 0;
        int pickChampionId = 0;
        boolean isSelfPick = false;
        boolean isSelfBan = false;
        boolean pickIsInProgress = false;
        boolean banIsInProgress = false;
        LinkedHashSet<Integer> alloyPrePickSet = new LinkedHashSet<>(5);

        LinkedHashSet<String> selfTeamHeros = new LinkedHashSet<>(5);
        LinkedHashSet<String> enemyTeamHeros = new LinkedHashSet<>(5);
        LinkedHashSet<String> selfBanNames = new LinkedHashSet<>(5);
        LinkedHashSet<String> enemyBanNames = new LinkedHashSet<>(5);


        // 处理Actions数据
        if (sessionInfo.getActions() != null && !sessionInfo.getActions().isEmpty()) {
            for (List<ChampSelectSessionInfo.Action> actionList : sessionInfo.getActions()) {
                for (ChampSelectSessionInfo.Action action : actionList) {
                    boolean allyAction = action.getIsAllyAction();
                    Integer championId = action.getChampionId();

                    if (allyAction) {
                        if (championId > 0) {
                            if (Objects.equals(action.getType(), "ban") && action.getCompleted()) {
//                                log.info("友方禁用英雄：{}", Heros.getNameById(championId));
                                selfBanNames.add(Heros.getNameById(championId));
                            }
                            if (Objects.equals(action.getType(), "pick") && action.getCompleted()) {
//                                log.info("友方选择英雄：{}", Heros.getNameById(championId));
                                selfTeamHeros.add(Heros.getNameById(championId));
                            } else if (Objects.equals(action.getType(), "pick") && !action.getCompleted()) {
//                                log.info("友方预选英雄：{}", Heros.getNameById(championId));
                                alloyPrePickSet.add(championId);
                            }
                        } else {
//                            log.info("友方操作环节：不涉及英雄, action = {}", JSON.toJSONString(action));
                        }
                    } else {
                        if (championId > 0) {
                            if (Objects.equals(action.getType(), "ban") && action.getCompleted()) {
//                                log.info("敌方禁用英雄：{}", Heros.getNameById(championId));
                                enemyBanNames.add(Heros.getNameById(championId));
                            }
                            if (Objects.equals(action.getType(), "pick") && action.getCompleted()) {
//                                log.info("敌方选择英雄：{}", Heros.getNameById(championId));
                                enemyTeamHeros.add(Heros.getNameById(championId));
                            } else if (Objects.equals(action.getType(), "pick") && !action.getCompleted()) {
//                                log.info("敌方预选英雄：{}", Heros.getNameById(championId));
                            }
                        } else {
//                            log.info("敌方操作环节：不涉及英雄, action = {}", JSON.toJSONString(action));
                        }
                    }


                    // 检查当前玩家动作
                    if (action.getActorCellId() != sessionInfo.getLocalPlayerCellId()) {
                        continue;
                    }

//                    log.info("本人操作环节：{},LocalPlayerCellId: {},ActorCellId: {}", JSON.toJSONString(action), sessionInfo.getLocalPlayerCellId(), action.getActorCellId());
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
        for (Integer id : alloyPrePickSet) {
            if (null != id) {
                preNames.add(Heros.getNameById(id));
            }
        }

        if (CollectionUtils.isNotEmpty(preNames)) {
//            log.info("预选名单为: {}", preNames);
        }

        // 自动选择英雄
        if (clientCfg.getAutoPickChampID() > 0 && isSelfPick) {
//            log.info("进入本人操作阶段");
            if (pickIsInProgress) {
//                log.info("本人正在选择英雄...");
                pickChampion(clientCfg.getAutoPickChampID(), userPickActionId);
            } else if (pickChampionId == 0) {
//                log.info("本人正在预选英雄...");
//                prePickChampion(clientCfg.getAutoPickChampID(), userPickActionId);
            }
        }

        // 自动禁用英雄
        if (clientCfg.getAutoBanChampID() > 0 && isSelfBan && banIsInProgress) {
//            log.info("本人正在禁用英雄，预选名单为: {}", preNames);
            if (!alloyPrePickSet.contains(clientCfg.getAutoBanChampID())) {
//                log.info("预选名单不包含将要禁用的英雄：{}, 可以禁用", Heros.getNameById(clientCfg.getAutoBanChampID()));
                banChampion(clientCfg.getAutoBanChampID(), userBanActionId);
            } else {
//                log.info("预选名单包含将要禁用的英雄：{}, 取消禁用", Heros.getNameById(clientCfg.getAutoBanChampID()));
            }
        }
        if (CollectionUtils.isNotEmpty(selfTeamHeros)) {
            log.info("我方阵容为: {}", selfTeamHeros);
        }
        if (CollectionUtils.isNotEmpty(enemyTeamHeros)) {
            log.info("敌方阵容为: {}", enemyTeamHeros);
        }
        if (CollectionUtils.isNotEmpty(selfBanNames)) {
            log.info("我方ban英雄为: {}", selfBanNames);
        }
        if (CollectionUtils.isNotEmpty(enemyBanNames)) {
            log.info("敌方ban英雄为: {}", enemyBanNames);
        }
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
