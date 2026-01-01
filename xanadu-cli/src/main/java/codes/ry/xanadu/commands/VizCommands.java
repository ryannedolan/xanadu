package codes.ry.xanadu.commands;

import codes.ry.xanadu.Frame;
import codes.ry.xanadu.Image;
import codes.ry.xanadu.Style;
import codes.ry.xanadu.StyledImages;
import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandInput;
import codes.ry.xanadu.command.CommandProvider;
import codes.ry.xanadu.command.CommandResult;
import java.util.ArrayList;
import java.util.List;

public final class VizCommands implements CommandProvider {
  private static final String BAR = "bar";
  private static final String HBAR = "hbar";
  private static final String SPARK = "spark";
  private static final char[] SPARK_CHARS = {'▁', '▂', '▃', '▄', '▅', '▆', '▇', '█'};
  private static final int BOX_BAR_THRESHOLD = 6;
  private static final int BOX_BAR_THICKNESS = 3;
  private static final int BOX_BAR_GAP = 1;
  private static final int BOX_BAR_GAP_WIDE = 2;

  @Override
  public boolean supports(CommandInput input) {
    String name = input.name.toLowerCase();
    return BAR.equals(name) || HBAR.equals(name) || SPARK.equals(name);
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
    return java.util.Set.of(BAR, HBAR, SPARK);
  }

  @Override
  public java.util.List<String> usage(String commandName) {
    switch (commandName) {
      case BAR:
        return java.util.List.of("bar <values...>");
      case HBAR:
        return java.util.List.of("hbar <values...>");
      case SPARK:
        return java.util.List.of("spark <values...>");
      default:
        return java.util.List.of(commandName);
    }
  }

  private void execute(CommandContext context, CommandInput input) {
    List<Float> values = parseValues(context, input);
    if (values == null) {
      return;
    }
    switch (input.name.toLowerCase()) {
      case BAR:
        context.render(barChart(context, values));
        return;
      case HBAR:
        context.render(horizontalBars(context, values));
        return;
      case SPARK:
        context.render(sparkline(context, values));
        return;
      default:
        context.error("Unknown command: " + input.name);
    }
  }

  private List<Float> parseValues(CommandContext context, CommandInput input) {
    if (input.args.isEmpty()) {
      context.error("No values provided.");
      for (String line : usage(input.name.toLowerCase())) {
        context.out.println("  " + line);
      }
      context.out.flush();
      return null;
    }
    List<Float> values = new ArrayList<>();
    for (String raw : input.args) {
      try {
        values.add(Float.parseFloat(raw));
      } catch (NumberFormatException e) {
        context.error("Invalid number: " + raw);
        return null;
      }
    }
    return values;
  }

  private Frame barChart(CommandContext context, List<Float> values) {
    Style style = context.style;
    Style boxStyle = Style.box();
    boolean useBoxes = values.size() <= BOX_BAR_THRESHOLD;
    int height =
        context.maxHeight > 0
            ? Math.max(useBoxes ? 3 : 4, context.maxHeight - 2)
            : (useBoxes ? 8 : 10);
    float max = max(values);
    if (max <= 0f) {
      max = 1f;
    }
    if (useBoxes) {
      Frame chart = boxBarChart(values, height, max, boxStyle);
      return chart == null ? style.frame(1, 1, Image.flood(' ')) : chart;
    }
    Frame chart = null;
    Frame gap = style.frame(height, 1, Image.flood(' '));
    for (int i = 0; i < values.size(); i++) {
      float value = values.get(i);
      float scaled = value * height / max;
      if (scaled < 0f) {
        scaled = 0f;
      }
      Frame bar = style.vbar(height, scaled);
      if (chart == null) {
        chart = bar;
      } else {
        chart = chart.append(gap).append(bar);
      }
    }
    if (chart == null) {
      return style.frame(1, 1, Image.flood(' '));
    }
    return chart.border();
  }

  private Frame horizontalBars(CommandContext context, List<Float> values) {
    Style style = context.style;
    Style boxStyle = Style.box();
    boolean useBoxes = values.size() <= BOX_BAR_THRESHOLD;
    int width = context.maxWidth > 0 ? Math.max(6, context.maxWidth - 2) : 20;
    float max = max(values);
    if (max <= 0f) {
      max = 1f;
    }
    if (useBoxes) {
      Frame chart = boxHBarChart(values, width, max, boxStyle);
      return chart == null ? style.frame(1, 1, Image.flood(' ')) : chart;
    }
    Frame chart = null;
    for (float value : values) {
      float scaled = value * width / max;
      if (scaled < 0f) {
        scaled = 0f;
      }
      Frame bar = style.hbar(width, scaled);
      chart = chart == null ? bar : chart.appendVertical(bar);
    }
    if (chart == null) {
      return style.frame(1, 1, Image.flood(' '));
    }
    return chart.border();
  }

  private Frame sparkline(CommandContext context, List<Float> values) {
    float max = max(values);
    if (max <= 0f) {
      max = 1f;
    }
    StringBuilder sb = new StringBuilder();
    for (float value : values) {
      float normalized = value / max;
      if (normalized < 0f) {
        normalized = 0f;
      }
      if (normalized > 1f) {
        normalized = 1f;
      }
      int index = Math.round(normalized * (SPARK_CHARS.length - 1));
      sb.append(SPARK_CHARS[index]);
    }
    return context.style.text(sb.toString());
  }

  private float max(List<Float> values) {
    float max = Float.NEGATIVE_INFINITY;
    for (float value : values) {
      max = Math.max(max, value);
    }
    return max == Float.NEGATIVE_INFINITY ? 0f : max;
  }

  private Frame boxBarChart(List<Float> values, int height, float max, Style boxStyle) {
    if (values.isEmpty()) {
      return null;
    }
    Frame chart = null;
    Frame gap = boxStyle.frame(height, BOX_BAR_GAP_WIDE, Image.flood(' '));
    for (float value : values) {
      float scaled = value * height / max;
      if (scaled < 0f) {
        scaled = 0f;
      }
      int barHeight = (int) Math.round(scaled);
      if (value > 0f && barHeight < 2) {
        barHeight = 2;
      }
      Frame bar;
      if (barHeight > 0) {
        Image image = rectImage(height - barHeight, 0, barHeight, BOX_BAR_THICKNESS, boxStyle);
        bar = new Frame(height, BOX_BAR_THICKNESS, image, boxStyle);
      } else {
        bar = new Frame(height, BOX_BAR_THICKNESS, Image.flood(' '), boxStyle);
      }
      chart = chart == null ? bar : chart.append(gap).append(bar);
    }
    if (chart == null) {
      return null;
    }
    final Frame chart2 = chart;
    Image axis = (i, j) -> (i == chart2.height - 1)
        ? boxStyle.glyph(Style.EAST | Style.WEST)
        : ' ';
    Image combined = StyledImages.combine(chart.image, axis, boxStyle);
    return chart.withImage(combined);
  }

  private Frame boxHBarChart(List<Float> values, int width, float max, Style boxStyle) {
    if (values.isEmpty()) {
      return null;
    }
    Frame chart = null;
    for (float value : values) {
      float scaled = value * width / max;
      if (scaled < 0f) {
        scaled = 0f;
      }
      int barWidth = (int) Math.round(scaled);
      if (value > 0f && barWidth < 2) {
        barWidth = 2;
      }
      Frame bar;
      if (barWidth > 0) {
        Image image = rectImage(0, 0, BOX_BAR_THICKNESS, barWidth, boxStyle);
        bar = new Frame(BOX_BAR_THICKNESS, width, image, boxStyle);
      } else {
        bar = new Frame(BOX_BAR_THICKNESS, width, Image.flood(' '), boxStyle);
      }
      chart = chart == null ? bar : chart.appendVertical(bar);
    }
    if (chart == null) {
      return null;
    }
    Image axis = (i, j) -> (j == 0)
        ? boxStyle.glyph(Style.NORTH | Style.SOUTH)
        : ' ';
    Image combined = StyledImages.combine(chart.image, axis, boxStyle);
    return chart.withImage(combined);
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
}
