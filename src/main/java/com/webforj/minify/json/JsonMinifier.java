package com.webforj.minify.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webforj.minify.common.AssetMinifier;
import com.webforj.minify.common.MinificationException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * JSON minifier implementation using Jackson streaming API.
 *
 * <p>
 * This minifier removes unnecessary whitespace and formatting from JSON files while preserving
 * data integrity. It uses Jackson's streaming API (JsonParser and JsonGenerator) for
 * memory-efficient processing of large JSON files.
 * </p>
 *
 * <p>
 * By default, this minifier skips common configuration files that should remain human-readable:
 * </p>
 * <ul>
 * <li>package.json - NPM package configuration</li>
 * <li>tsconfig.json - TypeScript configuration</li>
 * <li>*.lock.json - Package lock files</li>
 * </ul>
 *
 * <p>
 * Additional files can be excluded using the webforj-minify.txt configuration file with glob
 * patterns.
 * </p>
 *
 * @author Kevin Hagel
 */
public class JsonMinifier implements AssetMinifier {

  private static final Logger logger = Logger.getLogger(JsonMinifier.class.getName());
  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Minifies JSON content by removing unnecessary whitespace.
   *
   * <p>
   * Uses Jackson's streaming API to process JSON token by token without loading the entire
   * document into memory. This approach is efficient for large JSON files.
   * </p>
   *
   * @param content the JSON content to minify
   * @param sourceFile the source file path (used for error reporting)
   * @return the minified JSON content
   * @throws MinificationException if minification fails due to I/O errors
   */
  @Override
  public String minify(String content, Path sourceFile) throws MinificationException {
    try {
      StringWriter writer = new StringWriter();
      JsonFactory factory = mapper.getFactory();

      try (JsonParser parser = factory.createParser(content);
          JsonGenerator generator = factory.createGenerator(writer)) {

        // Disable auto-close to prevent premature stream closure
        generator.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

        // Copy all tokens from parser to generator (removes formatting/whitespace)
        while (parser.nextToken() != null) {
          generator.copyCurrentEvent(parser);
        }
      }

      return writer.toString();
    } catch (JsonParseException e) {
      // Malformed JSON - return original content with warning
      logger.warning("Malformed JSON in " + sourceFile + ", skipping minification: "
          + e.getMessage());
      return content;
    } catch (Exception e) {
      throw new MinificationException("Failed to minify JSON file: " + sourceFile, e);
    }
  }

  /**
   * Returns the file extensions supported by this minifier.
   *
   * @return a set containing "json"
   */
  @Override
  public Set<String> getSupportedExtensions() {
    return Set.of("json");
  }

  /**
   * Determines whether a specific file should be minified.
   *
   * <p>
   * Skips common configuration files that should remain human-readable:
   * </p>
   * <ul>
   * <li>package.json - NPM dependencies and scripts</li>
   * <li>tsconfig.json - TypeScript compiler options</li>
   * <li>*-lock.json - Package lock files (package-lock.json, composer-lock.json, etc.)</li>
   * <li>*.lock.json - Alternative lock file pattern (dependencies.lock.json, etc.)</li>
   * </ul>
   *
   * @param filePath the path to the file being considered for minification
   * @return true if the file should be minified, false otherwise
   */
  @Override
  public boolean shouldMinify(Path filePath) {
    String filename = filePath.getFileName().toString();

    // Skip common configuration files
    if (filename.equals("package.json")) {
      return false;
    }

    if (filename.equals("tsconfig.json")) {
      return false;
    }

    // Skip lock files (package-lock.json, composer-lock.json, etc.)
    if (filename.endsWith("-lock.json") || filename.endsWith(".lock.json")) {
      return false;
    }

    return true;
  }

  /**
   * Configures the minifier with options from the plugin configuration.
   *
   * <p>
   * This minifier currently does not support configuration options. The method is provided for
   * future extensibility.
   * </p>
   *
   * @param options configuration options (currently unused)
   */
  @Override
  public void configure(Map<String, Object> options) {
    // No configuration options currently supported
    // Future options could include:
    // - prettyPrint: false (default) / true (for debugging)
    // - skipValidation: false (default) / true (for performance)
  }
}
