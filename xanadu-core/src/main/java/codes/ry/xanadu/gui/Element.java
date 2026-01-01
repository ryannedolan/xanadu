package codes.ry.xanadu.gui;

import java.io.PrintWriter;


public interface Element {

  void handle(Event event);

  default void dump(PrintWriter p) {
    p.print(this.toString());
  }
}
