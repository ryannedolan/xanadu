package codes.ry.xanadu;

public final class StyledImages {
  private StyledImages() {}

  public static Image withStyle(Image base, TextStyle style) {
    if (style == null || style == TextStyle.NORMAL) {
      return base;
    }
    return new StyledImageWrapper(
        base,
        (i, j) -> {
          TextStyle baseStyle = styleAt(base, i, j);
          if (base.at(i, j) == ' ') {
            return baseStyle;
          }
          return merge(baseStyle, style);
        });
  }

  public static Image combine(Image a, Image b, Style style) {
    if (!(a instanceof StyledImage) && !(b instanceof StyledImage)) {
      return a.combine(b, style);
    }
    return new StyledImageWrapper(
        (i, j) -> style.combine(a.at(i, j), b.at(i, j)),
        (i, j) -> merge(styleAt(a, i, j), styleAt(b, i, j)));
  }

  public static Image overlay(Image base, Image overlay) {
    if (!(base instanceof StyledImage) && !(overlay instanceof StyledImage)) {
      return base.overlay(overlay);
    }
    return new StyledImageWrapper(
        (i, j) -> {
          char c = overlay.at(i, j);
          return c != ' ' ? c : base.at(i, j);
        },
        (i, j) -> merge(styleAt(base, i, j), styleAt(overlay, i, j)));
  }

  public static Image offset(Image image, int di, int dj) {
    if (!(image instanceof StyledImage)) {
      return image.offset(di, dj);
    }
    return new StyledImageWrapper(
        (i, j) -> image.at(i - di, j - dj),
        (i, j) -> styleAt(image, i - di, j - dj));
  }

  public static Image limit(Image image, int height, int width) {
    if (!(image instanceof StyledImage)) {
      return image.limit(height, width);
    }
    return new StyledImageWrapper(
        (i, j) -> {
          if (i >= height || j >= width) {
            return ' ';
          }
          return image.at(i, j);
        },
        (i, j) -> {
          if (i >= height || j >= width) {
            return TextStyle.NORMAL;
          }
          return styleAt(image, i, j);
        });
  }

  public static Image clip(Image image, int height, int width) {
    if (!(image instanceof StyledImage)) {
      return (i, j) -> {
        if (i < 0 || j < 0 || i >= height || j >= width) {
          return ' ';
        }
        return image.at(i, j);
      };
    }
    return new StyledImageWrapper(
        (i, j) -> {
          if (i < 0 || j < 0 || i >= height || j >= width) {
            return ' ';
          }
          return image.at(i, j);
        },
        (i, j) -> {
          if (i < 0 || j < 0 || i >= height || j >= width) {
            return TextStyle.NORMAL;
          }
          return styleAt(image, i, j);
        });
  }

  private static TextStyle styleAt(Image image, int i, int j) {
    if (image instanceof StyledImage) {
      return ((StyledImage) image).styleAt(i, j);
    }
    return TextStyle.NORMAL;
  }

  private static TextStyle merge(TextStyle a, TextStyle b) {
    if (a == TextStyle.BOLD || b == TextStyle.BOLD) {
      return TextStyle.BOLD;
    }
    return TextStyle.NORMAL;
  }

  private interface StyleResolver {
    TextStyle styleAt(int i, int j);
  }

  private static final class StyledImageWrapper implements StyledImage {
    private final Image base;
    private final StyleResolver styleResolver;

    private StyledImageWrapper(Image base, StyleResolver styleResolver) {
      this.base = base;
      this.styleResolver = styleResolver;
    }

    @Override
    public char at(int i, int j) {
      return base.at(i, j);
    }

    @Override
    public TextStyle styleAt(int i, int j) {
      return styleResolver.styleAt(i, j);
    }

    @Override
    public Image overlay(Image m) {
      return StyledImages.overlay(this, m);
    }

    @Override
    public Image underlay(Image m) {
      return StyledImages.overlay(m, this);
    }

    @Override
    public Image combine(Image m, Style style) {
      return StyledImages.combine(this, m, style);
    }

    @Override
    public Image offset(int di, int dj) {
      return StyledImages.offset(this, di, dj);
    }

    @Override
    public Image limit(int height, int width) {
      return StyledImages.limit(this, height, width);
    }

    @Override
    public Image crop(Rect rect) {
      return offset(rect.top, rect.left).limit(rect.height, rect.width);
    }
  }
}
