package codes.ry.xanadu.chatgpt;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.models.Model;
import com.openai.models.models.ModelListPage;
import java.util.ArrayList;
import java.util.List;

final class ChatGptClient {
  private final OpenAIClient client;
  private final String model;

  ChatGptClient(String apiKey, String model) {
    this.client = OpenAIOkHttpClient.builder().apiKey(apiKey).build();
    this.model = model;
  }

  ChatResult chat(List<ChatCompletionMessageParam> messages) {
    try {
      ChatCompletionCreateParams request =
          ChatCompletionCreateParams.builder()
              .model(model)
              .messages(messages)
              .build();
      ChatCompletion result = client.chat().completions().create(request);
      if (result.choices() == null || result.choices().isEmpty()) {
        return new ChatResult(null, null);
      }
      ChatCompletion.Choice choice = result.choices().get(0);
      String content = choice.message().content().orElse(null);
      String finishReason = choice.finishReason().asString();
      return new ChatResult(content, finishReason);
    } catch (Exception e) {
      return new ChatResult(null, null);
    }
  }

  List<String> listModels() {
    try {
      ModelListPage page = client.models().list();
      List<String> names = new ArrayList<>();
      for (Model model : page.data()) {
        String id = model.id();
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
