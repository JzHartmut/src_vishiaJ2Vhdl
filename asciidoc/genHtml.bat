:loop
cls
::call C:\Programs\Asciidoc\genAsciidoc2Html.bat Vhdl/Java2Vhdl_StyleGuide.adoc ../../../../html/Vhdl
::call C:\Programs\Asciidoc\genAsciidoc2Html.bat Vhdl/Java2Vhdl_TestOutput.adoc ../../../../html/Vhdl
call C:\Programs\Asciidoc\genAsciidoc2Html.bat Vhdl/Java2Vhdl_ToolsAndExample.adoc ../../../../html/Vhdl
::call C:\Programs\Asciidoc\genAsciidoc2Html.bat Vhdl/Java2Vhdl_Approaches.adoc ../../../../html/Vhdl
::call C:\Programs\Asciidoc\genAsciidoc2Html.bat Vhdl/Java2Vhdl_TranslatorInternals.adoc ../../../../html/Vhdl
echo done
pause
goto :loop

