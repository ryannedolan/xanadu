package codes.ry.xanadu.command;

public final class Continuation {
  public final String commandName;
  public final Handler handler;

  public Continuation(String commandName, Handler handler) {
    this.commandName = commandName;
    this.handler = handler;
  }

  @FunctionalInterface
  public interface Handler {
    ContinuationResult onLine(String line, CommandContext context);
  }
}
