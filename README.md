# Dillydally

A configurable HTTP/HTTPS server with automatic SSL certificate renewal and a pluggable handler extension system.

## Features

- **HTTP/HTTPS serving** — virtual-thread-per-task executor for high concurrency
- **Automatic SSL** — self-signed or ACME (Let's Encrypt) certificates with renewal
- **Configurable handler layout** — assign handlers to URL paths via JSON
- **Built-in handlers** — static file serving, path redirect, protocol redirect
- **Extensible** — load custom handlers from external JARs at runtime

## Quick Start

### Requirements

- JDK 21+
- Gradle 9.7.+

### Build & Run

```bash
./gradlew run
```

First run generates `config/config.json` with defaults. Edit it, then run again.

### Build a Fat JAR

```bash
./gradlew shadowJar
```

Output: `app/build/libs/app-all.jar`

## Configuration

All configuration lives in `config/config.json`. The server writes defaults on first run. 

see [USAGE.md](./USAGE.md).

## License

See [LICENSE](./LICENSE)
