package codes.ry.xanadu.render;

import codes.ry.xanadu.Image;

final class ToStringRendererProvider implements RendererProvider {
  @Override
  public boolean supports(Object value) {
    return true;
  }

  @Override
  public Renderer renderer() {
    return new Renderer() {
      @Override
      public Image render(Object value, RenderContext context) {
        if (context.maxWidth > 0) {
          return context.style.text(context.maxWidth, String.valueOf(value));
        }
        return context.style.text(String.valueOf(value));
      }
    };
  }
}
