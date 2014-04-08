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

package org.echocat.adam.profile.element;

import com.atlassian.confluence.user.UserDetailsManager;
import com.atlassian.user.User;
import org.apache.commons.collections15.map.LRUMap;
import org.echocat.adam.access.ViewEditAccess;
import org.echocat.adam.access.AccessProvider;
import org.echocat.adam.configuration.Configuration;
import org.echocat.adam.configuration.ConfigurationRepository;
import org.echocat.adam.configuration.localization.Localization;
import org.echocat.adam.configuration.profile.Element;
import org.echocat.adam.configuration.profile.Element.ContextAttribute;
import org.echocat.adam.configuration.profile.Group;
import org.echocat.adam.configuration.profile.Profile;
import org.echocat.adam.template.Template;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static java.util.Collections.synchronizedMap;
import static org.echocat.adam.profile.element.ElementModel.PERSONAL_INFORMATION_ELEMENT_ID;
import static org.echocat.adam.profile.element.ElementModel.Type.*;

public class ElementModelProvider {

    @Nonnull
    private final Map<Group, Iterable<ElementModel>> _cache = synchronizedMap(new LRUMap<Group, Iterable<ElementModel>>(3));

    @Nonnull
    private final UserDetailsManager _userDetailsManager;
    @Nonnull
    private final ConfigurationRepository _configurationRepository;
    @Nonnull
    private final AccessProvider _accessProvider;

    public ElementModelProvider(@Nonnull UserDetailsManager userDetailsManager, @Nonnull ConfigurationRepository configurationRepository, @Nonnull AccessProvider accessProvider) {
        _userDetailsManager = userDetailsManager;
        _configurationRepository = configurationRepository;
        _accessProvider = accessProvider;
    }

    @Nullable
    public ElementModel tryProvideFor(@Nonnull String id) {
        return tryProvideFor(getConfiguration(), id);
    }

    @Nullable
    public ElementModel tryProvideFor(@Nullable Configuration configuration, @Nonnull String id) {
        return configuration != null ? tryProvideFor(configuration.getProfile(), id) : null;
    }

    @Nullable
    public ElementModel tryProvideFor(@Nullable Profile profile, @Nonnull String id) {
        return profile != null ? tryProvideFor(profile.getGroups(), id) : null;
    }

    @Nullable
    public ElementModel tryProvideFor(@Nullable Iterable<Group> groups, @Nonnull String id) {
        ElementModel result = null;
        if (groups != null) {
            for (final Group group : groups) {
                final ElementModel candidate = tryProvideFor(group, id);
                if (candidate != null) {
                    result = candidate;
                    break;
                }
            }
        }
        return result;
    }

    @Nullable
    public ElementModel tryProvideFor(@Nullable Group group, @Nonnull String id) {
        ElementModel result = null;
        if (group != null) {
            final Iterable<ElementModel> elementModels = provideFor(group);
            for (final ElementModel candidate : elementModels) {
                if (id.equals(candidate.getId())) {
                    result = candidate;
                    break;
                }
            }
        }
        return result;
    }

    @Nonnull
    public Iterable<ElementModel> provideFor(@Nonnull Group group) {
        Iterable<ElementModel> result = _cache.get(group);
        if (result == null) {
            result = provideFor(group.getId(), group.getElements());
            _cache.put(group, result);
        }
        return result;
    }

    @Nonnull
    protected Iterable<ElementModel> provideFor(@Nonnull String groupId, @Nullable Iterable<Element> original) {
        final Collection<String> standardIds = getStandardIdsFor(groupId);
        final Set<String> missingStandardIds = new LinkedHashSet<>(standardIds);
        final List<ElementModel> result = new ArrayList<>();
        if (original != null) {
            for (final Element element : original) {
                final String id = element.getId();
                result.add(new WrappedElement(element, standardIds.contains(id)));
                missingStandardIds.remove(id);
            }
        }
        for (final String missingStandardId : missingStandardIds) {
            result.add(new DefaultStandardElement(missingStandardId));
        }
        return result;
    }

    @Nonnull
    protected Collection<String> getStandardIdsFor(@Nonnull String groupId) {
        final Set<String> result = new LinkedHashSet<>();
        if ("personal".equals(groupId)) {
            result.add(PERSONAL_INFORMATION_ELEMENT_ID);
            result.add(ElementModel.FULL_NAME_ELEMENT_ID);
            result.add(ElementModel.EMAIL_ELEMENT_ID);
        }
        result.addAll(_userDetailsManager.getProfileKeys(groupId));
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

    protected class DefaultStandardElement extends ElementModelSupport {

        @Nonnull
        private final String _id;

        public DefaultStandardElement(@Nonnull String id) {
            _id = id;
        }

        @Override
        public boolean isStandard() {
            return true;
        }

        @Override
        public boolean isVisibleOnOverviews() {
            final String id = getId();
            return EMAIL_ELEMENT_ID.equals(id)
                || FULL_NAME_ELEMENT_ID.equals(id);
        }

        @Override
        public boolean isDefaultForReports() {
            final String id = getId();
            return EMAIL_ELEMENT_ID.equals(id)
                || PHONE_ELEMENT_ID.equals(id)
                || FULL_NAME_ELEMENT_ID.equals(id);
        }

        @Nullable
        @Override
        public List<String> getContextAttributeKeys() {
            return null;
        }

        @Nonnull
        @Override
        public String getId() {
            return _id;
        }

        @Nonnull
        @Override
        public ViewEditAccess getAccess() {
            return new StandardElementAccess();
        }

        @Nonnull
        @Override
        public Type getType() {
            final String id = getId();
            final Type result;
            if (PERSONAL_INFORMATION_ELEMENT_ID.equals(id)) {
                result = wikiMarkup;
            } else if (EMAIL_ELEMENT_ID.equals(id)) {
                result = emailAddress;
            } else if (WEBSITE_ELEMENT_ID.equals(id)) {
                result = url;
            } else if (PHONE_ELEMENT_ID.equals(id)) {
                result = phoneNumber;
            } else {
                result = singleLineText;
            }
            return result;
        }

        @Nullable
        @Override
        public Template getTemplate() {
            return null;
        }

        @Override
        public boolean isSearchable() {
            return true;
        }

        @Override
        public boolean isVisibleIfEmpty() {
            return false;
        }

    }

    protected class StandardElementAccess implements ViewEditAccess {

        @Nonnull
        @Override
        public Visibility checkView(@Nonnull User forUser, @Nullable User target) {
            return Visibility.allowed;
        }

        @Nonnull
        @Override
        public Editeditability checkEdit(@Nonnull User forUser, @Nullable User target) {
            return Editeditability.allowed;
        }

    }

    protected class WrappedElement extends ElementModelSupport {

        @Nonnull
        private final Element _source;
        private final boolean _standard;

        public WrappedElement(@Nonnull Element source, boolean standard) {
            _source = source;
            _standard = standard;
        }

        @Override
        public boolean isStandard() {
            return _standard;
        }

        @Override
        public boolean isDefaultForReports() {
            final Boolean value = _source.getDefaultForReports();
            final boolean result;
            if (value != null) {
                result = value;
            } else {
                final String id = getId();
                result = EMAIL_ELEMENT_ID.equals(id)
                    || PHONE_ELEMENT_ID.equals(id)
                    || FULL_NAME_ELEMENT_ID.equals(id);
            }
            return result;
        }

        @Override
        public boolean isVisibleOnOverviews() {
            final Boolean value = _source.getVisibleOnOverviews();
            final boolean result;
            if (value != null) {
                result = value;
            } else {
                final String id = getId();
                result = EMAIL_ELEMENT_ID.equals(id)
                    || FULL_NAME_ELEMENT_ID.equals(id);
            }
            return result;
        }

        @Override
        @Nonnull
        public String getId() {
            return _source.getId();
        }

        @Nullable
        @Override
        public List<String> getContextAttributeKeys() {
            final List<String> result;
            final List<ContextAttribute> contextAttributes = _source.getContextAttributes();
            if (contextAttributes != null) {
                result = new ArrayList<>(contextAttributes.size());
                for (final ContextAttribute contextAttribute : contextAttributes) {
                    result.add(contextAttribute.getId());
                }
            } else {
                result = null;
            }
            return result;
        }

        @Override
        @Nonnull
        public ViewEditAccess getAccess() {
            return _accessProvider.provideFor(_source.getAccess());
        }

        @Override
        @Nonnull
        public Type getType() {
            return _source.getType();
        }

        @Override
        @Nullable
        public Template getTemplate() {
            return _source.getTemplate();
        }

        @Override
        public boolean isSearchable() {
            return _source.isSearchable();
        }

        @Override
        public boolean isVisibleIfEmpty() {
            return _source.isVisibleIfEmpty();
        }

        @Nullable
        @Override
        protected Collection<Localization> getConfigurationBasedLocalizations() {
            return _source.getLocalizations();
        }

    }

}
