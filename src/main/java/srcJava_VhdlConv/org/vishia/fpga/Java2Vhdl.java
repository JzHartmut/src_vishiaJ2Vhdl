package org.vishia.fpga;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.script.ScriptException;

import org.vishia.parseJava.JavaParser;
import org.vishia.parseJava.JavaSrc;
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
  public static final String sVersion = "2022-04-28";

  
  
  
  
  
  
  
  
  
  
  
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
    
    
    Args(){
      super.aboutInfo = "Java2Vhdl made by HSchorrig, 2022-02-16 - 2022-02-16";
      super.helpInfo=" all args obligate, see www.vishia.org/fpga/TODO";
      addArg(new Argument("-i", ":path/to/template.vhd", this.setInVhdl));
      addArg(new Argument("-o", ":path/to/output.vhd", this.setOutVhdl));
      addArg(new Argument("-tmp", ":path/to/dirTmp", this.setDirTmpVhdl));
      addArg(new Argument("-sdir", ":path/to/srcJava", this.setDirJavaVhdlSource));
      addArg(new Argument("-rep", ":path/to/fileReport.txt", this.setOutContentReport));
      addArg(new Argument("", "pkg.path.VhdlModule", this.setJavaVhdlSrc));
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

  
  /**Aggregation to VhdlConv, it contains some more data. */
  final VhdlConv vhdlConv = VhdlConv.d;
  
  final J2Vhdl_FpgaData fdata = VhdlConv.d.fdata; //new J2Vhdl_FpgaData(); 
  
  
  OutTextPreparer vhdlHead, vhdlAfterPort, vhdlConst;

  
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
    parseAll();                                                              // parse top level and depending classes. 
    evaluateModules();
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
    genProcesses(out);
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



  /**Parse primary all argument files and secondly all depending files in inner classes "Ref".
   * The first primary file (named firstly in argument list) is the toplevel. 
   * Note: more as one time referenced files are parsed only one time, using and checking the {@link #idxParseResult},
   * {@link #parseSrc(File, String, String, int)} is called recursively.  
   * @throws Exception 
   */
  public void parseAll() 
  throws Exception 
  { 
    List<String> javaSrcToProcess = new LinkedList<String>();
    javaSrcToProcess.addAll(this.args.fJavaVhdlSrc);
    boolean bTopLevel = true;
    while(javaSrcToProcess.size() >0) {
      String pathSrcJava = javaSrcToProcess.remove(0);
      JavaSrc parseResult = parseSrc(this.args.dirJavaVhdlSrc, pathSrcJava);
      if(parseResult !=null) {
        if(parseResult.getSize_classDefinition() >0) //...for
        for(JavaSrc.ClassDefinition pclass: parseResult.get_classDefinition()) {
          String className = pclass.get_classident();          // should contain only one public class.
          J2Vhdl_ModuleType moduleType = new J2Vhdl_ModuleType(className, parseResult, pclass, bTopLevel);
          this.fdata.idxModuleTypes.put(className, moduleType);      // Store in idxModuleTypes with the simple className
          J2Vhdl_ModuleInstance topMdl = null;
          if(bTopLevel) {                                      // build an module instance also from the top level file as Module
            topMdl = moduleType.topInstance;
            assert(topMdl !=null); // was created in ctor
            this.fdata.idxModules.put(className, topMdl);
          }
          //
          if(pclass.getSize_classDefinition() >0) //...for // iterate over all inner classes to search a Modules class
          for(JavaSrc.ClassDefinition iclass: pclass.get_classDefinition()) {
            //----------------------------------------------------------------------------------
            String iClassName = iclass.get_classident();       // search a module class inside the given class.
            if(iClassName.equals("Modules")) {                 // The Modules class contains all used sub modules.
              //
              // -------------------------------------------   // All elements in class Modules:
              for(JavaSrc.VariableInstance mVar: iclass.get_variableDefinition()) {
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
                
        } } } } 
      }
      bTopLevel = false;
    } //while(javaSrcToProcess.size() >0);
  }

  
  
  /**Parse one source as part of the VHDL contribution. 
   * <ul> 
   * <li>It is either a top level class given as argument class for {@link Java2Vhdl#main(String[])}
   * <li>or it is a used sub module which is named in the <code>class Modules</code> inner class of this parsed class.
   *   For that classes this operation is also called recursively inside.
   * </ul> 
   * <pre>
   * public class Modules {
   *   public final SubModule nameInstance = new SubModule(references, for_aggregations));
   * </pre>  
   * For each detected sub modules in this module an entry in {@link #idxModules} is created and saved:
   * <ul>
   * <li><code>As key: String nameInstance</code>: name of the sub module in Java, 
   *   it builds the first part for the name of the RECORD instance for this module in VHDL.
   *   The second part comes from the inner class instance for a PROCESS.
   *   <br>For example the RECORD instance name is <code>nameInstance_q</code>
   * <li>As entry: {@link Module}. Its content is prepared in {@link #prepareModule(Module, org.vishia.parseJava.JavaSrc.VariableInstance)}.
   * description see their.
   * </ul> 
   * Also the {@link #idxParseResult} is supplemented with this parsed source
   * for more as one sub modules (VHDL RECORD SIGNAL instance) with the same type. <br>
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
      JavaSrc parseResult = this.parser.parseJava(fParse);
      return parseResult;
    }
  }
  
  
  
  private void evaluateModules ( ) throws Exception {
    for(Map.Entry<String, J2Vhdl_ModuleType> e : this.fdata.idxModuleTypes.entrySet()) {
      J2Vhdl_ModuleType mdlt = e.getValue();
      String sClassName = mdlt.nameType;
      if(mdlt.moduleClass.getSize_classDefinition()>0) //...for
      for(JavaSrc.ClassDefinition iclass: mdlt.moduleClass.get_classDefinition()) {
        //----------------------------------------------------------------------------------
        String iClassName = iclass.get_classident();       // search a module class inside the given class.
        if(iClassName.equals("Modules")) {                 // The Modules class contains all used sub modules.
          //
          // -------------------------------------------   // All elements in class Modules:
          for(JavaSrc.VariableInstance mVar: iclass.get_variableDefinition()) {
            //--------------------------------------------------------------
            String nameSubModule = mVar.get_variableName();  //This builds the left part of instance name for all RECORD instances in VHDL
            final JavaSrc.Type type = mVar.get_type();
            final String sType = type.get_name();          // search the appropriate parsed source.java result
            System.out.println("  Module: " + nameSubModule + " : " + sType);
            J2Vhdl_ModuleType typeSubModule = this.fdata.idxModuleTypes.get(sType);
            if(typeSubModule !=null) {
              J2Vhdl_ModuleInstance subModule = new J2Vhdl_ModuleInstance(nameSubModule, typeSubModule, false); //typeSubModule, false);
              prepareModuleInstance(subModule, mVar);
              typeSubModule.XXXidxSubModules.put(nameSubModule, subModule);  // register the module instance in the type as used composite sub module
              this.fdata.idxModules.put(nameSubModule, subModule); // register the module globally as existing module instance in the whole VHDL file (it's a RECORD instance)
              if(mdlt.topInstance !=null) {
                mdlt.topInstance.idxAggregatedModules.put(nameSubModule, subModule);
              }
            } else {
              VhdlConv.vhdlError("J2Vhdl_ModuleType not found :" + sType, mVar);
            }
            
          }
          evaluateModulesCtor(iclass);                     // find init(ref, ref...) statement in ctor
        }
        else if( mdlt.isTopLevel() && ( iClassName.equals("Input")
               || iClassName.equals("Output") )) {          // In/Output signals of the whole FPGA or a VHDL sub module.
          String name = sClassName + "." + Character.toLowerCase(iClassName.charAt(0)) + iClassName.substring(1);
          String nameType = sClassName + "_" + iClassName;  // J2Vhdl_ModuleType ToplevelType_Input
          J2Vhdl_ModuleType inoutType = new J2Vhdl_ModuleType(nameType, null, iclass, false);
          prepareIfcOperationsInModuleType(inoutType, iclass);
          J2Vhdl_ModuleInstance inoutModule = new J2Vhdl_ModuleInstance(name, inoutType, true);  // J2Vhdl_ModuleInstance ToplevelType_input
          this.fdata.idxModules.put(name, inoutModule);
        }
        else if(iClassName.equals("Ref")) {
          
        }
        else {                                             // all other classes are Process classes, gather there variables
//          this.vhdlConv.mapVariables(nameModule, iclass);
        }
      }
    }
  }

  
  
  
  
  private void evaluateInterfacesInModules ( ) throws Exception {
    for(Map.Entry<String, J2Vhdl_ModuleType> e : this.fdata.idxModuleTypes.entrySet()) {
      J2Vhdl_ModuleType mdlt = e.getValue();
//      String sClassName = mdlt.nameType;
      if(mdlt.moduleClass.getSize_classDefinition()>0) //...for
      for(JavaSrc.ClassDefinition iclass: mdlt.moduleClass.get_classDefinition()) {
        
      }
      prepareIfcOperationsInModuleType(mdlt, mdlt.moduleClass);
    }
  }
  



  
  
  
  /**Search and evaluate the ctor of the <code>Modules</code> inner class.
   * It may contain some <code>this.module.init(ref,...)</code> routines for module aggregations.
   * @param iclass
   */
  private void evaluateModulesCtor ( JavaSrc.ClassDefinition iclass ) {
    Iterable<JavaSrc.ConstructorDefinition> ctors = iclass.get_constructorDefinition();
    if(ctors !=null) {
      for(JavaSrc.ConstructorDefinition ctor: ctors) {
        
        Iterable<JavaSrc.Statement> stmnts = ctor.get_statement();
        if(stmnts !=null) for(JavaSrc.Statement stmnt : stmnts) {
          JavaSrc.Expression operExpr = stmnt.get_Expression();
          if(operExpr !=null) {
            JavaSrc.SimpleValue operVal = operExpr.get_ExprPart().iterator().next().get_value();
            if(operVal !=null) {
              JavaSrc.SimpleMethodCall oper = operVal.get_simpleMethodCall();
              String nameOper = oper.get_methodName(); 
              if(nameOper.equals("init") || nameOper.startsWith("init_")) {     // the init(ref,...) call in the ctor of Modules
                JavaSrc.ActualArguments actArgs = oper.get_actualArguments();
                int zArgs = actArgs.getSize_Expression();
                JavaSrc.Reference refSubModule = operVal.get_reference();
                String nameSubmodule = refSubModule.getSimpleRefVariable();
                if(nameSubmodule == null) { VhdlConv.vhdlError("style guide init", oper); }
                else {
                  J2Vhdl_ModuleInstance subModule = this.fdata.idxModules.get(nameSubmodule);
                  //J2Vhdl_ModuleType subModuleType = subModule.type;
                  JavaSrc.ClassDefinition subModuleClass = subModule.type.moduleClass;
                  for(JavaSrc.MethodDefinition operInitType : subModuleClass.get_methodDefinition()) {
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
  
  
  
  
  
  /**It prepares a module instance which is given in the <pre>
   * public class Modules {
   *   public final Module nameInstance = new Module(references, for_aggregations));
   * </pre>
   *   <ul>
   *   <li>{@link Module#moduleClass} The parse result of the Java src file, the relevant class
   *   <li>{@link Module#idxAggregatedModules}. This is an index of the aggregated module of this sub module
   *     build from the formal and actual arguments in the constructor,
   *     in the code example above: <code>(references, for_aggregations)</code>
   *     This index (java.util.Map) is used for the association from a reference to an aggregation
   *     to another module inside the Java code (<code>formalName</code>)
   *     to the really used module identifier.
   *     Each entry contains:
   *     <ul>
   *     <li>key <code>formalName</code>: name of the formal argument in the constructor of the Submodule. 
   *       This is the internal name used in Java code of this module, not related to VHDL.
   *       The same <code>formalName</code> should be used as reference in the inner class <code>Ref</code>
   *       in this class: <pre>
   *       private static final class Ref {
   *         final Type formalName;
   *       </pre>
   *       Hence the used reference in the Java code to an access data form another module is:<br>
   *       <code>ref.formalName.accessOp()</code>
   *     <li>{@link VhdlConv.AggregatedModule#name} <code>moduleName</code> of the aggregated module in the parent. The real used module. 
   *       It builds the first part of the SIGNAL name of the RECORD in VHDL.
   *     <li>{@link VhdlConv.AggregatedModule#idxAccess}. This is also in index, contains:
   *       <ul>
   *       <li>String key: name of an access operation, able to use in an interface.
   *       <li>Accessed variable in the inner process class in the form as it is necessary for VHDL. 
   *         It is built from the access operation (interface implementation) which contains for example<br>
   *         <code>return q.variable; //</code>whereby q is the internal instance for the process.<br>
   *         This is translaed to <code>"_q.variable"</code> which builds the correct access to a RECORD instance in VHDL
   *         joined with the instance name <code>moduleName_q.variable</code> for VHDL access.
   *       </ul> 
   *     </ul>
   * </ul> 
   * @param module The simple created module only from the ctor {@link Module#Module(String, JavaSrc)}
   * @param mVar This is the parse result from the variable in the <code>class Modules { Type variable = new Type(...)</code>
   */
  private void prepareModuleInstance ( J2Vhdl_ModuleInstance module, JavaSrc.VariableInstance mVar) {

    JavaSrc.Expression newExpr = mVar.get_Expression();    //This is the init value of the module, should be new Module(...)
    JavaSrc.ExprPart newPart = newExpr.get_ExprPart().iterator().next();  //the only one part of expression is new Module(...)
    JavaSrc.NewObject newObj = newPart.get_value().get_newObject();       //the constructor of new
    JavaSrc.ActualArguments actArgs = newObj.get_actualArguments();       //the actual args are the instance names of the used submodules.
    if(actArgs !=null) {
      int zArgs = actArgs.getSize_Expression();            // Number of args, >=1 to search the proper ctor of the referenced class
      
      Iterator<JavaSrc.Argument> formalArgs = null;        // The arguments of the matching ctor of the module class to get names in the module class
      JavaSrc.ClassDefinition moduleClass = module.type.moduleClass; 
      for(JavaSrc.ConstructorDefinition moduleCtor : moduleClass.get_constructorDefinition()) {
        if(moduleCtor.getSize_argument() == zArgs) {       // the ctor which has the same number of arguments should match, do not check type of arguments.
          formalArgs = moduleCtor.get_argument().iterator();
          break;
        }
      }
      if(formalArgs == null) { VhdlConv.vhdlError("constructor in submodule not found", newObj); }
      else {
        if(module.nameInstance.equals("txSpe"))
          Debugutil.stop();
        associateActualWithTypeArgumentRefs(module, actArgs, formalArgs);
      }
    }
  }
  
  
  
  private void associateActualWithTypeArgumentRefs ( J2Vhdl_ModuleInstance module, JavaSrc.ActualArguments actArgs, Iterator<JavaSrc.Argument> formalArgs ) {
    for(JavaSrc.Expression aggrArgExpr: actArgs.get_Expression() ) {  //the expression for the new Module(value, ...
      JavaSrc.ExprPart aggrArgExpr1 = aggrArgExpr.get_ExprPart().iterator().next();  //The only one part of the expression
      JavaSrc.SimpleValue aggrVal = aggrArgExpr1.get_value();
      //StringBuilder sbAggrRef = new StringBuilder();
      String sAggrRef = "";                                // build aAggrName to search the aggregated module maybe with a reference,
      JavaSrc.Reference aggrRef = aggrVal.get_reference(); // usual this is without aggregation if calling arguments are used.
      while(aggrRef !=null) {
        JavaSrc.SimpleVariable aggrRefVar = aggrRef.get_referenceAssociation();
        if(aggrRefVar !=null) {
          sAggrRef = aggrRefVar.get_variableName() + ".";  // The variable name of the last ref is used only. All other is Java internally
        }
        aggrRef = aggrRef.get_reference();     // next ref in chain ref.refNext
      }
      String sAggrVarName = aggrVal.get_simpleVariable().get_variableName();
      String sAggrName;
      if(sAggrVarName.equals("this") && sAggrRef !=null) {      // it is a reference to the enclosing class Type.this
        sAggrName = sAggrRef.substring(0, sAggrRef.length()-1);  //The own module is stored with the Type name.
      } else {
        sAggrName = sAggrRef + sAggrVarName;
      }
      J2Vhdl_ModuleInstance aggrModule = this.fdata.idxModules.get(sAggrName);
      if(aggrModule ==null) {
        Debugutil.stop();
      }
      //String sAggrRecordInstance = sAggrRef + sAggrName;
      JavaSrc.Argument ctorArg = formalArgs.next();
      String sNameFormalArg = ctorArg.get_variableName();   //formalName: name of the formal argument, it is exactly the used reference in the module.
      //VhdlConv.AggregatedModule aggrModule = new VhdlConv.AggregatedModule();
      //aggrModule.name = sAggrRecordInstance;
      if(aggrModule == null) {
        System.out.println("    Aggregation: " + sNameFormalArg + "<-- ???moduleNotFound");
      } else {
        System.out.println("    Aggregation: " + sNameFormalArg + "<--" + aggrModule.nameInstance);
      }
      module.idxAggregatedModules.put(sNameFormalArg, aggrModule);
    }

  }

  
  
  
  
  
  
  private void prepareIfcOperationsInModuleType(J2Vhdl_ModuleType moduleType, JavaSrc.ClassDefinition pclass) throws Exception {
    Iterable<JavaSrc.MethodDefinition> iter = pclass.get_methodDefinition();
    if(iter !=null) for(JavaSrc.MethodDefinition oper: iter ) {
      String nameOper = oper.get_name();
      if(nameOper.equals("ct"))
        Debugutil.stop();
      JavaSrc.ModifierMethod modif = oper.get_ModifierMethod();
      String sAnnot = modif == null ? null: modif.get_Annotation();
      String name = oper.get_name();
      String a_Override = oper.get_A_Override();
      //String sAnnotation = modif == null ? null :modif.get_Annotation();       // only search for interface implementig operations
      if( a_Override !=null /*&& sAnnotation.startsWith("@Override") */  
          && !name.equals("step")                          // but not from the FpgaModule_ifc
          && !name.equals("update") 
        || sAnnot !=null 
          && sAnnot.equals("Fpga.GetterVhdl")
        ) {          
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
                  String nameVhdl = moduleType.nameType + "_" + name;
                  constDef = createConst(nameVhdl, nameVhdl, expr);
                  expr = null;  //no more necessary.
            } } }
            moduleType.idxIfcExpr.put(name, new J2Vhdl_ModuleType.IfcConstExpr(expr, constDef));

            
//            
//            JavaSrc.ExprPart exprPart = expr.get_ExprPart().iterator().next(); //first part
//            JavaSrc.SimpleValue val = exprPart.get_value();
//            JavaSrc.Reference ref = val.get_reference();   // return processInstance. normally variable inside a process class
//            while(ref !=null) {
//              JavaSrc.SimpleVariable refVar = ref.get_referenceAssociation();
//              String sRefVar = refVar == null? null : refVar.get_variableName();  // other than a variable in the reference is not expected.
//              if(sRefVar !=null && !sRefVar.equals("this") && !sRefVar.equals("ref")) {
//                sbAccess.append(".").append(sRefVar);      // this.ref. is ignroed
//              }
//              ref = ref.get_reference();                   // next member in reference
//            }
//            JavaSrc.SimpleVariable var = val.get_simpleVariable();
//            if(var !=null) {                               // access to variable in a process data class, usual case
//              sbAccess.append('.').append(var.get_variableName());
//            } else {
//              JavaSrc.SimpleMethodCall ifcOper = val.get_simpleMethodCall();
//              if(ifcOper !=null) {                         // this is an access to another interface, resolve later.
//                sbAccess.append('.').append(ifcOper.get_methodName()).append("()");
//              } else {
//                JavaSrc.ConstNumber constVal = val.get_constNumber();
//                if(constVal !=null) {                      // a constant returned from interface
//                  String nameVhdl = moduleType.nameType + "_" + name;
//                  //String javaPath = name + "()";
//                  createConst(nameVhdl, nameVhdl, expr);
//                  sbAccess.append("#").append(nameVhdl);
//              } }
//            }
//            moduleType.idxIfcOperation.put(name, sbAccess.toString());
          }
        }
      }
    }
  }
  
  
  
  
  /**This operation looks for all variable in all inner VHDL Process classes
   * and calls {@link VhdlConv#mapVariables(String, String, org.vishia.parseJava.JavaSrc.ClassDefinition)} 
   * for each module which fills the global visible 
   * {@link VhdlConv#idxVars} with all variables from all PROCESS classes
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
        this.vhdlConv.setInnerClass(nameProcess, nameModule);              // records from all inner classes, same name as type
     //   this.vhdlConv.mapInOutVariables(type.moduleClass);
      } else if(module.bInOutModule) {
        final String nameProcess = nameModule;
        this.vhdlConv.setInnerClass(nameProcess, nameModule);              // records from all inner classes, same name as type
        this.vhdlConv.mapInOutVariables(type.moduleClass, module);

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
      gatherConstValues(theclass, nameTheClass + "_" + nameTheClass, nameModule);
      Iterable<JavaSrc.ClassDefinition> iclasses = theclass.get_classDefinition(); 
      if(iclasses !=null) for(JavaSrc.ClassDefinition iclass : iclasses) { // get inner class of public module class  
        String nameiClass = iclass.get_classident();
        String annotation = iclass.get_Annotation();
        if(annotation !=null && annotation.equals("Fpga.VHDL_PROCESS") || nameiClass.equals("In") || nameiClass.equals("Out")) {
//        if( (!isToplevel || !nameiClass.equals("In") && !nameiClass.equals("Out")) && !nameiClass.equals("Ref") && !nameiClass.equals("Modules")) {
          final String nameProcess = nameModule + "_" + nameiClass;
          this.vhdlConv.setInnerClass(nameProcess, nameModule);              // records from all inner classes, same name as type
          this.vhdlConv.mapVariables(nameModule, nameTheClass, iclass);
    } }
  }
  

  
  
  void gatherConstValues ( JavaSrc.ClassDefinition iclass, String nameType, String nameModule) throws Exception {
    if(iclass.getSize_enumDefinition()>0) { 
      for( JavaSrc.EnumDefinition enumd: iclass.get_enumDefinition()) {
        String identType = enumd.get_enumTypeIdent();
        for(JavaSrc.EnumElement enume: enumd.get_enumElement()) {
          String ident = enume.get_enumIdent();
          String nameVhdl = nameType + "_" + identType + "_" + ident;
          String javaPath = nameModule + "." + identType + "." + ident;
          JavaSrc.ActualArguments args = enume.get_actualArguments();
          JavaSrc.Expression expr = null;
          if(args !=null) {
            expr = args.get_Expression().iterator().next();  //use the first argument.
          }
          if(expr !=null) {
            createConst(javaPath, nameVhdl, expr);
          }
        }
    } }
  }
  
  
  
  /**Creates a {@link J2Vhdl_ConstDef} and stores in {@link VhdlConv#idxConstDef} from a given expression
   * @param javaPath
   * @param nameVhdl
   * @param valueterm expression contains only the const value, no more.
   * @throws Exception 
   */
  J2Vhdl_ConstDef createConst (String javaPath, String nameVhdl, JavaSrc.Expression expr) throws Exception {
    VhdlExprTerm valueterm = this.vhdlConv.genExprOnePart(expr, null, "");
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
    J2Vhdl_ConstDef constDef = new J2Vhdl_ConstDef(new J2Vhdl_Variable(nameVhdl, false, type, nrBits, javaPath, nameVhdl), valueVhdl);
    this.fdata.idxConstDef.put(javaPath, constDef);
    return constDef;
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
        if(theclass.getSize_classDefinition() >0)  //...for
        for(JavaSrc.ClassDefinition iclass : theclass.get_classDefinition()) { // get inner class of public module class  
          final String nameiClass = iclass.get_classident();
          String sAnnot = iclass.get_Annotation();
          if(sAnnot !=null && sAnnot.equals("Fpga.VHDL_PROCESS") && iclass.getSize_variableDefinition() >0
            || ( !result.isTopLevel() && (nameiClass.equals("In") || nameiClass.equals("Out")) ) ) {
            final String nameRecord = nameModule + "_" + nameiClass;
            this.vhdlConv.setInnerClass(nameRecord, nameModule);
            wOut.append("\nTYPE ").append(nameRecord).append("_REC IS RECORD");
            for(JavaSrc.VariableInstance var:iclass.get_variableDefinition()) {
              String varName = var.get_variableName();
              if( !varName.equals("_time_") && !varName.startsWith("m_")) {
                String sVhdlType = this.vhdlConv.assembleType(var, nameRecord);
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
    boolean isTopLevel = false;  //only modules stored which are not top level.
    for(Map.Entry<String, J2Vhdl_ModuleInstance> esrc: this.fdata.idxModules.entrySet()) {                         // all sources
      J2Vhdl_ModuleInstance moduleInstance = esrc.getValue();
      String nameModule = esrc.getKey();
      //for(JavaSrc.ClassDefinition theclass : moduleInstance.type.javaSrc.get_classDefinition()) { // get the only one public class of module
      JavaSrc.ClassDefinition theclass = moduleInstance.type.moduleClass;  
      String nameClass = theclass.get_classident();
      Iterable<JavaSrc.ClassDefinition> iclasses = theclass.get_classDefinition(); 
      if(iclasses !=null) { 
        for(JavaSrc.ClassDefinition iclass : iclasses) { // get inner class of public module class  
          String nameiClass = iclass.get_classident();
          String sAnnot = iclass.get_Annotation();
          if(sAnnot !=null && sAnnot.equals("Fpga.VHDL_PROCESS") && iclass.getSize_variableDefinition() >0
             || ( !isTopLevel && (nameiClass.equals("In") || nameiClass.equals("Out")) ) ) {
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
      this.vhdlConv.setAggregatedModules(moduleInstance.idxAggregatedModules);
      String sModule = esrc.getKey();
      JavaSrc.ClassDefinition theclass = moduleInstance.type.moduleClass;     // get the only one public class of module
        Iterable<JavaSrc.ClassDefinition> iclasses = theclass.get_classDefinition(); 
        if(iclasses !=null) for(JavaSrc.ClassDefinition iclass : iclasses) { // get inner class of public module class  
          String sAnnot = iclass.get_Annotation();
          String nameiClass = iclass.get_classident();
          if(sAnnot !=null && sAnnot.equals("Fpga.VHDL_PROCESS")) {            // it is an inner class for a VHDL RECORD and PROCESS
            //
            String namePrc = sModule  + "_" + nameiClass;                      // search that ctor of the class
            String nameInnerClassVariable = Character.toLowerCase(nameiClass.charAt(0))+ nameiClass.substring(1);
            JavaSrc.ConstructorDefinition ctor = this.vhdlConv.getCtorProcess(iclass, nameInnerClassVariable); // which is designated with @Fpga.CTOR_PROCESS
            if(ctor !=null) {
              String ctorName = ctor.get_constructor();
              if(ctorName.equals("Qrx"))
                Debugutil.stop();
              if(namePrc.equals("clr_Q"))
                Debugutil.stop();
              wOut.append("\n\n\n").append(namePrc).append("_PRC: PROCESS ( clk )");
              //String nameIclass = sModule  + "." + Character.toLowerCase(nameiClass.charAt(0)) + nameiClass.substring(1);
              String nameIclass = Character.toLowerCase(nameiClass.charAt(0)) + nameiClass.substring(1);
              this.vhdlConv.setInnerClass(nameIclass, sModule);
              this.vhdlConv.createProcessVar(wOut, ctor);                      // gather and output definition of local process variables
              wOut.append("\nBEGIN IF(clk'event AND clK='1') THEN\n");
              for(JavaSrc.Statement stmnt: ctor.get_statement()) {             // all first level statements in the ctor
                //CharSequence txt = this.vhdlConv.genStatement(stmnt, 1);
                this.vhdlConv.genStmnt(wOut, stmnt, moduleInstance, nameInnerClassVariable, 1, true);
              } 
              //
              this.vhdlConv.cleanProcessVar();
              wOut.append("\nEND IF; END PROCESS;\n");
              //
            }
        } }
      }
    
  }

  
  
  
  void genOutput(StringBuilder wOut) throws Exception {
    for(Map.Entry<String, J2Vhdl_ModuleType> esrc:  this.fdata.idxModuleTypes.entrySet()) {
      J2Vhdl_ModuleType src = esrc.getValue();
      if(src.isTopLevel()) {                                 // All toplevel variable
        J2Vhdl_ModuleInstance topInstance = src.topInstance;
        this.vhdlConv.setAggregatedModules(topInstance.idxAggregatedModules);
        this.vhdlConv.setInnerClass(src.nameType, topInstance.nameInstance);
        JavaSrc.ClassDefinition tclass = src.moduleClass;
        if(tclass.getSize_methodDefinition() >0) //...for
        for(JavaSrc.MethodDefinition oper: tclass.get_methodDefinition()) {
          if(oper.get_name().equals("update")) {
            for(JavaSrc.Statement stmnt : oper.get_methodbody().get_statement()) {
              this.vhdlConv.genStmnt(wOut, stmnt, topInstance, src.nameType, 0, false);
            }
          }
        }
      }
    }
  }

  
  void reportContentOfAll(Appendable out) throws IOException {
    StringFormatter sf = new StringFormatter(out, false, "\n", 100);
    for(Map.Entry<String, J2Vhdl_ModuleType> emdl: this.fdata.idxModuleTypes.entrySet()) {
      J2Vhdl_ModuleType mdl = emdl.getValue();
      String name = emdl.getKey();
      out.append("\n== J2Vhdl_ModuleType: ").append(name);
      out.append("\n  ifcOperation()    | access    {@link J2Vhdl_ModuleType#idxIfcOperation}");
      out.append("\n--------------------+----------------\n");
//      for(Map.Entry<String, String> eIfc : mdl.idxIfcOperation.entrySet()) {
//        String ifcName = eIfc.getKey();
//        String ifcAccess = eIfc.getValue();
//        sf.reset();
//        sf.add("  ").add(ifcName).add("()").pos(20).add("| ").add(ifcAccess);
//        sf.flushLine("\n");
//      }
      for(Map.Entry<String, J2Vhdl_ModuleType.IfcConstExpr> eIfc : mdl.idxIfcExpr.entrySet()) {
        String ifcName = eIfc.getKey();
        J2Vhdl_ModuleType.IfcConstExpr ifcAccess = eIfc.getValue();
        sf.reset();
        String sAccess;
        //VhdlExprTerm.ExprType type;
        if(ifcAccess.constVal !=null) { sAccess = ifcAccess.constVal.var.sElemVhdl; }
        else { sAccess = ifcAccess.expr.toString(); }
        sf.add("  ").add(ifcName).add("()").pos(20).add("| ").add(sAccess);
        sf.flushLine("\n");
      }
      out.append("--------------------+----------------\n");
    }

    for(Map.Entry<String, J2Vhdl_ModuleInstance> emdl: this.fdata.idxModules.entrySet()) {
      J2Vhdl_ModuleInstance mdl = emdl.getValue();
      String name = emdl.getKey();
      out.append("\n== Module: ").append(name);
      out.append("\n  localName         | accessed module     {@link J2Vhdl_ModuleInstance#idxAggregatedModules}");
      out.append("\n--------------------+----------------\n");
      for(Map.Entry<String, J2Vhdl_ModuleInstance> eIfc : mdl.idxAggregatedModules.entrySet()) {
        String innerName = eIfc.getKey();
        J2Vhdl_ModuleInstance refMdl = eIfc.getValue();
        String sType = refMdl !=null ? refMdl.type.moduleClass.get_classident(): "???refModuleNotFound";
        String sName = refMdl !=null ? refMdl.nameInstance : "not found";
        sf.reset();
        sf.add("  ").add(innerName).pos(20).add("| ").add(sName).add(" : ").add(sType);
        sf.flushLine("\n");
      }
      out.append("--------------------+----------------\n");
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
    out.append("-----------------------------------+----------------------------------------+----------------\n\n");
  }
  
  
  
}


