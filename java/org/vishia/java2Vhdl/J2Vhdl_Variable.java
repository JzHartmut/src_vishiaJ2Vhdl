package org.vishia.java2Vhdl;

/**Describes a variable defined in a RECORD used as remaining variable for a PROCESS.
 * The type, search key and VHDL name is stored.
 */
public class J2Vhdl_Variable {
  

  
  /**Version, history and license.
   * <ul>
   * <li>2022-07-28 instead bLocal now a better Location enum created.
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
  public final static String sVersion = "2022-07-28"; 


  
  
  /**The variable is either part of Input or Output of the VHDL module,
   * or it is a RECORD element, or local in a PROCESS
   * @author hartmut
   *
   */
  enum Location {
    record("REC")
    , local("LOCAL")
    , input("IN")
    , output("OUT")
    , inout("INOUT")
    ;
    final String s;

    private Location(String s) {
      this.s = s;
    }
    
  }
  
  
  
  /**True then it is a PROCESS variable, assigned with := */
  //final boolean isLocal;  
  
  /**Where to find the variable. */
  Location location;
  
  final String name;
  
  final int nrBits;  //TODO stored in type!
  
  /**Type of the variable in VHDL */
  final VhdlExprTerm.ExprType type;
  
  final String sElemJava;
  
  /**Qualified name in VHDL */
  final String sElemVhdl;

  public J2Vhdl_Variable(String name, Location location, VhdlExprTerm.ExprType type, int nrBits, String sElemJava, String sElemVhdl) {
    super();
    this.name = name;
    this.location = location;
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
  

  J2Vhdl_Variable.Location getLocation ( ) { return this.location; }
  
  
}
