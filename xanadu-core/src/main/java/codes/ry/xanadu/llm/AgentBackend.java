package codes.ry.xanadu.llm;

import java.util.List;

public interface AgentBackend {
  String id();

  String displayName();

  boolean isConfigured();

  String missingConfigMessage();

  List<String> listModels();

  AgentResponse chat(List<AgentMessage> messages, String model);

  default String normalizeResponse(String response) {
    return response;
  }
}
