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

import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.user.actions.AbstractUsersAction;
import com.atlassian.confluence.user.actions.UserAware;
import com.atlassian.plugin.web.model.WebPanel;
import com.atlassian.renderer.RenderContext;
import com.atlassian.user.User;
import org.echocat.adam.localization.LocalizationHelper;
import org.echocat.adam.profile.element.ElementRenderer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.atlassian.confluence.renderer.radeox.macros.MacroUtils.defaultVelocityContext;
import static com.atlassian.confluence.util.velocity.VelocityUtils.getRenderedTemplate;
import static java.util.Locale.US;
import static org.echocat.jomon.runtime.CollectionUtils.asImmutableList;

public class EditUserProfileWebPanel implements WebPanel {

    @Nonnull
    protected static final String TEMPLATE_NAME_SUFFIX = ".vm";
    @Nonnull
    protected static final String TEMPLATE_NAME_PREFIX = EditUserProfileWebPanel.class.getName().replace('.', '/');

    @Nonnull
    private final GroupProvider _groupProvider;
    @Nonnull
    private final ProfileProvider _profileProvider;
    @Nonnull
    private final LocalizationHelper _localizationHelper;
    @Nonnull
    private final ElementRenderer _elementRenderer;
    @Nonnull
    private final GroupRenderer _groupRenderer;
    @Nonnull
    private final LocaleManager _localeManager;

    public EditUserProfileWebPanel(@Nonnull GroupProvider groupProvider, @Nonnull ProfileProvider profileProvider, @Nonnull LocalizationHelper localizationHelper, @Nonnull ElementRenderer elementRenderer, @Nonnull GroupRenderer groupRenderer, @Nonnull LocaleManager localeManager) {
        _groupProvider = groupProvider;
        _profileProvider = profileProvider;
        _localizationHelper = localizationHelper;
        _elementRenderer = elementRenderer;
        _groupRenderer = groupRenderer;
        _localeManager = localeManager;
    }

    @Override
    public String getHtml(@Nullable Map<String, Object> context) {
        final User currentUser = findCurrentUserOf(context);
        final User user = findUserOf(context);
        final Profile profile = findProfileFor(user);
        final List<Group> groups = asImmutableList(_groupProvider);
        final Locale locale = getLocaleFor(currentUser);

        final Map<String, Object> velocityContext = defaultVelocityContext();
        velocityContext.putAll(context);
        velocityContext.put("currentUser", currentUser);
        velocityContext.put("user", user);
        velocityContext.put("profile", profile);
        velocityContext.put("groups", groups);
        velocityContext.put("elementRenderer", _elementRenderer);
        velocityContext.put("groupRenderer", _groupRenderer);
        velocityContext.put("localizationHelper", _localizationHelper);
        velocityContext.put("locale", locale);

        velocityContext.put("wikiStyleRenderer", _localeManager);
        velocityContext.put("rendererContext", new RenderContext());

        final String templateName = getTemplateNameFor(currentUser);
        return getRenderedTemplate(templateName, velocityContext);
    }

    @Nonnull
    protected String getTemplateNameFor(@Nullable User currentUser) {
        return TEMPLATE_NAME_PREFIX + (currentUser != null ? "" : ".anonymous") + TEMPLATE_NAME_SUFFIX;
    }

    @Nullable
    protected User findCurrentUserOf(@Nullable Map<String, Object> context) {
        final Object plain = context != null ? context.get("remoteuser") : null;
        return plain instanceof User ? (User) plain : null;
    }

    @Nullable
    protected User findUserOf(@Nullable Map<String, Object> context) {
        final Object plain = context != null ? context.get("action") : null;
        final User result;
        if (plain instanceof UserAware) {
            result = ((UserAware)plain).getUser();
        } else if (plain instanceof AbstractUsersAction) {
            result = ((AbstractUsersAction) plain).getUser();
        } else {
            result = null;
        }
        return result;
    }

    @Nonnull
    protected Locale getLocaleFor(@Nullable User user) {
        final Locale localeFromUser = user != null ? _localeManager.getLocale(user) : null;
        return localeFromUser != null ? localeFromUser : US;
    }

    @Nullable
    protected Profile findProfileFor(@Nullable User user) {
        return user != null ? _profileProvider.provideFor(user) : null;
    }

    @Override
    public void writeHtml(@Nonnull Writer writer, @Nullable Map<String, Object> context) throws IOException {
        writer.write(getHtml(context));
    }

}
