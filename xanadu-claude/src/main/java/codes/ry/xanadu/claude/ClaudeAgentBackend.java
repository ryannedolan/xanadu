package codes.ry.xanadu.claude;

import codes.ry.xanadu.llm.AgentBackend;
import codes.ry.xanadu.llm.AgentFinishReason;
import codes.ry.xanadu.llm.AgentMessage;
import codes.ry.xanadu.llm.AgentResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClaudeAgentBackend implements AgentBackend {
  private static final String API_KEY_ENV = "ANTHROPIC_API_KEY";

  @Override
  public String id() {
    return "claude";
  }

  @Override
  public String displayName() {
    return "Claude";
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
    ClaudeClient client = new ClaudeClient(apiKey);
    return client.listModels();
  }

  @Override
  public AgentResponse chat(List<AgentMessage> messages, String model) {
    String apiKey = System.getenv(API_KEY_ENV);
    if (apiKey == null || apiKey.isBlank()) {
      return null;
    }
    ClaudeClient client = new ClaudeClient(apiKey);
    String system = systemPrompt(messages);
    List<Map<String, Object>> payload = new ArrayList<>();
    for (AgentMessage message : messages) {
      String role = message.role();
      if ("system".equalsIgnoreCase(role)) {
        continue;
      }
      String normalized = role.toLowerCase(Locale.ROOT);
      String anthropicRole = "assistant".equals(normalized) ? "assistant" : "user";
      payload.add(Map.of("role", anthropicRole, "content", message.content()));
    }
    ClaudeClient.ClaudeResponse response = client.chat(model, system, payload);
    if (response == null) {
      return AgentResponse.of(null, AgentFinishReason.OTHER);
    }
    return AgentResponse.of(response.text, mapFinishReason(response.stopReason));
  }

  private String systemPrompt(List<AgentMessage> messages) {
    StringBuilder sb = new StringBuilder();
    for (AgentMessage message : messages) {
      if (!"system".equalsIgnoreCase(message.role())) {
        continue;
      }
      if (sb.length() > 0) {
        sb.append('\n');
      }
      sb.append(message.content());
    }
    return sb.toString();
  }

  private AgentFinishReason mapFinishReason(String reason) {
    if (reason == null) {
      return AgentFinishReason.OTHER;
    }
    String normalized = reason.toLowerCase(Locale.ROOT);
    if (normalized.contains("max_tokens")) {
      return AgentFinishReason.LENGTH;
    }
    if (normalized.contains("stop")) {
      return AgentFinishReason.STOP;
    }
    return AgentFinishReason.OTHER;
  }
}
