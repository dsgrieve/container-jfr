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
package com.redhat.rhjmc.containerjfr.net.web.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.openjdk.jmc.rjmx.services.jfr.IEventTypeInfo;

import com.google.gson.Gson;

import com.redhat.rhjmc.containerjfr.jmc.serialization.SerializableEventTypeInfo;
import com.redhat.rhjmc.containerjfr.net.AuthManager;
import com.redhat.rhjmc.containerjfr.net.TargetConnectionManager;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.HttpStatusException;

class TargetEventsGetHandler extends AbstractAuthenticatedRequestHandler {

    private final TargetConnectionManager connectionManager;
    private final Gson gson;

    @Inject
    TargetEventsGetHandler(AuthManager auth, TargetConnectionManager connectionManager, Gson gson) {
        super(auth);
        this.connectionManager = connectionManager;
        this.gson = gson;
    }

    @Override
    public HttpMethod httpMethod() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/api/v1/targets/:targetId/events";
    }

    @Override
    void handleAuthenticated(RoutingContext ctx) {
        try {
            String targetId = ctx.pathParam("targetId");
            List<SerializableEventTypeInfo> templates =
                    connectionManager.executeConnectedTask(
                            targetId,
                            connection -> {
                                Collection<? extends IEventTypeInfo> origInfos =
                                        connection.getService().getAvailableEventTypes();
                                List<SerializableEventTypeInfo> infos =
                                        new ArrayList<>(origInfos.size());
                                for (IEventTypeInfo info : origInfos) {
                                    infos.add(new SerializableEventTypeInfo(info));
                                }
                                return infos;
                            });
            ctx.response().end(gson.toJson(templates));
        } catch (Exception e) {
            throw new HttpStatusException(500, e);
        }
    }
}
