package com.redhat.rhjmc.containerjfr.platform;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.redhat.rhjmc.containerjfr.core.util.log.Logger;

import org.apache.commons.lang3.exception.ExceptionUtils;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1Service;
import io.kubernetes.client.models.V1ServiceList;
import io.kubernetes.client.models.V1ServicePort;
import io.kubernetes.client.models.V1ServiceSpec;

class KubeApiPlatformClient implements PlatformClient {

    private final Logger logger;
    private final CoreV1Api api;
    private final String namespace;

    KubeApiPlatformClient(Logger logger, CoreV1Api api, String namespace) {
        this.logger = logger;
        this.api = api;
        this.namespace = namespace;
    }

    @Override
    public List<ServiceRef> listDiscoverableServices() {
        try {
            String currentNamespace = namespace;
            V1ServiceList services = api
                .listNamespacedService(currentNamespace, null, null, null, null, null, null, null, null, null);
            List<ServiceRef> initialRefs = new ArrayList<>();
            for (V1Service service : services.getItems()) {
                V1ServiceSpec spec = service.getSpec();
                for (V1ServicePort port : spec.getPorts()) {
                    initialRefs.add(new ServiceRef(spec.getClusterIP(), null, port.getPort()));
                }
            }
            return initialRefs
                .parallelStream()
                .map(this::resolveServiceRefHostname)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (ApiException e) {
            logger.warn(e.getMessage());
            logger.warn(e.getResponseBody());
            return Collections.emptyList();
        }
    }

    private ServiceRef resolveServiceRefHostname(ServiceRef in) {
        try {
            String hostname = InetAddress.getByName(in.getIp()).getCanonicalHostName();
            logger.debug(String.format("Resolved %s to %s", in.getIp(), hostname));
            return new ServiceRef(in.getIp(), hostname, in.getPort());
        } catch (UnknownHostException e) {
            logger.debug(ExceptionUtils.getStackTrace(e));
            return null;
        }
    }
}
