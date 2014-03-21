/*****************************************************************************************
 * *** BEGIN LICENSE BLOCK *****
 *
 * echocat Adam, Copyright (c) 2014 echocat
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 *
 * *** END LICENSE BLOCK *****
 ****************************************************************************************/

package org.echocat.adam.configuration;

import com.atlassian.bandana.BandanaManager;
import org.apache.commons.io.IOUtils;
import org.echocat.jomon.runtime.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext.GLOBAL_CONTEXT;
import static java.lang.System.currentTimeMillis;
import static org.echocat.adam.configuration.ConfigurationMarshaller.unmarshall;

public class ConfigurationRepository {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationRepository.class);

    @Nonnull
    public static final String KEY = Configuration.class.getName();
    @Nonnull
    public static final Duration CACHE_EXPIRES_AT = new Duration("15s");

    @Nonnull
    private final BandanaManager _bandanaManager;

    @Nonnegative
    private volatile long _expiresAt;
    @Nullable
    private volatile Configuration _cached;

    @Autowired
    public ConfigurationRepository(@Nonnull BandanaManager bandanaManager) {
        _bandanaManager = bandanaManager;
    }

    public void flush() {
        _cached = null;
        _expiresAt = 0;
    }

    @Nonnull
    public Configuration get() {
        Configuration result = _cached;
        if (result == null || _expiresAt < currentTimeMillis()) {
            result = getInternal();
            _cached = result;
            _expiresAt = currentTimeMillis() + CACHE_EXPIRES_AT.in(TimeUnit.MILLISECONDS);
        }
        return result;
    }

    @Nonnull
    public String getPlain() {
        final Object plain = _bandanaManager.getValue(GLOBAL_CONTEXT, KEY);
        String result;
        if (plain instanceof String) {
            result = (String) plain;
        } else {
            try (final InputStream is = getClass().getResourceAsStream("default.configuration.xml")) {
                result = IOUtils.toString(is);
            } catch (final IOException e) {
                LOG.warn("Could not load the default configuration. Will use an empty one.", e);
                result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<configuration></configuration>";
            }
        }
        return result;

    }

    @Nonnull
    protected Configuration getInternal() {
        final String plain = getPlain();
        Configuration result;
        try {
            result = unmarshall(plain, "database:configuration.xml");
        } catch (final Exception e) {
            LOG.warn("Could not unmarshall the configuration. Will use an empty one.", e);
            result = new Configuration();
        }
        return result;
    }

    public void set(@Nullable String xml) {
        _bandanaManager.setValue(GLOBAL_CONTEXT, KEY, xml);
        _cached = null;
        _expiresAt = currentTimeMillis() + CACHE_EXPIRES_AT.in(TimeUnit.MILLISECONDS);
    }

    @Nonnull
    public BandanaManager getBandanaManager() {
        return _bandanaManager;
    }

}
