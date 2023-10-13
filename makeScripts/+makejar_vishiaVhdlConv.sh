echo ====== start script ===============================================================
echo execute  $0
## Set the current dir 3 level before the script, it sees the src/srcDir/makeScripts:
cd $(dirname $0)/../../..
echo currdir $PWD

## Determines the name of some files marked with the date.
## If it not set, the current date will be used. This is usual proper, to prevent confusion.
export VERSIONSTAMP=""

## Determines the timestamp of the files in the jar. The timestamp determines the MD5 check code. 
## Do not change the timestamp on repeated build, and check the checksum and content of jar.
## If it is equal, it is a reproduces build. The $TIMEinJAR is important 
##  because it determines the timestamp and hence the checksum in the jar file. 
## Using another timestamp on equal result files forces another MD5.
## Hence let this unchanged in comparison to a pre-version 
## if it is assumed that the sources are unchanged.
## Only then a comparison of MD5 is possible. 
## The comparison byte by byte inside the jar (zip) file is always possible.
export TIMEinJAR="2023-10-12+00:00"

## This directory contains some basic scripts. Should be exists
export MAKEBASEDIR="src/srcJava_vishiaBase/makeScripts"

## Determine the name of some files and directories with the component's name:
export DSTNAME="vishiaVhdlConv"

# Select the location and the proper vishiaBase
# for generation with a given timestamp of vishiaBase in the vishia file tree:
if test -f tools/vishiaBase.jar
then export JAR_vishiaBase="tools/vishiaBase.jar"
# for generation side beside: 
elif test -f jars/vishiaBase.jar
then export JAR_vishiaBase="jars/vishiaBase.jar"
else
  echo vishiaBase.jar not found, abort
  exit
fi
if test "$OS" = "Windows_NT"; then export sepPath=";"; else export sepPath=":"; fi

## Determines the sources for this component to create a jar
SRCDIRNAME="src/vishiaJ2Vhdl"
export SRC_ALL="$SRCDIRNAME/java"            ## use all sources from here
export SRC_ALL2="src/vishiaFpga/java"        ## use all sources also from here
export SRCPATH="";                           ## search path for depending sources if FILE1SRC is given
export FILE1SRC=""                           ## use a specific source file (with depending ones)
export SRC_MAKE="$SRCDIRNAME/makeScripts"    ## add it to the source.zip 
export MANIFEST="$SRCDIRNAME/makeScripts/$DSTNAME.manifest"

# Determines search path for compiled sources (in jar) for this component. 
# do not left empty because it is used as argument for javac
export CLASSPATH="$JAR_vishiaBase"

# Determines resource files to store in the jar
export RESOURCEFILES="$SRC_ALL:**/*.zbnf $SRC_ALL:**/*.txt $SRC_ALL:**/*.xml $SRC_ALL:**/*.png"


#now run the common script:
chmod 777 $MAKEBASEDIR/-makejar-coreScript.sh
chmod 777 $DEPLOYSCRIPT
$MAKEBASEDIR/-makejar-coreScript.sh



