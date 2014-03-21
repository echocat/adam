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

import com.atlassian.user.User;
import org.echocat.adam.access.ViewAccess;
import org.echocat.adam.localization.LocalizedSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.echocat.adam.access.ViewAccess.Visibility.allowed;
import static org.echocat.adam.access.ViewAccess.Visibility.isBetterVisibility;

public abstract class ColumnSupport extends LocalizedSupport implements Column {

    private final ViewAccess _access = new AccessImpl();

    @Nonnull
    @Override
    public ViewAccess getAccess() {
        return _access;
    }

    @Override
    public boolean equals(Object o) {
        final boolean result;
        if (this == o) {
            result = true;
        } else if (!(o instanceof Column)) {
            result = false;
        } else {
            // noinspection OverlyStrongTypeCast
            result = getId().equals(((Column)o).getId());
        }
        return result;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("column:").append(getId()).append('{');
        sb.append("models:");
        boolean first = true;
        for (final ColumnElementModel elementModel : this) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(elementModel.getId()).append(":").append(elementModel.getType());
        }
        sb.append('}');
        return sb.toString();
    }

    protected class AccessImpl implements ViewAccess {

        @Nonnull
        @Override
        public Visibility checkView(@Nullable User forUser, @Nullable User target) {
            Visibility result = Visibility.forbidden;
            boolean thereAreAnyElement = false;
            for (final ColumnElementModel elementModel : ColumnSupport.this) {
                thereAreAnyElement = true;
                final Visibility candidate = elementModel.getAccess().checkView(forUser, target);
                if (isBetterVisibility(candidate, result)) {
                    result = candidate;
                }
            }
            return thereAreAnyElement ? result : allowed;
        }

    }

}
