package codes.ry.xanadu;

public class Rect {
  public final int top;
  public final int left;
  public final int bottom;
  public final int right;
  public final int height;
  public final int width;

  public Rect(int top, int left, int height, int width) {
    this.top = top;
    this.left = left;
    this.height = height;
    this.width = width;
    this.bottom = top + height;
    this.right = left + width;
  }

  public Rect offset(int i, int j) {
    return new Rect(top + i, left + j, height, width);
  }

  public boolean contains(int i, int j) {
    return i >= left && i < right && j >= top && j < bottom;
  }

  public boolean overlaps(Rect r) {
    return left < r.right && right > r.left && top < r.bottom && bottom > r.top;
  }
}
