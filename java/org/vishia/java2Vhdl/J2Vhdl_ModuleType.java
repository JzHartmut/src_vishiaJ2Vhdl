package org.vishia.java2Vhdl;

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
  
  /**Instance of a top level module. Only for a top level ModuleType an instance is built immediately.
   * All other Module types are only existent because there is a composite reference which builds the instance,
   * and this can be more as one instances for the same type, or also the same type used in different module types as sub module.
   * Then this composite reference is null.
   */
  J2Vhdl_ModuleInstance topInstance;
  //boolean isTopLevel;

  /**Association between the name of any interface operation in this module
   * to the appropriate RECORD variable in VHDL. 
   * The value comes from the <code> return q.var;</code> in the interface operation.
   * Whereas <code>q</code> is the instance of an inner class which builds a VHDL PROCESS 
   * and <code>var</code> is a variable there. 
   */
  Map<String, String> XXXidxIfcOperation = new TreeMap<String, String>();
  
  Map<String, IfcConstExpr> idxIfcExpr = new TreeMap<String, IfcConstExpr>();
  
  /**Composite sub modules name as key and the module instance. 
   * Hint: Also stored in */
  Map<String, J2Vhdl_ModuleInstance> XXXidxSubModules = new TreeMap<String, J2Vhdl_ModuleInstance>();


  public J2Vhdl_ModuleType(String nameType, JavaSrc javaSrc, JavaSrc.ClassDefinition moduleClass, boolean isTopLevel) {
    this.nameType = nameType;
    //this.javaSrc = javaSrc;
    this.moduleClass = moduleClass;
    if(isTopLevel) {
      this.topInstance = new J2Vhdl_ModuleInstance(nameType, this, false);
    } else {
      this.topInstance = null;
    }
  }
  
  boolean isTopLevel() { return this.topInstance !=null; }
  
  @Override public String toString() { return this.nameType;  }
  

}
