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

package org.echocat.adam.profile;

import com.atlassian.confluence.search.lucene.filter.TermFilter;
import com.atlassian.confluence.search.v2.lucene.LuceneSearchFilterMapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.queries.ChainedFilter;
import org.apache.lucene.search.Filter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.apache.lucene.queries.ChainedFilter.OR;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;
import static org.apache.lucene.search.BooleanClause.Occur.MUST_NOT;

public class UserProfileDataFilterMapper implements LuceneSearchFilterMapper<UserProfileDataFilter> {

    @Override
    public Filter convertToLuceneSearchFilter(@Nonnull UserProfileDataFilter filter) {
        final BooleanFilter result = new BooleanFilter();
        final Filter include = toFilter(filter.getIncludingGroups());
        if (include != null) {
            result.add(include, MUST);
        }
        final Filter exclude = toFilter(filter.getExcludingGroups());
        if (exclude != null) {
            result.add(exclude, MUST_NOT);
        }
        return result;
    }

    @Nullable
    protected Filter toFilter(@Nullable Iterable<String> groups) {
        final Iterator<String> i = groups != null ? groups.iterator() : null;
        final Filter result;
        if (i != null && i.hasNext()) {
            final Filter first = new TermFilter(new Term("group", i.next()));
            if (i.hasNext()) {
                final List<Filter> filters = new ArrayList<>();
                filters.add(first);
                while (i.hasNext()) {
                    filters.add(new TermFilter(new Term("group", i.next())));
                }
                result = new ChainedFilter(filters.toArray(new Filter[filters.size()]), OR);
            } else {
                result = first;
            }
        } else {
            result = null;
        }
        return result;
    }

}
