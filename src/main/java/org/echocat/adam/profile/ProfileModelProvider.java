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

package org.echocat.adam.profile;

import org.echocat.adam.configuration.Configuration;
import org.echocat.adam.configuration.ConfigurationRepository;
import org.echocat.adam.configuration.profile.Profile;
import org.springframework.beans.factory.DisposableBean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;

public class ProfileModelProvider implements DisposableBean {

    @Nullable
    private static ProfileModelProvider c_instance;

    @Nonnull
    public static ProfileModelProvider profileModelProvider() {
        final ProfileModelProvider result = c_instance;
        if (result == null) {
            throw new IllegalStateException("There is currently no instance registered.");
        }
        return result;
    }

    @Nonnull
    private final GroupProvider _groupProvider;
    @Nonnull
    private final ConfigurationRepository _configurationRepository;

    public ProfileModelProvider(@Nonnull GroupProvider groupProvider, @Nonnull ConfigurationRepository configurationRepository) {
        _groupProvider = groupProvider;
        _configurationRepository = configurationRepository;
        c_instance = this;
    }

    @Nonnull
    public ProfileModel get() {
        return new ProfileModelWrapper(getConfiguration());
    }

    @Nonnull
    protected Configuration getConfiguration() {
        return getConfigurationRepository().get();
    }

    @Nullable
    protected Profile getProfileFromConfiguration() {
        return getConfiguration().getProfile();
    }

    @Nonnull
    public GroupProvider getGroupProvider() {
        return _groupProvider;
    }

    @Nonnull
    public ConfigurationRepository getConfigurationRepository() {
        return _configurationRepository;
    }

    @Override
    public void destroy() throws Exception {
        c_instance = null;
    }

    protected class ProfileModelWrapper extends ProfileModelSupport {

        @Nonnull
        private final Configuration _configuration;

        public ProfileModelWrapper(@Nonnull Configuration configuration) {
            _configuration = configuration;
        }

        @Override
        public Iterator<Group> iterator() {
            return getGroupProvider().provideFor(_configuration.getProfile()).iterator();
        }

    }

}
