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

import com.atlassian.bonnie.analyzer.LuceneAnalyzerFactory;
import com.atlassian.confluence.search.v2.lucene.LuceneQueryMapper;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.echocat.adam.report.Column;
import org.echocat.adam.report.ColumnElementModel;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.atlassian.bonnie.BonnieConstants.LUCENE_VERSION;
import static org.apache.lucene.queryparser.classic.QueryParser.Operator.AND;

public class UserProfileDataQueryMapper implements LuceneQueryMapper<UserProfileDataQuery> {

    @Nonnull
    private final LuceneAnalyzerFactory _luceneAnalyzerFactory;

    public UserProfileDataQueryMapper(@Nonnull LuceneAnalyzerFactory luceneAnalyzerFactory) {
        _luceneAnalyzerFactory = luceneAnalyzerFactory;
    }

    @Override
    public Query convertToLuceneQuery(@Nonnull UserProfileDataQuery query) {
        final QueryParser parser = new MultiFieldQueryParser(LUCENE_VERSION, toFieldsArray(query), _luceneAnalyzerFactory.createAnalyzer());
        parser.setDefaultOperator(AND);
        final String searchTerm = query.getSearchTerm();
        try {
            return parser.parse(searchTerm != null ? searchTerm : "");
        } catch (final ParseException e) {
            throw new RuntimeException("Unable to parse query: " + searchTerm, e);
        }
    }

    @Nonnull
    protected Set<String> toFields(@Nonnull UserProfileDataQuery query) {
        final Set<String> fields = new HashSet<>();
        final Iterable<Column> columns = query.getColumns();
        if (columns != null) {
            for (final Column column : columns) {
                for (final ColumnElementModel elementModel : column) {
                    if (elementModel.isSearchable()) {
                        fields.add("profile." + elementModel.getId());
                    }
                }
            }
        }
        return fields;
    }

    @Nonnull
    protected String[] toFieldsArray(@Nonnull UserProfileDataQuery query) {
        final Collection<String> fields = toFields(query);
        return fields.toArray(new String[fields.size()]);
    }

}
