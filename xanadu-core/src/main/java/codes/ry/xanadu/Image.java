package codes.ry.xanadu;

@FunctionalInterface
public interface Image {

  char at(int i, int j);

  default Image overlay(Image m) {
    return (i, j) -> {
      char c = m.at(i, j);
      return c != ' ' ? c : at(i, j);
    };
  }

  default Image underlay(Image m) {
    return (i, j) -> {
      char c = at(i, j);
      return c != ' ' ? c : m.at(i, j);
    };
  }

  default Image combine(Image m, Style style) {
    return (i, j) -> style.combine(at(i, j), m.at(i, j));
  }

  default Image offset(int di, int dj) {
    return (i, j) -> at(i - di, j - dj);
  }

  default Image limit(int height, int width) {
    return (i, j) -> {
      if (i >= height || j >= width) {
        return ' ';
      } else {
        return at(i, j);
      }
    };
  }

  default Image crop(Rect rect) {
    return offset(rect.top, rect.left).limit(rect.height, rect.width);
  }

  static Image flood(char c) {
    return (i, j) -> c;
  }

  static Image text(String s) {
    return (i, j) -> i == 0 && j >= 0 && j < s.length() ? s.charAt(j) : ' ';
  }

  static Image text(int cols, String s) {
    var lines = Utils.wrapLines(s, cols);
    return (i, j) -> i >= 0 && i < lines.size() ? text(lines.get(i)).at(0, j) : ' ';
  }

  static Image hbar(float length) {
    int[] parts = barParts(length);
    int full = parts[0];
    int eighths = parts[1];
    return (i, j) -> {
      if (i != 0 || j < 0) {
        return ' ';
      }
      if (j < full) {
        return '█';
      }
      if (j == full && eighths > 0) {
        return horizontalBlock(eighths);
      }
      return ' ';
    };
  }

  static Image vbar(float length) {
    int[] parts = barParts(length);
    int full = parts[0];
    int eighths = parts[1];
    int height = full + (eighths > 0 ? 1 : 0);
    return (i, j) -> {
      if (j != 0 || i < 0) {
        return ' ';
      }
      int rowFromBottom = height - 1 - i;
      if (rowFromBottom < 0) {
        return ' ';
      }
      if (rowFromBottom < full) {
        return '█';
      }
      if (rowFromBottom == full && eighths > 0) {
        return verticalBlock(eighths);
      }
      return ' ';
    };
  }

  private static int[] barParts(float length) {
    if (Float.isNaN(length) || length <= 0) {
      return new int[] {0, 0};
    }
    int full = (int) Math.floor(length);
    float frac = length - full;
    int eighths = Math.round(frac * 8f);
    if (eighths == 8) {
      full += 1;
      eighths = 0;
    }
    return new int[] {full, eighths};
  }

  private static char horizontalBlock(int eighths) {
    switch (eighths) {
      case 1:
        return '▏';
      case 2:
        return '▎';
      case 3:
        return '▍';
      case 4:
        return '▌';
      case 5:
        return '▋';
      case 6:
        return '▊';
      case 7:
        return '▉';
      default:
        return '█';
    }
  }

  private static char verticalBlock(int eighths) {
    switch (eighths) {
      case 1:
        return '▁';
      case 2:
        return '▂';
      case 3:
        return '▃';
      case 4:
        return '▄';
      case 5:
        return '▅';
      case 6:
        return '▆';
      case 7:
        return '▇';
      default:
        return '█';
    }
  }
}
