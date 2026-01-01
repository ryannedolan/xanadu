package codes.ry.xanadu.render;

import codes.ry.xanadu.Frame;
import codes.ry.xanadu.Image;
import codes.ry.xanadu.Style;
import codes.ry.xanadu.StyledImage;
import codes.ry.xanadu.TextStyle;
import java.io.PrintWriter;

public final class RenderContext {
  private static final String ANSI_BOLD = "\u001b[1m";
  private static final String ANSI_RESET = "\u001b[0m";
  public final Style style;
  public final int maxWidth;
  public final int maxHeight;
  public final RenderService service;
  public final boolean clipFrames;

  public RenderContext(
      Style style, int maxWidth, int maxHeight, RenderService service, boolean clipFrames) {
    this.style = style;
    this.maxWidth = maxWidth;
    this.maxHeight = maxHeight;
    this.service = service;
    this.clipFrames = clipFrames;
  }

  public void dump(Image image, PrintWriter writer) {
    int height = maxHeight;
    int width = maxWidth;
    Frame frame = null;
    StyledImage styled = null;
    if (image instanceof Frame) {
      frame = (Frame) image;
      height = frame.drawRect.height;
      width = frame.drawRect.width;
      if (frame.image instanceof StyledImage) {
        styled = (StyledImage) frame.image;
      }
      if (clipFrames && maxHeight > 0) {
        height = Math.min(height, maxHeight);
      }
      if (clipFrames && maxWidth > 0) {
        width = Math.min(width, maxWidth);
      }
    } else if (image instanceof StyledImage) {
      styled = (StyledImage) image;
    }
    writer.append('\n');
    for (int i = 0; i < height; i++) {
      TextStyle currentStyle = TextStyle.NORMAL;
      int drawI = frame == null ? i : i + frame.drawRect.top;
      for (int j = 0; j < width; j++) {
        if (styled != null) {
          TextStyle nextStyle = styled.styleAt(drawI, j);
          if (nextStyle != currentStyle) {
            if (nextStyle == TextStyle.BOLD) {
              writer.append(ANSI_BOLD);
            } else {
              writer.append(ANSI_RESET);
            }
            currentStyle = nextStyle;
          }
        }
        writer.append(image.at(i, j));
      }
      if (currentStyle == TextStyle.BOLD) {
        writer.append(ANSI_RESET);
      }
      writer.append('\n');
    }
  }
}
