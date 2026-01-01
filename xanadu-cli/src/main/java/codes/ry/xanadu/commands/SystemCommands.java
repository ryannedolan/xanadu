package codes.ry.xanadu.commands;

import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandProvider;
import codes.ry.xanadu.command.CommandService;
import codes.ry.xanadu.command.ReflectiveCommandProvider;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SystemCommands extends ReflectiveCommandProvider {
  public void quit(CommandContext context) {
    throw new codes.ry.xanadu.Repl.ExitSignal();
  }

  public void q(CommandContext context) {
    quit(context);
  }


  public void help(CommandContext context) {
    PrintWriter out = context.out;
    CommandService service = context.commandService();
    out.println("Commands:");
    for (CommandProvider provider : orderedProviders(service)) {
      String state = providerState(service, provider);
      out.println("  [" + state + "] " + provider.name());
      for (String signature : signatures(provider)) {
        out.println("    " + signature);
      }
    }
    out.flush();
  }

  public void help(CommandContext context, String commandName) {
    help(context, commandName, null);
  }

  public void help(CommandContext context, String commandName, String subcommand) {
    PrintWriter out = context.out;
    CommandService service = context.commandService();
    CommandProvider provider = findProviderForCommand(service, commandName);
    if (provider == null) {
      context.error("Unknown command: " + commandName);
      return;
    }
    out.println("Usage:");
    List<String> usages = provider.usage(commandName);
    if (usages.isEmpty()) {
      out.println("  " + commandName);
    } else if (subcommand == null) {
      for (String usage : usages) {
        out.println("  " + usage);
      }
    } else {
      boolean matched = false;
      for (String usage : usages) {
        if (usage.startsWith(commandName + " " + subcommand)) {
          out.println("  " + usage);
          matched = true;
        }
      }
      if (!matched) {
        context.warn("No detailed usage for " + commandName + " " + subcommand);
        for (String usage : usages) {
          out.println("  " + usage);
        }
      }
    }
    List<String> subs = provider.subcommands(commandName);
    if (!subs.isEmpty()) {
      out.println();
      out.println("Subcommands:");
      for (String sub : subs) {
        out.println("  " + commandName + " " + sub);
      }
    }
    out.flush();
  }

  @Override
  public java.util.List<String> usage(String commandName) {
    switch (commandName) {
      case "quit":
      case "q":
        return java.util.List.of(commandName);
      default:
        return super.usage(commandName);
    }
  }

  public void enable(CommandContext context, String providerName) {
    CommandProvider provider = findProvider(context.commandService(), providerName);
    if (provider == null) {
      context.error("No provider named: " + providerName);
      return;
    }
    context.setCommandService(context.commandService().enable(provider));
    context.out.println("Enabled: " + provider.name());
    context.out.flush();
  }

  public void disable(CommandContext context, String providerName) {
    CommandProvider provider = findProvider(context.commandService(), providerName);
    if (provider == null) {
      context.error("No provider named: " + providerName);
      return;
    }
    context.setCommandService(context.commandService().disable(provider));
    context.out.println("Disabled: " + provider.name());
    context.out.flush();
  }

  public void loglevel(CommandContext context) {
    context.out.println("Log level: " + context.logLevel().name().toLowerCase());
    context.out.flush();
  }

  public void loglevel(CommandContext context, String level) {
    var parsed = codes.ry.xanadu.command.LogLevel.parse(level);
    if (parsed == null) {
      context.error("Unknown log level: " + level);
      context.out.println("Available levels: error, warn, info, debug");
      context.out.flush();
      return;
    }
    context.setLogLevel(parsed);
    context.out.println("Log level set to " + parsed.name().toLowerCase());
    context.out.flush();
  }

  public void lastexception(CommandContext context) {
    Throwable error = context.lastException();
    if (error == null) {
      context.out.println("No exception recorded.");
      context.out.flush();
      return;
    }
    context.out.println(context.formatStackTrace(error));
    context.out.flush();
  }

  private static List<CommandProvider> orderedProviders(CommandService service) {
    List<CommandProvider> ordered = new ArrayList<>();
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

  private static String providerState(CommandService service, CommandProvider provider) {
    if (service.disabledProviders().contains(provider)) {
      return "disabled";
    }
    if (service.enabledProviders().contains(provider)) {
      return "enabled";
    }
    return "default";
  }

  private static CommandProvider findProvider(CommandService service, String name) {
    for (CommandProvider provider : orderedProviders(service)) {
      if (provider.name().equals(name)) {
        return provider;
      }
    }
    return null;
  }

  private static CommandProvider findProviderForCommand(CommandService service, String commandName) {
    for (CommandProvider provider : orderedProviders(service)) {
      if (provider.commandNames().contains(commandName)) {
        return provider;
      }
    }
    return null;
  }

  private static List<String> signatures(CommandProvider provider) {
    Set<String> lines = new LinkedHashSet<>();
    for (String name : provider.commandNames()) {
      List<String> subs = provider.subcommands(name);
      if (!subs.isEmpty()) {
        for (String sub : subs) {
          lines.add(name + " " + sub);
        }
      }
      for (String usage : provider.usage(name)) {
        lines.add(usage);
      }
    }
    return new ArrayList<>(lines);
  }

}
