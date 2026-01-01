package codes.ry.xanadu.gui;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Container implements Element {
  private final List<Element> elements = new ArrayList<>();
  private Element focused = null;

  @Override
  public void handle(Event event) {
    for (var e : elements) {
      e.handle(event);
    }
  }

  void add(Element e) {
    elements.add(e);
  }

  void remove(Element e) {
    elements.remove(e);
  }

  void show(Element e) {
    elements.remove(e);
    elements.add(e);
  }

  void blur() {
    focused = null;
  }

  void focus(Element e) {
    show(e);
    focused = e;
  }

  Element focused() {
    return focused;
  }

  @Override
  public void dump(PrintWriter p) {
    for (var e : elements) {
      e.dump(p);
    }
  }
}
