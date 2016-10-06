/*****************************************************************************************
 * *** BEGIN LICENSE BLOCK *****
 *
 * echocat Adam, Copyright (c) 2014-2016 echocat
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

package org.echocat.adam.configuration.template;

import org.echocat.adam.template.TemplateFormat;
import org.echocat.adam.template.TemplateSupport;
import org.eclipse.persistence.oxm.annotations.XmlValueExtension;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import static org.echocat.adam.configuration.ConfigurationContants.SCHEMA_NAMESPACE;
import static org.echocat.adam.template.TemplateFormat.velocity;

@XmlType(name = "template", namespace = SCHEMA_NAMESPACE)
public class Template extends TemplateSupport {

    @Nonnull
    private String _source = "";
    @Nonnull
    private TemplateFormat _format = velocity;

    @Override
    @Nonnull
    public String getSource() {
        return _source;
    }

    @XmlValue
    @XmlValueExtension
    public void setSource(@Nonnull String source) {
        _source = source;
    }

    @Override
    @Nonnull
    public TemplateFormat getFormat() {
        return _format;
    }

    @XmlAttribute(name = "format")
    public void setFormat(@Nonnull TemplateFormat format) {
        _format = format;
    }

}
