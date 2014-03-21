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

import org.echocat.adam.access.ViewAccess;
import org.echocat.adam.localization.Localized;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface Report extends Iterable<Column>, Localized {

    @Nonnull
    public static final String DEFAULT_ID = "all";

    @Nonnull
    public ViewAccess getAccess();

    @Nullable
    public Filter getFilter();

    @Nonnull
    public View getDefaultView();

    @Nonnegative
    public int getResultsPerPage();

}
