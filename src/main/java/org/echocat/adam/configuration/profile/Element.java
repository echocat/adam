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

package org.echocat.adam.configuration.profile;

import org.echocat.adam.configuration.IdEnabled;
import org.echocat.adam.configuration.access.viewedit.ViewEditAccess;
import org.echocat.adam.configuration.localization.Localized;
import org.echocat.adam.configuration.template.Template;
import org.echocat.adam.profile.element.ElementModel.Type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

import static org.echocat.adam.configuration.ConfigurationContants.SCHEMA_NAMESPACE;
import static org.echocat.adam.profile.element.ElementModel.Type.singleLineText;

@XmlType(name = "profileGroupElement", namespace = SCHEMA_NAMESPACE)
public class Element extends Localized {

    @Nullable
    private List<ContextAttribute> _contextAttributes;
    @Nullable
    private ViewEditAccess _access;
    @Nonnull
    private Type _type = singleLineText;
    @Nullable
    private Template _template;
    private boolean _searchable = true;
    private boolean _visibleIfEmpty;
    private boolean _defaultForReports;

    @Nullable
    @XmlElement(name = "contextAttribute", namespace = SCHEMA_NAMESPACE)
    public List<ContextAttribute> getContextAttributes() {
        return _contextAttributes;
    }

    public void setContextAttributes(@Nullable List<ContextAttribute> contextAttributes) {
        _contextAttributes = contextAttributes;
    }

    @Nullable
    @XmlElement(name = "access", namespace = SCHEMA_NAMESPACE)
    public ViewEditAccess getAccess() {
        return _access;
    }

    public void setAccess(@Nonnull ViewEditAccess access) {
        _access = access;
    }

    @Nonnull
    @XmlAttribute(name = "type")
    public Type getType() {
        return _type;
    }

    public void setType(@Nonnull Type type) {
        _type = type;
    }

    @Nullable
    @XmlElement(name = "template", namespace = SCHEMA_NAMESPACE, type = Template.class)
    public Template getTemplate() {
        return _template;
    }

    public void setTemplate(@Nullable Template template) {
        _template = template;
    }

    @XmlAttribute(name = "searchable")
    public boolean isSearchable() {
        return _searchable;
    }

    public void setSearchable(boolean searchable) {
        _searchable = searchable;
    }

    @XmlAttribute(name = "visibleIfEmpty")
    public boolean isVisibleIfEmpty() {
        return _visibleIfEmpty;
    }

    public void setVisibleIfEmpty(boolean visibleIfEmpty) {
        _visibleIfEmpty = visibleIfEmpty;
    }

    @XmlAttribute(name = "defaultForReports")
    public boolean isDefaultForReports() {
        return _defaultForReports;
    }

    public void setDefaultForReports(boolean defaultForReports) {
        _defaultForReports = defaultForReports;
    }

    @XmlType(name = "profileGroupElementContextAttribute", namespace = SCHEMA_NAMESPACE)
    public static class ContextAttribute extends IdEnabled {}

}
