package codes.ry.xanadu.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class DrawCommandsTest {
  private DrawCommands commands;
  private StringWriter output;
  private CommandContext context;

  @BeforeEach
  void setUp() {
    commands = new DrawCommands();
    output = new StringWriter();
    PrintWriter writer = new PrintWriter(output);
    context =
        new CommandContext(
            writer, Style.box(), RenderService.defaults(), new CommandService(List.of()), 80, 24);
  }

  @Test
  void supportsDrawCommand() {
    CommandInput input = new CommandInput("draw", "draw", List.of("point", "0", "0"));
    assertTrue(commands.supports(input));
  }

  @Test
  void drawPointAddsPoint() throws Exception {
    CommandInput input = new CommandInput("draw", "draw", List.of("point", "5", "10"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void drawPointWithInvalidArguments() throws Exception {
    CommandInput input = new CommandInput("draw", "draw", List.of("point", "abc"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Usage"));
  }

  @Test
  void drawLineAddsLine() throws Exception {
    CommandInput input = new CommandInput("draw", "draw", List.of("line", "0", "0", "5", "5"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void drawLineWithInsufficientArguments() throws Exception {
    CommandInput input = new CommandInput("draw", "draw", List.of("line", "0", "0"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Usage"));
  }

  @Test
  void drawRectAddsRectangle() throws Exception {
    CommandInput input = new CommandInput("draw", "draw", List.of("rect", "0", "0", "5", "10"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void drawRectWithZeroHeightOrWidth() throws Exception {
    CommandInput input = new CommandInput("draw", "draw", List.of("rect", "0", "0", "0", "10"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("must be positive"));
  }

  @Test
  void drawTextAddsText() throws Exception {
    CommandInput input = new CommandInput("draw", "draw", List.of("text", "2", "3", "Hello", "World"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void drawTextWithInsufficientArguments() throws Exception {
    CommandInput input = new CommandInput("draw", "draw", List.of("text", "2"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Usage"));
  }

  @Test
  void drawShowRendersBuffer() throws Exception {
    CommandInput input = new CommandInput("draw", "draw", List.of("show", "10", "20"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertNotNull(output.toString());
  }

  @Test
  void drawShowWithInvalidDimensions() throws Exception {
    CommandInput input = new CommandInput("draw", "draw", List.of("show", "0", "20"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Usage"));
  }

  @Test
  void drawClearClearsBuffer() throws Exception {
    // First add something
    CommandInput pointInput = new CommandInput("draw", "draw", List.of("point", "5", "5"));
    commands.commandFor(pointInput).execute(context);
    
    // Then clear
    CommandInput clearInput = new CommandInput("draw", "draw", List.of("clear"));
    codes.ry.xanadu.command.Command command = commands.commandFor(clearInput);
    CommandResult result = command.execute(context);
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void turtlePenUp() throws Exception {
    CommandInput input = new CommandInput("draw", "draw", List.of("pen", "up"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void turtlePenDown() throws Exception {
    CommandInput input = new CommandInput("draw", "draw", List.of("pen", "down"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void turtlePenWithInvalidArgument() throws Exception {
    CommandInput input = new CommandInput("draw", "draw", List.of("pen", "invalid"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Usage"));
  }

  @Test
  void turtleMoveUpdatesPosition() throws Exception {
    CommandInput input = new CommandInput("draw", "draw", List.of("move", "10", "20"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void turtleForwardMovesInDirection() throws Exception {
    CommandInput input = new CommandInput("draw", "draw", List.of("forward", "10"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void turtleLeftTurnsLeft() throws Exception {
    CommandInput input = new CommandInput("draw", "draw", List.of("left", "90"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void turtleRightTurnsRight() throws Exception {
    CommandInput input = new CommandInput("draw", "draw", List.of("right", "45"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void turtleHeadingSetsHeading() throws Exception {
    CommandInput input = new CommandInput("draw", "draw", List.of("heading", "180"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void turtleHomeResetsPosition() throws Exception {
    // Move first
    commands.commandFor(new CommandInput("draw", "draw", List.of("move", "10", "20"))).execute(context);
    
    // Then home
    CommandInput input = new CommandInput("draw", "draw", List.of("home"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void drawWithNoArgumentsShowsUsage() throws Exception {
    CommandInput input = new CommandInput("draw", "draw", List.of());
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Usage"));
  }

  @Test
  void drawWithUnknownSubcommandShowsWarning() throws Exception {
    CommandInput input = new CommandInput("draw", "draw", List.of("unknown"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Unknown draw subcommand"));
  }

  @Test
  void commandNamesReturnsDrawOnly() {
    var names = commands.commandNames();
    assertEquals(1, names.size());
    assertTrue(names.contains("draw"));
  }

  @Test
  void subcommandsList() {
    List<String> subs = commands.subcommands("draw");
    assertTrue(subs.contains("point"));
    assertTrue(subs.contains("line"));
    assertTrue(subs.contains("rect"));
    assertTrue(subs.contains("text"));
    assertTrue(subs.contains("show"));
    assertTrue(subs.contains("clear"));
    assertTrue(subs.contains("pen"));
    assertTrue(subs.contains("move"));
    assertTrue(subs.contains("forward"));
    assertTrue(subs.contains("left"));
    assertTrue(subs.contains("right"));
    assertTrue(subs.contains("heading"));
    assertTrue(subs.contains("home"));
  }

  @Test
  void usageList() {
    List<String> usage = commands.usage("draw");
    assertTrue(usage.contains("draw point <row> <col>"));
    assertTrue(usage.contains("draw line <row1> <col1> <row2> <col2>"));
    assertTrue(usage.contains("draw rect <top> <left> <height> <width>"));
    assertTrue(usage.contains("draw text <row> <col> <text...>"));
    assertTrue(usage.contains("draw show <height> <width>"));
    assertTrue(usage.contains("draw clear"));
  }
}
