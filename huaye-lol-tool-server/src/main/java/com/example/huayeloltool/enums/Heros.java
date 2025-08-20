package com.example.huayeloltool.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum Heros {

    ANNIE(1, "Annie", "安妮", "黑暗之女"),

    OLAF(2, "Olaf", "奥拉夫", "狂战士"),

    GALIO(3, "Galio", "加里奥", "正义巨像"),

    TWISTED_FATE(4, "TwistedFate", "崔斯特", "卡牌大师"),

    XIN_ZHAO(5, "XinZhao", "赵信", "德邦总管"),

    URGOT(6, "Urgot", "厄加特", "无畏战车"),

    LEBLANC(7, "Leblanc", "乐芙兰", "诡术妖姬"),

    VLADIMIR(8, "Vladimir", "弗拉基米尔", "猩红收割者"),

    FIDDLESTICKS(9, "FiddleSticks", "费德提克", "远古恐惧"),

    KAYLE(10, "Kayle", "凯尔", "正义天使"),

    MASTER_YI(11, "MasterYi", "易", "无极剑圣"),

    ALISTAR(12, "Alistar", "阿利斯塔", "牛头酋长"),

    RYZE(13, "Ryze", "瑞兹", "符文法师"),

    SION(14, "Sion", "赛恩", "亡灵战神"),

    SIVIR(15, "Sivir", "希维尔", "战争女神"),

    SORAKA(16, "Soraka", "索拉卡", "众星之子"),

    TEEMO(17, "Teemo", "提莫", "迅捷斥候"),

    TRISTANA(18, "Tristana", "崔丝塔娜", "麦林炮手"),

    WARWICK(19, "Warwick", "沃里克", "祖安怒兽"),

    NUNU(20, "Nunu", "努努", "雪原双子"),

    MISS_FORTUNE(21, "MissFortune", "厄运小姐", "赏金猎人"),

    ASHE(22, "Ashe", "艾希", "寒冰射手"),

    TRYNDAMERE(23, "Tryndamere", "泰达米尔", "蛮族之王"),

    JAX(24, "Jax", "贾克斯", "武器大师"),

    MORIGANA(25, "Morgana", "莫甘娜", "堕落天使"),

    ZILEAN(26, "Zilean", "基兰", "时光守护者"),

    SINGED(27, "Singed", "辛吉德", "炼金术士"),

    EVELYNN(28, "Evelynn", "伊芙琳", "痛苦之拥"),

    TWITCH(29, "Twitch", "图奇", "瘟疫之源"),

    KARTHUS(30, "Karthus", "卡尔萨斯", "死亡颂唱者"),

    CHOGATH(31, "Chogath", "科加斯", "虚空恐惧"),

    AMUMU(32, "Amumu", "阿木木", "殇之木乃伊"),

    RAMMUS(33, "Rammus", "拉莫斯", "披甲龙龟"),

    ANIVIA(34, "Anivia", "艾尼维亚", "冰晶凤凰"),

    SHACO(35, "Shaco", "萨科", "恶魔小丑"),

    DR_MUNDO(36, "DrMundo", "蒙多医生", "祖安狂人"),

    SONA(37, "Sona", "娑娜", "琴瑟仙女"),

    KASSADIN(38, "Kassadin", "卡萨丁", "虚空行者"),

    IRELIA(39, "Irelia", "艾瑞莉娅", "刀锋舞者"),

    JANNA(40, "Janna", "迦娜", "风暴之怒"),

    GANGPLANK(41, "Gangplank", "普朗克", "海洋之灾"),

    CORKI(42, "Corki", "库奇", "英勇投弹手"),

    KARMA(43, "Karma", "卡尔玛", "天启者"),

    TARIC(44, "Taric", "塔里克", "瓦洛兰之盾"),

    VEIGAR(45, "Veigar", "维迦", "邪恶小法师"),

    TRUNDLE(48, "Trundle", "特朗德尔", "巨魔之王"),

    SWAIN(50, "Swain", "斯维因", "诺克萨斯统领"),

    CAITLYN(51, "Caitlyn", "凯特琳", "皮城女警"),

    BLITZCRANK(53, "Blitzcrank", "布里茨", "蒸汽机器人"),

    MALPHITE(54, "Malphite", "墨菲特", "熔岩巨兽"),

    KATARINA(55, "Katarina", "卡特琳娜", "不祥之刃"),

    NOCTURNE(56, "Nocturne", "魔腾", "永恒梦魇"),

    MAOKAI(57, "Maokai", "茂凯", "扭曲树精"),

    RENEKTON(58, "Renekton", "雷克顿", "荒漠屠夫"),

    JARVAN_IV(59, "JarvanIV", "嘉文四世", "德玛西亚皇子"),

    ELISE(60, "Elise", "伊莉丝", "蜘蛛女皇"),

    ORIANNA(61, "Orianna", "奥莉安娜", "发条魔灵"),

    WUKONG(62, "MonkeyKing", "孙悟空", "齐天大圣"),

    BRAND(63, "Brand", "布兰德", "复仇焰魂"),

    LEE_SIN(64, "LeeSin", "李青", "盲僧"),

    VAYNE(67, "Vayne", "薇恩", "暗夜猎手"),

    RUMBLE(68, "Rumble", "兰博", "机械公敌"),

    CASSIOPEIA(69, "Cassiopeia", "卡西奥佩娅", "魔蛇之拥"),

    SKARNER(72, "Skarner", "斯卡纳", "上古领主"),

    HEIMERDINGER(74, "Heimerdinger", "黑默丁格", "大发明家"),

    NASUS(75, "Nasus", "内瑟斯", "沙漠死神"),

    NIDALEE(76, "Nidalee", "奈德丽", "狂野女猎手"),

    UDYR(77, "Udyr", "乌迪尔", "兽灵行者"),

    POPPY(78, "Poppy", "波比", "圣锤之毅"),

    GRAGAS(79, "Gragas", "古拉加斯", "酒桶"),

    PANTHEON(80, "Pantheon", "潘森", "不屈之枪"),

    EZREAL(81, "Ezreal", "伊泽瑞尔", "探险家"),

    MORDEKAISER(82, "Mordekaiser", "莫德凯撒", "铁铠冥魂"),

    YORICK(83, "Yorick", "约里克", "牧魂人"),

    AKALI(84, "Akali", "阿卡丽", "离群之刺"),

    KENNEN(85, "Kennen", "凯南", "狂暴之心"),

    GAREN(86, "Garen", "盖伦", "德玛西亚之力"),

    LEONA(89, "Leona", "蕾欧娜", "曙光女神"),

    MALZAHAR(90, "Malzahar", "玛尔扎哈", "虚空先知"),

    TALON(91, "Talon", "泰隆", "刀锋之影"),

    RIVEN(92, "Riven", "锐雯", "放逐之刃"),

    KOG_MAW(96, "KogMaw", "克格莫", "深渊巨口"),

    SHEN(98, "Shen", "慎", "暮光之眼"),

    LUX(99, "Lux", "拉克丝", "光辉女郎"),

    XERATH(101, "Xerath", "泽拉斯", "远古巫灵"),

    SHYVANA(102, "Shyvana", "希瓦娜", "龙血武姬"),

    AHRI(103, "Ahri", "阿狸", "九尾妖狐"),

    GRAVES(104, "Graves", "格雷福斯", "法外狂徒"),

    FIZZ(105, "Fizz", "菲兹", "潮汐海灵"),

    VOLIBEAR(106, "Volibear", "沃利贝尔", "不灭狂雷"),

    RENGAR(107, "Rengar", "雷恩加尔", "傲之追猎者"),

    VARUS(110, "Varus", "韦鲁斯", "惩戒之箭"),

    NAUTILUS(111, "Nautilus", "诺提勒斯", "深海泰坦"),

    VIKTOR(112, "Viktor", "维克托", "奥术先驱"),

    SEJUANI(113, "Sejuani", "瑟庄妮", "北地之怒"),

    FIORA(114, "Fiora", "菲奥娜", "无双剑姬"),

    ZIGGS(115, "Ziggs", "吉格斯", "爆破鬼才"),

    LULU(117, "Lulu", "璐璐", "仙灵女巫"),

    DRAVEN(119, "Draven", "德莱文", "荣耀行刑官"),

    HECARIM(120, "Hecarim", "赫卡里姆", "战争之影"),

    KHAZIX(121, "Khazix", "卡兹克", "虚空掠夺者"),

    DARIUS(122, "Darius", "德莱厄斯", "诺克萨斯之手"),

    JAYCE(126, "Jayce", "杰斯", "未来守护者"),

    LISSANDRA(127, "Lissandra", "丽桑卓", "冰霜女巫"),

    DIANA(131, "Diana", "黛安娜", "皎月女神"),

    QUINN(133, "Quinn", "奎因", "德玛西亚之翼"),

    SYNDRA(134, "Syndra", "辛德拉", "暗黑元首"),

    AURELION_SOL(136, "AurelionSol", "奥瑞利安索尔", "铸星龙王"),

    KAYN(141, "Kayn", "凯隐", "影流之镰"),

    ZOE(142, "Zoe", "佐伊", "暮光星灵"),

    ZYRA(143, "Zyra", "婕拉", "荆棘之兴"),

    KAISA(145, "Kaisa", "卡莎", "虚空之女"),

    SERAPHINE(147, "Seraphine", "萨勒芬妮", "星籁歌姬"),

    GNAR(150, "Gnar", "纳尔", "迷失之牙"),

    ZAC(154, "Zac", "扎克", "生化魔人"),

    YASUO(157, "Yasuo", "亚索", "疾风剑豪"),

    VELKOZ(161, "Velkoz", "维克兹", "虚空之眼"),

    TALIYAH(163, "Taliyah", "塔莉垭", "岩雀"),

    CAMILLE(164, "Camille", "卡蜜尔", "青钢影"),

    AKSHAN(166, "Akshan", "阿克尚", "影哨"),

    BELVETH(200, "Belveth", "卑尔维斯", "虚空女皇"),

    BRAUM(201, "Braum", "布隆", "弗雷尔卓德之心"),

    JHIN(202, "Jhin", "烬", "戏命师"),

    KINDRED(203, "Kindred", "千珏", "永猎双子"),

    ZERI(221, "Zeri", "泽丽", "祖安花火"),

    JINX(222, "Jinx", "金克丝", "暴走萝莉"),

    TAHM_KENCH(223, "TahmKench", "塔姆", "河流之王"),

    BRIAR(233, "Briar", "贝蕾亚", "狂厄蔷薇"),

    VIEGO(234, "Viego", "佛耶戈", "破败之王"),

    SENNA(235, "Senna", "赛娜", "涤魂圣枪"),

    LUCIAN(236, "Lucian", "卢锡安", "圣枪游侠"),

    ZED(238, "Zed", "劫", "影流之主"),

    KLED(240, "Kled", "克烈", "暴怒骑士"),

    EKKO(245, "Ekko", "艾克", "时间刺客"),

    QIYANA(246, "Qiyana", "奇亚娜", "元素女皇"),

    VI(254, "Vi", "蔚", "皮城执法官"),

    AATROX(266, "Aatrox", "亚托克斯", "暗裔剑魔"),

    NAMI(267, "Nami", "娜美", "唤潮鲛姬"),

    AZIR(268, "Azir", "阿兹尔", "沙漠皇帝"),

    YUUMI(350, "Yuumi", "悠米", "魔法猫咪"),

    SAMIRA(360, "Samira", "莎弥拉", "沙漠玫瑰"),

    THRESH(412, "Thresh", "锤石", "魂锁典狱长"),

    ILLAOI(420, "Illaoi", "俄洛伊", "海兽祭司"),

    REK_SAI(421, "RekSai", "雷克塞", "虚空遁地兽"),

    IVERN(427, "Ivern", "艾翁", "翠神"),

    KALISTA(429, "Kalista", "卡莉丝塔", "复仇之矛"),

    BARD(432, "Bard", "巴德", "星界游神"),

    RAKAN(497, "Rakan", "洛", "幻翎"),

    XAYAH(498, "Xayah", "霞", "逆羽"),

    ORNN(516, "Ornn", "奥恩", "山隐之焰"),

    SYLAS(517, "Sylas", "塞拉斯", "解脱者"),

    NEEKO(518, "Neeko", "妮蔻", "万花通灵"),

    APHELIOS(523, "Aphelios", "厄斐琉斯", "残月之肃"),

    RELL(526, "Rell", "芮尔", "镕铁少女"),

    PYKE(555, "Pyke", "派克", "血港鬼影"),

    VEX(711, "Vex", "薇古丝", "愁云使者"),

    YONE(777, "Yone", "永恩", "封魔剑魂"),

    AMBESSA(799, "Ambessa", "安蓓萨", "铁血狼母"),

    MEL(800, "Mel", "梅尔", "流光镜影"),

    SETT(875, "Sett", "瑟提", "腕豪"),

    LILLIA(876, "Lillia", "莉莉娅", "含羞蓓蕾"),

    GWEN(887, "Gwen", "格温", "灵罗娃娃"),

    RENATA(888, "Renata", "烈娜塔·戈拉斯克", "炼金男爵"),

    AURORA(893, "Aurora", "阿萝拉", "双界灵兔"),

    NILAH(895, "Nilah", "尼菈", "不羁之悦"),

    KSANTE(897, "KSante", "奎桑提", "纳祖芒荣耀"),

    SMOLDER(901, "Smolder", "斯莫德", "炽炎雏龙"),

    MILIO(902, "Milio", "米利欧", "明烛"),

    HWEI(910, "Hwei", "彗", "异画师"),

    NAAFIRI(950, "Naafiri", "纳亚菲利", "百裂冥犬");


    private final Integer heroId;
    private final String name;
    private final String alias;
    private final String title;

    private static final Map<Integer, String> valueToNicknameMap = new HashMap<>();

    static {
        for (Heros option : Heros.values()) {
            valueToNicknameMap.put(option.getHeroId(), option.getTitle());
        }
    }

    public static String getNameById(int id) {
        return valueToNicknameMap.get(id);
    }

    private static final Map<Integer, String> heroAliasMap = new HashMap<>();

    static {
        for (Heros option : Heros.values()) {
            heroAliasMap.put(option.getHeroId(), option.getName());
        }
    }

    public static String getAliasById(int id) {
        return heroAliasMap.get(id);
    }

    public static String getImageById(int id) {
        return "http://game.gtimg.cn/images/lol/act/img/champion/" + heroAliasMap.get(id) + ".png";
    }
}
