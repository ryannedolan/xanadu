package codes.ry.xanadu;

import java.util.HashMap;
import java.util.Map;

public final class Style {
  public static final int NORTH = 0b1000;
  public static final int EAST = 0b0100;
  public static final int SOUTH = 0b0010;
  public static final int WEST = 0b0001;

  private final Map<Character, Integer> masks;
  private final char[] glyphs;

  private Style(Map<Character, Integer> masks, char[] glyphs) {
    this.masks = masks;
    this.glyphs = glyphs;
  }

  public char combine(char a, char b) {
    if (a == ' ') {
      return b;
    }
    if (b == ' ') {
      return a;
    }
    Integer maskA = masks.get(a);
    Integer maskB = masks.get(b);
    if (maskA == null || maskB == null) {
      return b;
    }
    int mask = maskA | maskB;
    char combined = glyphs[mask];
    return combined == 0 ? b : combined;
  }

  public char glyph(int mask) {
    char glyph = glyphs[mask & 0b1111];
    return glyph == 0 ? ' ' : glyph;
  }

  public static Style box() {
    Map<Character, Integer> masks = new HashMap<>();
    char[] glyphs = new char[16];
    glyphs[0] = ' ';
    add(masks, glyphs, '─', EAST | WEST);
    add(masks, glyphs, '│', NORTH | SOUTH);
    add(masks, glyphs, '┌', EAST | SOUTH);
    add(masks, glyphs, '┐', SOUTH | WEST);
    add(masks, glyphs, '└', NORTH | EAST);
    add(masks, glyphs, '┘', NORTH | WEST);
    add(masks, glyphs, '├', NORTH | EAST | SOUTH);
    add(masks, glyphs, '┤', NORTH | SOUTH | WEST);
    add(masks, glyphs, '┬', EAST | SOUTH | WEST);
    add(masks, glyphs, '┴', NORTH | EAST | WEST);
    add(masks, glyphs, '┼', NORTH | EAST | SOUTH | WEST);
    return new Style(masks, glyphs);
  }

  public static Style doubleBox() {
    Map<Character, Integer> masks = new HashMap<>();
    char[] glyphs = new char[16];
    glyphs[0] = ' ';
    add(masks, glyphs, '═', EAST | WEST);
    add(masks, glyphs, '║', NORTH | SOUTH);
    add(masks, glyphs, '╔', EAST | SOUTH);
    add(masks, glyphs, '╗', SOUTH | WEST);
    add(masks, glyphs, '╚', NORTH | EAST);
    add(masks, glyphs, '╝', NORTH | WEST);
    add(masks, glyphs, '╠', NORTH | EAST | SOUTH);
    add(masks, glyphs, '╣', NORTH | SOUTH | WEST);
    add(masks, glyphs, '╦', EAST | SOUTH | WEST);
    add(masks, glyphs, '╩', NORTH | EAST | WEST);
    add(masks, glyphs, '╬', NORTH | EAST | SOUTH | WEST);
    return new Style(masks, glyphs);
  }

  public static Style blocks() {
    Map<Character, Integer> masks = new HashMap<>();
    char[] glyphs = new char[16];
    glyphs[0] = ' ';
    for (int i = 1; i < glyphs.length; i++) {
      glyphs[i] = '█';
    }
    glyphs[NORTH | EAST] = '▙';
    glyphs[NORTH | WEST] = '▟';
    glyphs[SOUTH | EAST] = '▛';
    glyphs[SOUTH | WEST] = '▜';
    masks.put('█', NORTH | EAST | SOUTH | WEST);
    masks.put('▙', NORTH | EAST);
    masks.put('▟', NORTH | WEST);
    masks.put('▛', SOUTH | EAST);
    masks.put('▜', SOUTH | WEST);
    return new Style(masks, glyphs);
  }

  public static Style subcellBlocks() {
    Map<Character, Integer> masks = new HashMap<>();
    char[] glyphs = new char[16];
    glyphs[0] = ' ';
    add(masks, glyphs, '▘', NORTH);
    add(masks, glyphs, '▝', EAST);
    add(masks, glyphs, '▖', SOUTH);
    add(masks, glyphs, '▗', WEST);
    add(masks, glyphs, '▀', NORTH | EAST);
    add(masks, glyphs, '▌', NORTH | SOUTH);
    add(masks, glyphs, '▐', EAST | WEST);
    add(masks, glyphs, '▄', SOUTH | WEST);
    add(masks, glyphs, '▚', NORTH | WEST);
    add(masks, glyphs, '▞', EAST | SOUTH);
    add(masks, glyphs, '▛', NORTH | EAST | SOUTH);
    add(masks, glyphs, '▜', NORTH | EAST | WEST);
    add(masks, glyphs, '▙', NORTH | SOUTH | WEST);
    add(masks, glyphs, '▟', EAST | SOUTH | WEST);
    add(masks, glyphs, '█', NORTH | EAST | SOUTH | WEST);
    return new Style(masks, glyphs);
  }

  public static Style tree() {
    Map<Character, Integer> masks = new HashMap<>();
    char[] glyphs = new char[16];
    glyphs[0] = ' ';
    add(masks, glyphs, '│', NORTH | SOUTH);
    add(masks, glyphs, '─', EAST | WEST);
    add(masks, glyphs, '├', NORTH | SOUTH | EAST);
    add(masks, glyphs, '└', NORTH | EAST);
    glyphs[SOUTH | EAST] = '├';
    glyphs[EAST] = '─';
    return new Style(masks, glyphs);
  }

  public static Style fill() {
    Map<Character, Integer> masks = new HashMap<>();
    char[] glyphs = new char[16];
    glyphs[0] = ' ';
    for (int i = 1; i < glyphs.length; i++) {
      glyphs[i] = '░';
    }
    masks.put('░', NORTH | EAST | SOUTH | WEST);
    return new Style(masks, glyphs);
  }

  public Frame frame(int height, int width) {
    return new Frame(height, width, Image.flood(' '), this);
  }

  public Frame frame(int height, int width, Image image) {
    return new Frame(height, width, image, this);
  }

  public Frame text(int cols, String txt) {
    var lines = Utils.wrapLines(txt, cols);
    int height = lines.size();
    int width = Math.max(0, cols);
    Image image = (i, j) -> {
      if (i < 0 || i >= lines.size()) {
        return ' ';
      }
      String line = lines.get(i);
      if (j < 0 || j >= line.length()) {
        return ' ';
      }
      return line.charAt(j);
    };
    return frame(height, width, image);
  }

  public Frame text(String txt) {
    var lines = Utils.wrapLines(txt, Integer.MAX_VALUE);
    int height = lines.size();
    int width = 0;
    for (String line : lines) {
      width = Math.max(width, line.length());
    }
    Image image = (i, j) -> {
      if (i < 0 || i >= lines.size()) {
        return ' ';
      }
      String line = lines.get(i);
      if (j < 0 || j >= line.length()) {
        return ' ';
      }
      return line.charAt(j);
    };
    return frame(height, width, image);
  }

  public Frame hbar(int width, float length) {
    return frame(1, width, Image.hbar(length));
  }

  public Frame vbar(int height, float length) {
    int offset = height - Utils.barCells(length);
    return frame(height, 1, Image.vbar(length).offset(offset, 0));
  }

  private static void add(Map<Character, Integer> masks, char[] glyphs, char glyph, int mask) {
    masks.put(glyph, mask);
    glyphs[mask] = glyph;
  }
}
