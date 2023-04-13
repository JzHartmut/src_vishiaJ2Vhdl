export DST=docuSrcJava_vishiaVhdlConv

if test -d ../../srcJava_vishiaBase; then export vishiaBase="../../srcJava_vishiaBase"
else export vishiaBase="../../../../../../../Java/cmpnJava_vishiaBase/src/main/java/srcJava_vishiaBase"
fi

if test -d ../../srcJava_vishiaBase; then export vishiaBase="../../srcJava_vishiaBase"
else export vishiaBase="../../../../../../../Java/cmpnJava_vishiaBase/src/main/java/srcJava_vishiaBase"
fi


export SRC="-subpackages org.vishia"
export SRCPATH="../java;$vishiaBase"
export CLASSPATH="xxxxx"
if test -d ../../../../../../../Java/docuSrcJava_vishiaBase
then export LINKPATH="-link ../../../../../../../Java/docuSrcJava_vishiaBase"
fi
if test -d ../../../docuSrcJava_vishiaBase
then export LINKPATH="-link ../../../docuSrcJava_vishiaBase"
fi


$vishiaBase/makeScripts/-genjavadocbase.sh


