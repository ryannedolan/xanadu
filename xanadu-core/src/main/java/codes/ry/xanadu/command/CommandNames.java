package codes.ry.xanadu.command;

import java.util.Set;
import java.util.TreeSet;

public final class CommandNames {
  private CommandNames() {}

  public static Set<String> list(CommandService service) {
    Set<String> names = new TreeSet<>();
    for (CommandProvider provider : service.providers()) {
      names.addAll(provider.commandNames());
    }
    for (CommandProvider provider : service.enabledProviders()) {
      names.addAll(provider.commandNames());
    }
    return names;
  }

  public static Set<String> subcommands(CommandService service, String command) {
    Set<String> subs = new TreeSet<>();
    for (CommandProvider provider : service.providers()) {
      subs.addAll(provider.subcommands(command));
    }
    for (CommandProvider provider : service.enabledProviders()) {
      subs.addAll(provider.subcommands(command));
    }
    return subs;
  }
}
