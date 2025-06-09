package com.example.huayeloltool.model.base;

import com.example.huayeloltool.enums.Heros;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class GameGlobalSetting {

    /**
     * 是否自动接受对局
     */
    private Boolean autoAcceptGame = true;

    /**
     * 自动选择的英雄ID
     */
    private Integer autoPickChampID = 0;

    /**
     * 自动禁用的英雄ID
     */
    private Integer autoBanChampID = Heros.OUTLAW.getId();

    private Boolean autoBanChamp = true;

    private static GameGlobalSetting instance;

    public static GameGlobalSetting getInstance() {
        if (instance == null) {
            instance = new GameGlobalSetting();
        }
        return instance;
    }

}
