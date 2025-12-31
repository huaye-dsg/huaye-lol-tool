package com.example.huayeloltool.model.game;

import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.model.cache.CustomGameCache;
import com.example.huayeloltool.model.champion.ChampSelectSessionInfo;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * 当前本局游戏信息
 * 使用单例模式管理游戏会话状态
 * 
 * <p>线程安全说明：
 * 由于 WebSocket 事件可能并发触发多次英雄选择/禁用事件，
 * 使用 {@link AtomicBoolean} 保证 ban/pick 操作的原子性，
 * 避免重复执行自动 ban 或自动 pick 操作。
 */
@Data
public class CustomGameSession {

    // 游戏模式
    private volatile Integer queueId;

    // 楼层。1 - 5 楼
    private volatile Integer floor;

    // 位置。例如中路
    private volatile String position;

    /**
     * 是否已经禁用了英雄
     * 
     * <p>使用 AtomicBoolean 而非 volatile Boolean 的原因：
     * volatile 只能保证可见性，无法保证"检查-执行"操作的原子性。
     * 例如：if (!isBanned) { doBan(); isBanned = true; }
     * 多个线程可能同时通过 if 检查，导致重复 ban 英雄。
     * 
     * <p>AtomicBoolean.compareAndSet(false, true) 是原子操作，
     * 只有一个线程能成功将 false 改为 true，其他线程会失败并跳过执行。
     */
    private final AtomicBoolean isBanned = new AtomicBoolean(false);

    /**
     * 是否已经选择了英雄
     * 
     * <p>同 isBanned，使用 AtomicBoolean 保证自动选人操作只执行一次
     */
    private final AtomicBoolean isSelected = new AtomicBoolean(false);

    // 已处理过的动作 ID
    private final Set<String> processedActionIds = ConcurrentHashMap.newKeySet();

    // 队友位置映射
    private Map<Integer, ChampSelectSessionInfo.Player> positionMap = new ConcurrentHashMap<>();


    // 使用 volatile 关键字保证可见性
    private static volatile CustomGameSession instance;

    // 私有构造函数，防止外部实例化
    private CustomGameSession() {
    }

    /**
     * 懒初始化队友位置映射
     * 
     * <p>线程安全说明：
     * 使用 synchronized 关键字保证多线程环境下的安全初始化。
     * 虽然使用了 ConcurrentHashMap，但 isEmpty() 检查和后续的 forEach 操作
     * 不是原子的，需要同步保护。
     * 
     * @param team 队友列表
     */
    public synchronized void initPositionMapIfEmpty(List<ChampSelectSessionInfo.Player> team) {
        if (positionMap.isEmpty()) {
            team.forEach(player -> positionMap.putIfAbsent(player.getCellId(), player));
        }
    }

    // 双重检查锁定实现单例模式
    public static CustomGameSession getInstance() {
        if (instance == null) {
            synchronized (CustomGameSession.class) {
                if (instance == null) {
                    instance = new CustomGameSession();
                }
            }
        }
        return instance;
    }

    public boolean markActionProcessed(String actionId) {
        return processedActionIds.add(actionId);
    }

    public void markActionUnProcessed(String actionId) {
        processedActionIds.remove(actionId);
    }

    /**
     * 尝试标记为已禁用状态（原子操作）
     * 
     * <p>使用 CAS（Compare-And-Swap）操作：
     * 只有当前值为 false 时才设置为 true 并返回 true，
     * 如果当前值已经是 true，则返回 false。
     * 
     * <p>这保证了即使多个线程同时调用，也只有一个线程能成功。
     * 
     * @return true 表示成功获取到 ban 的执行权，false 表示已有其他线程在执行
     */
    public boolean tryMarkBanned() {
        return isBanned.compareAndSet(false, true);
    }

    /**
     * 重置禁用状态（用于 ban 失败后的回滚）
     */
    public void resetBanned() {
        isBanned.set(false);
    }

    /**
     * 获取当前禁用状态
     */
    public boolean getIsBanned() {
        return isBanned.get();
    }

    /**
     * 尝试标记为已选择状态（原子操作）
     * 
     * @return true 表示成功获取到 pick 的执行权，false 表示已有其他线程在执行
     */
    public boolean tryMarkSelected() {
        return isSelected.compareAndSet(false, true);
    }

    /**
     * 重置选择状态（用于 pick 失败后的回滚）
     */
    public void resetSelected() {
        isSelected.set(false);
    }

    /**
     * 获取当前选择状态
     */
    public boolean getIsSelected() {
        return isSelected.get();
    }

    // 重置所有状态
    public void reset() {
        queueId = null;
        floor = null;
        position = null;
        isBanned.set(false);
        isSelected.set(false);
        processedActionIds.clear();
        positionMap.clear();
        CustomGameCache.clear();
    }

    // 是否是单双排排位
    public static boolean isSoloRank() {
        CustomGameSession session = getInstance();
        return Objects.equals(GameEnums.GameQueueID.RANK_SOLO.getId(), session.getQueueId());
    }
}
