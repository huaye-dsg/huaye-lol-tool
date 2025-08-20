package com.example.huayeloltool.model.conversation;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMsg {

    /**
     * 消息内容
     */
    private String body;

//    /**
//     * 发送者ID
//     */
//    private String fromId;

    /**
     * 发送者平台ID
     */
//    private String fromPid;

    /**
     * 发送者召唤师ID
     */
    private long fromSummonerId;
//
//    /**
//     * 消息ID
//     */
//    private String id;
//
//    /**
//     * 是否为历史消息
//     */
//    private boolean isHistorical;
//
//    /**
//     * 时间戳
//     */
//    private LocalDateTime timestamp;

    /**
     * 消息类型
     */
    private String type;

}
