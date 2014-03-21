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

import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static org.echocat.jomon.runtime.util.ResourceUtils.closeQuietly;

public class ConfigurationContants {

    public static final String SCHEMA_NAMESPACE = "https://adam.echocat.org/schemas/configuration.xsd";
    public static final String SCHEMA_XSD_LOCATION = "org/echocat/adam/schemas/configuration.xsd";
    public static final Schema SCHEMA;

    static {
        final SchemaFactory schemaFactory = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
        final InputStream is = ConfigurationContants.class.getClassLoader().getResourceAsStream(SCHEMA_XSD_LOCATION);
        if (is == null){
            throw new IllegalStateException("There is no '" + SCHEMA_XSD_LOCATION + "' in classpath.");
        }
        try {
            SCHEMA = schemaFactory.newSchema(new StreamSource(is, "classpath:/" + SCHEMA_XSD_LOCATION));
        } catch (final SAXException e) {
            throw new RuntimeException("Could not load '" + SCHEMA_XSD_LOCATION + "'.", e);
        } finally {
            closeQuietly(is);
        }
    }

    private ConfigurationContants() {}
}
