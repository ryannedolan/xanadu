package codes.ry.xanadu.command;

public abstract class NamedCommandProvider implements CommandProvider {
  private final String name;

  protected NamedCommandProvider(String name) {
    this.name = name;
  }

  @Override
  public boolean supports(CommandInput input) {
    return name.equals(input.name);
  }
}
