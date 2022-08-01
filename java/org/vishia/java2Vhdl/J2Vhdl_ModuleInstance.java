package org.vishia.java2Vhdl;

import java.util.Map;
import java.util.TreeMap;

import org.vishia.java2Vhdl.parseJava.JavaSrc;


/**This class contains and prepares data relevant for one module instance in Java to generate VHDL.
 * Representation of a module instance of a javaSrc which is used anywhere from the top level. 
 * The index of all module instances is {@link Java2Vhdl#idxModules}.
 * @author hartmut Schorrig
 *
 */
public class J2Vhdl_ModuleInstance {
  
  /**instance name first local, then changed to the top level global name as stored in {@link J2Vhdl_FpgaData#idxModules} */
  String nameInstance;
  
  final boolean bInOutModule;
  
  final J2Vhdl_ModuleType type;
  
  /**The variable which is used to create this instance. It is null only for the top level.
   * This element should be removed if the content is used (save memory, garbage it. ). */
  JavaSrc.VariableInstance mVarInit;
  
  /**The init operation which is used to initialize this instance. May be null if the instance is create complete in the new ctor(...refs)
   * This element should be removed if the content is used (save memory, garbage it. ). */
  JavaSrc.SimpleMethodCall operInit;
  
  /**Composite sub modules name as key and the module instance. 
   * Hint: Also stored in {@link J2Vhdl_FpgaData#idxModules} for VHDL-flat usage*/
  Map<String, J2Vhdl_ModuleInstance> idxSubModules = null;


  
  /**Associations between the used internal name name as key and the aggregated module. */
  Map<String, J2Vhdl_ModuleInstance.InnerAccess> idxAggregatedModules = new TreeMap<String, J2Vhdl_ModuleInstance.InnerAccess>();

  public J2Vhdl_ModuleInstance(String nameInstance, J2Vhdl_ModuleType type, boolean bInOutModule
      , JavaSrc.VariableInstance mVarInit, JavaSrc.SimpleMethodCall operInit) {
    this.type = type;
    this.nameInstance = nameInstance;
    this.bInOutModule = bInOutModule;
    this.mVarInit = mVarInit;
    this.operInit = operInit;
  }

  @Override public String toString() { return this.nameInstance + ": "+ this.type.toString(); }
  
  
  /**Used for referenced Modules, additional with an inner access.
   *
   */
  public static class InnerAccess {
    
    final J2Vhdl_ModuleInstance mdl;
    final String sAccess;
    
    public InnerAccess(J2Vhdl_ModuleInstance mdl, String sAccess) { 
      this.mdl = mdl; this.sAccess = sAccess; 
    }
  }
}
