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

While generating the client code, it is possible that some of the annotations are missing or incorrect, leading to incorrect generation
and so in this case, such (server) annotations should be modified. There is a special Kill Bill branch, `work-for-release-0.19.x-doc`,
which can be used to submit PRs for that purpose.

Note that there is some usefull information about annotations in these pages.

* [github wiki](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X)
* [wagger doc](https://swagger.io/docs/specification/2-0/)


## Using the generator

**Pre-requisite:** Read the [swagger README](https://github.com/swagger-api/swagger-codegen/blob/master/README.md) to install the binary
and get some understanding on how things work.


Then, we can generate client generated code; for instance for `java`, we would run the following command:

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

Note that, it may be useful to start the generator using the jar and potentially attach a debugger (e.g port `5005`):

```
java \
-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005 \
-DmodelDocs=false \
-DgenerateApiDocs=false \
-cp target/killbill-swagger-codegen-1.0.0.jar \
io.swagger.codegen.Codegen \
-l killbill-java  \
-i kbswagger.json \
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

So, in summary, the `kbswagger.json` input along with the custom code and templates provide a flexible way to generate client libraries in any language.

# Supported Languages

## Java

There is a new swagger module called `killbill-java`, that is used to generate the java client library. As explained earlier
it consists of some templates and code module:

* [KillbillJavaGenerator](https://github.com/killbill/killbill-swagger-coden/blob/master/src/main/java/org/killbill/billing/codegen/languages/KillbillJavaGenerator.java)
* [killbill-java](https://github.com/killbill/killbill-swagger-coden/tree/master/src/main/resources/killbill-java) templates.




