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

  @Test
  void leavesSqlUnchangedWhenBlockCommentIsUnclosed() {
    String sql =
        """
        SELECT 1 /* keep this fragment
        FROM dual
        """;

    assertEquals(sql, SqlMinifier.minify(sql));
  }

  @Test
  void leavesPostgresSqlUnchangedWhenDollarQuotedStringIsUnclosed() {
    String sql =
        """
        SELECT $$-- not a comment
        FROM messages
        """;

    assertEquals(sql, SqlMinifier.minify(sql, SqlMinifier.Dialect.POSTGRES));
  }

  @Test
  void handlesCrLfLineComments() {
    String sql = "SELECT 1 -- comment\r\nFROM dual\r\nWHERE id = ?";

    assertEquals("SELECT 1 FROM dual WHERE id=?", SqlMinifier.minify(sql));
  }

  @Test
  void handlesCrOnlyLineComments() {
    String sql = "SELECT 1 -- comment\rFROM dual\rWHERE enabled != false";

    assertEquals("SELECT 1 FROM dual WHERE enabled!=false", SqlMinifier.minify(sql));
  }

  @Test
  void removesBlockCommentsAtEndOfSql() {
    String sql = "SELECT 1 /* trailing block comment */";

    assertEquals("SELECT 1", SqlMinifier.minify(sql));
  }

  @Test
  void postgresDialectPreservesTaggedDollarQuotedFunctionBody() {
    String sql =
        """
        SELECT $function$
        BEGIN
          -- keep this comment inside the function body
          RETURN '/* not a comment */';
        END
        $function$ -- remove this comment
        """;

    assertEquals(
        "SELECT $function$\n"
            + "BEGIN\n"
            + "  -- keep this comment inside the function body\n"
            + "  RETURN '/* not a comment */';\n"
            + "END\n"
            + "$function$",
        SqlMinifier.minify(sql, SqlMinifier.Dialect.POSTGRES));
  }

  @Test
  void leavesSqlUnchangedWhenSingleQuotedStringIsUnclosed() {
    String sql =
        """
        SELECT 'unterminated -- keep this fragment
        FROM dual
        """;

    assertEquals(sql, SqlMinifier.minify(sql));
  }

  @Test
  void leavesSqlUnchangedWhenDoubleQuotedIdentifierIsUnclosed() {
    String sql =
        """
        SELECT "unterminated -- keep this fragment
        FROM dual
        """;

    assertEquals(sql, SqlMinifier.minify(sql));
  }

  @Test
  void preservesEscapedDoubleQuotesInsideIdentifiers() {
    String sql =
        """
        SELECT "a "" -- not a comment"
        FROM "table "" /* not a comment */"
        """;

    assertEquals(
        "SELECT \"a \"\" -- not a comment\" FROM \"table \"\" /* not a comment */\"",
        SqlMinifier.minify(sql));
  }

  @Test
  void treatsInvalidPostgresDollarDelimiterAsOrdinarySql() {
    String sql =
        """
        SELECT $bad-tag$ -- remove this comment
        FROM messages
        """;

    assertEquals(
        "SELECT $bad-tag$ FROM messages", SqlMinifier.minify(sql, SqlMinifier.Dialect.POSTGRES));
  }

  @Test
  void preservesPostgresDollarSignInStandardDialect() {
    String sql =
        """
        SELECT $$not a postgres string in standard dialect$$ -- remove this comment
        FROM messages
        """;

    assertEquals(
        "SELECT $$not a postgres string in standard dialect$$ FROM messages",
        SqlMinifier.minify(sql));
  }

  @Test
  void postgresDialectPreservesDollarQuotedStringAfterTokenWhitespace() {
    String sql =
        """
        SELECT $$value with -- comment marker$$
        """;

    assertEquals(
        "SELECT $$value with -- comment marker$$",
        SqlMinifier.minify(sql, SqlMinifier.Dialect.POSTGRES));
  }

  @Test
  void removesWhitespaceAroundLessThanOrEqualOperator() {
    String sql =
        """
        SELECT *
        FROM scores
        WHERE score <= 10
        """;

    assertEquals("SELECT * FROM scores WHERE score<=10", SqlMinifier.minify(sql));
  }

  @Test
  void returnsEmptyStringForWhitespaceOnlySql() {
    assertEquals("", SqlMinifier.minify("   \n \t  "));
  }

  @Test
  void preservesTrailingSpaceInsideQuotedText() {
    assertEquals("SELECT 'kept '", SqlMinifier.minify("SELECT 'kept '   "));
  }
}
