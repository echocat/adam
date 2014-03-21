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

package org.echocat.adam.report;

import com.atlassian.confluence.velocity.htmlsafe.HtmlSafe;
import com.atlassian.user.User;
import org.echocat.adam.profile.Profile;
import org.echocat.adam.profile.element.ElementRenderer;
import org.echocat.adam.report.ColumnElementModel.Format;
import org.echocat.adam.template.Template;
import org.echocat.adam.template.TemplateFormat;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.atlassian.confluence.renderer.radeox.macros.MacroUtils.defaultVelocityContext;
import static com.atlassian.confluence.util.velocity.VelocityUtils.getRenderedContent;
import static org.echocat.adam.template.TemplateFormat.plain;
import static org.echocat.adam.template.TemplateFormat.velocity;

public class ColumnRenderer implements DisposableBean {

    @Nullable
    private static ColumnRenderer c_instance;

    @Nonnull
    public static ColumnRenderer columnRenderer() {
        final ColumnRenderer result = c_instance;
        if (result == null) {
            throw new IllegalStateException("There is currently no instance registered.");
        }
        return result;
    }

    @Nonnull
    private final ElementRenderer _elementRenderer;

    @Autowired
    public ColumnRenderer(@Nonnull ElementRenderer elementRenderer) {
        _elementRenderer = elementRenderer;
        c_instance = this;
    }

    @Nonnull
    protected Map<String, Object> renderViewXhtmlOfElementModelsFor(@Nonnull Column column, @Nullable User currentUser, @Nonnull Profile profile) {
        final Map<String, Object> result = new LinkedHashMap<>();
        for (final ColumnElementModel model : column) {
            final Format format = model.getFormat();
            final Object content;
            if (format == Format.plain) {
                content = profile.getValue(model);
            } else if (format == Format.formatted) {
                content = _elementRenderer.renderViewXhtml(model, currentUser, profile);
            } else {
                throw new IllegalArgumentException("Could not handle format " + format + " of " + column + ":" + model + ".");
            }
            result.put(model.getId(), content);
        }
        return Collections.unmodifiableMap(result);
    }

    @Nullable
    @HtmlSafe
    public String renderViewXhtml(@Nonnull Column column, @Nullable User currentUser, @Nonnull Profile profile) {
        return renderViewXhtml(column, currentUser, profile, null);
    }

    @Nullable
    @HtmlSafe
    public String renderViewXhtml(@Nonnull Column column, @Nullable User currentUser, @Nonnull Profile profile, @Nullable Map<String, Object> properties) {
        final Template template = column.getTemplate();
        final String result;
        final Map<String, Object> elementModelValues = renderViewXhtmlOfElementModelsFor(column, currentUser, profile);
        if (template != null) {
            final TemplateFormat format = template.getFormat();
            if (format == velocity) {
                final Map<String, Object> context = defaultVelocityContext();
                context.putAll(elementModelValues);
                context.put("column", column);
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
            final StringBuilder sb = new StringBuilder();
            for (final Object value : elementModelValues.values()) {
                if (value != null) {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(value);
                }
            }
            result = sb.toString();
        }
        return result;
    }

    public boolean isRenderOfViewAllowedFor(@Nonnull Column column, @Nullable User currentUser, @Nonnull Profile profile) {
        boolean result = true;
        for (final ColumnElementModel model : column) {
            if (!_elementRenderer.isRenderOfViewAllowedFor(model, currentUser, profile)) {
                result = false;
                break;
            }
        }
        return result;
    }

    @Override
    public void destroy() throws Exception {
        c_instance = null;
    }

}
