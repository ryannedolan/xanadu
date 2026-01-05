package codes.ry.xanadu.command;

import java.util.ArrayList;
import java.util.List;

public final class CommandHistory {
  private static final String HISTORY_KEY = "__xanadu.command_history";
  private static final String CHAIN_KEY = "__xanadu.agent_chain";
  private static final String CHAIN_PARENTS_KEY = "__xanadu.agent_chain_parents";

  private CommandHistory() {}

  public enum Source {
    USER,
    AGENT
  }

  public record Entry(Source source, String command, String output, Boolean success, String chainId) {}

  public static void recordUser(CommandContext context, String command) {
    record(context, Source.USER, command, "", null, null);
  }

  public static void recordUser(CommandContext context, String command, boolean success) {
    record(context, Source.USER, command, "", success, null);
  }

  public static void recordAgent(CommandContext context, String command, String output, boolean success) {
    String chainId = currentChain(context);
    record(context, Source.AGENT, command, output, success, chainId);
  }

  public static String startChain(CommandContext context) {
    String parent = currentChain(context);
    String chainId = java.util.UUID.randomUUID().toString();
    parents(context).put(chainId, parent);
    context.put(CHAIN_KEY, chainId);
    return chainId;
  }

  public static String currentChain(CommandContext context) {
    return context == null ? null : context.get(CHAIN_KEY, String.class);
  }

  public static void restoreChain(CommandContext context, String chainId) {
    if (context == null) {
      return;
    }
    if (chainId == null) {
      context.remove(CHAIN_KEY);
    } else {
      context.put(CHAIN_KEY, chainId);
    }
  }

  public static List<String> chainAncestors(CommandContext context, String chainId) {
    List<String> chains = new ArrayList<>();
    if (chainId == null) {
      return chains;
    }
    java.util.Map<String, String> parents = parents(context);
    String current = chainId;
    while (current != null) {
      chains.add(current);
      current = parents.get(current);
    }
    return chains;
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
      CommandContext context,
      Source source,
      String command,
      String output,
      Boolean success,
      String chainId) {
    if (context == null || source == null || command == null || command.isBlank()) {
      return;
    }
    String normalizedOutput = output == null ? "" : output;
    entries(context).add(new Entry(source, command.trim(), normalizedOutput, success, chainId));
  }

  @SuppressWarnings("unchecked")
  private static java.util.Map<String, String> parents(CommandContext context) {
    Object existing = context.get(CHAIN_PARENTS_KEY);
    if (existing instanceof java.util.Map) {
      return (java.util.Map<String, String>) existing;
    }
    java.util.Map<String, String> map = new java.util.HashMap<>();
    context.put(CHAIN_PARENTS_KEY, map);
    return map;
  }
}
