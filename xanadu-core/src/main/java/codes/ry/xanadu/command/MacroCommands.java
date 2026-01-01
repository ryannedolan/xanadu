package codes.ry.xanadu.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MacroCommands implements CommandProvider {
  private static final Pattern PARAM_PATTERN = Pattern.compile("\\$(\\d+|@)");
  private final Map<String, Macro> macros = new HashMap<>();

  @Override
  public boolean supports(CommandInput input) {
    String name = input.name.toLowerCase(Locale.ROOT);
    if ("def".equals(name) || "undef".equals(name) || "macros".equals(name)) {
      return true;
    }
    return macros.containsKey(input.name);
  }

  @Override
  public Command commandFor(CommandInput input) {
    String name = input.name.toLowerCase(Locale.ROOT);
    if ("def".equals(name)) {
      return context -> {
        define(context, input);
        return CommandResult.SUCCESS;
      };
    }
    if ("undef".equals(name)) {
      return context -> {
        undefine(context, input);
        return CommandResult.SUCCESS;
      };
    }
    if ("macros".equals(name)) {
      return context -> {
        list(context);
        return CommandResult.SUCCESS;
      };
    }
    Macro macro = macros.get(input.name);
    if (macro == null) {
      return context -> {
        context.error("Unknown macro: " + input.name);
        return CommandResult.FAILURE;
      };
    }
    return context -> executeMacro(context, macro, input);
  }

  @Override
  public java.util.Set<String> commandNames() {
    java.util.Set<String> names = new java.util.TreeSet<>();
    names.add("def");
    names.add("undef");
    names.add("macros");
    names.addAll(macros.keySet());
    return names;
  }

  @Override
  public java.util.List<String> usage(String commandName) {
    if ("def".equals(commandName)) {
      return java.util.List.of("def <name> ... end");
    }
    if ("undef".equals(commandName)) {
      return java.util.List.of("undef <name>");
    }
    if ("macros".equals(commandName)) {
      return java.util.List.of("macros");
    }
    return java.util.List.of(commandName);
  }


  private void define(CommandContext context, CommandInput input) {
    if (input.args.isEmpty()) {
      context.error("Usage: def <name> ... end");
      return;
    }
    String name = input.args.get(0);
    String remainder = remainderAfterName(input);
    if (remainder == null) {
      remainder = "";
    }
    String inline = remainder.trim();
    Matcher matcher = Pattern.compile("(?s)^(.*)\\bend\\b\\s*$").matcher(inline);
    if (matcher.matches()) {
      String body = matcher.group(1).trim();
      List<String> lines = body.isEmpty() ? List.of() : List.of(body);
      putMacro(context, name, lines);
      return;
    }
    String initial = inline;
    context.continueWith(
        new Continuation(
            "def",
            (line, ctx) -> {
              if (line.trim().equals("end")) {
                List<String> lines = new ArrayList<>();
                if (!initial.isBlank()) {
                  lines.add(initial);
                }
                lines.addAll(collectLines(ctx));
                putMacro(ctx, name, lines);
                return ContinuationResult.end();
              }
              recordLine(ctx, line);
              return ContinuationResult.continueWithoutExecution();
            }));
    if (!initial.isBlank()) {
      recordLine(context, initial);
    }
  }

  private void undefine(CommandContext context, CommandInput input) {
    if (input.args.isEmpty()) {
      context.error("Usage: undef <name>");
      return;
    }
    String name = input.args.get(0);
    if (macros.remove(name) == null) {
      context.warn("Macro not found: " + name);
    }
  }

  private void list(CommandContext context) {
    if (macros.isEmpty()) {
      context.out.println("No macros defined.");
      context.out.flush();
      return;
    }
    List<String> names = new ArrayList<>(macros.keySet());
    names.sort(Comparator.naturalOrder());
    for (String name : names) {
      context.out.println(name);
    }
    context.out.flush();
  }

  private CommandResult executeMacro(CommandContext context, Macro macro, CommandInput input) {
    List<String> args = input.args;
    int required = macro.requiredArgs;
    if (required > args.size()) {
      context.error("Macro requires at least " + required + " arguments.");
      return CommandResult.FAILURE;
    }
    String rawArgs = input.tail();
    List<String> expanded = new ArrayList<>();
    for (String line : macro.lines) {
      expanded.add(expand(line, args, rawArgs));
    }
    return runLines(context, expanded);
  }

  private CommandResult runLines(CommandContext context, List<String> lines) {
    Continuation prev = context.continuation();
    context.clearContinuation();
    Continuation continuation = null;
    String continuationName = null;
    try {
      for (int i = 0; i < lines.size(); i++) {
        String line = lines.get(i);
        if (continuation == null) {
          if (!executeLine(context, line)) {
            return CommandResult.FAILURE;
          }
          continuation = context.continuation();
          continuationName = continuation == null ? null : continuation.commandName;
          context.clearContinuation();
          continue;
        }
        ContinuationResult result = continuation.handler.onLine(line, context);
        if (result == null) {
          continue;
        }
        if (result.tail != null) {
          String combined = continuationName + " " + result.tail;
          if (!executeLine(context, combined)) {
            return CommandResult.FAILURE;
          }
          continuation = context.continuation();
          continuationName = continuation == null ? null : continuation.commandName;
          context.clearContinuation();
        }
        if (!result.continueAfter) {
          continuation = null;
          continuationName = null;
        }
      }
      if (continuation != null) {
        context.error("Macro ended before continuation completed.");
        return CommandResult.FAILURE;
      }
      return CommandResult.SUCCESS;
    } finally {
      context.continueWith(prev);
    }
  }

  private boolean executeLine(CommandContext context, String line) {
    CommandInput input = CommandParser.parse(line);
    if (input == null) {
      return true;
    }
    var command = context.commandService().find(input);
    if (command.isEmpty()) {
      context.error("Unknown command: " + input.name);
      return false;
    }
    CommandResult result = command.get().execute(context);
    return result == null || !result.isFailure();
  }

  private String remainderAfterName(CommandInput input) {
    String tail = input.tail();
    if (tail == null || tail.isBlank()) {
      return "";
    }
    String name = input.args.get(0);
    if (tail.startsWith(name)) {
      return tail.substring(name.length()).trim();
    }
    return tail;
  }

  private void putMacro(CommandContext context, String name, List<String> lines) {
    int required = requiredArgs(lines);
    macros.put(name, new Macro(List.copyOf(lines), required));
  }


  private int requiredArgs(List<String> lines) {
    int max = 0;
    for (String line : lines) {
      Matcher matcher = PARAM_PATTERN.matcher(line);
      while (matcher.find()) {
        String token = matcher.group(1);
        if (token.equals("@")) {
          continue;
        }
        try {
          int index = Integer.parseInt(token);
          max = Math.max(max, index);
        } catch (NumberFormatException ignored) {
          // Ignore.
        }
      }
    }
    return max;
  }

  private String expand(String line, List<String> args, String rawArgs) {
    Matcher matcher = PARAM_PATTERN.matcher(line);
    StringBuffer buffer = new StringBuffer();
    while (matcher.find()) {
      String token = matcher.group(1);
      String replacement = "";
      if (token.equals("@")) {
        replacement = rawArgs == null ? "" : rawArgs;
      } else {
        int index = Integer.parseInt(token);
        if (index >= 1 && index <= args.size()) {
          replacement = args.get(index - 1);
        }
      }
      matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(buffer);
    return buffer.toString();
  }

  private void recordLine(CommandContext context, String line) {
    List<String> lines = getRecordedLines(context);
    lines.add(line);
  }

  private List<String> collectLines(CommandContext context) {
    List<String> lines = getRecordedLines(context);
    context.remove("__macro.def.lines");
    if (lines == null) {
      return List.of();
    }
    return new ArrayList<>(lines);
  }

  private List<String> getRecordedLines(CommandContext context) {
    Object value = context.get("__macro.def.lines");
    if (value instanceof List) {
      List<?> raw = (List<?>) value;
      boolean allStrings = true;
      for (Object entry : raw) {
        if (!(entry instanceof String)) {
          allStrings = false;
          break;
        }
      }
      if (allStrings) {
        @SuppressWarnings("unchecked")
        List<String> lines = (List<String>) raw;
        return lines;
      }
    }
    List<String> lines = new ArrayList<>();
    context.put("__macro.def.lines", lines);
    return lines;
  }

  private static final class Macro {
    private final List<String> lines;
    private final int requiredArgs;

    private Macro(List<String> lines, int requiredArgs) {
      this.lines = lines;
      this.requiredArgs = requiredArgs;
    }
  }

}
