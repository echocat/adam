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
import org.echocat.adam.view.ViewProvider;
import org.echocat.adam.rest.ModelResource.GroupModel;

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

@Path("/view")
public class ViewResource {

    @Nonnull
    private final ViewProvider _viewProvider;
    @Nonnull
    private final LocalizationHelper _localizationHelper;
    @Nonnull
    private final LocaleManager _localeManager;

    public ViewResource(@Nonnull ViewProvider viewProvider, @Nonnull LocalizationHelper localizationHelper, @Nonnull LocaleManager localeManager) {
        _viewProvider = viewProvider;
        _localizationHelper = localizationHelper;
        _localeManager = localeManager;
    }

    @GET
    @Path("/")
    @AnonymousAllowed
    @Produces({"application/json"})
    public Response getViews() {
        final ConfluenceUser currentUser = AuthenticatedUserThreadLocal.get();
        final Locale locale = currentUser != null ? _localeManager.getLocale(currentUser) : null;
        final List<View> views = new ArrayList<>();
        for (final org.echocat.adam.view.View view : _viewProvider) {
            views.add(new View(_localizationHelper, view, locale, currentUser));
        }
        return ok(views).build();
    }

    @Immutable
    @XmlRootElement
    public static class View {

        @Nonnull
        private final LocalizationHelper _localizationHelper;
        @Nonnull
        private final org.echocat.adam.view.View _original;
        @Nullable
        private final Locale _locale;
        @Nullable
        private final User _currentUser;

        public View(@Nonnull LocalizationHelper localizationHelper, @Nonnull org.echocat.adam.view.View original, @Nullable Locale locale, @Nullable User currentUser) {
            _localizationHelper = localizationHelper;
            _original = original;
            _locale = locale;
            _currentUser = currentUser;
        }

        @Nonnull
        @XmlElement
        public String getId() {
            return _original.getId();
        }

        @Nonnull
        @XmlElement
        public String getLabel() {
            return _localizationHelper.getTitleFor(_original, _locale);
        }

        @Nullable
        @XmlElement
        public String getHelpText() {
            return _localizationHelper.findHelpTextFor(_original, _locale);
        }

        @Nonnull
        @XmlElement
        public List<GroupModel> getGroups() {
            final List<GroupModel> result = new ArrayList<>();
            for (final Group group : _original) {
                if (group.getAccess().checkView(_currentUser, null).isViewAllowed()) {
                    result.add(new GroupModel(_localizationHelper, group, _locale, _currentUser));
                }
            }
            return result;
        }
    }

}