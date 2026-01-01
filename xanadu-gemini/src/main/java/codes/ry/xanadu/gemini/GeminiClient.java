package codes.ry.xanadu.gemini;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.ListModelsConfig;
import com.google.genai.types.Model;
import java.util.ArrayList;
import java.util.List;

final class GeminiClient {
  private final Client client;
  private final String model;

  GeminiClient(String apiKey, String modelName) {
    this.client = Client.builder().apiKey(apiKey).build();
    this.model = modelName;
  }

  GenerateContentResponse generate(String prompt) {
    GenerateContentConfig config = GenerateContentConfig.builder().build();
    return client.models.generateContent(model, prompt, config);
  }

  List<String> listModels() {
    ListModelsConfig config = ListModelsConfig.builder().build();
    List<String> names = new ArrayList<>();
    for (Model model : client.models.list(config)) {
      if (!isChatCapable(model)) {
        continue;
      }
      String name = model.name().orElse("");
      if (name.startsWith("models/")) {
        name = name.substring("models/".length());
      }
      if (name.isBlank()) {
        name = model.displayName().orElse("");
      }
      if (!name.isBlank()) {
        names.add(name);
      }
    }
    names.sort(String::compareToIgnoreCase);
    return names;
  }

  private static boolean isChatCapable(Model model) {
    if (model.supportedActions().isEmpty()) {
      return true;
    }
    for (String action : model.supportedActions().orElse(List.of())) {
      String normalized = action.toLowerCase();
      if (normalized.contains("generatecontent") || normalized.contains("chat")) {
        return true;
      }
    }
    return false;
  }
}
