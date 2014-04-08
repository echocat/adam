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

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.user.User;
import org.echocat.adam.localization.LocalizationHelper;
import org.echocat.adam.profile.element.ElementRenderer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.atlassian.confluence.macro.Macro.BodyType.NONE;
import static com.atlassian.confluence.renderer.radeox.macros.MacroUtils.defaultVelocityContext;
import static com.atlassian.confluence.security.Permission.VIEW;
import static com.atlassian.confluence.security.PermissionManager.TARGET_PEOPLE_DIRECTORY;
import static com.atlassian.confluence.util.velocity.VelocityUtils.getRenderedTemplate;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.echocat.adam.profile.UserProfileMacro.Format.full;
import static org.echocat.jomon.runtime.CollectionUtils.asImmutableList;

public class UserProfileMacro implements Macro {

    @Nonnull
    protected static final String TEMPLATE_NAME_SUFFIX = ".vm";
    @Nonnull
    protected static final String TEMPLATE_NAME_PREFIX = UserProfileMacro.class.getName().replace('.', '/');

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

    public UserProfileMacro(@Nonnull GroupProvider groupProvider, @Nonnull ProfileProvider profileProvider, @Nonnull UserAccessor userAccessor, @Nonnull LocalizationHelper localizationHelper, @Nonnull ElementRenderer elementRenderer, @Nonnull GroupRenderer groupRenderer, @Nonnull LocaleManager localeManager, @Nonnull PermissionManager permissionManager) {
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
    public String execute(@Nonnull Map<String, String> parameters, @Nullable String body, @Nonnull ConversionContext conversionContext) throws MacroExecutionException {
        final User currentUser = AuthenticatedUserThreadLocal.get();
        final User user = findUserFor(parameters);
        final Profile profile = findProfileFor(user);
        final List<Group> groups = asImmutableList(_groupProvider);
        final Format format = findFormatFor(parameters);
        final Locale locale = getLocaleFor(currentUser);

        final Map<String, Object> context = defaultVelocityContext();
        context.putAll(parameters);
        context.put("conversionContext", conversionContext);
        context.put("currentUser", currentUser);
        context.put("user", user);
        context.put("profile", profile);
        context.put("groups", groups);
        context.put("format", format);
        context.put("elementRenderer", _elementRenderer);
        context.put("groupRenderer", _groupRenderer);
        context.put("localizationHelper", _localizationHelper);
        context.put("locale", locale);

        context.put("wikiStyleRenderer", _localeManager);
        context.put("rendererContext", conversionContext.getPageContext());

        final String templateName = getTemplateNameFor(context, format, user);
        return getRenderedTemplate(templateName, context);
    }

    @Nonnull
    protected Locale getLocaleFor(@Nullable User user) {
        return _localizationHelper.getLocaleFor(user);
    }

    @Nullable
    private Format findFormatFor(@Nonnull Map<String, String> parameters) {
        final String plainFormat = parameters.get("format");
        Format result;
        if (isEmpty(plainFormat)) {
            result = full;
        } else {
            try {
                result = Format.valueOf(plainFormat);
            } catch (final IllegalArgumentException ignored) {
                result = null;
            }
        }
        return result;
    }

    @Nonnull
    protected String getTemplateNameFor(@Nonnull Map<String, Object> context, @Nullable Format format, @Nullable User user) {
        final ConfluenceUser currentUser = AuthenticatedUserThreadLocal.get();
        final String variant;
        if (format == null) {
            variant = ".illegalFormat";
        } else if ((user == null || !user.equals(currentUser)) && !_permissionManager.hasPermission(currentUser, VIEW, TARGET_PEOPLE_DIRECTORY)) {
            variant = ".accessDenied";
        } else {
            final Object username = context.get("username");
            if (username == null || username.toString().isEmpty()) {
                variant = ".missingUsername";
            } else if (context.get("user") == null) {
                variant = ".unknownUser";
            } else {
                variant = "." + format;
            }
        }
        return TEMPLATE_NAME_PREFIX + variant + TEMPLATE_NAME_SUFFIX;
    }

    @Nullable
    protected User findUserFor(@Nonnull Map<String, String> parameters) {
        final String userName = parameters.get("username");
        return isEmpty(userName) ? null : _userAccessor.getUserByName(userName);
    }

    @Nullable
    protected Profile findProfileFor(@Nullable User user) {
        return user != null ? _profileProvider.provideFor(user) : null;
    }

    @Override
    public BodyType getBodyType() {
        return NONE;
    }

    @Override
    public OutputType getOutputType() {
        return OutputType.BLOCK;
    }

    public static enum Format {
        elementsOnly,
        full,
        hover
    }

}
