//package com.example.huayeloltool.chatgpt;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONObject;
//import okhttp3.*;
//import okio.BufferedSource;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.net.Proxy;
//import java.util.Collections;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class Google {
//
//
//    private static final String API_KEY = "";
//    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;
//
//    private static final String stream = "Https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContentStream";
//
//
//    private static OkHttpClient client = new OkHttpClient.Builder()
//            .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7890)))
//            .build();
//
//    public static void main(String[] args) throws IOException {
//
//
//        // JSON 请求体
//
//        String question = "我是一名java开发工程师，我现在已经利用LOL的lcu api实现了 自动接收对局，自动禁用英雄，查询队友和对手战绩，你还有什么其他有趣且实用的功能推荐吗？";
////        streamGeminiToConsole(question);
//        GeminiRequest.Content.Part part = new GeminiRequest.Content.Part(question);
//        GeminiRequest.Content content = new GeminiRequest.Content(Collections.singletonList(part));
//        GeminiRequest request = new GeminiRequest(Collections.singletonList(content));
//        String json = JSON.toJSONString(request);
//        RequestBody body = RequestBody.create(
//                json, MediaType.parse("application/json"));
//
//        Request okrequest = new Request.Builder()
//                .url(URL)
//                .post(body)
//                .addHeader("Content-Type", "application/json")
//                .build();
//
//        try (Response response = client.newCall(okrequest).execute()) {
//            if (response.isSuccessful() && response.body() != null) {
//                GeminiResponse geminiResponse = JSON.parseObject(response.body().string(), GeminiResponse.class);
//                String replyLongText = geminiResponse.getReplyLongText();
//                System.out.println("模型回复为：" + replyLongText);
//            } else {
//                System.err.println("Request failed: " + response.code());
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void streamContent() {
//        try (VertexAI vertexAI = new VertexAI(projectId, location)) {
//            GenerativeModel model = new GenerativeModel(modelName, vertexAI);
//
//            // 发送流式请求
//            model.generateContentStream("写一个关于猫的可爱故事。")
//                    .stream()
//                    .forEach(response -> {
//                        try {
//                            String text = getTextFromResponse(response);
//                            if (text != null && !text.isEmpty()) {
//                                System.out.print(text); // 流式打印到控制台
//                            }
//                        } catch (Exception e) {
//                            System.err.println("处理响应时出错: " + e.getMessage());
//                        }
//                    });
//            System.out.println(); // 换行
//        } catch (Exception e) {
//            System.err.println("流式传输失败: " + e.getMessage());
//        }
//    }
//
//    // 从响应中提取文本
//    private String getTextFromResponse(GenerateContentResponse response) {
//        try {
//            return response.getCandidatesList()
//                    .get(0)
//                    .getContent()
//                    .getPartsList()
//                    .get(0)
//                    .getText();
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//}
