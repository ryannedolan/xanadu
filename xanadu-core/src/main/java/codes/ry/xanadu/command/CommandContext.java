package codes.ry.xanadu.command;

import codes.ry.xanadu.Image;
import codes.ry.xanadu.Style;
import codes.ry.xanadu.render.RenderContext;
import codes.ry.xanadu.render.RenderService;
import java.io.PrintWriter;

public final class CommandContext {
  public final PrintWriter out;
  public final Style style;
  public final RenderService renderService;
  public int maxWidth;
  public int maxHeight;
  private final java.util.Map<String, Object> state;
  private CommandService commandService;
  private Continuation continuation;
  private LogLevel logLevel;
  private static final String LAST_EXCEPTION_KEY = "__xanadu.last_exception";
  private static final String ANSI_RESET = "\u001b[0m";
  private static final String ANSI_RED = "\u001b[31m";
  private static final String ANSI_YELLOW = "\u001b[33m";
  private volatile boolean cancelled;
  private boolean failed;
  private boolean allowContinuation;
  private boolean clipFrames;
  private PrintWriter renderTap;
  private int renderTapWidth;
  private int renderTapHeight;
  private boolean renderTapClipFrames;

  public CommandContext(
      PrintWriter out,
      Style style,
      RenderService renderService,
      CommandService commandService,
      int maxWidth,
      int maxHeight) {
    this(out, style, renderService, commandService, maxWidth, maxHeight, new java.util.HashMap<>(), null);
  }

  private CommandContext(
      PrintWriter out,
      Style style,
      RenderService renderService,
      CommandService commandService,
      int maxWidth,
      int maxHeight,
      java.util.Map<String, Object> state,
      Continuation continuation) {
    this.out = out;
    this.style = style;
    this.renderService = renderService;
    this.commandService = commandService;
    this.maxWidth = maxWidth;
    this.maxHeight = maxHeight;
    this.state = state;
    this.continuation = continuation;
    this.logLevel = LogLevel.INFO;
    this.clipFrames = true;
    this.failed = false;
    this.allowContinuation = true;
  }

  public RenderContext renderContext() {
    return new RenderContext(style, maxWidth, maxHeight, renderService, clipFrames);
  }

  public void render(Image image) {
    if (out instanceof CapturePrintWriter) {
      ((CapturePrintWriter) out).setCaptureEnabled(false);
    }
    renderContext().dump(image, out);
    if (out instanceof CapturePrintWriter) {
      ((CapturePrintWriter) out).setCaptureEnabled(true);
    }
    if (renderTap != null) {
      new RenderContext(style, renderTapWidth, renderTapHeight, renderService, renderTapClipFrames)
          .dump(image, renderTap);
    }
    out.flush();
  }

  public void render(Object value) {
    Image image = renderService.render(value, renderContext());
    render(image);
  }

  public CommandService commandService() {
    return commandService;
  }

  public void setCommandService(CommandService commandService) {
    this.commandService = commandService;
  }

  public void setSize(int maxWidth, int maxHeight) {
    this.maxWidth = maxWidth;
    this.maxHeight = maxHeight;
  }

  public void setClipFrames(boolean clipFrames) {
    this.clipFrames = clipFrames;
  }

  public boolean clipFrames() {
    return clipFrames;
  }

  public void setRenderTap(PrintWriter writer, int maxWidth, int maxHeight, boolean clipFrames) {
    this.renderTap = writer;
    this.renderTapWidth = maxWidth;
    this.renderTapHeight = maxHeight;
    this.renderTapClipFrames = clipFrames;
  }

  public boolean allowContinuation() {
    return allowContinuation;
  }

  public void setAllowContinuation(boolean allowContinuation) {
    this.allowContinuation = allowContinuation;
  }

  public CommandContext fork(PrintWriter out) {
    CommandContext context =
        new CommandContext(out, style, renderService, commandService, maxWidth, maxHeight, state, null);
    context.logLevel = logLevel;
    context.clipFrames = clipFrames;
    return context;
  }

  public LogLevel logLevel() {
    return logLevel;
  }

  public void setLogLevel(LogLevel level) {
    if (level != null) {
      this.logLevel = level;
    }
  }

  public void error(String message) {
    failed = true;
    log(LogLevel.ERROR, message);
  }

  public void warn(String message) {
    log(LogLevel.WARN, message);
  }

  public void info(String message) {
    log(LogLevel.INFO, message);
  }

  public void debug(String message) {
    log(LogLevel.DEBUG, message);
  }

  public void log(LogLevel level, String message) {
    if (level == null || message == null) {
      return;
    }
    if (!logLevel.allows(level)) {
      return;
    }
    String line = "  - " + message;
    switch (level) {
      case ERROR:
        out.println(ANSI_RED + line + ANSI_RESET);
        break;
      case WARN:
        out.println(ANSI_YELLOW + line + ANSI_RESET);
        break;
      default:
        out.println(line);
        break;
    }
    out.flush();
  }

  public void logContinuation(LogLevel level, String message) {
    if (level == null || message == null) {
      return;
    }
    if (!logLevel.allows(level)) {
      return;
    }
    String line = "    " + message;
    switch (level) {
      case ERROR:
        out.println(ANSI_RED + line + ANSI_RESET);
        break;
      case WARN:
        out.println(ANSI_YELLOW + line + ANSI_RESET);
        break;
      default:
        out.println(line);
        break;
    }
    out.flush();
  }

  public void recordException(Throwable error) {
    if (error == null) {
      state.remove(LAST_EXCEPTION_KEY);
    } else {
      state.put(LAST_EXCEPTION_KEY, error);
      failed = true;
    }
  }

  public void fail() {
    failed = true;
  }

  public boolean failed() {
    return failed;
  }

  public void resetFailure() {
    failed = false;
  }

  public Throwable lastException() {
    Object value = state.get(LAST_EXCEPTION_KEY);
    return value instanceof Throwable ? (Throwable) value : null;
  }

  public String formatStackTrace(Throwable error) {
    if (error == null) {
      return "";
    }
    java.io.StringWriter output = new java.io.StringWriter();
    java.io.PrintWriter writer = new java.io.PrintWriter(output);
    error.printStackTrace(writer);
    writer.flush();
    return output.toString();
  }

  public void continueWith(Continuation continuation) {
    this.continuation = continuation;
  }

  public Continuation continuation() {
    return continuation;
  }

  public void clearContinuation() {
    continuation = null;
  }

  public void cancelCurrentCommand() {
    cancelled = true;
  }

  public boolean consumeCancel() {
    if (cancelled) {
      cancelled = false;
      return true;
    }
    return false;
  }

  public void put(String key, Object value) {
    if (value == null) {
      state.remove(key);
    } else {
      state.put(key, value);
    }
  }

  public Object get(String key) {
    return state.get(key);
  }

  public <T> T get(String key, Class<T> type) {
    Object value = state.get(key);
    if (type.isInstance(value)) {
      return type.cast(value);
    }
    return null;
  }

  public void remove(String key) {
    state.remove(key);
  }
}
