package org.vishia.java2Vhdl;

import java.util.List;
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
  
  /**All Processes with clarified ce() access which builds Time Groups
   * The key is the name of the time group, mdlName * "_" + timeGroupName ()
   * The value is a list of the names of all SIGNAL instances for Processes.
   */
  final Map<String, List<String>> idxTimeGroups = new TreeMap<String, List<String>>();
   
  /**All variable of all modules with its presence in VHDL. */
  TreeMap<String, J2Vhdl_Variable> idxVars = new TreeMap<String, J2Vhdl_Variable>();

  /**All members of all Record types in VHDL. */
  TreeMap<String, J2Vhdl_Variable> idxRecordVars = new TreeMap<String, J2Vhdl_Variable>();

  
  /**Local variable only of one PROCESS new filled on each process.  */
  TreeMap<String, J2Vhdl_Variable> idxProcessVars = new TreeMap<String, J2Vhdl_Variable>();

  
  /**All enum definitions of the whole VHDL design.  */
  TreeMap<String, String> idxEnumDef = new TreeMap<String, String>();

  /**All bit values of enum values of the whole VHDL design.  */
  TreeMap<String, String> idxEnumBitDef = new TreeMap<String, String>();

  /**All constant definitions of the whole VHDL design.  */
  TreeMap<String, J2Vhdl_ConstDef> idxConstDef = new TreeMap<String, J2Vhdl_ConstDef>();


  /**Instance of a top level module. Only for a top level ModuleType an instance is built immediately.
   * All other Module types are only existent because there is a composite reference which builds the instance,
   * and this can be more as one instances for the same type, or also the same type used in different module types as sub module.
   * Then this composite reference is null.
   */
  J2Vhdl_ModuleInstance topInstance;

  VhdlExprTerm.ExprType typeStdLogic = new VhdlExprTerm.ExprType(VhdlExprTerm.ExprTypeEnum.stdtype, 0);

  VhdlExprTerm.ExprType typeBit = new VhdlExprTerm.ExprType(VhdlExprTerm.ExprTypeEnum.bittype, 0);

  J2Vhdl_Variable varClk = new J2Vhdl_Variable("clk", J2Vhdl_Variable.Location.input, this.typeBit, 0, "clk", "clk");
  
  
}
