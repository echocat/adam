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

package org.echocat.adam.administration;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.core.FormAware;
import org.echocat.adam.configuration.Configuration;
import org.echocat.adam.configuration.ConfigurationMarshaller;
import org.echocat.adam.configuration.ConfigurationMarshaller.ParseException;
import org.echocat.adam.configuration.ConfigurationRepository;
import org.echocat.adam.synchronization.LdapDirectorySynchronizer;

import javax.annotation.Nonnull;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.echocat.jomon.runtime.CollectionUtils.isEmpty;

public class AdministrationAction extends ConfluenceActionSupport implements FormAware {

    @Nonnull
    private final ConfigurationRepository _configurationRepository;
    @Nonnull
    private final LdapDirectorySynchronizer _ldapDirectorySynchronizer;

    private String _configurationXml;
    private String _save;
    private String _saveAndSynchronizationNow;
    private String _validate;
    private boolean _configurationValid;

    public AdministrationAction(@Nonnull ConfigurationRepository configurationRepository, @Nonnull LdapDirectorySynchronizer ldapDirectorySynchronizer) {
        _configurationRepository = configurationRepository;
        _ldapDirectorySynchronizer = ldapDirectorySynchronizer;
    }

    @Override
    public String execute() throws Exception {
        validate();
        final String result;
        if (isSaveRequested() && isEmpty(getActionErrors())) {
            result = doSave();
        } else if (isValidationRequested()) {
            result = doValidation();
        } else {
            result = doView();
        }
        if (isSynchronizationRequested() && isEmpty(getActionErrors())) {
            synchronize();
        }
        return result;
    }

    protected void synchronize() throws Exception {
        final Thread thread = new Thread("LDAP directory synchronization job") {
            @Override
            public void run() {
                try {
                    _ldapDirectorySynchronizer.synchronize();
                } catch (final Exception e) {
                    LOG.warn("Could not synchronize users.", e);
                }
            }
        };
        thread.start();
    }

    @Nonnull
    public String doView() throws Exception {
        _configurationXml = _configurationRepository.getPlain();
        return "display";
    }

    @Nonnull
    public String doValidation() throws Exception {
        return "display";
    }

    @Nonnull
    public String doSave() throws Exception {
        _configurationRepository.set(_configurationXml);
        return "display";
    }

    @Override
    public void validate() {
        _configurationValid = false;
        if (isValidationRequested() || isSaveRequested()) {
            if (_configurationXml == null || isEmpty(_configurationXml)) {
                addFieldError("configurationXml", "org.echocat.adam.configurationXml.error.empty", new Object[0]);
            } else {
                try {
                    unmarshall();
                    _configurationValid = true;
                } catch (final ParseException e) {
                    addFieldError("configurationXml", "org.echocat.adam.configurationXml.error.couldNotParsed", new Object[] { e.getPublicMessage() });
                }
            }
        }
    }

    @Nonnull
    protected Configuration unmarshall() {
        return ConfigurationMarshaller.unmarshall(_configurationXml, "configuration:XML");
    }

    @Override
    public boolean isPermitted() {
        return permissionManager.isConfluenceAdministrator(getAuthenticatedUser());
    }

    public String getConfigurationXml() {
        return _configurationXml;
    }

    public void setConfigurationXml(String configurationXml) {
        _configurationXml = configurationXml;
    }

    @Nonnull
    protected Configuration getConfiguration() {
        return _configurationRepository.get();
    }

    public boolean isSaveRequested() {
        return !isEmpty(getSave()) || !isEmpty(getSaveAndSynchronizationNow());
    }

    public boolean isSynchronizationRequested() {
        return !isEmpty(getSaveAndSynchronizationNow());
    }

    public boolean isValidationRequested() {
        return !isEmpty(getValidate());
    }

    public String getSave() {
        return _save;
    }

    public void setSave(String save) {
        _save = save;
    }

    public String getSaveAndSynchronizationNow() {
        return _saveAndSynchronizationNow;
    }

    public void setSaveAndSynchronizationNow(String saveAndSynchronizationNow) {
        _saveAndSynchronizationNow = saveAndSynchronizationNow;
    }

    public String getValidate() {
        return _validate;
    }

    public void setValidate(String validate) {
        _validate = validate;
    }

    @Override
    public boolean isEditMode() {
        return true;
    }

    public boolean isConfigurationValid() {
        return _configurationValid;
    }
}
