package com.lab.ai.pusaman.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yang.nobel
 * @since 2026-06-12 15:33
 **/
@Configuration
public class RagConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel,
                                 ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultSystem("你是一个专业的英语老师，现在在对用户的中译英做评价，要根据给出的参考资料，评价用户的答案。" +
                        "只能使用参考资料中的答案作为参考答案，如果参考资料中没有，回答给用户**没有找到可参考的资料**，严禁自行翻译。" +
                        "如果用户回答不完全，指出差距在哪里，并给出参考答案。" +
                        "返回结果要求是一个标准的json格式，其中有两个字段，分别为j和r，对应「评价」和「参考答案」。不要有其他任何多余文本。" +
                        "比如，" +
                        "{\"j\":{你的评价},\"r\":{你的参考答案}} 。" +
                        "**参考答案**：你的参考答案。" +
                        "「评价」和「参考答案」都要满足markdown语法，使用加粗强调重点，" +
                        "多使用无序列表展示步骤，重要的数据或对比请用表格呈现。长段落之间请增加空行，保持排版清爽")
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

//    @Bean
//    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor() {
//        QueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
//                .allowEmptyContext(true)
//                .build();
//
//        return RetrievalAugmentationAdvisor.builder()
//                .queryAugmenter(queryAugmenter)
//                .build();
//    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel)
                .build();
    }
}
