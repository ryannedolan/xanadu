package codes.ry.xanadu.command;

import java.io.PrintWriter;
import java.io.Writer;

public final class CapturePrintWriter extends PrintWriter {
  private final TeeWriter teeWriter;

  public CapturePrintWriter(PrintWriter screen, PrintWriter capture) {
    this(new TeeWriter(screen, capture));
  }

  private CapturePrintWriter(TeeWriter teeWriter) {
    super(teeWriter, true);
    this.teeWriter = teeWriter;
  }

  public void setCaptureEnabled(boolean enabled) {
    teeWriter.setCaptureEnabled(enabled);
  }

  private static final class TeeWriter extends Writer {
    private final PrintWriter screen;
    private final PrintWriter capture;
    private boolean captureEnabled;

    private TeeWriter(PrintWriter screen, PrintWriter capture) {
      this.screen = screen;
      this.capture = capture;
      this.captureEnabled = true;
    }

    private void setCaptureEnabled(boolean enabled) {
      this.captureEnabled = enabled;
    }

    @Override
    public void write(char[] cbuf, int off, int len) {
      screen.write(cbuf, off, len);
      if (captureEnabled) {
        capture.write(cbuf, off, len);
      }
    }

    @Override
    public void flush() {
      screen.flush();
      if (captureEnabled) {
        capture.flush();
      }
    }

    @Override
    public void close() {
      screen.close();
      if (captureEnabled) {
        capture.close();
      }
    }
  }
}
