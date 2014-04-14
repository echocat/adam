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

package org.echocat.adam.view;

import org.apache.commons.collections15.map.LRUMap;
import org.echocat.adam.access.AccessProvider;
import org.echocat.adam.access.ViewAccess;
import org.echocat.adam.configuration.Configuration;
import org.echocat.adam.configuration.ConfigurationRepository;
import org.echocat.adam.configuration.view.View.Element;
import org.echocat.adam.localization.Localization;
import org.echocat.adam.profile.Group;
import org.echocat.adam.profile.GroupProvider;
import org.echocat.adam.profile.GroupSupport;
import org.echocat.adam.profile.element.ElementModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static java.util.Collections.synchronizedMap;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.echocat.adam.view.View.*;
import static org.echocat.jomon.runtime.CollectionUtils.asImmutableList;

public class ViewProvider implements Iterable<View>, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(ViewProvider.class);

    @Nullable
    private static ViewProvider c_instance;

    @Nonnull
    public static ViewProvider viewProvider() {
        final ViewProvider result = c_instance;
        if (result == null) {
            throw new IllegalStateException("There is currently no instance registered.");
        }
        return result;
    }

    @Nonnull
    private final Map<Iterable<org.echocat.adam.configuration.view.View>, Map<String, View>> _cache = synchronizedMap(new LRUMap<Iterable<org.echocat.adam.configuration.view.View>, Map<String, View>>(3));

    @Nonnull
    private final ConfigurationRepository _configurationRepository;
    @Nonnull
    private final GroupProvider _groupProvider;
    @Nonnull
    private final AccessProvider _accessProvider;

    @Autowired
    public ViewProvider(@Nonnull ConfigurationRepository configurationRepository, @Nonnull GroupProvider groupProvider, @Nonnull AccessProvider accessProvider) {
        _configurationRepository = configurationRepository;
        _groupProvider = groupProvider;
        _accessProvider = accessProvider;
        c_instance = this;
    }

    @Override
    public Iterator<View> iterator() {
        return provideFor(getConfiguration()).iterator();
    }

    @Nullable
    public View provideBy(@Nonnull String name) {
        return provideBy(name, getConfiguration());
    }

    @Nullable
    protected View provideBy(@Nonnull String name, @Nullable Configuration configuration) {
        View result = provideInternalFor(configuration).get(name);
        if (result == null && ALL_ID.equals(name)) {
            // We assume that if there is no "all" view available we have created a "default" view with all elements in it.
            result = provideDefault(configuration);
        }
        return result;
    }

    @Nonnull
    public String getDefaultId() {
        return getDefaultId(getConfiguration());
    }

    @Nonnull
    protected String getDefaultId(@Nullable Configuration configuration) {
        final String id = configuration != null ? configuration.getDefaultView() : null;
        return isEmpty(id) ? DEFAULT_ID : id;
    }

    @Nonnull
    public String getHoverId() {
        return getHoverId(getConfiguration());
    }

    @Nonnull
    protected String getHoverId(@Nullable Configuration configuration) {
        final String id = configuration != null ? configuration.getHoverView() : null;
        return isEmpty(id) ? COMPACT_ID : id;
    }

    @Nonnull
    public View provideDefault() {
        return provideDefault(getConfiguration());
    }

    @Nonnull
    protected View provideDefault(@Nullable Configuration configuration) {
        final View result = provideBy(getDefaultId(configuration), configuration);
        if (result == null) {
            throw new IllegalStateException("There is no default view available.");
        }
        return result;
    }

    @Nonnull
    public View provideHover() {
        return provideHover(getConfiguration());
    }

    @Nonnull
    protected View provideHover(@Nullable Configuration configuration) {
        final View result = provideBy(getHoverId(configuration), configuration);
        if (result == null) {
            throw new IllegalStateException("There is no compact view available.");
        }
        return result;
    }


    @Nonnull
    public Iterable<View> provideFor(@Nullable Configuration configuration) {
        return provideFor(configuration != null ? configuration.getViews() : null);
    }

    @Nonnull
    public Iterable<View> provideFor(@Nullable Iterable<org.echocat.adam.configuration.view.View> original) {
        return provideInternalFor(original).values();
    }

    @Nonnull
    protected Map<String, View> provideInternalFor(@Nullable Configuration configuration) {
        return provideInternalFor(configuration != null ? configuration.getViews() : null);
    }

    @Nonnull
    protected Map<String, View> provideInternalFor(@Nullable Iterable<org.echocat.adam.configuration.view.View> original) {
        Map<String, View> result = _cache.get(original);
        if (result == null) {
            result = new LinkedHashMap<>();
            if (original != null) {
                for (final org.echocat.adam.configuration.view.View view : original) {
                    final String id = view.getId();
                    if (result.containsKey(id)) {
                        LOG.warn("Found multiple compact definition for id '" + id + "'. The duplicate one will be ignored.");
                    } else {
                        result.put(id, createViewFor(view));
                    }
                }
            }
            final View defaultView = result.get(DEFAULT_ID);
            if (defaultView == null) {
                result.put(DEFAULT_ID, createDefaultView());
            } else {
                final Set<String> elementIds = defaultView.getElementIds();
                if (!elementIds.equals(_groupProvider.getAllKnownElementIds())) {
                    result.put(ALL_ID, createAllView());
                }
            }
            if (!result.containsKey(COMPACT_ID)) {
                result.put(COMPACT_ID, createCompactView());
            }
            _cache.put(original, result);
        }
        return result;
    }

    @Nonnull
    protected Configuration getConfiguration() {
        return getConfigurationRepository().get();
    }

    @Nonnull
    public ConfigurationRepository getConfigurationRepository() {
        return _configurationRepository;
    }

    @Nonnull
    protected View createViewFor(@Nonnull org.echocat.adam.configuration.view.View original) {
        final Set<String> allowedElementIds = new HashSet<>();
        final List<Element> elements = original.getElements();
        if (elements != null) {
            for (final Element element : elements) {
                allowedElementIds.add(element.getId());
            }
        }
        return createViewFor(original, allowedElementIds);
    }

    @Nonnull
    protected View createViewFor(@Nonnull org.echocat.adam.configuration.view.View original, @Nonnull Set<String> allowedElementIds) {
        return new ViewImpl(original, createGroupsFor(allowedElementIds), allowedElementIds);
    }

    @Nonnull
    protected View createAllView() {
        return new AllView(ALL_ID);
    }

    @Nonnull
    protected View createDefaultView() {
        return new AllView(DEFAULT_ID);
    }

    @Nonnull
    protected View createCompactView() {
        return new StaticView(COMPACT_ID, createGroupsFor(COMPACT_ELEMENT_IDS), COMPACT_ELEMENT_IDS);
    }

    @Nonnull
    public List<Group> createGroupsFor(@Nonnull Set<String> allowedElementIds) {
        final List<Group> result = new ArrayList<>();
        for (final Group group : _groupProvider) {
            final Collection<ElementModel> models = filterElementsFor(group, allowedElementIds);
            if (!models.isEmpty()) {
                result.add(new GroupImpl(group, models));
            }
        }
        return asImmutableList(result);
    }

    @Nonnull
    protected Collection<ElementModel> filterElementsFor(@Nonnull Group group, @Nonnull Set<String> allowedElementIds) {
        final List<ElementModel> result = new ArrayList<>();
        for (final ElementModel elementModel : group) {
            final String id = elementModel.getId();
            if (allowedElementIds.contains(id)) {
                result.add(elementModel);
            }
        }
        return result;
    }

    protected class GroupImpl extends GroupSupport {

        @Nonnull
        private final Group _original;
        @Nonnull
        private final List<ElementModel> _selectedElements;

        public GroupImpl(@Nonnull Group original, @Nonnull Iterable<ElementModel> selectedElements) {
            _original = original;
            _selectedElements = asImmutableList(selectedElements);
        }

        @Nullable
        @Override
        public String getId() {
            return _original.getId();
        }

        @Nullable
        @Override
        public Map<Locale, Localization> getLocalizations() {
            return _original.getLocalizations();
        }

        @Override
        public Iterator<ElementModel> iterator() {
            return _selectedElements.iterator();
        }

    }

    protected class ViewImpl extends ViewSupport {

        @Nonnull
        private final org.echocat.adam.configuration.view.View _original;
        @Nonnull
        private final Set<String> _elementIds;
        @Nonnull
        private final Iterable<Group> _groups;

        public ViewImpl(@Nonnull org.echocat.adam.configuration.view.View original, @Nonnull Iterable<Group> groups, @Nonnull Set<String> elementIds) {
            _original = original;
            _elementIds = elementIds;
            _groups = asImmutableList(groups);
        }

        @Nonnull
        @Override
        public String getId() {
            return _original.getId();
        }

        @Nullable
        @Override
        protected Collection<org.echocat.adam.configuration.localization.Localization> getConfigurationBasedLocalizations() {
            return _original.getLocalizations();
        }

        @Nonnull
        @Override
        public ViewAccess getAccess() {
            return _accessProvider.provideFor(_original.getAccess());
        }

        @Override
        public Iterator<Group> iterator() {
            return _groups.iterator();
        }

        @Override
        public Set<String> getElementIds() {
            return _elementIds;
        }
    }

    protected class StaticView extends ViewSupport {

        @Nonnull
        private final String _id;
        @Nonnull
        private final Iterable<Group> _groups;
        @Nonnull
        private final Set<String> _elementIds;

        public StaticView(@Nonnull String id, @Nonnull Iterable<Group> groups, @Nonnull Set<String> elementIds) {
            _id = id;
            _groups = groups;
            _elementIds = elementIds;
        }

        @Nonnull
        @Override
        public String getId() { return _id; }

        @Nonnull
        @Override
        public ViewAccess getAccess() {
            return _accessProvider.allowAllViewAccess();
        }

        @Override
        public Iterator<Group> iterator() {
            return _groups.iterator();
        }

        @Override
        public Set<String> getElementIds() {
            return _elementIds;
        }

    }

    protected class AllView extends ViewSupport {

        @Nonnull
        private final String _id;

        public AllView(@Nonnull String id) {_id = id;}

        @Nonnull
        @Override
        public String getId() { return _id; }

        @Nonnull
        @Override
        public ViewAccess getAccess() {
            return _accessProvider.allowAllViewAccess();
        }

        @Override
        public Iterator<Group> iterator() {
            return _groupProvider.iterator();
        }

        @Override
        public Set<String> getElementIds() {
            return _groupProvider.getAllKnownElementIds();
        }
    }

    @Override
    public void destroy() throws Exception {
        c_instance = null;
    }
}
