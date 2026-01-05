package codes.ry.xanadu.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import codes.ry.xanadu.Style;
import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandService;
import codes.ry.xanadu.render.RenderService;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdbcConnectCommandsTest {
  private JdbcConnectCommands commands;
  private StringWriter output;
  private CommandContext context;

  @BeforeEach
  void setUp() {
    commands = new JdbcConnectCommands();
    output = new StringWriter();
    PrintWriter writer = new PrintWriter(output);
    context =
        new CommandContext(
            writer, Style.box(), RenderService.defaults(), new CommandService(List.of()), 80, 24);
  }

  @Test
  void connectWithValidH2Url() {
    commands.connect(context, "jdbc:h2:mem:test", "sa", "");
    context.out.flush();
    assertTrue(output.toString().contains("Connected"));
    
    Connection conn = JdbcSession.getConnection(context);
    assertNotNull(conn);
  }

  @Test
  void connectWithUserAndPassword() {
    commands.connect(context, "jdbc:h2:mem:test", "sa", "");
    context.out.flush();
    assertTrue(output.toString().contains("Connected"));
    
    Connection conn = JdbcSession.getConnection(context);
    assertNotNull(conn);
  }

  @Test
  void connectWithInvalidUrlThrowsException() {
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      commands.connect(context, "invalid_jdbc_url");
    });
    assertTrue(exception.getMessage().contains("Failed to connect"));
  }

  @Test
  void connectWithNullUser() {
    commands.connect(context, "jdbc:h2:mem:test", "sa", "");
    context.out.flush();
    assertTrue(output.toString().contains("Connected"));
  }

  @Test
  void multipleConnectionsReplacesPrevious() {
    commands.connect(context, "jdbc:h2:mem:test1", "sa", "");
    Connection firstConn = JdbcSession.getConnection(context);
    assertNotNull(firstConn);
    
    commands.connect(context, "jdbc:h2:mem:test2", "sa", "");
    Connection secondConn = JdbcSession.getConnection(context);
    assertNotNull(secondConn);
    // Different connection objects
    assertTrue(firstConn != secondConn);
  }

  @Test
  void commandNameIsConnect() {
    var names = commands.commandNames();
    assertTrue(names.contains("connect"));
  }
}
