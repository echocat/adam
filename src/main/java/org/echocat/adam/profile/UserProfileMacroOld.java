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
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.TokenType;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.Macro;
import com.atlassian.renderer.v2.macro.MacroException;
import com.atlassian.renderer.v2.macro.WysiwygBodyType;
import org.echocat.adam.localization.LocalizationHelper;
import org.echocat.adam.profile.element.ElementRenderer;
import org.echocat.adam.view.ViewProvider;

import javax.annotation.Nonnull;
import java.util.Map;

import static com.atlassian.renderer.TokenType.BLOCK;
import static com.atlassian.renderer.v2.RenderMode.NO_RENDER;
import static com.atlassian.renderer.v2.macro.WysiwygBodyType.PREFORMAT;

@SuppressWarnings("rawtypes")
public class UserProfileMacroOld implements Macro {

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
    @Nonnull
    private final ViewProvider _viewProvider;

    public UserProfileMacroOld(@Nonnull GroupProvider groupProvider, @Nonnull ProfileProvider profileProvider, @Nonnull UserAccessor userAccessor, @Nonnull LocalizationHelper localizationHelper, @Nonnull ElementRenderer elementRenderer, @Nonnull GroupRenderer groupRenderer, @Nonnull LocaleManager localeManager, @Nonnull PermissionManager permissionManager, @Nonnull ViewProvider viewProvider) {
        _groupProvider = groupProvider;
        _profileProvider = profileProvider;
        _userAccessor = userAccessor;
        _localizationHelper = localizationHelper;
        _elementRenderer = elementRenderer;
        _groupRenderer = groupRenderer;
        _localeManager = localeManager;
        _permissionManager = permissionManager;
        _viewProvider = viewProvider;
    }

    @Override
    public TokenType getTokenType(Map map, String s, RenderContext renderContext) {
        return BLOCK;
    }

    @Override
    public boolean isInline() {
        return false;
    }

    @Override
    public boolean hasBody() {
        return false;
    }

    @Override
    public RenderMode getBodyRenderMode() {
        return NO_RENDER;
    }

    @Override
    public String execute(Map map, String body, RenderContext renderContext) throws MacroException {
        final UserProfileMacro macro = new UserProfileMacro(_profileProvider, _userAccessor, _localizationHelper, _elementRenderer, _groupRenderer, _localeManager, _permissionManager, _viewProvider);
        try {
            // noinspection unchecked
            return macro.execute(map, body, new DefaultConversionContext(renderContext));
        } catch (final MacroExecutionException e) {
            throw new MacroException(e.getMessage(), e);
        }
    }

    @Override
    public boolean suppressSurroundingTagDuringWysiwygRendering() {
        return false;
    }

    @Override
    public boolean suppressMacroRenderingDuringWysiwyg() {
        return true;
    }

    @Override
    public WysiwygBodyType getWysiwygBodyType() {
        return PREFORMAT;
    }
}
