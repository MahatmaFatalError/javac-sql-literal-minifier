package io.github.mahatmafatalerror.sqlminify;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LanguageSqlMarkerTest {

  @Test
  void detectsMarkerOnPreviousSourceSlice() {
    String source =
        """
        class Test {
          String sql = //language=sql
              \"""
              SELECT 1
              \""";
        }
        """;

    assertTrue(LanguageSqlMarker.isMarked(source, source.indexOf("\"\"\"")));
  }

  @Test
  void detectsJetbrainsMarkerWithSpaceAfterCommentSlashes() {
    String source =
        """
        class Test {
          // language=SQL
          String sql = \"""
              SELECT 1
              \""";
        }
        """;

    assertTrue(LanguageSqlMarker.isMarked(source, source.indexOf("\"\"\"")));
  }

  @Test
  void detectsJetbrainsMarkerWithWhitespaceAroundEquals() {
    String source =
        """
        class Test {
          // language = SQL
          String sql = \"""
              SELECT 1
              \""";
        }
        """;

    assertTrue(LanguageSqlMarker.isMarked(source, source.indexOf("\"\"\"")));
  }

  @Test
  void detectsJetbrainsMarkerWithPrefixAndSuffixOptions() {
    String source =
        """
        class Test {
          // language=SQL prefix=SELECT * FROM users WHERE suffix=ORDER BY id
          String sql = \"""
              active = true
              \""";
        }
        """;

    assertTrue(LanguageSqlMarker.isMarked(source, source.indexOf("\"\"\"")));
  }

  @Test
  void detectsMarkerOnSameLinePrefix() {
    String source =
        """
        class Test {
          String sql = //language=sql \"""
              SELECT 1
              \""";
        }
        """;

    assertTrue(LanguageSqlMarker.isMarked(source, source.indexOf("\"\"\"")));
  }

  @Test
  void ignoresUnmarkedTextBlock() {
    String source =
        """
        class Test {
          String sql =
              \"""
              SELECT 1
              \""";
        }
        """;

    assertFalse(LanguageSqlMarker.isMarked(source, source.indexOf("\"\"\"")));
  }

  @Test
  void ignoresMarkerAfterBlankLine() {
    String source =
        """
        class Test {
          String sql = //language=sql

              \"""
              SELECT 1
              \""";
        }
        """;

    assertFalse(LanguageSqlMarker.isMarked(source, source.indexOf("\"\"\"")));
  }

  @Test
  void ignoresDifferentLanguageMarker() {
    String source =
        """
        class Test {
          String json = //language=json
              \"""
              {"x": 1}
              \""";
        }
        """;

    assertFalse(LanguageSqlMarker.isMarked(source, source.indexOf("\"\"\"")));
  }

  @Test
  void ignoresLanguageIdThatOnlyStartsWithSql() {
    String source =
        """
        class Test {
          // language=sqlx
          String sql = \"""
              SELECT 1
              \""";
        }
        """;

    assertFalse(LanguageSqlMarker.isMarked(source, source.indexOf("\"\"\"")));
  }
}
