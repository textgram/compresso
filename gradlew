#!/bin/sh

PRG="$0"
APP_HOME=$(cd "$(dirname "$PRG")" > /dev/null && pwd)

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

MAX_FD="maximum"

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

os=$(uname)
case "$os" in
    CYGWIN* )
        cygwin=true
        ;;
    Darwin* )
        darwin=true
        ;;
    MINGW* )
        msys=true
        ;;
    NONSTOP* )
        nonstop=true
        ;;
esac

if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

if [ "$cygwin" = "true" -o "$msys" = "true" ] ; then
    APP_HOME=$(cygpath --path --mixed "$APP_HOME")
    JAVACMD=$(cygpath --unix "$JAVACMD")

    for arg do
        if
            case $arg in
                -*)   false ;;
                /?*)  t=${arg#/} t=/${t%%/*}
                      [ -e "$t" ] ;;
                *)    false ;;
            esac
        then
            arg=$(cygpath --path --ignore --mixed "$arg")
        fi
        args="$args \"$arg\""
    done
    unset -v arg
    set -- $args
fi

exec "$JAVACMD" "-Dorg.gradle.appname=$APP_BASE_NAME" -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
