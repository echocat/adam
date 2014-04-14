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

import com.atlassian.user.User;
import org.echocat.adam.profile.element.ElementModel;
import org.echocat.adam.profile.element.ElementRenderer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GroupRenderer implements DisposableBean {

    @Nullable
    private static GroupRenderer c_instance;

    @Nonnull
    public static GroupRenderer groupRenderer() {
        final GroupRenderer result = c_instance;
        if (result == null) {
            throw new IllegalStateException("There is currently no instance registered.");
        }
        return result;
    }

    @Nonnull
    private final ElementRenderer _elementRenderer;

    @Autowired
    public GroupRenderer(@Nonnull ElementRenderer elementRenderer) {
        _elementRenderer = elementRenderer;
        c_instance = this;
    }

    public boolean isRenderOfViewAllowedFor(@Nonnull Group group, @Nullable User currentUser, @Nonnull Profile profile) {
        boolean result = false;
        for (final ElementModel model : group) {
            if (_elementRenderer.isRenderOfViewAllowedFor(model, currentUser, profile)) {
                result = true;
                break;
            }
        }
        return result;
    }

    public boolean isRenderOfEditAllowedFor(@Nonnull Group group, @Nullable User currentUser, @Nonnull Profile profile) {
        boolean result = false;
        for (final ElementModel model : group) {
            if (model.getAccess().checkEdit(currentUser, profile).isEditAllowed()) {
                result = true;
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
