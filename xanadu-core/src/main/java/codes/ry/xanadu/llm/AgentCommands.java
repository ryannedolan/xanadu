package codes.ry.xanadu.llm;

import codes.ry.xanadu.StyledImage;
import codes.ry.xanadu.StyledImages;
import codes.ry.xanadu.TextStyle;
import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandInput;
import codes.ry.xanadu.command.CommandProvider;
import codes.ry.xanadu.command.CommandResult;
import codes.ry.xanadu.command.Continuation;
import codes.ry.xanadu.command.ContinuationResult;
import codes.ry.xanadu.command.LogLevel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AgentCommands implements CommandProvider {
  private static final String COMMAND = "agent";
  private static final String CHAT_COMMAND = "chat";
  private static final String MODEL_KEY = "agent.model";
  private static final String BACKEND_KEY = "agent.backend";
  private static final String HISTORY_PREFIX = "agent.history.";
  private static final String DEFAULT_BACKEND = "chatgpt";
  private static final String DEFAULT_MODEL = "gpt-5.2-chat-latest";
  private static final String ANSI_CYAN = "\u001b[36m";
  private static final String ANSI_RESET = "\u001b[0m";
  private static final Pattern TOOL_PATTERN =
      Pattern.compile("(?m)^\\s*" + CommandTooling.TOOL_NAME + "\\s*:\\s*(.+)$");

  private final List<AgentBackend> backends;
  private final Map<String, AgentBackend> backendById;

  public AgentCommands() {
    this(loadBackends());
  }

  public AgentCommands(List<AgentBackend> backends) {
    this.backends = List.copyOf(backends);
    java.util.Map<String, AgentBackend> map = new java.util.HashMap<>();
    for (AgentBackend backend : backends) {
      map.put(backend.id().toLowerCase(Locale.ROOT), backend);
    }
    this.backendById = java.util.Map.copyOf(map);
  }

  @Override
  public boolean supports(CommandInput input) {
    return COMMAND.equalsIgnoreCase(input.name) || CHAT_COMMAND.equalsIgnoreCase(input.name);
  }

  @Override
  public codes.ry.xanadu.command.Command commandFor(CommandInput input) {
    return context -> {
      execute(context, input);
      return CommandResult.SUCCESS;
    };
  }

  @Override
  public java.util.Set<String> commandNames() {
    return java.util.Set.of(COMMAND, CHAT_COMMAND);
  }

  @Override
  public java.util.List<String> subcommands(String commandName) {
    if (COMMAND.equals(commandName)) {
      return java.util.List.of("chat", "delegate", "model", "models", "showprompt", "reset");
    }
    return java.util.List.of();
  }

  @Override
  public java.util.List<String> usage(String commandName) {
    if (COMMAND.equals(commandName)) {
      return java.util.List.of(
          "agent chat <prompt>",
          "agent delegate <prompt>",
          "agent model",
          "agent model <name|backend:name>",
          "agent models",
          "agent showprompt",
          "agent reset");
    }
    if (CHAT_COMMAND.equals(commandName)) {
      return java.util.List.of("chat <prompt>");
    }
    return java.util.List.of(commandName);
  }

  private void execute(CommandContext context, CommandInput input) {
    if (CHAT_COMMAND.equalsIgnoreCase(input.name)) {
      CommandInput rewritten =
          new CommandInput(
              COMMAND + " chat " + String.join(" ", input.args),
              COMMAND,
              prepend("chat", input.args));
      chat(context, rewritten, CHAT_COMMAND, true, true);
      return;
    }
    if (input.args.isEmpty()) {
      context.out.println("Usage:");
      for (String line : usage(COMMAND)) {
        context.out.println("  " + line);
      }
      context.out.flush();
      return;
    }
    String sub = input.args.get(0).toLowerCase(Locale.ROOT);
    switch (sub) {
      case "chat":
        chat(context, input, COMMAND, true, true);
        return;
      case "delegate":
        delegate(context, input);
        return;
      case "model":
        context.clearContinuation();
        setModel(context, input);
        return;
      case "models":
        listModels(context);
        return;
      case "showprompt":
        showPrompt(context);
        return;
      case "reset":
        context.clearContinuation();
        reset(context);
        return;
      default:
        context.out.println("Unknown subcommand: " + sub);
        context.out.flush();
    }
  }

  private void reset(CommandContext context) {
    AgentBackend backend = currentBackend(context);
    if (backend != null) {
      context.remove(historyKey(backend.id()));
      context.out.println("Agent history cleared for " + backend.displayName() + ".");
    } else {
      context.out.println("Agent history cleared.");
    }
    context.out.flush();
  }

  private void setModel(CommandContext context, CommandInput input) {
    if (input.args.size() == 1) {
      context.out.println(currentModelLabel(context));
      context.out.flush();
      return;
    }
    String raw = input.args.get(1);
    String backendId = extractBackend(raw);
    String model = extractModel(raw);
    if (backendId == null) {
      String matchedBackend = findBackendForModel(model);
      if (matchedBackend == null) {
        context.warn("No backend found for model: " + model);
      }
      backendId = matchedBackend != null ? matchedBackend : currentBackendId(context);
    }
    if (backendId == null) {
      backendId = DEFAULT_BACKEND;
    }
    context.put(BACKEND_KEY, backendId);
    context.put(MODEL_KEY, model);
    context.out.println("Model set to " + backendId + ":" + model);
    context.out.flush();
  }

  private void chat(
      CommandContext context,
      CommandInput input,
      String promptName,
      boolean useHistory,
      boolean allowContinuation) {
    if (input.args.size() < 2) {
      if (allowContinuation) {
        context.continueWith(
            new Continuation(
                promptName,
                (line, ctx) -> ContinuationResult.executeAndContinue("chat " + line)));
        context.debug("Entering chat mode.");
        return;
      }
      context.out.println(
          "Usage: " + promptName + (COMMAND.equals(promptName) ? " chat" : "") + " <prompt>");
      context.out.flush();
      return;
    }
    AgentBackend backend = currentBackend(context);
    if (backend == null) {
      context.error("No agent backend is available.");
      return;
    }
    if (!backend.isConfigured()) {
      context.error(backend.missingConfigMessage());
      return;
    }
    if (allowContinuation) {
      context.continueWith(
          new Continuation(
              promptName,
              (line, ctx) -> ContinuationResult.executeAndContinue("chat " + line)));
    }
    context.debug("Starting agent chat with " + backend.displayName() + ".");
    String model = currentModel(context);
    String prompt = String.join(" ", input.args.subList(1, input.args.size()));
    List<AgentMessage> messages =
        useHistory ? history(context, backend) : freshHistory(context, backend, !allowContinuation);
    messages.add(new AgentMessage("user", prompt));
    List<String> lastToolCalls = List.of();
    boolean lastToolSucceeded = true;
    String pendingNormalized = null;
    Continuation pendingContinuation = null;
    String pendingContinuationName = null;
    StringBuilder pendingToolOutput = null;
    while (true) {
      if (context.consumeCancel()) {
        context.warn("Agent chat cancelled.");
        return;
      }
      context.debug("Sending request to " + backend.displayName() + ".");
      AgentResponse response = backend.chat(messages, model);
      if (response == null || response.text() == null || response.text().isBlank()) {
        context.error("No response from " + backend.displayName() + ".");
        return;
      }
      messages.add(new AgentMessage("assistant", response.text()));
      String normalized = backend.normalizeResponse(response.text());
      String combined =
          pendingNormalized == null ? normalized : pendingNormalized + "\n" + normalized;
      ResponseParse parsed = parseResponse(combined);
      if (parsed.incomplete) {
        pendingNormalized = combined;
        messages.add(new AgentMessage("user", "continue"));
        continue;
      }
      pendingNormalized = null;
      List<String> toolCalls = parsed.toolCalls;
      if (toolCalls.isEmpty()) {
        context.debug("Received response from " + backend.displayName() + ".");
        renderIndented(context, combined);
        if (response.finishReason() == AgentFinishReason.LENGTH) {
          messages.add(new AgentMessage("user", "continue"));
          continue;
        }
        return;
      }
      if (toolCalls.equals(lastToolCalls) && lastToolSucceeded) {
        context.warn(backend.displayName() + " repeated the same tool call; stopping.");
        return;
      }
      lastToolCalls = toolCalls;
      context.debug(backend.displayName() + " requested " + toolCalls.size() + " tool call(s).");
      ToolRunResult runResult =
          runToolSequence(
              context,
              parsed.segments,
              pendingContinuation,
              pendingContinuationName,
              pendingToolOutput);
      if (runResult.toolOutput.length() > 0) {
        messages.add(new AgentMessage("user", runResult.toolOutput.toString()));
      }
      pendingContinuation = runResult.continuation;
      pendingContinuationName = runResult.continuationName;
      pendingToolOutput = runResult.continuation == null ? null : runResult.toolOutput;
      if (!runResult.success) {
        lastToolSucceeded = false;
        return;
      }
      if (runResult.needsMoreInput) {
        lastToolSucceeded = false;
        messages.add(new AgentMessage("user", "continue"));
        continue;
      }
      lastToolSucceeded = true;
    }
  }

  private void delegate(CommandContext context, CommandInput input) {
    if (input.args.size() < 2) {
      context.out.println("Usage: agent delegate <prompt>");
      context.out.flush();
      return;
    }
    chat(context, input, "agent delegate", false, false);
  }

  private void listModels(CommandContext context) {
    boolean printed = false;
    for (AgentBackend backend : backends) {
      if (!backend.isConfigured()) {
        context.warn(backend.missingConfigMessage());
        continue;
      }
      context.debug("Fetching model list from " + backend.displayName() + ".");
      List<String> models = backend.listModels();
      if (models.isEmpty()) {
        context.out.println("No models returned from " + backend.displayName() + ".");
        printed = true;
        continue;
      }
      for (String name : models) {
        context.out.println(backend.id() + ":" + name);
        printed = true;
      }
    }
    if (printed) {
      context.out.flush();
    }
  }

  private void showPrompt(CommandContext context) {
    context.out.println(toolInstructions(context, false));
    context.out.flush();
  }

  @SuppressWarnings("unchecked")
  private List<AgentMessage> history(CommandContext context, AgentBackend backend) {
    String key = historyKey(backend.id());
    Object existing = context.get(key);
    if (existing instanceof List) {
      return (List<AgentMessage>) existing;
    }
    List<AgentMessage> messages = new ArrayList<>();
    messages.add(new AgentMessage("system", toolInstructions(context, false)));
    context.put(key, messages);
    return messages;
  }

  private List<AgentMessage> freshHistory(
      CommandContext context, AgentBackend backend, boolean delegated) {
    List<AgentMessage> messages = new ArrayList<>();
    messages.add(new AgentMessage("system", toolInstructions(context, delegated)));
    return messages;
  }

  private String toolInstructions(CommandContext context, boolean delegated) {
    StringBuilder sb = new StringBuilder();
    sb.append(CommandTooling.toolDescription(context.commandService()));
    sb.append(" You may interleave text with tool calls, but avoid it unless you need to explain something.");
    sb.append(" If you do explain, keep it brief and place it immediately before the relevant tool call.");
    sb.append(" To run commands, reply with a fenced block: ```");
    sb.append(CommandTooling.TOOL_NAME);
    sb.append("\\n<command line>\\n```.");
    sb.append(" For SQL commands, the command name is the verb; do not repeat it.");
    sb.append(" Example: `select * from users` (not `select SELECT * from users`).");
    sb.append(" End SQL statements with a semicolon.");
    sb.append(" To delegate a task, use: ```" + CommandTooling.TOOL_NAME + "\\nagent delegate <prompt>\\n```.");
    sb.append(" Delegated agents start fresh and cannot use continuation.");
    sb.append(" Use one command per line inside the fenced block.");
    sb.append(" If a command needs continuation (e.g., multi-line SQL), put the next line(s) immediately after it.");
    sb.append(" You can define macros with `def <name> ... end`, list them with `macros`, and delete them with `undef <name>`.");
    if (delegated) {
      sb.append(" This is a delegated task with no follow-up; avoid leading questions at the end.");
    }
    String usage = CommandTooling.detailedUsage(context.commandService());
    if (!usage.isBlank()) {
      sb.append("\n\nCommand usage:\n");
      sb.append(usage);
    }
    return sb.toString();
  }

  private ResponseParse parseResponse(String response) {
    List<ResponseSegment> segments = new ArrayList<>();
    List<String> toolCalls = new ArrayList<>();
    String[] lines = response.split("\\R", -1);
    StringBuilder textBuffer = new StringBuilder();
    boolean inFence = false;
    boolean xanaduFence = false;
    List<String> fenceLines = new ArrayList<>();
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      if (line.startsWith("```")) {
        if (inFence) {
          if (xanaduFence) {
            addXanaduFenceSegments(segments, toolCalls, fenceLines);
          } else {
            appendFenceText(textBuffer, fenceLines);
          }
          fenceLines.clear();
          inFence = false;
          xanaduFence = false;
        } else {
          flushTextBuffer(segments, textBuffer);
          inFence = true;
          xanaduFence = line.trim().equalsIgnoreCase("```" + CommandTooling.TOOL_NAME);
        }
        continue;
      }
      if (inFence) {
        fenceLines.add(line);
        continue;
      }
      Matcher matcher = TOOL_PATTERN.matcher(line);
      if (matcher.find()) {
        flushTextBuffer(segments, textBuffer);
        String call = matcher.group(1).trim();
        segments.add(ResponseSegment.toolLines(List.of(call)));
        if (!call.isBlank()) {
          toolCalls.add(call);
        }
        continue;
      }
      textBuffer.append(line);
      if (i + 1 < lines.length) {
        textBuffer.append('\n');
      }
    }
    if (inFence) {
      return new ResponseParse(segments, toolCalls, true);
    }
    if (!fenceLines.isEmpty()) {
      appendFenceText(textBuffer, fenceLines);
    }
    flushTextBuffer(segments, textBuffer);
    return new ResponseParse(segments, toolCalls, false);
  }

  private void renderIndented(CommandContext context, String response) {
    int maxWidth = context.maxWidth > 0 ? Math.max(1, context.maxWidth - 2) : Integer.MAX_VALUE;
    FormattedText formatted = new FormattedText(response, maxWidth);
    var padded = StyledImages.offset(formatted, 0, 2);
    var paddedFrame = context.style.frame(formatted.height, formatted.width + 2, padded);
    context.render(paddedFrame);
    context.out.println();
    context.out.flush();
  }

  private AgentBackend currentBackend(CommandContext context) {
    String backendId = currentBackendId(context);
    if (backendId == null) {
      backendId = DEFAULT_BACKEND;
    }
    return backendById.get(backendId.toLowerCase(Locale.ROOT));
  }

  private String currentBackendId(CommandContext context) {
    String backendId = context.get(BACKEND_KEY, String.class);
    if (backendId != null && !backendId.isBlank()) {
      return backendId;
    }
    return DEFAULT_BACKEND;
  }

  private String currentModel(CommandContext context) {
    String model = context.get(MODEL_KEY, String.class);
    if (model != null && !model.isBlank()) {
      return model;
    }
    return DEFAULT_MODEL;
  }

  private String currentModelLabel(CommandContext context) {
    return currentBackendId(context) + ":" + currentModel(context);
  }

  private String historyKey(String backendId) {
    return HISTORY_PREFIX + backendId.toLowerCase(Locale.ROOT);
  }

  private String extractBackend(String raw) {
    int idx = raw.indexOf(':');
    if (idx <= 0) {
      return null;
    }
    return raw.substring(0, idx);
  }

  private String extractModel(String raw) {
    int idx = raw.indexOf(':');
    if (idx < 0 || idx + 1 >= raw.length()) {
      return raw;
    }
    return raw.substring(idx + 1);
  }

  private String findBackendForModel(String model) {
    if (model == null || model.isBlank()) {
      return null;
    }
    for (AgentBackend backend : backends) {
      if (!backend.isConfigured()) {
        continue;
      }
      List<String> models = backend.listModels();
      for (String candidate : models) {
        if (candidate.equalsIgnoreCase(model)) {
          return backend.id();
        }
      }
    }
    return null;
  }

  private static List<AgentBackend> loadBackends() {
    List<AgentBackend> loaded = new ArrayList<>();
    java.util.ServiceLoader.load(AgentBackend.class).forEach(loaded::add);
    return loaded;
  }

  private static List<String> prepend(String head, List<String> args) {
    List<String> combined = new ArrayList<>();
    combined.add(head);
    combined.addAll(args);
    return combined;
  }

  private ToolRunResult runToolSequence(
      CommandContext context,
      List<ResponseSegment> segments,
      Continuation continuation,
      String continuationName,
      StringBuilder toolOutput) {
    StringBuilder output = toolOutput == null ? new StringBuilder() : toolOutput;
    for (ResponseSegment segment : segments) {
      if (segment.text != null && !segment.text.isBlank()) {
        renderIndented(context, segment.text);
      }
      if (segment.toolLines == null || segment.toolLines.isEmpty()) {
        continue;
      }
      ToolLineResult lineResult =
          runToolLines(context, segment.toolLines, continuation, continuationName, output);
      continuation = lineResult.continuation;
      continuationName = lineResult.continuationName;
      if (!lineResult.success) {
        return new ToolRunResult(output, false, continuation, continuationName, false);
      }
    }
    boolean needsMore = continuation != null;
    return new ToolRunResult(output, true, continuation, continuationName, needsMore);
  }

  private ToolLineResult runToolLines(
      CommandContext context,
      List<String> toolLines,
      Continuation continuation,
      String continuationName,
      StringBuilder toolOutput) {
    for (int i = 0; i < toolLines.size(); i++) {
      String line = toolLines.get(i);
      if (continuation == null) {
        if (context.consumeCancel()) {
          context.warn("Agent chat cancelled.");
          return ToolLineResult.failed(continuation, continuationName);
        }
        context.info(ANSI_CYAN + "> " + line + ANSI_RESET);
        CommandTooling.Result result = CommandTooling.execute(context, line, true);
        toolOutput.append("Command: ").append(line).append('\n');
        toolOutput.append(result.output).append('\n');
        if (!result.success) {
          context.warn("Tool failed; stopping further tool calls.");
          return ToolLineResult.failed(continuation, continuationName);
        }
        continuation = result.continuation;
        continuationName = continuation == null ? null : continuation.commandName;
        continue;
      }
      context.logContinuation(LogLevel.INFO, ANSI_CYAN + "  " + line + ANSI_RESET);
      ContinuationResult contResult = continuation.handler.onLine(line, context);
      if (contResult == null) {
        continue;
      }
      if (contResult.tail != null) {
        String combined = continuationName + " " + contResult.tail;
        CommandTooling.Result result = CommandTooling.execute(context, combined, true);
        toolOutput.append("Command: ").append(combined).append('\n');
        toolOutput.append(result.output).append('\n');
        if (!result.success) {
          context.warn("Tool failed; stopping further tool calls.");
          return ToolLineResult.failed(continuation, continuationName);
        }
        continuation = result.continuation;
        continuationName = continuation == null ? null : continuation.commandName;
      }
      if (!contResult.continueAfter) {
        continuation = null;
        continuationName = null;
      }
    }
    return ToolLineResult.success(continuation, continuationName);
  }

  private void addXanaduFenceSegments(
      List<ResponseSegment> segments, List<String> toolCalls, List<String> fenceLines) {
    List<String> lines = new ArrayList<>();
    for (String line : fenceLines) {
      String trimmed = line.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      lines.add(trimmed);
      toolCalls.add(trimmed);
    }
    if (!lines.isEmpty()) {
      segments.add(ResponseSegment.toolLines(lines));
    }
  }

  private void appendFenceText(StringBuilder textBuffer, List<String> fenceLines) {
    for (int i = 0; i < fenceLines.size(); i++) {
      textBuffer.append(fenceLines.get(i));
      if (i + 1 < fenceLines.size()) {
        textBuffer.append('\n');
      }
    }
  }

  private void flushTextBuffer(List<ResponseSegment> segments, StringBuilder textBuffer) {
    String text = textBuffer.toString().trim();
    if (!text.isBlank()) {
      segments.add(ResponseSegment.text(text));
    }
    textBuffer.setLength(0);
  }

  private static final class ResponseParse {
    private final List<ResponseSegment> segments;
    private final List<String> toolCalls;
    private final boolean incomplete;

    private ResponseParse(List<ResponseSegment> segments, List<String> toolCalls, boolean incomplete) {
      this.segments = segments;
      this.toolCalls = toolCalls;
      this.incomplete = incomplete;
    }
  }

  private static final class ResponseSegment {
    private final String text;
    private final List<String> toolLines;

    private ResponseSegment(String text, List<String> toolLines) {
      this.text = text;
      this.toolLines = toolLines;
    }

    private static ResponseSegment text(String text) {
      return new ResponseSegment(text, null);
    }

    private static ResponseSegment toolLines(List<String> lines) {
      return new ResponseSegment(null, lines);
    }
  }

  private static final class ToolRunResult {
    private final StringBuilder toolOutput;
    private final boolean success;
    private final Continuation continuation;
    private final String continuationName;
    private final boolean needsMoreInput;

    private ToolRunResult(
        StringBuilder toolOutput,
        boolean success,
        Continuation continuation,
        String continuationName,
        boolean needsMoreInput) {
      this.toolOutput = toolOutput;
      this.success = success;
      this.continuation = continuation;
      this.continuationName = continuationName;
      this.needsMoreInput = needsMoreInput;
    }
  }

  private static final class ToolLineResult {
    private final boolean success;
    private final Continuation continuation;
    private final String continuationName;

    private ToolLineResult(boolean success, Continuation continuation, String continuationName) {
      this.success = success;
      this.continuation = continuation;
      this.continuationName = continuationName;
    }

    private static ToolLineResult success(Continuation continuation, String continuationName) {
      return new ToolLineResult(true, continuation, continuationName);
    }

    private static ToolLineResult failed(Continuation continuation, String continuationName) {
      return new ToolLineResult(false, continuation, continuationName);
    }
  }

  private static final class FormattedText implements StyledImage {
    private final List<String> lines;
    private final List<boolean[]> boldFlags;
    private final int height;
    private final int width;

    private FormattedText(String text, int maxWidth) {
      List<LineBuffer> rawLines = parseLines(text);
      List<LineBuffer> wrapped = wrapLines(rawLines, maxWidth);
      this.lines = new ArrayList<>(wrapped.size());
      this.boldFlags = new ArrayList<>(wrapped.size());
      int max = 0;
      for (LineBuffer line : wrapped) {
        String lineText = line.text.toString();
        this.lines.add(lineText);
        this.boldFlags.add(line.bold);
        max = Math.max(max, lineText.length());
      }
      this.height = lines.size();
      this.width = Math.max(0, Math.min(maxWidth, max));
    }

    @Override
    public char at(int i, int j) {
      if (i < 0 || i >= lines.size()) {
        return ' ';
      }
      String line = lines.get(i);
      if (j < 0 || j >= line.length()) {
        return ' ';
      }
      return line.charAt(j);
    }

    @Override
    public TextStyle styleAt(int i, int j) {
      if (i < 0 || i >= boldFlags.size()) {
        return TextStyle.NORMAL;
      }
      boolean[] flags = boldFlags.get(i);
      if (j < 0 || j >= flags.length) {
        return TextStyle.NORMAL;
      }
      if (!flags[j]) {
        return TextStyle.NORMAL;
      }
      char c = at(i, j);
      return c == ' ' ? TextStyle.NORMAL : TextStyle.BOLD;
    }

    private static List<LineBuffer> parseLines(String text) {
      List<LineBuffer> lines = new ArrayList<>();
      LineBuffer current = new LineBuffer();
      boolean bold = false;
      int i = 0;
      while (i < text.length()) {
        char c = text.charAt(i);
        if (c == '\r' || c == '\n') {
          if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
            i++;
          }
          lines.add(current);
          current = new LineBuffer();
          i++;
          continue;
        }
        if (c == '*' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
          bold = !bold;
          i += 2;
          continue;
        }
        current.append(c, bold);
        i++;
      }
      lines.add(current);
      return lines;
    }

    private static List<LineBuffer> wrapLines(List<LineBuffer> lines, int maxWidth) {
      if (maxWidth <= 0) {
        return lines;
      }
      List<LineBuffer> wrapped = new ArrayList<>();
      for (LineBuffer line : lines) {
        if (line.length() <= maxWidth) {
          wrapped.add(line);
          continue;
        }
        int start = 0;
        int length = line.length();
        while (start < length) {
          int limit = Math.min(start + maxWidth, length);
          int breakAt = lastSpaceBefore(line, start, limit);
          if (breakAt > start) {
            wrapped.add(line.slice(start, breakAt));
            start = breakAt + 1;
          } else {
            wrapped.add(line.slice(start, limit));
            start = limit;
          }
          while (start < length && line.charAt(start) == ' ') {
            start++;
          }
        }
      }
      return wrapped;
    }

    private static final class LineBuffer {
      private final StringBuilder text = new StringBuilder();
      private boolean[] bold = new boolean[0];

      private void append(char c, boolean isBold) {
        int index = text.length();
        text.append(c);
        if (index >= bold.length) {
          bold = java.util.Arrays.copyOf(bold, Math.max(8, bold.length * 2));
        }
        bold[index] = isBold;
      }

      private int length() {
        return text.length();
      }

      private char charAt(int index) {
        return text.charAt(index);
      }

      private LineBuffer slice(int start, int end) {
        LineBuffer slice = new LineBuffer();
        for (int i = start; i < end; i++) {
          slice.append(text.charAt(i), bold[i]);
        }
        return slice;
      }
    }

    private static int lastSpaceBefore(LineBuffer line, int start, int end) {
      for (int i = end - 1; i > start; i--) {
        if (line.charAt(i) == ' ') {
          return i;
        }
      }
      return -1;
    }
  }
}
