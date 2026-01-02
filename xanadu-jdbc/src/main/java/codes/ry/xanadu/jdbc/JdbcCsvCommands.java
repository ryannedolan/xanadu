package codes.ry.xanadu.jdbc;

import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandInput;
import codes.ry.xanadu.command.CommandProvider;
import codes.ry.xanadu.command.CommandResult;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class JdbcCsvCommands implements CommandProvider {
  private static final String LOAD_COMMAND = "load";
  private static final String EXTRACT_COMMAND = "extract";
  private static final int DEFAULT_BATCH_SIZE = 500;

  @Override
  public boolean supports(CommandInput input) {
    String name = input.name.toLowerCase(Locale.ROOT);
    return LOAD_COMMAND.equals(name) || EXTRACT_COMMAND.equals(name);
  }

  @Override
  public codes.ry.xanadu.command.Command commandFor(CommandInput input) {
    return context -> {
      execute(context, input);
      return CommandResult.SUCCESS;
    };
  }

  @Override
  public java.util.Set<String> commandNames() {
    return java.util.Set.of(LOAD_COMMAND, EXTRACT_COMMAND);
  }

  @Override
  public java.util.List<String> usage(String commandName) {
    if (LOAD_COMMAND.equalsIgnoreCase(commandName)) {
      return java.util.List.of(
          "load <table> [path] [--header] [--delimiter=,] [--null=VALUE] [--batch=N] [--columns=a,b,c]");
    }
    if (EXTRACT_COMMAND.equalsIgnoreCase(commandName)) {
      return java.util.List.of(
          "extract <table> [--header] [--delimiter=,] [--null=VALUE]",
          "extract <path> [--header] [--delimiter=,] [--null=VALUE] <sql...>");
    }
    return java.util.List.of();
  }

  private void execute(CommandContext context, CommandInput input) {
    String name = input.name.toLowerCase(Locale.ROOT);
    if (LOAD_COMMAND.equals(name)) {
      loadCommand(context, input.args);
      return;
    }
    if (EXTRACT_COMMAND.equals(name)) {
      extractCommand(context, input.args);
      return;
    }
  }

  private void loadCommand(CommandContext context, List<String> args) {
    if (args.isEmpty()) {
      printUsage(context, LOAD_COMMAND);
      return;
    }
    String table = args.get(0);
    String path = null;
    int index = 1;
    if (index < args.size() && looksLikePath(args.get(index))) {
      path = args.get(index);
      index++;
    }
    CsvOptions options = parseOptions(context, args, index, true, true);
    if (options == null) {
      return;
    }
    if (options.nextIndex < args.size()) {
      context.error("Unexpected argument: " + args.get(options.nextIndex));
      return;
    }
    if (path == null) {
      path = defaultPathForTable(table);
    }
    loadCsv(context, table, path, options);
  }

  private void loadCsv(CommandContext context, String table, String path, CsvOptions options) {
    Connection connection = JdbcSession.getConnection(context);
    if (connection == null) {
      context.error("Not connected.");
      return;
    }
    Path csvPath = Path.of(path);
    if (!Files.exists(csvPath)) {
      context.error("CSV file not found: " + path);
      return;
    }
    try {
      List<String> columns = options.columns;
      try (Reader fileReader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8);
          PushbackReader reader = new PushbackReader(fileReader, 1)) {
        if (options.header) {
          List<String> header = readRecord(reader, options.delimiter);
          if (header == null) {
            context.error("CSV file is empty.");
            return;
          }
          if (columns == null) {
            columns = normalizeColumns(header);
            if (columns.size() != header.size()) {
              context.error("CSV header has blank column names.");
              return;
            }
          }
        }
        if (columns == null) {
          columns = resolveColumns(connection, table);
        }
        if (columns.isEmpty()) {
          context.error("No columns resolved for table: " + table);
          return;
        }
        String sql = buildInsertSql(table, columns);
        boolean restoreAutoCommit = connection.getAutoCommit();
        if (restoreAutoCommit) {
          connection.setAutoCommit(false);
        }
        long total = 0;
        int batchCount = 0;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
          List<String> row = readRecord(reader, options.delimiter);
          while (row != null) {
            if (isBlankRow(row)) {
              row = readRecord(reader, options.delimiter);
              continue;
            }
            if (row.size() != columns.size()) {
              context.error(
                  "CSV row has " + row.size() + " columns, expected " + columns.size() + ".");
              if (restoreAutoCommit) {
                connection.rollback();
                connection.setAutoCommit(true);
              }
              return;
            }
            bindRow(stmt, row, options.nullToken);
            stmt.addBatch();
            batchCount++;
            total++;
            if (batchCount >= options.batchSize) {
              stmt.executeBatch();
              batchCount = 0;
            }
            row = readRecord(reader, options.delimiter);
          }
          if (batchCount > 0) {
            stmt.executeBatch();
          }
          if (restoreAutoCommit) {
            connection.commit();
            connection.setAutoCommit(true);
          }
        } catch (SQLException e) {
          if (restoreAutoCommit) {
            connection.rollback();
            connection.setAutoCommit(true);
          }
          throw e;
        }
        context.out.println("Loaded " + total + " rows into " + table + ".");
        context.out.flush();
      }
    } catch (IOException | SQLException e) {
      throw new RuntimeException("CSV load failed: " + e.getMessage(), e);
    }
  }

  private void extractCommand(CommandContext context, List<String> args) {
    if (args.isEmpty()) {
      printUsage(context, EXTRACT_COMMAND);
      return;
    }
    String token = args.get(0);
    if (!looksLikePath(token)) {
      String table = token;
      CsvOptions options = parseOptions(context, args, 1, false, false);
      if (options == null) {
        return;
      }
      if (options.nextIndex < args.size()) {
        context.error("Unexpected argument: " + args.get(options.nextIndex));
        return;
      }
      String path = defaultPathForTable(table);
      String sql = "select * from " + table;
      extractCsv(context, path, sql, options);
      return;
    }
    String path = token;
    CsvOptions options = parseOptions(context, args, 1, false, false);
    if (options == null) {
      return;
    }
    if (options.nextIndex >= args.size()) {
      usageError(context, "extract <path> [--header] [--delimiter=,] [--null=VALUE] <sql...>");
      return;
    }
    String sql = String.join(" ", args.subList(options.nextIndex, args.size()));
    if (sql.isBlank()) {
      context.error("SQL is empty.");
      return;
    }
    Connection connection = JdbcSession.getConnection(context);
    if (connection == null) {
      context.error("Not connected.");
      return;
    }
    Path csvPath = Path.of(path);
    extractCsv(context, path, sql, options);
  }

  private void extractCsv(CommandContext context, String path, String sql, CsvOptions options) {
    Connection connection = JdbcSession.getConnection(context);
    if (connection == null) {
      context.error("Not connected.");
      return;
    }
    Path csvPath = Path.of(path);
    try (BufferedWriter writer = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8);
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {
      ResultSetMetaData meta = rs.getMetaData();
      int columnCount = meta.getColumnCount();
      if (options.header) {
        List<String> header = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
          header.add(meta.getColumnLabel(i));
        }
        writeRecord(writer, header, options.delimiter);
      }
      long total = 0;
      while (rs.next()) {
        List<String> row = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
          Object value = rs.getObject(i);
          if (value == null) {
            row.add(options.nullToken == null ? "" : options.nullToken);
          } else {
            row.add(String.valueOf(value));
          }
        }
        writeRecord(writer, row, options.delimiter);
        total++;
      }
      writer.flush();
      context.out.println("Extracted " + total + " rows to " + path + ".");
      context.out.flush();
    } catch (IOException | SQLException e) {
      throw new RuntimeException("CSV extract failed: " + e.getMessage(), e);
    }
  }

  private CsvOptions parseOptions(
      CommandContext context, List<String> args, int start, boolean allowColumns, boolean allowBatch) {
    CsvOptions options = new CsvOptions();
    int i = start;
    for (; i < args.size(); i++) {
      String arg = args.get(i);
      if (!arg.startsWith("--")) {
        break;
      }
      if ("--header".equalsIgnoreCase(arg)) {
        options.header = true;
        continue;
      }
      if (arg.startsWith("--delimiter=")) {
        String value = arg.substring("--delimiter=".length());
        Character delimiter = parseDelimiter(value);
        if (delimiter == null) {
          context.error("Invalid delimiter: " + value);
          return null;
        }
        options.delimiter = delimiter;
        continue;
      }
      if (arg.startsWith("--null=")) {
        options.nullToken = arg.substring("--null=".length());
        continue;
      }
      if (allowBatch && arg.startsWith("--batch=")) {
        String value = arg.substring("--batch=".length());
        Integer batch = parseInt(value);
        if (batch == null || batch <= 0) {
          context.error("Invalid batch size: " + value);
          return null;
        }
        options.batchSize = batch;
        continue;
      }
      if (allowColumns && arg.startsWith("--columns=")) {
        String value = arg.substring("--columns=".length());
        if (value.isBlank()) {
          context.error("Columns list is empty.");
          return null;
        }
        options.columns = normalizeColumns(List.of(value.split(",")));
        if (options.columns.isEmpty()) {
          context.error("Columns list is empty.");
          return null;
        }
        continue;
      }
      context.error("Unknown option: " + arg);
      return null;
    }
    options.nextIndex = i;
    return options;
  }

  private List<String> resolveColumns(Connection connection, String table) throws SQLException {
    TableName name = splitTableName(connection, table);
    DatabaseMetaData meta = connection.getMetaData();
    List<ColumnInfo> columns = new ArrayList<>();
    try (ResultSet rs = meta.getColumns(name.catalog, name.schema, name.table, null)) {
      while (rs.next()) {
        int ordinal = rs.getInt("ORDINAL_POSITION");
        String column = rs.getString("COLUMN_NAME");
        columns.add(new ColumnInfo(ordinal, column));
      }
    }
    columns.sort(Comparator.comparingInt(c -> c.ordinal));
    List<String> names = new ArrayList<>(columns.size());
    for (ColumnInfo column : columns) {
      names.add(column.name);
    }
    return names;
  }

  private boolean looksLikePath(String value) {
    String extension = extensionOf(value);
    return "csv".equalsIgnoreCase(extension);
  }

  private String extensionOf(String value) {
    if (value == null) {
      return "";
    }
    int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
    int dot = value.lastIndexOf('.');
    if (dot <= slash) {
      return "";
    }
    return value.substring(dot + 1);
  }

  private String defaultPathForTable(String table) {
    return table + ".csv";
  }

  private TableName splitTableName(Connection connection, String table) throws SQLException {
    String catalog = connection.getCatalog();
    String schema = null;
    String tableName = table;
    String[] parts = table.split("\\.");
    if (parts.length == 2) {
      schema = parts[0];
      tableName = parts[1];
    } else if (parts.length >= 3) {
      catalog = parts[0];
      schema = parts[1];
      tableName = parts[2];
    }
    return new TableName(catalog, schema, tableName);
  }

  private String buildInsertSql(String table, List<String> columns) {
    StringBuilder sb = new StringBuilder();
    sb.append("insert into ").append(table).append(" (");
    for (int i = 0; i < columns.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(columns.get(i));
    }
    sb.append(") values (");
    for (int i = 0; i < columns.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append('?');
    }
    sb.append(')');
    return sb.toString();
  }

  private void bindRow(PreparedStatement stmt, List<String> row, String nullToken) throws SQLException {
    for (int i = 0; i < row.size(); i++) {
      String value = row.get(i);
      if (nullToken != null && nullToken.equals(value)) {
        stmt.setObject(i + 1, null);
      } else {
        stmt.setObject(i + 1, value);
      }
    }
  }

  private boolean isBlankRow(List<String> row) {
    for (String value : row) {
      if (value != null && !value.isBlank()) {
        return false;
      }
    }
    return true;
  }

  private List<String> normalizeColumns(List<String> columns) {
    List<String> normalized = new ArrayList<>(columns.size());
    for (String column : columns) {
      String trimmed = column == null ? "" : column.trim();
      if (!trimmed.isEmpty()) {
        normalized.add(trimmed);
      }
    }
    return normalized;
  }

  private void writeRecord(BufferedWriter writer, List<String> row, char delimiter) throws IOException {
    for (int i = 0; i < row.size(); i++) {
      if (i > 0) {
        writer.write(delimiter);
      }
      writer.write(escapeField(row.get(i), delimiter));
    }
    writer.newLine();
  }

  private String escapeField(String value, char delimiter) {
    if (value == null) {
      return "";
    }
    boolean needsQuotes = false;
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '"' || c == '\n' || c == '\r' || c == delimiter) {
        needsQuotes = true;
        break;
      }
    }
    if (!needsQuotes) {
      return value;
    }
    String escaped = value.replace("\"", "\"\"");
    return "\"" + escaped + "\"";
  }

  private List<String> readRecord(PushbackReader reader, char delimiter) throws IOException {
    List<String> fields = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    boolean inQuotes = false;
    boolean sawAny = false;
    while (true) {
      int next = reader.read();
      if (next == -1) {
        if (!sawAny) {
          return null;
        }
        fields.add(field.toString());
        return fields;
      }
      char c = (char) next;
      sawAny = true;
      if (inQuotes) {
        if (c == '"') {
          int peek = reader.read();
          if (peek == '"') {
            field.append('"');
          } else {
            inQuotes = false;
            if (peek == delimiter) {
              fields.add(field.toString());
              field.setLength(0);
            } else if (peek == '\n') {
              fields.add(field.toString());
              return fields;
            } else if (peek == '\r') {
              int after = reader.read();
              if (after != '\n' && after != -1) {
                reader.unread(after);
              }
              fields.add(field.toString());
              return fields;
            } else if (peek != -1) {
              field.append((char) peek);
            } else {
              fields.add(field.toString());
              return fields;
            }
          }
        } else {
          field.append(c);
        }
        continue;
      }
      if (c == '"') {
        inQuotes = true;
        continue;
      }
      if (c == delimiter) {
        fields.add(field.toString());
        field.setLength(0);
        continue;
      }
      if (c == '\n') {
        fields.add(field.toString());
        return fields;
      }
      if (c == '\r') {
        int after = reader.read();
        if (after != '\n' && after != -1) {
          reader.unread(after);
        }
        fields.add(field.toString());
        return fields;
      }
      field.append(c);
    }
  }

  private Character parseDelimiter(String value) {
    if (value == null || value.isEmpty()) {
      return null;
    }
    if ("\\t".equals(value) || "\t".equals(value) || "tab".equalsIgnoreCase(value)) {
      return '\t';
    }
    if (value.length() == 1) {
      return value.charAt(0);
    }
    return null;
  }

  private Integer parseInt(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private void usageError(CommandContext context, String message) {
    context.out.println("Usage: " + message);
    context.out.flush();
  }

  private void printUsage(CommandContext context, String commandName) {
    for (String line : usage(commandName)) {
      context.out.println("Usage: " + line);
    }
    context.out.flush();
  }

  private static final class CsvOptions {
    boolean header;
    char delimiter = ',';
    String nullToken;
    int batchSize = DEFAULT_BATCH_SIZE;
    List<String> columns;
    int nextIndex;
  }

  private static final class ColumnInfo {
    final int ordinal;
    final String name;

    private ColumnInfo(int ordinal, String name) {
      this.ordinal = ordinal;
      this.name = name;
    }
  }

  private static final class TableName {
    final String catalog;
    final String schema;
    final String table;

    private TableName(String catalog, String schema, String table) {
      this.catalog = catalog;
      this.schema = schema;
      this.table = table;
    }
  }
}
