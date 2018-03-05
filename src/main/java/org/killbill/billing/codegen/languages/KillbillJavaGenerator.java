package org.killbill.billing.codegen.languages;

import io.swagger.codegen.*;
import io.swagger.codegen.languages.AbstractJavaCodegen;
import io.swagger.codegen.languages.JavaClientCodegen;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.models.properties.*;

import java.util.*;
import java.io.File;

public class KillbillJavaGenerator extends AbstractJavaCodegen implements CodegenConfig {


  private static final String NAME = "killbill-java";


  private static final String GROUP_ID = "org.kill-bill.billing";
  private static final String ARTIFACT_ID = "killbill-client-java-gen";
  private static final String VERSION = "0.0.1-SNAPSHOT";

  private static final String DOCS_PATH = "docs/";



  private static final String MAIN_JAVA = "main/java";
  private static final String MAIN_RESOURCE = "main/resources";

  private static final String API_PACKAGE = "org.killbill.billing.client.gen.api";
  private static final String MODEL_PACKAGE = "org.killbill.billing.client.gen.model";

  private static final String HTTP_USER_AGENT = "killbill/java-client";

  private static final String DATE_LIBRARY_JODA = "joda";
  private static final Boolean BIG_DECIMAL_AS_STRING = true;


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

  public KillbillJavaGenerator() {
    super();
    templateDir = "killbill-java";
    embeddedTemplateDir = "killbill-java";
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

    serializeBigDecimalAsString = BIG_DECIMAL_AS_STRING;
    hideGenerationTimestamp = true; /* Does not seem to work, use cli option to disable */
  }


  // Transform CodegenOperation obj into ExtendedCodegenOperation to add properties into mustache maps
  @Override
  public Map<String, Object> postProcessOperations(Map<String, Object> objs) {

    final Map<String, Object> operationsMap = (Map<String, Object>) super.postProcessOperations(objs).get("operations");
    List<CodegenOperation> operations = (List<CodegenOperation>) operationsMap.get("operation");
    List<ExtendedCodegenOperation> extOperations = new ArrayList<>(operations.size());

    for (CodegenOperation op : operations) {
      final ExtendedCodegenOperation ext = new ExtendedCodegenOperation(op);
      extOperations.add(ext);
    }
    operationsMap.put("operation", extOperations);
    return objs;
  }

  private static class ExtendedCodegenOperation extends CodegenOperation {

    public boolean isGet, isPost, isDelete, isPut, isOptions;

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
    }
  }

}