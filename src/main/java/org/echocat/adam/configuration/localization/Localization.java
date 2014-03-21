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

package org.echocat.adam.configuration.localization;

import org.echocat.jomon.runtime.jaxb.LocaleAdapter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Locale;

import static org.echocat.adam.configuration.ConfigurationContants.SCHEMA_NAMESPACE;

@XmlType(name = "localization", namespace = SCHEMA_NAMESPACE)
public class Localization implements org.echocat.adam.localization.Localization {

    @Nonnull
    private Locale _locale;
    @Nullable
    private String _title;
    @Nullable
    private String _helpText;

    @XmlAttribute(name = "locale", required = true)
    @XmlJavaTypeAdapter(LocaleAdapter.class)
    @Nonnull
    public Locale getLocale() {
        return _locale;
    }

    public void setLocale(@Nonnull Locale locale) {
        _locale = locale;
    }

    @Nullable
    @XmlAttribute(name = "title")
    @Override
    public String getTitle() {
        return _title;
    }

    public void setTitle(@Nullable String title) {
        _title = title;
    }

    @Nullable
    @XmlAttribute(name = "helpText")
    @Override
    public String getHelpText() {
        return _helpText;
    }

    public void setHelpText(@Nullable String helpText) {
        _helpText = helpText;
    }

}
