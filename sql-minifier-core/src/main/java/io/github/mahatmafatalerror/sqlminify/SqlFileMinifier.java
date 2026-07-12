package io.github.mahatmafatalerror.sqlminify;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

/** Minifies SQL resource files in place. */
public final class SqlFileMinifier {

  private static final List<String> DEFAULT_INCLUDES = List.of("**/*.sql", "*.sql");
  private static final List<String> DEFAULT_EXCLUDES = List.of();

  private SqlFileMinifier() {}

  /** Minifies all SQL files below {@code directory}. */
  public static Result minifyDirectory(Path directory) throws IOException {
    return minifyDirectory(directory, DEFAULT_INCLUDES, DEFAULT_EXCLUDES);
  }

  /** Minifies matching SQL files below {@code directory}. */
  public static Result minifyDirectory(Path directory, List<String> includes, List<String> excludes)
      throws IOException {
    if (!Files.isDirectory(directory)) {
      return new Result(0, 0);
    }

    List<PathMatcher> includeMatchers = matchers(includes);
    List<PathMatcher> excludeMatchers = matchers(excludes);
    int matchedFiles = 0;
    int changedFiles = 0;

    try (var files = Files.walk(directory)) {
      for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
        Path relativePath = directory.relativize(file);
        if (!matches(relativePath, includeMatchers) || matches(relativePath, excludeMatchers)) {
          continue;
        }

        matchedFiles++;
        String original = Files.readString(file, StandardCharsets.UTF_8);
        String minified = SqlMinifier.minify(original);
        if (!original.equals(minified)) {
          Files.writeString(file, minified, StandardCharsets.UTF_8);
          changedFiles++;
        }
      }
    }

    return new Result(matchedFiles, changedFiles);
  }

  private static List<PathMatcher> matchers(List<String> patterns) {
    return patterns.stream()
        .flatMap(pattern -> expandedPatterns(pattern).stream())
        .map(pattern -> FileSystems.getDefault().getPathMatcher("glob:" + pattern))
        .toList();
  }

  private static List<String> expandedPatterns(String pattern) {
    if (pattern.startsWith("**/")) {
      return List.of(pattern, pattern.substring(3));
    }
    return List.of(pattern);
  }

  private static boolean matches(Path relativePath, List<PathMatcher> matchers) {
    return matchers.stream().anyMatch(matcher -> matcher.matches(relativePath));
  }

  /** Summary of a SQL file minification run. */
  public record Result(int matchedFiles, int changedFiles) {}
}
