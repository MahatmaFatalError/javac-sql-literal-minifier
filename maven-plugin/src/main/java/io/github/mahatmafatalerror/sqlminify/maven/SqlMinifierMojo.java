package io.github.mahatmafatalerror.sqlminify.maven;

import io.github.mahatmafatalerror.sqlminify.SqlFileMinifier;
import io.github.mahatmafatalerror.sqlminify.SqlMinifier;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/** Minifies SQL resource files in the Maven output directory. */
@Mojo(name = "resources", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true)
public final class SqlMinifierMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
  File outputDirectory;

  @Parameter List<String> includes = List.of("**/*.sql", "*.sql");

  @Parameter List<String> excludes = List.of();

  @Parameter(property = "sqlMinifier.skip", defaultValue = "false")
  boolean skip;

  @Parameter(property = "sqlMinifier.dialect", defaultValue = "standard")
  String dialect = "standard";

  @Override
  public void execute() throws MojoExecutionException {
    if (skip) {
      getLog().info("Skipping SQL resource minification.");
      return;
    }

    try {
      SqlFileMinifier.Result result =
          SqlFileMinifier.minifyDirectory(
              outputDirectory.toPath(), includes, excludes, parseDialect(dialect));
      getLog()
          .info(
              "Minified "
                  + result.changedFiles()
                  + " of "
                  + result.matchedFiles()
                  + " SQL resource files.");
    } catch (IOException exception) {
      throw new MojoExecutionException("Failed to minify SQL resources.", exception);
    }
  }

  private static SqlMinifier.Dialect parseDialect(String dialect) throws MojoExecutionException {
    String configuredDialect = dialect == null ? "standard" : dialect;
    try {
      return SqlMinifier.Dialect.valueOf(configuredDialect.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new MojoExecutionException("Unsupported SQL dialect: " + configuredDialect, exception);
    }
  }
}
