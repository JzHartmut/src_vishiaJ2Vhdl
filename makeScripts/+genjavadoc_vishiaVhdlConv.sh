#export DSTDIR=D:/vishia/Java/
export DSTDIR=$TMP/_Javadoc/
mkdir $DSTDIR

if test -d ../../srcJava_vishiaBase; then export vishiaBase="../../srcJava_vishiaBase"
else export vishiaBase="../../../../../../../Java/cmpnJava_vishiaBase/src/main/java/srcJava_vishiaBase"
fi

if test -d ../../srcJava_vishiaBase; then export vishiaBase="../../srcJava_vishiaBase"
else export vishiaBase="../../../../../../../Java/cmpnJava_vishiaBase/src/main/java/srcJava_vishiaBase"
fi

if ! test -d $DSTDIR; then export DSTDIR=../../; fi
echo %DSTDIR%
export DST=docuSrcJava_vishiaVhdlConv

export SRC="-subpackages org.vishia"
export SRCPATH="..;$vishiaBase"
export CLASSPATH="xxxxx"
export LINKPATH="-link ../../../../../../../Java/docuSrcJava_vishiaBase"



$vishiaBase/_make/-genjavadocbase.sh


