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

import com.atlassian.confluence.user.UserDetailsManager;
import org.apache.commons.collections15.map.LRUMap;
import org.echocat.adam.configuration.Configuration;
import org.echocat.adam.configuration.ConfigurationRepository;
import org.echocat.adam.configuration.profile.Element;
import org.echocat.adam.configuration.profile.Profile;
import org.echocat.adam.localization.Localization;
import org.echocat.adam.profile.element.ElementModel;
import org.echocat.adam.profile.element.ElementModelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static java.util.Collections.synchronizedMap;
import static org.echocat.jomon.runtime.CollectionUtils.asImmutableSet;
import static org.echocat.jomon.runtime.CollectionUtils.emptyIterator;

public class GroupProvider implements Iterable<Group> {

    private static final Logger LOG = LoggerFactory.getLogger(GroupProvider.class);

    @Nonnull
    private final Map<Iterable<org.echocat.adam.configuration.profile.Group>, Iterable<Group>> _cache = synchronizedMap(new LRUMap<Iterable<org.echocat.adam.configuration.profile.Group>, Iterable<Group>>(3));
    @Nonnull
    private final Map<Iterable<org.echocat.adam.configuration.profile.Group>, Set<String>> _elementIdsCache = synchronizedMap(new LRUMap<Iterable<org.echocat.adam.configuration.profile.Group>, Set<String>>(3));

    @Nonnull
    private final ConfigurationRepository _configurationRepository;
    @Nonnull
    private final Set<String> _standardGroupIds;
    @Nonnull
    private final ElementModelProvider _elementModelProvider;

    @Autowired
    public GroupProvider(@Nonnull ConfigurationRepository configurationRepository, @Nonnull UserDetailsManager userDetailsManager, @Nonnull ElementModelProvider elementModelProvider) {
        _configurationRepository = configurationRepository;
        _elementModelProvider = elementModelProvider;
        _standardGroupIds = discoverStandardGroupIdsUsing(userDetailsManager);
    }

    @Nonnull
    public Set<String> getAllKnownElementIds() {
        return getAllKnownElementIdsFor(getConfiguration());
    }

    @Nonnull
    public Set<String> getAllKnownElementIdsFor(@Nullable Configuration configuration) {
        return getAllKnownElementIdsFor(configuration != null ? configuration.getProfile() : null);
    }

    @Nonnull
    public Set<String> getAllKnownElementIdsFor(@Nullable Profile profile) {
        return getAllKnownElementIdsFor(profile != null ? profile.getGroups() : null);
    }

    @Nonnull
    public Set<String> getAllKnownElementIdsFor(@Nullable Iterable<org.echocat.adam.configuration.profile.Group> original) {
        Set<String> result = _elementIdsCache.get(original);
        if (result == null) {
            result = new HashSet<>();
            if (original != null) {
                for (final org.echocat.adam.configuration.profile.Group group : original) {
                    final List<Element> elements = group.getElements();
                    if (elements != null) {
                        for (final Element element : elements) {
                            result.add(element.getId());
                        }
                    }
                }
            }
            result = asImmutableSet(result);
            _elementIdsCache.put(original, result);
        }
        return result;
    }

    @Override
    public Iterator<Group> iterator() {
        return provideFor(getConfiguration()).iterator();
    }

    @Nonnull
    public Iterable<Group> provideFor(@Nullable Configuration configuration) {
        return provideFor(configuration != null ? configuration.getProfile() : null);
    }

    @Nonnull
    public Iterable<Group> provideFor(@Nullable Profile profile) {
        return provideFor(profile != null ? profile.getGroups() : null);
    }

    @Nonnull
    public Iterable<Group> provideFor(@Nullable Iterable<org.echocat.adam.configuration.profile.Group> original) {
        Iterable<Group> result = _cache.get(original);
        if (result == null) {
            final Map<String, Group> idToGroup = new LinkedHashMap<>();
            if (original != null) {
                for (final org.echocat.adam.configuration.profile.Group group : original) {
                    final String id = group.getId();
                    if (idToGroup.containsKey(id)) {
                        LOG.warn("Found multiple group definition for id '" + id + "'. The duplicate one will be ingored.");
                    } else {
                        idToGroup.put(id, new GroupImpl(group));
                    }
                }
            }
            for (final String standardId : getStandardGroupIds()) {
                if (!idToGroup.containsKey(standardId)) {
                    idToGroup.put(standardId, new StandardGroup(standardId));
                }
            }
            result = asImmutableSet(idToGroup.values());
            _cache.put(original, result);
        }
        return result;
    }

    @Nonnull
    protected Set<String> getStandardGroupIds() {
        return _standardGroupIds;
    }

    @Nonnull
    protected Set<String> discoverStandardGroupIdsUsing(@Nonnull UserDetailsManager userDetailsManager) {
        return asImmutableSet(new LinkedHashSet<>(userDetailsManager.getProfileGroups()));
    }

    @Nonnull
    protected Configuration getConfiguration() {
        return getConfigurationRepository().get();
    }

    @Nonnull
    public ConfigurationRepository getConfigurationRepository() {
        return _configurationRepository;
    }

    protected class GroupImpl extends GroupSupport {

        @Nonnull
        private final org.echocat.adam.configuration.profile.Group _original;
        @Nullable
        private volatile Map<Locale, Localization> _localizations;

        public GroupImpl(@Nonnull org.echocat.adam.configuration.profile.Group original) {
            _original = original;
        }

        @Nonnull
        @Override
        public String getId() {
            return _original.getId();
        }

        @Nullable
        @Override
        public Map<Locale, Localization> getLocalizations() {
            Map<Locale, Localization> result = _localizations;
            if (result == null) {
                result = new LinkedHashMap<>();
                final List<org.echocat.adam.configuration.localization.Localization> localizations = _original.getLocalizations();
                if (localizations != null) {
                    for (final org.echocat.adam.configuration.localization.Localization localization : localizations) {
                        result.put(localization.getLocale(), localization);
                    }
                }
                _localizations = result;
            }
            return result;
        }

        @Override
        public Iterator<ElementModel> iterator() {
            return _elementModelProvider.provideFor(_original).iterator();
        }

    }

    protected class StandardGroup extends GroupSupport {

        @Nonnull
        private final String _id;

        public StandardGroup(@Nonnull String id) {
            _id = id;
        }


        @Nonnull
        @Override
        public String getId() {
            return _id;
        }

        @Nullable
        @Override
        public Map<Locale, Localization> getLocalizations() {
            return null;
        }

        @Override
        public Iterator<ElementModel> iterator() {
            return emptyIterator();
        }

    }

}
