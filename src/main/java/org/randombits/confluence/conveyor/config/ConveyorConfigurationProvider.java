package org.randombits.confluence.conveyor.config;

import com.opensymphony.util.ClassLoaderUtil;
import com.opensymphony.util.FileManager;
import com.opensymphony.util.TextUtils;
import com.opensymphony.xwork.ObjectFactory;
import com.opensymphony.xwork.config.Configuration;
import com.opensymphony.xwork.config.ConfigurationException;
import com.opensymphony.xwork.config.ConfigurationUtil;
import com.opensymphony.xwork.config.ExternalReferenceResolver;
import com.opensymphony.xwork.config.entities.*;
import com.opensymphony.xwork.config.providers.XmlConfigurationProvider;
import com.opensymphony.xwork.config.providers.XmlHelper;
import org.echocat.jomon.runtime.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@SuppressWarnings({"MethodWithMultipleReturnPoints", "rawtypes", "unchecked", "ParameterHidesMemberVariable", "AssignmentToMethodParameter", "InstanceVariableNamingConvention", "ThrowCaughtLocally"})
public class ConveyorConfigurationProvider extends XmlConfigurationProvider {
    private static final Logger LOG = LoggerFactory.getLogger(ConveyorConfigurationProvider.class);

    private String resourceName = "conveyor-config.xml";
    private Configuration configuration;
    private final Set includedFileNames = new TreeSet();
    private Exception failureException;
    private final List actionOverrides = new ArrayList(5);

    public ConveyorConfigurationProvider(String resourceName) {
        super(resourceName);
        this.resourceName = resourceName;
    }

    public ConveyorConfigurationProvider() {
    }

    @Override
    protected void addResultTypes(PackageConfig packageContext, Element element) {
        final NodeList resultTypeList = element.getElementsByTagName("result-type");

        for (int i = 0; i < resultTypeList.getLength(); i++) {
            final Element resultTypeElement = (Element) resultTypeList.item(i);
            final String name = resultTypeElement.getAttribute("name");
            final String className = resultTypeElement.getAttribute("class");
            final String def = resultTypeElement.getAttribute("default");
            try {
                final Class clazz = ClassLoaderUtil.loadClass(className, getClass());
                final ResultTypeConfig resultType = new ResultTypeConfig(name, clazz);
                packageContext.addResultTypeConfig(resultType);

                if ("true".equals(def)) { packageContext.setDefaultResultType(name); }
            } catch (final ClassNotFoundException e) {
                LOG.error("Result class [" + className + "] doesn't exist, ignoring", e);
            }
        }
    }

    private void checkElementName(Element element, String name) throws ConveyorException {
        if (!name.equals(element.getNodeName())) {
            throw new ConveyorException("Expected element named '" + name + "' but got '" + element.getNodeName() + "'.");
        }
    }

    public static Map copyParams(Map params) {
        if (params != null) {
            final Map copy = new HashMap();
            copy.putAll(params);
            return copy;
        }
        return null;
    }

    public static ResultConfig copyResultConfig(ResultConfig config) {
        return new ResultConfig(config.getName(), config.getClassName(), copyParams(config.getParams()));
    }

    public static Map copyResults(Map results) {
        if (results != null) {
            final Map copy = new HashMap();

            final Iterator i = results.entrySet().iterator();
            while (i.hasNext()) {
                final Map.Entry e = (Map.Entry) i.next();
                copy.put(e.getKey(), copyResultConfig((ResultConfig) e.getValue()));
            }

            return copy;
        }
        return null;
    }

    public static List copyInterceptors(List interceptors) {
        if (interceptors != null) {
            return new ArrayList(interceptors);
        }
        return null;
    }

    public static Object copyExternalRef(ExternalReference reference) {
        return new ExternalReference(reference.getName(), reference.getExternalRef(), reference.isRequired());
    }

    public static List copyExternalRefs(List externalRefs) {
        if (externalRefs != null) {
            final List copy = new ArrayList(externalRefs.size());
            final Iterator i = externalRefs.iterator();
            while (i.hasNext()) {
                copy.add(copyExternalRef((ExternalReference) i.next()));
            }
            return copy;
        }
        return null;
    }

    @Override
    public void destroy() {
        final Iterator i = actionOverrides.iterator();
        while (i.hasNext()) {
            final ActionOverrideDetails override = (ActionOverrideDetails) i.next();
            override.reset();
        }
        actionOverrides.clear();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ConveyorConfigurationProvider)) {
            return false;
        }

        final ConveyorConfigurationProvider configProvider = (ConveyorConfigurationProvider) o;

        return !(resourceName != null ? !resourceName.equals(configProvider.resourceName) : configProvider.resourceName != null);

    }

    public int hashCode() {
        return resourceName != null ? resourceName.hashCode() : 0;
    }

    @Override
    public void init(Configuration configuration) {
        this.configuration = configuration;

        destroy();
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(false);
            dbf.setNamespaceAware(true);

            final DocumentBuilder db = dbf.newDocumentBuilder();
            db.setEntityResolver(new EntityResolver() {
                @Override
                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    if ("-//randombits.org//Confluence Conveyor 0.2//EN".equals(publicId)) {
                        return new InputSource(ClassLoaderUtil.getResourceAsStream("confluence-conveyor-0.2.dtd", ConveyorConfigurationProvider.class));
                    }

                    return null;
                }
            });
            db.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) throws SAXException {
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    ConveyorConfigurationProvider.LOG.error(exception.getMessage() + " at (" + exception.getLineNumber() + ":" + exception.getColumnNumber() + ")");

                    throw exception;
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    ConveyorConfigurationProvider.LOG.error(exception.getMessage() + " at (" + exception.getLineNumber() + ":" + exception.getColumnNumber() + ")");

                    throw exception;
                }
            });
            includedFileNames.clear();

            loadConfigurationFile(resourceName, db);
        } catch (final RuntimeException | ConveyorException | ParserConfigurationException e) {
            fail(e);
        }
    }

    private void fail(Exception e) {
        LOG.error(e.getMessage(), e);
        failureException = e;
    }

    public Exception getFailureException() {
        return failureException;
    }

    public boolean isFailed() {
        return failureException != null;
    }

    private void loadConfigurationFile(String fileName, DocumentBuilder db) throws ConveyorException {
        if (!includedFileNames.contains(fileName)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Loading xwork configuration from: " + fileName);
            }

            includedFileNames.add(fileName);

            Document doc = null;
            InputStream is = null;
            try {
                is = getInputStream(fileName);

                if (is == null) {
                    throw new ConveyorException("Could not open file " + fileName);
                }

                doc = db.parse(is);
            } catch (final Exception e) {
                final String s = "Caught exception while loading file " + fileName;
                throw new ConveyorException(s, e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (final IOException e) {
                        throw new ConveyorException("Unable to close input stream", e);
                    }
                }
            }

            final Element rootElement = doc.getDocumentElement();
            checkElementName(rootElement, "conveyor-config");

            final NodeList children = rootElement.getChildNodes();
            final int childSize = children.getLength();

            for (int i = 0; i < childSize; i++) {
                final Node childNode = children.item(i);

                if ((childNode instanceof Element)) {
                    final Element child = (Element) childNode;

                    final String nodeName = child.getNodeName();

                    if (nodeName.equals("package-override")) {
                        addPackageOverride(child);
                    }
                    if (nodeName.equals("package")) {
                        addPackage(child);
                    } else if (nodeName.equals("include")) {
                        final String includeFileName = child.getAttribute("file");
                        loadConfigurationFile(includeFileName, db);
                    }
                }
            }

            if (LOG.isDebugEnabled()) { LOG.debug("Loaded xwork configuration from: " + fileName); }
        }
    }

    @Override
    protected InputStream getInputStream(String fileName) {
        return FileManager.loadFile(fileName, getClass());
    }

    @Override
    public boolean needsReload() {
        return true;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    protected void addPackageOverride(Element packageOverrideElement)
        throws ConveyorException {
        final PackageConfig overridePackage = findPackageConfig(packageOverrideElement);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Overridden: " + overridePackage);
        }

        addResultTypes(overridePackage, packageOverrideElement);

        loadInterceptors(overridePackage, packageOverrideElement);

        loadDefaultInterceptorRef(overridePackage, packageOverrideElement);

        loadGlobalResults(overridePackage, packageOverrideElement);

        final NodeList actionOverrideList = packageOverrideElement.getElementsByTagName("action-override");

        for (int i = 0; i < actionOverrideList.getLength(); i++) {
            final Element actionOverrideElement = (Element) actionOverrideList.item(i);
            overrideAction(actionOverrideElement, overridePackage);
        }

        final NodeList actionList = packageOverrideElement.getElementsByTagName("action");

        for (int i = 0; i < actionList.getLength(); i++) {
            final Element actionElement = (Element) actionList.item(i);

            final String name = actionElement.getAttribute("name");

            final ActionConfig existing = (ActionConfig) overridePackage.getAllActionConfigs().get(name);
            if (existing != null) {
                LOG.error("An action with the specified name already exists in the '" + overridePackage.getName() + "' package: " + name + "; " + existing.getClassName());
            } else {
                addAction(actionElement, overridePackage);
            }
        }
        configuration.addPackageConfig(overridePackage.getName(), overridePackage);
    }

    protected void overrideAction(Element actionOverrideElement, PackageConfig packageConfig) throws ConveyorException {
        final String name = actionOverrideElement.getAttribute("name");
        String className = actionOverrideElement.getAttribute("class");
        String methodName = actionOverrideElement.getAttribute("method");
        final String inheritAttr = actionOverrideElement.getAttribute("inherit");
        final boolean inherit = "true".equals(inheritAttr);

        className = !className.trim().isEmpty() ? className.trim() : null;
        methodName = !methodName.trim().isEmpty() ? methodName.trim() : null;

        if (TextUtils.stringSet(className)) {
            try {
                ObjectFactory.getObjectFactory().getClassInstance(className);
            } catch (final Exception e) {
                fail("Action class [" + className + "] not found, skipping action [" + name + "]", e);
                return;
            }
        } else if (!inherit) {
            throw new ConveyorException("No class specified for action override: " + name);
        }

        packageConfig = findPackageContext(packageConfig, name);

        if (packageConfig == null) {
            throw new ConveyorException("No existing action was found to override: " + name);
        }

        final ActionConfig oldAction = (ActionConfig) packageConfig.getActionConfigs().get(name);
        if (oldAction == null) {
            throw new ConveyorException("No existing action was found to override: " + name);
        }
        if (ActionOverrideConfig.class.getName().equals(oldAction.getClass().getName())) {
            throw new ConveyorException("The '" + name + "' action has already been overridden: " + oldAction.getClassName());
        }

        final Map actionParams = XmlHelper.getParams(actionOverrideElement);
        final Map results;
        try {
            results = buildResults(actionOverrideElement, packageConfig);
        } catch (final ConfigurationException e) {
            throw new ConveyorException("Error building results for action " + name + " in namespace " + packageConfig.getNamespace(), e);
        }

        final List interceptorList = buildInterceptorList(actionOverrideElement, packageConfig);

        final List externalrefs = buildExternalRefs(actionOverrideElement, packageConfig);

        final ActionOverrideConfig actionConfig = new ActionOverrideConfig(oldAction, inherit, methodName, className, actionParams, results, interceptorList, externalrefs, packageConfig.getName());

        packageConfig.addActionConfig(name, actionConfig);

        final ActionOverrideDetails details = new ActionOverrideDetails(packageConfig, name, actionConfig);
        actionOverrides.add(details);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Loaded " + (TextUtils.stringSet(packageConfig.getNamespace()) ? packageConfig.getNamespace() + "/" : "") + name + " in '" + packageConfig.getName() + "' package:" + actionConfig);
        }
    }

    private PackageConfig findPackageContext(PackageConfig packageConfig, String name) {
        final ActionConfig oldAction = (ActionConfig) packageConfig.getActionConfigs().get(name);
        if (oldAction != null) {
            return packageConfig;
        }
        final List parents = packageConfig.getParents();
        if (parents != null) {
            final Iterator i = parents.iterator();
            while (i.hasNext()) {
                packageConfig = findPackageContext((PackageConfig) i.next(), name);
                if (packageConfig != null) {
                    return packageConfig;
                }
            }
        }
        return null;
    }

    protected PackageConfig findPackageConfig(Element packageOverrideElement)
        throws ConveyorException {
        final String name = TextUtils.noNull(packageOverrideElement.getAttribute("name"));
        final String namespace = TextUtils.noNull(packageOverrideElement.getAttribute("namespace"));

        final PackageConfig config = configuration.getPackageConfig(name);
        if (config == null) { throw new ConveyorException("Unable to locate package to override: " + name); }
        if (!StringUtils.equals(namespace, config.getNamespace())) {
            throw new ConveyorException("The '" + name + "' package is is not specified to be in the '" + namespace + "' namepace.");
        }

        return config;
    }

    @Override
    protected void addPackage(Element packageElement) {
        final PackageConfig newPackage = buildPackageContext(packageElement);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Loaded " + newPackage);
        }

        addResultTypes(newPackage, packageElement);

        loadInterceptors(newPackage, packageElement);

        loadDefaultInterceptorRef(newPackage, packageElement);

        loadGlobalResults(newPackage, packageElement);

        final NodeList actionList = packageElement.getElementsByTagName("action");

        for (int i = 0; i < actionList.getLength(); i++) {
            final Element actionElement = (Element) actionList.item(i);
            addAction(actionElement, newPackage);
        }

        configuration.addPackageConfig(newPackage.getName(), newPackage);
    }

    @Override
    protected PackageConfig buildPackageContext(Element packageElement) {
        final String parent = packageElement.getAttribute("extends");
        final String abstractVal = packageElement.getAttribute("abstract");
        final boolean isAbstract = Boolean.valueOf(abstractVal);
        final String name = TextUtils.noNull(packageElement.getAttribute("name"));
        final String namespace = TextUtils.noNull(packageElement.getAttribute("namespace"));

        ExternalReferenceResolver erResolver = null;

        final String externalReferenceResolver = TextUtils.noNull(packageElement.getAttribute("externalReferenceResolver"));

        if (!"".equals(externalReferenceResolver)) {
            try {
                final Class erResolverClazz = ClassLoaderUtil.loadClass(externalReferenceResolver, ExternalReferenceResolver.class);

                erResolver = (ExternalReferenceResolver) erResolverClazz.newInstance();
            } catch (final ClassNotFoundException e) {
                final String msg = "Could not find External Reference Resolver: " + externalReferenceResolver + ". " + e.getMessage();

                fail(msg, e);
                return null;
            } catch (final Exception e) {
                final String msg = "Could not create External Reference Resolver: " + externalReferenceResolver + ". " + e.getMessage();

                fail(msg, e);
                return null;
            }
        }

        if (!TextUtils.stringSet(TextUtils.noNull(parent))) {
            return new PackageConfig(name, namespace, isAbstract, erResolver);
        }

        final List parents = ConfigurationUtil.buildParentsFromString(configuration, parent);

        if (parents.size() <= 0) {
            LOG.error("Unable to find parent packages " + parent);

            return new PackageConfig(name, namespace, isAbstract, erResolver);
        }
        return new PackageConfig(name, namespace, isAbstract, erResolver, parents);
    }

    private void fail(String message, Exception e) {
        fail(new ConveyorException(message, e));
    }

    private static class ActionOverrideDetails {
        private final PackageConfig packageConfig;
        private final String actionName;
        private final ActionOverrideConfig actionConfig;

        ActionOverrideDetails(PackageConfig packageConfig, String actionName, ActionOverrideConfig actionConfig) {
            this.packageConfig = packageConfig;
            this.actionName = actionName;
            this.actionConfig = actionConfig;
        }

        void reset() {
            final Map actionConfigs = packageConfig.getActionConfigs();
            if (actionConfigs.get(actionName).equals(actionConfig)) { packageConfig.addActionConfig(actionName, actionConfig.getOverriddenAction()); }
        }
    }
}