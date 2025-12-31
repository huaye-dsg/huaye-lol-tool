package com.example.huayeloltool.service;

import com.alibaba.fastjson2.JSON;

import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.enums.Heros;
import lombok.extern.slf4j.Slf4j;
import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.model.game.CustomGameSession;
import com.example.huayeloltool.model.base.GameGlobalSetting;
import com.example.huayeloltool.model.champion.ChampSelectSessionInfo;
import com.example.huayeloltool.model.champion.ChampionMastery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 英雄选择阶段事件处理器
 *
 * <p>负责解析 WebSocket 推送的英雄选择/禁用事件，并执行自动 ban/pick 操作。
 *
 * <p>线程安全说明：
 * LOL 客户端的 WebSocket 会频繁推送 ChampSelect 事件（每秒可能多次），
 * 这些事件会被 {@link MessageRouter} 分发到虚拟线程中并发处理。
 * 因此，自动 ban/pick 操作必须保证线程安全，避免重复执行。
 *
 * <p>解决方案：
 * 使用 {@link CustomGameSession#tryMarkBanned()} 和 {@link CustomGameSession#tryMarkSelected()}
 * 的 CAS 原子操作，确保只有一个线程能成功执行 ban/pick。
 */
@Slf4j
@Service
public class ChampionSelectHandler {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private GameGlobalSetting gameGlobalSetting;
    @Autowired
    LcuApiService lcuApiService;

    CustomGameSession customGameSession = CustomGameSession.getInstance();

    /**
     * 处理英雄选定/禁用事件
     */
    public void onChampSelectSessionUpdate(String sessionStr) {
        ChampSelectSessionInfo session = JSON.parseObject(sessionStr, ChampSelectSessionInfo.class);
        Map<Integer, ChampSelectSessionInfo.Player> positionMap = customGameSession.getPositionMap();
        List<ChampSelectSessionInfo.Player> myTeam = session.getMyTeam();

        // 懒初始化队友映射
        customGameSession.initPositionMapIfEmpty(myTeam);

        int localCellId = session.getLocalPlayerCellId();

        for (List<ChampSelectSessionInfo.Action> round : session.getActions()) {
            for (ChampSelectSessionInfo.Action action : round) {
                boolean completed = action.getCompleted();
                // 处理队友和对手锁定英雄的操作
                if (completed && action.getChampionId() > 0) {
                    String actionKey = buildActionKey(action);
                    if (customGameSession.markActionProcessed(actionKey)) {
                        handleCompletedAction(action, positionMap);
                    }
                    continue;
                }

                // 只处理自己、且未完成的操作。选择或禁用英雄
                if (!completed && action.getActorCellId() == localCellId && action.getIsInProgress()) {
                    String actionKey = buildActionKey(action);
                    if (customGameSession.markActionProcessed(actionKey)) {
                        handleSelfAction(action, actionKey);
                    }
                }
            }
        }
    }


    private void handleCompletedAction(ChampSelectSessionInfo.Action action, Map<Integer, ChampSelectSessionInfo.Player> posMap) {
        boolean isPick = "pick".equals(action.getType());
        boolean isAlly = action.getIsAllyAction();

        if (!(isPick && isAlly)) {
            // 只关注我方选择英雄
            return;
        }

        String positionDesc = Optional.ofNullable(posMap.get(action.getActorCellId()))
                .map(p -> GameEnums.Position
                        .getDescByValue(p.getAssignedPosition()))
                .orElse("未知");
        String which = isPick ? "选择（锁定）英雄" : "禁用英雄";
        String heroName = Heros.getNameById(action.getChampionId());

        String logMsg = isAlly
                //? String.format("【我方】位置：%s, 动作: %s, 英雄: %s", positionDesc, which, heroName)
                ? String.format("【我方】动作: %s, 英雄: %s", which, heroName)
                : String.format("【敌方】动作: %s, 英雄: %s", which, heroName);

        if (isPick && isAlly) {
            ChampSelectSessionInfo.Player player = posMap.get(action.getActorCellId());
            // 分析该玩家对这个英雄的熟练度
            logMsg = analyzeHeroMastery(player.getPuuid(), logMsg, action.getChampionId());
        }
        log.info(logMsg);
    }


    /**
     * 处理自己的 ban/pick 操作（自动禁用/选择英雄）
     *
     * <p>线程安全实现说明：
     *
     * <p>问题背景：
     * WebSocket 事件可能在短时间内多次触发（LOL 客户端每秒推送多次状态更新），
     * 如果使用简单的 if (!isBanned) { doBan(); isBanned = true; } 模式，
     * 多个线程可能同时通过 if 检查，导致重复调用 ban 接口。
     *
     * <p>解决方案：
     * 使用 {@link CustomGameSession#tryMarkBanned()} 的 CAS（Compare-And-Swap）操作：
     * - 该方法内部调用 AtomicBoolean.compareAndSet(false, true)
     * - 只有当前值为 false 时才能设置为 true 并返回 true
     * - 如果当前值已经是 true（其他线程已设置），则返回 false
     * - 这是一个原子操作，保证只有一个线程能成功
     *
     * <p>执行流程：
     * 1. 线程 A 和线程 B 同时进入 handleSelfAction
     * 2. 线程 A 调用 tryMarkBanned()，CAS 成功，返回 true，继续执行 ban 操作
     * 3. 线程 B 调用 tryMarkBanned()，CAS 失败（值已是 true），返回 false，跳过执行
     * 4. 如果线程 A 的 ban 操作失败，调用 resetBanned() 回滚状态，允许后续重试
     *
     * @param action    当前的选择/禁用动作信息
     * @param actionKey 动作唯一标识，用于去重和日志追踪
     */
    private void handleSelfAction(ChampSelectSessionInfo.Action action, String actionKey) {
        String type = action.getType();
        int actionId = action.getId();

        switch (type) {
            case "ban":
                // 检查是否配置了自动 ban 英雄
                if (gameGlobalSetting.getAutoBanChampID() <= 0) {
                    return;
                }

                // 使用 CAS 原子操作尝试获取 ban 的执行权
                // 只有第一个成功调用 tryMarkBanned() 的线程才能继续执行
                // 其他并发线程会因为 CAS 失败而直接返回，避免重复 ban
                if (!customGameSession.tryMarkBanned()) {
                    log.debug("已有其他线程在执行 ban 操作，跳过本次执行，actionKey: {}", actionKey);
                    return;
                }

                // 使用虚拟线程延迟执行，避免阻塞 WebSocket 事件处理线程
                // 延迟是为了等待 LOL 客户端 UI 完全加载，提高操作成功率
                Thread.startVirtualThread(() -> {
                    try {
                        Thread.sleep(Constant.BAN_DELAY_MS);
                        log.info("执行自动禁用英雄，英雄ID: {}，actionKey: {}",
                                gameGlobalSetting.getAutoBanChampID(), actionKey);

                        boolean success = lcuApiService.banChampion(gameGlobalSetting.getAutoBanChampID(), actionId);
                        if (!success) {
                            // ban 失败，重置状态以允许后续重试
                            // 同时将 actionKey 标记为未处理，使下次事件能重新触发
                            log.warn("自动禁用英雄失败，已重置状态，action: {}", JSON.toJSONString(action));
                            customGameSession.resetBanned();
                            customGameSession.markActionUnProcessed(actionKey);
                        } else {
                            log.info("自动禁用英雄成功");
                        }
                    } catch (InterruptedException e) {
                        // 线程被中断，重置状态
                        customGameSession.resetBanned();
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        // 其他异常，重置状态以允许重试
                        log.error("自动禁用英雄时发生异常", e);
                        customGameSession.resetBanned();
                        customGameSession.markActionUnProcessed(actionKey);
                    }
                });
                break;

            case "pick":
                // 检查是否配置了自动选择英雄
                if (gameGlobalSetting.getAutoPickChampID() <= 0) {
                    return;
                }

                // 使用 CAS 原子操作尝试获取 pick 的执行权
                // 原理同上，保证只有一个线程能执行选人操作
                if (!customGameSession.tryMarkSelected()) {
                    log.debug("已有其他线程在执行 pick 操作，跳过本次执行，actionKey: {}", actionKey);
                    return;
                }

                try {
                    log.info("执行自动选择英雄，英雄ID: {}", gameGlobalSetting.getAutoPickChampID());
                    lcuApiService.pickChampion(gameGlobalSetting.getAutoPickChampID(), actionId);
                    // 注意：pick 操作不需要延迟，因为选人阶段时间紧迫
                } catch (Exception e) {
                    // pick 失败，重置状态以允许重试
                    log.error("自动选择英雄失败", e);
                    customGameSession.resetSelected();
                }
                break;

            default:
                // 其他类型（如 ten_bans_reveal）忽略
                break;
        }
    }

    // 抽取 actionKey 构建
    private String buildActionKey(ChampSelectSessionInfo.Action action) {
        return String.join("_",
                String.valueOf(action.getActorCellId()),
                String.valueOf(action.getId()),
                action.getType(),
                String.valueOf(action.getCompleted()),
                String.valueOf(action.getIsInProgress())
        );
    }


    private String analyzeHeroMastery(String puuid, String logMessage, int championId) {
        // 获取玩家的所有英雄熟练度数据
        List<ChampionMastery> championMasteryList = lcuApiService.searchChampionMasteryData(puuid);

        // 查找特定英雄的熟练度信息
        Optional<ChampionMastery> optionalMastery = championMasteryList.stream()
                .filter(mastery -> mastery.getChampionId() == championId)
                .findFirst();

        // 如果找到匹配的英雄熟练度信息，则更新日志消息
        if (optionalMastery.isPresent()) {
            ChampionMastery mastery = optionalMastery.get();
            logMessage += String.format(", 等级: %d, 积分: %d，最后游玩: %s",
                    mastery.championLevel, mastery.championPoints,
                    convertTimestampToDate(mastery.lastPlayTime));
        }

        return logMessage;
    }

    /**
     * 把毫秒时间戳转为年月日
     */
    private String convertTimestampToDate(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DATE_FORMATTER);
    }


}
