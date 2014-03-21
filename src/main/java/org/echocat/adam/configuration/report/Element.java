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

package org.echocat.adam.configuration.report;

import org.echocat.adam.configuration.IdEnabled;
import org.echocat.adam.report.ColumnElementModel.Format;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import static org.echocat.adam.configuration.ConfigurationContants.SCHEMA_NAMESPACE;
import static org.echocat.adam.report.ColumnElementModel.Format.formatted;

@XmlType(name = "reportColumnElement", namespace = SCHEMA_NAMESPACE)
public class Element extends IdEnabled {

    @Nonnull
    private Format _format = formatted;

    @Nonnull
    @XmlAttribute(name = "format")
    public Format getFormat() {
        return _format;
    }

    public void setFormat(@Nonnull Format format) {
        _format = format;
    }

}
