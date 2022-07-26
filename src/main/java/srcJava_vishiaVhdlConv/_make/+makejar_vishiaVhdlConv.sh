echo =========================================================================
echo execute  $0
export DSTNAME="vishiaVhdlConv"
echo " ... generates the $DSTNAME.jar from srcJava_$DSTNAME core sources"

#Do not change the version on repeated build, and check the checksum and content of jar.
#If it is equal, it is a reproduces build. The $VERSIONSTAMP is important 
#  because it determines the timestamp and hence the checksum in the jar file. 
export VERSIONSTAMP="2022-07-26"
## Determine a dedicated vishiaBase-yyyy-mm-dd.jar or deactivate it to use the current vishiaBase.jar:
export VERSION_VISHIABASE="XX2021-07-01"


## The VERSIONSTAMP can come form calling script, elsewhere it is set with the current date.
## This determines the names of the results, but not the content and not the MD5 check sum.
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
export TIMEinJAR="$VERSIONSTAMP+00:00"

export MAKEBASEDIR="../../../../../../../Java/cmpnJava_vishiaBase/src/main/java/srcJava_vishiaBase/_make"
if ! test -d $MAKEBASEDIR; then
  export MAKEBASEDIR="../../srcJava_vishiaBase/_make"
fi

# It should have anytime the stamp of the newest file, independing of the VERSIONSTAMP
export SRCZIPFILE="$DSTNAME-$VERSIONSTAMP-source.zip"

# Select the location and the proper vishiaBase
# for generation with a given timestamp of vishiaBase in the vishia file tree:
if test -f ../../../../../../../Java/deploy/vishiaBase-$VERSION_VISHIABASE.jar
then export JAR_vishiaBase="../../../../../../../Java/deploy/vishiaBase-VERSION_VISHIABASE.jar"
#for generation in the vishia file tree:
elif test -f ../../../../../../../Java/tools/vishiaBase.jar
then export JAR_vishiaBase="../../../../../../../Java/tools/vishiaBase.jar"
# for generation side beside: 
elif test -f ../../jars/vishiaBase.jar
then export JAR_vishiaBase="../../jars/vishiaBase.jar"
else
  echo vishiaBase.jar not found, abort
  exit
fi
echo JAR_vishisBase=$JAR_vishiaBase

if test "$OS" = "Windows_NT"; then export sepPath=";"; else export sepPath=":"; fi
#The CLASSPATH is used for reference jars for compilation which should be present on running too.
##Note here libs is not really used only for enhancements
export CLASSPATH="$JAR_vishiaBase"

#It is also the tool for zip and jar used inside the core script
export JAR_zipjar=$JAR_vishiaBase

#determine the sources:
# Note: include sources of vishiaRun are part of the source.zip
export SRC_ALL=".."
export SRC_ALL2="../../vishiaFpga"
export SRCPATH="..;../../vishiaFpga"

# Resourcefiles for files in the jar
export RESOURCEFILES="..:**/*.zbnf ..:**/*.xml ..:**/*.png ..:**/*.txt"


# located from this workingdir as currdir for shell execution:
export MANIFEST=$DSTNAME.manifest

#$BUILD_TMP is the main build output directory. 
#possible to give $BUILD_TMP from outside. On argumentless call determine in tmp.
if test "$BUILD_TMP" = ""; then export BUILD_TMP="/tmp/BuildJava_$DSTNAME"; fi

#to store temporary class files:
export TMPJAVAC=$BUILD_TMP/javac

if ! test -d $BUILD_TMP/deploy; then mkdir --parent $BUILD_TMP/deploy; fi                                                                                                     


#now run the common script:
chmod 777 $MAKEBASEDIR/-makejar-coreScript.sh
chmod 777 $MAKEBASEDIR/-deployJar.sh
$MAKEBASEDIR/-makejar-coreScript.sh
$MAKEBASEDIR/-deployJar.sh


