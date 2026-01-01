package codes.ry.xanadu.command;

import java.util.List;

public final class ScriptRunner {
  private ScriptRunner() {}

  public static boolean run(CommandContext context, List<String> lines) {
    if (lines == null || lines.isEmpty()) {
      return true;
    }
    Continuation previous = context.continuation();
    context.clearContinuation();
    Continuation continuation = null;
    String continuationName = null;
    try {
      for (String line : lines) {
        if (continuation == null) {
          if (!executeLine(context, line)) {
            return false;
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
            return false;
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
        context.error("Script ended before continuation completed.");
        return false;
      }
      return true;
    } finally {
      context.continueWith(previous);
    }
  }

  private static boolean executeLine(CommandContext context, String line) {
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
}
