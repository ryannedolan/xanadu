package codes.ry.xanadu.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

class JdbcSqlCommandsTest {
  private JdbcSqlCommands commands;
  private StringWriter output;
  private CommandContext context;
  private Connection connection;

  @BeforeEach
  void setUp() throws Exception {
    commands = new JdbcSqlCommands();
    output = new StringWriter();
    PrintWriter writer = new PrintWriter(output);
    context =
        new CommandContext(
            writer, Style.box(), RenderService.defaults(), new CommandService(List.of()), 80, 24);
    
    // Setup H2 connection with unique database name for each test
    connection = DriverManager.getConnection("jdbc:h2:mem:test" + System.nanoTime(), "sa", "");
    JdbcSession.setConnection(context, connection);
  }

  @Test
  void supportsSelectCommand() {
    CommandInput input = new CommandInput("select * from users", "select", List.of("*", "from", "users"));
    assertTrue(commands.supports(input));
  }

  @Test
  void supportsInsertCommand() {
    CommandInput input = new CommandInput("insert into users", "insert", List.of("into", "users"));
    assertTrue(commands.supports(input));
  }

  @Test
  void supportsUpdateCommand() {
    CommandInput input = new CommandInput("update users", "update", List.of("users"));
    assertTrue(commands.supports(input));
  }

  @Test
  void supportsDeleteCommand() {
    CommandInput input = new CommandInput("delete from users", "delete", List.of("from", "users"));
    assertTrue(commands.supports(input));
  }

  @Test
  void supportsCreateCommand() {
    CommandInput input = new CommandInput("create table users", "create", List.of("table", "users"));
    assertTrue(commands.supports(input));
  }

  @Test
  void supportsAlterCommand() {
    CommandInput input = new CommandInput("alter table users", "alter", List.of("table", "users"));
    assertTrue(commands.supports(input));
  }

  @Test
  void supportsDropCommand() {
    CommandInput input = new CommandInput("drop table users", "drop", List.of("table", "users"));
    assertTrue(commands.supports(input));
  }

  @Test
  void supportsSqlCommand() {
    CommandInput input = new CommandInput("sql select 1", "sql", List.of("select", "1"));
    assertTrue(commands.supports(input));
  }

  @Test
  void supportsDdlCommand() {
    CommandInput input = new CommandInput("ddl create table", "ddl", List.of("create", "table"));
    assertTrue(commands.supports(input));
  }

  @Test
  void createTableExecutesSuccessfully() throws Exception {
    CommandInput input = new CommandInput("create table users (id int)", "create", List.of("table", "users", "(id", "int)"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Updated 0 rows"));
  }

  @Test
  void insertIntoTableExecutesSuccessfully() throws Exception {
    // Create table first
    connection.createStatement().execute("CREATE TABLE test_table (id INT, name VARCHAR(50))");
    
    CommandInput input = new CommandInput("insert into test_table values (1, 'Alice')", "insert", List.of("into", "test_table", "values", "(1,", "'Alice')"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Updated 1 rows"));
  }

  @Test
  void selectFromTableReturnsResults() throws Exception {
    // Setup test data
    connection.createStatement().execute("CREATE TABLE test_table (id INT, name VARCHAR(50))");
    connection.createStatement().execute("INSERT INTO test_table VALUES (1, 'Alice'), (2, 'Bob')");
    
    CommandInput input = new CommandInput("select * from test_table", "select", List.of("*", "from", "test_table"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    String rendered = output.toString();
    assertTrue(rendered.contains("Alice"));
    assertTrue(rendered.contains("Bob"));
  }

  @Test
  void updateTableExecutesSuccessfully() throws Exception {
    // Setup test data
    connection.createStatement().execute("CREATE TABLE test_table (id INT, name VARCHAR(50))");
    connection.createStatement().execute("INSERT INTO test_table VALUES (1, 'Alice')");
    
    CommandInput input = new CommandInput("update test_table set name = 'Bob' where id = 1", "update", List.of("test_table", "set", "name", "=", "'Bob'", "where", "id", "=", "1"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Updated 1 rows"));
  }

  @Test
  void deleteFromTableExecutesSuccessfully() throws Exception {
    // Setup test data
    connection.createStatement().execute("CREATE TABLE test_table (id INT, name VARCHAR(50))");
    connection.createStatement().execute("INSERT INTO test_table VALUES (1, 'Alice')");
    
    CommandInput input = new CommandInput("delete from test_table where id = 1", "delete", List.of("from", "test_table", "where", "id", "=", "1"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Updated 1 rows"));
  }

  @Test
  void sqlCommandExecutesQuery() throws Exception {
    connection.createStatement().execute("CREATE TABLE test_table (id INT)");
    connection.createStatement().execute("INSERT INTO test_table VALUES (1)");
    
    CommandInput input = new CommandInput("sql select * from test_table", "sql", List.of("select", "*", "from", "test_table"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
  }

  @Test
  void ddlCommandExecutesUpdate() throws Exception {
    CommandInput input = new CommandInput("ddl create table ddl_test (id int)", "ddl", List.of("create", "table", "ddl_test", "(id", "int)"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(context);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Updated 0 rows"));
  }

  @Test
  void executionWithoutConnectionShowsError() throws Exception {
    // Create context without connection
    CommandContext noConnContext = new CommandContext(
        new PrintWriter(output), 
        Style.box(), 
        RenderService.defaults(), 
        new CommandService(List.of()), 
        80, 
        24);
    
    CommandInput input = new CommandInput("select 1", "select", List.of("1"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    CommandResult result = command.execute(noConnContext);
    context.out.flush();
    assertEquals(CommandResult.SUCCESS, result);
    assertTrue(output.toString().contains("Not connected"));
  }

  @Test
  void invalidSqlThrowsException() throws Exception {
    CommandInput input = new CommandInput("select invalid_syntax", "select", List.of("invalid_syntax"));
    codes.ry.xanadu.command.Command command = commands.commandFor(input);
    
    assertThrows(RuntimeException.class, () -> {
      command.execute(context);
    });
  }

  @Test
  void commandNamesIncludesAllSqlCommands() {
    var names = commands.commandNames();
    assertTrue(names.contains("select"));
    assertTrue(names.contains("insert"));
    assertTrue(names.contains("update"));
    assertTrue(names.contains("delete"));
    assertTrue(names.contains("create"));
    assertTrue(names.contains("alter"));
    assertTrue(names.contains("drop"));
    assertTrue(names.contains("sql"));
    assertTrue(names.contains("ddl"));
  }

  @Test
  void subcommandsList() {
    List<String> createSubs = commands.subcommands("create");
    assertTrue(createSubs.contains("table"));
    assertTrue(createSubs.contains("view"));
    
    List<String> alterSubs = commands.subcommands("alter");
    assertTrue(alterSubs.contains("table"));
    
    List<String> dropSubs = commands.subcommands("drop");
    assertTrue(dropSubs.contains("table"));
  }

  @Test
  void usageReturnsCorrectFormat() {
    List<String> usage = commands.usage("select");
    assertEquals(1, usage.size());
    assertTrue(usage.get(0).contains("select"));
  }
}
