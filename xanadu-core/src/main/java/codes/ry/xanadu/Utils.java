package codes.ry.xanadu;

import java.util.ArrayList;
import java.util.List;

class Utils {

  static List<String> wrapLine(String text, int maxWidth) {
    List<String> lines = new ArrayList<>();
    StringBuilder currentLine = new StringBuilder();

    for (String word : text.split("\\s+")) {
      if (currentLine.length() == 0) {
        currentLine.append(word);
      } else if (currentLine.length() + 1 + word.length() <= maxWidth) {
        currentLine.append(' ').append(word);
      } else {
        lines.add(currentLine.toString());
        currentLine.setLength(0);
        currentLine.append(word);
      }
    }

    if (currentLine.length() > 0) {
      lines.add(currentLine.toString());
    }

    return lines;
  }

  static List<String> wrapLines(String text, int maxWidth) {
    List<String> result = new ArrayList<>();
    String[] rawLines = text.split("\\R", -1);

    for (String rawLine : rawLines) {
      if (rawLine.isEmpty()) {
        // Preserve blank lines exactly
        result.add("");
      } else {
        result.addAll(wrapLine(rawLine, maxWidth));
      }
    }
    return result;
  }

  static int barCells(float length) {
    if (Float.isNaN(length) || length <= 0) {
      return 0;
    }
    int full = (int) Math.floor(length);
    float frac = length - full;
    int eighths = Math.round(frac * 8f);
    if (eighths == 8) {
      full += 1;
      eighths = 0;
    }
    return full + (eighths > 0 ? 1 : 0);
  }
}
