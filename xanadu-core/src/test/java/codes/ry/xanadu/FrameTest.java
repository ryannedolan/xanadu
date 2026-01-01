package codes.ry.xanadu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

class FrameTest {

  @Test
  void borderDrawsOutsideContent() {
    Style style = Style.box();
    Frame frame = style.text(10, "hi").border();
    assertEquals(
        "\n┌──────────┐\n│hi        │\n└──────────┘\n",
        render(frame));
  }

  @Test
  void rowOfCellsUsesJunctions() {
    Style style = Style.box();
    Frame row =
        style
            .text(5, "A")
            .border()
            .append(style.text(5, "B").border())
            .append(style.text(5, "C").border());
    assertEquals(
        "\n┌─────┬─────┬─────┐\n│A    │B    │C    │\n└─────┴─────┴─────┘\n",
        render(row));
  }

  @Test
  void tableUsesJunctionsAcrossRows() {
    Style style = Style.box();
    Frame row1 =
        style
            .text(5, "A")
            .border()
            .append(style.text(5, "B").border())
            .append(style.text(5, "C").border());
    Frame row2 =
        style
            .text(5, "D")
            .border()
            .append(style.text(5, "E").border())
            .append(style.text(5, "F").border());
    Frame table = row1.appendVertical(row2);
    assertEquals(
        "\n┌─────┬─────┬─────┐\n│A    │B    │C    │\n├─────┼─────┼─────┤\n│D    │E    │F    │\n└─────┴─────┴─────┘\n",
        render(table));
  }

  private static String render(Frame frame) {
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    frame.dump(writer);
    writer.flush();
    return out.toString();
  }
}
