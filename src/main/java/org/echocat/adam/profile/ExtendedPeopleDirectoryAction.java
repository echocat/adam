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

import com.atlassian.bonnie.Searchable;
import com.atlassian.confluence.pages.ManualTotalPaginationSupport;
import com.atlassian.confluence.search.service.ContentTypeEnum;
import com.atlassian.confluence.search.v2.*;
import com.atlassian.confluence.search.v2.SearchManager.EntityVersionPolicy;
import com.atlassian.confluence.search.v2.filter.SubsetResultFilter;
import com.atlassian.confluence.search.v2.query.BooleanQuery;
import com.atlassian.confluence.search.v2.query.ContentTypeQuery;
import com.atlassian.confluence.search.v2.query.HasPersonalSpaceQuery;
import com.atlassian.confluence.search.v2.sort.FullnameSort;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.PersonalInformation;
import com.atlassian.confluence.user.actions.PeopleDirectoryAction;
import com.atlassian.confluence.velocity.htmlsafe.HtmlSafe;
import com.atlassian.user.User;
import com.google.common.collect.Sets;
import com.opensymphony.xwork.ActionContext;
import org.echocat.adam.localization.LocalizationHelper;
import org.echocat.adam.profile.element.ElementRenderer;
import org.echocat.adam.report.*;
import org.echocat.adam.report.View;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.atlassian.confluence.search.v2.lucene.LuceneSearchResults.EMPTY_RESULTS;
import static com.atlassian.confluence.util.GeneralUtil.urlEncode;
import static java.lang.reflect.Modifier.isStatic;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.join;
import static org.echocat.adam.localization.LocalizationHelper.localizationHelper;
import static org.echocat.adam.profile.ProfileProvider.profileProvider;
import static org.echocat.adam.profile.UserProfileDataFilter.userProfileDataFilterFor;
import static org.echocat.adam.profile.UserProfileDataQuery.userProfileDataQueryFor;
import static org.echocat.adam.profile.element.ElementRenderer.elementRenderer;
import static org.echocat.adam.report.ColumnRenderer.columnRenderer;
import static org.echocat.adam.report.ReportProvider.reportProvider;

public class ExtendedPeopleDirectoryAction extends PeopleDirectoryAction {

    // private static final Method MAKE_SEARCH_QUERY = getMethod(SearchQuery.class, "makeSearchQuery");
    private static final Method MAKE_SEARCH_FILTER = getMethod(SearchFilter.class, "makeSearchFilter");
    private static final Method IS_SHOW_ALL_PEOPLE = getMethod(boolean.class, "isShowingAllPeople");
    private static final Method DETERMINE_BLANK_EXPERIENCE = getMethod(void.class, "determineBlankExperience");

    private SearchManager _searchManager;

    private List<Report> _reports;
    private List<Profile> _profiles;
    private List<Column> _columns;
    private View _view;
    private Report _report;

    @Override
    public String doSearch() {
        // noinspection unchecked
        ActionContext.getContext().getSession().put("confluence.user.dir.search.string", getQueryString());
        final Report report = getEffectiveReport();
        _reports = determinateReports();
        _columns = determinateModelsFor(report);
        _profiles = searchFor(report, _columns);
        return "success";
    }

    @Override
    public String doBrowse() throws Exception {
        setQueryString("");
        final Report report = getEffectiveReport();
        _reports = determinateReports();
        _columns = determinateModelsFor(report);
        _profiles = searchFor(report, _columns);
        return "success";
    }

    @Nonnull
    protected List<Report> determinateReports() {
        final List<Report> result = new ArrayList<>();
        for (final Report report : getReportProvider()) {
            if (report.getAccess().checkView(getAuthenticatedUser(), null).isViewAllowed()) {
                result.add(report);
            }
        }
        return result;
    }

    @Nonnull
    protected List<Column> determinateModelsFor(@Nullable Report report) {
        final List<Column> columns = new ArrayList<>();
        if (report != null) {
            for (final Column column : report) {
                if (column.getAccess().checkView(getAuthenticatedUser(), null).isViewAllowed()) {
                    columns.add(column);
                }
            }
        }
        return columns;
    }

    @Nonnull
    protected List<Profile> searchFor(@Nullable Report report, @Nullable Iterable<Column> columns) {
        final List<Profile> profiles = new ArrayList<>();
        if (report != null && columns != null) {
            final SearchResults results = performSearch(report, makeSearchQuery(columns), makeSearchFilterFor(report));
            final List<Searchable> resultObjects = _searchManager.convertToEntities(results, EntityVersionPolicy.LATEST_VERSION);
            final ManualTotalPaginationSupport<Searchable> paginationSupport = (ManualTotalPaginationSupport<Searchable>) getPaginationSupport();
            paginationSupport.setPageSize(report.getResultsPerPage());
            paginationSupport.setStartIndex(getStartIndex());
            paginationSupport.setTotal(results.getUnfilteredResultsCount());
            paginationSupport.setItems(resultObjects);
            for (final Searchable resultObject : resultObjects) {
                final PersonalInformation personalInformation = (PersonalInformation) resultObject;
                final ConfluenceUser user = personalInformation.getUser();
                final Profile profile = profileProvider().provideFor(user);
                profiles.add(profile);
            }
        }
        if (isShowingAllPeople()) {
            determineBlankExperience();
        }
        return profiles;
    }

    @Nonnull
    protected SearchResults performSearch(@Nonnull Report forReport, @Nonnull SearchQuery query, @Nullable SearchFilter filter) {
        SearchResults results;
        try {
            results = _searchManager.search(new ContentSearch(query, FullnameSort.ASCENDING, filter, new SubsetResultFilter(getStartIndex(), forReport.getResultsPerPage())));
        } catch (final InvalidSearchException e) {
            throw new RuntimeException((new StringBuilder()).append("Invalid search: ").append(e).toString(), e);
        } catch (final RuntimeException e) {
            LOG.info((new StringBuilder()).append("Error executing people directory search, returning nothing. ").append(e).toString(), e);
            results = EMPTY_RESULTS;
        }
        return results;
    }

    @Nonnull
    protected SearchQuery makeSearchQuery(@Nonnull Iterable<Column> columns) {
        final Set<SearchQuery> searchTerms = Sets.newHashSet();
        searchTerms.add(new ContentTypeQuery(ContentTypeEnum.PERSONAL_INFORMATION));
        final UserProfileDataQuery userProfileDataQuery = userProfileDataQueryFor(getQueryString(), columns);
        if (userProfileDataQuery != null) {
            searchTerms.add(userProfileDataQuery);
        }
        if (isShowOnlyPersonal()) {
            searchTerms.add(new HasPersonalSpaceQuery());
        }
        return BooleanQuery.composeAndQuery(searchTerms);
    }

    @Nullable
    protected SearchFilter makeSearchFilterFor(@Nonnull Report report) {
        final SearchFilter original = invoke(MAKE_SEARCH_FILTER);
        final UserProfileDataFilter userProfileDataFilter = userProfileDataFilterFor(report);
        return userProfileDataFilter != null ? original.and(userProfileDataFilter) : original;
    }

    @Nullable
    protected boolean isShowingAllPeople() {
        return invoke(IS_SHOW_ALL_PEOPLE);
    }

    @Nullable
    protected void determineBlankExperience() {
        invoke(DETERMINE_BLANK_EXPERIENCE);
    }

    @Nullable
    protected <T> T invoke(@Nonnull Method method, @Nullable Object... arguments) {
        try {
            // noinspection unchecked
            return (T) method.invoke(this, arguments);
        } catch (final InvocationTargetException e) {
            final Throwable cause = e.getTargetException();
            if (cause instanceof RuntimeException) {
                // noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                // noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
                throw (Error) cause;
            } else {
                throw new RuntimeException("Could not invoke " + method + ".", cause != null ? cause : e);
            }
        } catch (final Exception e) {
            throw new RuntimeException("Could not invoke " + method + ".", e);
        }
    }

    @Override
    public void setSearchManager(SearchManager searchManager) {
        _searchManager = searchManager;
        super.setSearchManager(searchManager);
    }

    @Nonnull
    protected static Method getMethod(@Nonnull Class<?> returnType, @Nonnull String methodName, @Nullable Class<?>... argumentTypes) {
        final Class<PeopleDirectoryAction> c = PeopleDirectoryAction.class;
        final Method method;
        try {
            method = c.getDeclaredMethod(methodName, argumentTypes);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException("Could not find a method " + returnType.getName() + " " + c.getName() + "." + methodName + "(" + join(argumentTypes, ", ") + ").", e);
        }
        if (!method.getReturnType().equals(returnType)) {
            throw new IllegalArgumentException("Could not find a method " + returnType.getName() + " " + c.getName() + "." + methodName + "(" + join(argumentTypes, ", ") + ").");
        }
        if (isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("Could not find a NOT STATIC method " + returnType.getName() + " " + c.getName() + "." + methodName + "(" + join(argumentTypes, ", ") + ").");
        }
        method.setAccessible(true);
        return method;
    }

    @Nonnull
    public ElementRenderer getElementRenderer() {
        return elementRenderer();
    }

    @Nonnull
    public ColumnRenderer getColumnRenderer() {
        return columnRenderer();
    }

    @Nonnull
    public LocalizationHelper getLocalizationHelper() {
        return localizationHelper();
    }

    @Nonnull
    public ReportProvider getReportProvider() {
        return reportProvider();
    }

    public List<Profile> getProfiles() {
        return _profiles;
    }

    public List<Column> getColumns() {
        return _columns;
    }

    public List<Report> getReports() {
        return _reports;
    }

    public String getView() {
        return _view != null ? _view.name() : null;
    }

    public void setView(String view) {
        try {
            _view = View.valueOf(view);
        } catch (final IllegalArgumentException ignored) {
            _view = null;
        }
    }

    @Nonnull
    public View getEffectiveView() {
        View result = _view;
        if (result == null) {
            final Report report = getEffectiveReport();
            result = report != null ? report.getDefaultView() : View.cards;
        }
        return result;
    }

    public String getReport() {
        return _report != null ? _report.getId() : null;
    }

    public void setReport(String report) {
        _report = reportProvider().tryProvideViewable(getAuthenticatedUser(), null, report);
    }

    @Nullable
    public Report getEffectiveReport() {
        if (_report == null) {
            _report = reportProvider().tryProvideViewable(getAuthenticatedUser(), null, null);
        }
        return _report;
    }

    @Nonnull
    @HtmlSafe
    public String buildLinkFor(@Nonnull HttpServletRequest request) {
        return buildLinkFor(request, false);
    }

    @Nonnull
    @HtmlSafe
    public String buildLinkFor(@Nonnull HttpServletRequest request, @Nullable Report report) {
        return buildLinkFor(request, report, false);
    }

    @Nonnull
    @HtmlSafe
    public String buildLinkFor(@Nonnull HttpServletRequest request, boolean requireReportParameter) {
        return buildLinkFor(request, _report, requireReportParameter);
    }

    @Nonnull
    @HtmlSafe
    public String buildLinkFor(@Nonnull HttpServletRequest request, @Nullable Report report, boolean requireReportParameter) {
        return buildLinkFor(request, getQueryString(), _view, report, requireReportParameter);
    }

    @Nonnull
    @HtmlSafe
    public String buildLinkFor(@Nonnull HttpServletRequest request, @Nullable String queryString, @Nullable View view, @Nullable Report report, boolean requireReportParameter) {
        final StringBuilder sb = new StringBuilder();
        sb.append(request.getContextPath()).append('/');
        boolean qma = false;
        if (isEmpty(queryString)) {
            sb.append("browsepeople.action");
        } else {
            sb.append("dopeopledirectorysearch.action?queryString=").append(urlEncode(queryString));
            qma = true;
        }
        if (view != null) {
            final View targetView = evaluateViewFor(view, report, getAuthenticatedUser());
            if (targetView != null) {
                sb.append(qma ? "&" : "?").append("view=").append(targetView);
                qma = true;
            }
        }
        if (report != null) {
            final Report targetReport = evaluateReportFor(report, getAuthenticatedUser());
            if (targetReport != null) {
                sb.append(qma ? "&" : "?").append("report=").append(urlEncode(targetReport.getId()));
                qma = true;
            } else if (requireReportParameter) {
                sb.append(qma ? "&" : "?").append("report=").append(urlEncode(getReportProvider().provideDefaultId()));
                qma = true;
            }
        }
        if (isShowExternallyDeletedUsers()) {
            sb.append(qma ? "&" : "?").append("showExternallyDeletedUsers=true");
            qma = true;
        }
        if (isShowDeactivatedUsers()) {
            sb.append(qma ? "&" : "?").append("showDeactivatedUsers=true");
            qma = true;
        }
        if (isShowOnlyPersonal()) {
            sb.append(qma ? "&" : "?").append("showOnlyPersonal=true");
        }
        return sb.toString();
    }

    @Nonnull
    protected View getDefaultViewFor(@Nullable Report report, @Nullable User forUser) {
        final Report targetReport = evaluateReportFor(report, forUser);
        return targetReport != null ? targetReport.getDefaultView() : View.cards;
    }

    @Nullable
    protected Report evaluateReportFor(Report report, User forUser) {
        final Report defaultReport = reportProvider().tryProvideViewable(forUser, null, null);
        final Report targetReport = report != null ? report : defaultReport;
        return targetReport != null && !targetReport.equals(defaultReport) ? targetReport : null;
    }

    @Nullable
    protected View evaluateViewFor(@Nullable View view, @Nullable Report report, @Nullable User forUser) {
        final View result;
        if (view != null) {
            final View defaultView = getDefaultViewFor(report, forUser);
            if (defaultView != view) {
                result = view;
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public boolean isShowBlankExperience() {
        return super.isShowBlankExperience() && (_profiles == null || _profiles.isEmpty());
    }

}
