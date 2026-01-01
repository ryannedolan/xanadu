package codes.ry.xanadu;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Scroll implements Image {
  private int offset = 0;
  public final int cols;
  private final List<String> lines = new ArrayList<>();

  public Scroll(int cols) {
    this.cols = cols;
  }

  @Override
  public char at(int i, int j) {
    var s = lines.get(offset + i);
    if (j >= s.length()) {
      return ' ';
    } else {
      return s.charAt(j);
    }
  }

  public void append(String line) {
    lines.add(line);
  }

  public void scroll(int d) {
    offset += d;
    if (offset < 0) {
      offset = 0;
    }
    if (offset > lines.size() - 1) {
      offset = lines.size() - 1;
    }
  }

  public void dump(PrintWriter w) {
    for (int i = offset; i < lines.size(); i++) {
      w.append(lines.get(i));
      w.append('\n');
    }
  }
}
