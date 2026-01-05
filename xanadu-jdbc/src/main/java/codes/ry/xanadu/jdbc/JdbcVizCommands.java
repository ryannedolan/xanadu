package codes.ry.xanadu.jdbc;

import codes.ry.xanadu.command.Command;
import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandInput;
import codes.ry.xanadu.command.CommandProvider;
import codes.ry.xanadu.command.CommandResult;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class JdbcVizCommands implements CommandProvider {
  private static final String BAR = "bar";
  private static final String HBAR = "hbar";
  private static final String SPARK = "spark";
  private static final java.util.Set<String> QUERY_STARTERS =
      java.util.Set.of("select", "with", "values", "show", "describe", "explain");

  @Override
  public boolean supports(CommandInput input) {
    String name = input.name.toLowerCase(Locale.ROOT);
    if (!BAR.equals(name) && !HBAR.equals(name) && !SPARK.equals(name)) {
      return false;
    }
    if (input.args.isEmpty()) {
      return false;
    }
    if (allNumeric(input.args)) {
      return false;
    }
    if (input.args.size() == 1) {
      return true;
    }
    return isQueryStarter(input.args.get(0));
  }

  @Override
  public Command commandFor(CommandInput input) {
    return context -> {
      execute(context, input);
      return CommandResult.SUCCESS;
    };
  }

  @Override
  public java.util.Set<String> commandNames() {
    return java.util.Set.of(BAR, HBAR, SPARK);
  }

  @Override
  public java.util.List<String> usage(String commandName) {
    switch (commandName) {
      case BAR:
        return java.util.List.of("bar <table>", "bar <sql...>");
      case HBAR:
        return java.util.List.of("hbar <table>", "hbar <sql...>");
      case SPARK:
        return java.util.List.of("spark <table>", "spark <sql...>");
      default:
        return java.util.List.of(commandName);
    }
  }

  private void execute(CommandContext context, CommandInput input) {
    String sql = buildSql(context, input.name.toLowerCase(Locale.ROOT), input.args);
    if (sql == null) {
      return;
    }
    List<Float> values = queryValues(context, sql);
    if (values == null || values.isEmpty()) {
      if (values != null) {
        context.error("Query returned no rows.");
      }
      return;
    }
    delegateToCore(context, input.name, values);
  }

  private String buildSql(CommandContext context, String commandName, List<String> args) {
    if (args.isEmpty()) {
      return null;
    }
    String first = args.get(0);
    if (args.size() == 1 && !isQueryStarter(first)) {
      return "select * from " + first;
    }
    if (isQueryStarter(first)) {
      return stripTerminalSemicolon(String.join(" ", args));
    }
    usageError(context, commandName);
    return null;
  }

  private List<Float> queryValues(CommandContext context, String sql) {
    Connection connection = JdbcSession.getConnection(context);
    if (connection == null) {
      context.error("Not connected.");
      return null;
    }
    List<Float> values = new ArrayList<>();
    try (Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {
      ResultSetMetaData meta = rs.getMetaData();
      int columns = meta.getColumnCount();
      if (columns <= 0) {
        context.error("Query returned no columns.");
        return null;
      }
      if (columns > 1) {
        context.warn("Query returned multiple columns; using the first column.");
      }
      while (rs.next()) {
        Object value = rs.getObject(1);
        values.add(coerceNumber(context, value));
        if (context.failed()) {
          return null;
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Query failed: " + e.getMessage(), e);
    }
    return values;
  }

  private float coerceNumber(CommandContext context, Object value) {
    if (value == null) {
      return 0f;
    }
    if (value instanceof Number) {
      return ((Number) value).floatValue();
    }
    String raw = value.toString();
    if (raw == null || raw.isBlank()) {
      return 0f;
    }
    try {
      return Float.parseFloat(raw.trim());
    } catch (NumberFormatException e) {
      context.error("Non-numeric value in first column: " + raw);
      return 0f;
    }
  }

  private void delegateToCore(CommandContext context, String commandName, List<Float> values) {
    List<String> args = new ArrayList<>(values.size());
    for (Float value : values) {
      args.add(Float.toString(value));
    }
    String raw = commandName + " " + String.join(" ", args);
    CommandInput delegated = new CommandInput(raw, commandName, args);
    Optional<Command> command = context.commandService().find(delegated);
    if (command.isEmpty()) {
      context.error("No command available for " + commandName + ".");
      return;
    }
    command.get().execute(context);
  }

  private String stripTerminalSemicolon(String sql) {
    int end = sql.length();
    while (end > 0 && Character.isWhitespace(sql.charAt(end - 1))) {
      end--;
    }
    if (end > 0 && sql.charAt(end - 1) == ';') {
      end--;
    }
    return sql.substring(0, end).trim();
  }

  private void usageError(CommandContext context, String commandName) {
    for (String line : usage(commandName)) {
      context.out.println("Usage: " + line);
    }
    context.out.flush();
  }

  private boolean isQueryStarter(String token) {
    if (token == null) {
      return false;
    }
    return QUERY_STARTERS.contains(token.toLowerCase(Locale.ROOT));
  }

  private boolean allNumeric(List<String> args) {
    for (String raw : args) {
      if (!isNumeric(raw)) {
        return false;
      }
    }
    return true;
  }

  private boolean isNumeric(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    try {
      Float.parseFloat(value);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
