package org.vishia.java2Vhdl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.script.ScriptException;

import org.vishia.fpga.Fpga;
import org.vishia.java2Vhdl.parseJava.JavaParser;
import org.vishia.java2Vhdl.parseJava.JavaSrc;
import org.vishia.util.Arguments;
import org.vishia.util.Debugutil;
import org.vishia.util.OutTextPreparer;
import org.vishia.util.StringFormatter;



/**A converter from Java classes in a special form for hardware simulation
 * to a VHDL file. 
 * @author hartmut Schorrig
 *
 */
public class Java2Vhdl {

  /**Version, history and license.
   * <ul>
   * <li>2022-10-20 adapt change AnnotationUse in class syntax with usage for linked VHDL modules. 
   * <li>2022-08-22 in {@link #gatherAllVariables()}: also in the top level process classes ( {@link Fpga#} annotation VHDL_PROCESS) 
   *   is possible now, gather the variables also in the top level. 
   * <li>2022-08-07 in {@link #associateActualWithTypeArgumentRefs(J2Vhdl_ModuleInstance, org.vishia.java2Vhdl.parseJava.JavaSrc.ActualArguments, Iterator)}
   *   Association improved, with associations of inner modules gotten from Ref.
   * <li>2022-08-04 in {@link #genVhdlCall(StringBuilder)}: Now also generates statements to prepare the inputs.
   * <li>2022-08-01 {@link #evaluateModuleClassCtor(J2Vhdl_ModuleType, org.vishia.java2Vhdl.parseJava.JavaSrc.ClassContent)}
   *   now called with the type, saves the init routines, and {@link #prepareModuleInstance(J2Vhdl_ModuleInstance)}
   *   uses also the init operation.  
   * <li>2022-07-28 {@link #genVhdlCall(StringBuilder)}
   * <li>2022-07-28 {@link #createModuleInstances()} and {@link #prepareModuleInstance(J2Vhdl_ModuleInstance)} as extra call
   *   dissolved from {@link #evaluateModuleTypes()}. This is because in the past only the top level has sub modules,
   *   and now first all sub modules should be gathered, and after all are given, should be prepared.
   *   Before, the order of creation of the module type and prepare of the instance was related, hence one after another in one operation,
   *   but this is more a special case.  
   * <li>2022-07-28 {@link #genUsedVhdl(Appendable)} now writes the COMPONENT PORT
   * <li>2022-07-28 {@link #evaluateModuleTypes()} renamed from evaluateModules, now checks annotation <code>(at)FpgaVHDL_MODULE</code>,
   *   then parses Input, Output inner classes in {@link #evaluateVhdlMdlTypes(J2Vhdl_ModuleType)}.
   * <li>2022-07-25 in {@link #prepareIfcOperationsInModuleType(J2Vhdl_ModuleType, J2Vhdl_ModuleType, String, org.vishia.java2Vhdl.parseJava.JavaSrc.ClassContent)}:
   *   now Annotation (at)Fpga.OnlySim regarded for operations which are only for simulation.  They does not create VHDL code.  
   * <li>2022-06-11 Hartmut Big bug with small cause found: 
   *   For the CONSTANT names, the identifier for the access variable,
   *   for exampl <code>((AT)Fpga.IfcAccess Bit_ifc <b>stubFalse</b></code>  was not used in the name.
   *   Hence the CONSTANT definitions were not distinguished, the first has wined and the VHDL function was very faulty.
   *   Detect on the test design for SpeA card, in comparison real behavior with simulation, 
   *   and checked the VHDL file, from which the real behavior may be forded.
   * <li>2022-04-26 {@link Args#dirJavaVhdlSrc} as List, more as one source directory possible.
   * <li>2022-02: Hartmut www.vishia.de creation
   * </ul>
   * <ul>
   * <li>new: new functionality, downward compatibility.
   * <li>fnChg: Change of functionality, no changing of formal syntax, it may be influencing the functions of user,
   *            but mostly in a positive kind. 
   * <li>chg: Change of functionality, it should be checked syntactically, re-compilation necessary.
   * <li>adap: No changing of own functionality, but adapted to a used changed module.
   * <li>corr: correction of a bug, it should be a good thing.
   * <li>bug123: correction of a tracked bug.
   * <li>nice: Only a nice correction, without changing of functionality, without changing of syntax.
   * <li>descr: Change of description of elements.
   * </ul> 
   */
  public static final String sVersion = "2022-08-22";

  
  
  
  /**If not null, then checks whether the line(s) in this file are translated by an expression. 
   * See search-hit ::: dbgStop ::: to set a breakpoint for specific positions of translation code.
   * 
   */
  public String dbgStopFile = "SpiData.java";
  
  public int dbgStopLine1 = 506, dbgStopLine2 = 512;
  

  
  
  
  
  
  
  
  /**This class evaluates and holds the arguments
   */
  public static class Args extends Arguments {

    /**The given main file in VHDL. It may be only an empty frame but with all inputs/outputs
     */
    public File fInVhdl;
    
    /**The generated main file in VHDL. Content is supplemented.
     */
    public File fOutVhdl;
    
    /**The file to write inner content. */
    public File fOutContentReport;
    
    /**Directory path for the translated parts of Java inputs.
     * On re-translation files here will be checked with time stamp against changes.
     */
    public File dirTmpVhdl; 
    
    
    public boolean bLogParsing, bParseResult, bJavaData;
    
    /**The directory where all Java sources for Vhdl where placed.
     * It may contain the package path too. */
    public List<File> dirJavaVhdlSrc = new LinkedList<File>();
    
    /**Java input files with or without package path appropriate to {@link #dirJavaVhdlSrc}
     * It contains the file "path/to/name.java" inside some source paths.
     */
    public List<String> fJavaVhdlSrc;  
    
    Arguments.SetArgument setInVhdl = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      Args.this.fInVhdl = new File(val);
      return true;
    }};
    
    Arguments.SetArgument setOutVhdl = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      Args.this.fOutVhdl = new File(val);
      return true;
    }};
    
    Arguments.SetArgument setOutContentReport = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      Args.this.fOutContentReport = new File(val);
      return true;
    }};
    
    Arguments.SetArgument setDirTmpVhdl = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      Args.this.dirTmpVhdl = new File(val);
      return true;
    }};
    
    Arguments.SetArgument setDirJavaVhdlSource = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      Args.this.dirJavaVhdlSrc.add(new File(val));
      return true;
    }};
    
    Arguments.SetArgument setJavaVhdlSrc = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      if(Args.this.fJavaVhdlSrc == null) { Args.this.fJavaVhdlSrc = new LinkedList<String>(); }
      final String path;
      if(val.endsWith(".java")) {                         // either given as pkg/path/to/Myclass.java
        path = val;
      } else {
        path = val.replace('.', '/') + ".java";           // or given as pkg.path.to.MyClass
      }
      Args.this.fJavaVhdlSrc.add(path);
      return true;
    }};
    
    
    Arguments.SetArgument setSrcComment = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      J2Vhdl_GenExpr.d.bAppendLineColumn = true;
      return true;
    }};
    
    
    Arguments.SetArgument setLogParsing = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      Args.this.bLogParsing = true;
      return true;
    }};
    
    
    Arguments.SetArgument setParseResult = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      Args.this.bParseResult = true;
      return true;
    }};
    
    
    Arguments.SetArgument setJavaData = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      Args.this.bJavaData = true;
      return true;
    }};
    
    
    Args(){
      super.aboutInfo = "Java2Vhdl made by HSchorrig, 2022-02-16 - " + Java2Vhdl.sVersion;
      super.helpInfo=" see www.vishia.org/Fpga/html/Vhdl/Java2Vhdl_ToolsAndExample.html";
      addArg(new Argument("-i", ":path/to/template.vhd  ...optional, if given, read this file to insert", this.setInVhdl));
      addArg(new Argument("-o", ":path/to/output.vhd", this.setOutVhdl));
      addArg(new Argument("-top", ":pkg.path.VhdlTopModule ... the top level java file (without .java, as class path) ", this.setJavaVhdlSrc));
      addArg(new Argument("-sdir", ":path/to/srcJava  ... able to use more as one", this.setDirJavaVhdlSource));
      addArg(new Argument("-sl", " ... optional, if given, remark src and line", this.setSrcComment));
      addArg(new Argument("-parseData", " ... optional, if given, writes the parser java data tree", this.setJavaData));
      addArg(new Argument("-pd", " ... optional, same as -parseData", this.setParseResult));
      addArg(new Argument("-parseResult", " ... optional, if given, writes the parser result", this.setParseResult));
      addArg(new Argument("-pr", " ... optional, same as -parseResult", this.setParseResult));
      addArg(new Argument("-parseLog", " ... optional only with -parseResult, writes an elaborately parser log file", this.setLogParsing));
      addArg(new Argument("-pl", " ... optional, same as -parseLog", this.setLogParsing));
      addArg(new Argument("-tmp", ":path/to/dirTmp for log and result", this.setDirTmpVhdl));
      addArg(new Argument("-rep", ":path/to/fileReport.txt   ... optional", this.setOutContentReport));
    }

    @Override
    public boolean testArgs(Appendable msg) throws IOException {
      boolean bOk = true;
      if(this.fOutVhdl == null) { msg.append("-o:out.vhd obligate\n"); bOk = false; }
//      if(this.fInVhdl == null) { msg.append("-i:out.vhd obligate\n"); bOk = false; }
      if(!bOk) {
        super.showHelp(msg);
      }
      return bOk;
    }
    
  }
  
  final Args args;
  
  
  
  

  public static void main(String[] cmdArgs) {
    Args args = new Args();
    try {
      if(cmdArgs.length ==0) {
        args.showHelp(System.out);
        System.exit(1);                // no arguments, help is shown.
      }
      if(  false == args.parseArgs(cmdArgs, System.err)
        || false == args.testArgs(System.err)
        ) { 
        System.exit(2);                // argument error
      }
    }
    catch(Exception exc) {
      System.err.println("Unexpected Exception: " + exc.getMessage());
      exc.printStackTrace();
    }
    smain(args);
    System.exit(1); 
  }
  
  
  
  public static void smain(Args args) {
    try {
      Java2Vhdl thiz = new Java2Vhdl(args);
      thiz.exec();
    }
    catch(Exception exc) {
      System.err.println("Unexpected Exception: " + exc.getMessage());
      exc.printStackTrace();
    }
  }
  

  
  
  
  
  
//  final JZtxtcmdScript vhdlTplScript;
//  
//  final JZtxtcmdScript.Subroutine subVhdlDefinitonPart;
//  
//  final JZtxtcmdScript.Subroutine subVhdlProcessPart;
//  
//  final JZtxtcmd jzTcmd;
  
  
  final JavaParser parser = new JavaParser();

  
  /**Composition to Gen statement class, it contains some more data. */
  final J2Vhdl_GenStmnt genStmnt = new J2Vhdl_GenStmnt();

  /**Composition to Gen expression class, it contains some more data. */
  final J2Vhdl_GenExpr genExpr = this.genStmnt.genExpr;
  
  final J2Vhdl_FpgaData fdata = J2Vhdl_GenExpr.d.fdata; //new J2Vhdl_FpgaData(); 
  
  
  OutTextPreparer vhdlHead, vhdlAfterPort, vhdlConst;
  
  OutTextPreparer vhdlCmpnDef, vhdlCmpnCall;

  
  /**Creates all working instances.
   * Here the JZtxtcmd is created also, no more necessary.
   * @param args
   * @throws ScriptException
   * @throws ClassNotFoundException
   */
  Java2Vhdl(Args args) throws ScriptException, ClassNotFoundException {
    this.args = args;
//    File fVhdlTpl = new File("d:/vishia/spe/FPGA/src/test/JavaFPGA/jzTc/conv1.jzt.cmd");
//    this.jzTcmd = new JZtxtcmd();
//    this.vhdlTplScript = this.jzTcmd.compile(fVhdlTpl, null);
//  
//    this.subVhdlDefinitonPart = this.vhdlTplScript.getSubroutine("vhdlDefinitionPart");
//    this.subVhdlProcessPart = this.vhdlTplScript.getSubroutine("vhdlProcessPart");
// 
//  
//    Class<?> clazz = Class.forName("java.lang.Object"); //org.vishia.spehw.SpiData");
//    this.vhdlConv = VhdlConv.d; //new VhdlConv(clazz, fdata); //org.vishia.speHw.SpiData"));
//    this.varVhdlConv = new DataAccess.Variable<Object>('O', "vhdlConv", this.vhdlConv, false);
    

  
  }



  /**Executes whole parsing and conversion with all given {@link #args}.
   * @throws Exception 
   */
  void exec() throws Exception {
    
    InputStream inTpl = Java2Vhdl.class.getResourceAsStream("VhdlTemplate.txt");  //pathInJar with slash: from root.
    Map<String, String> tplTexts = OutTextPreparer.readTemplate(inTpl, "===");
    inTpl.close();
    this.vhdlHead = new OutTextPreparer("vhdlHead", null, "fpgaName", tplTexts.get("vhdlHead"));
    this.vhdlAfterPort = new OutTextPreparer("vhdlAfterPort", null, "fpgaName", tplTexts.get("vhdlAfterPort"));
    this.vhdlConst = new OutTextPreparer("vhdlConst", null, "name, type, value", tplTexts.get("vhdlConst"));
    this.vhdlCmpnDef = new OutTextPreparer("vhdlCmpnDef", null, "name, vars", tplTexts.get("vhdlCmpnDef"));
    this.vhdlCmpnCall = new OutTextPreparer("vhdlCmpnCall", null, "name, typeVhdl, preAssignments, vars", tplTexts.get("vhdlCmpnCall"));
    parseAll();                                                              // parse top level and depending classes. 
    evaluateModuleTypes();
    createModuleInstances();
    prepareModuleInstances();
    evaluateCtor();
    gatherAllVariables();
    evaluateInterfacesInModules();
    
    //=======================================================================================================
    if(this.args.fOutContentReport !=null) {               // report parsed content of modules, variables
      try {
        Writer foutRep = new FileWriter(this.args.fOutContentReport);
        reportContentOfAll(foutRep);
        foutRep.close();
      }catch(Exception exc) {
        System.err.println("-rep:" + this.args.fOutContentReport.getAbsolutePath() + " faulty:" +  exc.getMessage());
      }
    }
    //=======================================================================================================
    
    BufferedReader rIn = this.args.fInVhdl == null ? null: new BufferedReader(new FileReader(this.args.fInVhdl));
    Writer wOut = new FileWriter(this.args.fOutVhdl);
    String line;
    if(rIn !=null) {
      while( (line = rIn.readLine()) !=null) {             // write content from given vhdl file till --java2Vhdl INSERT
        wOut.append(line).append("\n");
        if(line.contains("Java2Vhdl") && line.contains("INSERT")) {
          break;
        }
      }
      wOut.flush();
    }
    else {                                                 // generate head of VHDL from Java
      StringBuilder sbHead = new StringBuilder();
      genHead(sbHead);
      wOut.append(sbHead);
    }
    genUsedVhdl(wOut);                                     //COMPONENT ... PORT .... call of external VHDL parts 
    genRecords(wOut);
    //
    
    wOut.flush();
    //
    genRecordInstances(wOut);
    wOut.append("\n\n");
    //
    genConstants(wOut);
    wOut.append("\n\n");
    //
    // ================= furthermore content from tmpl.vhdl
    //
    if(rIn !=null) {
      while( (line = rIn.readLine()) !=null) {             // write content from given vhdl file till --java2Vhdl INSERT
        if(line.contains("Java2Vhdl") && line.contains("END")) {
          wOut.append(line).append("\n");
          break;
        }
      }
      while( (line = rIn.readLine()) !=null) {             // write content from given vhdl file till --java2Vhdl INSERT
        wOut.append(line).append("\n");
        if(line.contains("Java2Vhdl") && line.contains("INSERT")) {
          break;
        }
      }
      wOut.flush();
    }
    else {                                                 // generate head of VHDL from Java
      wOut.append("\nBEGIN\n");
    }
    
    StringBuilder out = new StringBuilder(2400);
    genVhdlCall(out);
    wOut.append(out);
    
    out = new StringBuilder(2400);
    genProcesses(out);
    wOut.append(out);
    
    out = new StringBuilder(2400);
    genAssignments(out);
    wOut.append(out);
    
    
    
    
    out = new StringBuilder(2400);                         // generate output assignments in update() operation.
    genOutput(out);
    wOut.append(out);
    
    
    
    wOut.flush();
    //
    if(rIn !=null) {
      while( (line = rIn.readLine()) !=null) {     // write content before the ARCHITECTURE line
        if(line.contains("Java2Vhdl") && line.contains("END")) {
          wOut.append(line).append("\n");
          break;
        }
      }
      while( (line = rIn.readLine()) !=null) {     // write content before the ARCHITECTURE line
        wOut.append(line).append("\n");
      }
      rIn.close();
    }
    else {                                                 // generate head of VHDL from Java
      wOut.append("\n\nEND BEHAVIORAL;\n");
    }
    wOut.close();
    System.out.println("success generated: " + this.args.fOutVhdl);
  }



  /**Parse primary all argument files and secondly all depending files in inner classes "Modules".
   * The first primary file (named firstly in argument list) is the toplevel. 
   * <pre>
   * public class Modules {
   *   public final SubModule nameInstance = new SubModule(references, for_aggregations));
   * </pre>  
   * <ul>
   * <li>For each detected sub module type in this Modules an entry in {@link J2Vhdl_FpgaData#idxModuleTypes} is created and saved.
   * <li>An always parsed class is not parsed twice, it is possible to have more module instances of the same type.
   * <li>For each detected sub module instances in this Modules an entry in {@link J2Vhdl_FpgaData#idxModules} is created and saved.
   * <li><code>As key: String nameInstance</code>: name of the sub module in Java, 
   *   it builds the first part for the name of the RECORD instance for this module in VHDL.
   *   The second part comes from the inner class instance for a PROCESS.
   *   <br>For example the RECORD instance name is <code>nameInstance_q</code>
   * <li>As entry: {@link J2Vhdl_ModuleInstance}. Its content is prepared in {@link #prepareModuleInstance(J2Vhdl_ModuleInstance, org.vishia.java2Vhdl.parseJava.JavaSrc.VariableInstance)}.
   *   description see their.
   * </ul> 
   * This operation does not evaluate the content of the java class, it searches only for all sub modules in the <code>Modules</code> class.
   * The evaluation of the content is then done with the following called {@link #evaluateModules()}.
   * This approach assures that all modules are formally known in a first pass. 
   * Its content may use the modules in another order, for that all modules should be known. That is the second pass of translation.  
   * {@link #parseSrc(List, String)} is called internally.  
   * @throws Exception 
   */
  public void parseAll() 
  throws Exception 
  { 
    List<String> javaSrcToProcess = new LinkedList<String>();
    javaSrcToProcess.addAll(this.args.fJavaVhdlSrc);       // add all classes from argument
    int nToplevelFiles = javaSrcToProcess.size();
    //boolean bTopLevel = true;
    while(javaSrcToProcess.size() >0) {                    // loop over all, also new added files.
      boolean bTopLevel = --nToplevelFiles >=0;            // the first added files are top level
      String pathSrcJava = javaSrcToProcess.remove(0);
      //======>>>
      JavaSrc parseResult = parseSrc(this.args.dirJavaVhdlSrc, pathSrcJava);
      if(parseResult !=null) {
        if(parseResult.getSize_classDefinition() >0) //...for
        for(JavaSrc.ClassDefinition pclass: parseResult.get_classDefinition()) {  //typical one class, maybe more package private classes
          String className = pclass.get_classident();      // should contain only one public class.
          boolean isVhdlMdl = pclass.getAnnotation("Fpga.VHDL_MODULE") !=null;
          final J2Vhdl_ModuleType moduleType;
          if(isVhdlMdl) {
            moduleType= new J2Vhdl_ModuleVhdlType(className, parseResult, pclass, bTopLevel);
          } else {
            moduleType= new J2Vhdl_ModuleType(className, parseResult, pclass, bTopLevel);
          }
          this.fdata.idxModuleTypes.put(className, moduleType); // Store in idxModuleTypes with the simple className
          if(this.fdata.topInstance ==null) { //bTopLevel) {                                      // build an module instance also from the top level file as Module
            this.fdata.topInstance = new J2Vhdl_ModuleInstance(className, null, moduleType, false, null, null);
          }
          if(!isVhdlMdl) { //sAnnot == null || !sAnnot.contains("Fpga.VHDL_MODULE")) {
            JavaSrc.ClassContent zClassC = pclass.get_classContent();
            //
            if(zClassC.getSize_classDefinition() >0) //...for // iterate over all inner classes to search a Modules class
            for(JavaSrc.ClassDefinition iclass: zClassC.get_classDefinition()) {
              //----------------------------------------------------------------------------------
              String iClassName = iclass.get_classident();       // search a module class inside the given class.
              if(iClassName.equals("Modules")) {                 // The Modules class contains all used sub modules.
                JavaSrc.ClassContent ziClassC = iclass.get_classContent();
                //
                // -------------------------------------------   // All elements in class Modules:
                for(JavaSrc.VariableInstance mVar: ziClassC.get_variableDefinition()) {
                  //--------------------------------------------------------------
                  //String nameSubModule = mVar.get_variableName();  //This builds the left part of instance name for all RECORD instances in VHDL
                  final JavaSrc.Type type = mVar.get_type();
                  final String sType = type.get_name();          // search the appropriate parsed source.java result
                  J2Vhdl_ModuleType typeSubModule = this.fdata.idxModuleTypes.get(sType);
                  //System.out.println("  Module: " + nameSubModule);
                  if(typeSubModule == null) {                // parse only not already parsed source.java.
                    String pathSrcRef = parseResult.findTypeInImport(sType);
                    if(pathSrcRef == null) {                   // suggest that the module is in the same package as the top level module:
                      int posDirSrc = pathSrcJava.lastIndexOf('/') +1;
                      pathSrcRef = pathSrcJava.substring(0, posDirSrc) + sType; 
                    }
                    javaSrcToProcess.add(pathSrcRef + ".java");
                  }                                             
        } } } } } 
      }
//      bTopLevel = false;
    } //while(javaSrcToProcess.size() >0);
  }

  
  
  /**Parse one source as part of the VHDL contribution. 
   * <ul> 
   * <li>It is either a top level class given as argument class for {@link Java2Vhdl#main(String[])}
   * <li>or it is a used sub module which is named in the <code>class Modules</code> inner class of this parsed class.
   * </ul> 
   *  
   * This operation is called recusively for all the Submodules- tree of sources
   * @param dirSrc directory where the name.java file is found. 
   * @param nameSrc name.java with extension
   * @return
   * @throws Exception 
   */
  private JavaSrc parseSrc(List<File> dirSrcs, String pathSrcJava) 
  throws Exception 
  { System.out.print("\nparse: " + pathSrcJava);
    File fParse = null;
    for(File dirSrc : dirSrcs) {
      fParse = new File(dirSrc, pathSrcJava);
      if(fParse.exists()) {
        break;
      }
    }
    if(fParse == null) {
      System.err.println("not found in -sdir:faultyPath " + pathSrcJava );
      return null;
    } else {
      File tmpDir = null;
      if(this.args.bParseResult) { tmpDir = this.args.dirTmpVhdl; } // if null, no parse result.
      JavaSrc parseResult = this.parser.parseJava(fParse, tmpDir, this.args.bJavaData, this.args.bParseResult, this.args.bLogParsing);
      System.gc();
      return parseResult;
    }
  }
  
  
  
  /**Evaluates a parsed module (java file) to detect sub module defintions<ul>
   * <li>detect composite sub modules in the class Modules (instantiated as ref or m)
   * <li>detect input / output records
   * <li>detect composite sub modules as implementation class of an interface
   * </ul>
   * All this modules are able to implement interface types. 
   * This interface operations are stored in {@link J2Vhdl_ModuleType#idxIfcExpr} 
   * but only later in {@link #prepareIfcOperationsInModuleType(J2Vhdl_ModuleType, org.vishia.parseJava.JavaSrc.ClassDefinition)}.
   * @throws Exception
   */
  private void evaluateModuleTypes ( ) throws Exception {
    //add new detected inner types after evaluate the container idxModuleTypes:
    List<J2Vhdl_ModuleType> newInnerTypes = new LinkedList<J2Vhdl_ModuleType>();
    for(Map.Entry<String, J2Vhdl_ModuleType> e : this.fdata.idxModuleTypes.entrySet()) {
      J2Vhdl_ModuleType mdlt = e.getValue();
      String sClassName = mdlt.nameType;
      if(sClassName.equals("RxSpe")) {
        Debugutil.stop();
      }
      if(mdlt instanceof J2Vhdl_ModuleVhdlType) {
        evaluateVhdlMdlTypes((J2Vhdl_ModuleVhdlType)mdlt);
      }
      else {
        JavaSrc.ClassContent mdlClassC = mdlt.moduleClass.get_classContent();
        if(mdlClassC.getSize_classDefinition()>0) //...for
        for(JavaSrc.ClassDefinition iclass: mdlClassC.get_classDefinition()) {
          //----------------------------------------------------------------------------------
          String iClassName = iclass.get_classident();       // search a module class inside the given class.
          if(iClassName.equals("Modules")) {                 // The Modules class contains all used sub modules.
            JavaSrc.ClassContent iClassC = iclass.get_classContent();
            //
            // -------------------------------------------   // All elements in class Modules:
            for(JavaSrc.VariableInstance mVar: iClassC.get_variableDefinition()) { //are the composite existing modules
              //--------------------------------------------------------------
              String nameSubModule = mVar.get_variableName();  //This builds the left part of instance name for all RECORD instances in VHDL
              final JavaSrc.Type type = mVar.get_type();
              final String sType = type.get_name();          // search the appropriate parsed source.java result
              System.out.println("  Module: " + nameSubModule + " : " + sType);
              J2Vhdl_ModuleType typeSubModule = this.fdata.idxModuleTypes.get(sType);
              if(typeSubModule !=null) {
                if(mdlt.idxSubModulesVar == null) { 
                  mdlt.idxSubModulesVar = new TreeMap<String, JavaSrc.VariableInstance>();
                }
                mdlt.idxSubModulesVar.put(nameSubModule, mVar);  // register the module instance in the type as used composite sub module
                
              } else {
                J2Vhdl_GenExpr.vhdlError("evaluteModules() - J2Vhdl_ModuleType not found :" + sType + " in " + iClassName, mVar);
              }
              
            }
            evaluateModuleClassCtor(mdlt, iClassC);
          }
          else if( /*mdlt.isTopLevel() &&*/ ( iClassName.equals("Input")
                 || iClassName.equals("Output") )) {          // In/Output signals of the whole FPGA or a VHDL sub module.
            String sIclassName = Character.toLowerCase(iClassName.charAt(0)) + iClassName.substring(1); 
            String name = sClassName + "." + sIclassName;
            String nameType = sClassName + "_" + iClassName;  // J2Vhdl_ModuleType ToplevelType_Input
            J2Vhdl_ModuleType inoutType = new J2Vhdl_ModuleType(nameType, null, iclass, false);
            newInnerTypes.add(inoutType);    //instead: this.fdata.idxModuleTypes.put(name, inoutType); //concurrentmodificationException
            prepareIfcOperationsInModuleType(inoutType, inoutType, null, iclass.get_classContent());
            J2Vhdl_ModuleInstance inoutModule = new J2Vhdl_ModuleInstance(name, null, inoutType, true, null, null);  // J2Vhdl_ModuleInstance ToplevelType_input
            this.fdata.idxModules.put(name, inoutModule);
            searchForIfcAccess(iclass.get_classContent(), inoutType);
          }
          else if(iClassName.equals("Ref")) {
            
          }
          else {                                             // all other classes are Process classes, gather there variables
  //          this.vhdlConv.mapVariables(nameModule, iclass);
          }
        }
        searchForIfcAccess(mdlClassC, mdlt);
      }
    } //for this.fdata.idxModuleTypes
    for(J2Vhdl_ModuleType newInnerType : newInnerTypes) {  // an inner type does not need evaluated again.
      this.fdata.idxModuleTypes.put(newInnerType.nameType, newInnerType);
    }
  }

  
  
  /**Evaluates the ctor of a Module inner class. It searches for mdl.init(...)
   * @param mdlt the type where the inner class "Module" is part of. 
   * @param iclassC from the inner class "Module"
   */
  private void evaluateModuleClassCtor ( J2Vhdl_ModuleType mdlt, JavaSrc.ClassContent iclassC) {
    Iterable<JavaSrc.ConstructorDefinition> ctors = iclassC.get_constructorDefinition();
    if(ctors !=null) {
      for(JavaSrc.ConstructorDefinition ctor: ctors) {
        
        Iterable<JavaSrc.Statement> stmnts = ctor.get_statement();
        if(stmnts !=null) for(JavaSrc.Statement stmnt : stmnts) {
          JavaSrc.Expression operExpr = stmnt.get_Expression();
          if(operExpr !=null) {
            JavaSrc.SimpleValue operVal = operExpr.get_ExprPart().iterator().next().get_value();
            JavaSrc.SimpleMethodCall oper;
            if(operVal !=null && (oper = operVal.get_simpleMethodCall()) !=null) {
              String nameOper = oper.get_methodName(); 
              if(nameOper.equals("init") || nameOper.startsWith("init_")) {     // the init(ref,...) call in the ctor of Modules
                JavaSrc.Reference refSubModule = operVal.get_reference();
                String nameSubmodule = refSubModule.getSimpleRefVariable();
                if(nameSubmodule == null) { J2Vhdl_GenExpr.vhdlError("style guide init", oper); }
                else {
                  if(mdlt.idxSubModulesInit == null) { 
                    mdlt.idxSubModulesInit = new TreeMap<String, JavaSrc.SimpleMethodCall>();
                  }
                  mdlt.idxSubModulesInit.put(nameSubmodule, oper);  // register the module instance in the type as used composite sub module
                }
              }
            }
          }
        }
      }
    }
  }



  /**The module instances are built from {@link J2Vhdl_ModuleType#idxSubModules}
   * firstly from the top module, and then also from all SubModules in modules. 
   * 
   */
  private void createModuleInstances ( ) {
    J2Vhdl_ModuleInstance mdl = this.fdata.topInstance;
    this.fdata.idxModules.put(mdl.nameInstance, mdl); // register the module globally as existing module instance in the whole VHDL file (it's a RECORD instance)
    createModuleInstancesRecursively(mdl, null, 0);
  }
  
  
  private void createModuleInstancesRecursively ( J2Vhdl_ModuleInstance mdlParent, String nameMdl0, int recursion ) {
    J2Vhdl_ModuleType mdlt0 = mdlParent.type;
    if(recursion >3) {
      J2Vhdl_GenExpr.vhdlError("too many sub modules nested", mdlt0.moduleClass);
    }
    else if(mdlt0.idxSubModulesVar !=null ){
      for(Map.Entry<String, JavaSrc.VariableInstance> e : mdlt0.idxSubModulesVar.entrySet()) {
        final String innerNameSubmdl = e.getKey();
        JavaSrc.VariableInstance mVar = e.getValue();
        final JavaSrc.Type type = mVar.get_type();
        final String sType = type.get_name();          // search the appropriate parsed source.java result
        J2Vhdl_ModuleType typeSubmdl = this.fdata.idxModuleTypes.get(sType);
        final String nameSubmdl = nameMdl0 == null ? innerNameSubmdl : nameMdl0 + "_" + innerNameSubmdl;
        JavaSrc.SimpleMethodCall operInit = mdlt0.idxSubModulesInit == null ? null : mdlt0.idxSubModulesInit.get(innerNameSubmdl); //may existing or not
        J2Vhdl_ModuleInstance subModule = new J2Vhdl_ModuleInstance(nameSubmdl, mdlParent, typeSubmdl, false, mVar, operInit); //typeSubModule, false);
        if(mdlParent.idxSubModules == null) { mdlParent.idxSubModules = new TreeMap<String, J2Vhdl_ModuleInstance>(); }
        mdlParent.idxSubModules.put(innerNameSubmdl, subModule);
        this.fdata.idxModules.put(subModule.nameInstance, subModule); // register the module globally as existing module instance in the whole VHDL file (it's a RECORD instance)
        createModuleInstancesRecursively(subModule, subModule.nameInstance, recursion +1);
      }
    }
  }
  
  
  
  private void prepareModuleInstances ( ) {
    for(Map.Entry<String, J2Vhdl_ModuleInstance> e : this.fdata.idxModules.entrySet()) {
      J2Vhdl_ModuleInstance mdl = e.getValue();
      if(mdl.nameInstance.equals("data"))
        Debugutil.stop();
      if(mdl.mVarInit !=null) {       //not for the top level, only for modules which are created with = new Ctor(....)
        prepareModuleInstance(mdl);   // it uses the variable which creates the module. 
      }
    }
  }

  
  
  private void evaluateCtor ( ) {
    for(Map.Entry<String, J2Vhdl_ModuleInstance> e : this.fdata.idxModules.entrySet()) {
      J2Vhdl_ModuleInstance mdl = e.getValue();
    }
  }
  
  
  
  /**A module class with the (at) {@link Fpga.VHDL_MODULE}. Only the interfaces are evaluated. 
   * 
   */
  private void evaluateVhdlMdlTypes ( J2Vhdl_ModuleVhdlType mdlt ) {
    JavaSrc.ClassContent mdlClassC = mdlt.moduleClass.get_classContent();
    if(mdlClassC.getSize_classDefinition()>0) //...for
    for(JavaSrc.ClassDefinition iclass: mdlClassC.get_classDefinition()) {
      //----------------------------------------------------------------------------------
      String iClassName = iclass.get_classident();       // search a module class inside the given class.
      boolean bInput;
      if( /*mdlt.isTopLevel() &&*/ ( ( bInput = iClassName.equals("Input"))
          || iClassName.equals("Output") )) {          // In/Output signals of the whole FPGA or a VHDL sub module.
        final List<J2Vhdl_Variable> vars = bInput ? mdlt.createInputs() : mdlt.createOutputs();
        final J2Vhdl_Variable.Location location = bInput? J2Vhdl_Variable.Location.input : J2Vhdl_Variable.Location.output;
        JavaSrc.ClassContent iclassC = iclass.get_classContent();
        for(JavaSrc.VariableInstance mVar: iclassC.get_variableDefinition()) { //are the composite existing modules
          //--------------------------------------------------------------
          String name = mVar.get_variableName();
          J2Vhdl_Variable var = mdlt.idxIOVars.get(name);
          if(var !=null ) { //always existing, then it is IO
            var.location = J2Vhdl_Variable.Location.inout;
          } else {
            var = J2Vhdl_GenExpr.createVariable(mVar, location, null, "", null, mdlt.idxIOVars, null);
            mdlt.io.add(var);
          }
          vars.add(var);     //put in the adequate list
    } } }
  }
  
  
  
  
  private void searchForIfcAccess ( JavaSrc.ClassContent mdlClassC, J2Vhdl_ModuleType mdlt) throws Exception {
    if(mdlClassC.getSize_variableDefinition()>0) //... for
      for(JavaSrc.VariableInstance pVar: mdlClassC.get_variableDefinition()) {
        JavaSrc.ModifierVariable modif = pVar.get_ModifierVariable();
        if(modif !=null && modif.getSize_Annotation() >0) //...for
        for(String annot: modif.get_Annotation()) {
          if(annot.equals("Fpga.IfcAccess")) {             // a variable which contains operations for interface access.
            String sAccess = pVar.get_variableName();      // access for idxIfcAccess
            JavaSrc.ExprPart zInstance = pVar.get_Expression().get_ExprPart().iterator().next();
            JavaSrc.ClassContent zClassC = zInstance.get_value().get_newObject().get_impliciteImplementationClass();
            prepareIfcOperationsInModuleType(mdlt, mdlt, sAccess, zClassC);
            Debugutil.stop();
          }
        }
      }
  }
  
  
  private void evaluateInterfacesInModules ( ) throws Exception {
    for(Map.Entry<String, J2Vhdl_ModuleType> e : this.fdata.idxModuleTypes.entrySet()) {
      J2Vhdl_ModuleType mdlt = e.getValue();
      String sClassName = mdlt.nameType;
      if(sClassName.equals("RxSpe"))
        Debugutil.stop();
//      if(mdlt.moduleClass.getSize_classDefinition()>0) //...for
//      for(JavaSrc.ClassDefinition iclass: mdlt.moduleClass.get_classDefinition()) {
//        
//      }
      prepareIfcOperationsInModuleType(mdlt, mdlt, null, mdlt.moduleClass.get_classContent());
    }
  }
  



  
  
  
  /**Search and evaluate the ctor of the <code>Modules</code> inner class.
   * It may contain some <code>this.module.init(ref,...)</code> routines for module aggregations.
   * @param iclass
   */
  private void XXXevaluateModulesCtor ( J2Vhdl_ModuleInstance mdl) {
    JavaSrc.ClassContent iclassC = mdl.type.moduleClass.get_classContent();
    Iterable<JavaSrc.ConstructorDefinition> ctors = iclassC.get_constructorDefinition();
    if(ctors !=null) {
      for(JavaSrc.ConstructorDefinition ctor: ctors) {
        
        Iterable<JavaSrc.Statement> stmnts = ctor.get_statement();
        if(stmnts !=null) for(JavaSrc.Statement stmnt : stmnts) {
          JavaSrc.Expression operExpr = stmnt.get_Expression();
          if(operExpr !=null) {
            JavaSrc.SimpleValue operVal = operExpr.get_ExprPart().iterator().next().get_value();
            JavaSrc.SimpleMethodCall oper;
            if(operVal !=null && (oper = operVal.get_simpleMethodCall()) !=null) {
              String nameOper = oper.get_methodName(); 
              if(nameOper.equals("init") || nameOper.startsWith("init_")) {     // the init(ref,...) call in the ctor of Modules
                JavaSrc.ActualArguments actArgs = oper.get_actualArguments();
                int zArgs = actArgs.getSize_Expression();
                JavaSrc.Reference refSubModule = operVal.get_reference();
                String nameSubmodule = refSubModule.getSimpleRefVariable();
                if(nameSubmodule == null) { J2Vhdl_GenExpr.vhdlError("style guide init", oper); }
                else {
                  J2Vhdl_ModuleInstance subModule = mdl.idxSubModules.get(nameSubmodule);
                  JavaSrc.ClassDefinition subModuleClass = subModule.type.moduleClass;
                  JavaSrc.ClassContent subModuleClassC = subModuleClass.get_classContent();
                  for(JavaSrc.MethodDefinition operInitType : subModuleClassC.get_methodDefinition()) {
                    if(operInitType.get_name().equals(nameOper)) {  //the same init(...) or init_xy(...) in the sub modules class
                      if(operInitType.getSize_argument() == zArgs) { //distinguish between init operations because of number of arguments ...
                        Debugutil.stop();
                        associateActualWithTypeArgumentRefs(subModule, actArgs, operInitType.get_argument().iterator());
                      }
                    }
                  }
                }
                Debugutil.stop();
              }
            }
          }
        }
      }
    }
  }
  
  
  
  
  
  /**Prepares a module instance with the necessary aggregations. 
   * The module is defined in the <pre>
   * public class Modules {
   *   public final Module nameInstance = new Module(reference, ref2));
   *   public final OtherModule name = new OtherModule();
   *   //...
   *   Modules ( Ref ref, Moduleclass thism) {
   *     name.init(ref1, references)
   * </pre>
   * In Java the references, which builds the aggregations of the module, can be given on construction
   * or with the init routine. Both is equal, init() is necessary if references are crossed (not a pure tree).
   * <br>
   * In VHDL the references are other registered modules, instances of RECORDs, with its name. 
   * <br>
   * It uses {@link J2Vhdl_ModuleInstance#mVarInit} for the initial parameters on new Module(...),
   * which is stored on creation of the module. This variable is set to null after access, to garbage it.
   * It is the immediate parse result of the ctor. 
   * <br>
   * See called {@link #associateActualWithTypeArgumentRefs(J2Vhdl_ModuleInstance, org.vishia.java2Vhdl.parseJava.JavaSrc.ActualArguments, Iterator)}
   * for more details. 
   * 
   * @param module The simple created module only from the ctor {@link Module#Module(String, JavaSrc)}
   */
  private void prepareModuleInstance ( J2Vhdl_ModuleInstance module) {

    JavaSrc.Expression newExpr = module.mVarInit.get_Expression();    //This is the init value of the module, should be new Module(...)
    module.mVarInit = null;                                // mVarInit no more necessary, garbage it.
    JavaSrc.ExprPart newPart = newExpr.get_ExprPart().iterator().next();  //the only one part of expression is new Module(...)
    JavaSrc.NewObject newObj = newPart.get_value().get_newObject();       //the constructor of new
    JavaSrc.ActualArguments actArgs = newObj.get_actualArguments();       //the actual args are the instance names of the used submodules.
    if(actArgs !=null) {
      int zArgs = actArgs.getSize_Expression();            // Number of args, >=1 to search the proper ctor of the referenced class
      
      Iterator<JavaSrc.Argument> formalArgs = null;        // The arguments of the matching ctor of the module class to get names in the module class
      JavaSrc.ClassDefinition moduleClass = module.type.moduleClass; 
      JavaSrc.ClassContent moduleClassC = moduleClass.get_classContent();
      for(JavaSrc.ConstructorDefinition moduleCtor : moduleClassC.get_constructorDefinition()) {
        if(moduleCtor.getSize_argument() == zArgs) {       // the ctor which has the same number of arguments should match, do not check type of arguments.
          formalArgs = moduleCtor.get_argument().iterator();
          break;
        }
      }
      if(formalArgs == null) { 
        J2Vhdl_GenExpr.vhdlError("constructor in submodule not found", newObj); 
      }
      else {
        if(module.nameInstance.equals("data"))
          Debugutil.stop();
        associateActualWithTypeArgumentRefs(module, actArgs, formalArgs);
      }
    }
    if(module.operInit !=null) {
      JavaSrc.SimpleMethodCall operInit = module.operInit;
      module.operInit = null;   //garbage it.
      String nameOper = operInit.get_methodName();
      actArgs = operInit.get_actualArguments();
      int zArgs = actArgs.getSize_Expression();
      JavaSrc.ClassDefinition subModuleClass = module.type.moduleClass;
      JavaSrc.ClassContent subModuleClassC = subModuleClass.get_classContent();
      for(JavaSrc.MethodDefinition operInitType : subModuleClassC.get_methodDefinition()) {
        if(operInitType.get_name().equals(nameOper)) {  //the same init(...) or init_xy(...) in the sub modules class
          if(operInitType.getSize_argument() == zArgs) { //distinguish between init operations because of number of arguments ...
            associateActualWithTypeArgumentRefs(module, actArgs, operInitType.get_argument().iterator());
          }
        }
      }
    }
  }
  
  
  
  /**Associates this module to other modules. 
   * It fills {@link J2Vhdl_ModuleInstance#idxAggregatedModules} with the found module aggregations. 
   *     <ul>
   *     <li>key <code>formalName</code> from argument "formalArgs": name of the formal argument in the constructor of the Submodule
   *       or in the init() operation. 
   *       This is the internal name used in Java code of this module, not related to VHDL.
   *       The same <code>formalName</code> should be used as reference in the inner class <code>Ref</code>
   *       in this class: <pre>
   *       private static final class Ref {
   *         final Type formalName;
   *       </pre>
   *       Hence the used reference in the Java code to an access data form another module is:<br>
   *       <code>ref.formalName.accessOp()</code>
   *     <li>value the found module with the expression given with the argument "actArgs".
   *    </ul> 
   * <br>
   * "actArgs" can contain:
   * <ul>
   *   <li>"this.otherModule": Reference to a given module even in this Modules class. Usual for the top level.
   *     It uses module: {@link J2Vhdl_ModuleInstance#mdlParent} and their {@link J2Vhdl_ModuleInstance#idxSubModules}.
   *     For the top level it contains the same modules as {@link J2Vhdl_FpgaData#idxModules},
   *     but for sub modules the parent should be accessed (its content of the "class Modules").  
   *   <li>"Moduleclass.this" Reference to the parent module. Because the "class Module" may not be static, 
   *     this is possible in Java. You need write this kind of access (with dedicated this) in Java to detect it for the J2Vhdl translation. 
   *   <li>"thism": Same as "Moduleclass.this", reference to the parent module, which is this class file. 
   *     "thism" can be given as ctor argument name.
   *   <li>"ref.referredModule" Reference to a Module which is referred by the parent module. For sub modules.
   *     It uses module: {@link J2Vhdl_ModuleInstance#mdlParent} and their {@link J2Vhdl_ModuleInstance#idxAggregatedModules}. 
   *   <li>Any reference can contain also "....ifcAccess": A interface access defined in the referenced module.
   *     This is stored as {@link J2Vhdl_ModuleInstance.InnerAccess#sAccess} and used as key for
   *     {@link J2Vhdl_ModuleType#idxIfcExpr} .
   * </ul>
   * @param module for this module
   * @param actArgs arguments either from {@link J2Vhdl_ModuleInstance#mVarInit} for new Module(args) or from init(args)
   * @param formalArgs name of the formal arguments from the module's ctor.
   */
  private void associateActualWithTypeArgumentRefs ( J2Vhdl_ModuleInstance module, JavaSrc.ActualArguments actArgs, Iterator<JavaSrc.Argument> formalArgs ) {
    for(JavaSrc.Expression aggrArgExpr: actArgs.get_Expression() ) {  //the expression for the new Module(value, ...
      boolean dbgStop = false;
      if(this.dbgStopFile !=null) { 
        int[] lineColumn = new int[2];
        String file = aggrArgExpr.getSrcInfo(lineColumn);  // TxSpe BlinkingLedCt ClockDivider BlinkingLed_Fpga
        if(file.contains(this.dbgStopFile) && lineColumn[0] >= this.dbgStopLine1 && lineColumn[0] <= this.dbgStopLine2) {
          Debugutil.stop();
          dbgStop = true;
      } }
//      if(module.nameInstance.equals("ct_ct_clkDiv")) //"txSpe_crcGen"))
//        Debugutil.stop();
      JavaSrc.ExprPart aggrArgExpr1 = aggrArgExpr.get_ExprPart().iterator().next();  //The only one part of the expression
      JavaSrc.SimpleValue aggrVal = aggrArgExpr1.get_value();
      //StringBuilder sbAggrRef = new StringBuilder();
      String sAggrRef = null;                                // build aAggrName to search the aggregated module maybe with a reference,
      JavaSrc.Reference aggrRef = aggrVal.get_reference(); // usual this is without aggregation if calling arguments are used.
      while(aggrRef !=null) {
        JavaSrc.SimpleVariable aggrRefVar = aggrRef.get_referenceAssociation();
        if(aggrRefVar !=null) {
          String sAggrRef2 = aggrRefVar.get_variableName();// The variable name of the last ref is used only. All other is Java internally
          if(sAggrRef == null) { sAggrRef = sAggrRef2; }   // this.mdl => "mdl", FpgaTop.this => "FpgaTop"
          else { sAggrRef = sAggrRef + "." + sAggrRef2; }  // FpgaTop.this.input => "FpgaTop.input" used as name of referenced module. 
        }
        aggrRef = aggrRef.get_reference();     // next ref in chain ref.refNext
      }
      String sAggrVarName = aggrVal.get_simpleVariable().get_variableName();
      String sAggrName;
      final String sInnerName;
      final J2Vhdl_ModuleInstance aggrModule;
      if(sAggrRef == null) {
         sAggrRef = sAggrVarName;        //maybe also "thism" or local module
         sAggrVarName = null;
        //        sAggrName = sAggrVarName;                          // module inside Modules class
//        aggrModule = this.fdata.idxModules.get(sAggrName);
//        sInnerName = null;
      } 
      else if(sAggrRef.equals(module.mdlParent.type.nameType)) {  //EnvirClass.this for the reference:
        sAggrRef = "thism";
      }
      if(sAggrVarName !=null && sAggrVarName.equals("input") ) { // It can be only the OwnClass.this.input
        sAggrName = sAggrRef + "." + sAggrVarName;         // OwnClass.input is the name of the referenced module.
        aggrModule = this.fdata.idxModules.get(sAggrName);
        sInnerName = null;                                 // Note: OwnClass is usual the top level class, it has Input and Output as inner class
      } 
      else if(sAggrRef.equals("ref")) {
        sAggrName = "ref." + sAggrVarName;                              
        J2Vhdl_ModuleInstance.InnerAccess mdlAccess = module.mdlParent.idxAggregatedModules.get(sAggrVarName);
        aggrModule = mdlAccess.mdl;
        sInnerName = mdlAccess.sAccess;
      } 
      else if(sAggrRef.equals("thism")) {
        sAggrName = sAggrRef;                              
        aggrModule = module.mdlParent;
        sInnerName = sAggrVarName;                         // onwClass.this.ifcAgent or this.module.ifcAgent, then use it.
      } 
      else {
        sAggrName = sAggrRef;                              // either OwnClass.this or this.module.ifcModule, then it is OwnClass or module.
        aggrModule = module.mdlParent.idxSubModules.get(sAggrName);
        //aggrModule = this.fdata.idxModules.get(sAggrName);
        if(sAggrVarName == null || sAggrVarName.equals("this")) {
          sInnerName = null;                               // ownClass.this: ignore this is non relevant variable.
        } else {
          sInnerName = sAggrVarName;                       // onwClass.this.ifcAgent or this.module.ifcAgent, then use it.
        }
      }
//      if(sAggrVarName.equals("this") && sAggrRef !=null) {      // it is a reference to the enclosing class Type.this
//        sAggrName = sAggrRef.substring(0, sAggrRef.length()-1);  //The own module is stored with the Type name.
//      } else {
//        sAggrName = sAggrRef + sAggrVarName;
//      }
      if(aggrModule ==null) {
        Debugutil.stop();
      }
      //String sAggrRecordInstance = sAggrRef + sAggrName;
      JavaSrc.Argument ctorArg = formalArgs.next();
      String sNameFormalArg = ctorArg.get_variableName();   //formalName: name of the formal argument, it is exactly the used reference in the module.
      //VhdlConv.AggregatedModule aggrModule = new VhdlConv.AggregatedModule();
      //aggrModule.name = sAggrRecordInstance;
      if(aggrModule == null) {
        J2Vhdl_GenExpr.vhdlError("Error Aggregation module not found: " + module.nameInstance + "." + sNameFormalArg + "<-- " + sAggrName + ": ???moduleNotFound", actArgs);
      } else {
        System.out.println("    Aggregation: " + module.nameInstance + "." + sNameFormalArg + "<--" + aggrModule.nameInstance);
      }
      if(sInnerName !=null && sInnerName.equals("this"))
        Debugutil.stop();
      module.idxAggregatedModules.put(sNameFormalArg, new J2Vhdl_ModuleInstance.InnerAccess(aggrModule, sInnerName));
    }

  }

  
  
  
  
  
  
  private void prepareIfcOperationsInModuleType(J2Vhdl_ModuleType moduleType, J2Vhdl_ModuleType mdlTypeIdx, String sAccess, JavaSrc.ClassContent zclassC) throws Exception {
    Iterable<JavaSrc.MethodDefinition> iter = zclassC.get_methodDefinition();
    if(iter !=null) for(JavaSrc.MethodDefinition oper: iter ) {
      String nameOper = oper.get_name();
      if(! nameOper.equals("time")) {                      // ignore operation time(), it is not for VHDL output.
        if(nameOper.equals("ct"))
          Debugutil.stop();
        JavaSrc.ModifierMethod modif = oper.get_ModifierMethod();
        String sAnnot = modif == null ? null: modif.get_Annotation();
        String name = oper.get_name();
        String a_Override = modif == null ? null : modif.get_A_Override();
        if(a_Override ==null) {
          a_Override = oper.get_A_Override();  //TODO yet problem in syntax, do not quest @Override outside of the modifier.
          if(a_Override !=null)
            Debugutil.stop();
        }
        //String sAnnotation = modif == null ? null :modif.get_Annotation();       // only search for interface implementig operations
        boolean bOnlySim = false;
        if( a_Override !=null /*&& sAnnotation.startsWith("@Override") */  
            && !name.equals("step")                          // but not from the FpgaModule_ifc
            && !name.equals("update") 
          || sAnnot !=null 
            && ( sAnnot.equals("Fpga.GetterVhdl")
              || (bOnlySim = sAnnot.equals("Fpga.OnlySim"))
          )    ) {
          String sIfcOpName = (sAccess == null ? "" : sAccess + ".") + name;
          if(bOnlySim) {
            mdlTypeIdx.idxIfcExpr.put(sIfcOpName, new J2Vhdl_ModuleType.IfcConstExpr(null, null));   //empty
          } else {
            for(JavaSrc.Statement stmnt: oper.get_methodbody().get_statement()) {
              if(stmnt.get_returnStmnt() !=null) {             // return expr found
                J2Vhdl_ConstDef constDef = null;
                JavaSrc.Expression expr = stmnt.get_Expression(); 
                if(expr.getSize_ExprPart() ==1) {
                  JavaSrc.ExprPart exprPart = expr.get_ExprPart().iterator().next(); //first part
                  JavaSrc.SimpleValue val = exprPart.get_value();
                  if(val !=null) {
                    JavaSrc.ConstNumber constVal = val.get_constNumber();
                    if(constVal !=null) {                      // a constant returned from interface
                      String nameVhdl = moduleType.nameType  + (sAccess == null ? "" : "_" + sAccess) + "_" + name;
                      constDef = createConst(nameVhdl, nameVhdl, expr);
                      expr = null;  //no more necessary.
                } } }
                mdlTypeIdx.idxIfcExpr.put(sIfcOpName, new J2Vhdl_ModuleType.IfcConstExpr(expr, constDef));
              }
            }
          }
        }
      }
    }
  }
  
  
  
  
  /**This operation looks for all variable in all inner VHDL Process classes
   * and calls {@link J2Vhdl_GenExpr#mapVariables(String, String, org.vishia.parseJava.JavaSrc.ClassDefinition)} 
   * for each module which fills the global visible 
   * {@link J2Vhdl_GenExpr#idxVars} with all variables from all PROCESS classes
   * also for cross accesses. <br>
   * As result the name for VHDL and the search name for Java, and the type is stored.
   * @throws Exception 
   * 
   * 
   */
  void gatherAllVariables ( ) throws Exception {
    for(Map.Entry<String, J2Vhdl_ModuleType> esrc:  this.fdata.idxModuleTypes.entrySet()) {
      J2Vhdl_ModuleType src = esrc.getValue();
      if(src.isTopLevel()) {                                 // All toplevel variable
        gatherAllVariablesOfSubClasses(src.moduleClass, null, false);
      }
    }
    
    
    for(Map.Entry<String, J2Vhdl_ModuleInstance> esrc: this.fdata.idxModules.entrySet()) {// all sources
      //JavaSrc javaSrc = esrc.getValue().type.javaSrc;
      String nameModule = esrc.getKey();                   // the instance name of the module, not the type
      J2Vhdl_ModuleInstance module = esrc.getValue();
      J2Vhdl_ModuleType type = module.type;
      if(type.isTopLevel()) {
        final String nameProcess = nameModule;
        this.genExpr.setInnerClass(nameProcess, nameModule);              // records from all inner classes, same name as type
        gatherAllVariablesOfSubClasses(type.moduleClass, nameModule, false);
      } else if(module.bInOutModule) {
        final String nameProcess = nameModule;
        this.genExpr.setInnerClass(nameProcess, nameModule);              // records from all inner classes, same name as type
        this.genExpr.mapInOutVariables(type.moduleClass, module);

      } else {
        gatherAllVariablesOfSubClasses(type.moduleClass, nameModule, false);
      }
    }
  }
  
  
  /**Inner function called recursively for one Java sources all inner PROCESS classes.
   * @param type The parse result for one Java file. A module.
   * @param nameModule The instance name of a module in a Modules class, not the type name. Builds the RECORD and PROCESS name.
   * @param isToplevel
   * @throws Exception 
   */
  private void gatherAllVariablesOfSubClasses ( JavaSrc.ClassDefinition theclass, String nameModuleArg, boolean isToplevel) throws Exception {
      String nameTheClass = theclass.get_classident();
      final String nameModule = nameModuleArg !=null ? nameModuleArg: nameTheClass;  //for top level classes, not a module
      gatherEnumConstValues(theclass, nameTheClass + "_" + nameTheClass, nameModule);
      JavaSrc.ClassContent theClassC = theclass.get_classContent();
      Iterable<JavaSrc.ClassDefinition> iclasses = theClassC.get_classDefinition(); 
      if(iclasses !=null) for(JavaSrc.ClassDefinition iclass : iclasses) { // get inner class of public module class  
        String nameiClass = iclass.get_classident();
        JavaSrc.AnnotationUse annotLinkVhdl = iclass.getAnnotation("Fpga.LINK_VHDL_MODULE");
        JavaSrc.AnnotationUse annotProcess = iclass.getAnnotation("Fpga.VHDL_PROCESS");
        if( annotLinkVhdl !=null || annotProcess !=null 
         || nameiClass.equals("In") || nameiClass.equals("Out")) {
          final String nameProcess = nameModule + "_" + nameiClass;
          this.genExpr.setInnerClass(nameProcess, nameModule);              // records from all inner classes, same name as type
          this.genExpr.mapVariables(nameModule, nameTheClass, iclass);
    } }
  }
  

  
  
  /**This searches for enum definition in an inner class, defines its values for const, especially usable for states.
   * @param iclass
   * @param nameType
   * @param nameModule
   * @throws Exception
   */
  void gatherEnumConstValues ( JavaSrc.ClassDefinition iclass, String nameType, String nameModule) throws Exception {
    JavaSrc.ClassContent iClassC = iclass.get_classContent();
    String nameClass = iclass.get_classident();
    if(iClassC.getSize_enumDefinition()>0) { 
      for( JavaSrc.EnumDefinition enumd: iClassC.get_enumDefinition()) {
        String identType = enumd.get_enumTypeIdent();
        String nameEnumType = nameClass + "_" + identType;
        this.fdata.idxEnumDef.put(nameEnumType, nameEnumType);
        for(JavaSrc.EnumElement enume: enumd.get_enumElement()) {
          String ident = enume.get_enumIdent();
          String nameVhdl = nameType + "_" + identType + "_" + ident;
          String javaPath = nameModule + "." + identType + "." + ident;
          JavaSrc.ActualArguments args = enume.get_actualArguments();
          JavaSrc.Expression exprVal = null;
          JavaSrc.Expression exprBitnr = null;
          if(args !=null) {
            Iterator<JavaSrc.Expression> iterArgs = args.get_Expression().iterator();
            exprVal = iterArgs.next();  //use the first argument.
            if(iterArgs.hasNext()) {
              exprBitnr = iterArgs.next();  //use the first argument.
            }
          }
          if(exprVal !=null) {
            createConst(javaPath, nameVhdl, exprVal);
          }
          if(exprBitnr !=null) {
            createStateBit(nameEnumType + "_" + ident, exprBitnr);
          }
        }
    } }
  }
  
  
  
  /**Creates a {@link J2Vhdl_ConstDef} and stores in {@link J2Vhdl_GenExpr#idxConstDef} from a given expression
   * @param javaPath
   * @param nameVhdl
   * @param valueterm expression contains only the const value, no more.
   * @throws Exception 
   */
  J2Vhdl_ConstDef createConst (String javaPath, String nameVhdl, JavaSrc.Expression expr) throws Exception {
    VhdlExprTerm valueterm = this.genExpr.genExprOnePart(expr, null, "");
    String valueVhdl = valueterm.b.toString();             // one const value only, already converted to Vhdl format.
    VhdlExprTerm.ExprType type = valueterm.exprType_;
    VhdlExprTerm.ExprTypeEnum etype = valueterm.exprType_.etype;
    switch(etype) {
    case bitStdConst: type.etype = VhdlExprTerm.ExprTypeEnum.bittype; break; 
    case bitStdVconst: type.etype = VhdlExprTerm.ExprTypeEnum.bitVtype; break;
    default: //do nothing, type correct.  
    }  
    int nrBits = type.nrofElements;
    //String valueVhdl = null;
//    char cFormat = valueVhdl.length()>1 ? valueVhdl.charAt(0) : '\0';
//    switch(cFormat) {
//    case 'x': case 'X':                           // given as hex in form "0x12"
//      nrBits = (valueVhdl.length()-3)*4;
//      //valueVhdl = "x\"" + value.substring(2) + "\"";
//      type.etype = VhdlExprTerm.ExprTypeEnum.bitVtype;
//      break;
//    case '\"':
//      nrBits = (valueVhdl.length()-2);
//      //valueVhdl = "\"" + value.substring(2) + "\"";
//      type.etype = VhdlExprTerm.ExprTypeEnum.bitVtype;
//      break;
//    default:
//      nrBits = 32;
//      //valueVhdl = value;
//      type.etype = VhdlExprTerm.ExprTypeEnum.inttype;
//    }
//    type.nrofElements = nrBits;
    J2Vhdl_ConstDef constDef = new J2Vhdl_ConstDef(new J2Vhdl_Variable(nameVhdl, J2Vhdl_Variable.Location.record, type, nrBits, javaPath, nameVhdl), valueVhdl);
    this.fdata.idxConstDef.put(javaPath, constDef);
    return constDef;
  }
  
  
  
  
  void createStateBit ( String name, JavaSrc.Expression expr) throws Exception {
    VhdlExprTerm valueterm = this.genExpr.genExprOnePart(expr, null, "");
    String valueVhdl = valueterm.b.toString();             // one const value only, already converted to Vhdl format.
//    VhdlExprTerm.ExprType type = valueterm.exprType_;
    VhdlExprTerm.ExprTypeEnum etype = valueterm.exprType_.etype;
    if(etype == VhdlExprTerm.ExprTypeEnum.numConst && valueVhdl.charAt(0)!='-') {
      this.fdata.idxEnumBitDef.put(name, valueVhdl);
    }
  }
  
  
  /**Generate all head information in the VHDL file.
   * @param wOut
   * @throws IOException
   */
  void genHead(Appendable wOut) throws IOException {
    String fpgaName = this.args.fOutVhdl.getName(); //fJavaVhdlSrc.get(0);
    int posName = 0; //fpgaName.lastIndexOf('/')+1;
    int posExt = fpgaName.lastIndexOf('.');
    fpgaName = fpgaName.substring(posName, posExt);
    
    OutTextPreparer.DataTextPreparer args = this.vhdlHead.createArgumentDataObj();
    args.setArgument("fpgaName", fpgaName);
    this.vhdlHead.exec(wOut, args);
    String sep = "";
    for(Map.Entry<String, J2Vhdl_Variable> evar: this.fdata.idxVars.entrySet()) {
      String varName = evar.getKey();
      String portKind = null;
      String sVhdlName = null;
      
      if(varName.contains(".input.")) {                    // Module.input.PORTNAME
        posName = varName.lastIndexOf('.')+1;
        sVhdlName = varName.substring(posName);            // name after Module.input.PORTNAME
        String soutName = varName.substring(0, posName-7) + ".output." + sVhdlName;
        if(this.fdata.idxVars.get(soutName) !=null) {   // If same exists as output, then it is an INOUT
          portKind = "INOUT ";
        } else {
          portKind = "IN ";
        }
      }
      if(varName.contains(".output.")) {                  // output.PORTNAME
        posName = varName.lastIndexOf('.')+1;
        sVhdlName = varName.substring(posName);                  // name after input.PORTNAME
        String sInputName = varName.substring(0, posName-8) + ".input." + sVhdlName;
        if(this.fdata.idxVars.get(sInputName) !=null) {
          portKind = null;    //already written
        } else {
          portKind = "OUT ";
        }
      }
      if(portKind !=null) {
        J2Vhdl_Variable var = evar.getValue();
        String sVhdlType = var.getVhdlType();
        wOut.append(sep).append("  ").append(sVhdlName).append(" : ").append(portKind).append(sVhdlType);
        sep = ";\n";   //for the next line, not used on end
      }
    }
    args = this.vhdlAfterPort.createArgumentDataObj();
    args.setArgument("fpgaName", fpgaName);
    this.vhdlAfterPort.exec(wOut, args);
    
  }
  
  
  void genUsedVhdl(Appendable wOut) throws IOException {
    for(Map.Entry<String,J2Vhdl_ModuleType> esrc: this.fdata.idxModuleTypes.entrySet()) {                         // all sources
      J2Vhdl_ModuleType mdlt = esrc.getValue();
      if(mdlt instanceof J2Vhdl_ModuleVhdlType) {
        J2Vhdl_ModuleVhdlType mdlv = (J2Vhdl_ModuleVhdlType)mdlt;
        String sVhdlModule = getVhdlModuleType(mdlv);
        OutTextPreparer.DataTextPreparer args = this.vhdlCmpnDef.createArgumentDataObj();
        args.setArgument("name", sVhdlModule);
        args.setArgument("vars", mdlv.io);
        this.vhdlCmpnDef.exec(wOut, args);
      }
    }
  }
  

  /**Generate all RECORD type definitions in VHDL for all found inner classes which are processes.
   * They are all inner classes which are not named as In, Out, Ref or Modules
   * @param wOut
   * @throws IOException
   */
  void genRecords(Appendable wOut) throws IOException {
    for(Map.Entry<String,J2Vhdl_ModuleType> esrc: this.fdata.idxModuleTypes.entrySet()) {                         // all sources
      J2Vhdl_ModuleType result = esrc.getValue();
      //JavaSrc javaSrc = result.javaSrc;
      //for(JavaSrc.ClassDefinition theclass : javaSrc.get_classDefinition()) {   // get the only one public class of module
        JavaSrc.ClassDefinition theclass = result.moduleClass;
        String nameModule = theclass.get_classident();
        JavaSrc.ClassContent theClassC = theclass.get_classContent();
        if(theClassC.getSize_classDefinition() >0)  //...for
        for(JavaSrc.ClassDefinition iclass : theClassC.get_classDefinition()) { // get inner class of public module class  
          final String nameiClass = iclass.get_classident();
          JavaSrc.ClassContent iClassC = iclass.get_classContent();
          JavaSrc.AnnotationUse annotLinkVhdl = iclass.getAnnotation("Fpga.LINK_VHDL_MODULE");
          JavaSrc.AnnotationUse annotProcess = iclass.getAnnotation("Fpga.VHDL_PROCESS");
          if( (annotLinkVhdl !=null || annotProcess !=null) 
            && iClassC.getSize_variableDefinition() >0
            || ( !result.isTopLevel() && (nameiClass.equals("In") || nameiClass.equals("Out")) ) ) {
            final String nameRecord = nameModule + "_" + nameiClass;
            this.genExpr.setInnerClass(nameRecord, nameModule);
            wOut.append("\nTYPE ").append(nameRecord).append("_REC IS RECORD");
            for(JavaSrc.VariableInstance var:iClassC.get_variableDefinition()) {
              String varName = var.get_variableName();
              if( !varName.equals("_time_") && !varName.startsWith("m_")) {
                String sVhdlType = this.genExpr.assembleType(var, nameRecord);
                if(sVhdlType !=null) {
                  wOut.append("\n  ").append(varName).append(" : ").append(sVhdlType).append(";");
                }
              }
            }
            wOut.append("\nEND RECORD ").append(nameRecord).append("_REC;\n");
          }
        }
      
    }
  }
  
  
  
  /**Generate all instance definitions in VHDL for all found inner classes which are processes.
   * This inner classes are designated with 
   * @param wOut
   * @throws IOException
   */
  void genRecordInstances(Appendable wOut) throws IOException{
    wOut.append("\n-- == Definition of record instances for the processes:");
    wOut.append("\n-- VHDL-Syntax:          SIGNAL <signalName> : <typeName>");
    wOut.append("\n--SIGNAL {<&nameModule>_}<&nameProcessClass> : <&nameClass>_<&nameProcessClass>_REC");
    wOut.append("\n--   ... <&nameModule> is defined in  class Modules{...}  in top level Java file, name of the module instance.");
    wOut.append("\n--   ... <&nameModule> for Process classes in toplevel: name of the top level class and Java file (start with upper case).");
    wOut.append("\n--   ... more <&nameModule> can be defined in  class Modules{...}  in the module Java file, nested modules.");
    wOut.append("\n--   ... <&nameProcessClass> is the name of the inner class in the Java module, same as instance name of the process");
    wOut.append("\n--   ... <&nameClass> is the type name of the modul's class");
    boolean isTopLevel = false;  //only modules stored which are not top level.
    for(Map.Entry<String, J2Vhdl_ModuleInstance> esrc: this.fdata.idxModules.entrySet()) {                         // all sources
      J2Vhdl_ModuleInstance moduleInstance = esrc.getValue();
      String nameModule = esrc.getKey();
      //for(JavaSrc.ClassDefinition theclass : moduleInstance.type.javaSrc.get_classDefinition()) { // get the only one public class of module
      JavaSrc.ClassDefinition theclass = moduleInstance.type.moduleClass;  
      String nameClass = theclass.get_classident();
      JavaSrc.ClassContent theClassC = theclass.get_classContent();
      Iterable<JavaSrc.ClassDefinition> iclasses = theClassC.get_classDefinition(); 
      if(iclasses !=null) { 
        for(JavaSrc.ClassDefinition iclass : iclasses) { // get inner class of public module class  
          String nameiClass = iclass.get_classident();
          JavaSrc.ClassContent iClassC = iclass.get_classContent();
          JavaSrc.AnnotationUse annotLinkVhdl = iclass.getAnnotation("Fpga.LINK_VHDL_MODULE");
          JavaSrc.AnnotationUse annotProcess = iclass.getAnnotation("Fpga.VHDL_PROCESS");
          if( (annotLinkVhdl !=null || annotProcess !=null) 
           && iClassC.getSize_variableDefinition() >0   //meta data preparation for linked VHDL modules
           || ( !isTopLevel && (nameiClass.equals("In") || nameiClass.equals("Out")) ) // In and Out are only records for non-top level, for value-dataflow
            ) {
            final String nameProcess = nameModule + "_" + nameiClass;
            final String nameRecord = nameClass + "_" + nameiClass + "_REC";
            wOut.append("\nSIGNAL ").append(nameProcess).append(" : ").append(nameRecord).append(";");
          }
      } }
    }
  }


  
  /**Generate all instance definitions in VHDL for all found inner classes which are processes.
   * This inner classes are designated with 
   * @param wOut
   * @throws IOException
   */
  void genConstants(Appendable wOut) throws IOException{
    for(Map.Entry<String, J2Vhdl_ConstDef> edef: this.fdata.idxConstDef.entrySet()) {                         // all sources
      J2Vhdl_ConstDef def = edef.getValue();
      OutTextPreparer.DataTextPreparer args = this.vhdlConst.createArgumentDataObj();
      args.setArgument("name", def.var.sElemVhdl);
      String type = def.var.getVhdlType();
      args.setArgument("type", type);
      args.setArgument("value", def.value);
      this.vhdlConst.exec(wOut, args);
    }
  }


  /**Gen all processes for VHDL from all parsed sources.
   * @param wOut to write
   * @throws Exception 
   */
  void genProcesses(StringBuilder wOut) throws Exception {
    for(Map.Entry<String, J2Vhdl_ModuleInstance> esrc: this.fdata.idxModules.entrySet()) {          // all sources, instances 
      J2Vhdl_ModuleInstance moduleInstance = esrc.getValue();
      String sModule = esrc.getKey();
      JavaSrc.ClassDefinition theclass = moduleInstance.type.moduleClass;     // get the only one public class of module
      JavaSrc.ClassContent theClassC = theclass.get_classContent();
      Iterable<JavaSrc.ClassDefinition> iclasses = theClassC.get_classDefinition(); 
        if(iclasses !=null) for(JavaSrc.ClassDefinition iclass : iclasses) { // get inner class of public module class  
          String nameiClass = iclass.get_classident();
          if(iclass.getAnnotation("Fpga.VHDL_PROCESS") !=null) {
          //
            String namePrc = sModule  + "_" + nameiClass;                      // search that ctor of the class
            String nameInnerClassVariable = Character.toLowerCase(nameiClass.charAt(0))+ nameiClass.substring(1);
            JavaSrc.ConstructorDefinition ctor = this.genStmnt.getCtorProcess(iclass, nameInnerClassVariable); // which is designated with @Fpga.CTOR_PROCESS
            if(ctor !=null) {
              String ctorName = ctor.get_constructor();
              if(ctorName.equals("Qrx"))
                Debugutil.stop();
              if(namePrc.equals("clr_Q"))
                Debugutil.stop();
              wOut.append("\n\n\n").append(namePrc).append("_PRC: PROCESS ( clk )");
              //String nameIclass = sModule  + "." + Character.toLowerCase(nameiClass.charAt(0)) + nameiClass.substring(1);
              String nameIclass = Character.toLowerCase(nameiClass.charAt(0)) + nameiClass.substring(1);
              this.genExpr.setInnerClass(nameIclass, sModule);
              this.genExpr.createProcessVar(wOut, ctor);                      // gather and output definition of local process variables
              wOut.append("\nBEGIN IF(clk'event AND clK='1') THEN\n");
              for(JavaSrc.Statement stmnt: ctor.get_statement()) {             // all first level statements in the ctor
                //CharSequence txt = this.vhdlConv.genStatement(stmnt, 1);
                this.genStmnt.genStmnt(wOut, stmnt, moduleInstance, nameInnerClassVariable, 1, true);
              } 
              //
              this.genExpr.cleanProcessVar();
              wOut.append("\nEND IF; END PROCESS;\n");
              //
            }
        } }
      }
    
  }


  
  
  void genVhdlCall(StringBuilder wOut) throws Exception {
    for(Map.Entry<String, J2Vhdl_ModuleInstance> esrc: this.fdata.idxModules.entrySet()) {          // all sources, instances 
      J2Vhdl_ModuleInstance moduleInstance = esrc.getValue();
      String sModule = esrc.getKey();
      JavaSrc.ClassDefinition theclass = moduleInstance.type.moduleClass;     // get the only one public class of module
      JavaSrc.ClassContent theClassC = theclass.get_classContent();
      Iterable<JavaSrc.ClassDefinition> iclasses = theClassC.get_classDefinition(); 
        if(iclasses !=null) for(JavaSrc.ClassDefinition iclass : iclasses) { // get inner class of public module class  
          JavaSrc.AnnotationUse annot = iclass.getAnnotation("Fpga.LINK_VHDL_MODULE");
          String nameiClass = iclass.get_classident();
          if(annot !=null) {            // it is an inner class for a VHDL RECORD and PROCESS
            //
            String sVhdlModule = null;
            JavaSrc.ConstructorDefinition ctor = getCtorVhdlCall(iclass); // search that ctor of the class
            if(ctor !=null) {                              // which is designated with @Fpga.CTOR_PROCESS
              String nameVhdlMdl = sModule  + "_" + nameiClass + "_" + "vhdlMdl";                      
              String nameInnerClassVariable = Character.toLowerCase(nameiClass.charAt(0))+ nameiClass.substring(1);
              for(JavaSrc.Argument arg : ctor.get_argument()) {
                if(arg.get_variableName().equals("vhdlMdl")) {
                  JavaSrc.Type argType = arg.get_type();
                  String vhdlMdlType = argType.get_name();
                  J2Vhdl_ModuleType mdlType = this.fdata.idxModuleTypes.get(vhdlMdlType);
                  if(mdlType == null) { J2Vhdl_GenExpr.vhdlError("module not found: " + vhdlMdlType, arg); }
                  else {
                    sVhdlModule = getVhdlModuleType(mdlType);        // get VHDL entity and file name form annotation on the class
                  }
                  break;
                }
              }
              if(sVhdlModule == null) {
                J2Vhdl_GenExpr.vhdlError("LINK_VHDL_MODULE ctor must have an argument 'vhdlMdl'", ctor);
              }
              else {
                String ctorName = ctor.get_constructor();
                List<J2Vhdl_ModuleVhdlType.Assgn> assignments = new LinkedList<J2Vhdl_ModuleVhdlType.Assgn>();
                boolean bStepSeen = false;                 // before step() there may be assignments
                List<StringBuilder> preAssignments = null;
                for(JavaSrc.Statement stmnt: ctor.get_statement()) {             // all first level statements in the ctor
                  JavaSrc.Expression expr = stmnt.get_Expression();
                  if(expr !=null && expr.isAssignExpr()) {                 //without step and update, and test operations
                    
                    StringBuilder sAssg = new StringBuilder(100);  // for one line assignment
                    VhdlExprTerm term = this.genExpr.genExpression(sAssg, expr, false, false, moduleInstance, nameInnerClassVariable, "",  "<=", null);
                    int sep = sAssg.indexOf("<=");
                    int sepe = sAssg.indexOf(";");
                    final J2Vhdl_ModuleVhdlType.Assgn assgn;
                    if(sep >=0) {
                      if(term.varCurrent_.getLocation() == J2Vhdl_Variable.Location.input) {
                        assgn = new J2Vhdl_ModuleVhdlType.Assgn(sAssg.substring(0, sep).trim(), sAssg.substring(sep+2, sepe));
                      } else if(!bStepSeen) {
                        if(preAssignments == null) { preAssignments = new LinkedList<StringBuilder>(); }
                        preAssignments.add(sAssg);         // assignment before vhdl instance
                        assgn = null;                      // not for the vhdl instance
                      } else {   //not output, output is right side of expression.
                        assgn = new J2Vhdl_ModuleVhdlType.Assgn(sAssg.substring(sep+2, sepe).trim(), sAssg.substring(0, sep));
                      }
                    } else {
                      assgn = new J2Vhdl_ModuleVhdlType.Assgn("??", sAssg.toString());
                    }
                    if(assgn !=null) {
                      assignments.add(assgn);
                    }
                  } else if(!bStepSeen && expr !=null && expr.getSize_ExprPart() ==1) {
                    JavaSrc.ExprPart part0 = expr.get_ExprPart().iterator().next();
                    JavaSrc.SimpleValue val0= part0.get_value();
                    JavaSrc.SimpleMethodCall oper0;
                    if(val0 !=null && (oper0 = val0.get_simpleMethodCall()) !=null) {
                      String nameOper = oper0.get_methodName();
                      bStepSeen |= nameOper.equals("step");
                    }
                  }
                } 
                OutTextPreparer.DataTextPreparer args = this.vhdlCmpnCall.createArgumentDataObj();
                args.setArgument("name", nameVhdlMdl);
                args.setArgument("typeVhdl", sVhdlModule);
                args.setArgument("vars", assignments);
                args.setArgument("preAssignments", preAssignments);
                this.vhdlCmpnCall.exec(wOut, args);
              }
            }
        } }
      }
    
  }

  
  
  private static String getVhdlModuleType ( J2Vhdl_ModuleType mdlType) {
    String sVhdlMdlType = null;
    JavaSrc.AnnotationUse annotVhdlModule = mdlType.moduleClass.getAnnotation("Fpga.VHDL_MODULE");
    if(annotVhdlModule == null) { J2Vhdl_GenExpr.vhdlError("module should have annotation Fpga.VHDL_MODULE: ", mdlType.moduleClass); }
    else {
      for(JavaSrc.ParamNameValue param:  annotVhdlModule.get_param()) {
        sVhdlMdlType = param.get_simpleValue().get_simpleStringLiteral();
      }
    }
    return sVhdlMdlType;
  }
  

  /**Search the appropriate ctor of the given class which is designated with @{@link Fpga.VHDL_PROCESS}
   * @param clazz from this inner class
   * @return the parse result for that.
   */
  public JavaSrc.ConstructorDefinition getCtorVhdlCall ( JavaSrc.ClassDefinition clazz) {
    JavaSrc.ClassContent clazzC = clazz.get_classContent();
    if(clazzC.getSize_constructorDefinition()>0) {
      for(JavaSrc.ConstructorDefinition ctor: clazzC.get_constructorDefinition()) {
        JavaSrc.ModifierMethod modif = ctor.get_ModifierMethod();
        if(modif !=null) {
          String annot = modif.get_Annotation();
          if(annot !=null && annot.startsWith("Fpga.LINK_VHDL_MODULE")) {
            return ctor;
          }
        }
    } }
    return null;
  }
  
  
  /**Gen all processes for VHDL from all parsed sources.
   * @param wOut to write
   * @throws Exception 
   */
  void genAssignments(StringBuilder wOut) throws Exception {
    for(Map.Entry<String, J2Vhdl_ModuleInstance> esrc: this.fdata.idxModules.entrySet()) {          // all sources, instances 
      J2Vhdl_ModuleInstance mdl = esrc.getValue();
      J2Vhdl_ModuleType mdlt = mdl.type;
      String sModule = esrc.getKey();
      JavaSrc.ClassDefinition theclass = mdl.type.moduleClass;     // get the only one public class of module
      JavaSrc.ClassContent theClassC = theclass.get_classContent();
      Iterable<JavaSrc.MethodDefinition> ioper = theClassC.get_methodDefinition(); 
      if(ioper !=null) for(JavaSrc.MethodDefinition oper : ioper) { // get inner class of public module class  
        String nameOper = oper.get_name();
        if(nameOper.equals("prepare") || nameOper.equals("output")) {            // it is an inner class for a VHDL RECORD and PROCESS
          Iterable<JavaSrc.Statement> istmnt = oper.get_methodbody().get_statement();
          if(istmnt !=null) for(JavaSrc.Statement stmnt : istmnt) {
            boolean dbgStop = false;
            if(this.dbgStopFile !=null) { 
              int[] lineColumn = new int[2];
              String file = stmnt.getSrcInfo(lineColumn);  // TxSpe BlinkingLedCt ClockDivider BlinkingLed_Fpga
              if(file.contains(this.dbgStopFile) && lineColumn[0] >= this.dbgStopLine1 && lineColumn[0] <= this.dbgStopLine2) {
                Debugutil.stop();
                dbgStop = true;
            } }
            if(stmnt.isAssignExpr()) {                     // especially not step() and update(), or test operations. 
              //next line is faulty if a Process is created on top level, test with null is proper.
              //commented: this.vhdlConv.genStmnt(wOut, stmnt, mdl, mdlt.nameType, 0, false);
              this.genStmnt.genStmnt(wOut, stmnt, mdl, null, 0, false);
            }
//            JavaSrc.Expression expr = stmnt.get_Expression();
//            expr.get
          }
        }
      }
    }
  }
  
  
  /**This is only for the top instance. The update() copies values to the output.
   * @param wOut
   * @throws Exception
   */
  void genOutput(StringBuilder wOut) throws Exception {
    for(Map.Entry<String, J2Vhdl_ModuleType> esrc:  this.fdata.idxModuleTypes.entrySet()) {
      J2Vhdl_ModuleType src = esrc.getValue();
      if(src.isTopLevel()) {                                 // All toplevel variable
        J2Vhdl_ModuleInstance topInstance = this.fdata.topInstance;
        this.genExpr.setInnerClass(src.nameType, topInstance.nameInstance);
        JavaSrc.ClassDefinition theclass = src.moduleClass;
        JavaSrc.ClassContent theClassC = theclass.get_classContent();
        if(theClassC.getSize_methodDefinition() >0) //...for
        for(JavaSrc.MethodDefinition oper: theClassC.get_methodDefinition()) {
          if(oper.get_name().equals("update")) {
            for(JavaSrc.Statement stmnt : oper.get_methodbody().get_statement()) {
              this.genStmnt.genStmnt(wOut, stmnt, topInstance, src.nameType, 0, false);
            }
          }
        }
      }
    }
  }

  
  void reportContentOfAll(Appendable out) throws IOException {
    StringFormatter sf = new StringFormatter(out, false, "\n", 100);
    out.append("\n== J2Vhdl_ModuleType: {@link J2Vhdl_FpgaData#idxModuleTypes}\n");
    out.append(" ModuleType         |  ifcOperation()                       | access    {@link J2Vhdl_ModuleType#idxIfcExpr} \n");
    out.append("--------------------+---------------------------------------+------------------------------------------------\n");
    for(Map.Entry<String, J2Vhdl_ModuleType> emdl: this.fdata.idxModuleTypes.entrySet()) {
      J2Vhdl_ModuleType mdl = emdl.getValue();
      if(!(mdl instanceof J2Vhdl_ModuleVhdlType)) {
        String sNameModuleType = emdl.getKey();
        if(mdl.idxIfcExpr.size()==0) {
          out.append(sNameModuleType).append("\n");
        } else {
          for(Map.Entry<String, J2Vhdl_ModuleType.IfcConstExpr> eIfc : mdl.idxIfcExpr.entrySet()) {
            String ifcName = eIfc.getKey();
            J2Vhdl_ModuleType.IfcConstExpr ifcAccess = eIfc.getValue();
            sf.reset();
            String sAccess;
            //VhdlExprTerm.ExprType type;
            if(ifcAccess.constVal !=null) { sAccess = ifcAccess.constVal.var.sElemVhdl; }
            else if(ifcAccess.expr !=null) { sAccess = ifcAccess.expr.toString(); }
            else { sAccess = "... only sim"; }
            sf.add(sNameModuleType).pos(20).add("| ").add(ifcName).add("()").pos(60).add("| ").add(sAccess);
            sf.flushLine("\n");
            sNameModuleType = "";  //next lines, empty
          }
        }
        out.append("--------------------+---------------------------------------+------------------------------------------------\n");
      }
    }
    boolean bOwnVhdlModules = false;
    for(Map.Entry<String, J2Vhdl_ModuleType> emdl: this.fdata.idxModuleTypes.entrySet()) {
      J2Vhdl_ModuleType mdl = emdl.getValue();
      if(mdl instanceof J2Vhdl_ModuleVhdlType) {
        J2Vhdl_ModuleVhdlType mdlv = (J2Vhdl_ModuleVhdlType) mdl;
        if(!bOwnVhdlModules) {
          bOwnVhdlModules = true;
          out.append(" ModuleType VHDL    |  io \n");
          out.append("--------------------+---------------------------------------\n");
        }
        String sNameModuleType = emdl.getKey();
        for(J2Vhdl_Variable var : mdlv.io) {
          sf.add(sNameModuleType).pos(20).add("| ").add(var.sElemVhdl).add(" : ").add(var.location.s).add(" ").add(var.getVhdlType());
          sf.flushLine("\n");
          sNameModuleType = "";  //next lines, empty
        }
      }
    }

    for(Map.Entry<String, J2Vhdl_ModuleInstance> emdl: this.fdata.idxModules.entrySet()) {
      J2Vhdl_ModuleInstance mdl = emdl.getValue();
      String name = emdl.getKey();
      out.append("\n\n== Module: ").append(name);
      if(mdl.idxAggregatedModules !=null && mdl.idxAggregatedModules.size() >0) {
        out.append("\n  localName         | accessed module     {@link J2Vhdl_ModuleInstance#idxAggregatedModules}");
        out.append("\n--------------------+----------------\n");
        for(Map.Entry<String, J2Vhdl_ModuleInstance.InnerAccess> eIfc : mdl.idxAggregatedModules.entrySet()) {
          String innerName = eIfc.getKey();
          J2Vhdl_ModuleInstance.InnerAccess refMdl = eIfc.getValue();
          String sType = refMdl !=null && refMdl.mdl !=null ? refMdl.mdl.type.moduleClass.get_classident(): "???refModuleNotFound";
          String sName = refMdl !=null && refMdl.mdl !=null  ? refMdl.mdl.nameInstance + "." + refMdl.sAccess: "not found";
          sf.reset();
          sf.add("  ").add(innerName).pos(20).add("| ").add(sName).add(" : ").add(sType);
          sf.flushLine("\n");
        }
        out.append("--------------------+----------------");
      }
      if(mdl.idxSubModules !=null && mdl.idxSubModules.size() >0) {
        out.append("\n  localName         | sub module     {@link J2Vhdl_ModuleInstance#idxSubModules}");
        out.append("\n--------------------+----------------\n");
        for(Map.Entry<String, J2Vhdl_ModuleInstance> e : mdl.idxSubModules.entrySet()) {
          String innerName = e.getKey();
          J2Vhdl_ModuleInstance submdl = e.getValue();
          sf.reset();
          sf.add("  ").add(innerName).pos(20).add("| ").add(submdl.nameInstance).add(" : ").add(submdl.type.nameType);
          sf.flushLine("\n");
        }
        out.append("--------------------+----------------");
      }
    }
    //
    out.append("\n== Variables: ");
    out.append("\n  search-name                           | VHDL access : type ");
    out.append("\n----------------------------------------+----------------\n");
    for( Map.Entry<String, J2Vhdl_Variable> eVar: this.fdata.idxVars.entrySet()) {
      String nameVar = eVar.getKey();
      J2Vhdl_Variable var = eVar.getValue();
      sf.reset();
      sf.add("  ").add(nameVar).pos(40,1).add("| ").add(var.sElemVhdl).add(" : ").add(var.getVhdlType());
      sf.flushLine("\n");
    }
    out.append("----------------------------------------+----------------\n");
    //
    out.append("\n== Type Variables: ");
    out.append("\n  search-name                           | VHDL access : type ");
    out.append("\n----------------------------------------+----------------\n");
    for( Map.Entry<String, J2Vhdl_Variable> eVar: this.fdata.idxRecordVars.entrySet()) {
      String nameVar = eVar.getKey();
      J2Vhdl_Variable var = eVar.getValue();
      sf.reset();
      sf.add("  ").add(nameVar).pos(40,1).add("| ").add(var.sElemVhdl).add(" : ").add(var.getVhdlType());
      sf.flushLine("\n");
    }
    out.append("----------------------------------------+----------------\n\n");
    //
    out.append("\n== Constants:        {@link J2Vhdl_ModuleType#idxConstDef}");
    out.append("\n  search-name                           | VHDL access : value ");
    out.append("\n-----------------------------------+----------------------------------------+----------------\n");
    for( Map.Entry<String, J2Vhdl_ConstDef> eVar: this.fdata.idxConstDef.entrySet()) {
      String nameVar = eVar.getKey();
      J2Vhdl_ConstDef cvar = eVar.getValue();
      sf.reset();
      String vhdlType = cvar.var.getVhdlType();
      sf.add("  ").add(nameVar).pos(35,1).add("| ").add(cvar.var.sElemVhdl).pos(75,1).add(" | ").add(vhdlType).add(" := ").add(cvar.value);
      sf.flushLine("\n");
    }
    out.append("----------------------------------------+----------------\n\n");
    //
    out.append("\n== enumConstants:    {@link J2Vhdl_ModuleType#idxEnumBitDef}");
    out.append("\n  search-name                           | VHDL access : value ");
    out.append("\n-----------------------------------+----------------------------------------+----------------\n");
    for( Map.Entry<String, String> eVar: this.fdata.idxEnumBitDef.entrySet()) {
      String nameVar = eVar.getKey();
      String cvar = eVar.getValue();
      sf.reset();
      sf.add("  ").add(nameVar).pos(35,1).add("| ").add(cvar);
      sf.flushLine("\n");
    }
    out.append("-----------------------------------+----------------------------------------+----------------\n\n");
  }
  
  
  
}


