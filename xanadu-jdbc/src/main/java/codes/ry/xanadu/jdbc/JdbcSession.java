package codes.ry.xanadu.jdbc;

import codes.ry.xanadu.command.CommandContext;
import java.sql.Connection;

final class JdbcSession {
  static final String CONNECTION_KEY = "jdbc.connection";

  private JdbcSession() {}

  static Connection getConnection(CommandContext context) {
    return context.get(CONNECTION_KEY, Connection.class);
  }

  static void setConnection(CommandContext context, Connection connection) {
    context.put(CONNECTION_KEY, connection);
  }
}
