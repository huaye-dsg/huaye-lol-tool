package com.example.huayeloltool.model;

import lombok.Data;

import java.util.List;


@Data
public class TeamUsersInfo {
    private String conversationId;
    private List<Long> summonerIdList;

    public TeamUsersInfo(String conversationId, List<Long> summonerIdList) {
        this.conversationId = conversationId;
        this.summonerIdList = summonerIdList;
    }
}
