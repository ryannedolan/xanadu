package codes.ry.xanadu.command;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public abstract class ReflectiveCommandProvider implements CommandProvider {

  @Override
  public final boolean supports(CommandInput input) {
    return findMethod(input) != null;
  }

  @Override
  public final Command commandFor(CommandInput input) {
    Method match = findMethod(input);
    if (match == null) {
      throw new IllegalArgumentException("No matching command: " + input.name);
    }
    Object[] parsed = parseArgs(match.getParameterTypes(), input.args);
    return context -> invoke(match, context, parsed);
  }

  @Override
  public java.util.Set<String> commandNames() {
    java.util.Set<String> names = new java.util.TreeSet<>();
    Method[] methods = getClass().getDeclaredMethods();
    for (Method method : methods) {
      if (!Modifier.isPublic(method.getModifiers())) {
        continue;
      }
      if (method.isSynthetic()) {
        continue;
      }
      names.add(method.getName());
    }
    return names;
  }

  @Override
  public java.util.List<String> usage(String commandName) {
    java.util.List<String> lines = new java.util.ArrayList<>();
    Method[] methods = getClass().getDeclaredMethods();
    for (Method method : methods) {
      if (!Modifier.isPublic(method.getModifiers())) {
        continue;
      }
      if (method.isSynthetic()) {
        continue;
      }
      if (!method.getName().equals(commandName)) {
        continue;
      }
      lines.add(signature(method));
    }
    return lines;
  }

  private Method findMethod(CommandInput input) {
    Method[] methods = getClass().getDeclaredMethods();
    for (Method method : methods) {
      if (!Modifier.isPublic(method.getModifiers())) {
        continue;
      }
      if (!method.getName().equals(input.name)) {
        continue;
      }
      if (!isApplicable(method, input.args)) {
        continue;
      }
      return method;
    }
    return null;
  }

  private boolean isApplicable(Method method, List<String> args) {
    Class<?>[] types = method.getParameterTypes();
    int argOffset = 0;
    if (types.length > 0 && types[0] == CommandContext.class) {
      argOffset = 1;
    }
    if (types.length - argOffset != args.size()) {
      return false;
    }
    for (int i = argOffset; i < types.length; i++) {
      if (parseArg(types[i], args.get(i - argOffset)) == null) {
        return false;
      }
    }
    return true;
  }

  private Object[] parseArgs(Class<?>[] types, List<String> args) {
    List<Object> parsed = new ArrayList<>();
    int argOffset = 0;
    if (types.length > 0 && types[0] == CommandContext.class) {
      parsed.add(null);
      argOffset = 1;
    }
    for (int i = argOffset; i < types.length; i++) {
      parsed.add(parseArg(types[i], args.get(i - argOffset)));
    }
    return parsed.toArray();
  }

  private CommandResult invoke(Method method, CommandContext context, Object[] args) {
    try {
      if (args.length > 0 && args[0] == null && method.getParameterTypes()[0] == CommandContext.class) {
        args[0] = context;
      }
      Object result = method.invoke(this, args);
      if (result instanceof CommandResult) {
        return (CommandResult) result;
      }
      if (result instanceof Boolean) {
        return ((Boolean) result) ? CommandResult.SUCCESS : CommandResult.FAILURE;
      }
      return CommandResult.SUCCESS;
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Command method is not accessible: " + method.getName(), e);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause() == null ? e : e.getCause();
      throw new RuntimeException("Command failed: " + method.getName(), cause);
    }
  }

  private Object parseArg(Class<?> type, String raw) {
    if (type == String.class) {
      return raw;
    }
    if (type == int.class || type == Integer.class) {
      try {
        return Integer.parseInt(raw);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    if (type == long.class || type == Long.class) {
      try {
        return Long.parseLong(raw);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    if (type == float.class || type == Float.class) {
      try {
        return Float.parseFloat(raw);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    if (type == double.class || type == Double.class) {
      try {
        return Double.parseDouble(raw);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    if (type == boolean.class || type == Boolean.class) {
      if ("true".equalsIgnoreCase(raw)) {
        return true;
      }
      if ("false".equalsIgnoreCase(raw)) {
        return false;
      }
      return null;
    }
    return null;
  }

  private String signature(Method method) {
    StringBuilder sb = new StringBuilder();
    sb.append(method.getName());
    var params = method.getParameters();
    int index = 0;
    boolean first = true;
    for (var param : params) {
      if (param.getType() == CommandContext.class) {
        continue;
      }
      sb.append(' ');
      sb.append(paramName(param, index, params));
      first = false;
      index++;
    }
    return sb.toString();
  }

  private String paramName(java.lang.reflect.Parameter param, int index, java.lang.reflect.Parameter[] params) {
    if (param.isNamePresent()) {
      return param.getName();
    }
    int total = 0;
    for (var p : params) {
      if (p.getType() != CommandContext.class) {
        total++;
      }
    }
    String base = typeLabel(param.getType());
    if (total <= 1) {
      return base;
    }
    return base + (index + 1);
  }

  private String typeLabel(Class<?> type) {
    if (type == String.class) {
      return "string";
    }
    if (type == int.class || type == Integer.class) {
      return "int";
    }
    if (type == long.class || type == Long.class) {
      return "long";
    }
    if (type == float.class || type == Float.class) {
      return "float";
    }
    if (type == double.class || type == Double.class) {
      return "double";
    }
    if (type == boolean.class || type == Boolean.class) {
      return "bool";
    }
    return type.getSimpleName().toLowerCase();
  }
}
