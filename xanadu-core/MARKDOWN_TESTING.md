# Markdown-Based Testing Machinery

Xanadu provides a markdown-based testing framework that allows you to write self-documenting tests directly in markdown files.

## Overview

Tests are written as markdown files with fenced code blocks tagged as `xanadu`. Within these blocks:

- Lines starting with `>` are commands to execute
- Other lines are expected output
- `...` acts as a wildcard for matching any text (useful for non-deterministic or verbose output)

## Example

```markdown
# Demo Commands

## Echo Command

The `echo` command prints text to the output:

\`\`\`xanadu
> echo "hello world"
hello world
\`\`\`

## Add Command

The `add` command adds two numbers:

\`\`\`xanadu
> add 1 2
3
> add 50 50
100
\`\`\`
```

## Creating Tests

### 1. Create a Markdown File

Place your markdown file in `src/test/resources/markdown/` with descriptive examples of command usage.

### 2. Create a Test Class

Extend `MarkdownTestFixture` from `xanadu-core`:

```java
package codes.ry.xanadu.commands;

import codes.ry.xanadu.command.CommandService;
import codes.ry.xanadu.testing.MarkdownTestFixture;
import java.util.List;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class MyCommandsMarkdownTest extends MarkdownTestFixture {

  @Override
  protected CommandService createCommandService() {
    // Include the command providers you want to test
    return new CommandService(List.of(new MyCommands()));
  }

  @TestFactory
  List<DynamicTest> myCommandsTests() {
    return loadMarkdownTests("/markdown/my-commands.md");
  }
}
```

### 3. Add Dependencies

Make sure your module's `build.gradle` includes the test fixtures:

```gradle
dependencies {
  testImplementation testFixtures(project(':xanadu-core'))
}
```

## Output Matching

### Exact Match

When no `...` is present, the output must match exactly:

```xanadu
> add 1 2
3
```

### Wildcard Match

Use `...` to match any text. This is useful for:
- Large or formatted output (charts, tables)
- Non-deterministic output
- Output where only part of it matters

```xanadu
> bar 10 20 30
...
```

Multiple `...` can be used to check for specific parts:

```xanadu
> help
...Commands:...
```

## Command Syntax

Commands are parsed using the same parser as the REPL, so:
- Use quotes for strings with spaces: `echo "hello world"`
- Multiple arguments are space-separated: `add 1 2`
- Commands can be chained in the same test block

## Multiple Commands in One Block

You can test multiple related commands in sequence:

```xanadu
> echo "Starting test"
Starting test
> add 5 5
10
> echo "Done"
Done
```

## Best Practices

1. **Keep tests simple** - Focus on demonstrating usage rather than exhaustive coverage
2. **Make tests readable** - These markdown files serve as documentation
3. **Use wildcards wisely** - Use `...` for complex output, exact matches for simple output
4. **Group related tests** - Put related commands in the same markdown file
5. **Add context** - Use markdown headers and descriptions to explain what's being tested

## Limitations

- Continuations (multi-line commands) are not currently supported in markdown tests
- Commands that require interactive input should be tested separately
- Commands with side effects (file I/O, database) may be difficult to test this way

## Examples

See the following for complete examples:
- [demo-commands.md](../../xanadu-cli/src/test/resources/markdown/demo-commands.md)
- [viz-commands.md](../../xanadu-cli/src/test/resources/markdown/viz-commands.md)
- [draw-commands.md](../../xanadu-cli/src/test/resources/markdown/draw-commands.md)
- [system-commands.md](../../xanadu-cli/src/test/resources/markdown/system-commands.md)
