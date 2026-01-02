package codes.ry.xanadu.chatgpt;

import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;
import io.github.sashirestela.openai.domain.model.Model;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class ChatGptClient {
  private final SimpleOpenAI client;
  private final String model;

  ChatGptClient(String apiKey, String model) {
    this.client = SimpleOpenAI.builder().apiKey(apiKey).build();
    this.model = model;
  }

  ChatResult chat(List<ChatMessage> messages) {
    ChatRequest request =
        ChatRequest.builder()
            .model(model)
            .messages(messages)
            .build();
    try {
      CompletableFuture<Chat> future = client.chatCompletions().create(request);
      Chat result = future.join();
      if (result.getChoices() == null || result.getChoices().isEmpty()) {
        return new ChatResult(null, null);
      }
      Chat.Choice choice = result.getChoices().get(0);
      ChatMessage.ResponseMessage message = choice.getMessage();
      String content = message == null ? null : message.getContent();
      return new ChatResult(content, choice.getFinishReason());
    } catch (Exception e) {
      return new ChatResult(null, null);
    }
  }

  List<String> listModels() {
    try {
      CompletableFuture<List<Model>> future = client.models().getList();
      List<Model> models = future.join();
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
    } catch (Exception e) {
      return List.of();
    }
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
