package codes.ry.xanadu.command;

public interface Command {
  CommandResult execute(CommandContext context);
}
