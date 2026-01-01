package codes.ry.xanadu.jdbc;

import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.ReflectiveCommandProvider;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class JdbcConnectCommands extends ReflectiveCommandProvider {

  public void connect(CommandContext context, String url) {
    connect(context, url, null, null);
  }

  public void connect(CommandContext context, String url, String user, String password) {
    try {
      Connection connection;
      if (user == null && password == null) {
        connection = DriverManager.getConnection(url);
      } else {
        connection = DriverManager.getConnection(url, user, password);
      }
      JdbcSession.setConnection(context, connection);
      context.out.println("Connected.");
      context.out.flush();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to connect: " + e.getMessage(), e);
    }
  }
}
