package com.example.huayeloltool.enums;


public class Constant {

    public static final String LOL_UX_PROCESS_NAME = "LeagueClientUx";

    /**
     * 加入房间消息
     */
    public static final String JOINED_ROOM_MSG = "joined_room";

    /**
     * 系统类型的会话消息
     */
    public static final String CONVERSATION_MSG_TYPE_SYSTEM = "system";

    /**
     * 选择英雄补丁类型 - 选择
     */
    public static final String CHAMP_SELECT_PATCH_TYPE_PICK = "pick";

    /**
     * 选择英雄补丁类型 - 禁用
     */
    public static final String CHAMP_SELECT_PATCH_TYPE_BAN = "ban";

    /**
     * 离线状态
     */
    public static final String AVAILABILITY_OFFLINE = "offline";

    public static final Double DEFAULT_SCORE = 100.0;

    public static final String SCORE_RESULT = "【%s】【%d分】%s: %s %s ";
    public static final String KDA_FORMAT = "[%s-%s-%s-%d/%d/%d]";


    public static final String WIN_STR = "胜";
    public static final String LOSE_STR = "败";

    public static final String[] HORSE_NAME_CONF = {"通天代", "小代", "上等马", "中等马", "下等马", "牛 马"};

    /**
     * 默认查询对局数量
     */
    public static final int DEFAULT_GAME_HISTORY_LIMIT = 20;

    /**
     * 最小有效对局时长（秒）- 过滤重开等无效对局
     */
    public static final int MIN_VALID_GAME_DURATION = 300;

    /**
     * 操作延迟时间（毫秒）- 用于自动接受、自动ban等操作
     */
    public static final long ACTION_DELAY_MS = 1500;

    /**
     * 自动ban英雄延迟时间（毫秒）
     */
    public static final long BAN_DELAY_MS = 2000;

    /**
     * 得分计算权重配置
     */
    public static final double RECENT_GAME_WEIGHT = 0.7;  // 近期对局权重（24小时内）
    public static final double OTHER_GAME_WEIGHT = 0.3;   // 其他对局权重
    public static final int RECENT_GAME_HOURS = 24;       // 近期对局时间范围（小时）

    /**
     * 游戏分析相关常量
     */
    public static final int STANDARD_TEAM_SIZE = 5;       // 标准队伍人数
    public static final int ANALYSIS_GAME_COUNT = 3;      // 分析连胜连败的游戏数量
    public static final int MAX_KDA_DISPLAY = 5;          // 最多显示的KDA记录数
    public static final int BRIEF_KDA_DISPLAY = 3;        // 简要显示的KDA记录数

    /**
     * 腾讯官方英雄信息
     */
   public static final String TENCENT_HERO_LIST = "http://game.gtimg.cn/images/lol/act/img/js/heroList/hero_list.js";

    /**
     * 腾讯官方英雄图标
     */
    public static final String TENCENT_HERO_IMAGE = "http://game.gtimg.cn/images/lol/act/img/champion/Annie.png";

    /**
     * 拳头官方英雄信息
     */
    public static final String RIOT_HERO_LIST = "https://ddragon.leagueoflegends.com/cdn/14.12.1/data/zh_CN/champion.json";

    // OPGGapi
    public static final String OPGG_API = "https://lol-api-champion.op.gg/api/KR/champions/ranked/${championId}/${position}";

    // LCUAPIswagger
    public static final String LCU_API_SWAGGER = "https://lcu.kebs.dev/swagger.html";
}
