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

package com.github.nikolaybespalov;

import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Advapi32Util.EventLogIterator;
import com.sun.jna.platform.win32.Advapi32Util.EventLogRecord;
import com.sun.jna.ptr.IntByReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.jna.platform.win32.WinNT.*;
import static com.sun.jna.platform.win32.WinReg.HKEY_LOCAL_MACHINE;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class WindowsEventLogHandlerTest {
    private static final String SOURCE_NAME = "My Application";
    private static final String CONFIG_MESSAGE = "config-message-" + UUID.randomUUID();
    private static final String ENTERING_MESSAGE = "ENTRY";
    private static final String EXITING_MESSAGE = "RETURN";
    private static final String FINE_MESSAGE = "fine-message-" + UUID.randomUUID();
    private static final String FINER_MESSAGE = "finer-message-" + UUID.randomUUID();
    private static final String FINEST_MESSAGE = "finest-message-" + UUID.randomUUID();
    private static final String INFO_MESSAGE = "info-message-" + UUID.randomUUID();
    private static final String LOG_MESSAGE = "log-message-" + UUID.randomUUID();
    private static final String WARNING_MESSAGE = "warning-message-" + UUID.randomUUID();
    private static final String SEVERE_MESSAGE = "severe-message-" + UUID.randomUUID();
    private static Logger log = Logger.getLogger(WindowsEventLogHandler.class.getName(), "messages");
    private HANDLE hEventLog;

    @Before
    public void setUp() throws Exception {
        hEventLog = Advapi32.INSTANCE.OpenEventLog(null, SOURCE_NAME);
        Advapi32.INSTANCE.ClearEventLog(hEventLog, null);
    }

    @After
    public void tearDown() throws Exception {
        Advapi32.INSTANCE.ClearEventLog(hEventLog, null);
        Advapi32.INSTANCE.CloseEventLog(hEventLog);
    }

    @Test
    public void testConfig() {
        log.config(CONFIG_MESSAGE);

        assertEventLog(CONFIG_MESSAGE, EVENTLOG_INFORMATION_TYPE);
    }

    @Test
    public void testEntering() {
        log.entering(null, null);

        assertEventLog(ENTERING_MESSAGE, EVENTLOG_INFORMATION_TYPE);
    }

    @Test
    public void testExiting() {
        log.exiting(null, null);

        assertEventLog(EXITING_MESSAGE, EVENTLOG_INFORMATION_TYPE);
    }

    @Test
    public void testFine() {
        log.fine(FINE_MESSAGE);

        assertEventLog(FINE_MESSAGE, EVENTLOG_INFORMATION_TYPE);
    }

    @Test
    public void testFiner() {
        log.fine(FINER_MESSAGE);

        assertEventLog(FINER_MESSAGE, EVENTLOG_INFORMATION_TYPE);
    }

    @Test
    public void testFinest() {
        log.fine(FINEST_MESSAGE);

        assertEventLog(FINEST_MESSAGE, EVENTLOG_INFORMATION_TYPE);
    }

    @Test
    public void testInfo() {
        log.fine(INFO_MESSAGE);

        assertEventLog(INFO_MESSAGE, EVENTLOG_INFORMATION_TYPE);
    }

    @Test
    public void testLog() {
        log.log(Level.ALL, LOG_MESSAGE);

        assertEventLog(LOG_MESSAGE, EVENTLOG_INFORMATION_TYPE);
    }

    @Test
    public void testLogLevelOff() {
        log.log(Level.OFF, LOG_MESSAGE);

        assertEquals(0, getEventLogMessages());
    }

    @Test
    public void testSevere() {
        log.severe(SEVERE_MESSAGE);

        assertEventLog(SEVERE_MESSAGE, EVENTLOG_ERROR_TYPE);
    }

    @Test
    public void testWarning() {
        log.warning(WARNING_MESSAGE);

        assertEventLog(WARNING_MESSAGE, EVENTLOG_WARNING_TYPE);
    }

    @Test
    public void testLocalizationEn() {
        Locale.setDefault(Locale.ENGLISH);
        log.log(Level.INFO, "message1", "parameter");
        assertEventLog("Message with parameter", EVENTLOG_INFORMATION_TYPE);
    }

    @Test
    public void testLocalizationRu() {
        Locale.setDefault(new Locale("ru"));
        log.log(Level.INFO, "message1", "параметром");
        assertEventLog("Сообщение с параметром", EVENTLOG_INFORMATION_TYPE);
    }

    @Test
    public void testRegistry() {
        final String regKey = "SYSTEM\\CurrentControlSet\\Services\\EventLog\\Application\\My Application";

        Handler handler = new WindowsEventLogHandler();

        assertNotNull(Advapi32Util.registryGetValue(HKEY_LOCAL_MACHINE, regKey, "ParameterMessageFile"));

        handler.close();

        assertNull(Advapi32Util.registryGetValue(HKEY_LOCAL_MACHINE, regKey, "ParameterMessageFile"));
    }

    private void assertEventLog(String message, int eventType) {
        assertEquals(1, getEventLogMessages());
        assertEquals(SOURCE_NAME, getEventLogRecord().getSource());
        assertEquals(1, getEventLogRecord().getStrings().length);
        assertEquals(message, getEventLogRecord().getStrings()[0]);
        assertEquals(eventType, getEventLogRecord().getRecord().EventType.intValue());
    }

    private EventLogRecord getEventLogRecord() {
        ArrayList<EventLogRecord> eventLogRecords = new ArrayList<>();

        EventLogIterator it = new EventLogIterator(null, SOURCE_NAME, EVENTLOG_SEQUENTIAL_READ | EVENTLOG_BACKWARDS_READ);

        try {
            while (it.hasNext()) {
                eventLogRecords.add(it.next());
            }
        } finally {
            it.close();
        }

        assert eventLogRecords.get(0).getStrings().length == 1;

        return eventLogRecords.get(0);
    }

    private int getEventLogMessages() {
        IntByReference numberOfEventLogRecords = new IntByReference();

        assert Advapi32.INSTANCE.GetNumberOfEventLogRecords(hEventLog, numberOfEventLogRecords);

        return numberOfEventLogRecords.getValue();
    }
}