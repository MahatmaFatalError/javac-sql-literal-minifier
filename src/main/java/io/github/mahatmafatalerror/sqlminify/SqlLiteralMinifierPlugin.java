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
    Trees trees = Trees.instance(task);
    Options options = Options.parse(args);
    ((BasicJavacTask) task).addTaskListener(new MinifyingTaskListener(trees, options));
  }

  private static final class MinifyingTaskListener implements TaskListener {

    private final Trees trees;
    private final Options options;

    private MinifyingTaskListener(Trees trees, Options options) {
      this.trees = trees;
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

      ((JCTree) compilationUnit)
          .accept(new SqlLiteralTranslator(trees, compilationUnit, source, options));
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

    private SqlLiteralTranslator(
        Trees trees, CompilationUnitTree compilationUnit, CharSequence source, Options options) {
      this.trees = trees;
      this.compilationUnit = compilationUnit;
      this.source = source;
      this.options = options;
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

      literal.value = SqlMinifier.minify(sql, options.dialect());
    }

    private boolean startsWithTextBlockDelimiter(int start) {
      return start + 2 < source.length()
          && source.charAt(start) == '"'
          && source.charAt(start + 1) == '"'
          && source.charAt(start + 2) == '"';
    }
  }

  private record Options(Dialect dialect) {

    private static Options parse(String... args) {
      Dialect dialect = Dialect.STANDARD;
      for (String arg : args) {
        if ("dialect=postgres".equalsIgnoreCase(arg)) {
          dialect = Dialect.POSTGRES;
        }
      }
      return new Options(dialect);
    }
  }
}
