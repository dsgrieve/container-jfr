package com.redhat.rhjmc.containerjfr.net;

import java.nio.file.Path;

import javax.inject.Named;
import javax.inject.Singleton;

import com.redhat.rhjmc.containerjfr.sys.Clock;
import com.redhat.rhjmc.containerjfr.sys.Environment;
import com.redhat.rhjmc.containerjfr.tui.ClientWriter;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;

@Module
public abstract class NetworkModule {
    @Binds
    @IntoSet
    abstract ConnectionListener bindRecordingExporter(RecordingExporter exporter);

    @Provides
    @Singleton
    static RecordingExporter provideRecordingExporter(@Named("RECORDINGS_PATH") Path recordingsPath, Environment env, ClientWriter cw, NetworkResolver resolver) {
        return new RecordingExporter(recordingsPath, env, cw, resolver);
    }

    @Provides
    @Singleton
    static NetworkResolver provideNetworkResolver() {
        return new NetworkResolver();
    }

    @Provides
    @Singleton
    static JMCConnectionToolkit provideJMCConnectionToolkit(ClientWriter cw, Clock clock) {
        return new JMCConnectionToolkit(cw, clock);
    }
}