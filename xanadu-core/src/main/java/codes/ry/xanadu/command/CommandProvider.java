package codes.ry.xanadu.command;

public interface CommandProvider {
  boolean supports(CommandInput input);

  Command commandFor(CommandInput input);

  default String name() {
    return getClass().getSimpleName();
  }

  default java.util.Set<String> commandNames() {
    return java.util.Set.of();
  }

  default java.util.List<String> subcommands(String commandName) {
    return java.util.List.of();
  }

  default java.util.List<String> usage(String commandName) {
    return java.util.List.of(commandName);
  }
}
