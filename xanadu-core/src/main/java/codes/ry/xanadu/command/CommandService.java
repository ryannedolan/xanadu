package codes.ry.xanadu.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CommandService {
  private final List<CommandProvider> providers;
  private final List<CommandProvider> enabled;
  private final List<CommandProvider> disabled;

  public CommandService(List<CommandProvider> providers) {
    this(providers, List.of(), List.of());
  }

  private CommandService(
      List<CommandProvider> providers,
      List<CommandProvider> enabled,
      List<CommandProvider> disabled) {
    this.providers = List.copyOf(providers);
    this.enabled = List.copyOf(enabled);
    this.disabled = List.copyOf(disabled);
  }

  public Optional<Command> find(CommandInput input) {
    for (CommandProvider provider : enabled) {
      if (disabled.contains(provider)) {
        continue;
      }
      if (provider.supports(input)) {
        return Optional.of(provider.commandFor(input));
      }
    }
    for (CommandProvider provider : providers) {
      if (disabled.contains(provider) || enabled.contains(provider)) {
        continue;
      }
      if (provider.supports(input)) {
        return Optional.of(provider.commandFor(input));
      }
    }
    return Optional.empty();
  }

  public CommandService enable(CommandProvider provider) {
    var nextEnabled = new ArrayList<>(enabled);
    if (!nextEnabled.contains(provider)) {
      nextEnabled.add(provider);
    }
    var nextDisabled = new ArrayList<>(disabled);
    nextDisabled.remove(provider);
    return new CommandService(providers, nextEnabled, nextDisabled);
  }

  public CommandService disable(CommandProvider provider) {
    var nextDisabled = new ArrayList<>(disabled);
    if (!nextDisabled.contains(provider)) {
      nextDisabled.add(provider);
    }
    var nextEnabled = new ArrayList<>(enabled);
    nextEnabled.remove(provider);
    return new CommandService(providers, nextEnabled, nextDisabled);
  }

  public List<CommandProvider> providers() {
    return providers;
  }

  public List<CommandProvider> enabledProviders() {
    return enabled;
  }

  public List<CommandProvider> disabledProviders() {
    return disabled;
  }
}
