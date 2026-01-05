package codes.ry.xanadu.jdbc;

import codes.ry.xanadu.Frame;
import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.render.TableRenderer;
import java.util.Arrays;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

final class JdbcTableRenderer {
  private final CommandContext context;

  JdbcTableRenderer(CommandContext context) {
    this.context = context;
  }

  void render(ResultSet rs) throws SQLException {
    ResultSetMetaData meta = rs.getMetaData();
    int cols = meta.getColumnCount();
    String[] names = new String[cols];
    for (int i = 0; i < cols; i++) {
      names[i] = meta.getColumnLabel(i + 1);
    }
    int fetchSize = rs.getFetchSize();
    if (fetchSize <= 0) {
      fetchSize = context.maxHeight > 0 ? context.maxHeight : Integer.MAX_VALUE;
    } else if (context.maxHeight > 0) {
      fetchSize = Math.max(fetchSize, context.maxHeight);
    }
    List<Object[]> batch = new ArrayList<>();
    if (!rs.next()) {
      dumpBatch(names, List.of());
      return;
    }
    do {
      Object[] row = new Object[cols];
      for (int i = 0; i < cols; i++) {
        row[i] = rs.getObject(i + 1);
      }
      batch.add(row);
      if (batch.size() >= fetchSize) {
        dumpBatch(names, batch);
        batch.clear();
      }
    } while (rs.next());
    if (!batch.isEmpty()) {
      dumpBatch(names, batch);
    }
  }

  private void dumpBatch(String[] names, List<Object[]> rows) {
    List<List<Object>> values = new ArrayList<>(rows.size());
    for (Object[] row : rows) {
      values.add(Arrays.asList(row));
    }
    Frame table =
        TableRenderer.render(
            context.style,
            context.renderService,
            values,
            Arrays.asList(names),
            TableRenderer.standardAlignment());
    boolean clip = context.clipFrames();
    context.setClipFrames(false);
    try {
      if (table != null) {
        context.render(table);
      }
    } finally {
      context.setClipFrames(clip);
    }
  }
}
