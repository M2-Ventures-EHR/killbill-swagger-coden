#!/bin/bash


#set -x

###############################################################################
#                                                                             #
#                             !! EDIT AS NEEDED !!                            #
#                                                                             #
###############################################################################

#
# RUNNING INSTANCE OF KILL BILL
#
KILLBILL_BASE_URL=http://127.0.0.1:8080
KB_USER_CREDS="admin:password"
KB_API_KEY=bob
KB_API_SECRET=lazar


# UPDATE BASED ON ENVIRONMENT
CURL="curl"
JQ="jq"
JAVA="java"


#
# LOCATION OF MAVEN .M2
#
M2="$HOME/.m2"

#
# LOCATION OF THE SWAGGER swagger-codegen-cli.jar
#
SWAGGER_CODEGEN_VERSION="2.4.0-SNAPSHOT"
SWAGGER_CODEGEN_JAR="$M2/repository/io/swagger/swagger-codegen-cli/$SWAGGER_CODEGEN_VERSION/swagger-codegen-cli-$SWAGGER_CODEGEN_VERSION.jar"

KB_SWAGGER_CODEGEN_VERSION="1.0.0"
KB_SWAGGER_CODEGEN_JAR="target/killbill-swagger-coden-$KB_SWAGGER_CODEGEN_VERSION.jar"


###############################################################################
#                                                                             #
#                                SANITY                                       #
#                                                                             #
###############################################################################


$CURL --version > /dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "Abort: Need to have a curl command setup in the PATH" >&2
  exit 1;
fi

$JQ --version > /dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "Abort: Need to have a jq command setup in the PATH" >&2
  exit 1;
fi

$JAVA -version > /dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "Abort: Need to have a java command setup in the PATH" >&2
  exit 1;
fi

if [ ! -d $M2 ]; then
  echo "Abort: Cannot find .m2 directory under $M2 " >&2
  exit 1;
fi

if [ ! -f $SWAGGER_CODEGEN_JAR ]; then
  echo "Abort: Cannot find swagger client jar $SWAGGER_CODEGEN_JAR  " >&2
  exit 1;
fi

if [ ! -f $KB_SWAGGER_CODEGEN_JAR ]; then
  echo "Abort: Cannot find KB swagger client jar $KB_SWAGGER_CODEGEN_JAR  " >&2
  exit 1;
fi

if [ ! -d $OUTPUT ]; then
  echo "Abort: output directory $OUTPUT does not exist " >&2
  exit 1;
fi


###############################################################################
#                                                                             #
#                                UTIL CMDS                                    #
#                                                                             #
###############################################################################

function usage() {
  echo "> ./build.sh -l <language> -o <output> []-v] [-d] [-w] " >&2
  echo "" >&2
  echo "# Example to generate code for java, run validation, and wait for java debugger to start on port 5005: " >&2
  echo "> ./build.sh -l killbill-java -o ../killbill-client-java -v -w" >&2
  echo "" >&2
  echo "# Example to generate code from input file and api version " >&2
  echo "> ./build.sh -l killbill-java -o ../killbill-client-java -i <swagger.yaml> -a <killbill_api_version>" >&2
  exit 1
}

function curl_with_err() {
  $CURL --fail --silent "$@"
  local ret=$?
  if [ $ret -ne 0 ]; then
    if [ $ret -eq 7 ]; then
        echo "CURL: Fail to connect to host " >&2
    else
        echo "CURL: Failed " >&2
    fi
    echo "EXIT...... " >&2
    exit 1
  fi
}


function kb_curl() {
  curl_with_err -u $KB_USER_CREDS "$@"
  local ret=$?
  if [ $ret -ne 0 ]; then
    if [ $ret -eq 7 ]; then
        echo "CURL: Fail to connect to host " >&2
    else
        echo "CURL: Failed " >&2
    fi
    echo "EXIT...... " >&2
    exit 1
  fi
}

function kb_api() {
  kb_curl \
    -H "X-Killbill-ApiKey: $KB_API_KEY" \
    -H "X-Killbill-ApiSecret: $KB_API_SECRET" \
    "$@"
}

#
# KB APIs
#

function kb_node_info() {
  kb_api \
    -H "Content-Type: application/json" \
    "$KILLBILL_BASE_URL/1.0/kb/nodesInfo"
}

function kb_swagger() {
  local format=$1
  kb_api \
    "$KILLBILL_BASE_URL/swagger.$format"
}

function validate_schema() {
  local tmp=$1
  local output="$tmp/swagger_validation.json"
  
  kb_swagger 'json' > "$tmp/swagger.json"
  curl_with_err \
        -X POST \
        -d @/tmp/swagger.json \
        -H 'Content-Type:application/json' \
        -o $output \
        http://online.swagger.io/validator/debug
  
  r=`cat $output | jq '.schemaValidationMessages'`
  
  if [ $r == "null" ]; then
    echo "*** Schema Validation Successful ! ";
  else
    echo "*** Schema Validation Failed ! "
    cat $output
  fi
}


function extract_version() {
  local input=$1
  local key=$2
  echo $input |  jq ".[0].$key" | sed 's/\"//g'
}


function write_version_file() {


  echo "Extracting node info from Kill Bill" >&2

  local node_info=$1
  local tmp=$2
  local kbVersion=`extract_version $node_info "kbVersion"`
  local kbApiVersion=`extract_version $node_info "apiVersion"`
  local kbPluginApiVersion=`extract_version $node_info "pluginApiVersion"`
  cat >> $tmp/KB_VERSION <<EOF
swaggerVersion=$SWAGGER_CODEGEN_VERSION
kbVersion=$kbVersion
kbApiVersion=$kbApiVersion
kbPluginApiVersion=$kbPluginApiVersion
EOF
}

function validate_and_return_api_jar() {

  local kbApiVersion=$1
  local kbapi="$M2/repository/org/kill-bill/billing/killbill-api/$kbApiVersion/killbill-api-$kbApiVersion.jar"
  if [ ! -f $kbapi ]; then
    echo "Abort: Cannot find kb api jar $kbapi  " >&2
    exit 1;
  fi
  echo $kbapi
}


function generate_client_code() {

  local apiJar=$1
  local swaggerInput=$2
  local client=$3
  local output=$4
  local wait_for_debug=$5

  echo "Generating client code for language $client into $$output" >&2

  local java_debug=
  if  [ ! -z $wait_for_debug ]; then
    java_debug=" -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 "
    echo "Waiting for debug to connect on port 5005..." >&2
    echo "" >&2
  fi

  $JAVA $java_debug\
  -DkbApiJar=$apiJar \
  -DapiDocs=false \
  -DapiTests=false \
  -DmodelDocs=false \
  -DgenerateApiDocs=false \
  -cp $SWAGGER_CODEGEN_JAR:$KB_SWAGGER_CODEGEN_JAR \
  io.swagger.codegen.SwaggerCodegen generate \
  -l $client  \
  -i $swaggerInput  \
  -o $output
}

function copy_files() {

  local tmp=$1
  local output=$2

  echo "Copying version under $output/.swagger-codegen " >&2

  if [ ! -d $output/.swagger-codegen ]; then
    mkdir $output/.swagger-codegen
  fi
  cp "$tmp/KB_VERSION" $output/.swagger-codegen

  cp "$tmp/kbswagger.yaml" $output/.swagger-codegen
}

###############################################################################
#                                                                             #
#                                BUILD SCRIPT                                 #
#                                                                             #
###############################################################################


while getopts ":o:i:a:l:vdw" options; do
  case $options in
        w ) WAIT_DEBUGGER=1;;
        v ) VALIDATE_SCHEMA=1;;
        o ) OUTPUT=$OPTARG;;
        l ) LANGUAGE=$OPTARG;;
        i ) INPUT=$OPTARG;;        
        a ) API_VERSION=$OPTARG;;
    h ) usage;;
    * ) usage;;
  esac
done


TMP=
# Retrieve swagger.yml spec
if [ -z $INPUT ]; then

    # Create tmp dir
    TMP=`mktemp -d`
    echo "Temp directory TMP = $TMP" >&2

    function cleanup {
        if [ -d $TMP ]; then
            rm -rf $TMP
        fi
    }
    trap cleanup EXIT


    # Fetch KB VERSIONS
    NODE_INFO=`kb_node_info`

    # Create VERSION FILE
    write_version_file $NODE_INFO $TMP

    # Extract Api version
    API_VERSION=`grep kbApiVersion "$TMP/KB_VERSION"  | cut -d '=' -f 2`

    # Validate schema
    if [ ! -z $VALIDATE_SCHEMA ]; then
      validate_schema $TMP
    fi

    kb_swagger "yaml" > "$TMP/kbswagger.yaml"
    INPUT="$TMP/kbswagger.yaml"
    
else
    if [ ! -f $INPUT ]; then
       echo "Abort: Cannot find swagger spec $INPUT  " >&2
       exit 1;
    fi
    if [ -z API_VERSION ]; then
       echo "Abort: Requires to pass KB api version when using input swagger file (Kill Bill servernot running) " >&2
       exit 1;
    fi
fi

# Extract KB api artifact jar
API_JAR=`validate_and_return_api_jar $API_VERSION`


# Run generator
generate_client_code $API_JAR $INPUT $LANGUAGE $OUTPUT $WAIT_DEBUGGER


# Copy VERSION file
if [ ! -z $TMP ]; then
    copy_files $TMP $OUTPUT
fi

