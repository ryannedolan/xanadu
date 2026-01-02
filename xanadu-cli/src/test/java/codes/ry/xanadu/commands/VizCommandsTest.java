package codes.ry.xanadu.commands;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import codes.ry.xanadu.Style;
import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandInput;
import codes.ry.xanadu.command.CommandResult;
import codes.ry.xanadu.command.CommandService;
import codes.ry.xanadu.render.RenderService;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VizCommandsTest {
  private VizCommands commands;
  private StringWriter output;
  private CommandContext context;

  @BeforeEach
  void setUp() {
    commands = new VizCommands();
    output = new StringWriter();
    PrintWriter writer = new PrintWriter(output);
    context =
        new CommandContext(
            writer, Style.box(), RenderService.defaults(), new CommandService(List.of()), 80, 24);
  }

  @Test
  void supportsBarCommand() {
    CommandInput input = new CommandInput("bar 10 20 30", "bar", List.of("10", "20", "30"));
    assertTrue(commands.supports(input));
  }

  @Test
  void supportsHbarCommand() {
    CommandInput input = new CommandInput("hbar 10 20 30", "hbar", List.of("10", "20", "30"));
    assertTrue(commands.supports(input));
  }

  @Test
  void supportsSparkCommand() {
    CommandInput input = new CommandInput("spark 1 2 3", "spark", List.of("1", "2", "3"));
    assertTrue(commands.supports(input));
  }

  @Test
  void barCreatesVerticalBarChart() throws Exception {
    CommandInput input = new CommandInput("bar 10 20 30", "bar", List.of("10", "20", "30"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    String rendered = output.toString();
    assertNotNull(rendered);
    assertTrue(rendered.length() > 0);
  }

  @Test
  void barWithSingleValue() throws Exception {
    CommandInput input = new CommandInput("bar 50", "bar", List.of("50"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    String rendered = output.toString();
    assertNotNull(rendered);
  }

  @Test
  void barWithZeroValues() throws Exception {
    CommandInput input = new CommandInput("bar 0 0 0", "bar", List.of("0", "0", "0"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void barWithNoValuesShowsError() throws Exception {
    CommandInput input = new CommandInput("bar", "bar", List.of());
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    String rendered = output.toString();
    assertTrue(rendered.contains("No values provided"));
  }

  @Test
  void barWithInvalidNumberShowsError() throws Exception {
    CommandInput input = new CommandInput("bar 10 abc 30", "bar", List.of("10", "abc", "30"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    String rendered = output.toString();
    assertTrue(rendered.contains("Invalid number"));
  }

  @Test
  void hbarCreatesHorizontalBarChart() throws Exception {
    CommandInput input = new CommandInput("hbar 10 20 30", "hbar", List.of("10", "20", "30"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    String rendered = output.toString();
    assertNotNull(rendered);
    assertTrue(rendered.length() > 0);
  }

  @Test
  void hbarWithFloatingPointValues() throws Exception {
    CommandInput input = new CommandInput("hbar 10.5 20.3 30.7", "hbar", List.of("10.5", "20.3", "30.7"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void sparkCreatesSparkline() throws Exception {
    CommandInput input = new CommandInput("spark 1 2 4 8 16", "spark", List.of("1", "2", "4", "8", "16"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    String rendered = output.toString();
    assertNotNull(rendered);
    assertTrue(rendered.length() > 0);
    // Sparkline should be rendered (may contain block characters or other rendering)
    // Just check that something was produced
  }

  @Test
  void sparkWithSingleValue() throws Exception {
    CommandInput input = new CommandInput("spark 100", "spark", List.of("100"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void sparkWithNegativeValue() throws Exception {
    CommandInput input = new CommandInput("spark -5 10 20", "spark", List.of("-5", "10", "20"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void commandNamesReturnsCorrectSet() {
    var names = commands.commandNames();
    assertEquals(3, names.size());
    assertTrue(names.contains("bar"));
    assertTrue(names.contains("hbar"));
    assertTrue(names.contains("spark"));
  }

  @Test
  void usageReturnsCorrectUsageForBar() {
    List<String> usage = commands.usage("bar");
    assertEquals(1, usage.size());
    assertEquals("bar <values...>", usage.get(0));
  }

  @Test
  void usageReturnsCorrectUsageForHbar() {
    List<String> usage = commands.usage("hbar");
    assertEquals(1, usage.size());
    assertEquals("hbar <values...>", usage.get(0));
  }

  @Test
  void usageReturnsCorrectUsageForSpark() {
    List<String> usage = commands.usage("spark");
    assertEquals(1, usage.size());
    assertEquals("spark <values...>", usage.get(0));
  }

  private void assertEquals(CommandResult expected, CommandResult actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
  }

  private void assertEquals(int expected, int actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
  }

  private void assertEquals(String expected, String actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
  }
}
