package com.example.huayeloltool.service;

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
//        log.info("游戏选择会话变更： {}", JSON.toJSONString(sessionInfo));
//        log.info("游戏选择会话变更");

        Integer localPlayerCellId = sessionInfo.getLocalPlayerCellId();
        SelfGameSession.setFloor(localPlayerCellId);

        List<ChampSelectSessionInfo.Player> myTeams = sessionInfo.getMyTeam();
        if (CollectionUtils.isNotEmpty(myTeams)) {
            for (ChampSelectSessionInfo.Player player : myTeams) {
                if (Objects.equals(player.getCellId(), localPlayerCellId)) {
                    SelfGameSession.setPosition(GameEnums.Position.getDescByValue(player.getAssignedPosition()));
                    break;
                }
            }
        }


        List<ChampSelectSessionInfo.Player> myTeam = sessionInfo.getMyTeam();
        List<Integer> collect = myTeam.stream().filter(item -> item.getChampionId() != null && item.getChampionId() > 0).map(item -> item.getChampionId()).collect(Collectors.toList());
        List<String> selfHeroNames = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(collect)) {
            selfHeroNames = collect.stream().map(Heros::getNameById).collect(Collectors.toList());
        }


        List<String> selfPreHeroNames = myTeam.stream().filter(item -> item.getChampionPickIntent() != null && item.getChampionPickIntent() > 0).map(item -> Heros.getNameById(item.getChampionId())).collect(Collectors.toList());


        List<ChampSelectSessionInfo.Player> theirTeam = sessionInfo.getTheirTeam();
        List<Integer> collec2t = theirTeam.stream().filter(item -> item.getChampionId() != null && item.getChampionId() > 0).map(item -> item.getChampionId()).collect(Collectors.toList());
        List<String> enmtyHeroNames = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(collec2t)) {
            enmtyHeroNames = collec2t.stream().map(Heros::getNameById).collect(Collectors.toList());
        }

        List<String> theirPreHeroNames = theirTeam.stream().filter(item -> item.getChampionPickIntent() != null && item.getChampionPickIntent() > 0).map(item -> Heros.getNameById(item.getChampionId())).collect(Collectors.toList());

//        log.info("敌方puuid：{}", theirTeam.stream().map(ChampSelectSessionInfo.Player::getPuuid).collect(Collectors.toList()));


        List<String> selfBansNames = sessionInfo.getBans().getMyTeamBans().stream().map(Heros::getNameById).collect(Collectors.toList());
        List<String> theirBansNames = sessionInfo.getBans().getTheirTeamBans().stream().map(Heros::getNameById).collect(Collectors.toList());
        log.info("【游戏信息】游戏模式：{}，位置：{}，楼层：{}楼。\n " +
                        "我方阵容英雄为：{} \n " +
                        "我方禁用英雄为：{} \n " +
                        "我方预选英雄为：{} \n " +
                        "敌方阵容英雄为：{} \n " +
                        "敌方禁用英雄为：{} \n " +
                        "敌方预选英雄为：{} \n ",
                GameEnums.GameQueueID.getGameNameMap(SelfGameSession.getQueueId()),
                GameEnums.Position.getDescByValue(SelfGameSession.getPosition()),
                SelfGameSession.getFloor() + 1,
                selfHeroNames,
                selfBansNames,
                selfPreHeroNames,
                enmtyHeroNames,
                theirBansNames,
                theirPreHeroNames
        );


        if (SelfGameSession.isBaned() && SelfGameSession.isSelected()) {
            log.info("已经选择完并且仅用了。跳过");
            return;
        }


        // 先查找当前进行中的动作组
        List<ChampSelectSessionInfo.Action> currentActionGroup = null;
        if (sessionInfo.getActions() != null && !sessionInfo.getActions().isEmpty()) {
            for (List<ChampSelectSessionInfo.Action> actionList : sessionInfo.getActions()) {
                for (ChampSelectSessionInfo.Action action : actionList) {
                    if (action.getIsInProgress()) {
                        currentActionGroup = actionList;
                        break;
                    }
                }
                if (currentActionGroup != null) {
                    break;
                }
            }
        }


        // 如果找到了正在进行中的动作组，只处理这一组的动作
        if (currentActionGroup != null) {
            for (ChampSelectSessionInfo.Action action : currentActionGroup) {
                // 检查是否为当前玩家的操作
                if (!Objects.equals(action.getActorCellId(), sessionInfo.getLocalPlayerCellId())) {
                    continue;
                }

                // 如果该动作已处理，则跳过
                if (SelfGameSession.getProcessedActionIds().contains(action.getId())) {
                    continue;
                }

                if (action.getIsInProgress() && !action.getCompleted()) {
                    if ("pick".equalsIgnoreCase(action.getType()) && !SelfGameSession.isSelected()) {
                        log.info("本人选择英雄, actionId: {}", action.getId());
                        SelfGameSession.addProcessedActionIds(action.getId());
                        if (clientCfg.getAutoPickChampID() != null && clientCfg.getAutoPickChampID() > 0) {
                            pickChampion(clientCfg.getAutoPickChampID(), action.getId());
                            SelfGameSession.setIsSelected(true);
                            // 找到操作后，跳出循环，防止重复处理
                            break;
                        }
                    } else if ("ban".equalsIgnoreCase(action.getType()) && !SelfGameSession.isBaned()) {
                        log.info("本人禁用英雄, actionId: {}", action.getId());
                        if (clientCfg.getAutoBanChampID() != null && clientCfg.getAutoBanChampID() > 0) {
                            banChampion(clientCfg.getAutoBanChampID(), action.getId());
                            SelfGameSession.setIsBanned(true);
                            SelfGameSession.addProcessedActionIds(action.getId());
                            // 找到操作后，跳出循环，防止重复处理
                            break;
                        }
                    }
                    if (SelfGameSession.isBaned() && SelfGameSession.isSelected()) {
                        log.info("已经选择完并且仅用了。跳过");
                        return;
                    }
                }
            }
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
