package codes.ry.xanadu.render;

import codes.ry.xanadu.Frame;
import codes.ry.xanadu.Image;
import codes.ry.xanadu.Style;
import codes.ry.xanadu.StyledImage;
import codes.ry.xanadu.StyledImages;
import codes.ry.xanadu.TextStyle;
import java.util.ArrayList;
import java.util.List;

public final class TableRenderer {
  private TableRenderer() {}

  public enum Align {
    LEFT,
    CENTER,
    RIGHT
  }

  @FunctionalInterface
  public interface CellAlignment {
    Align alignment(int row, int col, Object value, boolean header);
  }

  public static CellAlignment standardAlignment() {
    return (row, col, value, header) -> {
      if (header) {
        return Align.CENTER;
      }
      return value instanceof Number ? Align.RIGHT : Align.LEFT;
    };
  }

  public static CellAlignment rightAlignment() {
    return (row, col, value, header) -> Align.RIGHT;
  }

  public static Frame render(
      Style style,
      RenderService renderService,
      List<? extends List<?>> rows,
      List<?> header,
      CellAlignment alignment) {
    return render(style, renderService, rows, header, Style.boxHeader(), alignment);
  }

  public static Frame render(
      Style style,
      RenderService renderService,
      List<? extends List<?>> rows,
      List<?> header,
      Style headerStyle,
      CellAlignment alignment) {
    if ((rows == null || rows.isEmpty()) && (header == null || header.isEmpty())) {
      return null;
    }
    int cols = 0;
    if (header != null) {
      cols = Math.max(cols, header.size());
    }
    if (rows != null) {
      for (List<?> row : rows) {
        cols = Math.max(cols, row == null ? 0 : row.size());
      }
    }
    if (cols == 0) {
      return null;
    }
    int[] widths = new int[cols];
    RenderContext sizing = new RenderContext(style, 0, 0, renderService, false);
    Image[] headerImages = null;
    if (header != null && !header.isEmpty()) {
      headerImages = new Image[cols];
      for (int i = 0; i < cols; i++) {
        Object value = cellValue(header, i);
        Image image = renderService.render(value, sizing);
        image = StyledImages.withStyle(image, TextStyle.BOLD);
        headerImages[i] = image;
        widths[i] = Math.max(widths[i], widthForCell(value, image));
      }
    }
    List<Image[]> rowImages = new ArrayList<>();
    if (rows != null) {
      for (List<?> row : rows) {
        Image[] images = new Image[cols];
        for (int i = 0; i < cols; i++) {
          Object value = cellValue(row, i);
          Image image = renderService.render(value, sizing);
          images[i] = image;
          widths[i] = Math.max(widths[i], widthForCell(value, image));
        }
        rowImages.add(images);
      }
    }
    Frame table = null;
    if (headerImages != null) {
      Frame headerFrame = buildRow(style, headerStyle, widths, headerImages, header, alignment, true, 0);
      table = headerFrame;
    }
    for (int r = 0; r < rowImages.size(); r++) {
      Image[] images = rowImages.get(r);
      List<?> rowValues = rows.get(r);
      Frame rowFrame = buildRow(style, style, widths, images, rowValues, alignment, false, r);
      table = table == null ? rowFrame : table.appendVertical(rowFrame);
    }
    return table;
  }

  private static Frame buildRow(
      Style style,
      Style borderStyle,
      int[] widths,
      Image[] images,
      List<?> values,
      CellAlignment alignment,
      boolean header,
      int rowIndex) {
    Frame rowFrame = null;
    for (int i = 0; i < widths.length; i++) {
      Object value = cellValue(values, i);
      Image cell = images[i];
      int cellHeight = 1;
      if (cell instanceof Frame) {
        cellHeight = Math.max(1, ((Frame) cell).drawRect.height);
      }
      int contentWidth = Math.min(widths[i], widthForCell(value, cell));
      Align align = alignment.alignment(rowIndex, i, value, header);
      int offset = alignmentOffset(widths[i], contentWidth, align);
      Image aligned = offsetImage(cell, 0, offset);
      aligned = limitImage(aligned, cellHeight, widths[i]);
      Frame cellFrame = style.frame(cellHeight, widths[i], aligned).border(borderStyle);
      rowFrame = rowFrame == null ? cellFrame : rowFrame.append(cellFrame);
    }
    return rowFrame == null ? style.frame(1, 1, Image.flood(' ')) : rowFrame;
  }

  private static Object cellValue(List<?> values, int index) {
    if (values == null || index >= values.size()) {
      return "";
    }
    Object value = values.get(index);
    return value == null ? "" : value;
  }

  private static int widthForCell(Object value, Image image) {
    if (image instanceof Frame) {
      return Math.max(1, ((Frame) image).drawRect.width);
    }
    if (value == null) {
      return 0;
    }
    return value.toString().length();
  }

  private static int alignmentOffset(int width, int contentWidth, Align align) {
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

  private static Image offsetImage(Image image, int di, int dj) {
    if (image instanceof StyledImage) {
      return StyledImages.offset(image, di, dj);
    }
    return image.offset(di, dj);
  }

  private static Image limitImage(Image image, int height, int width) {
    if (image instanceof StyledImage) {
      return StyledImages.limit(image, height, width);
    }
    return image.limit(height, width);
  }
}
