#shell script to generate jar file
#it can be run under Windows using MinGW: sh.exe - thisScript.sh
#MinGW is part of git, it should be known for Gcc compile too.

#$BUILD_TMP is the build output directory. 
#possible to give $BUILD_TMP from outside. On argumentless call determine in build.
#the build should be a temporary directory for build outputs, beside the src of the src-tree
#It can be removed too, then always create newly
echo PWD=$PWD
if test "$BUILD_TMP" = ""; then                                                      ## check whether a build exists:
  if test -d ../../build; then export BUILD_TMP="../../build"                        ## beside the component java tree
  elif test -d ../../../../../build; then export BUILD_TMP="../../../../../build"    ## beside the common src tree
  else
    export BUILD_TMP="/tmp/BuildJava/$DSTNAME"
    if ! test -d $BUILD_TMP; then mkdir -p $BUILD_TMP; fi
  fi
fi  
echo BUILD_TMP = $BUILD_TMP

# clean the build dir because maybe old faulty content: 
# note there was a problem detected on link folders (JUNCTION) in Windows file system, 
#      mkdir is related to the original directory, not to the linked folder using ../../../path
# therefore change temporary to the $BUILD_TMP to use mkdir
export PWDD="$PWD"
cd $BUILD_TMP
echo PWD=$PWD
if test -d javac_$DSTNAME; then rm -f -r -d javac_$DSTNAME; fi
mkdir -p javac_$DSTNAME/binjar   
mkdir javac_$DSTNAME/result
if ! test -d deploy; then mkdir deploy; fi;
cd $PWDD
echo PWD=$PWD
export TMPJAVAC="$BUILD_TMP/javac_$DSTNAME"

#determine out file names from VERSIONSTAMP
if test "$JARFILE" = ""; then export JARFILE="$BUILD_TMP/deploy/$DSTNAME-$VERSIONSTAMP.jar"; fi
if test "$MD5FILE" = ""; then export MD5FILE="$BUILD_TMP/deploy/$DSTNAME-$VERSIONSTAMP.jar.MD5.txt"; fi

if test "$TIMEinJAR" = ""; then export TIMEinJAR=$VERSIONSTAMP+00:00;  fi

##specific condition, use the yet compiled class files to zip:
if test "$JAR_vishiaBase" = "__vishiaBase_CLASSfiles__"; then export JAR_vishiaBase=$TMPJAVAC/binjar;
elif test "$JAR_vishiaBase" = ""; then
  if test -f ../../tools/vishiaBase.jar; then export JAR_vishiaBase="../../tools/vishiaBase.jar"
  elif test -f ../../jars/vishiaBase.jar; then export JAR_vishiaBase="../../jars/vishiaBase.jar"
  elif test -f ../../../../../tools/vishiaBase.jar; then export JAR_vishiaBase="../../../../../tools/vishiaBase.jar"
  elif test -f ../../../../../../../Java/tools/vishiaBase.jar; then export JAR_vishiaBase="../../../../../../../Java/tools/vishiaBase.jar"
  else echo ERROR vishiaBase.jar not able to found.
  fi
fi


echo
echo ====== javac ================================================
echo execute  $0
echo pwd=$(PWD)
echo  ... compile java and generate jar with binary-compatible content. 
echo DSTNAME = $DSTNAME  ## output file names
echo BUILD_TMP = $BUILD_TMP  ## root for all temporary outputs
echo VERSIONSTAMP = $VERSIONSTAMP  ## determine suffix of output file names
echo TIMEinJAR = $TIMEinJAR  ## determine timestamp in jar
echo SRC_ALL = $SRC_ALL      ## gather all *.java there
echo SRC_ALL2 = $SRC_ALL2  ## gather all *.java there - optional
echo FILE1SRC = $FILE1SRC  ## alternatively: argument files for javac
echo RESOURCEFILES = $RESOURCEFILES  ## additional files in jar
echo SRCPATH = $SRCPATH  - search path sources for javac
echo CLASSPATH = $CLASSPATH - search path jars for javac
echo JAR_vishiaBase = $JAR_vishiaBase  - jar file for jar/zip generation
echo TMPJAVAC =  $TMPJAVAC  - temporary files while compilation
echo JARFILE = $JARFILE  - generated jar    
echo MD5FILE = $MD5FILE  - generated MD5 text file
echo SRCZIPFILE = $SRCZIPFILE - generated sozrce.zip file


if test "$JAVAC_HOME" = ""; then
  export JAVAC_HOME="$($(dirname $0)/JAVAC_HOME.sh)"
fi  
echo JAVAC_HOME = $JAVAC_HOME
##regards an empty JAVAC_HOME, then javac should be able as command in the path:
if test "$JAVAC_HOME" = ""; then export JAVAC="javac"; else export JAVAC="$JAVAC_HOME/bin/javac"; fi
echo JAVAC = $JAVAC                                                                                       
echo Output to: $JARFILE
echo ===============================================================

##Automatic build a zip file if SRC_ALL and maybe additionally SRC_ALL2 is given.
##SRC_ALL refers to the java package path root directory,
##but the source.zip should contain the parent folder which is srcJava_xyz/org/... 
export SRCZIP=""
if ! test "$SRC_ALL" = ""; then
  echo source-set all files = $SRC_ALL
  find $SRC_ALL -name "*.java" > $TMPJAVAC/sources.txt
  export FILE1SRC=@$TMPJAVAC/sources.txt
  export SRCZIP=$SRC_ALL/..:**/*  ## with the srcJava_... dir
fi  
if ! test "$SRC_ALL2" = ""; then 
  echo source-set all files = $SRC_ALL2
  find $SRC_ALL2 -name "*.java" >> $TMPJAVAC/sources.txt
  export FILE1SRC=@$TMPJAVAC/sources.txt
  export SRCZIP="$SRCZIP $SRC_ALL2/..:**/*"                         
fi  
echo compile javac
echo pwd=$(pwd)
##echo ls /tmp
##ls /tmp
echo $JAVAC -encoding UTF-8 -d $TMPJAVAC/binjar -cp $CLASSPATH -sourcepath $SRCPATH $FILE1SRC 
###$JAVAC_HOME/bin/
$JAVAC -encoding UTF-8 -d $TMPJAVAC/binjar -cp $CLASSPATH -sourcepath $SRCPATH $FILE1SRC 
if test ! $? == 0; then
  echo ERROR javac --?????????????????????????????????????????--
  if test -f $JARFILE; then rm $JARFILE; fi    ##prevent usage of an older version here.  
  exit 5
fi  
echo build jar
##do not use: $JAVAC_HOME/bin/jar -n0cvfM $JARFILE -C $TMPJAVAC/binjar . > $TMPJAVAC/jar.txt
echo pwd=$(pwd)
echo java -cp $JAR_vishiaBase org.vishia.zip.Zip -o:$JARFILE -manifest:$MANIFEST -sort -time:$TIMEinJAR  $TMPJAVAC/binjar:**/*.class $RESOURCEFILES
java -cp $JAR_vishiaBase org.vishia.zip.Zip -o:$JARFILE -manifest:$MANIFEST -sort -time:$TIMEinJAR  $TMPJAVAC/binjar:**/*.class $RESOURCEFILES
if ! test "$MD5FILE" = ""; then echo output MD5 checksum
  md5sum -b $JARFILE > $MD5FILE
  echo "  srcFiles: $SRCZIPFILE" >> $MD5FILE
fi  

if test ! "$SRCZIPFILE" = ""; then  ##not produced if $SRC_ALL is empty instead $FILE1SRC is given from outside.
  pwd
  echo java -cp $JAR_vishiaBase org.vishia.zip.Zip -o:$BUILD_TMP/deploy/$SRCZIPFILE -sort $SRCZIP
  java -cp $JAR_vishiaBase org.vishia.zip.Zip -o:$BUILD_TMP/deploy/$SRCZIPFILE -sort $SRCZIP
  if test -f $BUILD_TMP/deploy/$SRCZIPFILE; then echo ok $BUILD_TMP/deploy/$SRCZIPFILE; else echo ERROR src.zip $BUILD_TMP/deploy/$SRCZIPFILE; fi
fi  

echo ===================================================================================
if test -f $JARFILE; then echo ok $JARFILE; else echo ERROR $JARFILE; fi
echo ===================================================================================


if test -f $MAKEBASEDIR/-deployJar.sh; then 
  $MAKEBASEDIR/-deployJar.sh 
fi


