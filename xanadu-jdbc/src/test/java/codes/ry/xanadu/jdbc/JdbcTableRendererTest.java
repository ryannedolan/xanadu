package codes.ry.xanadu.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import codes.ry.xanadu.Style;
import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandService;
import codes.ry.xanadu.render.RenderService;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import org.h2.tools.SimpleResultSet;
import org.junit.jupiter.api.Test;

class JdbcTableRendererTest {
  private static final String ANSI_BOLD = "\u001b[1m";
  private static final String ANSI_RESET = "\u001b[0m";

  @Test
  void rendersEmptyResultSetWithHeaderAndBottomBorder() throws SQLException {
    SimpleResultSet rs = new SimpleResultSet();
    rs.addColumn("A", Types.VARCHAR, 5, 0);
    rs.addColumn("B", Types.INTEGER, 3, 0);

    String expected = expectedTable(rs, List.of());
    String output = render(rs);
    assertEquals(expected, output);
  }

  @Test
  void rendersTwoRowTableWithHeaderSeparator() throws SQLException {
    SimpleResultSet rs = new SimpleResultSet();
    rs.addColumn("A", Types.VARCHAR, 5, 0);
    rs.addColumn("B", Types.INTEGER, 3, 0);
    rs.addRow("one", 1);
    rs.addRow("two", 2);

    String expected =
        expectedTable(rs, List.of(new Object[] {"one", 1}, new Object[] {"two", 2}));
    String output = render(rs);
    assertEquals(
        expected,
        output);
  }

  private String render(SimpleResultSet rs) throws SQLException {
    StringWriter output = new StringWriter();
    PrintWriter writer = new PrintWriter(output);
    CommandContext context =
        new CommandContext(
            writer,
            Style.box(),
            RenderService.defaults(),
            new CommandService(List.of()),
            80,
            24);
    new JdbcTableRenderer(context).render(rs);
    writer.flush();
    return output.toString();
  }

  private String expectedTable(SimpleResultSet rs, List<Object[]> rows) throws SQLException {
    var meta = rs.getMetaData();
    int cols = meta.getColumnCount();
    String[] names = new String[cols];
    int[] widths = new int[cols];
    for (int i = 0; i < cols; i++) {
      names[i] = meta.getColumnLabel(i + 1);
      widths[i] = Math.max(1, names[i].length());
    }
    for (int r = 0; r < rows.size(); r++) {
      Object[] row = rows.get(r);
      for (int c = 0; c < cols; c++) {
        String value = row[c] == null ? "" : row[c].toString();
        widths[c] = Math.max(widths[c], value.length());
      }
    }
    List<String> lines = new java.util.ArrayList<>();
    lines.add(borderLine('┌', '┬', '┐', widths, '─'));
    lines.add(ANSI_BOLD + dataLine('│', '│', '│', widths, names, true) + ANSI_RESET);
    lines.add(borderLine('╪', '╪', '╪', widths, '═'));
    for (int r = 0; r < rows.size(); r++) {
      Object[] row = rows.get(r);
      lines.add(dataLine('│', '│', '│', widths, row, false));
      if (r + 1 < rows.size()) {
        lines.add(borderLine('├', '┼', '┤', widths, '─'));
      }
    }
    lines.add(borderLine('└', '┴', '┘', widths, '─'));
    return "\n" + String.join("\n", lines) + "\n";
  }

  private String borderLine(char left, char mid, char right, int[] widths, char fill) {
    StringBuilder sb = new StringBuilder();
    sb.append(left);
    for (int i = 0; i < widths.length; i++) {
      for (int j = 0; j < widths[i]; j++) {
        sb.append(fill);
      }
      sb.append(i + 1 == widths.length ? right : mid);
    }
    return sb.toString();
  }

  private String dataLine(
      char left, char mid, char right, int[] widths, Object[] values, boolean header) {
    StringBuilder sb = new StringBuilder();
    sb.append(left);
    for (int i = 0; i < widths.length; i++) {
      Object value = values[i];
      String text = value == null ? "" : value.toString();
      sb.append(padAligned(text, widths[i], alignmentFor(value, header)));
      sb.append(i + 1 == widths.length ? right : mid);
    }
    return sb.toString();
  }

  private Align alignmentFor(Object value, boolean header) {
    if (header) {
      return Align.CENTER;
    }
    if (value instanceof Number) {
      return Align.RIGHT;
    }
    return Align.LEFT;
  }

  private String padAligned(String value, int width, Align align) {
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

  private enum Align {
    LEFT,
    CENTER,
    RIGHT
  }
}
