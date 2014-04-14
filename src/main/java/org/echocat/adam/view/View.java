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

package org.echocat.adam.view;

import org.echocat.adam.access.ViewAccess;
import org.echocat.adam.localization.Localized;
import org.echocat.adam.profile.Group;

import javax.annotation.Nonnull;
import java.util.Set;

import static org.echocat.adam.profile.element.ElementModel.EMAIL_ELEMENT_ID;
import static org.echocat.adam.profile.element.ElementModel.FULL_NAME_ELEMENT_ID;
import static org.echocat.jomon.runtime.CollectionUtils.asImmutableSet;

public interface View extends Localized, Iterable<Group> {

    @Nonnull
    public static final String DEFAULT_ID = "default";
    @Nonnull
    public static final String COMPACT_ID = "compact";
    @Nonnull
    public static final String ALL_ID = "all";
    @Nonnull
    public static final Set<String> COMPACT_ELEMENT_IDS = asImmutableSet(FULL_NAME_ELEMENT_ID, EMAIL_ELEMENT_ID);

    @Nonnull
    public ViewAccess getAccess();

    public Set<String> getElementIds();

}
