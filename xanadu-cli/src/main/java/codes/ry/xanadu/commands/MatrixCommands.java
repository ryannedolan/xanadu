package codes.ry.xanadu.commands;

import codes.ry.xanadu.Frame;
import codes.ry.xanadu.Image;
import codes.ry.xanadu.Style;
import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandInput;
import codes.ry.xanadu.command.CommandProvider;
import codes.ry.xanadu.command.CommandResult;
import codes.ry.xanadu.command.ReportLabels;
import codes.ry.xanadu.render.RenderContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MatrixCommands implements CommandProvider {
  private static final String MAT = "mat";
  private static final String EYE = "eye";
  private static final String ZEROS = "zeros";
  @Override
  public boolean supports(CommandInput input) {
    String name = input.name.toLowerCase(Locale.ROOT);
    if (MAT.equals(name)) {
      if (input.args.isEmpty()) {
        return true;
      }
      String tail = input.tail();
      if (tail == null || tail.isBlank()) {
        return true;
      }
      return numericOrSeparators(input.args);
    }
    if (EYE.equals(name)) {
      return input.args.size() == 1 && parseInt(input.args.get(0)) != null;
    }
    if (ZEROS.equals(name)) {
      return (input.args.size() == 1 || input.args.size() == 2) && parseInt(input.args.get(0)) != null;
    }
    return false;
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
    return java.util.Set.of(MAT, EYE, ZEROS);
  }

  @Override
  public java.util.List<String> usage(String commandName) {
    switch (commandName) {
      case MAT:
        return java.util.List.of("mat <values...>", "mat <values...; values...; values...>");
      case EYE:
        return java.util.List.of("eye <n>");
      case ZEROS:
        return java.util.List.of("zeros <rows> [cols]");
      default:
        return java.util.List.of(commandName);
    }
  }

  private void execute(CommandContext context, CommandInput input) {
    String name = input.name.toLowerCase(Locale.ROOT);
    switch (name) {
      case MAT:
        renderMatrix(context, parseMatrix(input.tail()));
        return;
      case EYE:
        renderMatrix(context, identityMatrix(context, input));
        return;
      case ZEROS:
        renderMatrix(context, zerosMatrix(context, input));
        return;
      default:
        context.error("Unknown command: " + input.name);
    }
  }

  private List<List<Double>> identityMatrix(CommandContext context, CommandInput input) {
    Integer size = parseInt(input.args.get(0));
    if (size == null || size <= 0) {
      usageError(context, EYE);
      return null;
    }
    List<List<Double>> matrix = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      List<Double> row = new ArrayList<>();
      for (int j = 0; j < size; j++) {
        row.add(i == j ? 1.0 : 0.0);
      }
      matrix.add(row);
    }
    return matrix;
  }

  private List<List<Double>> zerosMatrix(CommandContext context, CommandInput input) {
    Integer rows = parseInt(input.args.get(0));
    Integer cols = input.args.size() > 1 ? parseInt(input.args.get(1)) : rows;
    if (rows == null || cols == null || rows < 0 || cols < 0) {
      usageError(context, ZEROS);
      return null;
    }
    List<List<Double>> matrix = new ArrayList<>();
    for (int i = 0; i < rows; i++) {
      List<Double> row = new ArrayList<>();
      for (int j = 0; j < cols; j++) {
        row.add(0.0);
      }
      matrix.add(row);
    }
    return matrix;
  }

  private List<List<Double>> parseMatrix(String tail) {
    if (tail == null || tail.isBlank()) {
      return null;
    }
    String[] parts = tail.split(";");
    List<List<Double>> matrix = new ArrayList<>();
    for (String part : parts) {
      String trimmed = part.trim();
      if (trimmed.isEmpty()) {
        return null;
      }
      String[] tokens = trimmed.split("\\s+");
      List<Double> row = new ArrayList<>();
      for (String token : tokens) {
        Double value = parseDouble(token);
        if (value == null) {
          return null;
        }
        row.add(value);
      }
      if (row.isEmpty()) {
        return null;
      }
      matrix.add(row);
    }
    return matrix;
  }

  private void renderMatrix(CommandContext context, List<List<Double>> matrix) {
    if (matrix == null || matrix.isEmpty()) {
      usageError(context, MAT);
      return;
    }
    int columns = matrix.get(0).size();
    for (List<Double> row : matrix) {
      if (row.size() != columns) {
        context.error("Matrix rows must have the same length.");
        return;
      }
    }
    ReportLabels.Labels labels = ReportLabels.consume(context);
    if (labels != null && !labels.isEmpty()) {
      if (!isBlank(labels.title)) {
        renderHeaderFrame(context, labels.title, Style.boxHeader());
      }
      if (!isBlank(labels.subtitle)) {
        renderHeaderFrame(context, labels.subtitle, Style.header());
      }
    }
    Frame frame = buildFrame(context, matrix);
    context.render(frame);
    if (labels != null && !labels.isEmpty()) {
      if (!isBlank(labels.xLabel)) {
        context.out.println("X: " + labels.xLabel);
      }
      if (!isBlank(labels.yLabel)) {
        context.out.println("Y: " + labels.yLabel);
      }
      if (!isBlank(labels.note)) {
        context.out.println("Note: " + labels.note);
      }
    }
    context.out.flush();
  }

  private Double parseDouble(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return Double.parseDouble(raw);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Integer parseInt(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return Integer.parseInt(raw);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private boolean numericOrSeparators(List<String> args) {
    for (String raw : args) {
      String cleaned = raw.replace(";", "").trim();
      if (cleaned.isEmpty()) {
        continue;
      }
      if (parseDouble(cleaned) == null) {
        return false;
      }
    }
    return true;
  }

  private void usageError(CommandContext context, String commandName) {
    for (String line : usage(commandName)) {
      context.out.println("Usage: " + line);
    }
    context.out.flush();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private void renderHeaderFrame(CommandContext context, String text, Style style) {
    if (text == null || text.isBlank()) {
      return;
    }
    String trimmed = text.trim();
    Image title = Image.text(trimmed);
    Frame frame = style.frame(1, trimmed.length(), title).border(style);
    context.render(frame);
  }

  private Frame buildFrame(CommandContext context, List<List<Double>> matrix) {
    RenderContext sizing = new RenderContext(context.style, 0, 0, context.renderService, false);
    int cols = matrix.get(0).size();
    List<Image[]> rendered = new ArrayList<>(matrix.size());
    int[] widths = new int[cols];
    for (List<Double> row : matrix) {
      Image[] rowImages = new Image[cols];
      for (int i = 0; i < cols; i++) {
        Image image = context.renderService.render(row.get(i), sizing);
        rowImages[i] = image;
        widths[i] = Math.max(widths[i], widthForCell(row.get(i), image));
      }
      rendered.add(rowImages);
    }
    Frame table = null;
    for (int r = 0; r < matrix.size(); r++) {
      List<Double> row = matrix.get(r);
      Image[] rowImages = rendered.get(r);
      Frame rowFrame = null;
      for (int i = 0; i < row.size(); i++) {
        Image cell = rowImages[i];
        int cellHeight = 1;
        if (cell instanceof Frame) {
          cellHeight = Math.max(1, ((Frame) cell).drawRect.height);
        }
        int contentWidth = Math.min(widths[i], widthForCell(row.get(i), cell));
        int offset = Math.max(0, widths[i] - contentWidth);
        Image aligned = cell.offset(0, offset).limit(cellHeight, widths[i]);
        Frame cellFrame = context.style.frame(cellHeight, widths[i], aligned).border();
        rowFrame = rowFrame == null ? cellFrame : rowFrame.append(cellFrame);
      }
      if (rowFrame == null) {
        rowFrame = context.style.frame(1, 1, Image.flood(' ')).border();
      }
      table = table == null ? rowFrame : table.appendVertical(rowFrame);
    }
    return table == null ? context.style.frame(1, 1, Image.flood(' ')).border() : table;
  }

  private int widthForCell(Object value, Image image) {
    if (image instanceof Frame) {
      return Math.max(1, ((Frame) image).drawRect.width);
    }
    if (value == null) {
      return 0;
    }
    return value.toString().length();
  }
}
