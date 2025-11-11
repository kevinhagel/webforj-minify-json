# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

The webforJ JSON Minifier is a custom minifier implementation that demonstrates the extensibility of the [webforJ build-time asset minification plugin](https://github.com/webforj/webforj-plugins/tree/main/webforj-minify). This project serves as a reference implementation for developers who want to create their own custom minifiers.

## Purpose

This is a **demonstration/example project** that shows:
- How to implement the `AssetMinifier` interface
- How to use Jackson streaming API for memory-efficient JSON processing
- How to register a minifier using Java SPI (Service Provider Interface)
- How to create smart file filtering logic with `shouldMinify()`
- How to write comprehensive tests for a minifier
- How to package and distribute a custom minifier

## Architecture

### Key Components

**JsonMinifier.java** - Main implementation
- Implements `com.webforj.minify.common.AssetMinifier` interface
- Uses Jackson's `JsonParser` and `JsonGenerator` for streaming
- Processes JSON token-by-token without loading entire document into memory
- Handles files of any size efficiently

**SPI Registration** - Service discovery
- Location: `META-INF/services/com.webforj.minify.common.AssetMinifier`
- Contains: `com.webforj.minify.json.JsonMinifier`
- Enables automatic discovery by webforJ minification plugin

**Smart Defaults** - Built-in filtering logic
- Skips `package.json` (NPM configuration)
- Skips `tsconfig.json` (TypeScript configuration)
- Skips `*-lock.json` files (package-lock.json, composer-lock.json, etc.)
- Skips `*.lock.json` files (dependencies.lock.json, etc.)

### Interface Contract

All minifiers must implement:
```java
public interface AssetMinifier {
    String minify(String content, Path sourceFile) throws MinificationException;
    Set<String> getSupportedExtensions();
    default boolean shouldMinify(Path filePath) { return true; }
    default void configure(Map<String, Object> options) { }
}
```

## Development Workflow

### Building
```bash
mvn clean install
```

### Testing
```bash
mvn test
```

All tests must pass before committing.

### Code Style
- Follow Google Java Style Guide
- Use 2-space indentation
- Maximum line length: 100 characters
- All public methods must have Javadoc

## Testing Approach

The test suite (`JsonMinifierTest.java`) covers:
- Simple and nested JSON structures
- Array-based JSON documents
- Unicode and escaped characters
- Large files (1000+ records)
- Malformed JSON handling (should return original content)
- File filtering logic (`shouldMinify()`)
- Empty documents

**Important**: When malformed JSON is detected, the minifier logs a warning and returns the original content unchanged. This is by design - minification errors should not fail builds.

## Key Design Decisions

### 1. Streaming vs Tree Processing
**Decision**: Use Jackson streaming API (JsonParser/JsonGenerator)
**Rationale**:
- Handles files of any size without memory issues
- Processes token-by-token, never loads entire JSON into memory
- Faster than tree-based parsing for large files
- More appropriate for build-time processing

### 2. Error Handling Philosophy
**Decision**: Return original content on malformed JSON
**Rationale**:
- Build-time minification should not break builds
- Better to ship unminified than to fail
- Warning logged for developer awareness
- Consistent with webforJ minification plugin philosophy

### 3. File Filtering Strategy
**Decision**: Two-level control (code + configuration)
**Rationale**:
- **Code-level** (`shouldMinify()`): Skip files that should NEVER be minified (config files)
- **Pattern-level** (`webforj-minify.txt`): Let users control project-specific exclusions
- Provides sensible defaults while maintaining flexibility

### 4. Configuration Files to Skip
**Decision**: Skip package.json, tsconfig.json, and lock files
**Rationale**:
- These files must remain human-readable for tools/IDEs
- Minifying them breaks npm, TypeScript, and other tooling
- Lock files are auto-generated and should not be modified
- Users can override via `webforj-minify.txt` if needed

## Dependencies

**Runtime**:
- `jackson-core` (2.18.2) - JSON streaming
- `jackson-databind` (2.18.2) - ObjectMapper and factories

**Provided**:
- `webforj-minify-foundation` (25.10-SNAPSHOT) - Core interfaces
  - Scope: `provided` (already on classpath at runtime)

**Test**:
- `junit-jupiter` (5.11.4) - Testing framework

## Usage by Consumers

Users add this minifier as a **plugin dependency** (not a project dependency):

### Maven
```xml
<plugin>
  <groupId>com.webforj</groupId>
  <artifactId>webforj-minify-maven-plugin</artifactId>
  <version>25.10-SNAPSHOT</version>
  <dependencies>
    <dependency>
      <groupId>com.webforj</groupId>
      <artifactId>webforj-minify-json</artifactId>
      <version>1.0.0</version>
    </dependency>
  </dependencies>
</plugin>
```

### Gradle
```kotlin
dependencies {
    add("webforjMinifier", "com.webforj:webforj-minify-json:1.0.0")
}
```

## Extending This Implementation

If you want to add features:

### Add Configuration Options
```java
@Override
public void configure(Map<String, Object> options) {
    if (options.containsKey("prettyPrint")) {
        this.prettyPrint = Boolean.parseBoolean(options.get("prettyPrint").toString());
    }
}
```

### Add More File Filtering
```java
@Override
public boolean shouldMinify(Path filePath) {
    String filename = filePath.getFileName().toString();

    // Add your custom logic
    if (filename.startsWith("test-")) {
        return false;
    }

    return true;
}
```

### Support Additional Formats
To support JSONC (JSON with comments) or JSON5:
1. Add the appropriate Jackson module
2. Configure the JsonFactory
3. Update `getSupportedExtensions()` to return `["json", "jsonc", "json5"]`

## Common Issues

### Issue: Tests fail with "name clash" error
**Cause**: `configure()` method signature doesn't match interface
**Solution**: Use `Map<String, Object>`, not `Map<String, String>`

### Issue: Minifier not discovered by plugin
**Cause**: SPI file missing or incorrect
**Solution**: Verify `META-INF/services/com.webforj.minify.common.AssetMinifier` contains full class name

### Issue: Large files cause OutOfMemoryError
**Cause**: Not using streaming API correctly
**Solution**: Ensure using `JsonParser` and `JsonGenerator`, not tree-based parsing

## Repository Status

**Visibility**: Private (until webforj-minify plugins are publicly released)
**Purpose**: Internal example and reference implementation
**Sharing**: Add collaborators via GitHub Settings → Collaborators

When webforj-minify is publicly released, consider making this repository public to help the community create custom minifiers.

## Related Projects

- [webforj-minify](https://github.com/webforj/webforj-plugins/tree/main/webforj-minify) - Parent plugin architecture
- [webforj-minify-phcss-css](https://github.com/webforj/webforj-plugins/tree/main/webforj-minify/webforj-minify-phcss-css) - CSS minifier
- [webforj-minify-closure-js](https://github.com/webforj/webforj-plugins/tree/main/webforj-minify/webforj-minify-closure-js) - JavaScript minifier

## Contact

**Author**: Kevin Hagel (khagel@basis.cloud)
**Organization**: BASIS International
**License**: MIT
