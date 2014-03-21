package org.randombits.confluence.conveyor;

import com.atlassian.event.api.EventListener;
import com.atlassian.plugin.StateAware;
import com.opensymphony.xwork.config.ConfigurationProvider;
import org.randombits.confluence.conveyor.config.ConveyorConfigurationProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.opensymphony.xwork.config.ConfigurationManager.addConfigurationProvider;
import static com.opensymphony.xwork.config.ConfigurationManager.getConfiguration;
import static com.opensymphony.xwork.config.ConfigurationManager.getConfigurationProviders;
import static org.echocat.jomon.runtime.CollectionUtils.addAll;
import static org.echocat.jomon.runtime.CollectionUtils.asImmutableList;
import static org.echocat.jomon.runtime.CollectionUtils.isNotEmpty;

public class ConveyorListener implements StateAware {

    private final Set<ConveyorConfigurationProvider> _providers = new HashSet<>();
    private boolean _enabled;

    public ConveyorListener() {
        addProviders(createProviders());
        enabled();
    }

    @Nonnull
    protected Iterable<ConveyorConfigurationProvider> createProviders() {
        return asImmutableList(new ConveyorConfigurationProvider("/org/echocat/adam/convoyed/conveyor-config.xml"));
    }

    protected void addProviders(@Nullable Iterable<ConveyorConfigurationProvider> providers) {
        synchronized (_providers) {
            final boolean wasEnabled = _enabled;
            try {
                disabled();
                addAll(_providers, providers);
            } finally {
                if (wasEnabled) {
                    enabled();
                }
            }
        }
    }

    protected void reload() {
        synchronized (_providers) {
            if (isNotEmpty(_providers)) {
                for (final ConveyorConfigurationProvider provider : _providers) {
                    addConfigurationProvider(provider);
                }
                getConfiguration().reload();
            }
        }
    }

    @Override
    public void enabled() {
        synchronized (_providers) {
            if (!_enabled) {
                reload();
                _enabled = true;
            }
        }
    }

    @Override
    public void disabled() {
        synchronized (_providers) {
            if (_enabled) {
                for (final ConveyorConfigurationProvider provider : _providers) {
                    provider.destroy();
                    //noinspection unchecked
                    final List<ConfigurationProvider> allProviders = getConfigurationProviders();
                    // noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (allProviders) {
                        allProviders.remove(provider);
                    }
                }
                getConfiguration().reload();
                _enabled = false;
            }
        }
    }


    @EventListener
    public void handles(@SuppressWarnings("UnusedParameters") @Nonnull DummyEvent event) {}

    public static class DummyEvent {}

}
