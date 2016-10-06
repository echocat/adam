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

package org.echocat.adam.configuration;

import org.echocat.adam.configuration.access.view.Administrator;
import org.echocat.adam.configuration.access.view.Default;
import org.echocat.adam.configuration.access.view.ViewAccess;
import org.echocat.adam.configuration.access.viewedit.Owner;
import org.echocat.adam.configuration.access.viewedit.ViewEditAccess;
import org.echocat.adam.configuration.localization.Localization;
import org.echocat.adam.configuration.profile.Element;
import org.echocat.adam.configuration.profile.Group;
import org.echocat.adam.configuration.report.Column;
import org.echocat.adam.configuration.report.Filter;
import org.echocat.adam.configuration.report.Report;
import org.echocat.adam.configuration.template.Template;
import org.eclipse.persistence.exceptions.XMLMarshalException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.*;
import java.io.*;
import java.net.URL;

import static java.lang.Thread.currentThread;
import static javax.xml.bind.JAXBContext.newInstance;
import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.echocat.adam.configuration.ConfigurationContants.SCHEMA;
import static org.echocat.adam.configuration.profile.Element.ContextAttribute;
import static org.eclipse.persistence.jaxb.MarshallerProperties.INDENT_STRING;
import static org.eclipse.persistence.jaxb.MarshallerProperties.NAMESPACE_PREFIX_MAPPER;

public final class ConfigurationMarshaller {

    private static final JAXBContext JAXB_CONTEXT;
    private static final ConfigurationNamespacePrefixMapper NAMESPACE_PREFIX_MAPPER_INSTANCE = new ConfigurationNamespacePrefixMapper();

    static {
        final ClassLoader old = currentThread().getContextClassLoader();
        try {
            currentThread().setContextClassLoader(ConfigurationMarshaller.class.getClassLoader());
            JAXB_CONTEXT = newInstance(
                Configuration.class,
                Element.class,
                Localization.class,
                ContextAttribute.class,
                Template.class,
                Group.class,
                ViewAccess.class,
                ViewEditAccess.class,
                Administrator.class,
                org.echocat.adam.configuration.access.viewedit.Administrator.class,
                Default.class,
                Owner.class,
                org.echocat.adam.configuration.access.view.Group.class,
                Report.class,
                Column.class,
                Filter.class,
                org.echocat.adam.configuration.report.Group.class
            );
        } catch (final Exception e) {
            throw new ConfigurationException("Could not create jaxb context.", e);
        } finally {
            currentThread().setContextClassLoader(old);
        }
    }

    @Nullable
    public static Configuration unmarshall(@Nonnull Reader content) {
        return unmarshall(content, null);
    }

    @Nullable
    public static Configuration unmarshall(@Nonnull Reader content, @Nullable String systemId) {
        try {
            final Unmarshaller unmarshaller = unmarshallerFor(content, systemId);
            final InputSource source = new InputSource(content);
            source.setSystemId(systemId != null ? systemId : "<unknownSource>");
            return (Configuration) unmarshaller.unmarshal(source);
        } catch (final UnmarshalException e) {
            throw new ParseException(e, (systemId != null ? systemId : content.toString()));
        } catch (final JAXBException e) {
            throw new ConfigurationException("Could not unmarshall: " + (systemId != null ? systemId : content), e);
        }
    }

    @Nullable
    public static Configuration unmarshall(@Nullable URL content) throws IOException {
        return unmarshall(content, null);
    }

    @Nullable
    public static Configuration unmarshall(@Nullable URL content, @Nullable String systemId) throws IOException {
        final Configuration result;
        if (content != null) {
            try (final InputStream is = content.openStream()) {
                try (final Reader reader = new InputStreamReader(is)) {
                    result = unmarshall(reader, systemId != null ? systemId : content.toExternalForm());
                }
            }
        } else {
            result = null;
        }
        return result;
    }

    @Nullable
    public static Configuration unmarshall(@Nullable String content) {
        return unmarshall(content, null);
    }

    @Nullable
    public static Configuration unmarshall(@Nullable String content, @Nullable String systemId) {
        return isEmpty(content) ? null : unmarshall(new StringReader(content), systemId);
    }

    @Nonnull
    private static Unmarshaller unmarshallerFor(@Nonnull Object element, @Nullable String systemId) {
        final Unmarshaller unmarshaller;
        try {
            unmarshaller = JAXB_CONTEXT.createUnmarshaller();
            unmarshaller.setSchema(SCHEMA);
        } catch (final JAXBException e) {
            throw new ConfigurationException("Could not create unmarshaller to unmarshall " + (systemId != null ? systemId : element.toString()) + ".", e);
        }
        return unmarshaller;
    }

    @Nullable
    public static String marshall(@Nullable Configuration configuration) {
        final String result;
        if (configuration != null) {
            final StringWriter to = new StringWriter();
            try {
                final Marshaller marshaller = marshallerFor(configuration);
                marshaller.marshal(configuration, to);
            } catch (final JAXBException e) {
                throw new ConfigurationException("Could not marshall " + configuration + ".", e);
            }
            result = to.toString();
        } else {
            result = null;
        }
        return result;
    }

    @Nonnull
    private static Marshaller marshallerFor(@Nonnull Configuration configuration) {
        final Marshaller marshaller;
        try {
            marshaller = JAXB_CONTEXT.createMarshaller();
            marshaller.setProperty(JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(INDENT_STRING, "    ");
            marshaller.setProperty(NAMESPACE_PREFIX_MAPPER, NAMESPACE_PREFIX_MAPPER_INSTANCE);
        } catch (final Exception e) {
            throw new ConfigurationException("Could not create marshaller to marshall " + configuration + ".", e);
        }
        return marshaller;
    }

    public static class ConfigurationException extends RuntimeException {

        public ConfigurationException(String message) {
            super(message);
        }

        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public static class ParseException extends ConfigurationException {

        @Nonnull
        private final UnmarshalException _cause;
        @Nonnull
        private final String _systemId;

        public ParseException(@Nonnull UnmarshalException cause, @Nonnull String systemId) {
            super(cause.getMessage(), cause);
            _cause = cause;
            _systemId = systemId;
        }

        @Override
        public String getMessage() {
            final String message = super.getMessage();
            return isEmpty(message) ? getPublicMessage() : message;
        }

        @Nonnull
        public String getPublicMessage() {
            final SAXParseException e = findSAXParseException();
            final String result;
            if (e != null) {
                result = e.getMessage() + " (source:" + e.getSystemId() + ", line: " + e.getLineNumber() + ":" + e.getColumnNumber() + ")";
            } else {
                result = "Error while parse; systemId: " + _systemId;
            }
            return result;
        }

        @Nullable
        protected SAXParseException findSAXParseException() {
            final Throwable linkedException = _cause.getLinkedException();
            final SAXParseException result;
            if (linkedException instanceof XMLMarshalException && linkedException.getCause() instanceof SAXParseException) {
                result = (SAXParseException) linkedException.getCause();
            } else if (linkedException instanceof SAXParseException) {
                result = (SAXParseException) linkedException;
            } else {
                result = null;
            }
            return result;
        }

    }

    private ConfigurationMarshaller() {}
}
