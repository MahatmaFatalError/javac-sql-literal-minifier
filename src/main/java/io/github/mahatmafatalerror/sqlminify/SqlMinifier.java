package io.github.mahatmafatalerror.sqlminify;

/** Removes SQL comments and collapses SQL whitespace outside quoted text. */
public final class SqlMinifier {

  private SqlMinifier() {}

  /** Returns a minified SQL string while preserving quoted string and identifier contents. */
  public static String minify(String sql) {
    String withoutComments = removeComments(sql);
    return collapseWhitespace(withoutComments);
  }

  private static String removeComments(String sql) {
    StringBuilder result = new StringBuilder(sql.length());
    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;

    for (int i = 0; i < sql.length(); i++) {
      char current = sql.charAt(i);
      char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

      if (inSingleQuote) {
        result.append(current);
        if (current == '\'' && next == '\'') {
          result.append(next);
          i++;
        } else if (current == '\'') {
          inSingleQuote = false;
        }
        continue;
      }

      if (inDoubleQuote) {
        result.append(current);
        if (current == '"' && next == '"') {
          result.append(next);
          i++;
        } else if (current == '"') {
          inDoubleQuote = false;
        }
        continue;
      }

      if (current == '\'') {
        inSingleQuote = true;
        result.append(current);
      } else if (current == '"') {
        inDoubleQuote = true;
        result.append(current);
      } else if (current == '-' && next == '-') {
        i = skipLineComment(sql, i + 2);
      } else if (current == '/' && next == '*') {
        i = skipBlockComment(sql, i + 2);
      } else {
        result.append(current);
      }
    }

    return result.toString();
  }

  private static int skipLineComment(String sql, int index) {
    int i = index;
    while (i < sql.length() && sql.charAt(i) != '\n' && sql.charAt(i) != '\r') {
      i++;
    }
    return i - 1;
  }

  private static int skipBlockComment(String sql, int index) {
    int i = index;
    while (i + 1 < sql.length()) {
      if (sql.charAt(i) == '*' && sql.charAt(i + 1) == '/') {
        return i + 1;
      }
      i++;
    }
    return sql.length() - 1;
  }

  private static String collapseWhitespace(String sql) {
    StringBuilder result = new StringBuilder(sql.length());
    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;
    boolean pendingWhitespace = false;

    for (int i = 0; i < sql.length(); i++) {
      char current = sql.charAt(i);
      char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

      if (inSingleQuote) {
        result.append(current);
        if (current == '\'' && next == '\'') {
          result.append(next);
          i++;
        } else if (current == '\'') {
          inSingleQuote = false;
        }
        continue;
      }

      if (inDoubleQuote) {
        result.append(current);
        if (current == '"' && next == '"') {
          result.append(next);
          i++;
        } else if (current == '"') {
          inDoubleQuote = false;
        }
        continue;
      }

      if (Character.isWhitespace(current)) {
        pendingWhitespace = result.length() > 0;
      } else {
        if (pendingWhitespace) {
          result.append(' ');
          pendingWhitespace = false;
        }
        result.append(current);
        if (current == '\'') {
          inSingleQuote = true;
        } else if (current == '"') {
          inDoubleQuote = true;
        }
      }
    }

    int length = result.length();
    if (length > 0 && result.charAt(length - 1) == ' ') {
      result.setLength(length - 1);
    }
    return result.toString();
  }
}
