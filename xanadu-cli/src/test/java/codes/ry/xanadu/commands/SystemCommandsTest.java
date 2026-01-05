package codes.ry.xanadu.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import codes.ry.xanadu.Style;
import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandProvider;
import codes.ry.xanadu.command.CommandService;
import codes.ry.xanadu.command.LogLevel;
import codes.ry.xanadu.render.RenderService;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SystemCommandsTest {
  private SystemCommands commands;
  private StringWriter output;
  private CommandContext context;

  @BeforeEach
  void setUp() {
    commands = new SystemCommands();
    output = new StringWriter();
    PrintWriter writer = new PrintWriter(output);
    CommandService service = new CommandService(List.of(commands));
    context = new CommandContext(writer, Style.box(), RenderService.defaults(), service, 80, 24);
  }

  @Test
  void quitThrowsExitSignal() {
    assertThrows(codes.ry.xanadu.Repl.ExitSignal.class, () -> commands.quit(context));
  }

  @Test
  void qShorthandThrowsExitSignal() {
    assertThrows(codes.ry.xanadu.Repl.ExitSignal.class, () -> commands.q(context));
  }

  @Test
  void helpListsAllCommands() {
    commands.help(context);
    context.out.flush();
    String result = output.toString();
    assertTrue(result.contains("Commands:"));
    assertTrue(result.contains("help"));
  }

  @Test
  void helpWithCommandNameShowsUsage() {
    commands.help(context, "help");
    context.out.flush();
    String result = output.toString();
    assertTrue(result.contains("Usage:"));
  }

  @Test
  void helpWithUnknownCommandShowsError() {
    commands.help(context, "unknown_command_xyz");
    context.out.flush();
    String result = output.toString();
    assertTrue(result.contains("Unknown command"));
  }

  @Test
  void helpWithSubcommandShowsSpecificUsage() {
    commands.help(context, "help", "subcommand");
    context.out.flush();
    String result = output.toString();
    assertTrue(result.contains("Usage:"));
  }

  @Test
  void enableEnablesProvider() {
    CommandProvider provider = new DemoCommands();
    CommandService service = new CommandService(List.of(provider));
    context.setCommandService(service);
    commands.enable(context, "DemoCommands");
    context.out.flush();
    assertTrue(output.toString().contains("Enabled: DemoCommands"));
  }

  @Test
  void enableUnknownProviderShowsError() {
    commands.enable(context, "unknown_provider");
    context.out.flush();
    assertTrue(output.toString().contains("No provider named"));
  }

  @Test
  void disableDisablesProvider() {
    CommandProvider provider = new DemoCommands();
    CommandService service = new CommandService(List.of(provider));
    context.setCommandService(service);
    commands.disable(context, "DemoCommands");
    context.out.flush();
    assertTrue(output.toString().contains("Disabled: DemoCommands"));
  }

  @Test
  void disableUnknownProviderShowsError() {
    commands.disable(context, "unknown_provider");
    context.out.flush();
    assertTrue(output.toString().contains("No provider named"));
  }

  @Test
  void loglevelWithoutArgumentShowsCurrentLevel() {
    commands.loglevel(context);
    context.out.flush();
    String result = output.toString();
    assertTrue(result.contains("Log level:"));
  }

  @Test
  void loglevelSetsLevel() {
    commands.loglevel(context, "debug");
    context.out.flush();
    assertTrue(output.toString().contains("Log level set to debug"));
    assertEquals(LogLevel.DEBUG, context.logLevel());
  }

  @Test
  void loglevelWithInvalidLevelShowsError() {
    commands.loglevel(context, "invalid");
    context.out.flush();
    String result = output.toString();
    assertTrue(result.contains("Unknown log level"));
    assertTrue(result.contains("Available levels"));
  }

  @Test
  void lastexceptionWithNoExceptionShowsMessage() {
    commands.lastexception(context);
    context.out.flush();
    assertTrue(output.toString().contains("No exception recorded"));
  }

  @Test
  void lastexceptionWithExceptionShowsStackTrace() {
    Exception e = new RuntimeException("Test exception");
    context.recordException(e);
    commands.lastexception(context);
    context.out.flush();
    String result = output.toString();
    assertTrue(result.contains("RuntimeException"));
    assertTrue(result.contains("Test exception"));
  }

  @Test
  void usageReturnsCorrectUsageForQuit() {
    List<String> usage = commands.usage("quit");
    assertEquals(1, usage.size());
    assertEquals("quit", usage.get(0));
  }

  @Test
  void usageReturnsCorrectUsageForQ() {
    List<String> usage = commands.usage("q");
    assertEquals(1, usage.size());
    assertEquals("q", usage.get(0));
  }
}
