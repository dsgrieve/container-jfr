/*-
 * #%L
 * Container JFR
 * %%
 * Copyright (C) 2020 Red Hat, Inc.
 * %%
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or data
 * (collectively the "Software"), free of charge and under any and all copyright
 * rights in the Software, and any and all patent rights owned or freely
 * licensable by each licensor hereunder covering either (i) the unmodified
 * Software as contributed to or provided by such licensor, or (ii) the Larger
 * Works (as defined below), to deal in both
 *
 * (a) the Software, and
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software (each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 * The above copyright notice and either this complete permission notice or at
 * a minimum a reference to the UPL must be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * #L%
 */
package com.redhat.rhjmc.containerjfr.net;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.remote.JMXServiceURL;

import com.redhat.rhjmc.containerjfr.core.log.Logger;
import com.redhat.rhjmc.containerjfr.core.net.JFRConnection;
import com.redhat.rhjmc.containerjfr.core.net.JFRConnectionToolkit;

public class TargetConnectionManager {

    public static final Pattern HOST_PORT_PAIR_PATTERN =
            Pattern.compile("^([^:\\s]+)(?::(\\d{1,5}))?$");

    private final Logger logger;
    // FIXME verify concurrent connection safety and remove locking
    private final ReentrantLock lock = new ReentrantLock();
    // maintain a short-lived cache of connections to allow nested ConnectedTasks
    // without having to manage connection reuse
    private final Map<String, JFRConnection> activeConnections = new HashMap<>();
    private final JFRConnectionToolkit jfrConnectionToolkit;

    TargetConnectionManager(Logger logger, JFRConnectionToolkit jfrConnectionToolkit) {
        this.logger = logger;
        this.jfrConnectionToolkit = jfrConnectionToolkit;
    }

    public <T> T executeConnectedTask(String targetId, ConnectedTask<T> task) throws Exception {
        try {
            if (activeConnections.containsKey(targetId)) {
                return task.execute(activeConnections.get(targetId));
            } else {
                try (JFRConnection connection = connect(targetId)) {
                    activeConnections.put(targetId, connection);
                    return task.execute(connection);
                }
            }
        } finally {
            activeConnections.remove(targetId);
        }
    }

    /**
     * Returns a new JFRConnection to the specified Target. This does not do any connection reuse or
     * other management, so clients are responsible for cleaning up the connection when they are
     * finished with it. When possible, clients should use executeConnectedTask instead, which does
     * perform automatic cleanup when the provided task has been completed.
     */
    public JFRConnection connect(String targetId) throws Exception {
        try {
            return attemptConnectAsJMXServiceURL(targetId);
        } catch (Exception e) {
            return attemptConnectAsHostPortPair(targetId);
        }
    }

    private JFRConnection attemptConnectAsJMXServiceURL(String url) throws Exception {
        return connect(new JMXServiceURL(url));
    }

    private JFRConnection attemptConnectAsHostPortPair(String s) throws Exception {
        Matcher m = HOST_PORT_PAIR_PATTERN.matcher(s);
        if (!m.find()) {
            return null;
        }
        String host = m.group(1);
        String port = m.group(2);
        if (port == null) {
            port = "9091";
        }
        return connect(host, Integer.parseInt(port));
    }

    private JFRConnection connect(JMXServiceURL url) throws Exception {
        logger.trace(String.format("Locking connection %s", url.toString()));
        lock.lockInterruptibly();
        return jfrConnectionToolkit.connect(
                url,
                List.of(
                        lock::unlock,
                        () ->
                                logger.trace(
                                        String.format("Unlocking connection %s", url.toString()))));
    }

    private JFRConnection connect(String host, int port) throws Exception {
        logger.trace(String.format("Locking connection %s:%d", host, port));
        lock.lockInterruptibly();
        return jfrConnectionToolkit.connect(
                host,
                port,
                List.of(
                        lock::unlock,
                        () ->
                                logger.trace(
                                        String.format("Unlocking connection %s:%d", host, port))));
    }

    public interface ConnectedTask<T> {
        T execute(JFRConnection connection) throws Exception;
    }
}
