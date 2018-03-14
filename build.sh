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

# TODO SCRIPT PARAMS
CLIENT="killbill-java"
OUTPUT="/Users/sbrossier/Src/killbill/killbill-client-java"


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


$CURL --version > /dev/null
if [ $? -ne 0 ]; then
  echo "Abort: Need to have a curl command setup in the PATH" >&2
  exit 1;
fi

$JQ --version > /dev/null
if [ $? -ne 0 ]; then
  echo "Abort: Need to have a jq command setup in the PATH" >&2
  exit 1;
fi

$JAVA -version > /dev/null
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

function myecho() {
  for i in "$@"; do
    case "$i" in
      *\ *)
        echo -n "\"$i\" " ;;
      *\&*)
        echo -n "\"$i\" " ;;
      *)
      echo -n "$i "
      ;;
    esac
  done
  echo ""
}

function kb_curl() {
  myecho $CURL "$@" >&2
  $CURL --fail --silent -u $KB_USER_CREDS "$@"
  local ret=$?
  if [ $ret -ne 0 ]; then
    myecho "Failed to execute command:" $CURL_CMD "$@"
    exit 1
  fi
  echo ""
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


#
#
#
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

  echo "Generating client code for language $client into $$output" >&2

  $JAVA \
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

function copy_version_file() {

  local tmp=$1
  local output=$2

  echo "Copying version under $output/.swagger-codegen " >&2

  if [ ! -d $output/.swagger-codegen ]; then
    mkdir $output/.swagger-codegen
  fi
  cp "$tmp/VERSION" $output/.swagger-codegen
}

###############################################################################
#                                                                             #
#                                BUILD SCRIPT                                 #
#                                                                             #
###############################################################################



# Create tmp dir
TMP=`mktemp -d`
echo "Temp directory TMP = $TMP" >&2

function cleanup {
  rm -rf $TMP
}
trap cleanup EXIT


# Create VERSION FILE
write_version_file `kb_node_info` $TMP

# Extract KB api artifact jar
apiJar=`validate_and_return_api_jar "$TMP/VERSION"`

echo "apiJar is $apiJar"

# Retrieve swagger.yml spec
kb_swagger > "$TMP/kbswagger.yaml"

head "$TMP/kbswagger.yaml"

# Run generator
generate_client_code $apiJar "$TMP/kbswagger.yaml" $CLIENT $OUTPUT

# Copy VERSION file
copy_version_file $TMP $OUTPUT
