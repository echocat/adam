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

import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.plugin.web.model.WebPanel;
import com.atlassian.user.User;
import org.echocat.adam.localization.LocalizationHelper;
import org.echocat.adam.profile.element.ElementRenderer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import static org.echocat.adam.profile.UserProfileMacro.Format.elementsOnly;

public class ViewMyUserProfileWebPanel implements WebPanel {

    @Nonnull
    private final GroupProvider _groupProvider;
    @Nonnull
    private final ProfileProvider _profileProvider;
    @Nonnull
    private final UserAccessor _userAccessor;
    @Nonnull
    private final LocalizationHelper _localizationHelper;
    @Nonnull
    private final ElementRenderer _elementRenderer;
    @Nonnull
    private final GroupRenderer _groupRenderer;
    @Nonnull
    private final LocaleManager _localeManager;
    @Nonnull
    private final PermissionManager _permissionManager;

    public ViewMyUserProfileWebPanel(@Nonnull GroupProvider groupProvider, @Nonnull ProfileProvider profileProvider, @Nonnull UserAccessor userAccessor, @Nonnull LocalizationHelper localizationHelper, @Nonnull ElementRenderer elementRenderer, @Nonnull GroupRenderer groupRenderer, @Nonnull LocaleManager localeManager, @Nonnull PermissionManager permissionManager) {
        _groupProvider = groupProvider;
        _profileProvider = profileProvider;
        _userAccessor = userAccessor;
        _localizationHelper = localizationHelper;
        _elementRenderer = elementRenderer;
        _groupRenderer = groupRenderer;
        _localeManager = localeManager;
        _permissionManager = permissionManager;
    }

    @Override
    public String getHtml(@Nullable Map<String, Object> context) {
        final Map<String, String> propertiesToForward = new HashMap<>();
        if (context !=  null) {
            final Object user = context.get("user");
            if (user instanceof User) {
                propertiesToForward.put("username", ((Principal) user).getName());
            }
        }
        propertiesToForward.put("format", elementsOnly.toString());
        return render(propertiesToForward);
    }

    @Override
    public void writeHtml(@Nonnull Writer writer, @Nullable Map<String, Object> context) throws IOException {
        writer.write(getHtml(context));
    }

    @Nonnull
    public String render(@Nonnull Map<String, String> parameters) {
        try {
            return getUserProfileMacro().execute(parameters, null, new DefaultConversionContext(null));
        } catch (final MacroExecutionException e) {
            throw new RuntimeException("Could not render marco using: " + parameters, e);
        }
    }

    @Nonnull
    public UserProfileMacro getUserProfileMacro() {
        return new UserProfileMacro(_groupProvider, _profileProvider, _userAccessor, _localizationHelper, _elementRenderer, _groupRenderer, _localeManager, _permissionManager);
    }

}
