package org.vishia.java2Vhdl;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.java2Vhdl.parseJava.JavaSrc;
import org.vishia.java2Vhdl.parseJava.JavaSrc.Expression;

/**This class contains and prepares data relevant for one module instance in Java to generate VHDL.
 * Representation of a module type of a javaSrc which is used anywhere as type of a module instance from the top level. 
 * The index of all module types is {@link Java2Vhdl#idxParseResult}.
 * @author hartmut Schorrig
 *
 */
public class J2Vhdl_ModuleType {
  
  /**Version, history and license.
   * <ul>
   * <li>2022-08-01 new {@link #idxSubModulesInit}. {@link #idxSubModulesVar} renamed from idxSubModules.
   *   The initialization from the instantiation (var) and from init should be adequate. 
   *   Both is now handled together in {@link Java2Vhdl#createModuleInstancesRecursively(J2Vhdl_ModuleInstance, String, int)}. 
   * <li>2022-07-30 now the topInstance is in {@link J2Vhdl_FpgaData#topInstance}
   * <li>2022-07-30 {@link #idxSubModulesVar} is now used again, it is filled per ModulType before {@link J2Vhdl_FpgaData#idxModules} is filled.
   *   This is because sub modules in modules should be given more as one, each for each module.  
   * <li>2022-07-28 Because of new {@link J2Vhdl_ModuleVhdlType} some fields are moved to the new one. 
   *   It should be near the same as the version 2 times before.
   * <li>2022-07-28 Hartmut enhanced for called included VHDL modules with {@link #inputs}, {@link #outputs} and {@link #idxIOVars}
   * <li>2022-04 created
   * </ul>
   * <br><br>
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL is not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   */
  public final static String sVersion = "2022-08-01"; 

  
  static class IfcConstExpr {
    final JavaSrc.Expression expr;
    final J2Vhdl_ConstDef constVal;
  
    public IfcConstExpr(Expression expr, J2Vhdl_ConstDef constVal) {
      this.expr = expr;
      this.constVal = constVal;
    }
  }
  
  
  /**The name of the type is the name of the parsed file.java 
   * which is identically with the name of the public class in the file due to Java conventions check by the compiler.
   * The name is unified in the given environment because of import statements in all module. 
   * This is due to the Java convention for package paths and import. 
   * Usage of a full qualified class Type in the java sources is not supported.
   * (It is the difference between: <pre>
   * import my.pkg.path.ClassType;
   * ... new ClassType(...) // not full qualified, use import
   * --------------------------- or
   * ... new my.pkg.path.ClassType(...) //full qualified, this is not supported.
   * </pre>
   * It means it is not possible to use the same Module Type name for different modules in different packages. 
   * All used Module Type names should be different in the given translation environment. 
   */
  final String nameType;
  
  //JavaSrc javaSrc;
  
  final JavaSrc.ClassDefinition moduleClass;
  
  boolean isTopLevelType;
  //boolean isTopLevel;

  
  
  
  
  
  /**Association between the name of any interface operation in this module
   * to the appropriate RECORD variable in VHDL. 
   * The value comes from the <code> return q.var;</code> in the interface operation.
   * Whereas <code>q</code> is the instance of an inner class which builds a VHDL PROCESS 
   * and <code>var</code> is a variable there. 
   */
  Map<String, String> XXXidxIfcOperation = new TreeMap<String, String>();
  
  Map<String, IfcConstExpr> idxIfcExpr = new TreeMap<String, IfcConstExpr>();
  
  /**Composite sub modules name as key and the variable with type to initialize. 
   * This is temporary till {@link J2Vhdl_ModuleInstance#idxSubModules} is created. */
  Map<String, JavaSrc.VariableInstance> idxSubModulesVar = null;

  /**Composite sub modules name as key and the init operation to initialize. 
   * This is temporary till {@link J2Vhdl_ModuleInstance#idxSubModules} is created. */
  Map<String, JavaSrc.SimpleMethodCall> idxSubModulesInit = null;

  
  

  public J2Vhdl_ModuleType(String nameType, JavaSrc javaSrc, JavaSrc.ClassDefinition moduleClass, boolean isTopLevel) {
    this.nameType = nameType;
    //this.javaSrc = javaSrc;
    this.moduleClass = moduleClass;
    this.isTopLevelType = isTopLevel;
  }
  
  boolean isTopLevel() { return this.isTopLevelType; }
  
  
  
  @Override public String toString() { return this.nameType;  }
  

}
