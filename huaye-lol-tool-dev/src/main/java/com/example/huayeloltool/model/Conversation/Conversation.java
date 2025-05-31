package com.example.huayeloltool.model.Conversation;

import com.example.huayeloltool.enums.GameEnums;
import lombok.Data;

@Data
public class Conversation {
//    private String gameName;
//    private String gameTag;
    private String id;
//    private String inviterId;
//    private boolean isMuted;
//    private Object lastMessage;  // 根据需要调整类型
//    private String name;
//    private String password;
//    private String pid;
//    private String targetRegion;
    private GameEnums.GameFlow type;
//    private int unreadMessageCount;

}
