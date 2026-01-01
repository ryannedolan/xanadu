package codes.ry.xanadu.render;

public interface RendererProvider {
  boolean supports(Object value);

  Renderer renderer();
}
