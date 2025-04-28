package com.example.huayeloltool.chatgpt;//package com.example.huayeloltool.chatgpt;
//
//import com.volcengine.ark.runtime.model.Usage;
//import com.volcengine.ark.runtime.model.completion.chat.*;
//import com.volcengine.ark.runtime.service.ArkService;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class DouBaoApi {
//
//
//    //<dependency>
//    //    <groupId>com.volcengine</groupId>
//    //    <artifactId>volcengine-java-sdk-ark-runtime</artifactId>
//    //    <version>0.2.3</version>
//    //    </dependency>
//    //
//
//    static String apiKey = "";
//    static String mode1 = "doubao-1-5-lite-32k-250115";
//    static String mode2 = "ep-20250417170708-8zf98";
//
//
//    public static void main(String[] args) {
//        // 从环境变量中获取API密钥
//
//        // 创建ArkService实例
//        ArkService arkService = ArkService.builder().apiKey(apiKey).build();
//
//        // 初始化消息列表
//        List<ChatMessage> chatMessages = new ArrayList<>();
//
//        // 创建Prompt消息
//        ChatMessage promptMessage = ChatMessage.builder()
//                .role(ChatMessageRole.USER) // 设置消息角色为用户
//                .content("你是一名英雄联盟职业赛事分析师，需要根据双方阵容提供以下四方面的详细建议：\n" +
//                        "英雄推荐：基于阵容克制、版本强度（S14）、团队容错率，推荐1-3个最优选择\n" +
//                        "召唤师技能：标注技能组合的战术意图（如「闪现/点燃：强化对线压制」）\n" +
//                        "天赋配置：主系+副系符文，注明关键符文选择逻辑（如「征服者：配合持续输出」）\n" +
//                        "打法策略：分阶段说明（对线期/中期团战/后期决策），包含视野布控、资源争夺目标和敌方突破口分析." +
//                        "英雄对抗数据可参考网址：https://op.gg/champions 实时数据") // 设置消息内容
//
//                .build();
//
//
//        // 创建用户消息
//        ChatMessage userMessage = ChatMessage.builder()
//                .role(ChatMessageRole.USER) // 设置消息角色为用户
//                .content("【我方阵容】 \n" +
//                        "上单：诺克萨斯之手\n" +
//                        "打野：德玛西亚皇子\n" +
//                        "中单：我。还没有选英雄\n" +
//                        "ADC：寒冰射手\n" +
//                        "辅助：蕾欧娜\n" +
//                        "\n" +
//                        "【敌方阵容】\n" +
//                        "上单：荒漠屠夫\n" +
//                        "打野：盲僧\n" +
//                        "中单：发条魔灵\n" +
//                        "ADC：莎弥拉\n" +
//                        "辅助：泰坦") // 设置消息内容
//                .build();
//
//        // 添加到消息列表
//        chatMessages.add(promptMessage);
//        chatMessages.add(userMessage);
//
//        // 一次性请求
//        single(chatMessages, arkService);
//
//
//        // 流式返回，拒绝等待
//        //stream(chatMessages, arkService);
//
//    }
//
//    //private static void stream(List<ChatMessage> chatMessages, ArkService arkService) {
//    //    ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
//    //            .model(mode2) // 需要替换为Model ID
//    //            .stream(Boolean.TRUE)
//    //            .messages(chatMessages) // 设置消息列表
//    //            .build();
//    //    try {
//    //        arkService.streamChatCompletion(chatCompletionRequest)
//    //                .forEach(choice -> {
//    //                    List<ChatCompletionChoice> choices = choice.getChoices();
//    //                    if (choices != null && !choices.isEmpty()) {
//    //                        ChatCompletionChoice chatCompletionChoice = choices.get(0);
//    //                        ChatMessage message = chatCompletionChoice.getMessage();
//    //                        String content = message.getContent().toString();
//    //                        System.out.print(content);
//    //                    }
//    //                });
//    //    } catch (Exception e) {
//    //        System.out.println("请求失败: " + e.getMessage());
//    //    } finally {
//    //        // 关闭服务执行器
//    //        arkService.shutdownExecutor();
//    //    }
//    //}
//
//    private static void single(List<ChatMessage> chatMessages, ArkService arkService) {
//        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
//                .model(mode1)// 需要替换为Model ID
//                //.maxTokens(100)
//                .messages(chatMessages) // 设置消息列表
//                .build();
//
//        // 发送聊天完成请求并打印响应
//        try {
//            // 获取响应并打印每个选择的消息内容
//            ChatCompletionResult chatCompletion = arkService.createChatCompletion(chatCompletionRequest);
//            Usage usage = chatCompletion.getUsage();
//            long totalTokens = usage.getTotalTokens();
//            System.out.println("本次回答耗费总Tokens: " + totalTokens);
//
//            chatCompletion.getChoices()
//                    .forEach(choice -> System.out.println(choice.getMessage().getContent()));
//        } catch (Exception e) {
//            System.out.println("请求失败: " + e.getMessage());
//        } finally {
//            // 关闭服务执行器
//            arkService.shutdownExecutor();
//        }
//    }
//}
