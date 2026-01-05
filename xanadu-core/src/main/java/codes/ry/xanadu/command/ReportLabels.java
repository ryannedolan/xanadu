package codes.ry.xanadu.command;

public final class ReportLabels {
  private static final String LABEL_KEY = "report.labels";

  private ReportLabels() {}

  public static void setTitle(CommandContext context, String value) {
    if (context == null) {
      return;
    }
    pending(context).title = value;
  }

  public static void setSubtitle(CommandContext context, String value) {
    if (context == null) {
      return;
    }
    pending(context).subtitle = value;
  }

  public static void setXLabel(CommandContext context, String value) {
    if (context == null) {
      return;
    }
    pending(context).xLabel = value;
  }

  public static void setYLabel(CommandContext context, String value) {
    if (context == null) {
      return;
    }
    pending(context).yLabel = value;
  }

  public static void setNote(CommandContext context, String value) {
    if (context == null) {
      return;
    }
    pending(context).note = value;
  }

  public static Labels consume(CommandContext context) {
    if (context == null) {
      return Labels.empty();
    }
    Pending pending = context.get(LABEL_KEY, Pending.class);
    if (pending == null || pending.isEmpty()) {
      return Labels.empty();
    }
    context.remove(LABEL_KEY);
    return pending.freeze();
  }

  public static boolean hasPending(CommandContext context) {
    Pending pending = context == null ? null : context.get(LABEL_KEY, Pending.class);
    return pending != null && !pending.isEmpty();
  }

  private static Pending pending(CommandContext context) {
    Pending existing = context.get(LABEL_KEY, Pending.class);
    if (existing != null) {
      return existing;
    }
    Pending created = new Pending();
    context.put(LABEL_KEY, created);
    return created;
  }

  public static final class Labels {
    public final String title;
    public final String subtitle;
    public final String xLabel;
    public final String yLabel;
    public final String note;

    private Labels(String title, String subtitle, String xLabel, String yLabel, String note) {
      this.title = title;
      this.subtitle = subtitle;
      this.xLabel = xLabel;
      this.yLabel = yLabel;
      this.note = note;
    }

    public boolean isEmpty() {
      return isBlank(title) && isBlank(subtitle) && isBlank(xLabel) && isBlank(yLabel) && isBlank(note);
    }

    public static Labels empty() {
      return new Labels(null, null, null, null, null);
    }
  }

  private static final class Pending {
    private String title;
    private String subtitle;
    private String xLabel;
    private String yLabel;
    private String note;

    private boolean isEmpty() {
      return isBlank(title) && isBlank(subtitle) && isBlank(xLabel) && isBlank(yLabel) && isBlank(note);
    }

    private Labels freeze() {
      return new Labels(title, subtitle, xLabel, yLabel, note);
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
