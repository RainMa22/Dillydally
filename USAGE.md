# Dillydally — Usage Guide

Dillydally is a lightweight HTTP/HTTPS server with a pluggable handler system. You assign handlers to URL paths via a JSON configuration file, and each incoming request to that path is dispatched to the configured handler.

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Configuration Reference](#configuration-reference)
3. [Handler Assignment Model](#handler-assignment-model)
4. [Built-in Handlers](#built-in-handlers)
   - [FileHandler](#filehandler)
   - [PathRedirect](#pathredirect)
   - [ProtocolRedirect](#protocolredirect)
5. [Registering Custom Handlers](#registering-custom-handlers)
6. [Full Configuration Example](#full-configuration-example)
7. [Notes/GOTCHAs](#notes--gotchas)

---

## Quick Start

1. Run the server once to generate a default `config.json`:

   ```bash
   ./gradlew run
   ```

2. Edit `config.json` to define your handler layout (see below).
3. Run again — the server reads the config and starts.

---
## Configuration Reference


### Top-level `config.json`

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `httpPort` | `int` | `80` | HTTP listen port |
| `httpsPort` | `int` | `443` | HTTPS listen port |
| `doHttps` | `boolean` | `true` | Enable HTTPS (requires certificate config) |
| `serverUrl` | `string` | `"self-sign"` | `"self-sign"` or an ACME directory URL |
| `domains` | `string[]` | `["localhost","127.0.0.1"]` | Domains for the TLS certificate |
| `layoutScheme` | `object` | `{"/": {"FileHandler": {...}}}` | Path-to-handler mapping (see [Handler Assignment Model](#handler-assignment-model)) |
| `sslCertificateConf` | `object` | *(see [SSL Certificate Config](#ssl-certificate-config))* | Certificate and ACME settings |

### SSL Certificate Config

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `type` | `string` | `"handler"` | `"handler"` denotes a handler based HTTP-01 challenge completion strategy <br> (deprecated) `"file"` denotes a filed-based HTTP-01 challenge completion strategy|
| `pathToWebRootDir` | `string` | `"res/static"` | (deprecated) if `type` is `file`, Web root for ACME HTTP-01 challenges(to write `.well-known/acme-challenges`). |
| `pathToACMEPEM` | `string` | `"config/acme.pem"` | Path to save/load ACME account key PEM |
| `pathToSSLKeyPEM` | `string` | `"config/key.pem"` | Path to save/load TLS private key PEM |
| `pathToSSLCertPEM` | `string` | `"config/cert.pem"` | Path to save/load TLS certificate PEM |
| `nPollingRetries` | `int` | `10` | ACME order polling retries |
| `renewalThresholdInDays` | `int` | `5` | Renew cert when fewer than N days remain |
| `acmePassword` | `string` | *(randomly generated if not defined)* | ACME account password to encrypt/decrypt the PEM file |
| `sslKeyPassword` | `string` | *(randomly generated if not defined)* | SSL key password to encrypt/decrypt the PEM file |


### Handler Assignment Model

The core concept is a **path-to-handler mapping** called the *handler layout*:

```
URL path  ──►  Handler instance (constructed with kwargs)
```

The layout is expressed as a JSON object where:
- Each **key** is a URL path prefix (e.g., `"/"`, `"/static"`, `"/api"`).
- Each **value** is a single-key JSON object whose key is the **handler name** and value is the **kwargs** for that handler.

```json
{
  "/": {
    "FileHandler": { "directoryPath": "res/static" }
  },
  "/docs": {
    "PathRedirect": { "redirectTo": "https://docs.example.com", "appendPath": true }
  }
}
```

When a request arrives, DillyDally matches the request URI against the registered context paths and dispatches to the assigned `HttpHandler`.

<!-- ### How assignment works internally

1. `Server.main()` reads `config.json` and deserializes it into a `ConfBean`.
2. `ConfBean.getHandlerLayout()` delegates to `HandlerLayoutLoader.fromJson()`.
3. `HandlerLayoutLoader` iterates the JSON keys, looks up each handler name in the `HandlerRegisty`, calls its constructor function with the kwargs map, and builds a `Map<String, HttpHandler>`.
4. The server creates an HTTP context for each entry: `http.createContext(path, handler)`. -->

---

### Available Handlers

#### FileHandler

Serves static files from a directory.

**handler name:** `FileHandler`  
**kwarg:**

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `directoryPath` | `string` | `"."` | Absolute or relative path to the served directory |

**Behavior:**
- Resolves the request path against the directory (path traversal is blocked — resolves outside the root return `400`).
- Directory requests try `index.html`, then `index.htm`, then generate an HTML directory listing.
- Files are served with `Content-Type` from `Files.probeContentType()`.
- Missing paths return `404` with `<h1>Not Found</h1>`.

**Example:**
```json
"/static": {
  "FileHandler": { "directoryPath": "res/static" }
}
```

---

#### PathRedirect

Issues a `307 Temporary Redirect` to a target URL.

**handler name:** `PathRedirect`  
**kwargs:**

| Key | Type | Description |
|-----|------|-------------|
| `redirectTo` | `string` (URL) | The target base URL |
| `appendPath` | `boolean` | If `true`, appends the requested path to the target |

**Example — redirect `/old-docs/something` → `https://new.site/something`:**
```json
"/old-docs": {
  "PathRedirect": {
    "redirectTo": "https://new.site",
    "appendPath": true
  }
}
```

**Example — redirect `/blog/*` all to a single URL:**
```json
"/blog": {
  "PathRedirect": {
    "redirectTo": "https://medium.com/@me",
    "appendPath": false
  }
}
```

---

#### ProtocolRedirect

Issues a `307 Temporary Redirect` that changes the URL scheme (e.g., HTTP → HTTPS).

**handler name:** `ProtocolRedirect`  
**kwargs:**

| Key | Type | Description |
|-----|------|-------------|
| `protocol` | `string` | Target scheme: `"https"`, `"http"`, etc. |
| `port` | `int` *(optional)* | Override the destination port (defaults to the default port for the given protocol) |

**Example — force HTTPS:**
```json
"/": {
  "ProtocolRedirect": { "protocol": "https" }
}
```

**Example — redirect to a specific port:**
```json
"/": {
  "ProtocolRedirect": { "protocol": "https", "port": 8443 }
}
```

---

## Registering Custom Handlers

Registering a Custom Handler involves creating a project that imports the current `DillyDally.jar` as a provided library using either IntelliJ (Easier)

or with Gradle Configuration:
```groovy
dependencies {
  // ...
  compileonly <path_to_DillyDally_jar>
  // ...
}
```

or with Maven Configuration:

```xml
<!-- ... -->
<dependency>
    <groupId>me.rainma22</groupId>
    <artifactId>dillydally</artifactId>
    <version>1.0</version>
    <scope>provided</scope>
    <systemPath>{path_to_DillyDally_jar}</systemPath>
</dependency>
<!-- ... -->
```

You will need to implement `me.rainma22.abstracts.DillyDallyExtension` which puts your handler logic into the `HandlerRegistry`
```Java
// ...
import me.rainma22.abstracts.DillyDallyExtension;
public class myExtension implements DillyDallyExtension{
  public void onLoad(HandlerRegistry hr){
    hr.register(MyHandler.class, (kwargs) -> {
      // STUB: kwargs handing logic here;
      return new MyHandler(...);
    } );

    // replace my Handler.class and relevant constructor logic
  }
}
```
You will need to package your extension into a Jar file

> (For Beginner: We recommend packing your extensions in a fat Jar such that you won't need to worry about the dependencies jars in the next step). 

Then, in `config/config.json`
```json
{
  // ...
  "extensionNameSpaces": {
    "{namespace}": [
      {path_to_your_jarfile},
      {additional_dependencies}
    ]},
  // ...
  "enabledExtensions": ["{namespace}_{canonical_name_of_your_extension}"],
  // ...
  "layoutScheme": {
    "/": {
      "{canonical_name_of_your_handler}": {
        // kwargs of your handler
      }
    }
  }
  // ...
}
```

Once the configuration is complete, you should be able to see your handler at work for `/*`.

> Namespaces can be any string of your choice a different URLClassLoader are given for different namespaces, if you have extensions with conflicting dependencies, use a different namespace.
> - if the extension entered into `enabledExtensions` does not have a `{namespace}_` prefix, the `default` namespace will be used. 

---

## Full Configuration Example

```json
{
  "httpPort": 80,
  "httpsPort": 443,
  "doHttps": true,
  "serverUrl": "self-sign",
  "domains": ["localhost", "127.0.0.1"],
  "extensionNamespaces": {"default": []},
  "enabledExtensions": [],
  "layoutScheme": {
    "/": {
      "FileHandler": {
        "directoryPath": "res/static"
      }
    },
    "/docs": {
      "PathRedirect": {
        "redirectTo": "https://docs.example.com",
        "appendPath": true
      }
    },
    "/force-https": {
      "ProtocolRedirect": {
        "protocol": "https"
      }
    }
  },
  "sslCertificateConf": {
    "type": "self-sign",
    "pathToWebRootDir": "res/static",
    "nPollingRetries": 10,
    "pathToACMEPEM": "config/acme.pem",
    "pathToSSLKeyPEM": "config/key.pem",
    "pathToSSLCertPEM": "config/cert.pem",
    "renewalThresholdInDays": 5
  }
}
```

---

## Notes / GOTCHAs

- Path matching follows `com.sun.net.httpserver.HttpServer.createContext()` semantics — a context path `"/api"` matches `/api`, `/api/`, `/api/foo`, etc.
- Each path key in `layoutScheme` maps to **exactly one** handler (the JSON enforces a single-key object).
- The `layoutScheme` default serves the current working directory at `/` via `FileHandler`.
- All handlers run on a virtual-thread-per-task executor (`Executors.newVirtualThreadPerTaskExecutor()`).
- built-in handlers are referenced by their keyword("handler name") and not their canonical names.
