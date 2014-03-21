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

package org.echocat.adam.localization;

import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.util.i18n.DocumentationBean;
import com.atlassian.confluence.util.i18n.DocumentationBeanFactory;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.velocity.htmlsafe.HtmlSafe;
import com.atlassian.user.User;
import org.echocat.adam.profile.element.ElementModel;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

import static java.util.Locale.US;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.echocat.adam.profile.element.ElementModel.Type.wikiMarkup;

public class LocalizationHelper implements DisposableBean {

    @Nonnull
    public static final Locale DEFAULT_LOCALE = US;

    @Nullable
    private static LocalizationHelper c_instance;

    @Nonnull
    public static LocalizationHelper localizationHelper() {
        final LocalizationHelper result = c_instance;
        if (result == null) {
            throw new IllegalStateException("There is currently no instance registered.");
        }
        return result;
    }

    @Nonnull
    private final I18NBeanFactory _i18NBeanFactory;
    @Nonnull
    private final DocumentationBeanFactory _documentationBeanFactory;
    @Nonnull
    private final LocaleManager _localeManager;


    @Autowired
    public LocalizationHelper(@Nonnull LocaleManager localeManager, @Nonnull I18NBeanFactory i18NBeanFactory, @Nonnull DocumentationBeanFactory documentationBeanFactory) {
        _localeManager = localeManager;
        _i18NBeanFactory = i18NBeanFactory;
        _documentationBeanFactory = documentationBeanFactory;
        c_instance = this;
    }

    @Nonnull
    public String getTitleFor(@Nonnull Localized localized) {
        return getTitleFor(localized, DEFAULT_LOCALE);
    }

    @Nonnull
    public String getTitleFor(@Nonnull Localized localized, @Nullable Locale locale) {
        final Map<Locale, Localization> localizations = localized.getLocalizations();
        return localizations != null ? getTitleFor(localized, locale, localizations) : getDefaultTitleFor(localized, locale);
    }

    @Nonnull
    protected String getDefaultTitleFor(@Nonnull Localized localized, @Nullable Locale locale) {
        final String result;
        if (localized instanceof ElementModel && !((ElementModel)localized).isStandard()) {
            result = capitalize(localized.getId());
        } else {
            final String id = localized.getId();
            final I18NBean i18n = _i18NBeanFactory.getI18NBean(locale != null ? locale : DEFAULT_LOCALE);
            result = i18n.getText("org.echocat.adam." + id);
        }
        return result;
    }

    @Nonnull
    protected String getTitleFor(@Nonnull Localized localized, @Nullable Locale locale, @Nonnull Map<Locale, Localization> using) {
        String result = findTitleFor(using, locale);
        if (result == null) {
            result = findTitleFor(using, DEFAULT_LOCALE);
        }
        if (result == null) {
            result = getDefaultTitleFor(localized, locale);
        }
        return result;
    }

    @Nullable
    protected String findTitleFor(@Nonnull Map<Locale, Localization> localizations, @Nullable Locale locale) {
        return locale != null ? findTitleFor(localizations.get(locale)) : null;
    }

    @Nullable
    protected String findTitleFor(@Nullable Localization localization) {
        return localization != null ? localization.getTitle() : null;
    }

    @Nullable
    @HtmlSafe
    public String findHelpTextFor(@Nonnull Localized localized) {
        return findHelpTextFor(localized, DEFAULT_LOCALE);
    }

    @Nullable
    @HtmlSafe
    public String findHelpTextFor(@Nonnull Localized localized, @Nullable Locale locale) {
        final Map<Locale, Localization> localizations = localized.getLocalizations();
        return localizations != null ? findHelpTextFor(localized, locale, localizations) : findDefaultHelpTextFor(localized, locale);
    }

    @Nullable
    protected String findDefaultHelpTextFor(@SuppressWarnings("UnusedParameters") @Nonnull Localized localized, @Nullable Locale locale) {
        final String result;
        if (localized instanceof ElementModel && ((ElementModel)localized).getType() == wikiMarkup) {
            final I18NBean i18n = _i18NBeanFactory.getI18NBean(locale != null ? locale : DEFAULT_LOCALE);
            final DocumentationBean documentationBean = _documentationBeanFactory.getDocumentationBean();
            final String text = i18n.getText("hint.input.accepts.wiki.markup");
            final String link = documentationBean.getLink("help.input.accepts.wiki.markup");
            final String linkText = i18n.getText("more.about.input.accepts.wiki.markup");
            result = text + " <a href=\"" + link + "\" target=\"_blank\">" + linkText + "</a>";
        } else {
            result = "";
        }
        return result;
    }

    @Nullable
    protected String findHelpTextFor(@Nonnull Localized localized, @Nullable Locale locale, @Nonnull Map<Locale, Localization> using) {
        String result = findHelpTextFor(using, locale);
        if (result == null) {
            result = findHelpTextFor(using, DEFAULT_LOCALE);
        }
        if (result == null) {
            result = findDefaultHelpTextFor(localized, locale);
        }
        return result;
    }

    @Nullable
    protected String findHelpTextFor(@Nonnull Map<Locale, Localization> localizations, @Nullable Locale locale) {
        return locale != null ? findHelpTextFor(localizations.get(locale)) : null;
    }

    @Nullable
    protected String findHelpTextFor(@Nullable Localization localization) {
        return localization != null ? localization.getHelpText() : null;
    }

    @Nullable
    public Locale findLocaleFor(@Nullable User user) {
        final Locale localeFromUser = user != null ? _localeManager.getLocale(user) : null;
        return localeFromUser != null ? localeFromUser : null;
    }

    @Nonnull
    public Locale getLocaleFor(@Nullable User user) {
        final Locale locale = findLocaleFor(user);
        return locale != null ? locale : US;
    }

    @Override
    public void destroy() throws Exception {
        c_instance = null;
    }
}
