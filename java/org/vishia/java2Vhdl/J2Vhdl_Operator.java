package org.vishia.java2Vhdl;

import java.util.Map;
import java.util.TreeMap;


/**Describes all operators used in VHDL and contains a Map of all operations: {@value #operatorMap}
 * @author Hartmut Schorrig
 *
 */
public class J2Vhdl_Operator {
  
  
  enum OpBool {
    forceBool(true, false, false),
    maybeBool(false, true, false),
    indepBool(false, false, false),
    isAssign(false, false, true),
    isBool(false, true, false)
    ;
    OpBool(boolean forceBool, boolean maybeBool, boolean bAssign){
      this.bForceToBool = forceBool; this.bMaybeBool = maybeBool; this.bAssign = bAssign;
    }
    /**true on compare operators which forces a boolean type. */
    final boolean bForceToBool;
    
    /**true on operators which can be executed with BIT types else also boolean AND OR XOR
     * A conversion to bool is done if the expression should be boolean because it is in an VHDL IF operation */
    final boolean bMaybeBool;
    final boolean bAssign;
  }

  
  final String sJava; 
  //final int precedJava; 
  final String sVhdlBool; 
  final String sVhdlVal; 
  final int precedVhdl;
  final boolean bAnd;
  final OpBool opBool;   //operator forces the expression to be boolean
  /**Not null, then use this operator to re-check if the type is BIT or STD_VECTOR
   * to get a BIT result (XOR, XNOR) instead ==, != */
  final String sCheckEqXor;
  final int idConv;

  public J2Vhdl_Operator(String sJava, int precedJava, String sVhdlBool, String sVhdlVal, int predecVhdl, boolean bAnd, OpBool opBool, String bCheckEqXor) {
    this(sJava, precedJava, sVhdlBool, sVhdlVal, predecVhdl, bAnd, opBool, bCheckEqXor, 0);
  } 
  
  public J2Vhdl_Operator(String sJava, int precedJava, String sVhdlBool, String sVhdlVal, int predecVhdl, boolean bAnd, OpBool opBool, String sCheckEqXor, int idConv) {
    this.sJava = sJava;
    //this.precedJava = precedJava;
    this.sVhdlBool = sVhdlBool;
    this.sVhdlVal = sVhdlVal;
    this.precedVhdl = predecVhdl;
    this.bAnd = bAnd;
    this.opBool = opBool;
    this.sCheckEqXor = sCheckEqXor;
    this.idConv = idConv;
  } 
  
  
  /**Map of all operators. Key is the Java writing text.
   * The map will be filled statically on loading the class.
   * Precedence in VHDL:
   * 1 Misc: ** abs not
   * 2 Multiplying: * / mod rem
   * 3 Sign: + -
   * 4 Adding: + - &
   * 5 Shift: sll srl sla sra rol ror
   * 6 Relational: = /= < <= > >=
   * 7 Logical: and or nand nor xor xnor
   */
  final static Map<String, J2Vhdl_Operator> operatorMap = new TreeMap<String, J2Vhdl_Operator>();
  static
  { operatorMap.put(";",   new J2Vhdl_Operator( ";",   1, "; ",   "; ",    1, false, OpBool.indepBool, null ));     // start of expression is highest precedence
    operatorMap.put("@",   new J2Vhdl_Operator( "@",  15, " @ ",  " @ ",  10, false, OpBool.indepBool, null ));     // start of expression is highest precedence
    operatorMap.put("=",   new J2Vhdl_Operator( "=",   2, " <= ", " <= ",  2, false, OpBool.isAssign, null ));
    operatorMap.put("?",   new J2Vhdl_Operator( "?",   3, " #? ", " #? ",  3, false, OpBool.indepBool, null , -1));
    operatorMap.put("||",  new J2Vhdl_Operator( "||",  4, " OR ", " OR ",  4, true,  OpBool.isBool, null ));
    operatorMap.put("&&",  new J2Vhdl_Operator( "&&",  5, " AND "," AND ", 4, true,  OpBool.isBool, null ));
    operatorMap.put("|",   new J2Vhdl_Operator( "|",   6, " OR ", " OR ",  4, true,  OpBool.maybeBool, null ));
    operatorMap.put("^",   new J2Vhdl_Operator( "^",   7, " XOR "," XOR ", 4, true,  OpBool.maybeBool, null ));
    operatorMap.put("=^",  new J2Vhdl_Operator( "^=",  7," XNOR "," XNOR ",4, true,  OpBool.maybeBool, null ));
    operatorMap.put("&",   new J2Vhdl_Operator( "&",   8, " AND "," AND ", 4, true,  OpBool.maybeBool, null ));
    operatorMap.put("==",  new J2Vhdl_Operator( "==",  9, " = "  , " = " , 5, false, OpBool.forceBool, "=^" ));
    operatorMap.put("!=",  new J2Vhdl_Operator( "!=",  9, " /= " , " /= ", 5, false, OpBool.forceBool, "^" ));
    operatorMap.put(">=",  new J2Vhdl_Operator( ">=", 10, " >= ", " >= ",  5, false, OpBool.forceBool, null ));
    operatorMap.put("<=",  new J2Vhdl_Operator( "<=", 10, " <= ", " <= ",  5, false, OpBool.forceBool, null ));
    operatorMap.put(">",   new J2Vhdl_Operator( ">",  10, " > ",  " > ",   5, false, OpBool.forceBool, null ));
    operatorMap.put("<",   new J2Vhdl_Operator( "<",  10, " < ",  " < ",   5, false, OpBool.forceBool, null ));
    operatorMap.put(">>>", new J2Vhdl_Operator( ">>>",11, " SRA "," SRA ", 6, false, OpBool.indepBool, null ));
    operatorMap.put(">>",  new J2Vhdl_Operator( ">>", 11, " SRL "," SRL ", 6, false, OpBool.indepBool, null ));
    operatorMap.put("<<",  new J2Vhdl_Operator( "<<", 11, " SLL "," SLL ", 6, false, OpBool.indepBool, null ));
    operatorMap.put("+",   new J2Vhdl_Operator( "+",  12, "?+ ",  " + ",   8, false, OpBool.indepBool, null ));
    operatorMap.put("-",   new J2Vhdl_Operator( "-",  12, "?- ",  " - ",   8, false, OpBool.indepBool, null ));
    operatorMap.put("*",   new J2Vhdl_Operator( "*",  13, "?* ",  " * ",   9, false, OpBool.indepBool, null ));
    operatorMap.put("/",   new J2Vhdl_Operator( "/",  13, "?/ ",  " / ",   9, false, OpBool.indepBool, null ));
    
  }

  
  
  
  
  @Override public String toString() { return this.sVhdlVal; }

}
