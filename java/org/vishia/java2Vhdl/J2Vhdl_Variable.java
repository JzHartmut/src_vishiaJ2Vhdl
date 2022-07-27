package org.vishia.java2Vhdl;

/**Describes a variable defined in a RECORD used as remaining variable for a PROCESS.
 * The type, search key and VHDL name is stored.
 */
public class J2Vhdl_Variable {
  /**True then it is a PROCESS variable, assigned with := */
  final boolean isLocal;  
  final String name;
  
  final int nrBits;  //TODO stored in type!
  
  final VhdlExprTerm.ExprType type;
  
  final String sElemJava;
  
  final String sElemVhdl;

  public J2Vhdl_Variable(String name, boolean isLocal, VhdlExprTerm.ExprType type, int nrBits, String sElemJava, String sElemVhdl) {
    super();
    this.name = name;
    this.isLocal = isLocal;
    this.nrBits = nrBits;
    this.type = type;
    this.sElemJava = sElemJava;
    this.sElemVhdl = sElemVhdl;
  }
  
  @Override public String toString() { return this.sElemJava; }
  
  
  
  /**Returns such as "BIT" or "STD_LOGIC_VECTOR(15 DOWNTO 0)" adequate its definition
   * @return "??TYPE ..." in not proper.
   */
  public String getVhdlType ( ) {
    final String type;
    if(this.type.etype == VhdlExprTerm.ExprTypeEnum.bittype) { type = "BIT"; }
    else if(this.type.etype == VhdlExprTerm.ExprTypeEnum.bitVtype) { type = "BIT_VECTOR(" + (this.nrBits-1) + " DOWNTO 0)"; }
    else if(this.type.etype == VhdlExprTerm.ExprTypeEnum.stdtype) { type = "STD_LOGIC"; }
    else if(this.type.etype == VhdlExprTerm.ExprTypeEnum.stdVtype) { type = "STD_LOGIC_VECTOR(" + (this.nrBits-1) + " DOWNTO 0)"; }
    else if(this.type.etype == VhdlExprTerm.ExprTypeEnum.inttype) { type = "INTEGER"; }
    else if(this.type.etype == VhdlExprTerm.ExprTypeEnum.numConst) { type = "INTEGER"; }
    else if(this.type.etype == VhdlExprTerm.ExprTypeEnum.bitStdConst) { type = "BIT"; }
    else if(this.type.etype == VhdlExprTerm.ExprTypeEnum.bitStdVconst) { type = "BIT_VECTOR(" + (this.nrBits-1) + " DOWNTO 0)"; }
    else { type = "??TYPE " + this.type.toString(); }
    return type;
  }
  

}
