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
  echo "> ./build.sh -l <language> -o <output> [-d] [-w] " >&2
  echo "" >&2
  echo "# Example to generate code for java and wait for java debugger to start on port 5005: " >&2
  echo "> ./build.sh -l killbill-java -o ../killbill-client-java -w" >&2
  exit 1
}


function kb_curl() {
  $CURL --fail --silent -u $KB_USER_CREDS "$@"
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
  kb_api \
    "$KILLBILL_BASE_URL/swagger.yaml"
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
  cat >> $tmp/VERSION <<EOF
swaggerVersion=$SWAGGER_CODEGEN_VERSION
kbVersion=$kbVersion
kbApiVersion=$kbApiVersion
kbPluginApiVersion=$kbPluginApiVersion
EOF
}

function validate_and_return_api_jar() {

  local versionFile=$1

  echo "Fetching killbill api jar : $versionFile" >&2

  local kbApiVersion=`grep kbApiVersion $versionFile  | cut -d '=' -f 2`
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
  cp "$tmp/VERSION" $output/.swagger-codegen

  cp "$tmp/kbswagger.yaml" $output/.swagger-codegen
}

###############################################################################
#                                                                             #
#                                BUILD SCRIPT                                 #
#                                                                             #
###############################################################################


while getopts ":o:l:dw" options; do
  case $options in
        w ) WAIT_DEBUGGER=1;;
        o ) OUTPUT=$OPTARG;;
        l ) LANGUAGE=$OPTARG;;
    h ) usage;;
    * ) usage;;
  esac
done





# Create tmp dir
TMP=`mktemp -d`
echo "Temp directory TMP = $TMP" >&2

function cleanup {
  rm -rf $TMP
}
trap cleanup EXIT

# Fetch KB VERSIONS
NODE_INFO=`kb_node_info`

# Create VERSION FILE
write_version_file $NODE_INFO $TMP

# Extract KB api artifact jar
apiJar=`validate_and_return_api_jar "$TMP/VERSION"`

# Retrieve swagger.yml spec
kb_swagger > "$TMP/kbswagger.yaml"

head "$TMP/kbswagger.yaml"

# Run generator
generate_client_code $apiJar "$TMP/kbswagger.yaml" $LANGUAGE $OUTPUT $WAIT_DEBUGGER

# Copy VERSION file
copy_files $TMP $OUTPUT
