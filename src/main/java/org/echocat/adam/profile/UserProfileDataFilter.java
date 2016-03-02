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

package org.echocat.adam.profile;

import com.atlassian.confluence.search.v2.AbstractChainableSearchFilter;
import com.atlassian.confluence.search.v2.SearchFilter;
import org.echocat.adam.report.Filter;
import org.echocat.adam.report.Report;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class UserProfileDataFilter extends AbstractChainableSearchFilter {

    @Nullable
    public static UserProfileDataFilter userProfileDataFilterFor(@Nonnull Report report) {
        return userProfileDataFilterFor(report != null ? report.getFilter() : null);
    }

    @Nullable
    public static UserProfileDataFilter userProfileDataFilterFor(@Nullable Filter filter) {
        final UserProfileDataFilter result;
        if (filter != null && filter.hasTerms()) {
            result = userProfileDataFilter();
            result.setIncludingGroups(filter.getIncludingGroups());
            result.setExcludingGroups(filter.getExcludingGroups());
        } else {
            result = null;
        }
        return result;
    }

    @Nonnull
    public static UserProfileDataFilter userProfileDataFilter() {
        return new UserProfileDataFilter();
    }

    @Nullable
    private Iterable<String> _includingGroups;
    @Nullable
    private Iterable<String> _excludingGroups;

    @Nullable
    public Iterable<String> getIncludingGroups() {
        return _includingGroups;
    }

    public void setIncludingGroups(@Nullable Iterable<String> includingGroups) {
        _includingGroups = includingGroups;
    }

    @Nullable
    public Iterable<String> getExcludingGroups() {
        return _excludingGroups;
    }

    public void setExcludingGroups(@Nullable Iterable<String> excludingGroups) {
        _excludingGroups = excludingGroups;
    }

    @Override
    public String getKey() {
        return "userProfileDataFilter";
    }

    @SuppressWarnings("override")
    public SearchFilter expand() {
        return this;
    }
}
