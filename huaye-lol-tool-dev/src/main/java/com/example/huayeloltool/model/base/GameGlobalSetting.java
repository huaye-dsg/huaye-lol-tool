package com.example.huayeloltool.model.base;

import com.example.huayeloltool.enums.Heros;
import lombok.Getter;

@Getter
public class GameGlobalSetting {

    /**
     * 是否自动接受对局
     */
    private final Boolean autoAcceptGame = true;

    /**
     * 自动选择的英雄ID
     */
    private final Integer autoPickChampID = 0;

    /**
     * 自动禁用的英雄ID
     */
    private final Integer autoBanChampID = Heros.OUTLAW.getId();

    private static GameGlobalSetting instance;

    public static GameGlobalSetting getInstance() {
        if (instance == null) {
            instance = new GameGlobalSetting();
        }
        return instance;
    }

}
