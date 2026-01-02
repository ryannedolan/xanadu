package codes.ry.xanadu.chatgpt;

import codes.ry.xanadu.llm.AgentBackend;
import codes.ry.xanadu.llm.AgentFinishReason;
import codes.ry.xanadu.llm.AgentMessage;
import codes.ry.xanadu.llm.AgentResponse;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import java.util.ArrayList;
import java.util.List;

public final class ChatGptAgentBackend implements AgentBackend {
  private static final String API_KEY_ENV = "OPENAI_API_KEY";

  @Override
  public String id() {
    return "chatgpt";
  }

  @Override
  public String displayName() {
    return "ChatGPT";
  }

  @Override
  public boolean isConfigured() {
    String apiKey = System.getenv(API_KEY_ENV);
    return apiKey != null && !apiKey.isBlank();
  }

  @Override
  public String missingConfigMessage() {
    return API_KEY_ENV + " is not set.";
  }

  @Override
  public List<String> listModels() {
    String apiKey = System.getenv(API_KEY_ENV);
    if (apiKey == null || apiKey.isBlank()) {
      return List.of();
    }
    ChatGptClient client = new ChatGptClient(apiKey, "gpt-4o-mini");
    return client.listModels();
  }

  @Override
  public AgentResponse chat(List<AgentMessage> messages, String model) {
    String apiKey = System.getenv(API_KEY_ENV);
    if (apiKey == null || apiKey.isBlank()) {
      return null;
    }
    ChatGptClient client = new ChatGptClient(apiKey, model);
    List<ChatMessage> chatMessages = new ArrayList<>();
    for (AgentMessage message : messages) {
      ChatMessage chatMessage = convertMessage(message);
      if (chatMessage != null) {
        chatMessages.add(chatMessage);
      }
    }
    ChatGptClient.ChatResult result = client.chat(chatMessages);
    if (result == null) {
      return AgentResponse.of(null, AgentFinishReason.OTHER);
    }
    AgentFinishReason finishReason = mapFinishReason(result.finishReason);
    return AgentResponse.of(result.content, finishReason);
  }

  private ChatMessage convertMessage(AgentMessage message) {
    String role = message.role();
    String content = message.content();
    if (content == null || content.isBlank()) {
      return null;
    }
    if ("user".equalsIgnoreCase(role)) {
      return ChatMessage.UserMessage.of(content);
    } else if ("system".equalsIgnoreCase(role)) {
      return ChatMessage.SystemMessage.of(content);
    } else if ("assistant".equalsIgnoreCase(role)) {
      return ChatMessage.AssistantMessage.of(content);
    }
    // Default to user message for unknown roles
    return ChatMessage.UserMessage.of(content);
  }

  private AgentFinishReason mapFinishReason(String reason) {
    if (reason == null) {
      return AgentFinishReason.OTHER;
    }
    String normalized = reason.toLowerCase();
    if (normalized.contains("length")) {
      return AgentFinishReason.LENGTH;
    }
    if (normalized.contains("stop")) {
      return AgentFinishReason.STOP;
    }
    return AgentFinishReason.OTHER;
  }
}
