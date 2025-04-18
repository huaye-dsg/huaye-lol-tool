package com.example.huayeloltool.chatgpt;

import lombok.Data;

import java.util.List;

/**
 * Gemini API 响应结构封装类
 * 可用于接收模型返回的内容、消耗统计等
 */
@Data
public class GeminiResponse {

    /**
     * 候选回复内容（通常只取第一个）
     */
    private List<Candidate> candidates;

    /**
     * Token 使用统计信息
     */
    private UsageMetadata usageMetadata;

    /**
     * 返回的模型版本信息，例如：gemini-2.0-flash
     */
    private String modelVersion;

    /**
     * 提取第一个候选内容的纯文本回复（用于快速获取回答）
     *
     * @return 模型返回的文本内容；如果为空，返回 null
     */
    public String getReplyText() {
        if (candidates != null && !candidates.isEmpty()) {
            Candidate candidate = candidates.get(0);
            if (candidate.getContent() != null
                    && candidate.getContent().getParts() != null
                    && !candidate.getContent().getParts().isEmpty()) {
                return candidate.getContent().getParts().get(0).getText();
            }
        }
        return null;
    }

    /**
     * 提取完整的模型回复文本（拼接所有 parts）
     *
     * @return 模型返回的完整文本内容；如果为空返回 null
     */
    public String getReplyLongText() {
        if (candidates != null && !candidates.isEmpty()) {
            Candidate candidate = candidates.get(0);
            if (candidate.getContent() != null
                    && candidate.getContent().getParts() != null
                    && !candidate.getContent().getParts().isEmpty()) {

                // 拼接所有 parts 的 text
                StringBuilder fullText = new StringBuilder();
                for (Part part : candidate.getContent().getParts()) {
                    if (part.getText() != null) {
                        fullText.append(part.getText());
                    }
                }
                return fullText.toString().trim();
            }
        }
        return null;
    }


    // ================= 子类定义 =================

    /**
     * 模型候选回复结构
     */
    @Data
    public static class Candidate {

        /**
         * 实际的回复内容（含文本、角色等）
         */
        private Content content;

        /**
         * 模型终止生成的原因：
         * - STOP：正常完成
         * - MAX_TOKENS：达到最大 token 限制
         * - SAFETY：触发安全机制，终止生成
         * - ERROR：生成出错
         */
        private String finishReason;

        /**
         * 平均对数概率（用于评估输出质量，一般不使用）
         */
        private Double avgLogprobs;
    }

    /**
     * 回复内容结构（包含文本和角色）
     */
    @Data
    public static class Content {

        /**
         * 回复的片段列表，可能有多个片段（通常只有一个）
         */
        private List<Part> parts;

        /**
         * 内容的来源角色：
         * - user：用户输入
         * - model：模型输出
         */
        private String role;
    }

    /**
     * 回复的具体片段内容（例如纯文本）
     */
    @Data
    public static class Part {

        /**
         * 具体文本内容，如："你好，请问我能帮你什么？"
         */
        private String text;
    }

    /**
     * API 使用统计信息（token 计数）
     */
    @Data
    public static class UsageMetadata {

        /**
         * 用户 prompt 消耗的 token 数
         */
        private int promptTokenCount;

        /**
         * 模型回复消耗的 token 数
         */
        private int candidatesTokenCount;

        /**
         * 总 token 消耗数
         */
        private int totalTokenCount;

        /**
         * Prompt token 的详细信息（按类型分类）
         */
        private List<TokensDetail> promptTokensDetails;

        /**
         * 回复部分的 token 详细信息
         */
        private List<TokensDetail> candidatesTokensDetails;
    }

    /**
     * Token 明细结构
     */
    @Data
    public static class TokensDetail {

        /**
         * 模态类型（目前一般为 "TEXT"，未来可能支持图片、音频等）
         */
        private String modality;

        /**
         * 当前类型的 token 数量
         */
        private int tokenCount;
    }
}
