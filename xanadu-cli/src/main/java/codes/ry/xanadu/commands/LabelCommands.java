package codes.ry.xanadu.commands;

import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandInput;
import codes.ry.xanadu.command.CommandProvider;
import codes.ry.xanadu.command.CommandResult;
import codes.ry.xanadu.command.ReportLabels;
import java.util.Locale;

public final class LabelCommands implements CommandProvider {
  private static final String TITLE = "title";
  private static final String SUBTITLE = "subtitle";
  private static final String XLABEL = "xlabel";
  private static final String YLABEL = "ylabel";
  private static final String NOTE = "note";

  @Override
  public boolean supports(CommandInput input) {
    String name = input.name.toLowerCase(Locale.ROOT);
    return TITLE.equals(name)
        || SUBTITLE.equals(name)
        || XLABEL.equals(name)
        || YLABEL.equals(name)
        || NOTE.equals(name);
  }

  @Override
  public codes.ry.xanadu.command.Command commandFor(CommandInput input) {
    return context -> {
      execute(context, input);
      return CommandResult.SUCCESS;
    };
  }

  @Override
  public java.util.Set<String> commandNames() {
    return java.util.Set.of(TITLE, SUBTITLE, XLABEL, YLABEL, NOTE);
  }

  @Override
  public java.util.List<String> usage(String commandName) {
    switch (commandName) {
      case TITLE:
        return java.util.List.of("title <text...>");
      case SUBTITLE:
        return java.util.List.of("subtitle <text...>");
      case XLABEL:
        return java.util.List.of("xlabel <text...>");
      case YLABEL:
        return java.util.List.of("ylabel <text...>");
      case NOTE:
        return java.util.List.of("note <text...>");
      default:
        return java.util.List.of(commandName);
    }
  }

  private void execute(CommandContext context, CommandInput input) {
    String value = input.tail();
    if (value == null || value.isBlank()) {
      usageError(context, input.name.toLowerCase(Locale.ROOT));
      return;
    }
    switch (input.name.toLowerCase(Locale.ROOT)) {
      case TITLE:
        ReportLabels.setTitle(context, value);
        break;
      case SUBTITLE:
        ReportLabels.setSubtitle(context, value);
        break;
      case XLABEL:
        ReportLabels.setXLabel(context, value);
        break;
      case YLABEL:
        ReportLabels.setYLabel(context, value);
        break;
      case NOTE:
        ReportLabels.setNote(context, value);
        break;
      default:
        context.error("Unknown label command: " + input.name);
        return;
    }
    context.out.println("Set " + input.name.toLowerCase(Locale.ROOT) + ".");
    context.out.flush();
  }

  private void usageError(CommandContext context, String commandName) {
    for (String line : usage(commandName)) {
      context.out.println("Usage: " + line);
    }
    context.out.flush();
  }
}
