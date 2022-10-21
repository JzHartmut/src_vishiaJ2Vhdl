echo =========================================================================
echo execute $0
## Set the current dir 2 level before the script, it is the srcDir/makeScripts
cd $(dirname $0)/../..
echo currdir $PWD
export DSTNAME="vishiaVhdlConv"
echo " ... generates the $DSTNAME.jar from srcJava_$DSTNAME core sources"

#Do not change the version on repeated build, and check the checksum and content of jar.
#If it is equal, it is a reproduces build. The $VERSIONSTAMP is important 
#  because it determines the timestamp and hence the checksum in the jar file. 
export VERSIONSTAMP="2022-10-21"

## Determine a dedicated vishiaBase-yyyy-mm-dd.jar or deactivate it to use the current vishiaBase.jar:
export VERSION_VISHIABASE="XX2021-07-01"


## The VERSIONSTAMP can come form calling script, elsewhere it is set with the current date.
## This determines the names of the results, but not the content and not the MD5 check sum.
## See $TIMEinJAR_VISHIABASE in next block.
if test "$VERSIONSTAMP" = ""; then export VERSIONSTAMP=$(date -I); fi   ## writes current date

## Determines the timestamp of the files in the jar. The timestamp determines also
## the MD5 check code. 
## Do not change the version on repeated build, and check the checksum and content of jar.
## If it is equal, it is a reproduces build. The $VERSIONSTAMP is important 
##  because it determines the timestamp and hence the checksum in the jar file. 
## Using another timestamp on equal result files forces another MD5.
## Hence let this unchanged in comparison to a pre-version 
## if it is assumed that the sources are unchanged.
## Only then a comparison of MD5 is possible. 
## The comparison byte by byte inside the jar (zip) file is always possible.
## Use this timestamp for file in jars, influences the MD5 check:
export TIMEinJAR=""   ##get from $VERSIONSTAMP
##Note: The next is worse because it prevents reproducible results:
##export TIMEinJAR_VISHIABASE="$VERSIONSTAMP+00:00"
export SRCDIRNAME="vishiaJ2Vhdl"  ##must identical to the own location

## This directory contains some basic scripts. Should be exists
export MAKEBASEDIR="srcJava_vishiaBase/makeScripts"

#The SRCZIPFILE name will be written in MD5 file also for vishiaMiniSys.
# It should have anytime the stamp of the newest file, independing of the VERSIONSTAMP
export SRCZIPFILE="$DSTNAME-$VERSIONSTAMP-source.zip"

# Select the location and the proper vishiaBase
# for generation with a given timestamp of vishiaBase in the vishia file tree:
if test -f ../tools/vishiaBase.jar
then export JAR_vishiaBase="../tools/vishiaBase.jar"
# for generation side beside: 
elif test -f ../../jars/vishiaBase.jar
then export JAR_vishiaBase="../../jars/vishiaBase.jar"
else
  echo vishiaBase.jar not found, abort
  exit
fi
if test "$OS" = "Windows_NT"; then export sepPath=";"; else export sepPath=":"; fi
#The CLASSPATH is used for reference jars for compilation which should be present on running too.
export CLASSPATH="$JAR_vishiaBase"

#It is the tool for zip and jar used inside the core script
export JAR_zipjar=$JAR_vishiaBase

export MANIFEST=$SRCDIRNAME/makeScripts/$DSTNAME.manifest

##This selects the files to compile
export SRC_MAKE="$SRCDIRNAME/makeScripts" 
export SRC_ALL="$SRCDIRNAME/java"
export SRC_ALL2="vishiaFpga/java"
unset FILE1SRC    ##left empty to compile all sources

##This is the path to find sources for javac, maybe more comprehensive as SRC_ALL
unset SRCPATH       ##set it with SRC_ALL;SRC_ALL2

# Resourcefiles for files in the jar
export RESOURCEFILES="$SRCDIRNAME/java:**/*.zbnf $SRCDIRNAME/java:**/*.txt $SRCDIRNAME/java:**/*.xml $SRCDIRNAME/java:**/*.png"


#now run the common script:
export DEPLOYSCRIPT="$MAKEBASEDIR/-deployJar.sh"
echo DEPLOYSCRIPT=$DEPLOYSCRIPT

chmod 777 $MAKEBASEDIR/-makejar-coreScript.sh
chmod 777 $DEPLOYSCRIPT
$MAKEBASEDIR/-makejar-coreScript.sh

