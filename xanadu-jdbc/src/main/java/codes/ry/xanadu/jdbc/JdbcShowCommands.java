package codes.ry.xanadu.jdbc;

import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandInput;
import codes.ry.xanadu.command.CommandProvider;
import codes.ry.xanadu.command.CommandResult;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public final class JdbcShowCommands implements CommandProvider {
  private static final java.util.List<String> SUBCOMMANDS =
      java.util.List.of(
          "tables",
          "schemas",
          "catalogs",
          "table-types",
          "types",
          "columns",
          "primary-keys",
          "imported-keys",
          "exported-keys",
          "indexes",
          "procedures",
          "functions",
          "procedure-columns",
          "function-columns",
          "udts",
          "clientinfo",
          "attributes");

  @Override
  public boolean supports(CommandInput input) {
    return "show".equalsIgnoreCase(input.name) && !input.args.isEmpty();
  }

  @Override
  public codes.ry.xanadu.command.Command commandFor(CommandInput input) {
    String type = input.args.get(0).toLowerCase(Locale.ROOT);
    java.util.List<String> args = input.args.subList(1, input.args.size());
    return context -> {
      execute(context, type, args);
      return CommandResult.SUCCESS;
    };
  }

  @Override
  public java.util.Set<String> commandNames() {
    return java.util.Set.of("show");
  }

  @Override
  public java.util.List<String> subcommands(String commandName) {
    if ("show".equalsIgnoreCase(commandName)) {
      return SUBCOMMANDS;
    }
    return java.util.List.of();
  }

  @Override
  public java.util.List<String> usage(String commandName) {
    if (!"show".equalsIgnoreCase(commandName)) {
      return java.util.List.of();
    }
    return java.util.List.of(
        "show tables [pattern] [schema] [catalog]",
        "show schemas",
        "show catalogs",
        "show table-types",
        "show types",
        "show columns <table> [schema] [catalog] [columnPattern]",
        "show primary-keys <table> [schema] [catalog]",
        "show imported-keys <table> [schema] [catalog]",
        "show exported-keys <table> [schema] [catalog]",
        "show indexes <table> [schema] [catalog]",
        "show procedures [pattern] [schema] [catalog]",
        "show functions [pattern] [schema] [catalog]",
        "show procedure-columns <procedure> [schema] [catalog] [columnPattern]",
        "show function-columns <function> [schema] [catalog] [columnPattern]",
        "show udts [pattern] [schema] [catalog]",
        "show clientinfo",
        "show attributes <type> [schema] [catalog] [attributePattern]");
  }

  private void execute(CommandContext context, String type, java.util.List<String> args) {
    Connection connection = JdbcSession.getConnection(context);
    if (connection == null) {
      context.error("Not connected.");
      return;
    }
    try {
      DatabaseMetaData meta = connection.getMetaData();
      ResultSet rs = null;
      switch (type) {
        case "tables":
          rs =
              meta.getTables(
                  arg(args, 2, connection.getCatalog()),
                  arg(args, 1, null),
                  patternOrAll(arg(args, 0, null)),
                  null);
          break;
        case "schemas":
          rs = meta.getSchemas();
          break;
        case "catalogs":
          rs = meta.getCatalogs();
          break;
        case "tabletypes":
        case "table-types":
          rs = meta.getTableTypes();
          break;
        case "typeinfo":
        case "types":
          rs = meta.getTypeInfo();
          break;
        case "columns":
          if (!requireArgs(context, type, args, 1)) {
            return;
          }
          rs =
              meta.getColumns(
                  arg(args, 2, connection.getCatalog()),
                  arg(args, 1, null),
                  arg(args, 0, null),
                  patternOrAll(arg(args, 3, null)));
          break;
        case "primarykeys":
        case "primary-keys":
          if (!requireArgs(context, type, args, 1)) {
            return;
          }
          rs =
              meta.getPrimaryKeys(
                  arg(args, 2, connection.getCatalog()),
                  arg(args, 1, null),
                  arg(args, 0, null));
          break;
        case "importedkeys":
        case "imported-keys":
          if (!requireArgs(context, type, args, 1)) {
            return;
          }
          rs =
              meta.getImportedKeys(
                  arg(args, 2, connection.getCatalog()),
                  arg(args, 1, null),
                  arg(args, 0, null));
          break;
        case "exportedkeys":
        case "exported-keys":
          if (!requireArgs(context, type, args, 1)) {
            return;
          }
          rs =
              meta.getExportedKeys(
                  arg(args, 2, connection.getCatalog()),
                  arg(args, 1, null),
                  arg(args, 0, null));
          break;
        case "indexes":
        case "indexinfo":
          if (!requireArgs(context, type, args, 1)) {
            return;
          }
          rs =
              meta.getIndexInfo(
                  arg(args, 2, connection.getCatalog()),
                  arg(args, 1, null),
                  arg(args, 0, null),
                  false,
                  false);
          break;
        case "procedures":
          rs =
              meta.getProcedures(
                  arg(args, 2, connection.getCatalog()),
                  arg(args, 1, null),
                  patternOrAll(arg(args, 0, null)));
          break;
        case "functions":
          rs =
              meta.getFunctions(
                  arg(args, 2, connection.getCatalog()),
                  arg(args, 1, null),
                  patternOrAll(arg(args, 0, null)));
          break;
        case "procedurecolumns":
        case "procedure-columns":
          if (!requireArgs(context, type, args, 1)) {
            return;
          }
          rs =
              meta.getProcedureColumns(
                  arg(args, 2, connection.getCatalog()),
                  arg(args, 1, null),
                  arg(args, 0, null),
                  patternOrAll(arg(args, 3, null)));
          break;
        case "functioncolumns":
        case "function-columns":
          if (!requireArgs(context, type, args, 1)) {
            return;
          }
          rs =
              meta.getFunctionColumns(
                  arg(args, 2, connection.getCatalog()),
                  arg(args, 1, null),
                  arg(args, 0, null),
                  patternOrAll(arg(args, 3, null)));
          break;
        case "udts":
        case "udt":
          rs =
              meta.getUDTs(
                  arg(args, 2, connection.getCatalog()),
                  arg(args, 1, null),
                  patternOrAll(arg(args, 0, null)),
                  null);
          break;
        case "clients":
        case "clientinfo":
          rs = meta.getClientInfoProperties();
          break;
        case "attributes":
          if (!requireArgs(context, type, args, 1)) {
            return;
          }
          rs =
              meta.getAttributes(
                  arg(args, 2, connection.getCatalog()),
                  arg(args, 1, null),
                  arg(args, 0, null),
                  patternOrAll(arg(args, 3, null)));
          break;
        default:
          context.out.println("Unknown show command: " + type);
          context.out.flush();
          return;
      }
      try {
        new JdbcTableRenderer(context).render(rs);
      } finally {
        rs.close();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Show failed: " + e.getMessage(), e);
    }
  }

  private String patternOrAll(String pattern) {
    return pattern == null || pattern.isBlank() ? "%" : pattern;
  }

  private boolean requireArgs(CommandContext context, String type, java.util.List<String> args, int count) {
    if (args.size() < count) {
      context.out.println("Usage: show " + type + " <name> [schema] [catalog] [pattern]");
      context.out.flush();
      return false;
    }
    return true;
  }

  private String arg(java.util.List<String> args, int index, String fallback) {
    if (index < 0 || index >= args.size()) {
      return fallback;
    }
    String value = args.get(index);
    return value.isBlank() ? fallback : value;
  }
}
