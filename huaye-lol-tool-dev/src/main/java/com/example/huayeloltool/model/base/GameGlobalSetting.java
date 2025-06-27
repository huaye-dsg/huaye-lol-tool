package com.example.huayeloltool.model.base;

import com.example.huayeloltool.enums.Heros;
import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
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
    private Integer autoBanChampID = Heros.GRAVES.getHeroId();

    private Boolean autoBanChamp = true;
}
