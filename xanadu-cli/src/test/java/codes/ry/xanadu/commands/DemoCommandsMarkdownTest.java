package codes.ry.xanadu.commands;

import codes.ry.xanadu.command.CommandService;
import codes.ry.xanadu.testing.MarkdownTestFixture;
import java.util.List;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class DemoCommandsMarkdownTest extends MarkdownTestFixture {

  @Override
  protected CommandService createCommandService() {
    return new CommandService(List.of(new DemoCommands()));
  }

  @TestFactory
  List<DynamicTest> demoCommandsTests() {
    return loadMarkdownTests("/markdown/demo-commands.md");
  }
}
