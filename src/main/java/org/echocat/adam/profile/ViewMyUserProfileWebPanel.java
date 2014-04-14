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

import com.atlassian.confluence.themes.GlobalHelper;
import com.atlassian.plugin.web.model.WebPanel;
import com.atlassian.user.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.security.Principal;
import java.util.Map;

import static com.atlassian.confluence.renderer.radeox.macros.MacroUtils.defaultVelocityContext;
import static com.atlassian.confluence.util.velocity.VelocityUtils.getRenderedTemplate;

public class ViewMyUserProfileWebPanel implements WebPanel {

    public static final String TEMPLATE_NAME = ViewMyUserProfileWebPanel.class.getName().replace('.', '/') + ".vm";

    @Override
    public String getHtml(@Nullable Map<String, Object> context) {
        final Map<String, Object> velocityContext = defaultVelocityContext();
        velocityContext.putAll(context);
        if (context !=  null) {
            final Object user = context.get("user");
            if (user instanceof User) {
                velocityContext.put("username", ((Principal) user).getName());
            }
        }
        velocityContext.put("helper", new GlobalHelper());
        return getRenderedTemplate(TEMPLATE_NAME, velocityContext);
    }

    @Override
    public void writeHtml(@Nonnull Writer writer, @Nullable Map<String, Object> context) throws IOException {
        writer.write(getHtml(context));
    }

}
