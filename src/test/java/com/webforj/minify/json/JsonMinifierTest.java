package com.webforj.minify.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webforj.minify.common.MinificationException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for JsonMinifier.
 *
 * @author Kevin Hagel
 */
class JsonMinifierTest {

  private JsonMinifier minifier;
  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    minifier = new JsonMinifier();
    mapper = new ObjectMapper();
  }

  @Test
  void testGetSupportedExtensions() {
    Set<String> extensions = minifier.getSupportedExtensions();
    assertEquals(1, extensions.size());
    assertTrue(extensions.contains("json"));
  }

  @Test
  void testMinifySimpleJson() throws Exception {
    String input = """
        {
          "name": "test",
          "value": 42,
          "active": true
        }
        """;

    Path sourceFile = Paths.get("test.json");
    String result = minifier.minify(input, sourceFile);

    // Verify the result is valid JSON
    JsonNode original = mapper.readTree(input);
    JsonNode minified = mapper.readTree(result);
    assertEquals(original, minified);

    // Verify size reduction
    assertTrue(result.length() < input.length());

    // Verify no unnecessary whitespace
    assertFalse(result.contains("  "));
    assertFalse(result.contains("\n"));
  }

  @Test
  void testMinifyNestedJson() throws Exception {
    String input = """
        {
          "user": {
            "name": "John Doe",
            "age": 30,
            "address": {
              "street": "123 Main St",
              "city": "Anytown"
            }
          },
          "items": [
            {"id": 1, "name": "Item 1"},
            {"id": 2, "name": "Item 2"}
          ]
        }
        """;

    Path sourceFile = Paths.get("nested.json");
    String result = minifier.minify(input, sourceFile);

    // Verify the result is valid JSON with same structure
    JsonNode original = mapper.readTree(input);
    JsonNode minified = mapper.readTree(result);
    assertEquals(original, minified);

    // Verify size reduction
    assertTrue(result.length() < input.length());
  }

  @Test
  void testMinifyArrayJson() throws Exception {
    String input = """
        [
          {
            "id": 1,
            "name": "Alice"
          },
          {
            "id": 2,
            "name": "Bob"
          },
          {
            "id": 3,
            "name": "Charlie"
          }
        ]
        """;

    Path sourceFile = Paths.get("array.json");
    String result = minifier.minify(input, sourceFile);

    // Verify the result is valid JSON
    JsonNode original = mapper.readTree(input);
    JsonNode minified = mapper.readTree(result);
    assertEquals(original, minified);

    // Verify size reduction
    assertTrue(result.length() < input.length());
  }

  @Test
  void testMinifyWithUnicodeCharacters() throws Exception {
    String input = """
        {
          "greeting": "Hello, 世界!",
          "emoji": "😀",
          "special": "Ñoño"
        }
        """;

    Path sourceFile = Paths.get("unicode.json");
    String result = minifier.minify(input, sourceFile);

    // Verify the result is valid JSON
    JsonNode original = mapper.readTree(input);
    JsonNode minified = mapper.readTree(result);
    assertEquals(original, minified);

    // Verify Unicode characters are preserved
    assertTrue(result.contains("世界"));
    assertTrue(result.contains("😀"));
    assertTrue(result.contains("Ñoño"));
  }

  @Test
  void testMinifyWithEscapedCharacters() throws Exception {
    String input = """
        {
          "quote": "She said \\"Hello\\"",
          "newline": "Line 1\\nLine 2",
          "tab": "Column1\\tColumn2"
        }
        """;

    Path sourceFile = Paths.get("escaped.json");
    String result = minifier.minify(input, sourceFile);

    // Verify the result is valid JSON
    JsonNode original = mapper.readTree(input);
    JsonNode minified = mapper.readTree(result);
    assertEquals(original, minified);
  }

  @Test
  void testMinifyMalformedJson() throws Exception {
    String malformedJson = """
        {
          "name": "test",
          "value": 42,
          "unclosed": [
        """;

    Path sourceFile = Paths.get("malformed.json");
    String result = minifier.minify(malformedJson, sourceFile);

    // Should return original content for malformed JSON
    assertEquals(malformedJson, result);
  }

  @Test
  void testMinifyEmptyJson() throws Exception {
    String input = "{}";

    Path sourceFile = Paths.get("empty.json");
    String result = minifier.minify(input, sourceFile);

    // Verify the result is valid JSON
    assertNotNull(result);
    assertEquals("{}", result);
  }

  @Test
  void testMinifyEmptyArray() throws Exception {
    String input = "[]";

    Path sourceFile = Paths.get("empty-array.json");
    String result = minifier.minify(input, sourceFile);

    // Verify the result is valid JSON
    assertNotNull(result);
    assertEquals("[]", result);
  }

  @Test
  void testShouldMinifyDataFiles() {
    // Data files should be minified
    assertTrue(minifier.shouldMinify(Paths.get("data/users.json")));
    assertTrue(minifier.shouldMinify(Paths.get("api/response.json")));
    assertTrue(minifier.shouldMinify(Paths.get("config/settings.json")));
  }

  @Test
  void testShouldSkipPackageJson() {
    // package.json should NOT be minified (NPM configuration)
    assertFalse(minifier.shouldMinify(Paths.get("package.json")));
    assertFalse(minifier.shouldMinify(Paths.get("frontend/package.json")));
  }

  @Test
  void testShouldSkipTsconfigJson() {
    // tsconfig.json should NOT be minified (TypeScript configuration)
    assertFalse(minifier.shouldMinify(Paths.get("tsconfig.json")));
    assertFalse(minifier.shouldMinify(Paths.get("frontend/tsconfig.json")));
  }

  @Test
  void testShouldSkipLockFiles() {
    // Lock files should NOT be minified
    assertFalse(minifier.shouldMinify(Paths.get("package-lock.json")));
    assertFalse(minifier.shouldMinify(Paths.get("composer.lock.json")));
    assertFalse(minifier.shouldMinify(Paths.get("dependencies.lock.json")));
  }

  @Test
  void testMinifyLargeJson() throws Exception {
    // Generate a large JSON structure
    StringBuilder sb = new StringBuilder();
    sb.append("[\n");
    for (int i = 0; i < 1000; i++) {
      sb.append("  {\n");
      sb.append("    \"id\": ").append(i).append(",\n");
      sb.append("    \"name\": \"User ").append(i).append("\",\n");
      sb.append("    \"email\": \"user").append(i).append("@example.com\",\n");
      sb.append("    \"active\": ").append(i % 2 == 0).append("\n");
      sb.append("  }");
      if (i < 999) {
        sb.append(",");
      }
      sb.append("\n");
    }
    sb.append("]");

    String input = sb.toString();
    Path sourceFile = Paths.get("large.json");
    String result = minifier.minify(input, sourceFile);

    // Verify the result is valid JSON
    JsonNode original = mapper.readTree(input);
    JsonNode minified = mapper.readTree(result);
    assertEquals(original, minified);

    // Verify significant size reduction
    assertTrue(result.length() < input.length());
    assertTrue(input.length() - result.length() > 1000); // At least 1KB saved
  }

  @Test
  void testConfigureMethodDoesNotThrow() {
    // configure() should accept options gracefully (even if unused)
    minifier.configure(null);
    minifier.configure(java.util.Collections.emptyMap());
    minifier.configure(java.util.Map.of("prettyPrint", "false"));
  }
}
