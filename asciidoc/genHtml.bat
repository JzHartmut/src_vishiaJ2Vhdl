:loop
pause
cls
echo on
set CP="../../../tools/vishiaBase.jar"
java -cp %CP% org.vishia.tools.PrepareAsciidoc --@%0:prep
::prep ##
::-wd:D:/vishia/Fpga/wrk_vishiaJ2Vhdl/src/vishiaJ2Vhdl/asciidoc/  ##working dir
::-in:Vhdl/Java2Vhdl_UserManual.adoc                  ## input file
::---in:Vhdl/Java2Vhdl_Approaches.adoc
::---in:Vhdl/Java2Vhdl_UserGuide.adoc
::---in:Vhdl/Java2Vhdl_ExampleBlinkingLed.adoc
::-o:Vhdl.gen/                        ## output directory (file same as input)
::-lmax:85                            ## max line length in pre blocks
::-rlink:3:https://vishia.org         ## substituation of links
::-rlink:2:https://vishia.org/Fpga
::-rlink:1:https://vishia.org/Fpga/html
::-rlink:0:https://vishia.org/Fpga/html/Vhdl

echo off
call C:\Programs\Asciidoc\genAsciidoc2Html.bat Vhdl.gen/Java2Vhdl_UserManual.adoc ../../../../html/Vhdl
call C:\Programs\Asciidoc\genAsciidoc2pdf.bat Vhdl.gen/Java2Vhdl_UserManual.adoc ../../../../pdf
::call C:\Programs\Asciidoc\genAsciidoc2Html.bat Vhdl/Java2Vhdl_StyleGuide.adoc ../../../../html/Vhdl
::call C:\Programs\Asciidoc\genAsciidoc2Html.bat Vhdl/Java2Vhdl_TestOutput.adoc ../../../../html/Vhdl
::call C:\Programs\Asciidoc\genAsciidoc2Html.bat Vhdl.gen/Java2Vhdl_ToolsAndExample.adoc ../../../../html/Vhdl
::call C:\Programs\Asciidoc\genAsciidoc2Html.bat Vhdl/Java2Vhdl_Approaches.adoc ../../../../html/Vhdl
::call C:\Programs\Asciidoc\genAsciidoc2Html.bat Vhdl/Java2Vhdl_TranslatorInternals.adoc ../../../../html/Vhdl
echo done
goto :loop

