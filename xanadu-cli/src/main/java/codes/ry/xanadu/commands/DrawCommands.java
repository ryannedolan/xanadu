package codes.ry.xanadu.commands;

import codes.ry.xanadu.Frame;
import codes.ry.xanadu.Image;
import codes.ry.xanadu.StyledImages;
import codes.ry.xanadu.Style;
import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandInput;
import codes.ry.xanadu.command.CommandProvider;
import codes.ry.xanadu.command.CommandResult;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DrawCommands implements CommandProvider {
  private static final String COMMAND = "draw";
  private static final String BUFFER_KEY = "draw.buffer";
  private static final String TURTLE_KEY = "draw.turtle";
  private static final Style BOX_STYLE = Style.box();
  private static final Style SUBCELL_STYLE = Style.subcellBlocks();
  private static final char POINT_GLYPH = '█';

  @Override
  public boolean supports(CommandInput input) {
    return COMMAND.equalsIgnoreCase(input.name);
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
    return java.util.Set.of(COMMAND);
  }

  @Override
  public java.util.List<String> subcommands(String commandName) {
    if (!COMMAND.equals(commandName)) {
      return java.util.List.of();
    }
    return java.util.List.of(
        "point",
        "line",
        "rect",
        "text",
        "clear",
        "show",
        "pen",
        "move",
        "forward",
        "left",
        "right",
        "heading",
        "home");
  }

  @Override
  public java.util.List<String> usage(String commandName) {
    if (!COMMAND.equals(commandName)) {
      return java.util.List.of(commandName);
    }
    return java.util.List.of(
        "draw point <row> <col>",
        "draw line <row1> <col1> <row2> <col2>",
        "draw rect <top> <left> <height> <width>",
        "draw text <row> <col> <text...>",
        "draw show <height> <width>",
        "draw clear",
        "draw pen <up|down>",
        "draw move <row> <col>",
        "draw forward <steps>",
        "draw left <degrees>",
        "draw right <degrees>",
        "draw heading <degrees>",
        "draw home");
  }

  private void execute(CommandContext context, CommandInput input) {
    if (input.args.isEmpty()) {
      printUsage(context);
      return;
    }
    String sub = input.args.get(0).toLowerCase(Locale.ROOT);
    switch (sub) {
      case "point":
        drawPoint(context, input.args);
        return;
      case "line":
        drawLine(context, input.args);
        return;
      case "rect":
        drawRect(context, input.args);
        return;
      case "text":
        drawText(context, input.args);
        return;
      case "show":
        drawShow(context, input.args);
        return;
      case "clear":
        drawClear(context, input.args);
        return;
      case "pen":
        turtlePen(context, input.args);
        return;
      case "move":
        turtleMove(context, input.args);
        return;
      case "forward":
        turtleForward(context, input.args);
        return;
      case "left":
        turtleTurn(context, input.args, true);
        return;
      case "right":
        turtleTurn(context, input.args, false);
        return;
      case "heading":
        turtleHeading(context, input.args);
        return;
      case "home":
        turtleHome(context, input.args);
        return;
      default:
        context.warn("Unknown draw subcommand: " + sub);
        printUsage(context);
    }
  }

  private void drawPoint(CommandContext context, List<String> args) {
    if (args.size() != 3) {
      usageError(context, "draw point <row> <col>");
      return;
    }
    Integer row = parseInt(args.get(1));
    Integer col = parseInt(args.get(2));
    if (row == null || col == null) {
      usageError(context, "draw point <row> <col>");
      return;
    }
    Image point = (i, j) -> (i == row && j == col) ? POINT_GLYPH : ' ';
    overlay(context, point);
  }

  private void drawLine(CommandContext context, List<String> args) {
    if (args.size() != 5) {
      usageError(context, "draw line <row1> <col1> <row2> <col2>");
      return;
    }
    Integer row1 = parseInt(args.get(1));
    Integer col1 = parseInt(args.get(2));
    Integer row2 = parseInt(args.get(3));
    Integer col2 = parseInt(args.get(4));
    if (row1 == null || col1 == null || row2 == null || col2 == null) {
      usageError(context, "draw line <row1> <col1> <row2> <col2>");
      return;
    }
    Image line;
    if (row1 == row2 || col1 == col2) {
      line = straightLineImage(row1, col1, row2, col2, BOX_STYLE);
      overlayWithStyle(context, line, BOX_STYLE);
    } else {
      line = aaLineImage(row1, col1, row2, col2, SUBCELL_STYLE);
      overlayWithStyle(context, line, SUBCELL_STYLE);
    }
  }

  private void drawRect(CommandContext context, List<String> args) {
    if (args.size() != 5) {
      usageError(context, "draw rect <top> <left> <height> <width>");
      return;
    }
    Integer top = parseInt(args.get(1));
    Integer left = parseInt(args.get(2));
    Integer height = parseInt(args.get(3));
    Integer width = parseInt(args.get(4));
    if (top == null || left == null || height == null || width == null) {
      usageError(context, "draw rect <top> <left> <height> <width>");
      return;
    }
    if (height <= 0 || width <= 0) {
      context.warn("Rectangle height and width must be positive.");
      return;
    }
    Image rect = rectImage(top, left, height, width, BOX_STYLE);
    overlayWithStyle(context, rect, BOX_STYLE);
  }

  private void drawText(CommandContext context, List<String> args) {
    if (args.size() < 4) {
      usageError(context, "draw text <row> <col> <text...>");
      return;
    }
    Integer row = parseInt(args.get(1));
    Integer col = parseInt(args.get(2));
    if (row == null || col == null) {
      usageError(context, "draw text <row> <col> <text...>");
      return;
    }
    String text = String.join(" ", args.subList(3, args.size()));
    Image image = Image.text(text).offset(row, col);
    overlay(context, image);
  }

  private void drawShow(CommandContext context, List<String> args) {
    if (args.size() != 3) {
      usageError(context, "draw show <height> <width>");
      return;
    }
    Integer height = parseInt(args.get(1));
    Integer width = parseInt(args.get(2));
    if (height == null || width == null || height <= 0 || width <= 0) {
      usageError(context, "draw show <height> <width>");
      return;
    }
    Image buffer = buffer(context);
    Frame frame = new Frame(height, width, buffer, context.style);
    frame.dump(context.out);
    context.out.flush();
  }

  private void drawClear(CommandContext context, List<String> args) {
    if (args.size() != 1) {
      usageError(context, "draw clear");
      return;
    }
    context.put(BUFFER_KEY, Image.flood(' '));
  }

  private void turtlePen(CommandContext context, List<String> args) {
    if (args.size() != 2) {
      usageError(context, "draw pen <up|down>");
      return;
    }
    String mode = args.get(1).toLowerCase(Locale.ROOT);
    TurtleState turtle = turtle(context);
    switch (mode) {
      case "up":
        turtle.penDown = false;
        return;
      case "down":
        turtle.penDown = true;
        return;
      default:
        usageError(context, "draw pen <up|down>");
    }
  }

  private void turtleMove(CommandContext context, List<String> args) {
    if (args.size() != 3) {
      usageError(context, "draw move <row> <col>");
      return;
    }
    Integer row = parseInt(args.get(1));
    Integer col = parseInt(args.get(2));
    if (row == null || col == null) {
      usageError(context, "draw move <row> <col>");
      return;
    }
    TurtleState turtle = turtle(context);
    if (turtle.penDown) {
      drawSegment(context, turtle.row, turtle.col, row, col);
    }
    turtle.row = row;
    turtle.col = col;
  }

  private void turtleForward(CommandContext context, List<String> args) {
    if (args.size() != 2) {
      usageError(context, "draw forward <steps>");
      return;
    }
    Double steps = parseDouble(args.get(1));
    if (steps == null) {
      usageError(context, "draw forward <steps>");
      return;
    }
    TurtleState turtle = turtle(context);
    double radians = Math.toRadians(turtle.headingDegrees);
    int targetRow = (int) Math.round(turtle.row - steps * Math.sin(radians));
    int targetCol = (int) Math.round(turtle.col + steps * Math.cos(radians));
    if (turtle.penDown) {
      drawSegment(context, turtle.row, turtle.col, targetRow, targetCol);
    }
    turtle.row = targetRow;
    turtle.col = targetCol;
  }

  private void turtleTurn(CommandContext context, List<String> args, boolean left) {
    if (args.size() != 2) {
      usageError(context, left ? "draw left <degrees>" : "draw right <degrees>");
      return;
    }
    Double degrees = parseDouble(args.get(1));
    if (degrees == null) {
      usageError(context, left ? "draw left <degrees>" : "draw right <degrees>");
      return;
    }
    TurtleState turtle = turtle(context);
    turtle.headingDegrees = normalizeHeading(turtle.headingDegrees + (left ? degrees : -degrees));
  }

  private void turtleHeading(CommandContext context, List<String> args) {
    if (args.size() != 2) {
      usageError(context, "draw heading <degrees>");
      return;
    }
    Double degrees = parseDouble(args.get(1));
    if (degrees == null) {
      usageError(context, "draw heading <degrees>");
      return;
    }
    TurtleState turtle = turtle(context);
    turtle.headingDegrees = normalizeHeading(degrees);
  }

  private void turtleHome(CommandContext context, List<String> args) {
    if (args.size() != 1) {
      usageError(context, "draw home");
      return;
    }
    TurtleState turtle = turtle(context);
    turtle.row = 0;
    turtle.col = 0;
    turtle.headingDegrees = 0.0;
  }

  private void drawSegment(CommandContext context, int row1, int col1, int row2, int col2) {
    Image line;
    if (row1 == row2 || col1 == col2) {
      line = straightLineImage(row1, col1, row2, col2, BOX_STYLE);
      overlayWithStyle(context, line, BOX_STYLE);
    } else {
      line = aaLineImage(row1, col1, row2, col2, SUBCELL_STYLE);
      overlayWithStyle(context, line, SUBCELL_STYLE);
    }
  }

  private void overlay(CommandContext context, Image overlay) {
    Image base = buffer(context);
    context.put(BUFFER_KEY, StyledImages.overlay(base, overlay));
  }

  private void overlayWithStyle(CommandContext context, Image overlay, Style style) {
    Image base = buffer(context);
    context.put(BUFFER_KEY, StyledImages.combine(base, overlay, style));
  }

  private Image buffer(CommandContext context) {
    Image buffer = context.get(BUFFER_KEY, Image.class);
    if (buffer == null) {
      buffer = Image.flood(' ');
      context.put(BUFFER_KEY, buffer);
    }
    return buffer;
  }

  private static Integer parseInt(String raw) {
    try {
      return Integer.parseInt(raw);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static Double parseDouble(String raw) {
    try {
      return Double.parseDouble(raw);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static double normalizeHeading(double degrees) {
    double normalized = degrees % 360.0;
    if (normalized < 0) {
      normalized += 360.0;
    }
    return normalized;
  }

  private void usageError(CommandContext context, String usage) {
    context.error("Usage: " + usage);
  }

  private void printUsage(CommandContext context) {
    context.out.println("Usage:");
    for (String line : usage(COMMAND)) {
      context.out.println("  " + line);
    }
    context.out.flush();
  }

  private static Image rectImage(int top, int left, int height, int width, Style style) {
    int bottom = top + height - 1;
    int right = left + width - 1;
    return (i, j) -> {
      if (i < top || i > bottom || j < left || j > right) {
        return ' ';
      }
      if (i != top && i != bottom && j != left && j != right) {
        return ' ';
      }
      int mask = 0;
      if (i > top && (j == left || j == right)) {
        mask |= Style.NORTH;
      }
      if (i < bottom && (j == left || j == right)) {
        mask |= Style.SOUTH;
      }
      if (j > left && (i == top || i == bottom)) {
        mask |= Style.WEST;
      }
      if (j < right && (i == top || i == bottom)) {
        mask |= Style.EAST;
      }
      return mask == 0 ? ' ' : style.glyph(mask);
    };
  }

  private static Image straightLineImage(int row1, int col1, int row2, int col2, Style style) {
    int minRow = Math.min(row1, row2);
    int maxRow = Math.max(row1, row2);
    int minCol = Math.min(col1, col2);
    int maxCol = Math.max(col1, col2);
    if (row1 == row2) {
      int row = row1;
      int mask = Style.EAST | Style.WEST;
      char glyph = style.glyph(mask);
      return (i, j) -> (i == row && j >= minCol && j <= maxCol) ? glyph : ' ';
    }
    int col = col1;
    int mask = Style.NORTH | Style.SOUTH;
    char glyph = style.glyph(mask);
    return (i, j) -> (j == col && i >= minRow && i <= maxRow) ? glyph : ' ';
  }

  private static Image aaLineImage(int row1, int col1, int row2, int col2, Style style) {
    Map<Long, Integer> masks = new HashMap<>();
    int y1 = row1 * 2 + 1;
    int x1 = col1 * 2 + 1;
    int y2 = row2 * 2 + 1;
    int x2 = col2 * 2 + 1;

    int dx = Math.abs(x2 - x1);
    int dy = Math.abs(y2 - y1);
    int sx = x1 < x2 ? 1 : -1;
    int sy = y1 < y2 ? 1 : -1;
    int err = dx - dy;

    int minRow = Integer.MAX_VALUE;
    int maxRow = Integer.MIN_VALUE;
    int minCol = Integer.MAX_VALUE;
    int maxCol = Integer.MIN_VALUE;

    int x = x1;
    int y = y1;
    while (true) {
      int row = y / 2;
      int col = x / 2;
      int mask = subcellMask(y, x);
      long key = pointKey(row, col);
      masks.put(key, masks.getOrDefault(key, 0) | mask);
      if (row < minRow) {
        minRow = row;
      }
      if (row > maxRow) {
        maxRow = row;
      }
      if (col < minCol) {
        minCol = col;
      }
      if (col > maxCol) {
        maxCol = col;
      }
      if (x == x2 && y == y2) {
        break;
      }
      int e2 = 2 * err;
      if (e2 > -dy) {
        err -= dy;
        x += sx;
      }
      if (e2 < dx) {
        err += dx;
        y += sy;
      }
    }

    int finalMinRow = minRow;
    int finalMaxRow = maxRow;
    int finalMinCol = minCol;
    int finalMaxCol = maxCol;
    return (i, j) -> {
      if (i < finalMinRow || i > finalMaxRow || j < finalMinCol || j > finalMaxCol) {
        return ' ';
      }
      Integer mask = masks.get(pointKey(i, j));
      if (mask == null || mask == 0) {
        return ' ';
      }
      return style.glyph(mask);
    };
  }

  private static int subcellMask(int y, int x) {
    int subY = y & 1;
    int subX = x & 1;
    if (subY == 0 && subX == 0) {
      return Style.NORTH;
    }
    if (subY == 0) {
      return Style.EAST;
    }
    if (subX == 0) {
      return Style.SOUTH;
    }
    return Style.WEST;
  }

  private static long pointKey(int row, int col) {
    return (((long) row) << 32) | (col & 0xffffffffL);
  }

  private static final class TurtleState {
    private int row;
    private int col;
    private double headingDegrees;
    private boolean penDown;

    private TurtleState() {
      this.row = 0;
      this.col = 0;
      this.headingDegrees = 0.0;
      this.penDown = true;
    }
  }

  private TurtleState turtle(CommandContext context) {
    TurtleState turtle = context.get(TURTLE_KEY, TurtleState.class);
    if (turtle == null) {
      turtle = new TurtleState();
      context.put(TURTLE_KEY, turtle);
    }
    return turtle;
  }
}
