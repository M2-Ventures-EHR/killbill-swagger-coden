# Kill Bill Client Code Generation

This repository is an extension of the [swagger coden module](https://github.com/swagger-api/swagger-codegen#making-your-own-codegen-modules),
and is used to generate our Kill Bill client libraries. It contains both a set of templates and and java modules to allow to customize the generated client library.


The repo was generated using the [`meta` verb from the code generator](https://github.com/swagger-api/swagger-codegen#making-your-own-codegen-modules),
and then tweaked by hand to fit our use case. This initial step should only be needed once.

## Kill Bill Swagger Schema

The generator will require the Kill Bill swagger schema, and at this point we are using on swagger `1.5` (OA `2.0`).
The swagger schema can be obtained by hitting a running instance of Kill Bill:

```
curl http://127.0.0.1:8080/swagger.json > ./kbswagger.json 
```

Note that we can also export yam using:
```
curl http://127.0.0.1:8080/swagger.yaml > ./kbswagger.yaml 
```

While generating the client code, it is possible that some of the annotations are missing or incorrect, leading to incorrect generation
and so in this case, such (server) annotations should be modified. There is a special Kill Bill branch, `work-for-release-0.19.x-doc`,
which can be used to submit PRs for that purpose.

Note that there is some usefull information about annotations in these pages.

* [github wiki](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X)
* [wagger doc](https://swagger.io/docs/specification/2-0/)


## Using the generator

**Pre-requisite:** Read the [swagger README](https://github.com/swagger-api/swagger-codegen/blob/master/README.md) to install the binary and get some understanding on how things work.


The standard command to generate the code for `java` would be:

```
swagger-codegen  generate \
--invoker-package="client" \
--api-package="api" \
--model-package="models" \
--http-user-agent="killbill/java-client" \
--lang=java \
-i kbswagger.json \
-o ../killbill-client-java # Repo where to generate the code
```

However, because we are using a custom module -- and also because we may want to set breakpoints to debug -- a better option
is to launch the genrator this way, specifying both the ``swagger-codegen-cli.jar` and our module jar:

So, asssuming the path for `swagger-codegen-cli.jar` is set into `GEN_JAR` env variable:

```
java \
-DapiDocs=false \
-DapiTests=false \
-DmodelDocs=false \
-DgenerateApiDocs=false \
-cp $GEN_JAR:generator/target/killbill-swagger-coden-1.0.0.jar \
io.swagger.codegen.SwaggerCodegen generate \
-l killbill-java  \
-i kbswagger.json  \
-o ../killbill-java-client
```

## Internals

The generator will rely on the swagger gen extensions to customize the code. For each supported language,
we will find a generator under the [languages package](https://github.com/killbill/killbill-swagger-coden/tree/master/src/main/java/org/killbill/billing/codegen/languages).
The customization is done by implementing/overriding methods from the [CodegenConfig](https://github.com/swagger-api/swagger-codegen/blob/master/modules/swagger-codegen/src/main/java/io/swagger/codegen/CodegenConfig.java)
interface.

The code customization allows for the following:

* Allows to define basic behavior (e.g generate tmestamp)
* Allows to define mappings (e.g type mappings)
* Allows to customize various input and output location (e.g output folder)
* Allows to implement hooks during generation to customize the object map passed to the Mustache engine (e.g define variable that can be used during template execution)

In addition to code customization, we can also define our own Mustache templates to generate the client code we want to have.

Modifiying the generator, including the Mustache template would require building the repo: `mvn -DskipTests=true -Dmaven.javadoc.skip=true install`

So, in summary, the `kbswagger.json` input along with the custom code and templates provide a flexible way to generate client libraries in any language.



# Supported Languages

## Java

There is a new swagger module called `killbill-java`, that is used to generate the java client library. As explained earlier
it consists of some templates and code module:

* [KillbillJavaGenerator](https://github.com/killbill/killbill-swagger-coden/blob/master/src/main/java/org/killbill/billing/codegen/languages/KillbillJavaGenerator.java)
* [killbill-java](https://github.com/killbill/killbill-swagger-coden/tree/master/src/main/resources/killbill-java) templates.

The code generation has been limited to generating model and api files, but at this point we have decided to reuse our existing http client, and reuse the mechanism we have in place, [RequestOptions](https://github.com/killbill/killbill-client-java/blob/killbill-client-java-0.41.7/src/main/java/org/killbill/billing/client/RequestOptions.java) --to pass additional headers through our apis.


The generated code has been check-in in the existing Kill Bill [client java repo](https://github.com/killbill/killbill-client-java), but in a branch called `swagger-gen`. Note that, at this point this is still an experiment, and we don't know whether this branch will eventually be merged.




