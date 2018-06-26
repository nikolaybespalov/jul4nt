# jul4nt

[Java Logging] [Handler] which publishes log records to [Windows Event Log]

![Windows Event Viewer](http://www.reviversoft.com/blog/wp-content/uploads/2013/12/Windows_XP_Event_Viewer.png)

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.nikolaybespalov/jul4nt/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.nikolaybespalov/jul4nt)
[![AppVeyor](https://ci.appveyor.com/api/projects/status/github/nikolaybespalov/jul4nt?svg=true)](https://ci.appveyor.com/project/nikolaybespalov/jul4nt)
[![Codacy](https://api.codacy.com/project/badge/Grade/5a4bb3b313a14dcd931c9b7532252baa)](https://www.codacy.com/app/nikolaybespalov/jul4nt)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/5a4bb3b313a14dcd931c9b7532252baa)](https://www.codacy.com/app/nikolaybespalov/jul4nt?utm_source=github.com&utm_medium=referral&utm_content=nikolaybespalov/jul4nt&utm_campaign=Badge_Coverage)


## How to use?

The library is available on Maven central. You can start to use the library by adding it to `dependencies` section of `pom.xml`:
```xml
  <dependencies>
    <!-- ... -->
    <dependency>
      <groupId>com.github.nikolaybespalov</groupId>
      <artifactId>jul4nt</artifactId>
      <version>${jul4nt.version}</version>
      <scope>runtime</scope>
    </dependency>
    <!-- ... -->
  </dependencies>
```

Or use it in `build.gradle`:
```java
  dependencies {
    // ...
    runtime("com.github.nikolaybespalov:jul4nt:{jul4nt.version}")
    // ...
  }
```

Now you can use the logging configuration file with the following options:
```properties
# 
handlers = com.github.nikolaybespalov.jul4nt.EventLogHandler

# Specifies the default level for the Handler (defaults to Level.INFO)
com.github.nikolaybespalov.jul4nt.EventLogHandler.level = Level.SEVERE

# Specifies the name of a Filter class to use (defaults to no Filter)
com.github.nikolaybespalov.jul4nt.EventLogHandler.filter = 

# Specifies the name of a Formatter class to use (defaults to internal implementation)
com.github.nikolaybespalov.jul4nt.EventLogHandler.formatter = java.util.logging.SimpleFormatter

# The name of the character set encoding to use (defaults to the default platform encoding)
com.github.nikolaybespalov.jul4nt.EventLogHandler.encoding = UTF-8

# The name of the Source Name to use (defaults to EventLogHandler)
com.github.nikolaybespalov.jul4nt.EventLogHandler.sourceName = My Application

# Allows automatically create the required registry key (defaults to true)
com.github.nikolaybespalov.jul4nt.EventLogHandler.autoCreateRegKey = true

# Allows automatically delete the required registry key (defaults to false)
com.github.nikolaybespalov.jul4nt.EventLogHandler.autoDeleteRegKey = false
```

Or use the above-described configuration properties as system properties. For example:
```properties
-Dcom.github.nikolaybespalov.jul4nt.EventLogHandler.sourceName="My Application"
```

## Example
An [example](https://github.com/nikolaybespalov/jul4nt-example) of using the jul4nt

[Java Logging]: https://docs.oracle.com/javase/8/docs/technotes/guides/logging/overview.html "Java Logging"
[Handler]: https://docs.oracle.com/javase/8/docs/api/java/util/logging/Handler.html "Handler"
[Windows Event Log]: https://msdn.microsoft.com/ru-ru/library/windows/desktop/aa385780(v=vs.85).aspx "Windows Event Log"
