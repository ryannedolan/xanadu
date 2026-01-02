package codes.ry.xanadu.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import codes.ry.xanadu.Style;
import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandService;
import codes.ry.xanadu.render.RenderService;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DemoCommandsTest {
  private DemoCommands commands;
  private StringWriter output;
  private CommandContext context;

  @BeforeEach
  void setUp() {
    commands = new DemoCommands();
    output = new StringWriter();
    PrintWriter writer = new PrintWriter(output);
    context =
        new CommandContext(
            writer, Style.box(), RenderService.defaults(), new CommandService(List.of()), 80, 24);
  }

  @Test
  void echoOutputsText() {
    commands.echo(context, "Hello, World!");
    context.out.flush();
    assertEquals("Hello, World!\n", output.toString());
  }

  @Test
  void echoHandlesEmptyString() {
    commands.echo(context, "");
    context.out.flush();
    assertEquals("\n", output.toString());
  }

  @Test
  void echoHandlesSpecialCharacters() {
    commands.echo(context, "Line1\nLine2\tTabbed");
    context.out.flush();
    assertEquals("Line1\nLine2\tTabbed\n", output.toString());
  }

  @Test
  void addComputesSum() {
    commands.add(context, 5, 10);
    context.out.flush();
    assertEquals("15\n", output.toString());
  }

  @Test
  void addHandlesNegativeNumbers() {
    commands.add(context, -5, 10);
    context.out.flush();
    assertEquals("5\n", output.toString());
  }

  @Test
  void addHandlesZero() {
    commands.add(context, 0, 0);
    context.out.flush();
    assertEquals("0\n", output.toString());
  }

  @Test
  void addHandlesLargeNumbers() {
    commands.add(context, Integer.MAX_VALUE - 1, 1);
    context.out.flush();
    assertEquals(Integer.MAX_VALUE + "\n", output.toString());
  }

  @Test
  void renderOutputsText() {
    commands.render(context, "test");
    context.out.flush();
    String result = output.toString();
    assertTrue(result.contains("test"));
  }
}
