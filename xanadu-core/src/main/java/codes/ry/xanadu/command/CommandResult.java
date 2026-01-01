package codes.ry.xanadu.command;

public enum CommandResult {
  SUCCESS,
  FAILURE;

  public boolean isFailure() {
    return this == FAILURE;
  }
}
