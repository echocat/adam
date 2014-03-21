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

import org.echocat.adam.configuration.access.view.ViewAccess;
import org.echocat.adam.configuration.localization.Localized;
import org.echocat.adam.report.View;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

import static org.echocat.adam.configuration.ConfigurationContants.SCHEMA_NAMESPACE;
import static org.echocat.adam.report.Report.DEFAULT_ID;

@XmlType(name = "report", namespace = SCHEMA_NAMESPACE)
public class Report extends Localized {

    public Report() {
        setId(DEFAULT_ID);
    }

    @Nullable
    private List<Column> _columns;
    @Nullable
    private Filter _filter;
    @Nullable
    private ViewAccess _access;
    @Nonnull
    private View _defaultView = View.cards;
    @Nonnegative
    private int _resultsPerPage = 50;

    @Nullable
    @XmlElement(name = "column", namespace = SCHEMA_NAMESPACE)
    public List<Column> getColumns() {
        return _columns;
    }

    public void setColumns(@Nullable List<Column> columns) {
        _columns = columns;
    }

    @Nullable
    @XmlElement(name = "filter", namespace = SCHEMA_NAMESPACE)
    public Filter getFilter() {
        return _filter;
    }

    public void setFilter(@Nullable Filter filter) {
        _filter = filter;
    }

    @Nullable
    @XmlElement(name = "access", namespace = SCHEMA_NAMESPACE)
    public ViewAccess getAccess() {
        return _access;
    }

    public void setAccess(@Nullable ViewAccess access) {
        _access = access;
    }

    @Nonnull
    @XmlAttribute(name = "defaultView")
    public View getDefaultView() {
        return _defaultView;
    }

    public void setDefaultView(@Nonnull View defaultView) {
        _defaultView = defaultView;
    }

    @XmlAttribute(name = "resultsPerPage")
    public int getResultsPerPage() {
        return _resultsPerPage;
    }

    public void setResultsPerPage(int resultsPerPage) {
        _resultsPerPage = resultsPerPage;
    }
}
