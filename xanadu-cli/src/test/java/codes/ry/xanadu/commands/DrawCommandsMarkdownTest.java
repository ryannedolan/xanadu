package codes.ry.xanadu.commands;

import codes.ry.xanadu.command.CommandService;
import codes.ry.xanadu.testing.MarkdownTestFixture;
import java.util.List;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class DrawCommandsMarkdownTest extends MarkdownTestFixture {

  @Override
  protected CommandService createCommandService() {
    return new CommandService(List.of(new DrawCommands()));
  }

  @TestFactory
  List<DynamicTest> drawCommandsTests() {
    return loadMarkdownTests("/markdown/draw-commands.md");
  }
}
