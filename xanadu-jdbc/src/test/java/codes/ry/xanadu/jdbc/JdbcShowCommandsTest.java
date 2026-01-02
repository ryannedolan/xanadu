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
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdbcShowCommandsTest {
  private JdbcShowCommands commands;
  private StringWriter output;
  private CommandContext context;
  private Connection connection;

  @BeforeEach
  void setUp() throws Exception {
    commands = new JdbcShowCommands();
    output = new StringWriter();
    PrintWriter writer = new PrintWriter(output);
    context =
        new CommandContext(
            writer, Style.box(), RenderService.defaults(), new CommandService(List.of()), 80, 24);
    
    // Setup H2 connection with unique database name for each test
    connection = DriverManager.getConnection("jdbc:h2:mem:test" + System.nanoTime(), "sa", "");
    JdbcSession.setConnection(context, connection);
    
    // Create test table for some tests
    connection.createStatement().execute("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50))");
  }

  @Test
  void supportsShowCommand() {
    CommandInput input = new CommandInput("show tables", "show", List.of("tables"));
    assertTrue(commands.supports(input));
  }

  @Test
  void doesNotSupportShowWithoutArgs() {
    CommandInput input = new CommandInput("show", "show", List.of());
    assertTrue(!commands.supports(input));
  }

  @Test
  void showTablesListsTables() throws Exception {
    CommandInput input = new CommandInput("show tables", "show", List.of("tables"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    String rendered = output.toString();
    assertTrue(rendered.contains("TEST_TABLE"));
  }

  @Test
  void showSchemasListsSchemas() throws Exception {
    CommandInput input = new CommandInput("show schemas", "show", List.of("schemas"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void showCatalogsListsCatalogs() throws Exception {
    CommandInput input = new CommandInput("show catalogs", "show", List.of("catalogs"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void showTableTypesListsTypes() throws Exception {
    CommandInput input = new CommandInput("show table-types", "show", List.of("table-types"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void showTypesListsTypes() throws Exception {
    CommandInput input = new CommandInput("show types", "show", List.of("types"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void showColumnsListsColumns() throws Exception {
    CommandInput input = new CommandInput("show columns test_table", "show", List.of("columns", "test_table"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    String rendered = output.toString();
    assertTrue(rendered.contains("ID") || rendered.contains("NAME"));
  }

  @Test
  void showColumnsWithoutTableNameShowsUsage() throws Exception {
    CommandInput input = new CommandInput("show columns", "show", List.of("columns"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Usage"));
  }

  @Test
  void showPrimaryKeysListsKeys() throws Exception {
    CommandInput input = new CommandInput("show primary-keys test_table", "show", List.of("primary-keys", "test_table"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void showImportedKeysListsKeys() throws Exception {
    CommandInput input = new CommandInput("show imported-keys test_table", "show", List.of("imported-keys", "test_table"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void showExportedKeysListsKeys() throws Exception {
    CommandInput input = new CommandInput("show exported-keys test_table", "show", List.of("exported-keys", "test_table"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void showIndexesListsIndexes() throws Exception {
    CommandInput input = new CommandInput("show indexes test_table", "show", List.of("indexes", "test_table"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void showProceduresListsProcedures() throws Exception {
    CommandInput input = new CommandInput("show procedures", "show", List.of("procedures"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void showFunctionsListsFunctions() throws Exception {
    CommandInput input = new CommandInput("show functions", "show", List.of("functions"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void showClientinfoListsClientInfo() throws Exception {
    CommandInput input = new CommandInput("show clientinfo", "show", List.of("clientinfo"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void showWithoutConnectionShowsError() throws Exception {
    CommandContext noConnContext = new CommandContext(
        new PrintWriter(output), 
        Style.box(), 
        RenderService.defaults(), 
        new CommandService(List.of()), 
        80, 
        24);
    
    CommandInput input = new CommandInput("show tables", "show", List.of("tables"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(noConnContext);
    noConnContext.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Not connected"));
  }

  @Test
  void showUnknownCommandShowsMessage() throws Exception {
    CommandInput input = new CommandInput("show unknown_command", "show", List.of("unknown_command"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Unknown show command"));
  }

  @Test
  void commandNamesReturnsShow() {
    var names = commands.commandNames();
    assertEquals(1, names.size());
    assertTrue(names.contains("show"));
  }

  @Test
  void subcommandsList() {
    List<String> subs = commands.subcommands("show");
    assertTrue(subs.contains("tables"));
    assertTrue(subs.contains("schemas"));
    assertTrue(subs.contains("catalogs"));
    assertTrue(subs.contains("columns"));
    assertTrue(subs.contains("primary-keys"));
    assertTrue(subs.contains("imported-keys"));
    assertTrue(subs.contains("exported-keys"));
    assertTrue(subs.contains("indexes"));
    assertTrue(subs.contains("procedures"));
    assertTrue(subs.contains("functions"));
  }

  @Test
  void usageList() {
    List<String> usage = commands.usage("show");
    assertTrue(usage.size() > 0);
    assertTrue(usage.stream().anyMatch(u -> u.contains("show tables")));
    assertTrue(usage.stream().anyMatch(u -> u.contains("show schemas")));
    assertTrue(usage.stream().anyMatch(u -> u.contains("show catalogs")));
  }
}
