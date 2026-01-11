package org.backendbrilliance.aitutor.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    @Value("classpath:prompts/tutor.st")
    private Resource sbPromptTemplate;

    public ChatService(ChatClient.Builder chatClientBuilder,
                       ChatMemory chatMemory, VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        // Add a memory advisor to the chat client
        var chatMemoryAdvisor = MessageChatMemoryAdvisor
                .builder(chatMemory)
                .build();

        // Build the chat client
        chatClient = chatClientBuilder
                .defaultAdvisors(chatMemoryAdvisor)
                .build();
    }

    public Flux<String> chatStream(String message, String chatId) {

        PromptTemplate template = new PromptTemplate(sbPromptTemplate);
        Map<String, Object> promptParam = new HashMap<>();
        promptParam.put("input", message);
        promptParam.put("documents", String.join("\n", findSimilarDocuments(message)));

        return chatClient.prompt(template.create(promptParam))
                .advisors(advisorSpec ->
                        advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId)
                )
                .stream()
                .content();
    }

    private List<String> findSimilarDocuments(String message) {
        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.builder().query(message).topK(3).build()
        );
        return similarDocuments.stream()
                .map(Document::getText).toList();
    }
}
