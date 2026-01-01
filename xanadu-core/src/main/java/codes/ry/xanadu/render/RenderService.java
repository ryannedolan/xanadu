package codes.ry.xanadu.render;

import codes.ry.xanadu.Image;
import java.util.List;

public final class RenderService {
  private final List<RendererProvider> providers;
  private final List<RendererProvider> enabled;
  private final List<RendererProvider> disabled;

  public RenderService(List<RendererProvider> providers) {
    this(providers, List.of(), List.of());
  }

  private RenderService(
      List<RendererProvider> providers,
      List<RendererProvider> enabled,
      List<RendererProvider> disabled) {
    this.providers = List.copyOf(providers);
    this.enabled = List.copyOf(enabled);
    this.disabled = List.copyOf(disabled);
  }

  public Image render(Object value, RenderContext context) {
    for (RendererProvider provider : enabled) {
      if (disabled.contains(provider)) {
        continue;
      }
      if (provider.supports(value)) {
        return provider.renderer().render(value, context);
      }
    }
    for (RendererProvider provider : providers) {
      if (disabled.contains(provider) || enabled.contains(provider)) {
        continue;
      }
      if (provider.supports(value)) {
        return provider.renderer().render(value, context);
      }
    }
    return context.style.text(String.valueOf(value));
  }

  public RenderService enable(RendererProvider provider) {
    if (enabled.contains(provider)) {
      return this;
    }
    var next = new java.util.ArrayList<>(enabled);
    next.add(provider);
    return new RenderService(providers, next, disabled);
  }

  public RenderService disable(RendererProvider provider) {
    if (disabled.contains(provider)) {
      return this;
    }
    var next = new java.util.ArrayList<>(disabled);
    next.add(provider);
    return new RenderService(providers, enabled, next);
  }

  public static RenderService defaults() {
    return new RenderService(List.of(new IterableRendererProvider(), new ToStringRendererProvider()));
  }
}
