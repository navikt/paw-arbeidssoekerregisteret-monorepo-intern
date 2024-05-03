@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  api-perioder startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and API_PERIODER_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\api-perioder.jar;%APP_HOME%\lib\arbeidssoekerregisteret-kotlin-24.03.25.160-1.jar;%APP_HOME%\lib\interne-eventer-24.03.25.160-1.jar;%APP_HOME%\lib\kafka-24.02.21.12-1.jar;%APP_HOME%\lib\hoplite-config-24.02.21.12-1.jar;%APP_HOME%\lib\token-validation-ktor-v2-3.1.5.jar;%APP_HOME%\lib\opentelemetry-ktor-2.0-2.1.0-alpha.jar;%APP_HOME%\lib\opentelemetry-extension-kotlin-1.35.0.jar;%APP_HOME%\lib\pdl-client-24.01.12.26-1.jar;%APP_HOME%\lib\graphql-kotlin-ktor-client-7.0.1.jar;%APP_HOME%\lib\graphql-kotlin-client-serialization-7.0.1.jar;%APP_HOME%\lib\graphql-kotlin-client-7.0.1.jar;%APP_HOME%\lib\ktor-serialization-jackson-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-server-netty-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-server-metrics-micrometer-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-server-auth-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-server-call-id-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-server-content-negotiation-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-server-cors-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-server-openapi-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-server-status-pages-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-server-swagger-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-server-host-common-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-server-sessions-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-server-call-logging-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-server-html-builder-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-server-core-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-client-cio-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-client-content-negotiation-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-client-okhttp-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-client-serialization-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-client-json-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-client-core-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-websocket-serialization-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-serialization-kotlinx-json-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-serialization-kotlinx-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-serialization-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-events-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-http-cio-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-websockets-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-network-tls-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-http-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-network-jvm-2.3.9.jar;%APP_HOME%\lib\ktor-utils-jvm-2.3.9.jar;%APP_HOME%\lib\client-2024.03.04_10.19-63a652788672.jar;%APP_HOME%\lib\hoplite-toml-2.8.0.RC3.jar;%APP_HOME%\lib\hoplite-yaml-2.8.0.RC3.jar;%APP_HOME%\lib\hoplite-core-2.8.0.RC3.jar;%APP_HOME%\lib\api-2024.03.04_10.19-63a652788672.jar;%APP_HOME%\lib\kotlinx-coroutines-slf4j-1.7.3.jar;%APP_HOME%\lib\ktor-io-jvm-2.3.9.jar;%APP_HOME%\lib\kotlinx-coroutines-core-jvm-1.7.3.jar;%APP_HOME%\lib\kotlinx-coroutines-jdk8-1.7.3.jar;%APP_HOME%\lib\rest-3.2023.09.13_04.55-a8ff452fbd94.jar;%APP_HOME%\lib\okhttp-4.12.0.jar;%APP_HOME%\lib\kotlin-stdlib-jdk8-1.9.22.jar;%APP_HOME%\lib\kotlin-stdlib-jdk7-1.9.22.jar;%APP_HOME%\lib\kotlinx-serialization-core-jvm-1.5.1.jar;%APP_HOME%\lib\kotlinx-serialization-json-jvm-1.5.1.jar;%APP_HOME%\lib\kotlinx-html-jvm-0.9.1.jar;%APP_HOME%\lib\swagger-codegen-generators-1.0.38.jar;%APP_HOME%\lib\swagger-codegen-3.0.41.jar;%APP_HOME%\lib\swagger-parser-2.1.19.jar;%APP_HOME%\lib\swagger-parser-v2-converter-2.1.19.jar;%APP_HOME%\lib\swagger-parser-v3-2.1.19.jar;%APP_HOME%\lib\swagger-core-2.2.19.jar;%APP_HOME%\lib\jackson-datatype-jsr310-2.16.1.jar;%APP_HOME%\lib\log-3.2024.02.21_11.18-8f9b43befae1.jar;%APP_HOME%\lib\logstash-logback-encoder-7.4.jar;%APP_HOME%\lib\json-3.2023.09.13_04.55-a8ff452fbd94.jar;%APP_HOME%\lib\jackson-datatype-jdk8-2.16.1.jar;%APP_HOME%\lib\swagger-codegen-2.4.30.jar;%APP_HOME%\lib\swagger-compat-spec-parser-1.0.68.jar;%APP_HOME%\lib\swagger-parser-1.0.68.jar;%APP_HOME%\lib\swagger-core-1.6.12.jar;%APP_HOME%\lib\jackson-dataformat-yaml-2.16.1.jar;%APP_HOME%\lib\json-patch-1.13.jar;%APP_HOME%\lib\json-schema-validator-2.2.14.jar;%APP_HOME%\lib\json-schema-core-1.2.14.jar;%APP_HOME%\lib\jackson-coreutils-equivalence-1.0.jar;%APP_HOME%\lib\jackson-coreutils-2.0.jar;%APP_HOME%\lib\jackson-databind-2.16.1.jar;%APP_HOME%\lib\swagger-parser-core-2.1.19.jar;%APP_HOME%\lib\swagger-models-2.2.19.jar;%APP_HOME%\lib\swagger-models-1.6.12.jar;%APP_HOME%\lib\jackson-annotations-2.16.1.jar;%APP_HOME%\lib\jackson-core-2.16.1.jar;%APP_HOME%\lib\jackson-module-kotlin-2.16.1.jar;%APP_HOME%\lib\kotlin-reflect-1.9.22.jar;%APP_HOME%\lib\okio-jvm-3.7.0.jar;%APP_HOME%\lib\kotlin-stdlib-1.9.22.jar;%APP_HOME%\lib\opentelemetry-instrumentation-annotations-2.1.0.jar;%APP_HOME%\lib\opentelemetry-ktor-common-2.1.0-alpha.jar;%APP_HOME%\lib\opentelemetry-instrumentation-api-incubator-2.1.0-alpha.jar;%APP_HOME%\lib\opentelemetry-instrumentation-api-2.1.0.jar;%APP_HOME%\lib\opentelemetry-extension-incubator-1.35.0-alpha.jar;%APP_HOME%\lib\opentelemetry-api-1.35.0.jar;%APP_HOME%\lib\micrometer-registry-prometheus-1.12.3.jar;%APP_HOME%\lib\token-client-core-3.1.5.jar;%APP_HOME%\lib\token-client-3.2024.02.21_11.18-8f9b43befae1.jar;%APP_HOME%\lib\audit-log-3.2024.02.21_11.18-8f9b43befae1.jar;%APP_HOME%\lib\logback-syslog4j-1.0.0.jar;%APP_HOME%\lib\simpleclient_logback-0.16.0.jar;%APP_HOME%\lib\logback-classic-1.5.2.jar;%APP_HOME%\lib\kafka-clients-3.6.0.jar;%APP_HOME%\lib\annotations-23.0.0.jar;%APP_HOME%\lib\token-validation-core-3.1.5.jar;%APP_HOME%\lib\util-3.2024.02.21_11.18-8f9b43befae1.jar;%APP_HOME%\lib\jcl-over-slf4j-2.0.9.jar;%APP_HOME%\lib\slf4j-ext-1.7.36.jar;%APP_HOME%\lib\handlebars-4.3.1.jar;%APP_HOME%\lib\slf4j-api-2.0.12.jar;%APP_HOME%\lib\config-1.4.3.jar;%APP_HOME%\lib\jansi-2.4.1.jar;%APP_HOME%\lib\opentelemetry-context-1.35.0.jar;%APP_HOME%\lib\micrometer-core-1.12.3.jar;%APP_HOME%\lib\simpleclient_common-0.16.0.jar;%APP_HOME%\lib\oauth2-oidc-sdk-11.2.jar;%APP_HOME%\lib\nimbus-jose-jwt-9.35.jar;%APP_HOME%\lib\jakarta.validation-api-3.0.2.jar;%APP_HOME%\lib\caffeine-3.1.8.jar;%APP_HOME%\lib\logback-core-1.5.2.jar;%APP_HOME%\lib\janino-3.1.10.jar;%APP_HOME%\lib\tomlj-1.1.0.jar;%APP_HOME%\lib\snakeyaml-2.2.jar;%APP_HOME%\lib\netty-codec-http2-4.1.106.Final.jar;%APP_HOME%\lib\alpn-api-1.1.3.v20160715.jar;%APP_HOME%\lib\netty-transport-native-kqueue-4.1.106.Final.jar;%APP_HOME%\lib\netty-transport-native-epoll-4.1.106.Final.jar;%APP_HOME%\lib\opentelemetry-semconv-1.23.1-alpha.jar;%APP_HOME%\lib\micrometer-observation-1.12.3.jar;%APP_HOME%\lib\micrometer-commons-1.12.3.jar;%APP_HOME%\lib\HdrHistogram-2.1.12.jar;%APP_HOME%\lib\LatencyUtils-2.0.3.jar;%APP_HOME%\lib\simpleclient-0.16.0.jar;%APP_HOME%\lib\zstd-jni-1.5.5-1.jar;%APP_HOME%\lib\lz4-java-1.8.0.jar;%APP_HOME%\lib\snappy-java-1.1.10.4.jar;%APP_HOME%\lib\jcip-annotations-1.0-1.jar;%APP_HOME%\lib\uri-template-0.10.jar;%APP_HOME%\lib\guava-32.1.3-jre.jar;%APP_HOME%\lib\checker-qual-3.37.0.jar;%APP_HOME%\lib\error_prone_annotations-2.21.1.jar;%APP_HOME%\lib\content-type-2.2.jar;%APP_HOME%\lib\json-smart-2.4.11.jar;%APP_HOME%\lib\lang-tag-1.7.jar;%APP_HOME%\lib\syslog4j-0.9.30.jar;%APP_HOME%\lib\commons-compiler-3.1.10.jar;%APP_HOME%\lib\jakarta.ws.rs-api-3.1.0.jar;%APP_HOME%\lib\jakarta.servlet-api-6.0.0.jar;%APP_HOME%\lib\antlr4-runtime-4.11.1.jar;%APP_HOME%\lib\netty-codec-http-4.1.106.Final.jar;%APP_HOME%\lib\netty-handler-4.1.106.Final.jar;%APP_HOME%\lib\netty-codec-4.1.106.Final.jar;%APP_HOME%\lib\netty-transport-classes-kqueue-4.1.106.Final.jar;%APP_HOME%\lib\netty-transport-classes-epoll-4.1.106.Final.jar;%APP_HOME%\lib\netty-transport-native-unix-common-4.1.106.Final.jar;%APP_HOME%\lib\netty-transport-4.1.106.Final.jar;%APP_HOME%\lib\netty-buffer-4.1.106.Final.jar;%APP_HOME%\lib\netty-resolver-4.1.106.Final.jar;%APP_HOME%\lib\netty-common-4.1.106.Final.jar;%APP_HOME%\lib\simpleclient_tracer_otel-0.16.0.jar;%APP_HOME%\lib\simpleclient_tracer_otel_agent-0.16.0.jar;%APP_HOME%\lib\accessors-smart-2.4.11.jar;%APP_HOME%\lib\jmustache-1.15.jar;%APP_HOME%\lib\swagger-parser-safe-url-resolver-2.1.19.jar;%APP_HOME%\lib\commons-io-2.15.0.jar;%APP_HOME%\lib\commons-lang3-3.13.0.jar;%APP_HOME%\lib\commons-cli-1.5.0.jar;%APP_HOME%\lib\simpleclient_tracer_common-0.16.0.jar;%APP_HOME%\lib\asm-9.3.jar;%APP_HOME%\lib\json-20220924.jar;%APP_HOME%\lib\failureaccess-1.0.1.jar;%APP_HOME%\lib\listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar;%APP_HOME%\lib\msg-simple-1.2.jar;%APP_HOME%\lib\btf-1.3.jar;%APP_HOME%\lib\jsr305-3.0.2.jar;%APP_HOME%\lib\commonmark-0.17.0.jar;%APP_HOME%\lib\jakarta.xml.bind-api-2.3.3.jar;%APP_HOME%\lib\validation-api-1.1.0.Final.jar;%APP_HOME%\lib\httpclient-4.5.14.jar;%APP_HOME%\lib\swagger-annotations-2.2.19.jar;%APP_HOME%\lib\jakarta.activation-api-1.2.2.jar;%APP_HOME%\lib\swagger-annotations-1.6.12.jar;%APP_HOME%\lib\mailapi-1.6.2.jar;%APP_HOME%\lib\joda-time-2.10.5.jar;%APP_HOME%\lib\libphonenumber-8.11.1.jar;%APP_HOME%\lib\jopt-simple-5.0.4.jar;%APP_HOME%\lib\httpcore-4.4.16.jar;%APP_HOME%\lib\commons-logging-1.2.jar;%APP_HOME%\lib\commons-codec-1.11.jar;%APP_HOME%\lib\rhino-1.7.7.2.jar


@rem Execute api-perioder
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %API_PERIODER_OPTS%  -classpath "%CLASSPATH%" no.nav.paw.arbeidssokerregisteret.ApplicationKt %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable API_PERIODER_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%API_PERIODER_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
