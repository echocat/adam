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

import com.atlassian.confluence.search.v2.SearchFilter;
import com.atlassian.confluence.search.v2.SearchQuery;
import org.echocat.adam.report.Column;
import org.echocat.jomon.runtime.CollectionUtils;
import org.echocat.jomon.runtime.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static org.echocat.jomon.runtime.CollectionUtils.asList;

public class UserProfileDataQuery implements SearchQuery {

    @Nullable
    public static UserProfileDataQuery userProfileDataQueryFor(@Nullable String searchTerm, @Nullable Iterable<Column> onColumns) {
        final UserProfileDataQuery result;
        if (StringUtils.isNotEmpty(searchTerm) && CollectionUtils.isNotEmpty(onColumns)) {
            result = userProfileDataQuery();
            result.setSearchTerm(searchTerm);
            result.setColumns(onColumns);
        } else {
            result = null;
        }
        return result;
    }

    @Nonnull
    public static UserProfileDataQuery userProfileDataQuery() {
        return new UserProfileDataQuery();
    }

    @Nullable
    private String _searchTerm;
    @Nullable
    private Iterable<Column> _columns;

    @Nullable
    public String getSearchTerm() {
        return _searchTerm;
    }

    public void setSearchTerm(@Nullable String searchTerm) {
        _searchTerm = searchTerm;
    }

    @Nullable
    public Iterable<Column> getColumns() {
        return _columns;
    }

    public void setColumns(@Nullable Iterable<Column> columns) {
        _columns = columns;
    }

    @Override
    public List<Object> getParameters() {
        return asList(getSearchTerm(), getColumns());
    }

    @Override
    public String getKey() {
        return "userProfileDataQuery";
    }

    @SuppressWarnings("override")
    public SearchQuery expand() {
        return this;
    }
}
