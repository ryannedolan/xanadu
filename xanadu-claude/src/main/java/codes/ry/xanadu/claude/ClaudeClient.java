package codes.ry.xanadu.claude;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ClaudeClient {
  private static final String API_URL = "https://api.anthropic.com/v1/messages";
  private static final String MODELS_URL = "https://api.anthropic.com/v1/models";
  private static final String API_VERSION = "2023-06-01";

  private final HttpClient client;
  private final ObjectMapper mapper;
  private final String apiKey;

  ClaudeClient(String apiKey) {
    this.apiKey = apiKey;
    this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    this.mapper = new ObjectMapper();
  }

  ClaudeResponse chat(String model, String system, List<Map<String, Object>> messages) {
    Map<String, Object> payload =
        Map.of(
            "model",
            model,
            "max_tokens",
            1024,
            "system",
            system == null ? "" : system,
            "messages",
            messages);
    try {
      String body = mapper.writeValueAsString(payload);
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(API_URL))
              .timeout(Duration.ofSeconds(60))
              .header("x-api-key", apiKey)
              .header("anthropic-version", API_VERSION)
              .header("content-type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(body))
              .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        return null;
      }
      JsonNode root = mapper.readTree(response.body());
      JsonNode content = root.path("content");
      String stopReason = root.path("stop_reason").asText(null);
      if (content.isArray() && content.size() > 0) {
        JsonNode first = content.get(0);
        JsonNode text = first.path("text");
        if (text.isTextual()) {
          return new ClaudeResponse(text.asText(), stopReason);
        }
      }
      return new ClaudeResponse(null, stopReason);
    } catch (IOException | InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  List<String> listModels() {
    List<String> names = new ArrayList<>();
    try {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(MODELS_URL))
              .timeout(Duration.ofSeconds(30))
              .header("x-api-key", apiKey)
              .header("anthropic-version", API_VERSION)
              .GET()
              .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        return names;
      }
      JsonNode root = mapper.readTree(response.body());
      JsonNode data = root.path("data");
      if (!data.isArray()) {
        return names;
      }
      for (JsonNode entry : data) {
        JsonNode id = entry.path("id");
        if (id.isTextual()) {
          String name = id.asText();
          if (!name.isBlank()) {
            names.add(name);
          }
        }
      }
      names.sort(String::compareToIgnoreCase);
      return names;
    } catch (IOException | InterruptedException e) {
      Thread.currentThread().interrupt();
      return names;
    }
  }

  static final class ClaudeResponse {
    final String text;
    final String stopReason;

    private ClaudeResponse(String text, String stopReason) {
      this.text = text;
      this.stopReason = stopReason;
    }
  }
}
