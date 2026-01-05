package codes.ry.xanadu.command;

import java.util.ArrayList;
import java.util.List;

public final class CommandHistory {
  private static final String HISTORY_KEY = "__xanadu.command_history";

  private CommandHistory() {}

  public enum Source {
    USER,
    AGENT
  }

  public record Entry(Source source, String command, String output, Boolean success) {}

  public static void recordUser(CommandContext context, String command) {
    record(context, Source.USER, command, "", null);
  }

  public static void recordUser(CommandContext context, String command, boolean success) {
    record(context, Source.USER, command, "", success);
  }

  public static void recordAgent(CommandContext context, String command, String output, boolean success) {
    record(context, Source.AGENT, command, output, success);
  }

  public static List<Entry> snapshot(CommandContext context) {
    return new ArrayList<>(entries(context));
  }

  @SuppressWarnings("unchecked")
  private static List<Entry> entries(CommandContext context) {
    Object existing = context.get(HISTORY_KEY);
    if (existing instanceof List) {
      return (List<Entry>) existing;
    }
    List<Entry> entries = new ArrayList<>();
    context.put(HISTORY_KEY, entries);
    return entries;
  }

  private static void record(
      CommandContext context, Source source, String command, String output, Boolean success) {
    if (context == null || source == null || command == null || command.isBlank()) {
      return;
    }
    String normalizedOutput = output == null ? "" : output;
    entries(context).add(new Entry(source, command.trim(), normalizedOutput, success));
  }
}
