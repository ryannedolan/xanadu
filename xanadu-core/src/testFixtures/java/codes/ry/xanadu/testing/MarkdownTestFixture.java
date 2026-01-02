package codes.ry.xanadu.testing;

import codes.ry.xanadu.Style;
import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandInput;
import codes.ry.xanadu.command.CommandParser;
import codes.ry.xanadu.command.CommandService;
import codes.ry.xanadu.command.Continuation;
import codes.ry.xanadu.command.ContinuationResult;
import codes.ry.xanadu.render.RenderService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DynamicTest;

/**
 * Base class for markdown-based tests.
 * 
 * Parses markdown files with fenced code blocks tagged as 'xanadu'.
 * Within such blocks:
 * - Lines starting with '>' are commands to execute
 * - Other lines are expected output
 * - '...' acts as a wildcard for matching any text
 */
public abstract class MarkdownTestFixture {
  private static final Pattern FENCE_START = Pattern.compile("^```xanadu\\s*$");
  private static final Pattern FENCE_END = Pattern.compile("^```\\s*$");
  private static final String COMMAND_PREFIX = "> ";
  private static final String ELLIPSIS = "...";

  protected abstract CommandService createCommandService();

  protected List<DynamicTest> loadMarkdownTests(String resourcePath) {
    List<DynamicTest> tests = new ArrayList<>();
    List<TestBlock> blocks = parseMarkdownFile(resourcePath);
    
    for (int i = 0; i < blocks.size(); i++) {
      TestBlock block = blocks.get(i);
      final int testIndex = i;
      tests.add(DynamicTest.dynamicTest(
        "Test block " + (testIndex + 1) + " (line " + block.startLine + ")",
        () -> executeTestBlock(block)
      ));
    }
    
    return tests;
  }

  private List<TestBlock> parseMarkdownFile(String resourcePath) {
    List<TestBlock> blocks = new ArrayList<>();
    
    try (InputStream in = getClass().getResourceAsStream(resourcePath);
         BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
      
      String line;
      int lineNum = 0;
      boolean inFence = false;
      int blockStartLine = 0;
      List<String> currentBlock = new ArrayList<>();
      
      while ((line = reader.readLine()) != null) {
        lineNum++;
        
        if (!inFence && FENCE_START.matcher(line).matches()) {
          inFence = true;
          blockStartLine = lineNum;
          currentBlock.clear();
        } else if (inFence && FENCE_END.matcher(line).matches()) {
          inFence = false;
          if (!currentBlock.isEmpty()) {
            blocks.add(new TestBlock(blockStartLine, currentBlock));
          }
        } else if (inFence) {
          currentBlock.add(line);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to read markdown file: " + resourcePath, e);
    }
    
    return blocks;
  }

  private void executeTestBlock(TestBlock block) {
    CommandService commandService = createCommandService();
    StringWriter outputWriter = new StringWriter();
    PrintWriter out = new PrintWriter(outputWriter);
    
    CommandContext context = new CommandContext(
      out,
      Style.box(),
      RenderService.defaults(),
      commandService,
      80,
      24
    );
    
    List<CommandExecution> executions = new ArrayList<>();
    StringBuilder expectedOutput = new StringBuilder();
    String currentCommand = null;
    
    // Parse the block into commands and expected outputs
    for (String line : block.lines) {
      if (line.startsWith(COMMAND_PREFIX)) {
        // Save previous command's expected output
        if (currentCommand != null) {
          executions.add(new CommandExecution(currentCommand, expectedOutput.toString()));
          expectedOutput.setLength(0);
        }
        currentCommand = line.substring(COMMAND_PREFIX.length());
      } else {
        // This is expected output for the current command
        if (expectedOutput.length() > 0) {
          expectedOutput.append("\n");
        }
        expectedOutput.append(line);
      }
    }
    
    // Save the last command
    if (currentCommand != null) {
      executions.add(new CommandExecution(currentCommand, expectedOutput.toString()));
    }
    
    // Execute all commands and validate outputs
    for (CommandExecution execution : executions) {
      outputWriter.getBuffer().setLength(0);
      
      executeCommand(context, execution.command);
      
      String actualOutput = outputWriter.toString().trim();
      String expected = execution.expectedOutput.trim();
      
      if (!expected.isEmpty()) {
        validateOutput(actualOutput, expected, execution.command);
      }
    }
  }

  private void executeCommand(CommandContext context, String commandLine) {
    CommandInput input = CommandParser.parse(commandLine);
    if (input == null) {
      throw new AssertionError("Failed to parse command: " + commandLine);
    }
    
    var commandOpt = context.commandService().find(input);
    if (commandOpt.isEmpty()) {
      throw new AssertionError("Command not found: " + commandLine);
    }
    
    commandOpt.get().execute(context);
    
    // Handle continuations if any
    Continuation continuation = context.continuation();
    if (continuation != null) {
      // For now, we don't support continuation in tests
      // This would require multi-line command handling
      context.clearContinuation();
    }
  }

  private void validateOutput(String actual, String expected, String command) {
    if (expected.contains(ELLIPSIS)) {
      // Wildcard matching - check if expected parts are present
      String[] parts = expected.split(Pattern.quote(ELLIPSIS));
      int lastIndex = 0;
      
      for (String part : parts) {
        if (!part.isEmpty()) {
          int index = actual.indexOf(part, lastIndex);
          if (index < 0) {
            throw new AssertionError(
              "Output mismatch for command: " + command + "\n" +
              "Expected to contain: " + part + "\n" +
              "Actual output: " + actual
            );
          }
          lastIndex = index + part.length();
        }
      }
    } else {
      // Exact match
      if (!actual.equals(expected)) {
        throw new AssertionError(
          "Output mismatch for command: " + command + "\n" +
          "Expected: " + expected + "\n" +
          "Actual: " + actual
        );
      }
    }
  }

  private static class TestBlock {
    final int startLine;
    final List<String> lines;
    
    TestBlock(int startLine, List<String> lines) {
      this.startLine = startLine;
      this.lines = List.copyOf(lines);
    }
  }

  private static class CommandExecution {
    final String command;
    final String expectedOutput;
    
    CommandExecution(String command, String expectedOutput) {
      this.command = command;
      this.expectedOutput = expectedOutput;
    }
  }
}
