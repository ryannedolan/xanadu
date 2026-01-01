package codes.ry.xanadu.command;

public enum LogLevel {
  ERROR(0),
  WARN(1),
  INFO(2),
  DEBUG(3);

  private final int priority;

  LogLevel(int priority) {
    this.priority = priority;
  }

  public boolean allows(LogLevel messageLevel) {
    return messageLevel.priority <= priority;
  }

  public static LogLevel parse(String raw) {
    if (raw == null) {
      return null;
    }
    switch (raw.trim().toLowerCase()) {
      case "error":
        return ERROR;
      case "warn":
      case "warning":
        return WARN;
      case "info":
        return INFO;
      case "debug":
        return DEBUG;
      default:
        return null;
    }
  }
}
