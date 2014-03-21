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

package org.echocat.adam.directory;

import com.atlassian.crowd.directory.ldap.mapper.attribute.AttributeMapper;
import org.echocat.adam.profile.Group;
import org.echocat.adam.profile.GroupProvider;
import org.echocat.adam.profile.element.ElementModel;
import org.springframework.ldap.core.DirContextAdapter;

import javax.annotation.Nonnull;
import javax.naming.directory.Attribute;
import java.util.*;

import static java.util.Collections.singleton;
import static org.echocat.jomon.runtime.CollectionUtils.addAll;
import static org.echocat.jomon.runtime.CollectionUtils.asImmutableList;
import static org.echocat.jomon.runtime.CollectionUtils.asImmutableSet;

public class DirectoryHelper {

    @Nonnull
    private final GroupProvider _groupProvider;

    public DirectoryHelper(@Nonnull GroupProvider groupProvider) {
        _groupProvider = groupProvider;
    }

    @Nonnull
    public List<AttributeMapper> extendAttributeMappers(@Nonnull List<AttributeMapper> original) {
        final List<AttributeMapper> result = new ArrayList<>(original);
        for (final String contextKey : getAllAttributeKeys()) {
            result.add(new AttributeMapperImpl(contextKey));
        }
        return result;
    }

    @Nonnull
    public Set<String> getAllAttributeKeys() {
        final Set<String> result = new HashSet<>();
        for (final Group group : _groupProvider) {
            for (final ElementModel elementModel : group) {
                addAll(result, elementModel.getContextAttributeKeys());
            }
        }
        return asImmutableSet(result);
    }

    @Nonnull
    public List<ElementModel> getAllElementModels() {
        final List<ElementModel> result = new ArrayList<>();
        for (final Group group : _groupProvider) {
            for (final ElementModel elementModel : group) {
                addAll(result, elementModel);
            }
        }
        return asImmutableList(result);
    }

    protected class AttributeMapperImpl implements AttributeMapper {

        @Nonnull
        private final String _key;

        public AttributeMapperImpl(@Nonnull String key) {
            _key = key;
        }

        @Nonnull
        @Override
        public String getKey() {
            return _key;
        }

        @Override
        @Nonnull
        public Set<String> getValues(@Nonnull DirContextAdapter context) throws Exception {
            final Attribute attribute = context.getAttributes().get(getKey());
            final Object value = attribute != null ? attribute.get() : null;
            return value != null ? singleton(value.toString()) : Collections.<String>emptySet();
        }

        @Override
        @Nonnull
        public Set<String> getRequiredLdapAttributes() {
            return singleton(getKey());
        }
    }
}