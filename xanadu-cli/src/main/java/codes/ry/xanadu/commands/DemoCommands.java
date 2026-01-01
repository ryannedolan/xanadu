package codes.ry.xanadu.commands;

import codes.ry.xanadu.Image;
import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.ReflectiveCommandProvider;

public final class DemoCommands extends ReflectiveCommandProvider {

  public void echo(CommandContext context, String text) {
    context.out.println(text);
    context.out.flush();
  }

  public void add(CommandContext context, int a, int b) {
    context.out.println(a + b);
    context.out.flush();
  }

  public void render(CommandContext context, String text) {
    context.render(text);
  }

}
