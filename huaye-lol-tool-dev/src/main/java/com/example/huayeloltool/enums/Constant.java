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
