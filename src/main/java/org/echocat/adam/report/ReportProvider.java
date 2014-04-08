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

package org.echocat.adam.report;

import com.atlassian.user.User;
import org.apache.commons.collections15.map.LRUMap;
import org.echocat.adam.access.AccessProvider;
import org.echocat.adam.access.ViewAccess;
import org.echocat.adam.access.ViewEditAccess;
import org.echocat.adam.configuration.Configuration;
import org.echocat.adam.configuration.ConfigurationRepository;
import org.echocat.adam.configuration.report.Element;
import org.echocat.adam.configuration.report.Group;
import org.echocat.adam.localization.Localization;
import org.echocat.adam.profile.GroupProvider;
import org.echocat.adam.profile.Profile;
import org.echocat.adam.profile.element.ElementModel;
import org.echocat.adam.profile.element.ElementModelProvider;
import org.echocat.adam.profile.element.ElementModelSupport;
import org.echocat.adam.report.ColumnElementModel.Format;
import org.echocat.adam.template.Template;
import org.echocat.jomon.runtime.CollectionUtils;
import org.echocat.jomon.runtime.iterators.ChainedIterator;
import org.echocat.jomon.runtime.iterators.ConvertingIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static java.util.Collections.synchronizedMap;
import static java.util.Collections.unmodifiableMap;
import static org.echocat.adam.profile.element.ElementModel.FULL_NAME_ELEMENT_ID;
import static org.echocat.adam.template.Template.Simple.simpleTemplateFor;
import static org.echocat.jomon.runtime.CollectionUtils.asImmutableList;

public class ReportProvider implements Iterable<Report>, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(ReportProvider.class);

    @Nullable
    private static ReportProvider c_instance;

    @Nonnull
    public static ReportProvider reportProvider() {
        final ReportProvider result = c_instance;
        if (result == null) {
            throw new IllegalStateException("There is currently no instance registered.");
        }
        return result;
    }


    @Nonnull
    private final Map<Configuration, Map<String, List<Report>>> _cache = synchronizedMap(new LRUMap<Configuration, Map<String, List<Report>>>(3));

    @Nonnull
    private final ConfigurationRepository _configurationRepository;
    @Nonnull
    private final GroupProvider _groupProvider;
    @Nonnull
    private final ElementModelProvider _elementModelProvider;
    @Nonnull
    private final AccessProvider _accessProvider;

    public ReportProvider(@Nonnull ConfigurationRepository configurationRepository, @Nonnull GroupProvider groupProvider, @Nonnull ElementModelProvider elementModelProvider, @Nonnull AccessProvider accessProvider) {
        _configurationRepository = configurationRepository;
        _groupProvider = groupProvider;
        _elementModelProvider = elementModelProvider;
        _accessProvider = accessProvider;
        c_instance = this;
    }

    @Nullable
    public Report tryProvideViewable(@Nullable User forUser, @Nullable Profile target, @Nullable String preferredId) {
        return tryProvideViewable(getConfiguration(), forUser, target, preferredId);
    }

    @Nullable
    public Report tryProvideViewable(@Nonnull Configuration configuration, @Nullable User forUser, @Nullable Profile target, @Nullable String preferredId) {
        Report result = null;
        final List<Report> reports = preferredId != null ? tryProvideBy(configuration, preferredId) : null;
        if (reports != null) {
            for (final Report candidate : reports) {
                final ViewAccess access = candidate.getAccess();
                if (access.checkView(forUser, target).isViewAllowed()) {
                    result = candidate;
                    break;
                }
            }
        }
        if (result == null) {
            final List<Report> defaults = provideDefaultsBy(configuration);
            if (defaults != null) {
                for (final Report candidate : defaults) {
                    final ViewAccess access = candidate.getAccess();
                    if (access.checkView(forUser, target).isViewAllowed()) {
                        result = candidate;
                        break;
                    }
                }
            }
        }
        return result;
    }

    @Nonnull
    public List<Report> provideDefaults() {
        return provideDefaultsBy(getConfiguration());
    }

    @Nonnull
    public List<Report> provideDefaultsBy(@Nonnull Configuration configuration) {
        final String defaultReportId = configuration.getDefaultReport();
        final List<Report> reports = tryProvideBy(configuration, defaultReportId);
        if (reports == null || reports.isEmpty()) {
            throw new IllegalStateException("Could not find any default report.");
        }
        return reports;
    }

    @Nonnull
    public String provideDefaultId() {
        return provideDefaultIdBy(getConfiguration());
    }

    @Nonnull
    public String provideDefaultIdBy(@Nonnull Configuration configuration) {
        return configuration.getDefaultReport();
    }

    @Nullable
    public List<Report> tryProvideBy(@Nonnull String id) {
        return tryProvideBy(getConfiguration(), id);
    }

    @Nullable
    public List<Report> tryProvideBy(@Nonnull Configuration configuration, @Nonnull String id) {
        return getIdToReportsFor(configuration).get(id);
    }

    @Override
    @Nonnull
    public Iterator<Report> iterator() {
        return reportIteratorFor(getConfiguration());
    }

    @Nonnull
    public Iterator<Report> reportIteratorFor(@Nonnull Configuration configuration) {
        return new ChainedIterator<List<Report>, Report>(getIdToReportsFor(configuration).values()) {

            @Nullable
            @Override
            protected Iterator<Report> nextIterator(@Nullable List<Report> input) {
                return input != null ? input.iterator() : CollectionUtils.<Report>emptyIterator();
            }

        };
    }

    @Nonnull
    protected Map<String, List<Report>> getIdToReportsFor(@Nonnull Configuration configuration) {
        Map<String, List<Report>> result = _cache.get(configuration);
        if (result == null) {
            result = getIdToReportsIgnoringCacheFor(configuration);
            _cache.put(configuration, result);
        }
        return result;
    }

    @Nonnull
    protected Map<String, List<Report>> getIdToReportsIgnoringCacheFor(@Nullable Configuration configuration) {
        final List<org.echocat.adam.configuration.report.Report> sources = configuration.getReports();
        final Map<String, List<Report>> result = new LinkedHashMap<>();
        if (sources != null) {
            for (final org.echocat.adam.configuration.report.Report source : sources) {
                List<Report> reports = result.get(source.getId());
                if (reports == null) {
                    reports = new ArrayList<>(1);
                    result.put(source.getId(), reports);
                }
                reports.add(new ReportImpl(source));
            }
        }
        final String defaultId = provideDefaultIdBy(configuration);
        if (!result.containsKey(defaultId)) {
            result.put(defaultId, CollectionUtils.<Report>asList(new DefaultReport()));
        }
        return unmodifiableMap(result);
    }

    protected class DefaultReport extends ReportSupport {

        @Nonnull
        @Override
        public ViewAccess getAccess() {
            return new ViewAccess() {
                @Nonnull
                @Override
                public Visibility checkView(@Nullable User forUser, @Nullable User target) {
                    return forUser != null ? Visibility.allowed : Visibility.forbidden;
                }
            };
        }

        @Nullable
        @Override
        public Filter getFilter() {
            return null;
        }

        @Nonnull
        @Override
        public View getDefaultView() {
            return View.cards;
        }

        @Nullable
        @Override
        public Map<Locale, Localization> getLocalizations() {
            return null;
        }

        @Nullable
        @Override
        public String getId() {
            return DEFAULT_ID;
        }

        @Override
        public Iterator<Column> iterator() {
            return new ChainedIterator<org.echocat.adam.profile.Group, Column>(_groupProvider) { @Nonnull @Override protected Iterator<Column> nextIterator(@Nonnull org.echocat.adam.profile.Group input) {
                return new ConvertingIterator<ElementModel, Column>(input.iterator()) { @Override protected Column convert(@Nonnull ElementModel input) {
                    return new DefaultColumn(input);
                }};
            }};
        }

        @Override
        public int getResultsPerPage() {
            return 50;
        }
    }

    protected class ReportImpl extends ReportSupport {

        @Nonnull
        private final org.echocat.adam.configuration.report.Report _source;
        @Nonnull
        private final ViewAccess _access;
        @Nullable
        private final Filter _filter;
        @Nonnull
        private final List<Column> _columns;

        public ReportImpl(@Nonnull org.echocat.adam.configuration.report.Report source) {
            _source = source;
            _access = _accessProvider.provideFor(source.getAccess());
            _filter = determineFilterFor(source);
            _columns = determineColumnsFor(source);
        }

        @Nonnull
        @Override
        public ViewAccess getAccess() {
            return _access;
        }

        @Override
        public Iterator<Column> iterator() {
            return _columns.iterator();
        }

        @Nonnull
        @Override
        public String getId() {
            return _source.getId();
        }

        @Nullable
        @Override
        public Filter getFilter() {
            return _filter;
        }

        @Nonnull
        @Override
        public View getDefaultView() {
            return _source.getDefaultView();
        }

        @Nullable
        @Override
        protected Collection<org.echocat.adam.configuration.localization.Localization> getConfigurationBasedLocalizations() {
            return _source.getLocalizations();
        }

        @Nullable
        protected Filter determineFilterFor(@Nonnull org.echocat.adam.configuration.report.Report source) {
            final org.echocat.adam.configuration.report.Filter filter = source.getFilter();
            return filter != null ? new FilterImpl(filter) : null;
        }

        @Nonnull
        protected List<Column> determineColumnsFor(@Nonnull org.echocat.adam.configuration.report.Report source) {
            final List<Column> result = new ArrayList<>();
            final List<org.echocat.adam.configuration.report.Column> sourceColumns = source.getColumns();
            if (sourceColumns != null && !sourceColumns.isEmpty()) {
                for (final org.echocat.adam.configuration.report.Column sourceColumn : sourceColumns) {
                    result.add(new ColumnImpl(sourceColumn));
                }
            } else {
                for (final org.echocat.adam.profile.Group group : _groupProvider) {
                    for (final ElementModel elementModel : group) {
                        if (elementModel.isDefaultForReports()) {
                            result.add(new DefaultColumn(elementModel));
                        }
                    }
                }
            }
            return asImmutableList(result);
        }

        @Override
        public int getResultsPerPage() {
            return _source.getResultsPerPage();
        }
    }

    protected class FilterImpl implements Filter {

        @Nonnull
        private final org.echocat.adam.configuration.report.Filter _source;

        public FilterImpl(@Nonnull org.echocat.adam.configuration.report.Filter source) {
            _source = source;
        }

        @Nullable
        @Override
        public Iterable<String> getIncludingGroups() {
            return  groupsToIds(_source.getIncludingGroups());
        }

        @Nullable
        @Override
        public Iterable<String> getExcludingGroups() {
            return groupsToIds(_source.getExcludingGroups());
        }

        @Override
        public boolean hasTerms() {
            return !_source.getIncludingGroups().isEmpty() || !_source.getExcludingGroups().isEmpty();
        }

        @Nullable
        protected List<String> groupsToIds(@Nullable List<Group> groups) {
            final List<String> result;
            if (groups != null) {
                result = new ArrayList<>(groups.size());
                for (final Group group : groups) {
                    result.add(group.getId());
                }
            } else {
                result = null;
            }
            return result;
        }

    }

    protected class ColumnImpl extends ColumnSupport {

        @Nonnull
        private final org.echocat.adam.configuration.report.Column _source;
        @Nonnull
        private final List<ColumnElementModel> _elementModels;

        public ColumnImpl(@Nonnull org.echocat.adam.configuration.report.Column source) {
            _source = source;
            _elementModels = determineElementModelsFor(source);
        }

        @Nullable
        @Override
        public String getId() {
            return _source.getId();
        }

        @Nullable
        @Override
        protected Collection<org.echocat.adam.configuration.localization.Localization> getConfigurationBasedLocalizations() {
            return _source.getLocalizations();
        }

        @Nullable
        @Override
        public Map<Locale, Localization> getLocalizations() {
            Map<Locale, Localization> result = super.getLocalizations();
            if ((result == null || result.isEmpty()) && _elementModels.size() == 1) {
                result = _elementModels.get(0).getLocalizations();
            }
            return result;
        }

        @Nullable
        @Override
        public Template getTemplate() {
            return _source.getTemplate();
        }

        @Override
        public Iterator<ColumnElementModel> iterator() {
            return _elementModels.iterator();
        }

        @Nonnull
        @Override
        public Link getLink() {
            return _source.getLink();
        }

        @Nonnull
        protected List<ColumnElementModel> determineElementModelsFor(@Nonnull org.echocat.adam.configuration.report.Column source) {
            final List<ColumnElementModel> result = new ArrayList<>();
            final List<Element> sourceElements = source.getElements();
            if (sourceElements != null && !sourceElements.isEmpty()) {
                for (final Element sourceElement : sourceElements) {
                    final ElementModel plain = _elementModelProvider.tryProvideFor(getConfiguration(), sourceElement.getId());
                    if (plain != null) {
                        result.add(new ColumnElementModelImpl(plain, sourceElement.getFormat()));
                    } else {
                        LOG.warn("Could not find the elementModel #" + sourceElement.getId() + " defined in column #" + source.getId() + ". This element will be ignored for the report.");
                    }
                }
            } else {
                final ElementModel plain = _elementModelProvider.tryProvideFor(getConfiguration(), getId());
                if (plain != null) {
                    result.add(new ColumnElementModelImpl(plain, Format.formatted));
                }
            }
            return asImmutableList(result);
        }

    }

    protected class DefaultColumn extends ColumnSupport {

        @Nonnull
        private final ElementModel _elementModel;

        public DefaultColumn(@Nonnull ElementModel elementModel) {
            _elementModel = elementModel;
        }

        @Nullable
        @Override
        public Template getTemplate() {
            return simpleTemplateFor(getId());
        }

        @Override
        public Iterator<ColumnElementModel> iterator() {
            return CollectionUtils.<ColumnElementModel>asSingletonIterator(new ColumnElementModelImpl(_elementModel, Format.formatted));
        }

        @Nullable
        @Override
        public Map<Locale, Localization> getLocalizations() {
            return _elementModel.getLocalizations();
        }

        @Nonnull
        @Override
        public Link getLink() {
            return FULL_NAME_ELEMENT_ID.equals(getId()) ? Link.profile : Link.none;
        }

        @Nullable
        @Override
        public String getId() {
            return _elementModel.getId();
        }
    }

    protected class ColumnElementModelImpl extends ElementModelSupport implements ColumnElementModel {

        @Nonnull
        private final ElementModel _source;
        @Nonnull
        private final Format _format;

        public ColumnElementModelImpl(@Nonnull ElementModel source, @Nonnull Format format) {
            _source = source;
            _format = format;
        }

        @Nonnull
        @Override
        public Format getFormat() {
            return _format;
        }

        @Override
        public boolean isStandard() {return _source.isStandard();}

        @Override
        public boolean isDefaultForReports() {return _source.isDefaultForReports();}

        @Override
        @Nullable
        public List<String> getContextAttributeKeys() {return _source.getContextAttributeKeys();}

        @Override
        public boolean isVisibleOnOverviews() {
            return _source.isVisibleOnOverviews();
        }

        @Override
        public boolean isSearchable() {return _source.isSearchable();}

        @Override
        public boolean isVisibleIfEmpty() {return _source.isVisibleIfEmpty();}

        @Override
        @Nonnull
        public ViewEditAccess getAccess() {return _source.getAccess();}

        @Override
        @Nonnull
        public Type getType() {return _source.getType();}

        @Override
        @Nullable
        public Template getTemplate() {return _source.getTemplate();}

        @Override
        @Nullable
        public Map<Locale, Localization> getLocalizations() {return _source.getLocalizations();}

        @Override
        @Nullable
        public String getId() {return _source.getId();}
    }

    @Nonnull
    protected Configuration getConfiguration() {
        return getConfigurationRepository().get();
    }

    @Nonnull
    public ConfigurationRepository getConfigurationRepository() {
        return _configurationRepository;
    }

    @Override
    public void destroy() throws Exception {
        c_instance = null;
    }
}
