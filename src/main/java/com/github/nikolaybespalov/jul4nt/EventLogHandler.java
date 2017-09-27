//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

package com.github.nikolaybespalov.jul4nt;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.W32Errors;
import com.sun.jna.platform.win32.WinNT.*;
import com.sun.jna.platform.win32.WinReg.HKEYByReference;
import com.sun.jna.ptr.IntByReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.MessageFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Map;
import java.util.logging.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sun.jna.platform.win32.WinNT.*;
import static com.sun.jna.platform.win32.WinReg.HKEY_LOCAL_MACHINE;
import static java.util.logging.ErrorManager.*;

/**
 * This <tt>Java Util</tt> <tt>Handler</tt> publishes log records to <tt>Windows Event Log</tt>.
 * <p>
 * <b>Configuration:</b>
 * By default each <tt>EventLogHandler</tt> is initialized using the following
 * <tt>LogManager</tt> configuration properties.  If properties are not defined
 * (or have invalid values) then the specified default values are used.
 * <ul>
 * <li>   com.github.nikolaybespalov.jul4nt.level
 * specifies the default level for the <tt>Handler</tt>
 * (defaults to <tt>Level.INFO</tt>).
 * <li>   com.github.nikolaybespalov.jul4nt.filter
 * specifies the name of a <tt>Filter</tt> class to use
 * (defaults to no <tt>Filter</tt>).
 * <li>   com.github.nikolaybespalov.jul4nt.EventLogHandler.formatter
 * specifies the name of a <tt>Formatter</tt> class to use
 * (defaults to the internal implementation which only localizes the message if necessary).
 * <li>   com.github.nikolaybespalov.jul4nt.EventLogHandler.encoding
 * the name of the character set encoding to use (defaults to
 * the default platform encoding).
 * <li>   com.github.nikolaybespalov.jul4nt.EventLogHandler.sourceName
 * specifies the source name of a <tt>Event Log</tt>
 * (defaults to <tt>"EventLogHandler"</tt>).
 * <li>   com.github.nikolaybespalov.jul4nt.EventLogHandler.autoCreateRegKey
 * specifies to automatically create the required registry key
 * (defaults to <tt>true</tt>).
 * <li>   com.github.nikolaybespalov.jul4nt.EventLogHandler.autoDeleteRegKey
 * specifies to automatically delete the required registry key
 * (defaults to <tt>false</tt>).
 * </ul>
 *
 * @author <a href="mailto:nikolaybespalov@gmail.com">Nikolay Bespalov</a>
 */

public class EventLogHandler extends Handler implements AutoCloseable {
    private static final String SOURCE_NAME = "jul4nt";
    private static final String EVENT_MESSAGE_FILE = "jul4nt";
    private static final String EVENT_LOG_REG_KEY = "SYSTEM\\CurrentControlSet\\Services\\EventLog\\Application";
    private static final String PARAMETER_MESSAGE_FILE_REG_VALUE_NAME = "ParameterMessageFile";
    private static final String CLASS_NAME = "com.github.nikolaybespalov.jul4nt.EventLogHandler";

    private static final int MESSAGE_ID_SUCCESS = 0x1001;
    private static final int MESSAGE_ID_INFO = 0x40001002;
    private static final int MESSAGE_ID_WARNING = 0x80001003;
    private static final int MESSAGE_ID_ERROR = 0xC0001004;

    @SuppressWarnings("Duplicates")
    private static final Map<Level, Integer> LOG_LEVEL_TO_EVENT_TYPE = Collections.unmodifiableMap(Stream.of(
            new SimpleEntry<>(Level.SEVERE, EVENTLOG_ERROR_TYPE),
            new SimpleEntry<>(Level.WARNING, EVENTLOG_WARNING_TYPE),
            new SimpleEntry<>(Level.INFO, EVENTLOG_INFORMATION_TYPE),
            new SimpleEntry<>(Level.CONFIG, EVENTLOG_INFORMATION_TYPE),
            new SimpleEntry<>(Level.FINE, EVENTLOG_INFORMATION_TYPE),
            new SimpleEntry<>(Level.FINER, EVENTLOG_INFORMATION_TYPE),
            new SimpleEntry<>(Level.FINEST, EVENTLOG_INFORMATION_TYPE),
            new SimpleEntry<>(Level.ALL, EVENTLOG_INFORMATION_TYPE))
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue)));

    @SuppressWarnings("Duplicates")
    private static final Map<Level, Integer> LOG_LEVEL_TO_MESSAGE_ID = Collections.unmodifiableMap(Stream.of(
            new SimpleEntry<>(Level.SEVERE, MESSAGE_ID_ERROR),
            new SimpleEntry<>(Level.WARNING, MESSAGE_ID_WARNING),
            new SimpleEntry<>(Level.INFO, MESSAGE_ID_INFO),
            new SimpleEntry<>(Level.CONFIG, MESSAGE_ID_INFO),
            new SimpleEntry<>(Level.FINE, MESSAGE_ID_SUCCESS),
            new SimpleEntry<>(Level.FINER, MESSAGE_ID_SUCCESS),
            new SimpleEntry<>(Level.FINEST, MESSAGE_ID_SUCCESS),
            new SimpleEntry<>(Level.ALL, MESSAGE_ID_SUCCESS))
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue)));

    private static File messageFile;
    private static boolean autoDeleteRegKey;
    private String registryKeyName;
    private HANDLE hEventLog;

    static {
        if (Platform.isWindows()) {
            try {
                messageFile = Native.extractFromResourcePath(EVENT_MESSAGE_FILE);
            } catch (IOException e) {
                throw new LinkageError("Failed to extract Message File", e);
            }
        }
    }

    public EventLogHandler() {
        if (!Platform.isWindows()) {
            return;
        }

        final String sourceName = getLoggingOrSystemProperty("sourceName", SOURCE_NAME);

        boolean autoCreateRegKey = true;

        final String autoCreateRegKeyProperty = getLoggingOrSystemProperty("autoCreateRegKey");

        if (autoCreateRegKeyProperty != null) {
            autoCreateRegKey = Boolean.parseBoolean(autoCreateRegKeyProperty);
        }

        autoDeleteRegKey = false;

        final String autoDeleteRegKeyProperty = getLoggingOrSystemProperty("autoDeleteRegKey");

        if (autoDeleteRegKeyProperty != null) {
            autoDeleteRegKey = Boolean.parseBoolean(autoDeleteRegKeyProperty);
        }

        registryKeyName = EVENT_LOG_REG_KEY + "\\" + sourceName;

        if (getFormatter() == null) {
            setFormatter(new Formatter() {
                @Override
                public String format(LogRecord logRecord) {
                    return super.formatMessage(logRecord);
                }
            });
        }

        if (autoCreateRegKey) {
            HKEYByReference phkResult = new HKEYByReference();
            IntByReference lpdwDisposition = new IntByReference();

            int rc;

            try {
                if (Advapi32.INSTANCE.RegOpenKeyEx(HKEY_LOCAL_MACHINE, registryKeyName, 0,
                        KEY_READ, phkResult) != W32Errors.ERROR_SUCCESS) {
                    if ((rc = Advapi32.INSTANCE.RegCreateKeyEx(HKEY_LOCAL_MACHINE, registryKeyName, 0, null,
                            REG_OPTION_NON_VOLATILE, KEY_ALL_ACCESS, null, phkResult,
                            lpdwDisposition)) != W32Errors.ERROR_SUCCESS) {
                        reportError(MessageFormat.format("Failed to create registry key: {0} {1} {2}",
                                "HKEY_LOCAL_MACHINE", registryKeyName, Kernel32Util.formatMessage(rc)), null, OPEN_FAILURE);
                    } else {
                        final char[] messageFilePath = Native.toCharArray(messageFile.getAbsolutePath());

                        if ((rc = Advapi32.INSTANCE.RegSetValueEx(phkResult.getValue(), PARAMETER_MESSAGE_FILE_REG_VALUE_NAME, 0, REG_SZ,
                                messageFilePath, messageFilePath.length * Native.WCHAR_SIZE)) != W32Errors.ERROR_SUCCESS) {
                            reportError(MessageFormat.format("Failed to set registry value: {0} {1} {2} {3}",
                                    "HKEY_LOCAL_MACHINE", registryKeyName, PARAMETER_MESSAGE_FILE_REG_VALUE_NAME, Kernel32Util.formatMessage(rc)), null, OPEN_FAILURE);
                        }
                    }
                }
            } finally {
                if ((rc = Advapi32.INSTANCE.RegCloseKey(phkResult.getValue())) != W32Errors.ERROR_SUCCESS) {
                    reportError(MessageFormat.format("Failed to close registry key: {0} {1} {2}",
                            "HKEY_LOCAL_MACHINE", registryKeyName, Kernel32Util.formatMessage(rc)), null, OPEN_FAILURE);
                }
            }
        }

        hEventLog = Advapi32.INSTANCE.RegisterEventSource(null, sourceName);

        if (hEventLog == null) {
            reportError("Failed to register Event Source: "
                    + Kernel32Util.formatMessage(Native.getLastError()), null, OPEN_FAILURE);
        }
    }

    @Override
    public void publish(LogRecord logRecord) {
        if (!Platform.isWindows()) {
            return;
        }

        if (logRecord == null || hEventLog == null || logRecord.getLevel().equals(Level.OFF)) {
            return;
        }

        Memory data = null;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(512)) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(logRecord);
            byte[] bytes = byteArrayOutputStream.toByteArray();
            data = new Memory(bytes.length);
            data.write(0, bytes, 0, bytes.length);
        } catch (IOException e) {
            reportError("Failed to serialize LogRecord", e, WRITE_FAILURE);
        }

        if (!Advapi32.INSTANCE.ReportEvent(
                hEventLog,
                LOG_LEVEL_TO_EVENT_TYPE.get(logRecord.getLevel()),
                0,
                LOG_LEVEL_TO_MESSAGE_ID.get(logRecord.getLevel()),
                null,
                1,
                data != null ? (int) data.size() : 0,
                new String[]{getFormatter().format(logRecord)},
                data)) {
            reportError("Failed to report Event: "
                    + Kernel32Util.formatMessage(Native.getLastError()), null, WRITE_FAILURE);
        }
    }

    @Override
    public void flush() {
        // Do nothing
    }

    @Override
    public void close() throws SecurityException {
        if (!Platform.isWindows()) {
            return;
        }

        if (hEventLog != null && !Advapi32.INSTANCE.DeregisterEventSource(hEventLog)) {
            reportError("Failed to deregister Event Source: "
                    + Kernel32Util.formatMessage(Native.getLastError()), null, CLOSE_FAILURE);
        }

        if (autoDeleteRegKey) {
            int rc;

            if ((rc = Advapi32.INSTANCE.RegDeleteKey(HKEY_LOCAL_MACHINE, registryKeyName)) != W32Errors.ERROR_SUCCESS) {
                reportError(MessageFormat.format("Failed to delete registry key: {0} {1} {2}",
                        "HKEY_LOCAL_MACHINE", registryKeyName, Kernel32Util.formatMessage(rc)), null, CLOSE_FAILURE);
            } else {
                autoDeleteRegKey = false;
            }
        }
    }

    /**
     * Get the value of a logging property or system property. The method returns null if the property is not found or empty.
     *
     * @param name name of the property
     * @return property file key value if exist or {@code null}
     */
    private String getLoggingOrSystemProperty(final String name) {
        return getLoggingOrSystemProperty(name, null);
    }

    /**
     * Get the value of a logging property or system property. The method returns {@code defaultValue} if the property is not found or empty.
     *
     * @param name         name of the property
     * @param defaultValue the default value
     * @return property file key value if exist or {@code defaultValue}
     */
    private String getLoggingOrSystemProperty(final String name, final String defaultValue) {
        final String propertyKey = CLASS_NAME + "." + name;

        String propertyValue = LogManager.getLogManager().getProperty(propertyKey);

        if (propertyValue == null) {
            propertyValue = System.getProperty(propertyKey, defaultValue);
        }

        if (propertyValue == null || propertyValue.isEmpty()) {
            propertyValue = defaultValue;
        }

        return propertyValue;
    }
}
