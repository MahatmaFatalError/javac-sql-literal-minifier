package io.github.mahatmafatalerror.sqlminify;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SqlMinifierTest {

  @Test
  void removesLineCommentsAndCollapsesWhitespace() {
    String sql =
        """
        SELECT *
        FROM users -- only active users
        WHERE active = true
        """;

    assertEquals("SELECT * FROM users WHERE active=true", SqlMinifier.minify(sql));
  }

  @Test
  void removesBlockComments() {
    String sql =
        """
        SELECT /* internal note */ id
        FROM users
        """;

    assertEquals("SELECT id FROM users", SqlMinifier.minify(sql));
  }

  @Test
  void preservesCommentMarkersInsideSingleQuotedStrings() {
    String sql =
        """
        SELECT '-- not a comment', '/* also not a comment */'
        FROM messages
        """;

    assertEquals(
        "SELECT '-- not a comment','/* also not a comment */' FROM messages",
        SqlMinifier.minify(sql));
  }

  @Test
  void preservesEscapedSingleQuotes() {
    String sql =
        """
        SELECT 'it''s fine' -- real comment
        FROM dual
        """;

    assertEquals("SELECT 'it''s fine' FROM dual", SqlMinifier.minify(sql));
  }

  @Test
  void preservesDoubleQuotedIdentifiers() {
    String sql =
        """
        SELECT "weird -- column"
        FROM "table /* name */"
        """;

    assertEquals("SELECT \"weird -- column\" FROM \"table /* name */\"", SqlMinifier.minify(sql));
  }

  @Test
  void postgresDialectPreservesCommentMarkersInsideDollarQuotedStrings() {
    String sql =
        """
        SELECT $$-- not a comment/* neither */$$, $tag$/* keep */$tag$
        FROM messages -- real comment
        """;

    assertEquals(
        "SELECT $$-- not a comment/* neither */$$,$tag$/* keep */$tag$ FROM messages",
        SqlMinifier.minify(sql, SqlMinifier.Dialect.POSTGRES));
  }

  @Test
  void removesWhitespaceAroundSafePunctuationAndComparisonOperators() {
    String sql =
        """
        SELECT id , name
        FROM users
        WHERE id = ?
          AND status IN ( ? , ? )
          AND score >= 10
          AND deleted <> true
        """;

    assertEquals(
        "SELECT id,name FROM users WHERE id=? AND status IN(?,?) AND score>=10 AND deleted<>true",
        SqlMinifier.minify(sql));
  }
}
