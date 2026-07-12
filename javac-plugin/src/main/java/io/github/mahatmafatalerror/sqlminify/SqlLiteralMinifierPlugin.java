package io.github.mahatmafatalerror.sqlminify;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Log;
import io.github.mahatmafatalerror.sqlminify.SqlMinifier.Dialect;
import java.io.IOException;

/** Java compiler plugin that minifies marked SQL text block literals before bytecode generation. */
public final class SqlLiteralMinifierPlugin implements Plugin {

  @Override
  public String getName() {
    return "SqlLiteralMinifier";
  }

  @Override
  public void init(JavacTask task, String... args) {
    BasicJavacTask basicTask = (BasicJavacTask) task;
    Trees trees = Trees.instance(task);
    Options options = Options.parse(args);
    Log log = Log.instance(basicTask.getContext());
    basicTask.addTaskListener(new MinifyingTaskListener(trees, log, options));
  }

  private static final class MinifyingTaskListener implements TaskListener {

    private final Trees trees;
    private final Log log;
    private final Options options;

    private MinifyingTaskListener(Trees trees, Log log, Options options) {
      this.trees = trees;
      this.log = log;
      this.options = options;
    }

    @Override
    public void finished(TaskEvent event) {
      if (event.getKind() != Kind.PARSE) {
        return;
      }

      CompilationUnitTree compilationUnit = event.getCompilationUnit();
      CharSequence source = source(compilationUnit);
      if (source == null) {
        return;
      }

      MinificationStats stats = new MinificationStats();
      ((JCTree) compilationUnit)
          .accept(new SqlLiteralTranslator(trees, compilationUnit, source, options, stats));
      if (stats.skipped() > 0) {
        log.printRawLines(
            Log.WriterKind.NOTICE,
            "SqlLiteralMinifier: skipped unsafe SQL text block(s): " + stats.skipped());
      }
      if (options.report() && stats.textBlocks() > 0) {
        log.printRawLines(
            Log.WriterKind.NOTICE,
            "SqlLiteralMinifier: minified "
                + stats.textBlocks()
                + " SQL text block(s), saved "
                + stats.savedCharacters()
                + " character(s)");
      }
    }

    private static CharSequence source(CompilationUnitTree compilationUnit) {
      try {
        return compilationUnit.getSourceFile().getCharContent(true);
      } catch (IOException exception) {
        return null;
      }
    }
  }

  private static final class SqlLiteralTranslator extends TreeTranslator {

    private final Trees trees;
    private final CompilationUnitTree compilationUnit;
    private final CharSequence source;
    private final Options options;
    private final MinificationStats stats;

    private SqlLiteralTranslator(
        Trees trees,
        CompilationUnitTree compilationUnit,
        CharSequence source,
        Options options,
        MinificationStats stats) {
      this.trees = trees;
      this.compilationUnit = compilationUnit;
      this.source = source;
      this.options = options;
      this.stats = stats;
    }

    @Override
    public void visitLiteral(JCLiteral literal) {
      super.visitLiteral(literal);
      if (!(literal.getValue() instanceof String sql)) {
        return;
      }

      int start = (int) trees.getSourcePositions().getStartPosition(compilationUnit, literal);
      if (start < 0 || start >= source.length()) {
        return;
      }

      if (!startsWithTextBlockDelimiter(start)) {
        return;
      }

      if (!LanguageSqlMarker.isMarked(source, start)) {
        return;
      }

      SqlMinifier.Minification minification = SqlMinifier.minifySafely(sql, options.dialect());
      literal.value = minification.sql();
      if (!minification.safe()) {
        stats.recordSkipped();
      } else if (!sql.equals(minification.sql())) {
        stats.record(sql, minification.sql());
      }
    }

    private boolean startsWithTextBlockDelimiter(int start) {
      return start + 2 < source.length()
          && source.charAt(start) == '"'
          && source.charAt(start + 1) == '"'
          && source.charAt(start + 2) == '"';
    }
  }

  private static final class MinificationStats {

    private int textBlocks;
    private int savedCharacters;
    private int skipped;

    private void record(String original, String minified) {
      textBlocks++;
      savedCharacters += original.length() - minified.length();
    }

    private void recordSkipped() {
      skipped++;
    }

    private int textBlocks() {
      return textBlocks;
    }

    private int savedCharacters() {
      return savedCharacters;
    }

    private int skipped() {
      return skipped;
    }
  }

  private record Options(Dialect dialect, boolean report) {

    private static Options parse(String... args) {
      Dialect dialect = Dialect.STANDARD;
      boolean report = false;
      for (String arg : args) {
        if ("dialect=postgres".equalsIgnoreCase(arg)) {
          dialect = Dialect.POSTGRES;
        } else if ("report".equalsIgnoreCase(arg)) {
          report = true;
        }
      }
      return new Options(dialect, report);
    }
  }
}
