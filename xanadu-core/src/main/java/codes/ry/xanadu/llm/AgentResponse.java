package codes.ry.xanadu.llm;

public record AgentResponse(String text, AgentFinishReason finishReason) {
  public static AgentResponse of(String text, AgentFinishReason finishReason) {
    AgentFinishReason reason = finishReason == null ? AgentFinishReason.OTHER : finishReason;
    return new AgentResponse(text, reason);
  }
}
