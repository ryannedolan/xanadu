package codes.ry.xanadu.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import codes.ry.xanadu.Style;
import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandInput;
import codes.ry.xanadu.command.CommandResult;
import codes.ry.xanadu.command.CommandService;
import codes.ry.xanadu.render.RenderService;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JdbcCsvCommandsTest {
  private JdbcCsvCommands commands;
  private StringWriter output;
  private CommandContext context;
  private Connection connection;
  
  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() throws Exception {
    commands = new JdbcCsvCommands();
    output = new StringWriter();
    PrintWriter writer = new PrintWriter(output);
    context =
        new CommandContext(
            writer, Style.box(), RenderService.defaults(), new CommandService(List.of()), 80, 24);
    
    // Setup H2 connection
    connection = DriverManager.getConnection("jdbc:h2:mem:test");
    JdbcSession.setConnection(context, connection);
    
    // Create test table
    connection.createStatement().execute("CREATE TABLE test_table (id INT, name VARCHAR(50))");
  }

  @Test
  void supportsLoadCommand() {
    CommandInput input = new CommandInput("load test_table", "load", List.of("test_table"));
    assertTrue(commands.supports(input));
  }

  @Test
  void supportsExtractCommand() {
    CommandInput input = new CommandInput("extract test_table", "extract", List.of("test_table"));
    assertTrue(commands.supports(input));
  }

  @Test
  void loadCommandWithNoArgsShowsUsage() throws Exception {
    CommandInput input = new CommandInput("load", "load", List.of());
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("load"));
  }

  @Test
  void extractCommandWithNoArgsShowsUsage() throws Exception {
    CommandInput input = new CommandInput("extract", "extract", List.of());
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("extract"));
  }

  @Test
  void loadCommandWithNonExistentFileShowsError() throws Exception {
    Path csvPath = tempDir.resolve("nonexistent.csv");
    String raw = "load test_table " + csvPath;
    CommandInput input = new CommandInput(raw, "load", List.of("test_table", csvPath.toString()));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("CSV file not found"));
  }

  @Test
  void loadCommandLoadsDataFromCsv() throws Exception {
    // Create CSV file
    Path csvPath = tempDir.resolve("data.csv");
    Files.writeString(csvPath, "id,name\n1,Alice\n2,Bob\n");
    
    String raw = "load test_table " + csvPath + " --header";
    CommandInput input = new CommandInput(raw, "load", List.of("test_table", csvPath.toString(), "--header"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Loaded 2 rows"));
    
    // Verify data was loaded
    var rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM test_table");
    rs.next();
    assertEquals(2, rs.getInt(1));
  }

  @Test
  void loadCommandWithoutHeaderLoadsData() throws Exception {
    // Create CSV file without header
    Path csvPath = tempDir.resolve("data.csv");
    Files.writeString(csvPath, "1,Alice\n2,Bob\n");
    
    String raw = "load test_table " + csvPath;
    CommandInput input = new CommandInput(raw, "load", List.of("test_table", csvPath.toString()));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Loaded 2 rows"));
  }

  @Test
  void loadCommandWithCustomDelimiter() throws Exception {
    // Create TSV file
    Path csvPath = tempDir.resolve("data.tsv");
    Files.writeString(csvPath, "id\tname\n1\tAlice\n2\tBob\n");
    
    String raw = "load test_table " + csvPath + " --header --delimiter=\\t";
    CommandInput input = new CommandInput(raw, "load", List.of("test_table", csvPath.toString(), "--header", "--delimiter=\t"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Loaded 2 rows"));
  }

  @Test
  void extractCommandExtractsTableToFile() throws Exception {
    // Insert test data
    connection.createStatement().execute("INSERT INTO test_table VALUES (1, 'Alice'), (2, 'Bob')");
    
    Path csvPath = tempDir.resolve("output.csv");
    String raw = "extract test_table " + csvPath + " --header";
    CommandInput input = new CommandInput(raw, "extract", List.of("test_table", csvPath.toString(), "--header"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Extracted 2 rows"));
    
    // Verify file was created
    assertTrue(Files.exists(csvPath));
    String content = Files.readString(csvPath);
    assertTrue(content.contains("Alice"));
    assertTrue(content.contains("Bob"));
  }

  @Test
  void extractCommandWithSqlQuery() throws Exception {
    // Insert test data
    connection.createStatement().execute("INSERT INTO test_table VALUES (1, 'Alice'), (2, 'Bob')");
    
    Path csvPath = tempDir.resolve("output.csv");
    String raw = "extract " + csvPath + " --header select * from test_table where id = 1";
    CommandInput input = new CommandInput(raw, "extract", 
        List.of(csvPath.toString(), "--header", "select", "*", "from", "test_table", "where", "id", "=", "1"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Extracted 1 rows"));
    
    String content = Files.readString(csvPath);
    assertTrue(content.contains("Alice"));
    assertTrue(!content.contains("Bob"));
  }

  @Test
  void extractCommandWithoutConnection() throws Exception {
    CommandContext noConnContext = new CommandContext(
        new PrintWriter(output), 
        Style.box(), 
        RenderService.defaults(), 
        new CommandService(List.of()), 
        80, 
        24);
    
    Path csvPath = tempDir.resolve("output.csv");
    String raw = "extract test_table " + csvPath;
    CommandInput input = new CommandInput(raw, "extract", List.of("test_table", csvPath.toString()));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(noConnContext);
    noConnContext.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Not connected"));
  }

  @Test
  void loadCommandWithoutConnection() throws Exception {
    CommandContext noConnContext = new CommandContext(
        new PrintWriter(output), 
        Style.box(), 
        RenderService.defaults(), 
        new CommandService(List.of()), 
        80, 
        24);
    
    Path csvPath = tempDir.resolve("data.csv");
    Files.writeString(csvPath, "1,Alice\n");
    
    String raw = "load test_table " + csvPath;
    CommandInput input = new CommandInput(raw, "load", List.of("test_table", csvPath.toString()));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(noConnContext);
    noConnContext.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Not connected"));
  }

  @Test
  void commandNamesIncludesLoadAndExtract() {
    var names = commands.commandNames();
    assertEquals(2, names.size());
    assertTrue(names.contains("load"));
    assertTrue(names.contains("extract"));
  }

  @Test
  void usageReturnsCorrectFormatForLoad() {
    List<String> usage = commands.usage("load");
    assertEquals(1, usage.size());
    assertTrue(usage.get(0).contains("load"));
    assertTrue(usage.get(0).contains("table"));
  }

  @Test
  void usageReturnsCorrectFormatForExtract() {
    List<String> usage = commands.usage("extract");
    assertEquals(2, usage.size());
    assertTrue(usage.get(0).contains("extract"));
    assertTrue(usage.get(0).contains("table"));
  }
}
