package codes.ry.xanadu;

import codes.ry.xanadu.command.CommandContext;
import codes.ry.xanadu.command.CommandService;
import codes.ry.xanadu.render.RenderContext;
import codes.ry.xanadu.render.RenderService;
import java.io.PrintWriter;
import java.util.List;
import java.util.ServiceLoader;

public class Main {

  public static void main(String []arg) {
    PrintWriter w = new PrintWriter(System.out);
    List<codes.ry.xanadu.command.CommandProvider> providers = new java.util.ArrayList<>();
    ServiceLoader.load(codes.ry.xanadu.command.CommandProvider.class).forEach(providers::add);
    CommandService commandService = new CommandService(providers);
    CommandContext commandContext = new CommandContext(w, Style.box(), RenderService.defaults(), commandService, 40, 8);
    loadStartupScript(commandContext);
    Repl repl = new Repl(commandContext, Repl.defaultReader(commandContext), w);
    try {
      repl.run();
    } catch (java.io.IOException e) {
      throw new RuntimeException("REPL failed", e);
    }
  }

  private static void loadStartupScript(CommandContext context) {
    java.nio.file.Path homePath = homeRc();
    java.nio.file.Path localPath = java.nio.file.Path.of(".xanadurc");
    runScriptIfPresent(context, homePath);
    runScriptIfPresent(context, localPath);
  }

  private static void runScriptIfPresent(CommandContext context, java.nio.file.Path path) {
    if (path == null || !java.nio.file.Files.exists(path)) {
      return;
    }
    java.util.List<String> lines;
    try {
      lines = java.nio.file.Files.readAllLines(path);
    } catch (java.io.IOException e) {
      context.warn("Failed to read " + path + ": " + e.getMessage());
      return;
    }
    boolean ok = codes.ry.xanadu.command.ScriptRunner.run(context, lines);
    if (!ok) {
      context.warn("Startup script failed for " + path + ".");
    }
  }

  private static java.nio.file.Path homeRc() {
    String home = System.getProperty("user.home");
    if (home == null || home.isBlank()) {
      return null;
    }
    return java.nio.file.Path.of(home, ".xanadurc");
  }
}
