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

import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.velocity.htmlsafe.HtmlSafe;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.WikiStyleRenderer;
import com.atlassian.user.User;
import org.echocat.adam.profile.Profile;
import org.echocat.adam.template.Template;
import org.echocat.adam.template.TemplateFormat;
import org.echocat.jomon.runtime.util.Hint;
import org.echocat.jomon.runtime.util.Hint.Impl;
import org.echocat.jomon.runtime.util.Hints;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

import static com.atlassian.confluence.renderer.radeox.macros.MacroUtils.defaultVelocityContext;
import static com.atlassian.confluence.util.PlainTextToHtmlConverter.encodeHtmlEntities;
import static com.atlassian.confluence.util.PlainTextToHtmlConverter.matchAndReplaceSpaces;
import static com.atlassian.confluence.util.velocity.VelocityUtils.getRenderedContent;
import static com.atlassian.confluence.util.velocity.VelocityUtils.getRenderedTemplate;
import static com.opensymphony.util.TextUtils.leadingSpaces;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.echocat.adam.profile.element.ElementModel.FULL_NAME_ELEMENT_ID;
import static org.echocat.adam.profile.element.ElementModel.PERSONAL_INFORMATION_ELEMENT_ID;
import static org.echocat.adam.template.TemplateFormat.plain;
import static org.echocat.adam.template.TemplateFormat.velocity;
import static org.echocat.jomon.runtime.util.Hint.Impl.hint;
import static org.echocat.jomon.runtime.util.Hints.nonNullHints;

@SuppressWarnings("ConstantNamingConvention")
public class ElementRenderer implements DisposableBean {

    @Nonnull
    public static final Hint<Boolean> enableUserLinkIfPossible = hint(Boolean.class, ElementRenderer.class, "enableUserLinkIfPossible", true);
    public static final Hint<String> fullNameTagName = hint(String.class, ElementRenderer.class, "fullNameTagName");

    @Nullable
    private static ElementRenderer c_instance;

    @Nonnull
    public static ElementRenderer elementRenderer() {
        final ElementRenderer result = c_instance;
        if (result == null) {
            throw new IllegalStateException("There is currently no instance registered.");
        }
        return result;
    }

    @Nonnull
    private final WikiStyleRenderer _wikiStyleRenderer;
    @Nonnull
    private final SettingsManager _settingsManager;
    @Nonnull
    private final UserAccessor _userAccessor;

    @Autowired
    public ElementRenderer(@Nonnull WikiStyleRenderer wikiStyleRenderer, @Nonnull SettingsManager settingsManager, @Nonnull UserAccessor userAccessor) {
        _wikiStyleRenderer = wikiStyleRenderer;
        _settingsManager = settingsManager;
        _userAccessor = userAccessor;
        c_instance = this;
    }

    @Nonnull
    protected static final String TEMPLATE_NAME_PREFIX = ElementRenderer.class.getName().replace('.', '/');

    @Nullable
    public String renderContent(@Nonnull ElementModel model, @Nonnull Profile profile) {
        return renderContent(model, profile, null);
    }

    @Nonnull
    public String nodeIdFor(@Nonnull ElementModel model, @Nonnull Profile profile) {
        return "profile-element-value-" + profile.getName() + "-" + model.getId();
    }

    @Nullable
    public String renderContent(@Nonnull ElementModel model, @Nonnull Profile profile, @Nullable Map<String, Object> properties) {
        final Template template = model.getTemplate();
        final String result;
        if (template != null) {
            final TemplateFormat format = template.getFormat();
            if (format == velocity) {
                final Map<String, Object> context = defaultVelocityContext();
                context.put("model", model);
                context.put("profile", profile);
                context.put("newLine", "\n");
                if (properties != null) {
                    context.putAll(properties);
                }
                result = getRenderedContent(template.getSource(), context);
            } else  if (format == plain) {
                result = template.getSource();
            } else {
                throw new UnsupportedOperationException("Could not handle template: " + template);
            }
        } else {
            result = null;
        }
        return result;
    }

    public boolean isRenderOfViewAllowedFor(@Nonnull ElementModel model, @Nullable User currentUser, @Nonnull Profile profile) {
        final boolean result;
        if (model.getAccess().checkView(currentUser, profile).isViewAllowed()) {
            if (!isEmpty(profile.getValue(model))) {
                result = !PERSONAL_INFORMATION_ELEMENT_ID.equals(model.getId());
            } else {
                result = false;
            }
        } else {
            result = false;
        }
        return result;
    }

    public boolean isRenderOfEditAllowedFor(@Nonnull ElementModel model, @Nullable User currentUser, @Nonnull Profile profile) {
        final boolean result;
        if (model.getAccess().checkEdit(currentUser, profile).isEditAllowed() && model.getTemplate() == null) {
            final String id = model.getId();
            if (FULL_NAME_ELEMENT_ID.equals(id) || ElementModel.EMAIL_ELEMENT_ID.equals(id)) {
                result = !_settingsManager.getGlobalSettings().isExternalUserManagement()
                    && !_userAccessor.isReadOnly(profile);
            } else {
                result = true;
            }
        } else {
            result = false;
        }
        return result;
    }

    @Nonnull
    @HtmlSafe
    public String renderViewXhtml(@Nonnull ElementModel model, @Nullable User currentUser, @Nonnull Profile profile, @Nullable Hints hints) {
        final String value = getViewValueFor(model, currentUser, profile);
        return renderXhtml(model, currentUser, profile, value, "view", hints);
    }

    @Nonnull
    @HtmlSafe
    public String renderEditXhtml(@Nonnull ElementModel model, @Nullable User currentUser, @Nonnull Profile profile, @Nullable Hints hints) {
        final String value = profile.getValue(model);
        return renderXhtml(model, currentUser, profile, value, "edit", hints);
    }

    @Nonnull
    @HtmlSafe
    public String renderViewXhtml(@Nonnull ElementModel model, @Nullable User currentUser, @Nonnull Profile profile) {
        return renderViewXhtml(model, currentUser, profile, null);
    }

    @Nonnull
    @HtmlSafe
    public String renderEditXhtml(@Nonnull ElementModel model, @Nullable User currentUser, @Nonnull Profile profile) {
        return renderEditXhtml(model, currentUser, profile, null);
    }

    @Nonnull
    @HtmlSafe
    protected String renderXhtml(@Nonnull ElementModel model, @Nullable User currentUser, @Nonnull Profile profile, @Nullable String value, @Nonnull String mode, @Nullable Hints hints) {
        final Map<String, Object> context = defaultVelocityContext();
        context.put("readOnly", !isRenderOfEditAllowedFor(model, currentUser, profile));
        context.put("value", value);
        context.put("elementModel", model);
        context.put("currentUser", currentUser);
        context.put("profile", profile);
        context.put("wikiStyleRenderer", _wikiStyleRenderer);
        context.put("renderContext", new RenderContext());
        context.put("nodeId", nodeIdFor(model, profile));
        context.put("elementRenderer", this);
        context.put("hints", nonNullHints(hints));
        context.put("enableUserLinkIfPossible", enableUserLinkIfPossible);
        context.put("fullNameTagName", fullNameTagName);
        final String templateName = getXhtmlTemplateNameFor(model, mode);
       return getRenderedTemplate(templateName, context);
    }

    @Nonnull
    @HtmlSafe
    public String plainToXhtml(@SuppressWarnings("UnusedParameters") @Nonnull ElementModel mode, @Nullable String input) {
        String result = "";
        if (!isEmpty(input)) {
            result = encodeHtmlEntities(input);
            result = leadingSpaces(result);
            result = matchAndReplaceSpaces(result);
            result = result.replace("\n", "<br/>\n");
        }
        return result;
    }

    @Nonnull
    @HtmlSafe
    public String getAsString(@SuppressWarnings("UnusedParameters") @Nonnull ElementModel mode, @Nullable Object input) {
        return input != null ? input.toString() : "";
    }

    @Nullable
    protected String getViewValueFor(@Nonnull ElementModel model, @Nonnull User currentUser, @Nonnull Profile profile) {
        final String value = profile.getValue(model);
        final boolean masked = model.getAccess().checkView(currentUser, profile).isMasked();
        return value != null && masked ? "***" : value;
    }

    @Nonnull
    protected String getXhtmlTemplateNameFor(@Nonnull ElementModel model, @Nonnull String mode) {
        return TEMPLATE_NAME_PREFIX + "." + model.getType() + "." + mode + ".xhtml.vm";
    }

    @Override
    public void destroy() throws Exception {
        c_instance = null;
    }
}
