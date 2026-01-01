package codes.ry.xanadu.command;

public final class ContinuationResult {
  public final String tail;
  public final boolean continueAfter;

  private ContinuationResult(String tail, boolean continueAfter) {
    this.tail = tail;
    this.continueAfter = continueAfter;
  }

  public static ContinuationResult continueWithoutExecution() {
    return new ContinuationResult(null, true);
  }

  public static ContinuationResult executeAndContinue(String tail) {
    return new ContinuationResult(tail, true);
  }

  public static ContinuationResult executeAndEnd(String tail) {
    return new ContinuationResult(tail, false);
  }

  public static ContinuationResult end() {
    return new ContinuationResult(null, false);
  }
}
