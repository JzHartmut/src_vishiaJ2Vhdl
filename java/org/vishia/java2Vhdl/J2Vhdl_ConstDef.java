package org.vishia.java2Vhdl;


public class J2Vhdl_ConstDef {
  
  
  /**The definition of a constant how to access in VHDL (adequate to a variable). */
  J2Vhdl_Variable var;
  
  /**The VHDL presentation of the value assigned to the CONSTANT definition. */
  String value;

  public J2Vhdl_ConstDef(J2Vhdl_Variable var, String value) {
    this.var = var;
    this.value = value;
  }
  
  

}
