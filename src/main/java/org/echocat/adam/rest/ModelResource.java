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

package org.echocat.adam.rest;

import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.user.User;
import net.jcip.annotations.Immutable;
import org.echocat.adam.localization.LocalizationHelper;
import org.echocat.adam.profile.Group;
import org.echocat.adam.profile.GroupProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static javax.ws.rs.core.Response.ok;

@Path("/model")
public class ModelResource {

    @Nonnull
    private final GroupProvider _groupProvider;
    @Nonnull
    private final LocalizationHelper _localizationHelper;
    @Nonnull
    private final LocaleManager _localeManager;

    public ModelResource(@Nonnull GroupProvider groupProvider, @Nonnull LocalizationHelper localizationHelper, @Nonnull LocaleManager localeManager) {
        _groupProvider = groupProvider;
        _localizationHelper = localizationHelper;
        _localeManager = localeManager;
    }

    @GET
    @Path("/profile")
    @AnonymousAllowed
    @Produces({"application/json"})
    public Response getProfile() {
        final ConfluenceUser currentUser = AuthenticatedUserThreadLocal.get();
        final Locale locale = currentUser != null ? _localeManager.getLocale(currentUser) : null;
        return ok(new ProfileModel(_localizationHelper, _groupProvider, locale, currentUser)).build();
    }

    @Immutable
    @XmlRootElement
    public static class ProfileModel {

        @Nonnull
        private final LocalizationHelper _localizationHelper;
        @Nonnull
        private final GroupProvider _groupProvider;
        @Nullable
        private final Locale _locale;
        @Nullable
        private final User _currentUser;

        public ProfileModel(@Nonnull LocalizationHelper localizationHelper, @Nonnull GroupProvider groupProvider, @Nullable Locale locale, @Nullable User currentUser) {
            _localizationHelper = localizationHelper;
            _groupProvider = groupProvider;
            _locale = locale;
            _currentUser = currentUser;
        }

        @Nonnull
        @XmlElement
        public List<GroupModel> getGroups() {
            final List<GroupModel> result = new ArrayList<>();
            for (final Group group : _groupProvider) {
                if (group.getAccess().checkView(_currentUser, null).isViewAllowed()) {
                    result.add(new GroupModel(_localizationHelper, group, _locale, _currentUser));
                }
            }
            return result;
        }
    }

    @Immutable
    public static class GroupModel {

        @Nonnull
        private final LocalizationHelper _localizationHelper;
        @Nonnull
        private final Group _group;
        @Nullable
        private final Locale _locale;
        @Nullable
        private final User _currentUser;

        public GroupModel(@Nonnull LocalizationHelper localizationHelper, @Nonnull Group group, @Nullable Locale locale, @Nullable User currentUser) {
            _localizationHelper = localizationHelper;
            _group = group;
            _locale = locale;
            _currentUser = currentUser;
        }

        @Nonnull
        @XmlElement
        public String getId() {
            return _group.getId();
        }

        @Nonnull
        @XmlElement
        public String getLabel() {
            return _localizationHelper.getTitleFor(_group, _locale);
        }

        @Nullable
        @XmlElement
        public String getHelpText() {
            return _localizationHelper.findHelpTextFor(_group, _locale);
        }

        @XmlElement
        @Nonnull
        public List<ElementModel> getElements() {
            final List<ElementModel> result = new ArrayList<>();
            for (final org.echocat.adam.profile.element.ElementModel elementModel : _group) {
                if (elementModel.getAccess().checkView(_currentUser, null).isViewAllowed()) {
                    result.add(new ElementModel(_localizationHelper, elementModel, _locale));
                }
            }
            return result;
        }
    }

    @Immutable
    public static class ElementModel {

        @Nonnull
        private final LocalizationHelper _localizationHelper;
        @Nonnull
        private final org.echocat.adam.profile.element.ElementModel _element;
        @Nullable
        private final Locale _locale;

        public ElementModel(@Nonnull LocalizationHelper localizationHelper, @Nonnull org.echocat.adam.profile.element.ElementModel element, @Nullable Locale locale) {
            _localizationHelper = localizationHelper;
            _element = element;
            _locale = locale;
        }

        @Nonnull
        @XmlElement
        public String getId() {
            return _element.getId();
        }

        @Nonnull
        @XmlElement
        public String getLabel() {
            return _localizationHelper.getTitleFor(_element, _locale);
        }

        @Nullable
        @XmlElement
        public String getHelpText() {
            return _localizationHelper.findHelpTextFor(_element, _locale);
        }

    }

}