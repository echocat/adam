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

import org.echocat.adam.configuration.localization.Localized;
import org.echocat.adam.configuration.template.Template;
import org.echocat.adam.report.Column.Link;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import java.util.List;

import static org.echocat.adam.configuration.ConfigurationContants.SCHEMA_NAMESPACE;

@XmlType(name = "reportColumn", namespace = SCHEMA_NAMESPACE)
public class Column extends Localized {

    @Nullable
    private Template _template;
    @Nullable
    private List<Element> _elements;
    @Nonnull
    private Link _link = Link.none;

    @Nullable
    @XmlElement(name = "template", namespace = SCHEMA_NAMESPACE)
    public Template getTemplate() {
        return _template;
    }

    public void setTemplate(@Nullable Template template) {
        _template = template;
    }

    @Nullable
    @XmlElement(name = "element", namespace = SCHEMA_NAMESPACE)
    public List<Element> getElements() {
        return _elements;
    }

    public void setElements(@Nullable List<Element> elements) {
        _elements = elements;
    }

    @Nonnull
    @XmlAttribute(name = "link")
    public Link getLink() {
        return _link;
    }

    public void setLink(@Nonnull Link link) {
        _link = link;
    }

}
