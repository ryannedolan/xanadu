package codes.ry.xanadu.jdbc;

import codes.ry.xanadu.Frame;
import codes.ry.xanadu.Image;
import codes.ry.xanadu.Style;
import codes.ry.xanadu.TextStyle;
import codes.ry.xanadu.StyledImages;
import codes.ry.xanadu.command.CommandContext;
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
    int[] widths = new int[cols];
    for (int i = 0; i < cols; i++) {
      names[i] = meta.getColumnLabel(i + 1);
      widths[i] = Math.max(1, names[i].length());
    }
    int fetchSize = rs.getFetchSize();
    if (fetchSize <= 0) {
      fetchSize = context.maxHeight > 0 ? context.maxHeight : Integer.MAX_VALUE;
    } else if (context.maxHeight > 0) {
      fetchSize = Math.max(fetchSize, context.maxHeight);
    }
    List<Object[]> batch = new ArrayList<>();
    if (!rs.next()) {
      dumpBatch(names, widths, List.of());
      return;
    }
    do {
      Object[] row = new Object[cols];
      for (int i = 0; i < cols; i++) {
        row[i] = rs.getObject(i + 1);
      }
      batch.add(row);
      if (batch.size() >= fetchSize) {
        dumpBatch(names, widths, batch);
        batch.clear();
      }
    } while (rs.next());
    if (!batch.isEmpty()) {
      dumpBatch(names, widths, batch);
    }
  }

  private void dumpBatch(String[] names, int[] widths, List<Object[]> rows) {
    List<Image[]> rendered = renderRows(rows);
    int[] computed = computeWidths(names, rendered, rows);
    Frame headerRow = rowFrame(names, computed, true, Style.boxHeader());
    Frame table = headerRow;
    for (int i = 0; i < rows.size(); i++) {
      Frame rowFrame = rowFrame(rendered.get(i), rows.get(i), computed);
      table = table.appendVertical(rowFrame);
    }
    boolean clip = context.clipFrames();
    context.setClipFrames(false);
    try {
      context.render(table);
    } finally {
      context.setClipFrames(clip);
    }
  }

  private Frame rowFrame(Image[] images, Object[] row, int[] widths) {
    Frame combined = null;
    for (int i = 0; i < images.length; i++) {
      Image cell = images[i];
      int cellHeight = 1;
      if (cell instanceof Frame) {
        cellHeight = Math.max(1, ((Frame) cell).drawRect.height);
      }
      Align align = alignmentFor(row[i]);
      int contentWidth = Math.min(widths[i], widthForCell(row[i], cell));
      int offset = alignmentOffset(widths[i], contentWidth, align);
      Image aligned = cell.offset(0, offset).limit(cellHeight, widths[i]);
      Frame cellFrame = context.style.frame(cellHeight, widths[i], aligned).border();
      combined = combined == null ? cellFrame : combined.append(cellFrame);
    }
    return combined == null ? context.style.frame(1, 1, Image.flood(' ')) : combined;
  }

  private Frame rowFrame(String[] row, int[] widths, boolean header, Style borderStyle) {
    Frame combined = null;
    for (int i = 0; i < row.length; i++) {
      String value = row[i] == null ? "" : row[i];
      if (header) {
        value = singleLine(value);
      }
      Align align = header ? Align.CENTER : Align.LEFT;
      String padded = alignText(value, widths[i], align);
      Image limited = Image.text(padded);
      if (header) {
        limited = StyledImages.withStyle(limited, TextStyle.BOLD);
      }
      Frame cellFrame = context.style.frame(1, widths[i], limited).border(borderStyle);
      combined = combined == null ? cellFrame : combined.append(cellFrame);
    }
    return combined == null ? context.style.frame(1, 1, Image.flood(' ')) : combined;
  }

  private List<Image[]> renderRows(List<Object[]> rows) {
    var sizingContext = new codes.ry.xanadu.render.RenderContext(context.style, 0, 0, context.renderService, false);
    List<Image[]> rendered = new ArrayList<>(rows.size());
    for (Object[] row : rows) {
      Image[] images = new Image[row.length];
      for (int i = 0; i < row.length; i++) {
        images[i] = context.renderService.render(row[i], sizingContext);
      }
      rendered.add(images);
    }
    return rendered;
  }

  private int[] computeWidths(String[] names, List<Image[]> rendered, List<Object[]> rows) {
    int[] widths = new int[names.length];
    for (int i = 0; i < names.length; i++) {
      widths[i] = Math.max(1, names[i].length());
    }
    for (int r = 0; r < rows.size(); r++) {
      Object[] row = rows.get(r);
      Image[] images = rendered.get(r);
      for (int c = 0; c < row.length; c++) {
        int width = widthForCell(row[c], images[c]);
        widths[c] = Math.max(widths[c], width);
      }
    }
    return widths;
  }

  private int widthForCell(Object value, Image image) {
    if (image instanceof Frame) {
      return Math.max(1, ((Frame) image).drawRect.width);
    }
    if (value == null) {
      return 0;
    }
    return value.toString().length();
  }

  private Align alignmentFor(Object value) {
    if (value instanceof Number) {
      return Align.RIGHT;
    }
    return Align.LEFT;
  }

  private int alignmentOffset(int width, int contentWidth, Align align) {
    if (contentWidth >= width) {
      return 0;
    }
    int padding = width - contentWidth;
    switch (align) {
      case RIGHT:
        return padding;
      case CENTER:
        return padding / 2;
      default:
        return 0;
    }
  }

  private String alignText(String value, int width, Align align) {
    if (value.length() >= width) {
      return value.substring(0, width);
    }
    int padding = width - value.length();
    int left = 0;
    int right = padding;
    if (align == Align.RIGHT) {
      left = padding;
      right = 0;
    } else if (align == Align.CENTER) {
      left = padding / 2;
      right = padding - left;
    }
    StringBuilder sb = new StringBuilder(width);
    for (int i = 0; i < left; i++) {
      sb.append(' ');
    }
    sb.append(value);
    for (int i = 0; i < right; i++) {
      sb.append(' ');
    }
    return sb.toString();
  }

  private String singleLine(String value) {
    return value.replaceAll("\\s+", " ").trim();
  }

  private enum Align {
    LEFT,
    CENTER,
    RIGHT
  }
}
