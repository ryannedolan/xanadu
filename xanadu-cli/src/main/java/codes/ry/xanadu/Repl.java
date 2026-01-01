package codes.ry.xanadu;

import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandInput;
import codes.ry.xanadu.command.CommandParser;
import codes.ry.xanadu.command.CommandNames;
import codes.ry.xanadu.command.CommandResult;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.NullCompleter;
import java.nio.file.Path;

public final class Repl {
  private final CommandContext context;
  private final LineReader reader;
  private final PrintWriter out;

  public Repl(CommandContext context, LineReader reader, PrintWriter out) {
    this.context = context;
    this.reader = reader;
    this.out = out;
    installSignalHandler();
  }

  public void run() throws IOException {
    while (true) {
      String line;
      try {
        String prompt = "> ";
        if (context.continuation() != null) {
          prompt = context.continuation().commandName + " > ";
        }
        line = reader.readLine(prompt);
      } catch (UserInterruptException e) {
        context.cancelCurrentCommand();
        context.clearContinuation();
        continue;
      } catch (EndOfFileException e) {
        return;
      }
      if (context.continuation() != null) {
        if (line.isBlank()) {
          context.clearContinuation();
          continue;
        }
        try {
          handleContinuation(line);
        } catch (ExitSignal exit) {
          return;
        }
        continue;
      }
      CommandInput input = CommandParser.parse(line);
      if (input == null) {
        continue;
      }
      Optional<codes.ry.xanadu.command.Command> command = context.commandService().find(input);
      if (command.isEmpty()) {
        context.error("Unknown command: " + input.name);
        continue;
      }
      context.setSize(reader.getTerminal().getWidth(), reader.getTerminal().getHeight());
      context.resetFailure();
      try {
        CommandResult result = command.get().execute(context);
        if (result != null && result.isFailure()) {
          context.clearContinuation();
        }
      } catch (ExitSignal exit) {
        return;
      } catch (RuntimeException e) {
        if (isExitSignal(e)) {
          return;
        }
        context.recordException(e);
        context.error("Command failed: " + e.getMessage());
        String trace = context.formatStackTrace(e);
        if (!trace.isEmpty()) {
          context.debug(trace);
        }
        context.clearContinuation();
      }
    }
  }

  private void handleContinuation(String line) {
    var continuation = context.continuation();
    if (continuation == null) {
      return;
    }
    var result = continuation.handler.onLine(line, context);
    if (result == null) {
      return;
    }
    if (result.tail != null) {
      String combined = continuation.commandName + " " + result.tail;
      CommandInput input = CommandParser.parse(combined);
      if (input == null) {
        context.error("Unknown command: " + continuation.commandName);
      } else {
        context.setSize(reader.getTerminal().getWidth(), reader.getTerminal().getHeight());
        Optional<codes.ry.xanadu.command.Command> command = context.commandService().find(input);
        if (command.isEmpty()) {
          context.error("Unknown command: " + input.name);
        } else {
          context.resetFailure();
          try {
            CommandResult result2 = command.get().execute(context);
            if (result2 != null && result2.isFailure()) {
              context.clearContinuation();
            }
          } catch (ExitSignal exit) {
            throw exit;
          } catch (RuntimeException e) {
            if (isExitSignal(e)) {
              throw new ExitSignal();
            }
            context.recordException(e);
            context.error("Command failed: " + e.getMessage());
            String trace = context.formatStackTrace(e);
            if (!trace.isEmpty()) {
              context.debug(trace);
            }
            context.clearContinuation();
          }
        }
      }
    }
    if (!result.continueAfter) {
      context.clearContinuation();
    }
  }

  public static LineReader defaultReader(CommandContext context) {
    return LineReaderBuilder.builder()
        .completer(new CommandCompleter(context))
        .history(new org.jline.reader.impl.history.DefaultHistory())
        .variable(LineReader.HISTORY_FILE, historyPath())
        .build();
  }

  private void installSignalHandler() {
    try {
      Class<?> signalClass = Class.forName("sun.misc.Signal");
      Class<?> handlerClass = Class.forName("sun.misc.SignalHandler");
      Object sigint = signalClass.getConstructor(String.class).newInstance("INT");
      Object handler =
          java.lang.reflect.Proxy.newProxyInstance(
              handlerClass.getClassLoader(),
              new Class<?>[] {handlerClass},
              (proxy, method, args) -> {
                context.cancelCurrentCommand();
                context.clearContinuation();
                return null;
              });
      signalClass.getMethod("handle", signalClass, handlerClass).invoke(null, sigint, handler);
    } catch (ReflectiveOperationException ignored) {
      // Signal handling not available; fall back to JLine behavior.
    }
  }

  private static Path historyPath() {
    String home = System.getProperty("user.home");
    if (home == null || home.isBlank()) {
      return Path.of(".xanadu_history");
    }
    return Path.of(home, ".xanadu_history");
  }

  private static final class CommandCompleter implements Completer {
    private final CommandContext context;

    private CommandCompleter(CommandContext context) {
      this.context = context;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, java.util.List<Candidate> candidates) {
      if (line.wordIndex() != 0) {
        if (line.wordIndex() == 1 && !line.words().isEmpty()) {
          String command = line.words().get(0);
          for (String sub : CommandNames.subcommands(context.commandService(), command)) {
            candidates.add(new Candidate(sub));
          }
        }
        if (!candidates.isEmpty()) {
          return;
        }
        NullCompleter.INSTANCE.complete(reader, line, candidates);
        return;
      }
      for (String name : CommandNames.list(context.commandService())) {
        candidates.add(new Candidate(name));
      }
    }
  }

  public static final class ExitSignal extends RuntimeException {
    public ExitSignal() {
      super("exit");
    }
  }

  private static boolean isExitSignal(Throwable error) {
    Throwable current = error;
    while (current != null) {
      if (current instanceof ExitSignal) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }
}
