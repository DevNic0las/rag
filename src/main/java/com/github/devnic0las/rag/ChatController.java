package com.github.devnic0las.rag;



import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
@RestController
@RequestMapping("/chat")

public class ChatController {

  private final ChatClient chatClient;

  public ChatController(ChatClient.Builder builder, VectorStore vectorStore) {
    this.chatClient = builder
            .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore)
                    .searchRequest(SearchRequest.builder().topK(3).build())
                    .build())
            .build();
  }

  @PostMapping("/ask")
  public String ask(@RequestBody String question) {
    return chatClient.prompt().user(question).call().content();
  }

}
