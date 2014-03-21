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

package org.echocat.adam.access;

import com.atlassian.user.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ViewEditAccess extends ViewAccess {

    @Nonnull
    public Editeditability checkEdit(@Nullable User forUser, @Nullable User target);

    public static enum Editeditability {
        allowed,
        forbidden;

        public boolean isEditAllowed() {
            return this == allowed;
        }

        public boolean isBetterThen(@Nonnull Editeditability other) {
            return other == null || ordinal() < other.ordinal();
        }

        public boolean isBest() {
            return ordinal() == 0;
        }

        public static boolean isBestEditeditability(@Nullable Editeditability what) {
            return what != null && what.isBest();
        }

        public static boolean isBetterEditeditability(@Nullable Editeditability what, @Nullable Editeditability then) {
            return what != null && what.isBetterThen(then);
        }
    }
}
