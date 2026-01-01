package codes.ry.xanadu.command;

import java.util.List;

public final class CommandInput {
  public final String raw;
  public final String name;
  public final List<String> args;

  public CommandInput(String raw, String name, List<String> args) {
    this.raw = raw;
    this.name = name;
    this.args = List.copyOf(args);
  }

  public String tail() {
    if (raw == null) {
      return "";
    }
    int index = raw.indexOf(name);
    if (index < 0) {
      return "";
    }
    int start = index + name.length();
    if (start >= raw.length()) {
      return "";
    }
    return raw.substring(start).trim();
  }
}
