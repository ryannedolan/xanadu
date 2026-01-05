package codes.ry.xanadu.jdbc;

import codes.ry.xanadu.command.Command;
import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandInput;
import codes.ry.xanadu.command.CommandParser;
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

public final class JdbcStatsCommands implements CommandProvider {
  private static final String STATS = "stats";
  private static final java.util.Set<String> QUERY_STARTERS =
      java.util.Set.of("select", "with", "values", "show", "describe", "explain");

  @Override
  public boolean supports(CommandInput input) {
    String name = input.name.toLowerCase(Locale.ROOT);
    if (!STATS.equals(name)) {
      return false;
    }
    if (input.args.isEmpty()) {
      return false;
    }
    if (allNumeric(input.args)) {
      return false;
    }
    if (input.args.size() == 1 && !isQueryStarter(input.args.get(0))) {
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
    return java.util.Set.of(STATS);
  }

  @Override
  public java.util.List<String> usage(String commandName) {
    if (STATS.equals(commandName)) {
      return java.util.List.of("stats <table>", "stats <sql...>");
    }
    return java.util.List.of(commandName);
  }

  private void execute(CommandContext context, CommandInput input) {
    String name = input.name.toLowerCase(Locale.ROOT);
    String sql = buildSql(context, input.args, name);
    if (sql == null) {
      return;
    }
    List<Float> values = queryFirstColumn(context, sql);
    if (values == null || values.isEmpty()) {
      return;
    }
    String raw = buildStatsRaw(values);
    delegateToCore(context, raw);
  }

  private String buildSql(CommandContext context, List<String> args, String name) {
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
    usageError(context, name);
    return null;
  }

  private List<Float> queryFirstColumn(CommandContext context, String sql) {
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
        Float value = coerceNumber(context, rs.getObject(1));
        if (context.failed()) {
          return null;
        }
        values.add(value);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Query failed: " + e.getMessage(), e);
    }
    if (values.isEmpty()) {
      context.error("Query returned no rows.");
      return null;
    }
    return values;
  }

  private void delegateToCore(CommandContext context, String raw) {
    CommandInput delegated = CommandParser.parse(raw);
    if (delegated == null) {
      context.error("Failed to build delegated command.");
      return;
    }
    Optional<Command> command = context.commandService().find(delegated);
    if (command.isEmpty()) {
      context.error("No command available for " + delegated.name + ".");
      return;
    }
    command.get().execute(context);
  }

  private String buildStatsRaw(List<Float> values) {
    StringBuilder sb = new StringBuilder();
    sb.append(STATS).append(' ');
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        sb.append(' ');
      }
      sb.append(values.get(i));
    }
    return sb.toString();
  }

  private Float coerceNumber(CommandContext context, Object value) {
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
      context.error("Non-numeric value in result: " + raw);
      return 0f;
    }
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

  private boolean isQueryStarter(String token) {
    if (token == null) {
      return false;
    }
    return QUERY_STARTERS.contains(token.toLowerCase(Locale.ROOT));
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
}
