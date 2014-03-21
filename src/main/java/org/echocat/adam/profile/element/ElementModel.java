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

import org.echocat.adam.access.ViewEditAccess;
import org.echocat.adam.localization.Localized;
import org.echocat.adam.template.Template;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface ElementModel extends Localized {

    @Nonnull
    public static final String PERSONAL_INFORMATION_ELEMENT_ID = "personalInformation";
    @Nonnull
    public static final String FULL_NAME_ELEMENT_ID = "fullName";
    @Nonnull
    public static final String EMAIL_ELEMENT_ID = "email";
    @Nonnull
    public static final String WEBSITE_ELEMENT_ID = "website";
    @Nonnull
    public static final String PHONE_ELEMENT_ID = "phone";

    public boolean isStandard();

    public boolean isDefaultForReports();

    @Nullable
    public List<String> getContextAttributeKeys();

    public boolean isSearchable();

    public boolean isVisibleIfEmpty();

    @Nonnull
    public ViewEditAccess getAccess();

    @Nonnull
    public Type getType();

    @Nullable
    public Template getTemplate();

    public static enum Type {
        singleLineText,
        multiLineText,
        emailAddress,
        url,
        phoneNumber,
        wikiMarkup
    }

}
