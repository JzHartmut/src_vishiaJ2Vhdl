package org.vishia.java2Vhdl;

import java.util.Map;
import java.util.TreeMap;


/**This class contains and prepares data relevant for one module instance in Java to generate VHDL.
 * Representation of a module instance of a javaSrc which is used anywhere from the top level. 
 * The index of all module instances is {@link Java2Vhdl#idxModules}.
 * @author hartmut Schorrig
 *
 */
public class J2Vhdl_ModuleInstance {
  /**Java instance name in the class Modules */
  final String nameInstance;
  
  final boolean bInOutModule;
  
  final J2Vhdl_ModuleType type;
  
  /**Associations between the used internal name name as key and the aggregated module. */
  Map<String, J2Vhdl_ModuleInstance.InnerAccess> idxAggregatedModules = new TreeMap<String, J2Vhdl_ModuleInstance.InnerAccess>();

  public J2Vhdl_ModuleInstance(String nameInstance, J2Vhdl_ModuleType type, boolean bInOutModule) {
    this.type = type;
    this.nameInstance = nameInstance;
    this.bInOutModule = bInOutModule;
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
