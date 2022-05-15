package org.vishia.java2Vhdl;

import java.util.Map;
import java.util.TreeMap;


/**This class contains and prepares data relevant for the whole FPGA to generate VHDL.
 * @author Hartmut Schorrig
 *
 */
public class J2Vhdl_FpgaData {

  
  /**This contains all Java modules which are requested either by toplevel files
   * or by internal referenced modules as sub module.
   */
  final Map<String, J2Vhdl_ModuleType> idxModuleTypes = new TreeMap<String, J2Vhdl_ModuleType>();
  
  /**This contains all module names which are requested either by Module sub classes from the top level or in other modules. */
  final Map<String, J2Vhdl_ModuleInstance> idxModules = new TreeMap<String, J2Vhdl_ModuleInstance>();
  
   
  /**All variable of all modules with its presence in VHDL. */
  TreeMap<String, J2Vhdl_Variable> idxVars = new TreeMap<String, J2Vhdl_Variable>();

  /**All members of all Record types in VHDL. */
  TreeMap<String, J2Vhdl_Variable> idxRecordVars = new TreeMap<String, J2Vhdl_Variable>();

  
  /**Local variable only of one PROCESS new filled on each process.  */
  TreeMap<String, J2Vhdl_Variable> idxProcessVars = new TreeMap<String, J2Vhdl_Variable>();

  
  /**All constant definitions of the whole VHDL design.  */
  TreeMap<String, J2Vhdl_ConstDef> idxConstDef = new TreeMap<String, J2Vhdl_ConstDef>();


  
  
  
}
