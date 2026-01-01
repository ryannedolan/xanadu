package codes.ry.xanadu.command;

import java.util.ArrayList;
import java.util.List;

public final class CommandParser {
  private CommandParser() {}

  public static CommandInput parse(String line) {
    if (line == null) {
      return null;
    }
    String trimmed = line.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    List<String> tokens = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    char quote = 0;
    boolean escaping = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (escaping) {
        current.append(c);
        escaping = false;
        continue;
      }
      if (c == '\\') {
        escaping = true;
        continue;
      }
      if (quote != 0) {
        if (c == quote) {
          quote = 0;
        } else {
          current.append(c);
        }
        continue;
      }
      if (c == '"' || c == '\'') {
        quote = c;
        continue;
      }
      if (Character.isWhitespace(c)) {
        if (current.length() > 0) {
          tokens.add(current.toString());
          current.setLength(0);
        }
        continue;
      }
      current.append(c);
    }
    if (current.length() > 0) {
      tokens.add(current.toString());
    }
    if (tokens.isEmpty()) {
      return null;
    }
    String name = tokens.get(0);
    List<String> args = tokens.subList(1, tokens.size());
    return new CommandInput(line, name, args);
  }
}
