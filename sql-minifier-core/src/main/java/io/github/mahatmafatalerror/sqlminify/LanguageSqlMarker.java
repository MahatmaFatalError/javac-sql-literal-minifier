package io.github.mahatmafatalerror.sqlminify;

import java.util.Locale;

final class LanguageSqlMarker {

  private LanguageSqlMarker() {}

  static boolean isMarked(CharSequence source, int literalStart) {
    int lineStart = lineStart(source, literalStart);
    String sameLinePrefix = source.subSequence(lineStart, literalStart).toString();
    if (containsSqlLanguageMarker(sameLinePrefix)) {
      return true;
    }

    int previousLineEnd = previousLineEnd(source, lineStart);
    if (previousLineEnd < 0) {
      return false;
    }

    int previousLineStart = lineStart(source, previousLineEnd + 1);
    String previousLine = source.subSequence(previousLineStart, previousLineEnd + 1).toString();
    return containsSqlLanguageMarker(previousLine) && !previousLine.isBlank();
  }

  private static boolean containsSqlLanguageMarker(String line) {
    int commentStart = line.indexOf("//");
    if (commentStart < 0) {
      return false;
    }

    String comment = line.substring(commentStart + 2).stripLeading();
    String lowerCaseComment = comment.toLowerCase(Locale.ROOT);
    if (!lowerCaseComment.startsWith("language")) {
      return false;
    }

    int index = "language".length();
    index = skipWhitespace(lowerCaseComment, index);
    if (index >= lowerCaseComment.length() || lowerCaseComment.charAt(index) != '=') {
      return false;
    }

    index = skipWhitespace(lowerCaseComment, index + 1);
    return lowerCaseComment.startsWith("sql", index)
        && isLanguageIdEnd(lowerCaseComment, index + 3);
  }

  private static int skipWhitespace(String text, int index) {
    int i = index;
    while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
      i++;
    }
    return i;
  }

  private static boolean isLanguageIdEnd(String text, int index) {
    return index >= text.length() || !Character.isLetterOrDigit(text.charAt(index));
  }

  private static int previousLineEnd(CharSequence source, int lineStart) {
    int previousLineEnd = lineStart - 1;
    if (previousLineEnd >= 0 && source.charAt(previousLineEnd) == '\n') {
      previousLineEnd--;
    }
    if (previousLineEnd >= 0 && source.charAt(previousLineEnd) == '\r') {
      previousLineEnd--;
    }
    return previousLineEnd;
  }

  private static int lineStart(CharSequence source, int index) {
    int i = Math.max(0, index - 1);
    while (i >= 0) {
      char current = source.charAt(i);
      if (current == '\n' || current == '\r') {
        return i + 1;
      }
      i--;
    }
    return 0;
  }
}
