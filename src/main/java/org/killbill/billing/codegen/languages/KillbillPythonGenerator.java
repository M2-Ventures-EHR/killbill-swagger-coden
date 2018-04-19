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
}
