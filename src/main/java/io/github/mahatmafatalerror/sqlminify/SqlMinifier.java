package io.github.mahatmafatalerror.sqlminify;

/** Removes SQL comments and collapses SQL whitespace outside quoted text. */
public final class SqlMinifier {

  /** SQL dialects with literal forms that affect safe minification. */
  public enum Dialect {
    STANDARD,
    POSTGRES
  }

  private SqlMinifier() {}

  /** Returns a minified SQL string while preserving quoted string and identifier contents. */
  public static String minify(String sql) {
    return minify(sql, Dialect.STANDARD);
  }

  /** Returns a minified SQL string using dialect-specific literal handling. */
  public static String minify(String sql, Dialect dialect) {
    String withoutComments = removeComments(sql, dialect);
    return collapseWhitespace(withoutComments, dialect);
  }

  private static String removeComments(String sql, Dialect dialect) {
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

      int dollarQuotedStringEnd = dollarQuotedStringEnd(sql, i, dialect);
      if (dollarQuotedStringEnd >= 0) {
        result.append(sql, i, dollarQuotedStringEnd);
        i = dollarQuotedStringEnd - 1;
      } else if (current == '\'') {
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

  private static int dollarQuotedStringEnd(String sql, int start, Dialect dialect) {
    if (dialect != Dialect.POSTGRES || sql.charAt(start) != '$') {
      return -1;
    }

    int delimiterEnd = dollarQuoteDelimiterEnd(sql, start);
    if (delimiterEnd < 0) {
      return -1;
    }

    String delimiter = sql.substring(start, delimiterEnd);
    int end = sql.indexOf(delimiter, delimiterEnd);
    return end < 0 ? -1 : end + delimiter.length();
  }

  private static int dollarQuoteDelimiterEnd(String sql, int start) {
    int i = start + 1;
    while (i < sql.length() && sql.charAt(i) != '$') {
      char current = sql.charAt(i);
      if (!Character.isLetterOrDigit(current) && current != '_') {
        return -1;
      }
      i++;
    }
    return i < sql.length() ? i + 1 : -1;
  }

  private static String collapseWhitespace(String sql, Dialect dialect) {
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

      int dollarQuotedStringEnd = dollarQuotedStringEnd(sql, i, dialect);
      if (dollarQuotedStringEnd >= 0) {
        if (pendingWhitespace) {
          result.append(' ');
          pendingWhitespace = false;
        }
        result.append(sql, i, dollarQuotedStringEnd);
        i = dollarQuotedStringEnd - 1;
      } else if (Character.isWhitespace(current)) {
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
