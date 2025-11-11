# webforJ JSON Minifier

A JSON minifier implementation for the [webforJ build-time asset minification plugin](https://github.com/webforj/webforj-plugins/tree/main/webforj-minify).

This project demonstrates how to create custom minifier implementations using the webforJ minification plugin's extensible architecture based on Java's Service Provider Interface (SPI).

## Features

- **Memory-efficient**: Uses Jackson streaming API (JsonParser/JsonGenerator) for token-by-token processing
- **Large file support**: Can handle JSON files of any size without loading entire document into memory
- **Smart defaults**: Automatically skips configuration files (package.json, tsconfig.json, *.lock.json)
- **Configurable**: Use webforj-minify.txt to control which files are processed
- **Safe**: Malformed JSON files are left unchanged with a warning
- **Fast**: Minimal overhead, suitable for large builds

## Requirements

- Java 17 or higher
- Maven 3.6+ or Gradle 7.6+
- webforj-minify-foundation 25.10 or higher

## Prerequisites

Before using this JSON minifier, you must have the webforJ minification plugin installed in your local Maven repository:

```bash
# Clone and build the webforJ plugins project
git clone https://github.com/webforj/webforj-plugins.git
cd webforj-plugins/webforj-minify

# Install to local Maven repository
mvn clean install

# This installs:
# - webforj-minify-foundation (required dependency)
# - webforj-minify-maven-plugin (Maven plugin)
# - webforj-minify-gradle-plugin (Gradle plugin)
# - webforj-minify-phcss-css (optional CSS minifier)
# - webforj-minify-closure-js (optional JS minifier)
```

Once the webforJ minification plugins are installed, you can build and install this JSON minifier.

## Installation

### Maven

Add the minifier as a **plugin dependency** (not a project dependency):

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.webforj</groupId>
      <artifactId>webforj-minify-maven-plugin</artifactId>
      <version>25.10-SNAPSHOT</version>
      <executions>
        <execution>
          <goals>
            <goal>minify</goal>
          </goals>
        </execution>
      </executions>
      <dependencies>
        <!-- Add JSON minifier as plugin dependency -->
        <dependency>
          <groupId>com.webforj</groupId>
          <artifactId>webforj-minify-json</artifactId>
          <version>1.0.0</version>
        </dependency>
        <!-- Optional: Add other minifiers -->
        <dependency>
          <groupId>com.webforj</groupId>
          <artifactId>webforj-minify-phcss-css</artifactId>
          <version>25.10-SNAPSHOT</version>
        </dependency>
        <dependency>
          <groupId>com.webforj</groupId>
          <artifactId>webforj-minify-closure-js</artifactId>
          <version>25.10-SNAPSHOT</version>
        </dependency>
      </dependencies>
    </plugin>
  </plugins>
</build>
```

### Gradle (Kotlin DSL)

```kotlin
plugins {
    id("com.webforj.minify") version "25.10-SNAPSHOT"
}

dependencies {
    // Add annotation processor
    compileOnly("com.webforj:webforj-minify-foundation:25.10-SNAPSHOT")

    // Add JSON minifier using custom configuration
    add("webforjMinifier", "com.webforj:webforj-minify-json:1.0.0")

    // Optional: Add other minifiers
    add("webforjMinifier", "com.webforj:webforj-minify-phcss-css:25.10-SNAPSHOT")
    add("webforjMinifier", "com.webforj:webforj-minify-closure-js:25.10-SNAPSHOT")
}

webforjMinify {
    skip.set(false)
}
```

### Gradle (Groovy DSL)

```groovy
plugins {
    id 'com.webforj.minify' version '25.10-SNAPSHOT'
}

dependencies {
    // Add annotation processor
    compileOnly 'com.webforj:webforj-minify-foundation:25.10-SNAPSHOT'

    // Add JSON minifier using custom configuration
    add 'webforjMinifier', 'com.webforj:webforj-minify-json:1.0.0'

    // Optional: Add other minifiers
    add 'webforjMinifier', 'com.webforj:webforj-minify-phcss-css:25.10-SNAPSHOT'
    add 'webforjMinifier', 'com.webforj:webforj-minify-closure-js:25.10-SNAPSHOT'
}

webforjMinify {
    enabled = true
}
```

## Usage

Once installed, the JSON minifier will automatically process `.json` files during your build:

- **Maven**: Runs during `process-classes` phase (before WAR packaging)
- **Gradle**: Runs after `processResources` task, before `classes`

### Controlling Which Files Are Minified

The minifier uses two levels of control:

#### 1. Built-in Logic (Code-Level)

By default, the minifier automatically skips:
- `package.json` - NPM package configuration
- `tsconfig.json` - TypeScript configuration
- `*.lock.json` - Package lock files

All other `.json` files will be minified.

#### 2. Pattern-Based Control (Configuration File)

Create `src/main/resources/META-INF/webforj-minify.txt` to specify additional patterns:

```
# Include patterns - minify these JSON files
static/data/**/*.json
api/responses/*.json

# Exclude patterns - skip minification
!static/data/config/*.json
!**/*-config.json
```

**Pattern syntax:**
- `**` matches any number of directories
- `*` matches any characters within a directory or filename
- `!` prefix excludes files from minification
- Lines starting with `#` are comments

### Examples

**Example 1: Minify all data files except configurations**
```
# Include all JSON in data directory
static/data/**/*.json

# Except configuration files
!static/data/config/*.json
```

**Example 2: Minify API responses only**
```
api/responses/*.json
```

**Example 3: Exclude all test fixtures**
```
# Include all JSON
**/*.json

# Except test fixtures
!**/fixtures/*.json
!**/test-data/*.json
```

## How It Works

1. **Discovery**: The webforJ minification plugin uses Java SPI to discover minifiers at runtime
2. **Registration**: This JSON minifier registers itself via `META-INF/services/com.webforj.minify.common.AssetMinifier`
3. **Processing**: During build, the plugin:
   - Collects all `.json` files from your project
   - Checks `shouldMinify()` to skip configuration files
   - Applies patterns from `webforj-minify.txt` (if present)
   - Passes each file to `minify()` for processing
   - Replaces original files with minified versions

### Streaming Architecture

The minifier uses Jackson's streaming API for memory efficiency:

```java
JsonFactory factory = new ObjectMapper().getFactory();

try (JsonParser parser = factory.createParser(content);
     JsonGenerator generator = factory.createGenerator(writer)) {

    // Copy token by token (removes whitespace)
    while (parser.nextToken() != null) {
        generator.copyCurrentEvent(parser);
    }
}
```

This approach:
- Processes files token-by-token without loading entire JSON into memory
- Handles files of any size efficiently
- Preserves data integrity while removing formatting

## Building from Source

```bash
# Clone the repository
git clone https://github.com/kevinhagel/webforj-minify-json.git
cd webforj-minify-json

# Build and install to local Maven repository
mvn clean install
```

## Testing

Run the test suite:

```bash
mvn test
```

The test suite includes:
- Simple and nested JSON structures
- Large JSON files (1000+ records)
- Unicode and escaped characters
- Malformed JSON handling
- File filtering logic

## Creating Your Own Minifier

This project serves as a reference implementation. To create your own custom minifier:

1. **Create a new Maven project** with dependency on `webforj-minify-foundation`

2. **Implement the AssetMinifier interface:**
   ```java
   public class MyCustomMinifier implements AssetMinifier {
       @Override
       public String minify(String content, Path sourceFile) throws MinificationException {
           // Your minification logic
       }

       @Override
       public Set<String> getSupportedExtensions() {
           return Set.of("xml", "svg");  // Your file extensions
       }

       @Override
       public boolean shouldMinify(Path filePath) {
           // Custom logic to skip certain files
       }

       @Override
       public void configure(Map<String, String> options) {
           // Handle configuration from pom.xml/build.gradle
       }
   }
   ```

3. **Register via SPI** - Create `META-INF/services/com.webforj.minify.common.AssetMinifier`:
   ```
   com.example.MyCustomMinifier
   ```

4. **Build and use** as a plugin dependency (see Installation section)

## Architecture Benefits

The SPI-based architecture provides:

- **Loose coupling**: Minifiers don't depend on the plugin, plugin doesn't depend on specific minifiers
- **Extensibility**: Anyone can create custom minifiers without modifying the plugin
- **Modularity**: Users only download the minifiers they actually use
- **Discovery**: Minifiers are automatically discovered at runtime via Java's ServiceLoader
- **Flexibility**: Multiple minifiers can coexist, each handling different file types

## License

MIT License - see LICENSE file for details

## Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.

## Resources

- [webforJ Documentation](https://documentation.webforj.com/)
- [webforJ Minification Plugin](https://github.com/webforj/webforj-plugins/tree/main/webforj-minify)
- [Jackson Streaming API](https://github.com/FasterXML/jackson-core)
- [Java Service Provider Interface](https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html)

## Support

For questions or issues:
- Open an issue on GitHub
- Check the [webforJ documentation](https://documentation.webforj.com/)
- Ask on the [webforJ community forum](https://github.com/webforj/webforj/discussions)
