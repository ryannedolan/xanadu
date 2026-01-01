package codes.ry.xanadu;

import java.io.PrintWriter;

public class Frame implements Image {
  public final int height;
  public final int width;
  public final Image image;
  public final Style style;
  public final Rect drawRect;

  public Frame(int height, int width, Image image, Style style) {
    this(height, width, image, style, new Rect(0, 0, height, width));
  }

  private Frame(int height, int width, Image image, Style style, Rect drawRect) {
    this.height = height;
    this.width = width;
    this.image = image;
    this.style = style;
    this.drawRect = drawRect;
  }

  public void dump(PrintWriter w) {
    w.append('\n');
    for (int i = 0; i < drawRect.height; i++) {
      for (int j = 0; j < drawRect.width; j++) {
        w.append(at(i, j));
      }
      w.append('\n');
    }
  }

  @Override
  public final char at(int i, int j) {
    int drawI = i + drawRect.top;
    int drawJ = j + drawRect.left;
    return image.at(drawI, drawJ);
  }

  public Frame append(Frame next) {
    int gap = horizontalGap(this, next);
    int offset = width + gap;
    int combinedHeight = Math.max(height, next.height);
    int combinedWidth = width + gap + next.width;
    Image combined = StyledImages.combine(image, StyledImages.offset(next.image, 0, offset), style);
    Rect combinedDraw = union(drawRect, next.drawRect.offset(0, offset));
    return new Frame(combinedHeight, combinedWidth, combined, style, combinedDraw);
  }

  public Frame appendVertical(Frame next) {
    int gap = verticalGap(this, next);
    int offset = height + gap;
    int combinedHeight = height + gap + next.height;
    int combinedWidth = Math.max(width, next.width);
    Image combined = StyledImages.combine(image, StyledImages.offset(next.image, offset, 0), style);
    Rect combinedDraw = union(drawRect, next.drawRect.offset(offset, 0));
    return new Frame(combinedHeight, combinedWidth, combined, style, combinedDraw);
  }

  public Frame border(Style style) {
    Image border = (i, j) -> {
      int maxI = height;
      int maxJ = width;
      if (i < -1 || j < -1 || i > maxI || j > maxJ) {
        return ' ';
      }
      boolean isBorder = i == -1 || i == maxI || j == -1 || j == maxJ;
      if (!isBorder) {
        return ' ';
      }
      int mask = 0;
      if (isBorderCell(i - 1, j, maxI, maxJ)) {
        mask |= 0b1000;
      }
      if (isBorderCell(i, j + 1, maxI, maxJ)) {
        mask |= 0b0100;
      }
      if (isBorderCell(i + 1, j, maxI, maxJ)) {
        mask |= 0b0010;
      }
      if (isBorderCell(i, j - 1, maxI, maxJ)) {
        mask |= 0b0001;
      }
      return style.glyph(mask);
    };
    Image content = StyledImages.clip(image, height, width);
    Image combined = StyledImages.combine(content, border, style);
    Rect borderRect = new Rect(-1, -1, height + 2, width + 2);
    Rect combinedDraw = union(drawRect, borderRect);
    return new Frame(height, width, combined, style, combinedDraw);
  }

  public Frame border() {
    return border(style);
  }

  public Frame box() {
    return border(Style.box());
  }

  public Frame shadow(Style style) {
    Image shadow = (i, j) -> {
      int maxI = height;
      int maxJ = width;
      if (i < 0 || j < 0 || i > maxI || j > maxJ) {
        return ' ';
      }
      boolean isShadow = i == maxI || j == maxJ;
      if (!isShadow) {
        return ' ';
      }
      int mask = 0;
      if (isShadowCell(i - 1, j, maxI, maxJ)) {
        mask |= 0b1000;
      }
      if (isShadowCell(i, j + 1, maxI, maxJ)) {
        mask |= 0b0100;
      }
      if (isShadowCell(i + 1, j, maxI, maxJ)) {
        mask |= 0b0010;
      }
      if (isShadowCell(i, j - 1, maxI, maxJ)) {
        mask |= 0b0001;
      }
      return style.glyph(mask);
    };
    Image content = StyledImages.clip(image, height, width);
    Image combined = StyledImages.combine(shadow, content, style);
    Rect shadowRect = new Rect(0, 0, height + 1, width + 1);
    Rect combinedDraw = union(drawRect, shadowRect);
    return new Frame(height, width, combined, style, combinedDraw);
  }

  public Frame shadow() {
    return shadow(style);
  }

  public Frame dropShadow() {
    return shadow(Style.fill());
  }

  public Frame withImage(Image image) {
    return new Frame(height, width, image, style, drawRect);
  }

  public Frame withDrawRect(Rect drawRect) {
    return new Frame(height, width, image, style, drawRect);
  }

  private static int horizontalGap(Frame left, Frame right) {
    int marginRight = Math.max(0, left.drawRect.right - left.width);
    int marginLeft = Math.max(0, -right.drawRect.left);
    return Math.max(0, marginRight + marginLeft - 1);
  }

  private static int verticalGap(Frame top, Frame bottom) {
    int marginBottom = Math.max(0, top.drawRect.bottom - top.height);
    int marginTop = Math.max(0, -bottom.drawRect.top);
    return Math.max(0, marginBottom + marginTop - 1);
  }

  private static boolean isBorderCell(int i, int j, int maxI, int maxJ) {
    if (i < -1 || j < -1 || i > maxI || j > maxJ) {
      return false;
    }
    return i == -1 || i == maxI || j == -1 || j == maxJ;
  }

  private static boolean isShadowCell(int i, int j, int maxI, int maxJ) {
    if (i < 0 || j < 0 || i > maxI || j > maxJ) {
      return false;
    }
    return i == maxI || j == maxJ;
  }

  private static Rect union(Rect a, Rect b) {
    int top = Math.min(a.top, b.top);
    int left = Math.min(a.left, b.left);
    int bottom = Math.max(a.bottom, b.bottom);
    int right = Math.max(a.right, b.right);
    return new Rect(top, left, bottom - top, right - left);
  }
}
