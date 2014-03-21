package org.randombits.confluence.conveyor.config;

import com.opensymphony.xwork.config.entities.ActionConfig;

import java.util.List;
import java.util.Map;

import static org.randombits.confluence.conveyor.config.ConveyorConfigurationProvider.*;

@SuppressWarnings("rawtypes")
public class ActionOverrideConfig extends ActionConfig {
    private ActionConfig _overriddenAction;

    public ActionOverrideConfig() {
    }

    @SuppressWarnings("ParameterHidesMemberVariable")
    public ActionOverrideConfig(ActionConfig overriddenAction, boolean copySettings, String methodName, String className, Map parameters, Map results, List interceptors, List externalRefs, String packageName) {
        super(methodName, className, parameters, results, interceptors, externalRefs, packageName);
        setOverriddenAction(overriddenAction, copySettings);
    }

    public ActionOverrideConfig(ActionConfig overriddenAction, boolean copySettings) {
        this(overriddenAction, copySettings, null, null, null, null, null, null, null);
    }

    public ActionConfig getOverriddenAction() {
        return _overriddenAction;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void setOverriddenAction(ActionConfig overriddenAction, boolean copySettings) {
        _overriddenAction = overriddenAction;
        if (copySettings) {
            if (className == null) {
                setClassName(overriddenAction.getClassName());
                if (methodName == null) {
                    setMethodName(overriddenAction.getMethodName());
                }
            }

            final Map oldParams = copyParams(overriddenAction.getParams());
            if (oldParams != null) {
                if (params != null) {
                    oldParams.putAll(params);
                }
                params = oldParams;
            }

            final Map oldResults = copyResults(overriddenAction.getResults());
            if (oldResults != null) {
                if (results != null) {
                    oldResults.putAll(results);
                }
                results = oldResults;
            }

            if ((interceptors == null) || ((interceptors.isEmpty()) && (overriddenAction.getExternalRefs() != null))) {
                addInterceptors(copyInterceptors(overriddenAction.getInterceptors()));
            }
            if ((externalRefs == null) || ((externalRefs.isEmpty()) && (overriddenAction.getExternalRefs() != null))) {
                addExternalRefs(copyExternalRefs(overriddenAction.getExternalRefs()));
            }
            if (packageName == null) {
                setPackageName(overriddenAction.getPackageName());
            }
        }
    }

    public void setOverriddenAction(ActionConfig overriddenAction) {
        setOverriddenAction(overriddenAction, false);
    }
}