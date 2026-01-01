package codes.ry.xanadu.gemini;

import codes.ry.xanadu.llm.AgentBackend;
import codes.ry.xanadu.llm.AgentFinishReason;
import codes.ry.xanadu.llm.AgentMessage;
import codes.ry.xanadu.llm.AgentResponse;
import com.google.genai.types.FinishReason;
import com.google.genai.types.GenerateContentResponse;
import java.util.List;

public final class GeminiAgentBackend implements AgentBackend {
  private static final String API_KEY_ENV = "GOOGLE_API_KEY";

  @Override
  public String id() {
    return "gemini";
  }

  @Override
  public String displayName() {
    return "Gemini";
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
    GeminiClient client = new GeminiClient(apiKey, "gemini-2.0-flash-lite");
    return client.listModels();
  }

  @Override
  public AgentResponse chat(List<AgentMessage> messages, String model) {
    String apiKey = System.getenv(API_KEY_ENV);
    if (apiKey == null || apiKey.isBlank()) {
      return null;
    }
    GeminiClient client = new GeminiClient(apiKey, model);
    StringBuilder prompt = new StringBuilder();
    for (AgentMessage message : messages) {
      String role = message.role();
      String prefix = switch (role) {
        case "system" -> "System: ";
        case "assistant" -> "Assistant: ";
        default -> "User: ";
      };
      prompt.append(prefix).append(message.content());
      prompt.append('\n');
    }
    GenerateContentResponse response = client.generate(prompt.toString().trim());
    if (response == null) {
      return AgentResponse.of(null, AgentFinishReason.OTHER);
    }
    String text = response.text();
    AgentFinishReason finishReason = mapFinishReason(response.finishReason());
    return AgentResponse.of(text, finishReason);
  }

  @Override
  public String normalizeResponse(String response) {
    if (response == null) {
      return null;
    }
    String[] lines = response.split("\\R", -1);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      if (line.startsWith("Assistant:")) {
        line = line.substring("Assistant:".length()).trim();
      }
      sb.append(line);
      if (i + 1 < lines.length) {
        sb.append('\n');
      }
    }
    return sb.toString();
  }

  private AgentFinishReason mapFinishReason(FinishReason reason) {
    if (reason == null) {
      return AgentFinishReason.OTHER;
    }
    FinishReason.Known known = reason.knownEnum();
    if (known == FinishReason.Known.MAX_TOKENS) {
      return AgentFinishReason.LENGTH;
    }
    if (known == FinishReason.Known.STOP) {
      return AgentFinishReason.STOP;
    }
    return AgentFinishReason.OTHER;
  }
}
