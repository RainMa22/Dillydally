package me.rainma22.dillydally.handler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Formatter;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import me.rainma22.dillydally.exceptions.DillyDallyRuntimeException;

public class FileHandler implements HttpHandler {
    private Path fileDir;
    private static final Logger LOGGER = LogManager.getLogger();

    public FileHandler(Path fileDir) {
        this.fileDir = fileDir.toAbsolutePath();
    }

    private Supplier<Optional<InputStream>> getStreamIfExists(Path path) {
        return () -> {
            try {
                return Optional.of(Files.newInputStream(path));
            } catch (Exception e) {
                return Optional.empty();
            }
        };
    }

    private ByteArrayInputStream listFilesAsHtml(Path path) throws IOException {
        StringBuilder doc = new StringBuilder("<!DOCTYPE HTML>\n<html><body><ul>");
        Formatter entryFormatter = new Formatter(doc);
        try (var subFiles = Files.list(path)) {
            subFiles
                    .sorted((p1, p2) -> {
                        if (Files.isDirectory(p1) && !Files.isDirectory(p2)) {
                            return -1;
                        } else if (!Files.isDirectory(p1) && Files.isDirectory(p2)) {
                            return 1;
                        } else {
                            return p1.compareTo(p2);
                        }
                    })
                    .map(p -> {
                        String pathString = path.toAbsolutePath().relativize(p.toAbsolutePath())
                                .toString();
                        boolean isFolder = Files.isDirectory(p);
                        return pathString.concat(isFolder ? "/" : "");
                    })
                    .forEach(s -> entryFormatter.format("<li><a href=%s>%s</a></li>", s, s));

            doc.append("</ul></body></html>");
            byte[] res = doc.toString()
                    .getBytes(StandardCharsets.UTF_8);
            return new ByteArrayInputStream(res);
        }
    }

    @Override
    public void handle(HttpExchange exch) throws IOException {
        Path path = fileDir
                .resolve(Path.of(".", exch.getRequestURI().getPath().replaceFirst("/", "")));
        String fileName = "%s %s".formatted(path, fileDir);
        try {
            if (Files.notExists(path) || !path.toRealPath().startsWith(fileDir.toRealPath())) {
                exch.getResponseHeaders().set("Content-Type", "text/html");
                byte[] res = "<h1>Not Found</h1>".getBytes();

                exch.sendResponseHeaders(404, res.length);
                LOGGER.info("%s %d".formatted(fileName, 404));
                exch.getResponseBody().write(res);
                return;
            } else {

                if (Files.isDirectory(path)) {
                    exch.getResponseHeaders().set("Content-Type", "text/html");
                    InputStream res = getStreamIfExists(path.resolve("index.html")).get()
                            .or(getStreamIfExists(path.resolve("index.htm")))
                            .orElseGet(() -> {
                                try {
                                    return listFilesAsHtml(path);
                                } catch (IOException e) {
                                    String errMsg = "Unexpected Exception when listing directory %s as HTML";
                                    throw new DillyDallyRuntimeException(errMsg.formatted(path), e);
                                }
                            });

                    exch.sendResponseHeaders(200, 0);
                    LOGGER.info("%s %d".formatted(fileName, 200));
                    res.transferTo(exch.getResponseBody());
                    return;
                } else {
                    String contentType = Files.probeContentType(path);
                    if (contentType != null) {
                        exch.getResponseHeaders().set("Content-Type", contentType);
                    }
                    try (var in = path.toUri().toURL().openStream()) {
                        exch.sendResponseHeaders(200, 0);
                        LOGGER.info("%s %d".formatted(fileName, 200));
                        in.transferTo(exch.getResponseBody());
                    }
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            exch.sendResponseHeaders(500, 0);
            LOGGER.info("%s %d".formatted(fileName, 500));
        } finally {
            exch.close();
        }
    }
}
