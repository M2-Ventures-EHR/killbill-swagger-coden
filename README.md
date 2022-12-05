# Kill Bill Client Code Generation

This repository is an extension of the [swagger coden module](https://github.com/swagger-api/swagger-codegen#making-your-own-codegen-modules),
and is used to generate our Kill Bill client libraries. It contains both a set of templates and java modules to allow to customize the generated client library.


The repo was generated using the [`meta` verb from the code generator](https://github.com/swagger-api/swagger-codegen#making-your-own-codegen-modules),
and then tweaked by hand to fit our use case. This initial step should only be needed once.

## Kill Bill Swagger Schema

The generator will require the Kill Bill swagger schema, and at this point we are using on swagger `1.5` (OA `2.0`).
The swagger schema can be obtained by hitting a running instance of Kill Bill:

```
curl http://127.0.0.1:8080/swagger.json > ./kbswagger.json 
```

Note that we can also export yaml using:
```
curl http://127.0.0.1:8080/swagger.yaml > ./kbswagger.yaml 
```


Note that there is some useful information about annotations in these pages.

* [github wiki](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X)
* [swagger doc](https://swagger.io/docs/specification/2-0/)



## Using the generator

**Pre-requisite:** Read the [swagger README](https://github.com/swagger-api/swagger-codegen/blob/master/README.md) to install the binary and get some understanding on how things work. The generator relies on `swagger-codegen` version `2.4.1`. In order to download the `swagger-codegen-cli` required by the `build.sh` script, you can run the following (This downloads the `swagger-codegen-cli` jar file to the local Maven repo) :


```
mvn org.apache.maven.plugins:maven-dependency-plugin:2.8:get \
    -Dartifact=io.swagger:swagger-codegen-cli:2.4.1
```

Note that upgrading to `2.4.19`, latest patch version at the time of this writing led to incorrect code generation -- no further investigation was done, perhaps an easy fix -- but until we investigate we are stuck on this version.


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

## Using the build.sh script

To simplify the build process, this repo includes a [build.sh](https://github.com/killbill/killbill-swagger-coden/blob/a8de9958143f1954f53ff6634594e13f1c43909d/build.sh) script. We recommend that you use this script to build the client libraries. This script does the following:

* Retrieves the KB version (requires a running instance of Kill Bill).
* Retrieves the swagger spec (requires a running instance of Kill Bill).
* Runs the generator.
* Copies the `VERSION` file, under the client `.swagger-codegen` directory to keep track of the api against which this was generated.

In order to run this script, do the following:

* Ensure that you have a running Kill Bill instance
* Build this repo to generate the `killbill-swagger-codegen.jar` file as follows:

        mvn clean install -DskipTests=true
* Update `build.sh` as desired (You may need to update the `killbill-swagger-coden.jar` path, path of your local maven repo, etc.).
* Run the script as follows (On Windows, you can use [Git Bash](https://git-scm.com/download/win) to run this script):

        sh build.sh -l killbill-java -o ../killbill-client-java 
    
* To generate the KB client code and wait for debugger to start on port 5005 use:


        > ./build.sh -l killbill-java -o ../killbill-client-java -w


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

## Modifying the generator

Modifying the generator, including the Mustache template would require building the repo: `mvn -DskipTests=true -Dmaven.javadoc.skip=true install`

# Supported Languages

## Java

There is a swagger module called `killbill-java`, that is used to generate the java client library. As explained earlier
it consists of some templates and code module:

* [KillbillJavaGenerator](https://github.com/killbill/killbill-swagger-coden/blob/master/src/main/java/org/killbill/billing/codegen/languages/KillbillJavaGenerator.java)
* [killbill-java](https://github.com/killbill/killbill-swagger-coden/tree/master/src/main/resources/killbill-java) templates.

The code generation has been limited to generating model and api files, but at this point we have decided to reuse our existing http client, and reuse the mechanism we have in place, [RequestOptions](https://github.com/killbill/killbill-client-java/blob/killbill-client-java-0.41.7/src/main/java/org/killbill/billing/client/RequestOptions.java) --to pass additional headers through our apis.

In order to generate the Java client, use:

```
sh build.sh -l killbill-java -o ../killbill-client-java 
```


## Python

There is a swagger module called `killbill-python`, that is used to generate the python client library.

* [KillbillPythonGenerator](https://github.com/killbill/killbill-swagger-coden/blob/master/src/main/java/org/killbill/billing/codegen/languages/KillbillPythonGenerator.java)
* [killbill-python](https://github.com/killbill/killbill-swagger-coden/tree/master/src/main/resources/killbill-python) templates.


To generate the client enter the following command:

```sh
> ./build.sh -l killbill-python -o ../killbill-client-python
```
Note: If you don't want to generate the API docs, tests, and `git_push.hs` file just create a file named `.swagger-codegen-ignore` in the output directory.
with the following:
```
docs/*.md
test/*
git_push.sh
```
[Reff: How to skip certain files during code generation?](https://github.com/swagger-api/swagger-codegen/wiki/FAQ#how-to-skip-certain-files-during-code-generation)