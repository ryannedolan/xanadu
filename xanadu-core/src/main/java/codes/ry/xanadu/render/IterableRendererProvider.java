package codes.ry.xanadu.render;

import codes.ry.xanadu.Frame;
import codes.ry.xanadu.Image;
import codes.ry.xanadu.Style;

final class IterableRendererProvider implements RendererProvider {
  @Override
  public boolean supports(Object value) {
    return value instanceof Iterable<?>;
  }

  @Override
  public Renderer renderer() {
    return new Renderer() {
      @Override
      public Image render(Object value, RenderContext context) {
        Iterable<?> iterable = (Iterable<?>) value;
        var items = new java.util.ArrayList<>();
        for (Object item : iterable) {
          items.add(item);
        }
        Frame combined = null;
        for (int index = 0; index < items.size(); index++) {
          Object item = items.get(index);
          Image rendered = context.service.render(item, context);
          Frame frame = asFrame(rendered, context);
          Frame row = prefixWithTreeConnector(frame, index, items.size(), context);
          combined = combined == null ? row : combined.appendVertical(row);
        }
        return combined == null ? context.style.text("") : combined;
      }
    };
  }

  private Frame prefixWithTreeConnector(
      Frame frame,
      int index,
      int total,
      RenderContext context) {
    Style tree = Style.tree();
    boolean first = index == 0;
    boolean last = index == total - 1;
    Image connector = (i, j) -> {
      if (i < 0 || i >= frame.height) {
        return ' ';
      }
      if (j == 0) {
        if (i == 0) {
          int mask = 0;
          if (!first) {
            mask |= 0b1000;
          }
          if (!last) {
            mask |= 0b0010;
          }
          mask |= 0b0100;
          return tree.glyph(mask);
        }
        if (!last) {
          return tree.glyph(0b1000 | 0b0010);
        }
        return ' ';
      }
      if (j == 1) {
        return i == 0 ? tree.glyph(0b0100) : ' ';
      }
      return ' ';
    };
    Frame prefix = tree.frame(frame.height, 2, connector);
    return prefix.append(frame);
  }

  private Frame asFrame(Image image, RenderContext context) {
    if (image instanceof Frame) {
      return (Frame) image;
    }
    return context.style.frame(1, 1, image.limit(1, 1));
  }
}
