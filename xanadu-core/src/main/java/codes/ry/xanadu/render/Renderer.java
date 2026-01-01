package codes.ry.xanadu.render;

import codes.ry.xanadu.Image;

public interface Renderer {
  Image render(Object value, RenderContext context);
}
