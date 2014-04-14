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

package org.echocat.adam.configuration;

import org.echocat.adam.configuration.profile.Profile;
import org.echocat.adam.configuration.report.Report;
import org.echocat.adam.configuration.view.View;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

import static org.echocat.adam.configuration.ConfigurationContants.SCHEMA_NAMESPACE;

@XmlRootElement(name = "configuration", namespace = SCHEMA_NAMESPACE)
@XmlType(name = "configuration", namespace = SCHEMA_NAMESPACE)
public class Configuration {

    @Nullable
    private Profile _profile;
    @Nullable
    private List<Report> _reports;
    @Nullable
    private List<View> _views;
    @Nonnull
    private String _defaultReport = org.echocat.adam.report.Report.DEFAULT_ID;
    @Nonnull
    private String _defaultView = org.echocat.adam.view.View.DEFAULT_ID;
    @Nonnull
    private String _hoverView = org.echocat.adam.view.View.COMPACT_ID;

    @Nullable
    @XmlElement(name = "profile", namespace = SCHEMA_NAMESPACE)
    public Profile getProfile() {
        return _profile;
    }

    public void setProfile(@Nullable Profile profile) {
        _profile = profile;
    }

    @Nullable
    @XmlElement(name = "report", namespace = SCHEMA_NAMESPACE)
    public List<Report> getReports() {
        return _reports;
    }

    public void setReports(@Nullable List<Report> reports) {
        _reports = reports;
    }


    @Nullable
    @XmlElement(name = "view", namespace = SCHEMA_NAMESPACE)
    public List<View> getViews() {
        return _views;
    }

    public void setViews(@Nullable List<View> views) {
        _views = views;
    }

    @Nonnull
    @XmlAttribute(name = "defaultReport")
    public String getDefaultReport() {
        return _defaultReport;
    }

    public void setDefaultReport(@Nonnull String defaultReport) {
        _defaultReport = defaultReport;
    }

    @Nonnull
    @XmlAttribute(name = "defaultView")
    public String getDefaultView() {
        return _defaultView;
    }

    public void setDefaultView(@Nonnull String defaultView) {
        _defaultView = defaultView;
    }

    @Nonnull
    @XmlAttribute(name = "hoverView")
    public String getHoverView() {
        return _hoverView;
    }

    public void setHoverView(@Nonnull String hoverView) {
        _hoverView = hoverView;
    }

}
