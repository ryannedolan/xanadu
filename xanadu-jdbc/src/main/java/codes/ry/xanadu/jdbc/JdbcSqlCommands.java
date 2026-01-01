package codes.ry.xanadu.jdbc;

import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandInput;
import codes.ry.xanadu.command.CommandProvider;
import codes.ry.xanadu.command.CommandResult;
import codes.ry.xanadu.command.Continuation;
import codes.ry.xanadu.command.ContinuationResult;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public final class JdbcSqlCommands implements CommandProvider {
  private static final java.util.Set<String> QUERY_COMMANDS =
      java.util.Set.of("select", "values");
  private static final java.util.Set<String> UPDATE_COMMANDS =
      java.util.Set.of("insert", "update", "delete", "merge");
  private static final java.util.Set<String> DDL_COMMANDS =
      java.util.Set.of("create", "alter", "drop", "truncate");
  private static final String SQL_COMMAND = "sql";
  private static final String DDL_COMMAND = "ddl";
  private static final java.util.Set<String> QUERY_STARTERS =
      java.util.Set.of("select", "with", "values", "show", "describe", "explain");

  private static final java.util.List<String> CREATE_SUBCOMMANDS =
      java.util.List.of("table", "view", "index", "schema", "database");
  private static final java.util.List<String> ALTER_SUBCOMMANDS =
      java.util.List.of("table", "view", "index", "schema", "database");
  private static final java.util.List<String> DROP_SUBCOMMANDS =
      java.util.List.of("table", "view", "index", "schema", "database");
  private static final java.util.List<String> TRUNCATE_SUBCOMMANDS =
      java.util.List.of("table");

  @Override
  public boolean supports(CommandInput input) {
    String name = input.name.toLowerCase(Locale.ROOT);
    return QUERY_COMMANDS.contains(name)
        || UPDATE_COMMANDS.contains(name)
        || DDL_COMMANDS.contains(name)
        || SQL_COMMAND.equals(name)
        || DDL_COMMAND.equals(name);
  }

  @Override
  public codes.ry.xanadu.command.Command commandFor(CommandInput input) {
    String name = input.name.toLowerCase(Locale.ROOT);
    String sql = input.tail();
    return context -> {
      execute(context, name, sql);
      return CommandResult.SUCCESS;
    };
  }

  @Override
  public java.util.Set<String> commandNames() {
    java.util.Set<String> names = new java.util.TreeSet<>();
    names.addAll(QUERY_COMMANDS);
    names.addAll(UPDATE_COMMANDS);
    names.addAll(DDL_COMMANDS);
    names.add(SQL_COMMAND);
    names.add(DDL_COMMAND);
    return names;
  }

  @Override
  public java.util.List<String> subcommands(String commandName) {
    switch (commandName) {
      case "create":
        return CREATE_SUBCOMMANDS;
      case "alter":
        return ALTER_SUBCOMMANDS;
      case "drop":
        return DROP_SUBCOMMANDS;
      case "truncate":
        return TRUNCATE_SUBCOMMANDS;
      default:
        return java.util.List.of();
    }
  }

  @Override
  public java.util.List<String> usage(String commandName) {
    if (QUERY_COMMANDS.contains(commandName)) {
      return java.util.List.of(commandName + " <sql tail> (omit leading keyword)");
    }
    if (UPDATE_COMMANDS.contains(commandName)) {
      return java.util.List.of(commandName + " <sql tail> (omit leading keyword)");
    }
    if (DDL_COMMANDS.contains(commandName)) {
      return java.util.List.of(commandName + " <sql tail> (omit leading keyword)");
    }
    if (SQL_COMMAND.equals(commandName)) {
      return java.util.List.of("sql [sql]");
    }
    if (DDL_COMMAND.equals(commandName)) {
      return java.util.List.of("ddl <sql>");
    }
    return java.util.List.of();
  }

  private void execute(CommandContext context, String name, String sql) {
    if (sql.isEmpty()) {
      if (context.allowContinuation()) {
        startContinuation(context, name, sql);
        return;
      }
      context.error("SQL is empty.");
      return;
    }
    boolean hasSemicolon = endsWithSemicolon(sql);
    if (!hasSemicolon && context.allowContinuation()) {
      startContinuation(context, name, sql);
      return;
    }
    if (hasSemicolon) {
      sql = stripTerminalSemicolon(sql);
    }
    String statement = sql;
    if (!SQL_COMMAND.equals(name) && !DDL_COMMAND.equals(name)) {
      statement = name + " " + sql;
    }
    Connection connection = JdbcSession.getConnection(context);
    if (connection == null) {
      context.error("Not connected.");
      return;
    }
    try (Statement stmt = connection.createStatement()) {
      if (DDL_COMMAND.equals(name)) {
        int count = stmt.executeUpdate(statement);
        context.out.println("Updated " + count + " rows.");
        context.out.flush();
      } else if (SQL_COMMAND.equals(name)) {
        boolean isQuery = isLikelyQuery(statement);
        boolean executed = false;
        if (isQuery) {
          try (ResultSet rs = stmt.executeQuery(statement)) {
            new JdbcTableRenderer(context).render(rs);
            executed = true;
          }
        } else {
          try {
            int count = stmt.executeUpdate(statement);
            context.out.println("Updated " + count + " rows.");
            context.out.flush();
            executed = true;
          } catch (SQLException ignored) {
            // Fall back to execute() below.
          }
        }
        if (!executed) {
          boolean hasResultSet = stmt.execute(statement);
          if (hasResultSet) {
            try (ResultSet rs = stmt.getResultSet()) {
              if (rs != null) {
                new JdbcTableRenderer(context).render(rs);
              }
            }
          } else {
            int count = stmt.getUpdateCount();
            context.out.println("Updated " + count + " rows.");
            context.out.flush();
          }
        }
      } else if (QUERY_COMMANDS.contains(name)) {
        try (ResultSet rs = stmt.executeQuery(statement)) {
          new JdbcTableRenderer(context).render(rs);
        }
      } else {
        int count = stmt.executeUpdate(statement);
        context.out.println("Updated " + count + " rows.");
        context.out.flush();
      }
    } catch (SQLException e) {
      throw new RuntimeException("SQL failed: " + e.getMessage(), e);
    }
  }

  private void startContinuation(CommandContext context, String name, String sql) {
    StringBuilder buffer = new StringBuilder(sql);
    context.continueWith(
        new Continuation(
            name,
            (line, ctx) -> {
              if (buffer.length() > 0) {
                buffer.append('\n');
              }
              buffer.append(line);
              String combined = buffer.toString();
              if (!endsWithSemicolon(combined)) {
                return ContinuationResult.continueWithoutExecution();
              }
              return ContinuationResult.executeAndEnd(combined);
            }));
  }

  private boolean endsWithSemicolon(String sql) {
    int i = sql.length() - 1;
    while (i >= 0 && Character.isWhitespace(sql.charAt(i))) {
      i--;
    }
    return i >= 0 && sql.charAt(i) == ';';
  }

  private String stripTerminalSemicolon(String sql) {
    int end = sql.length();
    while (end > 0 && Character.isWhitespace(sql.charAt(end - 1))) {
      end--;
    }
    while (end > 0 && sql.charAt(end - 1) == ';') {
      end--;
      while (end > 0 && Character.isWhitespace(sql.charAt(end - 1))) {
        end--;
      }
    }
    return sql.substring(0, end).trim();
  }

  private boolean isLikelyQuery(String statement) {
    if (statement == null) {
      return false;
    }
    int i = 0;
    while (i < statement.length() && Character.isWhitespace(statement.charAt(i))) {
      i++;
    }
    if (i >= statement.length()) {
      return false;
    }
    int start = i;
    while (i < statement.length() && Character.isLetter(statement.charAt(i))) {
      i++;
    }
    if (start == i) {
      return false;
    }
    String token = statement.substring(start, i).toLowerCase(Locale.ROOT);
    return QUERY_STARTERS.contains(token);
  }
}
