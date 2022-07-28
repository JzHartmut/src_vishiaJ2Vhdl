@echo off
REM this file should exists in adapted form two levels to root, parallel to this component dir vishiaJ2Vhdl,
REM it is yourWorkingDir/src/mklink_SrcJava_vishiaFpga.bat
REM If it is started from the original given, then either the copy is done, or the adapted script is started.
echo called: %0
REM goto directory of the file
cd /d  %~d0%~p0
if exist ..\..\vishiaJ2Vhdl (
  REM the file is called from the original, in src/vishiaJ2Vhdl/script_UsedCompn:
  if exist ..\..\%~n0.bat (
    cd ..\..
    REM execute the given and maybe alread adapted file
    %~n0.bat
  ) else (
    REM copy this file only if not exists
    copy %0 ..\..
    echo The file %~n0%.bat is copied parallel to vishiaJ2Vhdl.
    echo You may adapt the paths, and then start this copy to create proper links.
    cd ..\..
    echo %CD%/%~n0.bat
    pause
    exit /b  
  )
)

echo create non existing links (JUNCTION) due to %CD%/%~n0.bat
if not exist vishiaJ2Vhdl (
  echo faulty location of %0
  pause
  exit /b
)

REM create a symbolic link to a directory where the src/vishiaFpga is originally stored, adapt this path if necessary.
cd ..
if not exist tools mklink /J tools ..\..\Java\tools
cd src

REM create a symbolic link to a directory where the src/vishiaFpga is originally stored, adapt this path if necessary.
if not exist java_vishiaBase mklink /J java_vishiaBase ..\..\..\Java\cmpnJava_vishiaBase\src\java_vishiaBase

REM The vishiaFpga should be stored here as original
if not exist vishiaFpga mklink /J vishiaFpga XXX-path-if-not-exist

dir
if not "%1" == "NOPAUSE" pause
