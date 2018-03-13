package org.killbill.billing.codegen.languages;

import io.swagger.codegen.*;
import io.swagger.codegen.languages.AbstractJavaCodegen;
import io.swagger.models.Model;
import io.swagger.models.Swagger;

import java.io.IOException;
import java.util.*;

public class KillbillJavaGenerator extends AbstractJavaCodegen implements CodegenConfig {


    private static final String NAME = "killbill-java";


    private static final String GROUP_ID = "org.kill-bill.billing";
    private static final String ARTIFACT_ID = "killbill-client-java-gen";
    private static final String VERSION = "0.0.1-SNAPSHOT";

    private static final String DOCS_PATH = "docs/";

    private static final String API_PACKAGE = "org.killbill.billing.client.api.gen";
    private static final String MODEL_PACKAGE = "org.killbill.billing.client.model.gen";

    private static final String HTTP_USER_AGENT = "killbill/java-client";

    private static final String DATE_LIBRARY_JODA = "joda";


    /**
     * Configures the type of generator.
     *
     * @return the CodegenType for this generator
     * @see io.swagger.codegen.CodegenType
     */
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    /**
     * Configures a friendly name for the generator.  This will be used by the generator
     * to select the library with the -l flag.
     *
     * @return the friendly name for the generator
     */
    public String getName() {
        return NAME;
    }

    /**
     * Returns human-friendly help for the generator.  Provide the consumer with help
     * tips, parameters here
     *
     * @return A string value for the help message
     */
    public String getHelp() {
        return "Generates a killbill-java client library.";
    }

    private final Map<String, Class> apiEnums;

    public KillbillJavaGenerator() {
        super();
        templateDir = "killbill-java";
        embeddedTemplateDir = "killbill-java";



        try {
            apiEnums = ClassUtil.findAPIEnum("/Users/sbrossier/.m2/repository/org/kill-bill/billing/killbill-api/0.51.9/killbill-api-0.51.9.jar");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void processOpts() {
        additionalProperties.put(DATE_LIBRARY, DATE_LIBRARY_JODA);

        super.processOpts();

        groupId = GROUP_ID;
        artifactId = ARTIFACT_ID;
        artifactVersion = VERSION;

        apiPackage = API_PACKAGE;
        modelPackage = MODEL_PACKAGE;

        apiDocPath = DOCS_PATH;

        httpUserAgent = HTTP_USER_AGENT;

        hideGenerationTimestamp = true; /* Does not seem to work, use cli option to disable */


        typeMapping.put("array", "List");
        typeMapping.put("map", "Map");

        typeMapping.put("file", "java.io.File");

        importMapping.clear();
        importMapping.put("UUID", "java.util.UUID");
        importMapping.put("List", "java.util.List");
        importMapping.put("LinkedList", "java.util.LinkedList");
        importMapping.put("ArrayList", "java.util.ArrayList");
        importMapping.put("DateTime", "org.joda.time.DateTime");
        importMapping.put("LocalDate", "org.joda.time.LocalDate");
        importMapping.put("BigDecimal", "java.math.BigDecimal");
        importMapping.put("HashMap", "java.util.HashMap");
        importMapping.put("Map", "java.util.Map");

        // Kill Bill API
        for (final String key : apiEnums.keySet()) {
            importMapping.put(key, apiEnums.get(key).getName().replaceAll("\\$", "."));
        }

        instantiationTypes.put("List", "java.util.ArrayList");
        instantiationTypes.put("Map", "java.util.HashMap");
    }

    @Override
    public void preprocessSwagger(Swagger swagger) {
        // Remove auditLogs from definitions as they will be included in our base KillBillObject
        for (final Model m : swagger.getDefinitions().values()) {
            if (m.getProperties() != null) {
                m.getProperties().remove("auditLogs");
            }
        }
    }


    private  String findEnumType(final String firstEnumValue) {
        for (final String key : apiEnums.keySet()) {
            Class claz = apiEnums.get(key);
            if (EnumSet.allOf(claz).iterator().next().toString().equals(firstEnumValue)) {
                return key;
            }

        }
        throw new IllegalStateException("Cannot find enum with first value " + firstEnumValue);
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        return objs;
    }

    // Transform CodegenOperation obj into ExtendedCodegenOperation to add properties into mustache maps
    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {

        final Map<String, Object> operationsMap = (Map<String, Object>) super.postProcessOperations(objs).get("operations");
        final List<CodegenOperation> operations = (List<CodegenOperation>) operationsMap.get("operation");
        final List<ExtendedCodegenOperation> extOperations = new ArrayList<>(operations.size());

        final List<Map<String, String>> imports = (List<Map<String, String>>) objs.get("imports");

        for (CodegenOperation op : operations) {
            final ExtendedCodegenOperation ext = new ExtendedCodegenOperation(op);
            extOperations.add(ext);
            if (ext.returnType != null && ext.isListContainer) {
                addImportIfRequired(imports, String.format("org.killbill.billing.client.model.%s", ext.returnType));
            }
            if (ext.bodyParam != null && ext.bodyParam.isContainer) {
                addImportIfRequired(imports, String.format("org.killbill.billing.client.model.%s", ext.bodyParam.dataType));
            }

            if (ext.isListContainer) {
                addImportIfRequired(imports, "java.util.List");
            } else if (ext.isMapContainer) {
                addImportIfRequired(imports, "java.util.Map");
            }
            for (final CodegenParameter p : ext.allParams) {
                if (p.isEnum) {
                    if (!p.isContainer) {
                        final String enumImport = importMapping.get(p.datatypeWithEnum);
                        addImportIfRequired(imports, enumImport);
                    } else if (p.items.isEnum) {
                        final String enumImport = importMapping.get(p.items.datatypeWithEnum);
                        addImportIfRequired(imports, enumImport);
                    }
                }
                if (p.isListContainer) {
                    addImportIfRequired(imports, "java.util.List");
                } else if (p.isMapContainer) {
                    addImportIfRequired(imports, "java.util.Map");
                }
            }

        }
        operationsMap.put("operation", extOperations);

        return objs;
    }


    private void addImportIfRequired(final List<Map<String, String>> imports, String newImport) {
        for (Map<String, String> im : imports) {
            for (String i : im.values()) {
                if (i.equals(newImport)) {
                    return;
                }
            }
        }
        final Map<String, String> im = new LinkedHashMap<String, String>();
        im.put("import", newImport);
        imports.add(im);
    }

    @Override
    public String toEnumName(CodegenProperty property) {
        if (property.isEnum) {
            if (property._enum != null && property._enum.size() > 0) {
                final String guessedEnumType = findEnumType(property._enum.get(0));
                return guessedEnumType;
            } else if (property.datatypeWithEnum != null) {
                return property.datatypeWithEnum;
            }
        }
        System.err.println("Mising _enum for " + property.name);
        return sanitizeName(camelize(property.name));
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        if (property.isEnum) {
            if (!property.isContainer) {
                final String guessedEnumType = findEnumType(property._enum.get(0));
                model.imports.add(guessedEnumType);
            } else if (property.items.isEnum) {
                final String guessedEnumType = findEnumType(property.items._enum.get(0));
                model.imports.add(guessedEnumType);
            }
        }
    }

    private static class ExtendedCodegenOperation extends CodegenOperation {

        public boolean isGet, isPost, isDelete, isPut, isOptions, isKillBillObjects;

        private ExtendedCodegenOperation(CodegenOperation o) {
            super();

            this.path = o.path;
            this.responseHeaders.addAll(o.responseHeaders);
            this.hasAuthMethods = o.hasAuthMethods;
            this.hasConsumes = o.hasConsumes;
            this.hasProduces = o.hasProduces;
            this.hasParams = o.hasParams;
            this.hasOptionalParams = o.hasOptionalParams;
            this.returnTypeIsPrimitive = o.returnTypeIsPrimitive;
            this.returnSimpleType = o.returnSimpleType;
            this.subresourceOperation = o.subresourceOperation;
            this.isMapContainer = o.isMapContainer;
            this.isListContainer = o.isListContainer;
            this.isMultipart = o.isMultipart;
            this.hasMore = o.hasMore;
            this.isResponseBinary = o.isResponseBinary;
            this.hasReference = o.hasReference;
            this.isRestfulIndex = o.isRestfulIndex;
            this.isRestfulShow = o.isRestfulShow;
            this.isRestfulCreate = o.isRestfulCreate;
            this.isRestfulUpdate = o.isRestfulUpdate;
            this.isRestfulDestroy = o.isRestfulDestroy;
            this.isRestful = o.isRestful;
            this.operationId = o.operationId;
            this.returnType = o.returnType;
            this.httpMethod = o.httpMethod;
            this.returnBaseType = o.returnBaseType;
            this.returnContainer = o.returnContainer;
            this.summary = o.summary;
            this.unescapedNotes = o.unescapedNotes;
            this.notes = o.notes;
            this.baseName = o.baseName;
            this.defaultResponse = o.defaultResponse;
            this.discriminator = o.discriminator;
            this.consumes = o.consumes;
            this.produces = o.produces;
            this.bodyParam = o.bodyParam;
            this.allParams = o.allParams;
            this.bodyParams = o.bodyParams;
            this.pathParams = o.pathParams;
            this.queryParams = o.queryParams;
            this.headerParams = o.headerParams;
            this.formParams = o.formParams;
            this.authMethods = o.authMethods;
            this.tags = o.tags;
            this.responses = o.responses;
            this.imports = o.imports;
            this.examples = o.examples;
            this.externalDocs = o.externalDocs;
            this.vendorExtensions = o.vendorExtensions;
            this.nickname = o.nickname;
            this.operationIdLowerCase = o.operationIdLowerCase;
            this.isGet = "GET".equalsIgnoreCase(httpMethod);
            this.isPost = "POST".equalsIgnoreCase(httpMethod);
            this.isPut = "PUT".equalsIgnoreCase(httpMethod);
            this.isDelete = "DELETE".equalsIgnoreCase(httpMethod);
            this.isOptions = "OPTIONS".equalsIgnoreCase(httpMethod);
            if ((isPost || isPut) && bodyParam != null && bodyParam.isContainer) {
                this.isKillBillObjects = true;
                this.bodyParam.dataType = String.format("%ss", this.bodyParam.baseType);
            }
            if (returnContainer != null && returnContainer.equals("array")) {
                this.isKillBillObjects = true;
                this.returnType = String.format("%ss", this.returnBaseType);
            }
        }
    }

}