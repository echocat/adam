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
import org.echocat.adam.access.ViewAccess;
import org.echocat.adam.localization.LocalizedSupport;
import org.echocat.adam.profile.element.ElementModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Iterator;

@XmlTransient
public abstract class GroupSupport extends LocalizedSupport implements Group {

    @Override
    public boolean equals(Object o) {
        final boolean result;
        if (this == o) {
            result = true;
        } else if (!(o instanceof Group)) {
            result = false;
        } else {
            // noinspection OverlyStrongTypeCast
            result = getId().equals(((Group)o).getId());
        }
        return result;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        return "group:" + getId();
    }

    @Nonnull
    @Override
    public ViewAccess getAccess() {
        return new ViewAccess() {
            @Nonnull
            @Override
            public Visibility checkView(@Nullable User forUser, @Nullable User target) {
                Visibility result = Visibility.forbidden;
                for (final ElementModel elementModel : GroupSupport.this) {
                    final Visibility candidate = elementModel.getAccess().checkView(forUser, target);
                    if (Visibility.isBetterVisibility(candidate, result)) {
                        result = candidate;
                    }
                }
                return result;
            }
        };
    }

    @Nullable
    @Override
    public String getId() {
        return null;
    }

    @Override
    public Iterator<ElementModel> iterator() {
        return null;
    }
}
