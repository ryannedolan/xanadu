package codes.ry.xanadu.chatgpt;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.model.Model;
import com.theokanning.openai.service.OpenAiService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

final class ChatGptClient {
  private final OpenAiService service;
  private final String model;

  ChatGptClient(String apiKey, String model) {
    this.service = new OpenAiService(apiKey, Duration.ofSeconds(60));
    this.model = model;
  }

  ChatResult chat(List<ChatMessage> messages) {
    ChatCompletionRequest request =
        ChatCompletionRequest.builder()
            .model(model)
            .messages(messages)
            .build();
    ChatCompletionResult result = service.createChatCompletion(request);
    if (result.getChoices() == null || result.getChoices().isEmpty()) {
      return new ChatResult(null, null);
    }
    ChatCompletionChoice choice = result.getChoices().get(0);
    ChatMessage message = choice.getMessage();
    String content = message == null ? null : message.getContent();
    return new ChatResult(content, choice.getFinishReason());
  }

  List<String> listModels() {
    List<Model> models = service.listModels();
    List<String> names = new ArrayList<>();
    for (Model model : models) {
      String id = model.getId();
      if (id == null || id.isBlank()) {
        continue;
      }
      if (isChatCapable(id)) {
        names.add(id);
      }
    }
    names.sort(String::compareToIgnoreCase);
    return names;
  }

  private static boolean isChatCapable(String id) {
    String normalized = id.toLowerCase();
    return normalized.startsWith("gpt-")
        || normalized.startsWith("o1")
        || normalized.startsWith("o3")
        || normalized.startsWith("chatgpt");
  }

  static final class ChatResult {
    final String content;
    final String finishReason;

    private ChatResult(String content, String finishReason) {
      this.content = content;
      this.finishReason = finishReason;
    }
  }
}
