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
import com.atlassian.confluence.plugins.rest.dto.UserDto;
import com.atlassian.confluence.plugins.rest.dto.UserDtoFactory;
import com.atlassian.confluence.plugins.rest.entities.UserPreferencesDto;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.user.User;
import net.jcip.annotations.Immutable;
import org.echocat.adam.localization.LocalizationHelper;
import org.echocat.adam.profile.*;
import org.echocat.adam.profile.element.ElementModel;
import org.echocat.adam.profile.element.ElementModel.Type;
import org.echocat.adam.profile.element.ElementRenderer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.atlassian.confluence.user.AuthenticatedUserThreadLocal.isAnonymousUser;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static org.echocat.jomon.runtime.CollectionUtils.asImmutableList;

@Path("/profile")
public class ProfileResource {

    @Nonnull
    private final UserDtoFactory _userDtoFactory;
    @Nonnull
    private final UserAccessor _userAccessor;
    @Nonnull
    private final UserHelper _userHelper;
    @Nonnull
    private final GroupProvider _groupProvider;
    @Nonnull
    private final GroupRenderer _groupRenderer;
    @Nonnull
    private final ElementRenderer _elementRenderer;
    @Nonnull
    private final ProfileProvider _profileProvider;
    @Nonnull
    private final LocalizationHelper _localizationHelper;
    @Nonnull
    private final LocaleManager _localeManager;

    public ProfileResource(@Nonnull UserDtoFactory userDtoFactory, @Nonnull UserAccessor userAccessor, @Nonnull UserHelper userHelper, @Nonnull GroupProvider groupProvider, @Nonnull GroupRenderer groupRenderer, @Nonnull ElementRenderer elementRenderer, @Nonnull ProfileProvider profileProvider, @Nonnull LocalizationHelper localizationHelper, @Nonnull LocaleManager localeManager) {
        _userDtoFactory = userDtoFactory;
        _userAccessor = userAccessor;
        _userHelper = userHelper;
        _groupProvider = groupProvider;
        _groupRenderer = groupRenderer;
        _elementRenderer = elementRenderer;
        _profileProvider = profileProvider;
        _localizationHelper = localizationHelper;
        _localeManager = localeManager;
    }

    @GET
    @Path("/{username}")
    @AnonymousAllowed
    @Produces({"application/json"})
    public Response getProfile(@PathParam("username") String username) {
        final Response response;
        final ConfluenceUser user = _userAccessor.getUserByName(username);
        final ConfluenceUser currentUser = AuthenticatedUserThreadLocal.get();
        final Locale locale = currentUser != null ? _localeManager.getLocale(currentUser) : null;
        if (user == null || !_userHelper.isProfileViewPermitted()) {
            response = getNotFoundResponse();
        } else {
            final UserDto original = _userDtoFactory.getUserDto(user);
            final ExtendedUserDto dto = new ExtendedUserDto(original, createGroupsFor(currentUser, user, locale));
            response = ok(dto).build();
        }
        return response;
    }

    @Nonnull
    protected Iterable<Group> createGroupsFor(@Nullable User currentUser, @Nonnull User user, @Nullable Locale locale) {
        return createGroupsFor(currentUser, _profileProvider.provideFor(user), locale);
    }

    @Nonnull
    protected Iterable<Group> createGroupsFor(@Nullable User currentUser, @Nonnull Profile profile, @Nullable Locale locale) {
        final List<Group> result = new ArrayList<>();
        for (final org.echocat.adam.profile.Group group : _groupProvider) {
            if (_groupRenderer.isRenderOfViewAllowedFor(group, currentUser, profile)) {
                final String id = group.getId();
                final String label = _localizationHelper.getTitleFor(group, locale);
                result.add(new Group(id, label, createElementsFor(group, currentUser, profile, locale)));
            }
        }
        return result;
    }

    @Nonnull
    protected Iterable<Element> createElementsFor(@Nonnull org.echocat.adam.profile.Group group, @Nullable User currentUser, @Nonnull Profile profile, @Nullable Locale locale) {
        final List<Element> result = new ArrayList<>();
        for (final ElementModel elementModel : group) {
            if (_elementRenderer.isRenderOfViewAllowedFor(elementModel, currentUser, profile)) {
                final String id = elementModel.getId();
                final String label = _localizationHelper.getTitleFor(elementModel, locale);
                final Type type = elementModel.getType();
                final String valueAsHtml = _elementRenderer.renderViewXhtml(elementModel, currentUser, profile);
                result.add(new Element(valueAsHtml, type, label, id));
            }
        }
        return result;
    }

    @Nonnull
    protected Response getNotFoundResponse() {
        return status(isAnonymousUser() ? UNAUTHORIZED : NOT_FOUND).build();
    }

    @Immutable
    @XmlRootElement
    public static class ExtendedUserDto {

        @Nonnull
        private final UserDto _original;
        @Nonnull
        private final List<Group> _groups;

        public ExtendedUserDto(@Nonnull UserDto original, @Nonnull Iterable<Group> groups) {
            _original = original;
            _groups = asImmutableList(groups);
        }

        @XmlElement
        public String getUserName() {return _original.getUserName();}

        @XmlElement
        public String getPosition() {return _original.getPosition();}

        @XmlElement
        public boolean isAnonymous() {return _original.isAnonymous();}

        @XmlElement
        public String getEmail() {return _original.getEmail();}

        @XmlElement
        public UserPreferencesDto getUserPreferences() {return _original.getUserPreferences();}

        @XmlElement
        public String getAbout() {return _original.getAbout();}

        @XmlElement
        public String getDepartment() {return _original.getDepartment();}

        @XmlElement
        public String getUrl() {return _original.getUrl();}

        @XmlElement
        public String getLocation() {return _original.getLocation();}

        @XmlElement
        public String getPhone() {return _original.getPhone();}

        @XmlElement
        public String getAvatarUrl() {return _original.getAvatarUrl();}

        @XmlElement
        public String getFullName() {return _original.getFullName();}

        @Nonnull
        @XmlElement
        public List<Group> getGroups() {
            return _groups;
        }

    }

    @Immutable
    public static class Group {

        @Nonnull
        private final String _id;
        @Nonnull
        private final String _label;
        @Nonnull
        private final List<Element> _elements;

        public Group(@Nonnull String id, @Nonnull String label, @Nonnull Iterable<Element> elements) {
            _id = id;
            _label = label;
            _elements = asImmutableList(elements);
        }

        @Nonnull
        @XmlElement
        public String getId() {
            return _id;
        }

        @Nonnull
        @XmlElement
        public String getLabel() {
            return _label;
        }

        @Nonnull
        @XmlElement
        public List<Element> getElements() {
            return _elements;
        }
    }

    @Immutable
    public static class Element {

        @Nonnull
        private final String _id;
        @Nonnull
        private final String _label;
        @Nonnull
        private final Type _type;
        @Nullable
        private final String _valueAsHtml;

        public Element(@Nullable String valueAsHtml, @Nonnull Type type, @Nonnull String label, @Nonnull String id) {
            _valueAsHtml = valueAsHtml;
            _type = type;
            _label = label;
            _id = id;
        }

        @Nonnull
        @XmlElement
        public String getId() {
            return _id;
        }

        @Nonnull
        @XmlElement
        public String getLabel() {
            return _label;
        }

        @Nonnull
        @XmlElement
        public Type getType() {
            return _type;
        }

        @Nullable
        @XmlElement
        public String getValueAsHtml() {
            return _valueAsHtml;
        }

    }
}