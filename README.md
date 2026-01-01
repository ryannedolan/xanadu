# Xanadu Data Console

Xanadu is a data console for thinking about data. It provides an interactive
REPL / CLI, text-based graphics, LLM-backed chat commands, and a growing set of
built-in visualization and drawing commands.

## Features
- REPL with pluggable commands and simple scripting
- Image and Frame primitives for composing terminal visuals
- Drawing commands with layering, box glyphs, and turtle graphics
- Quick charts: bar, hbar, and sparkline
- Optional LLM chat backends (ChatGPT, Claude, Gemini)

## Requirements
- Java 17

## Build
```bash
./gradlew build
```

## Run the CLI
```bash
./gradlew :xanadu-cli:install
./xan
```

## Quick Start

Basic help:
```text
> help
> help <command>
```

AI Chat (requires API key):
```text
> chat
chat > Show me what we can do with this.

  Here is a quick tour demonstrating the calculation, visualization, and database capabilities of Xanadu.

  - > add 50 50
      100
  - > spark 1 2 4 8 16 32 64

      ▁▁▁▂▃▅█
  - > bar 10 20 30 40 50

                          ┌─┐
                          │ │
                          │ │
                     ┌─┐  │ │
                     │ │  │ │
                     │ │  │ │
                ┌─┐  │ │  │ │
                │ │  │ │  │ │
           ┌─┐  │ │  │ │  │ │
           │ │  │ │  │ │  │ │
           │ │  │ │  │ │  │ │
      ┌─┐  │ │  │ │  │ │  │ │
      │ │  │ │  │ │  │ │  │ │
      ┴─┴──┴─┴──┴─┴──┴─┴──┴─┴
  - > draw clear
  - > draw rect 0 0 5 20
  - > draw text 2 2 Hello Xanadu!
  - > draw show 6 22

      ┌──────────────────┐
      │                  │
      │ Hello Xanadu!    │
      │                  │
      └──────────────────┘

  I will connect to the H2 in-memory database, create a `users` table, insert some sample data, and query it.

  - > connect jdbc:h2:mem:test
      Connected.
  - > create table users (id int primary key, name varchar(255));
      Updated 0 rows.
  - > insert into users values (1, 'Alice'), (2, 'Bob'), (3, 'Charlie');
      Updated 3 rows.
  - > select * from users;

      ┌──┬───────┐
      │ID│ NAME  │
      ╪══╪═══════╪
      │ 1│Alice  │
      ├──┼───────┤
      │ 2│Bob    │
      ├──┼───────┤
      │ 3│Charlie│
      └──┴───────┘
  - > select name from users where id > 1;

      ┌───────┐
      │ NAME  │
      ╪═══════╪
      │Bob    │
      ├───────┤
      │Charlie│
      └───────┘
```

## Configuration
- ChatGPT: set `OPENAI_API_KEY`
- Gemini: set `GOOGLE_API_KEY`
- Claude: etc

## Project layout
- `xanadu-core`: core image/frame and command infrastructure
- `xanadu-cli`: REPL and built-in commands
- `xanadu-chatgpt`, `xanadu-claude`, `xanadu-gemini`: LLM backends
- `xanadu-jdbc`: JDBC commands
