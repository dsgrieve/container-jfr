package com.redhat.rhjmc.containerjfr.platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import com.redhat.rhjmc.containerjfr.core.util.log.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.apache.commons.lang3.exception.ExceptionUtils;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.util.Config;

public class Platform {

    private final Logger logger;
    private final PlatformClient client;

    Platform(Logger logger) {
        this.logger = logger;
        PlatformCheckResult pcr;
        if ((pcr = detectKubernetesApi()).available) {
            logger.info("Kubernetes configuration detected");
            client = pcr.client;
        } else {
            logger.info("No runtime platform support available");
            client = new DefaultPlatformClient();
        }
    }

    private PlatformCheckResult detectKubernetesApi() {
        PlatformCheckResult pcr = new PlatformCheckResult();
        try {
            String namespace = getKubernetesNamespace();
            Configuration.setDefaultApiClient(Config.fromCluster());
            CoreV1Api api = new CoreV1Api();
            // arbitrary request - don't care about the result, just whether the API is available
            api.listNamespacedService(namespace, null, null, null, null, null, null, null, null, null);
            pcr.client = new KubeApiPlatformClient(logger, api, namespace);
            pcr.available = true;
        } catch (IOException e) {
            logger.debug(ExceptionUtils.getMessage(e));
            logger.debug(ExceptionUtils.getStackTrace(e));
        } catch (ApiException e) {
            logger.debug(ExceptionUtils.getMessage(e));
            logger.debug(e.getResponseBody());
            logger.debug(ExceptionUtils.getStackTrace(e));
        }
        return pcr;
    }

    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    private static String getKubernetesNamespace() throws IOException {
        return Files.readString(Paths.get(Config.SERVICEACCOUNT_ROOT, "namespace"));
    }

    public Optional<PlatformClient> getClient() {
        return Optional.of(client);
    }

    private static class PlatformCheckResult {
        boolean available;
        PlatformClient client;
    }
}
