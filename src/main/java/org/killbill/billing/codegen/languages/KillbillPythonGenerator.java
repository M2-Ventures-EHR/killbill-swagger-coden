package org.killbill.billing.codegen.languages;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import io.swagger.codegen.*;
import io.swagger.models.properties.*;
import io.swagger.codegen.languages.PythonClientCodegen;
import io.swagger.models.Swagger;

import javax.annotation.Nullable;

import java.util.*;
import java.io.File;

public class KillbillPythonGenerator extends PythonClientCodegen implements CodegenConfig {

    // source folder where to write the files
    protected String apiVersion = "0.0.1-SNAPSHOT";
    private static final String PACKAGE_NAME = "killbill";
    private static final String PACKAGE_VERSION = "0.0.1-SNAPSHOT";
    private static final Boolean EXCLUDE_TESTS = true;

    /**
     * Configures the type of generator.
     *
     * @return  the CodegenType for this generator
     * @see     io.swagger.codegen.CodegenType
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
        return "killbill-python";
    }

    /**
     * Returns human-friendly help for the generator.  Provide the consumer with help
     * tips, parameters here
     *
     * @return A string value for the help message
     */
    public String getHelp() {
        return "Generates a killbill-python client library.";
    }

    public KillbillPythonGenerator() {
        super();

        /**
         * Template Location.  This is the location which templates will be read from.  The generator
         * will use the resource stream to attempt to read the templates.
         */
        templateDir = "killbill-python";
        embeddedTemplateDir = "killbill-python";

        /**
         * Reserved words.  Override this with reserved words specific to your language
         */
        reservedWords = new HashSet<String> (
                Arrays.asList(
                        "sample1",  // replace with static values
                        "sample2")
        );

        /**
         * Additional Properties.  These values can be passed to the templates and
         * are available in models, apis, and supporting files
         */
        additionalProperties.put("apiVersion", apiVersion);

    }

    /**
     * Escapes a reserved word as defined in the `reservedWords` array. Handle escaping
     * those terms here.  This logic is only called if a variable matches the reserved words
     *
     * @return the escaped term
     */
    @Override
    public String escapeReservedWord(String name) {
        return "_" + name;  // add an underscore to the name
    }

    /**
     * Optional - type declaration.  This is a String which is used by the templates to instantiate your
     * types.  There is typically special handling for different property types
     *
     * @return a string value used as the `dataType` field for model templates, `returnType` for api templates
     */
    @Override
    public String getTypeDeclaration(Property p) {
        if(p instanceof ArrayProperty) {
            ArrayProperty ap = (ArrayProperty) p;
            Property inner = ap.getItems();
            return getSwaggerType(p) + "[" + getTypeDeclaration(inner) + "]";
        }
        else if (p instanceof MapProperty) {
            MapProperty mp = (MapProperty) p;
            Property inner = mp.getAdditionalProperties();
            return getSwaggerType(p) + "[String, " + getTypeDeclaration(inner) + "]";
        }
        return super.getTypeDeclaration(p);
    }

    /**
     * Optional - swagger type conversion.  This is used to map swagger types in a `Property` into
     * either language specific types via `typeMapping` or into complex models if there is not a mapping.
     *
     * @return a string value of the type or complex model for this property
     * @see io.swagger.models.properties.Property
     */
    @Override
    public String getSwaggerType(Property p) {
        String swaggerType = super.getSwaggerType(p);
        String type = null;
        if(typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType);
            if(languageSpecificPrimitives.contains(type))
                return toModelName(type);
        }
        else
            type = swaggerType;
        return toModelName(type);
    }

    @Override
    public void processOpts() {
        additionalProperties.put(CodegenConstants.PACKAGE_NAME, PACKAGE_NAME);
        additionalProperties.put(CodegenConstants.PACKAGE_VERSION, PACKAGE_VERSION);
        additionalProperties.put(CodegenConstants.EXCLUDE_TESTS, EXCLUDE_TESTS);

        super.processOpts();

    }

    @Override
    public void postProcessParameter(CodegenParameter parameter){
        postProcessPattern(parameter.pattern, parameter.vendorExtensions);
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        // process enum in models
        return postProcessModelsEnum(objs);
    }

    // Transform CodegenOperation obj into ExtendedCodegenOperation to add properties into mustache maps
    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {

        final Map<String, Object> operationsMap = (Map<String, Object>) super.postProcessOperations(objs).get("operations");
        final List<CodegenOperation> operations = (List<CodegenOperation>) operationsMap.get("operation");
        final List<KillbillPythonGenerator.ExtendedCodegenOperation> extOperations = new ArrayList<>(operations.size());

        for (CodegenOperation op : operations) {
            final KillbillPythonGenerator.ExtendedCodegenOperation ext = new KillbillPythonGenerator.ExtendedCodegenOperation(op);
            extOperations.add(ext);

            ext.allParams = convertToExtendedCodegenParam(ext.allParams);
            ext.bodyParams = convertToExtendedCodegenParam(ext.bodyParams);
            ext.pathParams = convertToExtendedCodegenParam(ext.pathParams);
            ext.queryParams = convertToExtendedCodegenParam(ext.queryParams);
            ext.formParams = convertToExtendedCodegenParam(ext.formParams);
        }
        operationsMap.put("operation", extOperations);

        return objs;
    }

    private List<CodegenParameter> convertToExtendedCodegenParam(final List<CodegenParameter> input) {
        final List<CodegenParameter> result = new ArrayList<>(input.size());
        for (final CodegenParameter p : input) {

            final ExtendedCodegenParameter e = new ExtendedCodegenParameter(p);
            result.add(e);
        }
        return result;
    }

    private static class ExtendedCodegenOperation extends CodegenOperation {

        public boolean isGet,
                isPost,
                isDelete,
                isPut,
                isOptions,
                hasNonRequiredDefaultQueryParams,
                isStream;


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
                this.bodyParam.dataType = String.format("%ss", this.bodyParam.baseType);
            }
            if (returnContainer != null && returnContainer.equals("array")) {
                this.returnType = String.format("%ss", this.returnBaseType);
            }
            this.isStream = produces != null && !produces.isEmpty() && produces.get(0).get("mediaType").equals("application/octet-stream");
            this.hasNonRequiredDefaultQueryParams = Iterables.any(this.queryParams, new Predicate<CodegenParameter>() {
                @Override
                public boolean apply(CodegenParameter input) {
                    return !input.required && input.defaultValue != null;
                }
            });
        }
    }

    private static class ExtendedCodegenParameter extends CodegenParameter {

        public boolean isMandatoryParam;
        public boolean isQueryPluginProperty;

        public ExtendedCodegenParameter(CodegenParameter p) {
            //
            // We treat pluginProperty query params differently:
            // Instead of generating ' List<String> pluginProperty', we generate Map<String, String> pluginProperty
            // and some glue code to correctly serialize those as query param.
            //
            this.isQueryPluginProperty = p.baseName != null && p.baseName.equals("pluginProperty") && p.isQueryParam && p.isListContainer;
            this.isFile = p.isFile;
            this.notFile = p.notFile;
            this.hasMore = p.hasMore;
            this.isContainer = p.isContainer;
            this.secondaryParam = p.secondaryParam;
            this.baseName = p.baseName;
            this.dataType = isQueryPluginProperty ? "Map<String, String>" : p.dataType;
            this.datatypeWithEnum = p.datatypeWithEnum;
            this.enumName = p.enumName;
            this.dataFormat = p.dataFormat;
            this.collectionFormat = p.collectionFormat;
            this.isCollectionFormatMulti = p.isCollectionFormatMulti;
            this.isPrimitiveType = p.isPrimitiveType;
            this.description = p.description;
            this.unescapedDescription = p.unescapedDescription;
            this.baseType = p.baseType;
            this.isFormParam = p.isFormParam;
            this.isQueryParam = p.isQueryParam;
            this.isPathParam = p.isPathParam;
            this.isHeaderParam = p.isHeaderParam;
            this.isCookieParam = p.isCookieParam;
            this.isBodyParam = p.isBodyParam;
            this.required = p.required;
            this.maximum = p.maximum;
            this.exclusiveMaximum = p.exclusiveMaximum;
            this.minimum = p.minimum;
            this.exclusiveMinimum = p.exclusiveMinimum;
            this.maxLength = p.maxLength;
            this.minLength = p.minLength;
            this.pattern = p.pattern;
            this.maxItems = p.maxItems;
            this.minItems = p.minItems;
            this.uniqueItems = p.uniqueItems;
            this.multipleOf = p.multipleOf;
            this.jsonSchema = p.jsonSchema;
            this.defaultValue = p.defaultValue;
            this.example = p.example;
            this.isEnum = p.isEnum;
            this._enum = p._enum;
            this.allowableValues = p.allowableValues;
            this.items = p.items;
            this.vendorExtensions = p.vendorExtensions;
            this.hasValidation = p.hasValidation;
            this.isBinary = p.isBinary;
            this.isByteArray = p.isByteArray;
            this.isString = p.isString;
            this.isNumeric = p.isNumeric;
            this.isInteger = p.isInteger;
            this.isLong = p.isLong;
            this.isDouble = p.isDouble;
            this.isFloat = p.isFloat;
            this.isNumber = p.isNumber;
            this.isBoolean = p.isBoolean;
            this.isDate = p.isDate || (dataFormat != null && dataFormat.equalsIgnoreCase("date"));
            this.isDateTime = p.isDateTime || (dataFormat != null && dataFormat.equalsIgnoreCase("date-time"));
            this.isUuid = p.isUuid || (dataFormat != null && dataFormat.equalsIgnoreCase("uuid"));
            this.isListContainer = p.isListContainer && !isQueryPluginProperty;
            this.isMapContainer = p.isMapContainer || isQueryPluginProperty;
            this.isMandatoryParam = !isHeaderParam && (required || (isQueryParam && defaultValue == null));

            // Remove 'x_killbill_' prefix
            String str = "x_killbill_";
            if (p.paramName.contains(str)){
                this.paramName = p.paramName.replace(str, "");
            }
            else{
                this.paramName = p.paramName;
            }
        }
    }
}
