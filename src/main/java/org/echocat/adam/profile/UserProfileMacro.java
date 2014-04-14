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
import org.echocat.adam.view.View;
import org.echocat.adam.view.ViewProvider;
import org.echocat.jomon.runtime.util.Hints;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.atlassian.confluence.macro.Macro.BodyType.NONE;
import static com.atlassian.confluence.renderer.radeox.macros.MacroUtils.defaultVelocityContext;
import static com.atlassian.confluence.security.Permission.VIEW;
import static com.atlassian.confluence.security.PermissionManager.TARGET_PEOPLE_DIRECTORY;
import static com.atlassian.confluence.util.velocity.VelocityUtils.getRenderedTemplate;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Enum.valueOf;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.uncapitalize;
import static org.echocat.adam.profile.element.ElementRenderer.enableUserLinkIfPossible;
import static org.echocat.adam.profile.element.ElementRenderer.fullNameTagName;
import static org.echocat.jomon.runtime.CollectionUtils.addAll;
import static org.echocat.jomon.runtime.StringUtils.split;

public class UserProfileMacro implements Macro {

    @Nonnull
    protected static final String TEMPLATE_NAME_SUFFIX = ".vm";
    @Nonnull
    protected static final String TEMPLATE_NAME_PREFIX = UserProfileMacro.class.getName().replace('.', '/');
    @Nonnull
    protected static final Pattern EXTRACT_VIEW_NAME_PATTERN = compile("\\$\\$view:([a-zA-Z0-9_\\-]+)\\$\\$");

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

    public UserProfileMacro(@Nonnull ProfileProvider profileProvider, @Nonnull UserAccessor userAccessor, @Nonnull LocalizationHelper localizationHelper, @Nonnull ElementRenderer elementRenderer, @Nonnull GroupRenderer groupRenderer, @Nonnull LocaleManager localeManager, @Nonnull PermissionManager permissionManager, @Nonnull ViewProvider viewProvider) {
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
    public String execute(@Nonnull Map<String, String> parameters, @Nullable String body, @Nonnull ConversionContext conversionContext) throws MacroExecutionException {
        final User currentUser = AuthenticatedUserThreadLocal.get();
        final User user = findUserFor(parameters);
        final Profile profile = findProfileFor(user);
        final Set<String> allowedElementIds = getAllowedElementIdsBy(parameters, currentUser);
        final List<Group> groups = _viewProvider.createGroupsFor(allowedElementIds);
        final Locale locale = getLocaleFor(currentUser);

        final Map<String, Object> context = defaultVelocityContext();
        context.putAll(parameters);
        context.put("conversionContext", conversionContext);
        context.put("currentUser", currentUser);
        context.put("user", user);
        context.put("profile", profile);
        context.put("groups", groups);
        context.put("elementRenderer", _elementRenderer);
        context.put("groupRenderer", _groupRenderer);
        context.put("localizationHelper", _localizationHelper);
        context.put("locale", locale);
        context.put("allowedElementIds", allowedElementIds);
        context.put("border", getValueFor(parameters, Border.visible, Border.visible, Border.hidden));
        context.put("avatar", getValueFor(parameters, Avatar.visible, Avatar.visible, Avatar.hidden));
        context.put("groupLabels", getValueFor(parameters, GroupLabels.visible, GroupLabels.visible, GroupLabels.hidden));
        context.put("labels", getValueFor(parameters, Labels.visible, Labels.visible, Labels.hidden));
        context.put("hints", getHintsFor(parameters));

        context.put("wikiStyleRenderer", _localeManager);
        context.put("rendererContext", conversionContext.getPageContext());

        final String templateName = getTemplateNameFor(context, user);
        return getRenderedTemplate(templateName, context);
    }

    @Nonnull
    protected Hints getHintsFor(@Nonnull Map<String, String> parameters) {
        final Hints hints = new Hints();
        final String plainEnableUserLinkIfPossible = parameters.get("enableUserLinkIfPossible");
        if (plainEnableUserLinkIfPossible != null) {
            hints.set(enableUserLinkIfPossible, Boolean.valueOf(plainEnableUserLinkIfPossible));
        }
        hints.set(fullNameTagName, parameters.get("fullNameTagName"));
        return hints;
    }

    @Nonnull
    protected <T extends Enum<T>> T getValueFor(@Nonnull Map<String, String> context, @Nonnull T defaultValue, @Nonnull T trueValue, @Nonnull T falseValue) {
        return getValueFor(context, defaultValue, trueValue, falseValue, uncapitalize(defaultValue.getClass().getSimpleName()));
    }

    @Nonnull
    protected <T extends Enum<T>> T getValueFor(@Nonnull Map<String, String> context, @Nonnull T defaultValue, @Nonnull T trueValue, @Nonnull T falseValue, @Nonnull String key) {
        final String plain = context.get(key);
        T result;
        try {
            // noinspection unchecked
            result = plain != null ? (T) valueOf(defaultValue.getClass(), plain) : defaultValue;
        } catch (final IllegalArgumentException ignored) {
            if (TRUE.toString().equalsIgnoreCase(plain)) {
                result = trueValue;
            } else if (FALSE.toString().equalsIgnoreCase(plain)) {
                result = falseValue;
            } else {
                result = defaultValue;
            }
        }
        return result;
    }

    @Nonnull
    protected Set<String> getAllowedElementIdsBy(@Nonnull Map<String, String> parameters, @Nullable User currentUser) {
        final String elements = parameters.get("elements");
        return getAllowedElementIdsBy(elements, currentUser);
    }

    @Nonnull
    protected Set<String> getAllowedElementIdsBy(@Nullable String elements, @Nullable User currentUser) {
        final View view = tryExtractViewFrom(elements, currentUser);
        final Set<String> result = new LinkedHashSet<>();
        if (view != null) {
            result.addAll(view.getElementIds());
        } else {
            addAll(result, split(elements, ",", false, true));
        }
        return result;
    }

    @Nullable
    protected View tryExtractViewFrom(@Nullable String elements, @Nullable User currentUser) {
        View result;
        if (elements == null) {
            result = _viewProvider.provideDefault();
        } else {
            final Matcher matcher = EXTRACT_VIEW_NAME_PATTERN.matcher(elements);
            if (matcher.matches()) {
                result = _viewProvider.provideBy(matcher.group(1));
                if (result == null) {
                    result = _viewProvider.provideDefault();
                }
            } else {
                result = null;
            }
        }
        return result != null && result.getAccess().checkView(currentUser, null).isViewAllowed() ? result : null;
    }

    @Nonnull
    protected Locale getLocaleFor(@Nullable User user) {
        return _localizationHelper.getLocaleFor(user);
    }

    @Nonnull
    protected String getTemplateNameFor(@Nonnull Map<String, Object> context, @Nullable User user) {
        final ConfluenceUser currentUser = AuthenticatedUserThreadLocal.get();
        final String variant;
        if ((user == null || !user.equals(currentUser)) && !_permissionManager.hasPermission(currentUser, VIEW, TARGET_PEOPLE_DIRECTORY)) {
            variant = ".accessDenied";
        } else {
            final Object username = context.get("username");
            if (username == null || username.toString().isEmpty()) {
                variant = ".missingUsername";
            } else if (context.get("user") == null) {
                variant = ".unknownUser";
            } else {
                variant = "";
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

    public static enum Border {
        visible,
        hidden
    }

    public static enum Avatar {
        visible,
        hidden
    }

    public static enum Labels {
        visible,
        hidden
    }

    public static enum GroupLabels {
        visible,
        big,
        hidden
    }
}
