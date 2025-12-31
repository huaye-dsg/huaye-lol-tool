package com.example.huayeloltool.service;

import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.enums.Heros;
import com.example.huayeloltool.model.cache.CustomGameCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 结果格式化服务
 * 
 * <p>负责格式化输出结果，包括：
 * - 队伍信息格式化
 * - 控制台输出格式化
 * - KDA信息展示
 */
@Slf4j
@Service
public class ResultFormatter {

    /**
     * 格式化队伍信息输出
     * 
     * @param title 标题（如"我方队友"、"敌方对手"）
     * @param teamList 队伍成员列表
     * @return 格式化后的字符串
     */
    public static String formatTeamInfo(String title, List<CustomGameCache.Item> teamList) {
        if (teamList == null || teamList.isEmpty()) {
            return String.format("\n【%s 队伍信息】\n暂无数据\n", title);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n").append("=".repeat(60)).append("\n");
        sb.append(String.format("【%s 队伍信息】", title)).append("\n");

        for (int i = 0; i < teamList.size(); i++) {
            CustomGameCache.Item item = teamList.get(i);
            
            // 基本信息行
            sb.append(String.format("%d. %s [%s分] %s %s\n",
                    i + 1,
                    item.getHorse(),
                    item.getScore(),
                    item.getRank(),
                    item.getSummonerName()
            ));

            // KDA信息行
            appendKdaInfo(sb, item.getCurrKDA());
            sb.append("\n");
        }
        
        sb.append("=".repeat(60)).append("\n");
        return sb.toString();
    }

    /**
     * 添加KDA信息到字符串构建器
     * 
     * @param sb 字符串构建器
     * @param kdaList KDA详情列表
     */
    private static void appendKdaInfo(StringBuilder sb, List<CustomGameCache.KdaDetail> kdaList) {
        if (kdaList == null || kdaList.isEmpty()) {
            return;
        }

        // 最多显示3场游戏的KDA
        int displayCount = Math.min(kdaList.size(), Constant.BRIEF_KDA_DISPLAY);
        List<CustomGameCache.KdaDetail> kdaDetails = kdaList.subList(0, displayCount);
        
        for (CustomGameCache.KdaDetail kdaDetail : kdaDetails) {
            sb.append(String.format("%s-%s-%s-%s  ",
                    kdaDetail.getQueueGame(),
                    kdaDetail.getWin() ? "胜" : "负",
                    Heros.getNameById(kdaDetail.getChampionId()),
                    formatKda(kdaDetail.getKills(), kdaDetail.getDeaths(), kdaDetail.getAssists())
            ));
        }
    }

    /**
     * 格式化KDA字符串
     * 
     * @param kills 击杀数
     * @param deaths 死亡数
     * @param assists 助攻数
     * @return 格式化的KDA字符串（如"10/2/8"）
     */
    private static String formatKda(Integer kills, Integer deaths, Integer assists) {
        return String.format("%d/%d/%d", 
                kills != null ? kills : 0,
                deaths != null ? deaths : 0, 
                assists != null ? assists : 0
        );
    }

    /**
     * 记录队伍分析完成的日志
     * 
     * @param title 队伍标题
     * @param teamList 队伍列表
     */
    public static void logTeamAnalysisComplete(String title, List<CustomGameCache.Item> teamList) {
        if (teamList != null && !teamList.isEmpty()) {
            log.info("{}分析完成，共{}人", title, teamList.size());
        }
    }
}