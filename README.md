# Windows Event Log Handler
[Java Logging] [Handler] which publishes log records to [Windows Event Log]

![Windows Event Viewer](https://upload.wikimedia.org/wikipedia/en/f/f2/Windows_XP_Event_Viewer.png)

## How to use?

The library is available on Maven central. You can start to use the library by adding it to `dependencies` section of `pom.xml`:
```xml
  <dependencies>
    <!-- ... -->
    <dependency>
      <groupId>com.github.nikolaybespalov</groupId>
      <artifactId>WindowsEventLogHandler</artifactId>
      <version>0.1.0-SNAPSHOT</version>
      <scope>runtime</scope>
    </dependency>
    <!-- ... -->
  </dependencies>
```

Or use it in `build.gradle`:
```java
  dependencies {
    // ...
    runtime("com.github.nikolaybespalov:WindowsEventLogHandler:0.1.0-SNAPSHOT")
    // ...
  }
```

Now you can use the logging configuration file with the following options:
```properties
# 
handlers = com.github.nikolaybespalov.WindowsEventLogHandler

# Specifies the default level for the Handler (defaults to Level.INFO)
com.github.nikolaybespalov.WindowsEventLogHandler.level = Level.SEVERE

# Specifies the name of a Filter class to use (defaults to no Filter)
com.github.nikolaybespalov.WindowsEventLogHandler.filter = 

# Specifies the name of a Formatter class to use (defaults to internal implementation)
com.github.nikolaybespalov.WindowsEventLogHandler.formatter = java.util.logging.SimpleFormatter

# The name of the character set encoding to use (defaults to the default platform encoding)
com.github.nikolaybespalov.WindowsEventLogHandler.encoding = UTF-8

# The name of the Source Name to use (defaults to EventLogHandler)
com.github.nikolaybespalov.WindowsEventLogHandler.sourceName = My Application

# Allows automatically create the required registry key (defaults to true)
com.github.nikolaybespalov.WindowsEventLogHandler.autoCreateRegKey = true

# Allows automatically delete the required registry key (defaults to false)
com.github.nikolaybespalov.WindowsEventLogHandler.autoDeleteRegKey = false
```

Or use the above-described configuration properties as system properties. For example:
```properties
-Dcom.github.nikolaybespalov.WindowsEventLogHandler.sourceName="My Application"
```

[Java Logging]: https://docs.oracle.com/javase/8/docs/technotes/guides/logging/overview.html "Java Logging"
[Handler]: https://docs.oracle.com/javase/8/docs/api/java/util/logging/Handler.html "Handler"
[Windows Event Log]: https://msdn.microsoft.com/ru-ru/library/windows/desktop/aa385780(v=vs.85).aspx "Windows Event Log"
