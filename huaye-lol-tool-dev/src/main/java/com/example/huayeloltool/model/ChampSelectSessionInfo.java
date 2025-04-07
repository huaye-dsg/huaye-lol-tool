package com.example.huayeloltool.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
public class ChampSelectSessionInfo extends CommonResp {
    private List<List<Action>> actions;
    private int localPlayerCellId;

    @Data
    public static class Action {
        private Integer actorCellId;
        private Integer championId;
        private Boolean completed;
        private Integer id;
        private Boolean isAllyAction;
        private Boolean isInProgress;
        private Integer pickTurn;
        private String champSelectPatchType;

    }
}

