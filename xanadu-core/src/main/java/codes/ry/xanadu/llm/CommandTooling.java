package codes.ry.xanadu.llm;

import codes.ry.xanadu.command.CapturePrintWriter;
import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandInput;
import codes.ry.xanadu.command.CommandParser;
import codes.ry.xanadu.command.CommandProvider;
import codes.ry.xanadu.command.CommandService;
import codes.ry.xanadu.command.CommandResult;
import codes.ry.xanadu.command.Continuation;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.List;

public final class CommandTooling {
  public static final String TOOL_NAME = "xanadu";

  private CommandTooling() {}

  public static String toolDescription(CommandService service) {
    Set<String> names = new TreeSet<>();
    for (var provider : service.providers()) {
      names.addAll(provider.commandNames());
    }
    for (var provider : service.enabledProviders()) {
      names.addAll(provider.commandNames());
    }
    if (names.isEmpty()) {
      return "Execute a xanadu command line.";
    }
    return "Execute xanadu command lines. Available commands: " + String.join(", ", names) + "."
        + " Command output is shown to the user automatically, so do not repeat it unless asked.";
  }

  public static String detailedUsage(CommandService service) {
    StringBuilder sb = new StringBuilder();
    for (var provider : orderedProviders(service)) {
      for (String command : provider.commandNames()) {
        List<String> usage = provider.usage(command);
        if (usage.isEmpty()) {
          sb.append(command).append('\n');
          continue;
        }
        for (String line : usage) {
          sb.append(line).append('\n');
        }
      }
    }
    return sb.toString().trim();
  }

  public static Result execute(CommandContext baseContext, String line) {
    return execute(baseContext, line, false);
  }

  public static Result execute(CommandContext baseContext, String line, boolean allowContinuation) {
    CommandInput input = CommandParser.parse(line);
    if (input == null) {
      return Result.failure("No command provided.");
    }
    Optional<codes.ry.xanadu.command.Command> command = baseContext.commandService().find(input);
    if (command.isEmpty()) {
      return Result.failure("Unknown command: " + input.name);
    }
    StringWriter output = new StringWriter();
    PrintWriter capture = new PrintWriter(output);
    PrintWriter screen = new PrintWriter(new IndentingWriter(baseContext.out, "      "));
    CapturePrintWriter writer = new CapturePrintWriter(screen, capture);
    CommandContext child = baseContext.fork(writer);
    child.setSize(displayWidth(baseContext), displayHeight(baseContext));
    child.setClipFrames(true);
    child.setRenderTap(capture, Integer.MAX_VALUE, Integer.MAX_VALUE, false);
    child.setAllowContinuation(allowContinuation);
    child.resetFailure();
    try {
      CommandResult result = command.get().execute(child);
      if (result != null && result.isFailure()) {
        child.fail();
      }
    } catch (RuntimeException e) {
      child.error("Command failed: " + e.getMessage());
      return Result.failure("Command failed: " + e.getMessage());
    } finally {
      writer.flush();
    }
    boolean success = !child.failed();
    return new Result(output.toString(), success, child.continuation());
  }

  private static List<CommandProvider> orderedProviders(CommandService service) {
    List<CommandProvider> ordered = new java.util.ArrayList<>();
    ordered.addAll(service.enabledProviders());
    for (CommandProvider provider : service.providers()) {
      if (!ordered.contains(provider)) {
        ordered.add(provider);
      }
    }
    for (CommandProvider provider : service.disabledProviders()) {
      if (!ordered.contains(provider)) {
        ordered.add(provider);
      }
    }
    return ordered;
  }

  private static int displayWidth(CommandContext context) {
    if (context.maxWidth <= 0) {
      return 0;
    }
    return Math.max(1, (context.maxWidth * 3) / 4);
  }

  private static int displayHeight(CommandContext context) {
    if (context.maxHeight <= 0) {
      return 0;
    }
    return Math.max(1, context.maxHeight / 2);
  }

  private static final class IndentingWriter extends java.io.Writer {
    private final PrintWriter out;
    private final String indent;
    private boolean lineStart;

    private IndentingWriter(PrintWriter out, String indent) {
      this.out = out;
      this.indent = indent;
      this.lineStart = true;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
      for (int i = off; i < off + len; i++) {
        char c = cbuf[i];
        if (lineStart) {
          out.write(indent);
          lineStart = false;
        }
        out.write(c);
        if (c == '\n') {
          lineStart = true;
        }
      }
    }

    @Override
    public void flush() {
      out.flush();
    }

    @Override
    public void close() {
      out.flush();
    }
  }

  public static final class Result {
    public final String output;
    public final boolean success;
    public final Continuation continuation;

    private Result(String output, boolean success, Continuation continuation) {
      this.output = output;
      this.success = success;
      this.continuation = continuation;
    }

    public static Result success(String output) {
      return new Result(output, true, null);
    }

    public static Result failure(String output) {
      return new Result(output, false, null);
    }
  }
}
