# Draw Commands

This document demonstrates the drawing commands for creating graphics.

## Clear Command

The `draw clear` command clears the drawing buffer:

```xanadu
> draw clear
```

## Point Command

The `draw point` command draws a point at specific coordinates:

```xanadu
> draw clear
> draw point 5 10
```

## Line Command

The `draw line` command draws a line between two points:

```xanadu
> draw clear
> draw line 0 0 5 5
```

## Rectangle Command

The `draw rect` command draws a rectangle:

```xanadu
> draw clear
> draw rect 0 0 5 10
```

## Text Command

The `draw text` command draws text at a position:

```xanadu
> draw clear
> draw text 2 2 Hello
```

Multiple words:

```xanadu
> draw clear
> draw text 1 1 Hello World
```

## Turtle Graphics

### Pen Control

```xanadu
> draw clear
> draw pen up
> draw pen down
```

### Movement

```xanadu
> draw clear
> draw move 10 10
> draw forward 5
```

### Rotation

```xanadu
> draw clear
> draw left 90
> draw right 45
> draw heading 180
```

### Home

```xanadu
> draw clear
> draw home
```
