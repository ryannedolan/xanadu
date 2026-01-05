package codes.ry.xanadu.commands;

import codes.ry.xanadu.Frame;
import codes.ry.xanadu.Image;
import codes.ry.xanadu.Style;
import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandInput;
import codes.ry.xanadu.command.CommandProvider;
import codes.ry.xanadu.command.CommandResult;
import codes.ry.xanadu.command.ReportLabels;
import codes.ry.xanadu.render.TableRenderer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class StatsCommands implements CommandProvider {
  private static final String STATS = "stats";
  private static final char BAR_HORIZONTAL = '─';
  private static final char BAR_VERTICAL = '│';
  private static final char MEAN_MARK = '▲';
  private static final char MEDIAN_MARK = '◆';
  private static final char OVERLAP_MARK = '✚';
  private static final char BULLET = '•';
  private static final DecimalFormat NUMBER_FORMAT;

  static {
    NUMBER_FORMAT = new DecimalFormat("0.####");
    NUMBER_FORMAT.setGroupingUsed(false);
  }

  @Override
  public boolean supports(CommandInput input) {
    String name = input.name.toLowerCase(Locale.ROOT);
    if (!STATS.equals(name)) {
      return false;
    }
    if (input.args.isEmpty()) {
      return true;
    }
    String tail = input.tail();
    if (tail.contains(";")) {
      return numericOrSeparators(input.args);
    }
    return allNumeric(input.args);
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
    return java.util.Set.of(STATS);
  }

  @Override
  public java.util.List<String> usage(String commandName) {
    if (STATS.equals(commandName)) {
      return java.util.List.of(
          "stats <values...>",
          "stats <values...; values...; values...>");
    }
    return java.util.List.of(commandName);
  }

  private void execute(CommandContext context, CommandInput input) {
    String name = input.name.toLowerCase(Locale.ROOT);
    if (STATS.equals(name)) {
      statsReport(context, input);
      return;
    }
    context.error("Unknown command: " + input.name);
  }

  private void statsReport(CommandContext context, CommandInput input) {
    if (input.args.isEmpty()) {
      usageError(context, STATS);
      return;
    }
    ReportLabels.Labels labels = ReportLabels.consume(context);
    String tail = input.tail();
    if (tail == null || tail.isBlank()) {
      usageError(context, STATS);
      return;
    }
    if (tail.contains(";")) {
      multiSeriesReport(context, tail, labels);
      return;
    }
    List<Float> values = parseValues(input.args);
    if (values == null || values.isEmpty()) {
      context.error("No numeric values provided.");
      return;
    }
    Stats stats = computeStats(values);
    printHeader(context, labels, "Stats Report");
    printStats(context, stats);
    printRangeBar(context, stats);
    printLabelFooter(context, labels);
  }

  private void multiSeriesReport(CommandContext context, String tail, ReportLabels.Labels labels) {
    List<List<Float>> series = parseSeries(tail);
    if (series == null || series.size() < 2) {
      context.error("Provide at least two arrays separated by ';'.");
      return;
    }
    boolean matrixLike = isMatrix(series);
    if (matrixLike) {
      printHeader(context, labels, "Matrix Stats Report");
    } else {
      printHeader(context, labels, "Multi-Series Stats Report");
    }
    printSeriesSummaries(context, series);
    if (matrixLike) {
      printCorrelationMatrix(context, series);
      printChiSquare(context, series);
    } else {
      printLengthNote(context, series);
    }
    printLabelFooter(context, labels);
  }

  private void printHeader(CommandContext context, ReportLabels.Labels labels, String fallback) {
    String title = (labels != null && !isBlank(labels.title)) ? labels.title : fallback;
    String subtitle = labels == null ? null : labels.subtitle;
    renderHeaderFrame(context, title, Style.boxHeader());
    if (!isBlank(subtitle)) {
      renderHeaderFrame(context, subtitle, Style.header());
    }
  }

  private void printStats(CommandContext context, Stats stats) {
    renderHeaderFrame(context, "Summary", Style.header());
    context.out.println("  " + BULLET + " Count: " + stats.count);
    context.out.println("  " + BULLET + " Sum: " + format(stats.sum));
    context.out.println("  " + BULLET + " Mean: " + format(stats.mean));
    context.out.println("  " + BULLET + " Median: " + format(stats.median));
    context.out.println("  " + BULLET + " Std Dev: " + format(stats.stddev));
    context.out.println("  " + BULLET + " Min: " + format(stats.min));
    context.out.println("  " + BULLET + " Max: " + format(stats.max));
    context.out.println("  " + BULLET + " Range: " + format(stats.max - stats.min));
    context.out.println("  " + BULLET + " P25: " + format(stats.p25));
    context.out.println("  " + BULLET + " P75: " + format(stats.p75));
    context.out.flush();
  }

  private void printRangeBar(CommandContext context, Stats stats) {
    renderHeaderFrame(context, "Range", Style.header());
    context.out.println("  " + rangeBar(stats.min, stats.max, stats.mean, stats.median, 30));
    context.out.println(
        "  min="
            + format(stats.min)
            + " mean="
            + format(stats.mean)
            + " median="
            + format(stats.median)
            + " max="
            + format(stats.max));
    context.out.flush();
  }

  private void printSeriesSummaries(CommandContext context, List<List<Float>> series) {
    renderHeaderFrame(context, "Series Summary", Style.header());
    int index = 1;
    for (List<Float> values : series) {
      Stats stats = computeStats(values);
      context.out.println("  " + BULLET + " Series " + index + ":");
      context.out.println("      Count: " + stats.count);
      context.out.println("      Mean: " + format(stats.mean));
      context.out.println("      Std Dev: " + format(stats.stddev));
      context.out.println("      Min: " + format(stats.min));
      context.out.println("      Max: " + format(stats.max));
      index++;
    }
    context.out.flush();
  }

  private void printCorrelationMatrix(CommandContext context, List<List<Float>> series) {
    int count = series.size();
    double[] means = new double[count];
    double[] stddevs = new double[count];
    for (int i = 0; i < count; i++) {
      Stats stats = computeStats(series.get(i));
      means[i] = stats.mean;
      stddevs[i] = stats.stddev;
    }
    renderHeaderFrame(context, "Correlation Matrix", Style.header());
    List<List<Object>> matrix = new ArrayList<>();
    List<Object> header = new ArrayList<>();
    header.add("");
    for (int i = 0; i < count; i++) {
      header.add("S" + (i + 1));
    }
    matrix.add(header);
    for (int i = 0; i < count; i++) {
      List<Object> row = new ArrayList<>();
      row.add("S" + (i + 1));
      for (int j = 0; j < count; j++) {
        double corr = correlation(series.get(i), series.get(j), means[i], means[j], stddevs[i], stddevs[j]);
        row.add(format(corr));
      }
      matrix.add(row);
    }
    renderMatrix(context, matrix);
    context.out.flush();
  }

  private void printChiSquare(CommandContext context, List<List<Float>> series) {
    int rows = series.size();
    int cols = series.get(0).size();
    if (rows < 2 || cols < 2) {
      return;
    }
    double[][] table = new double[rows][cols];
    for (int i = 0; i < rows; i++) {
      List<Float> row = series.get(i);
      for (int j = 0; j < cols; j++) {
        Float value = row.get(j);
        if (value == null || value < 0f || value % 1.0f != 0f) {
          return;
        }
        table[i][j] = value;
      }
    }
    double chiSquare = chiSquare(table);
    int df = (rows - 1) * (cols - 1);
    renderHeaderFrame(context, "Chi-Square Test", Style.header());
    context.out.println("  " + BULLET + " Chi-Square: " + format(chiSquare));
    context.out.println("  " + BULLET + " Degrees of Freedom: " + df);
    context.out.flush();
  }

  private void printLengthNote(CommandContext context, List<List<Float>> series) {
    int min = Integer.MAX_VALUE;
    int max = 0;
    for (List<Float> values : series) {
      min = Math.min(min, values.size());
      max = Math.max(max, values.size());
    }
    if (min != max) {
      renderHeaderFrame(context, "Notes", Style.header());
      context.out.println("  " + BULLET + " Series lengths differ; correlation skipped.");
      context.out.flush();
    }
  }

  private void printLabelFooter(CommandContext context, ReportLabels.Labels labels) {
    if (labels == null || labels.isEmpty()) {
      return;
    }
    boolean printed = false;
    if (!isBlank(labels.xLabel)) {
      context.out.println("X: " + labels.xLabel);
      printed = true;
    }
    if (!isBlank(labels.yLabel)) {
      context.out.println("Y: " + labels.yLabel);
      printed = true;
    }
    if (!isBlank(labels.note)) {
      context.out.println("Note: " + labels.note);
      printed = true;
    }
    if (printed) {
      context.out.flush();
    }
  }

  private Stats computeStats(List<Float> values) {
    int count = values.size();
    double sum = 0.0;
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    List<Double> sorted = new ArrayList<>(count);
    for (float value : values) {
      double v = value;
      sum += v;
      min = Math.min(min, v);
      max = Math.max(max, v);
      sorted.add(v);
    }
    Collections.sort(sorted);
    double mean = sum / count;
    double median = percentile(sorted, 0.5);
    double p25 = percentile(sorted, 0.25);
    double p75 = percentile(sorted, 0.75);
    double variance = 0.0;
    if (count > 1) {
      for (double v : sorted) {
        double diff = v - mean;
        variance += diff * diff;
      }
      variance = variance / (count - 1);
    }
    double stddev = Math.sqrt(variance);
    return new Stats(count, sum, mean, median, stddev, min, max, p25, p75);
  }

  private double percentile(List<Double> sorted, double p) {
    if (sorted.isEmpty()) {
      return 0.0;
    }
    if (sorted.size() == 1) {
      return sorted.get(0);
    }
    double pos = p * (sorted.size() - 1);
    int lower = (int) Math.floor(pos);
    int upper = (int) Math.ceil(pos);
    if (lower == upper) {
      return sorted.get(lower);
    }
    double weight = pos - lower;
    return sorted.get(lower) * (1.0 - weight) + sorted.get(upper) * weight;
  }

  private double correlation(
      List<Float> left, List<Float> right, double leftMean, double rightMean, double leftStd, double rightStd) {
    if (leftStd == 0.0 || rightStd == 0.0 || left.size() < 2) {
      return 0.0;
    }
    double sum = 0.0;
    for (int i = 0; i < left.size(); i++) {
      sum += (left.get(i) - leftMean) * (right.get(i) - rightMean);
    }
    double cov = sum / (left.size() - 1);
    return cov / (leftStd * rightStd);
  }

  private List<Float> parseValues(List<String> args) {
    List<Float> values = new ArrayList<>();
    for (String raw : args) {
      if (raw.contains(";")) {
        return null;
      }
      Float value = parseFloat(raw);
      if (value == null) {
        return null;
      }
      values.add(value);
    }
    return values;
  }

  private List<List<Float>> parseSeries(String tail) {
    String[] parts = tail.split(";");
    List<List<Float>> series = new ArrayList<>();
    for (String part : parts) {
      String trimmed = part.trim();
      if (trimmed.isEmpty()) {
        return null;
      }
      String[] tokens = trimmed.split("\\s+");
      List<Float> values = new ArrayList<>();
      for (String token : tokens) {
        Float value = parseFloat(token);
        if (value == null) {
          return null;
        }
        values.add(value);
      }
      if (values.isEmpty()) {
        return null;
      }
      series.add(values);
    }
    return series;
  }

  private boolean allNumeric(List<String> args) {
    for (String raw : args) {
      if (!isNumeric(raw)) {
        return false;
      }
    }
    return true;
  }

  private boolean numericOrSeparators(List<String> args) {
    for (String raw : args) {
      String cleaned = raw.replace(";", "").trim();
      if (cleaned.isEmpty()) {
        continue;
      }
      if (!isNumeric(cleaned)) {
        return false;
      }
    }
    return true;
  }

  private boolean isNumeric(String raw) {
    return parseFloat(raw) != null;
  }

  private Float parseFloat(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return Float.parseFloat(raw);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private void usageError(CommandContext context, String commandName) {
    for (String line : usage(commandName)) {
      context.out.println("Usage: " + line);
    }
    context.out.flush();
  }

  private String rangeBar(double min, double max, double mean, double median, int width) {
    if (width < 5) {
      width = 5;
    }
    char[] bar = new char[width];
    for (int i = 0; i < width; i++) {
      bar[i] = BAR_HORIZONTAL;
    }
    bar[0] = BAR_VERTICAL;
    bar[width - 1] = BAR_VERTICAL;
    if (max > min) {
      int meanPos = (int) Math.round((mean - min) / (max - min) * (width - 1));
      int medianPos = (int) Math.round((median - min) / (max - min) * (width - 1));
      meanPos = clamp(meanPos, 0, width - 1);
      medianPos = clamp(medianPos, 0, width - 1);
      bar[meanPos] = MEAN_MARK;
      bar[medianPos] = bar[medianPos] == MEAN_MARK ? OVERLAP_MARK : MEDIAN_MARK;
    } else {
      int mid = width / 2;
      bar[mid] = MEAN_MARK;
    }
    return new String(bar);
  }

  private int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  private String format(double value) {
    return NUMBER_FORMAT.format(value);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private void renderHeaderFrame(CommandContext context, String text, Style style) {
    if (text == null || text.isBlank()) {
      return;
    }
    Image title = Image.text(text.trim());
    Frame frame = style.frame(1, text.trim().length(), title).border(style);
    context.render(frame);
  }

  private boolean isMatrix(List<List<Float>> series) {
    if (series.isEmpty()) {
      return false;
    }
    int length = series.get(0).size();
    if (length == 0) {
      return false;
    }
    for (List<Float> values : series) {
      if (values.size() != length) {
        return false;
      }
    }
    return true;
  }

  private double chiSquare(double[][] table) {
    int rows = table.length;
    int cols = table[0].length;
    double[] rowTotals = new double[rows];
    double[] colTotals = new double[cols];
    double total = 0.0;
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        double value = table[i][j];
        rowTotals[i] += value;
        colTotals[j] += value;
        total += value;
      }
    }
    double chi = 0.0;
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        double expected = (rowTotals[i] * colTotals[j]) / total;
        if (expected <= 0.0) {
          continue;
        }
        double diff = table[i][j] - expected;
        chi += (diff * diff) / expected;
      }
    }
    return chi;
  }

  private void renderMatrix(CommandContext context, List<List<Object>> matrix) {
    if (matrix.isEmpty()) {
      return;
    }
    List<Object> header = matrix.get(0);
    List<List<Object>> rows =
        matrix.size() > 1 ? matrix.subList(1, matrix.size()) : java.util.List.of();
    Frame table =
        TableRenderer.render(
            context.style,
            context.renderService,
            rows,
            header,
            TableRenderer.rightAlignment());
    if (table != null) {
      context.render(table);
    }
  }

  private static final class Stats {
    private final int count;
    private final double sum;
    private final double mean;
    private final double median;
    private final double stddev;
    private final double min;
    private final double max;
    private final double p25;
    private final double p75;

    private Stats(
        int count,
        double sum,
        double mean,
        double median,
        double stddev,
        double min,
        double max,
        double p25,
        double p75) {
      this.count = count;
      this.sum = sum;
      this.mean = mean;
      this.median = median;
      this.stddev = stddev;
      this.min = min;
      this.max = max;
      this.p25 = p25;
      this.p75 = p75;
    }
  }
}
