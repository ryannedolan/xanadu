package codes.ry.xanadu.commands;

import codes.ry.xanadu.command.CommandService;
import codes.ry.xanadu.testing.MarkdownTestFixture;
import java.util.List;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class VizCommandsMarkdownTest extends MarkdownTestFixture {

  @Override
  protected CommandService createCommandService() {
    return new CommandService(List.of(new VizCommands()));
  }

  @TestFactory
  List<DynamicTest> vizCommandsTests() {
    return loadMarkdownTests("/markdown/viz-commands.md");
  }
}
