package org.vishia.java2Vhdl;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.vishia.genJavaOutClass.SrcInfo;
import org.vishia.java2Vhdl.parseJava.JavaSrc;
import org.vishia.util.Debugutil;
import org.vishia.util.StringFunctions;

/**Helds info for the current Expression term.
 */
public final class VhdlExprTerm extends SrcInfo {

  /**Version, history and license.
   * <ul>
   * <li>2022-08-20  {@link #genSimpleValue(org.vishia.java2Vhdl.parseJava.JavaSrc.SimpleValue, boolean, J2Vhdl_ModuleInstance, String, CharSequence, boolean)}:
   *   if an expression part is "time", it is the time argument of step, then it is a timing assignment. Ignore it.
   *   This was only peculiar on a term which has "time_" left side, not detected but "time" in the expression.
   * <li>2022-08-20 bugfix regard unary operator for a nested expression, "NOT state == StateXY" or "NOT(a AND b)" 
   * <li>2022-10-15 chg {@link #addOperand(VhdlExprTerm, J2Vhdl_Operator, org.vishia.java2Vhdl.parseJava.JavaSrc.ExprPart, boolean, J2Vhdl_ModuleInstance, String)}:
   *   The type for state comparison is now bittype, without conversion to boolean.
   *   With them the VHDL is simplified if a result is also a bit. 
   *   Fixed error "non proper types in expression," because faulty type of state cmp expression, now correct. 
   * <li>2022-08-07 new, {@link RefModuleInfo} separated {@link #getReferencedModule(org.vishia.java2Vhdl.parseJava.JavaSrc.SimpleValue, J2Vhdl_ModuleInstance, String)}
   *   should be used (todo) for intermediate references in a ctor. Especially for VhdlMdl on (at)Fpga.LINK_VHDL_MODULE 
   * <li>2022-08-06 {@link #genSimpleValue(org.vishia.java2Vhdl.parseJava.JavaSrc.SimpleValue, boolean, J2Vhdl_ModuleInstance, String, CharSequence, boolean)}
   *   now prepared to use instead {@link VhdlConv#XXXgetVariableAccess(org.vishia.java2Vhdl.parseJava.JavaSrc.SimpleValue, J2Vhdl_ModuleInstance, String).}
   *   The preparation is ok, the replacement is not tested yet.
   * <li>2022-08-06 new {@link #sTypeConversions}, used in new {@link #adjustType(StringBuilder, StringBuilder, ExprType, ExprType)}, 
   *    called in {@link #addOperand(VhdlExprTerm, J2Vhdl_Operator, org.vishia.java2Vhdl.parseJava.JavaSrc.ExprPart, boolean, J2Vhdl_ModuleInstance, String)}
   *    and in {@link J2Vhdl_GenExpr#genTrueFalse(Appendable, ExprType, CharSequence, org.vishia.java2Vhdl.parseJava.JavaSrc.ExprPart, J2Vhdl_ModuleInstance, String, boolean, CharSequence, CharSequence)}.
   *    The problem is: nested trueFalseValues. It is necessary to offer the really left type to all nesting levels.
   *    For that new argument 'assignType' for {@link J2Vhdl_GenExpr#genExpression(Appendable, org.vishia.java2Vhdl.parseJava.JavaSrc.Expression, boolean, boolean, J2Vhdl_ModuleInstance, String, CharSequence, CharSequence, ExprType)}.  
   * <li>2022-07-28 in {@link #getVariableAccess(org.vishia.java2Vhdl.parseJava.JavaSrc.SimpleVariable, J2Vhdl_ModuleInstance, String)}:
   *   Access to {@link J2Vhdl_ModuleVhdlType#idxIOVars}  
   * <li>2022-07-28 Access to Fpga.clk as fix dirty solution.
   * <li>2022-07-28 now beside "mdl" now also "thism" for access to the module class (environment class) 
   * <li>2022-07-25 in {@link #genSimpleValue(org.vishia.java2Vhdl.parseJava.JavaSrc.SimpleValue, boolean, J2Vhdl_ModuleInstance, String, CharSequence)}:
   *   "modules" (firstly "smd") as identification for sub module accepted.
   * <li>2022-07-25 in {@link #genSimpleValue(org.vishia.java2Vhdl.parseJava.JavaSrc.SimpleValue, boolean, J2Vhdl_ModuleInstance, String, CharSequence)}:
   *   if 'bTimeMaskVar' then do not search the (non existing) variable, prevent error message, tested on SPE-FPGA.
   *   The statements are commented yet, they were introduced only for tests. 
   * <li>2022-07-17 in  {@link #addOperand(VhdlExprTerm, J2Vhdl_Operator, org.vishia.java2Vhdl.parseJava.JavaSrc.ExprPart, boolean, J2Vhdl_ModuleInstance, String)}:
   *   Detection whether the rightExpr supplies a {@link VhdlExprTerm.ExprTypeEnum#stateBit} with a == operator. 
   *   Then it produces a simple access to the proper state variable bit. Usefull for 1 to n decoded states.
   * <li>2022-07-17 in {@link #genSimpleValue(org.vishia.java2Vhdl.parseJava.JavaSrc.SimpleValue, boolean, J2Vhdl_ModuleInstance, String, CharSequence)}:
   *   Now longer references are possible as only three stages. With them an access in internal coding of a enum state is possible. 
   * <li>2022-07-17 in {@link #getVariableAccess(org.vishia.java2Vhdl.parseJava.JavaSrc.SimpleVariable, J2Vhdl_ModuleInstance, String):
   *   If the variable is not found and its name is _val_ then the path is shorten to the reference only and try to get. 
   *   This is an access to inner values of enums. 
   * <li>2022-07-06 conversion routines on AND, OR etc. with mix logic (BIT, STD_LOGIC).
   * <li>2022-07-06 Now supports char as STD_LOGIC. 
   *   <ul>{@link #genExprPartValue(org.vishia.java2Vhdl.parseJava.JavaSrc.SimpleValue, boolean, J2Vhdl_ModuleInstance, String)} renamed
   *     from genExprPart ( ) before, because it uses only the value in a part. Better documented.
   *   <li>{@link #genSimpleValue(org.vishia.java2Vhdl.parseJava.JavaSrc.SimpleValue, boolean, J2Vhdl_ModuleInstance, String, CharSequence)}:
   *     regards the simpleCharLiteral in a simple value. 
   *   </ul>
   * <li>2022-04-29 Hartmut chg in genSimpleValue(...) writes 0 if 0 was parsed, mistake if 0x0 was written. This is todo.
   * <li>2022-04-29 Hartmut chg in {@link #genSimpleValue(org.vishia.parseJava.JavaSrc.SimpleValue, boolean, CharSequence)}:
   *   now uses the new ZbnfParser feature and writes the parsed Source for hexa numbers instead the value
   *   in form x"0123" instead "100100011" as before from the number,
   *   hence the correct number of digits is known. For bit values with "0b010101" it was done before 
   *   because this format was never converted to a number.
   * <li>2022-04-29 Hartmut new {@link ExprType} is now a class with enum type and number of bits
   *   Can be used to detect necessary completion with "000... on vectors for correct size, but not done yet.
   *   It is an effort for all usages, but may be proper.    
   * <li>2022-02-08 Hartmut created, improved for fist usage.
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
  public final static String sVersion = "2023-03-28"; 

  
  /**Type of a variable and a build expression.
   */
  enum ExprTypeEnum {
      /**Expression without value. */
      undef       (0, 0,0,0)
    , bittype     (1, 0,0,1)
    , stdtype     (2, 0,0,1)
    , bitStdConst (3, 0,1,0)   //'1'
    , bitVtype    (4, 1,0,0)
    , stdVtype    (5, 1,0,0)
    , bitStdVconst(6, 1,1,0)   //"1100"
    , stateBit    (7, 0, 1,0)
    , booltype    (8, 0,0,1)
    , boolUncompleteType(9, 0,0,1)
    , inttype     (10, 1,0,0)
    , uinttype    (11, 1,0,0)
    , numConst    (12, 0,1,0),
      /**A time variable in Java, not used for VHDL, internally for timing check. */
      timeVar     (13, 0,0,0),
      /**A mask variable in Java, not used for VHDL, to mask a longer vector. */
      maskVar     (14, 0,0,0)  
    ;
    final boolean bVector, bConst, bIsOrCanConvertToBool;
    final int ix;
    ExprTypeEnum(int ix, int bVector, int bConst, int bCanConvertToBool){ 
      this.ix = ix;
      this.bVector = bVector !=0; this.bConst = bConst !=0; this.bIsOrCanConvertToBool = bCanConvertToBool !=0; 
    }
  }
  
  /**Array of conversion between types. 
   * Columns are adequate to the lines. fromUndef, frombitType etc.
   * % is the placeholder for the original expression.
   */
  static final String[] convToUndef       = {null, null, null, null,null, null, null, null,null, null, null, null, null};
  static final String[] convTobitType     = {null, null, "TO_BIT(%)", null,"?bitv2bit", "?stdv2bit", "?constv2bit", "?state2bit","?bool2bit", "?ubool2bit", "?int2bit", "uint2bit", "?numConst2bit"};
  static final String[] convTostdType     = {null, "TO_STDULOGIC(%)", null, null, "?bitv2std", "?stdv2std", "?constv2std", "?state2std","?bool2std", "?ubool2std", "?int2std", "uint2std", "?numConst2std"};
  static final String[] convTobitstdConst = {null, null, null, null,null, null, null, null,null, null, null, null, null};
  static final String[] convTobitVtype    = {null, null, null, null,null, "TO_BITVECTOR(%)", null, null,null, null, null, null, null};
  static final String[] convTostdVtype    = {null, null, null, null,"TO_STDLOGICVECTOR(%)", null, null, null,null, null, null, null, null};
  static final String[] convTobitStdVconst= {null, null, null, null,null, null, null, null,null, null, null, null, null};
  static final String[] convTostateBit    = {null, null, null, null,null, null, null, null,null, null, null, null, null};
  static final String[] convToboolType    = {null, "(%)='1'", "(%)='1'", null,null, null, null, null,null, null, null, null, null};
  static final String[] convToboolUncompl = {null, null, null, null,null, null, null, null,null, null, null, null, null};
  static final String[] convTointtype     = {null, null, null, null,null, null, null, null,null, null, null, null, null};
  static final String[] convTouinttype    = {null, null, null, null,null, null, null, null,null, null, null, null, null};
  static final String[] convTonumConst    = {null, null, null, null,null, null, null, null,null, null, null, null, null};

  /**Array of all conversions. null: No conversion necessary.
   * All conversions which starts with "?" are faulty. They should be prevented already by compiler tested Java code.
   * Occurrence is a hint to an internal error. 
   * 
   */
  static final String[][] sTypeConversions = 
  { convToUndef       
  , convTobitType     
  , convTostdType     
  , convTobitstdConst 
  , convTobitVtype    
  , convTostdVtype    
  , convTobitStdVconst
  , convTostateBit    
  , convToboolType    
  , convToboolUncompl 
  , convTointtype     
  , convTouinttype    
  , convTonumConst    
  };


  static class ExprType {
    VhdlExprTerm.ExprTypeEnum etype;
    
    int nrofElements;
    
    void set(ExprType src) {
      this.etype = src.etype;
      this.nrofElements = src.nrofElements;
    }

    
    
    public ExprType() {
    }



    public ExprType(ExprTypeEnum etype, int nrofElements) {
      this.etype = etype;
      this.nrofElements = nrofElements;
    }

    @Override public String toString() { return this.etype.toString() + (this.nrofElements <=1 ? "" : Integer.toString(this.nrofElements)); }
  }


  
  
  /**Necessary for getVariable, variables are property of the whole VHDL file.
   * 
   */
  //final VhdlConv vhdlConv;
  
  /**term in VHDL till now generated. */
  public final StringBuilder b; 

  
  /**If >0 then one unary is stored before the only one operand.
   * 
   */
  int posAfterUnary = 0;

  /**True then the last expression part should be used as not for boolean.
   * 
   */
  boolean bNot;

  /**The operator with its precedence of this expression. Using the lowest operator. */
  J2Vhdl_Operator precedSegm; 

  /**Expression type of this term.  */
  final ExprType exprType_ = new ExprType();

  /**Associated variable if only simple term. Especially for assignment.
   */
  J2Vhdl_Variable varCurrent_; 

  /**Number of operands for this expression. */
  int nrOperands;
  //

  /**Creates an empty bowl for a term (some parts of an expression.  */
  public VhdlExprTerm(J2Vhdl_GenExpr vhdlConv) {
    super();
    //VhdlConv.d = vhdlConv;
    this.b = new StringBuilder(100);
    this.precedSegm = J2Vhdl_Operator.operatorMap.get("@");
    this.exprType_.etype = VhdlExprTerm.ExprTypeEnum.undef;
    this.exprType_.nrofElements = 1;
  }


  /**Creates and fills a bowl for a term with given precedence and type.
   * @param b
   * @param precedSegm
   * @param nrOperands
   */
  public VhdlExprTerm(StringBuilder b, J2Vhdl_Operator precedSegm, ExprType type, int nrOperands, J2Vhdl_GenExpr vhdlConv) {
    super();
    //VhdlConv.d = vhdlConv;
    this.b = b;
    this.precedSegm = precedSegm;
    this.exprType_.set(type);
    this.nrOperands = nrOperands;
  }

  void setVariable (J2Vhdl_Variable var ){
    this.varCurrent_ = var;
    //this.exprType_ = var.type;
  }

  void removeVariable ( ) { this.varCurrent_ = null; }

  J2Vhdl_Variable variable() { return this.varCurrent_;}


  void convertToBool ( ) {
    String sCmp = "='1'";
    if(this.nrOperands >1) { this.b.insert(0, "(").append(")"); }
    else if(this.posAfterUnary >0) {
      sCmp = "='0'";
      this.b.delete(0, this.posAfterUnary);
    }
    else if(this.bNot) {
      sCmp = "='0'";
      this.bNot = false;
    } 
    else {
      int posAfterNot = StringFunctions.startsWithAfterAnyChar(this.b, "NOT", " ");
      if(posAfterNot >0) {
        sCmp = "='0'";
        this.b.delete(0, posAfterNot);
      }
    }
    this.b.append(sCmp);
    this.exprType_.etype = VhdlExprTerm.ExprTypeEnum.booltype;
    this.exprType_.nrofElements = 1;
  }

  
  /**appends = '0' or = '1'
   * @param out
   * @param val
   * @throws IOException
   */
  private void convBoolExpr(Appendable out, JavaSrc.SimpleValue val) throws IOException {
      String uOp = val.get_unaryOperator();
      if(uOp !=null && uOp.equals("!")) {
        out.append(" = '0'");
      } else {
        out.append(" = '1'");          // boolean in VHDL is only with comparison = '1'
      }
  
  }




  void fulfillNeedBool ( boolean bNeedBool) {
    if(  bNeedBool
      && this.exprType_.etype != VhdlExprTerm.ExprTypeEnum.booltype
      ) {  // bForceToBool is on compare operators. 
      if(this.nrOperands >1) {
        this.b.insert(0, '(').append(')');
      }
      if(this.bNot) { this.b.append("='0'"); }     // change to a boolean expression because necessary in context.
      else { this.b.append("='1'"); }
      //
      //this.bNeedBool = false;          //it is done
      this.bNot = false;
      this.exprType_.etype = VhdlExprTerm.ExprTypeEnum.booltype;
      this.exprType_.nrofElements = 1;
    }

  }



  /**Adds an operator and operand to the expression.
   * @param exprRightArg maybe null, or given, the right part of expression
   * @param opPreced the operator
   * @param part if exprRightArg not given, read from part from parser result
   * @param genBool true then a boolean expression should be generated (used in IF etc.)
   * @param mdl The module
   * @param nameInnerClassVariable
   * @return true: ok, false: do not use this expression, it is not for VHDL 
   * @throws Exception
   */
  public boolean addOperand ( VhdlExprTerm exprRightArg, J2Vhdl_Operator opPreced, JavaSrc.ExprPart part
      , boolean genBool, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable, boolean dbgStop) throws Exception {
    //
    if( ! super.containsInfo()) { 
      super.setSrcInfo(part);                              // store the source info from the first part
    }
    if(dbgStop)
      Debugutil.stop();
    int posEnd = this.b.length();
    final VhdlExprTerm exprRight;
    //
    if(this.b.length() ==0) {                         // add part to the empty this ExprPart, for the first part of a term 
      //======>>>>>>
      if(!addPartValue(part.get_value(), false, mdl, nameInnerClassVariable, dbgStop)) {    // false if the operand is not valid, a mask or time 
        this.b.setLength(posEnd);                          // then remove the operator also again, 
        return false;                                      // faulty variable, especially mask, or time.
      }
      exprRight = null;      
    }
    else if(exprRightArg !=null) {                              // exprRightArg is than given if it comes from the operand precedence. It is calculated independent. 
      exprRight = exprRightArg;                             // use given exprRight, maybe a more complex term
    }
    else if(opPreced.sJava.equals("@")) {                  // it is an unary operation with the left value
      String sUnaryOp = part.get_unaryOperator();
      if(sUnaryOp ==null) { 
        J2Vhdl_GenExpr.vhdlError("RPN @ @ without unary operator: ", part); 
      } else {
        J2Vhdl_Operator unaryOp = J2Vhdl_Operator.operatorUnaryMap.get(sUnaryOp);
        if(unaryOp !=null) {
          this.b.insert(0, "(").insert(0,unaryOp.sVhdlBool).append(")");
        } else {
          J2Vhdl_GenExpr.vhdlError("faulty unary operator: " + sUnaryOp, part);
        }
      }
      exprRight = null;
    }
    else {                                                 // prepare the exprRight from part to add to this.
      // in boolean expressions it is often better to write immediately "variable = '1'" from any element for maybe boolean operations.
      final boolean bNeedBoolRight = opPreced.opBool.bMaybeBool   //operator is possible a boolean operator 
        && this.exprType_.etype.bIsOrCanConvertToBool                 //The left expression is convertible to boolean
        && (genBool || this.exprType_.etype == VhdlExprTerm.ExprTypeEnum.booltype);  //it is a boolean expression, or left is already a boolean type
        // then the right expression should write as boolean operand.
      //======>>>>>>>                                      // should be independently prepared, to adapt some conversions etc.
      exprRight = genExprPartValue(part.get_value(), opPreced, false/*bNeedBoolRight*/, mdl, nameInnerClassVariable, dbgStop);
      if(exprRight == null) {
        this.b.setLength(posEnd);                          // then remove the operator also again, 
        return true;                                      // faulty variable, especially mask, or time.
      }
      if( bNeedBoolRight) {
        exprRight.fulfillNeedBool(true);
      }
//      J2Vhdl_Variable varRight = exprRight.variable();
    }
    if(exprRight !=null && exprRight.exprType_.etype != ExprTypeEnum.timeVar && exprRight.exprType_.etype != ExprTypeEnum.maskVar) {
      //
      if(exprRight.exprType_.etype == VhdlExprTerm.ExprTypeEnum.stateBit) {
        if(exprRight.bNot) {
          assert(opPreced.sJava.equals("!="));
          this.b.insert(0, "NOT ");
          this.b.append("(").append(exprRight.b).append(")");  // This is only a simple access to the proper bit of the state variable vector.
        } else {
          assert(opPreced.sJava.equals("=="));
          this.b.append("(").append(exprRight.b).append(")");  // This is only a simple access to the proper bit of the state variable vector.
        }
        this.exprType_.nrofElements = 0;
        assert(this.exprType_.etype == VhdlExprTerm.ExprTypeEnum.bitVtype);
        this.exprType_.etype = VhdlExprTerm.ExprTypeEnum.bittype;   // a selected state by == ..State is always a simple BIT because the state vector is a BIT_VECTOR
      } else {
        if (exprRight.exprType_.etype == VhdlExprTerm.ExprTypeEnum.booltype) {
          fulfillNeedBool(true);                           // convert the left side also to boolean
        }
        this.exprLeftAppendOperator(opPreced, false);      // add operator to the expression term 
        //
        //------------------------------------------       // adjust the right type to the left one
        if(!adjustType(this.b, exprRight.b, this.exprType_, exprRight.exprType_)) {
          J2Vhdl_GenExpr.vhdlError("non proper types in expression, ", part);
        }
        //set the expression type resulting of the operator:
        if(opPreced.opBool.bForceToBool ) { 
          this.exprType_.etype = VhdlExprTerm.ExprTypeEnum.booltype;
          this.exprType_.nrofElements = 1;
        }
      }
//      final boolean bNeedBoolLeft = (exprRight.exprType_.etype == VhdlExprTerm.ExprTypeEnum.booltype)
//      ; //|| opPreced.opBool.bMaybeBool && genBool;              // The operator supports bool and bool is requested
//      //convert left expr to bool only if right expr is already bool. 
//      if(  !bNeedBoolLeft                                  // if bNeedBool, then conversion to boolean is anyway done in exprLeftAppendOperator in immediately following block
//        && !exprRight.exprType_.etype.bConst                 // typical case, right constant, should be proper, tested in VHDL
//        && !exprRight.exprType_.etype.bVector                // vector types should be clarified in source, tested in VHDL
//        && this.exprType_.etype != exprRight.exprType_.etype // the interest case is BIT vs. STD_LOGIC should be handled.
//        ) {
//        if( this.exprType_.etype == VhdlExprTerm.ExprTypeEnum.bittype) {
//          if( exprRight.exprType_.etype == VhdlExprTerm.ExprTypeEnum.stdtype) {
//            exprRight.b.insert(0, "TO_BIT(").append(")");    // use that type, which is given from left, either BIT or STD_LOGIC
//          }
//        } 
//        else if( this.exprType_.etype == VhdlExprTerm.ExprTypeEnum.stdtype) {
//          if( exprRight.exprType_.etype == VhdlExprTerm.ExprTypeEnum.bittype) {
//            exprRight.b.insert(0, "TO_STDULOGIC(").append(")");  //TP_STD_LOGIC does not exists, only STDULOGIC, should be work always.
//          }
//        }
//      }
//      //    
//      if(exprRight.exprType_.etype == VhdlExprTerm.ExprTypeEnum.stateBit) {
//        assert(opPreced.sJava.equals("=="));
//        this.b.append("(").append(exprRight.b).append(") = '1'");  // This is only a simple access to the proper bit of the state variable vector.
//        this.exprType_.etype = VhdlExprTerm.ExprTypeEnum.booltype;  //because it is a bit comparison instead state comparison
//      }
//      else {
//        if(posEnd >0) {                                        // empty on first left operand. 
//          this.exprLeftAppendOperator(opPreced, bNeedBoolLeft);// add operator to the expression term, maybe first left side convert to bool. 
//        } else {
//          if(!opPreced.sJava.equals("@")) {
//            System.err.println("exprLeftAddOperand: Start expression faulty");
//          }
//          assert(opPreced.sJava.equals("@"));                  // the first for empty term is always the @ operator (set accu)
//        }
//        //-------------------------------------------------    // needBool for the next following operator:
//        this.b.append(exprRight.b).append(' ');              // "+ @" right side expression used, it is the before prepared one.
//      }
    } //if exprRight !=null
    this.nrOperands +=1;

    return true;
  }



  /**Adjusts the type while copying the content from src to dst with type adjustification.
   * This operation is also used in {@link J2Vhdl_GenExpr#genTrueFalse(Appendable, ExprType, CharSequence, org.vishia.java2Vhdl.parseJava.JavaSrc.ExprPart, J2Vhdl_ModuleInstance, String, boolean, CharSequence, CharSequence)}.
   *  
   * @param dst can be the internal {@link #b} of this. 
   * @param src can be also the internal #b of the right value
   * @param typeDst necessary type, determined by the left value
   * @param typeSrc given type, determined by the right value
   * @return
   */
  static boolean adjustType ( StringBuilder dst, Appendable src, ExprType typeDst, ExprType typeSrc){
    boolean bOk = true;
    int ixTypeLeft = typeDst.etype.ix;
    int ixTypeRight = typeSrc.etype.ix;
    String sTypeConv = sTypeConversions[ixTypeLeft][ixTypeRight];
    if(sTypeConv !=null) {
      if(ixTypeLeft == ExprTypeEnum.booltype.ix) {
        Debugutil.stop();
      }
      if(sTypeConv.charAt(0)=='?') {                   // error in type conversion:
        bOk = false;
      }
      int posRepl = sTypeConv.indexOf('%');
      if(posRepl <0) {
        dst.append(sTypeConv).append(src);
      } else {                                         // append TYPE_CONV(exprRight)
        dst.append(sTypeConv.substring(0, posRepl)).append(src).append(sTypeConv.substring(posRepl+1)).append(' ');
      }
    } else {
      dst.append(src).append(' ');          // append exprRight
    }
    return bOk;
  }


  
  
  private void exprLeftAppendOperator ( J2Vhdl_Operator opPreced, boolean bNeedBool ) {
    if(opPreced.precedVhdl > this.precedSegm.precedVhdl) {    // ensure higher precedence of current part in infix form
      this.b.insert(0, " (").append(") ");
    } else {
      this.precedSegm = opPreced;                             // The lowest precedence of this segment of expression
    }
    if(! opPreced.opBool.bForceToBool) {  // bForceToBool is on compare operators. 
      this.fulfillNeedBool(bNeedBool);
    }
    //
    String sOpVhdl = this.exprType_.etype == VhdlExprTerm.ExprTypeEnum.bittype ? opPreced.sVhdlBool : opPreced.sVhdlVal;
    if(!opPreced.opBool.bAssign) {  
      this.b.append(sOpVhdl);
    }

  }



  public static VhdlExprTerm genExprPartValue (JavaSrc.SimpleValue val, J2Vhdl_Operator op, boolean needBool
    , J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable, boolean dbgStop) 
      throws Exception {
    VhdlExprTerm thiz = new VhdlExprTerm(J2Vhdl_GenExpr.d);
    thiz.precedSegm = op;
    boolean bOk = thiz.addPartValue(val, needBool, mdl, nameInnerClassVariable, dbgStop);
    return bOk ? thiz : null;
  }
  
  
  
  
  /**Writes the value of an ExprPart to the term
   * @param val The simpleValue to add
   * @param needBool If the value must be bool, then a not can be better done with ='0'
   * @param mdl The module from where the val was gotten (whereby val may have additionally inner references) 
   * @param nameInnerClassVariable inner class of mdl for val
   * @param dbgStop only for internal debugging, checked and set on call in 
   * @return true if ok
   * @throws Exception
   */
  private boolean addPartValue (JavaSrc.SimpleValue val, boolean needBool
      , J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable
      , boolean dbgStop) 
      throws Exception {
    String sUnaryOp = val.get_unaryOperator();             // unary operator
    if(sUnaryOp !=null && !needBool) {
      J2Vhdl_Operator unaryOp = J2Vhdl_Operator.operatorUnaryMap.get(sUnaryOp);
      if(unaryOp !=null) {
  //      if(sUnaryOp.equals("!")) { sUnaryOp = " NOT "; }     // logical NOT
  //      else if(sUnaryOp.equals("~")) { sUnaryOp = " NOT "; }// bitwise NOT
  //      else;                                                // unary  + or - remains.
        this.b.append(unaryOp.sVhdlBool);
        this.posAfterUnary = this.b.length();
      } else {
        J2Vhdl_GenExpr.vhdlError("faulty unary operator: " + sUnaryOp, val);
      }
    }
    //======>>>>>>>>>>>>>>>
    boolean bOk = true;
    try {
      genSimpleValue(val, false, mdl, nameInnerClassVariable, null, dbgStop);
      
      if(needBool) { // && this.exprType_.etype != VhdlExprTerm.ExprTypeEnum.booltype) {
        assert(false);  //no more used
        if(sUnaryOp !=null) {
          if(sUnaryOp.equals("!") || sUnaryOp.equals("~")) {
            this.b.append("='0'");
          } else {
            this.b.append("='0'??").append( sUnaryOp ).append("??") ;
          }
        } else {
          this.b.append("='1'");
        }
        this.exprType_.etype = VhdlExprTerm.ExprTypeEnum.booltype;
        this.exprType_.nrofElements = 1;
      } //-------------------------------------- // now the value or variable is written in the buffer
    } catch(Throwable exc) {
      J2Vhdl_GenExpr.vhdlError("XException: " + exc.getMessage(), val);
      return false;
    }
    return bOk;
  }


  static class RefModuleInfo {
    
    J2Vhdl_ModuleInstance mdlRef = null;                 // Generally use mdlRef, maybe other referenced module
    String sNameIclass = null;
    String sRef = null;                                    // String which is used to find the correct variables
    String sNameRefIfcAccess = null;
    boolean bRefIclass = false;                            // true then iClass is set per reference
    boolean bReferencedModule = false;
    boolean bRefToType = false;
    boolean bVarUsedForReference = false;
  
    void getReference ( JavaSrc.SimpleValue val, J2Vhdl_ModuleInstance mdlArg, String nameIclassArg) {
      JavaSrc.Reference ref = val.get_reference();
      this.mdlRef = mdlArg;
      while(ref !=null) {
        boolean bIsThis = ref.get_isThis()!=null;
        JavaSrc.SimpleVariable var = ref.get_referenceAssociation();
        JavaSrc.Reference refNext = ref.get_reference();
        JavaSrc.SimpleVariable varNext = refNext == null ? null : refNext.get_referenceAssociation();
        this.sRef = var == null ? null : var.get_variableName(); 
        final String sRefNext;
        final boolean bMaybeVarUsedForReference;;
        if(varNext == null){
          JavaSrc.SimpleVariable varval = val.get_simpleVariable();
          sRefNext = varval == null ? null : varval.get_variableName();   //may be used to get a reference itself from the SimpleValue
          bMaybeVarUsedForReference = true;
        } else { 
          sRefNext = varNext.get_variableName(); 
          bMaybeVarUsedForReference = false;
        } 
        boolean bRefNextUsed = false;
        
        if(var ==null && !bIsThis) { 
          J2Vhdl_GenExpr.vhdlError("only a reference with variable is supported", ref);
          throw new IllegalArgumentException("genExpression");
        }
        //
        if(bIsThis) {                          // iclass before this is only the enclosing class name, remove it. 
          if(this.bRefIclass) {
            if(nameIclassArg == null || nameIclassArg.length()==0) {
              
            } else {
              Debugutil.stop();
            }
          }
          this.sNameIclass = nameIclassArg;         
          this.bReferencedModule = true;
        } else if(this.sRef ==null) {          //do nothing if sRef is not given (maybe only for bIsThis)
        } else if(this.sRef.equals("z")) {
          this.sNameIclass = nameIclassArg;
          this.bReferencedModule = true;
        } else if(this.sRef.equals("mdl") || this.sRef.equals("thism")) {  //the own module
          this.sNameIclass = null;             // maybe null if operation of the module is called.
          this.bReferencedModule = true;
          //bRefNextUsed = true;
        } else if(this.sRef.equals("vhdlMdl")) {           // the associated VHDL external module to a LINK_VHDL_MODULE sub class
          this.mdlRef = this.mdlRef.idxSubModules.get(nameIclassArg.substring(8));  //without name part Vhdlink_, it is tested and asserted before.
          this.sNameIclass = null;                         // maybe null if operation of the module is called.
          this.bReferencedModule = true;
          //bRefNextUsed = true;
        } else if(this.sRef.equals("ref")) {                      // get the referenced module, and maybe an inner sAccess
          J2Vhdl_ModuleInstance.InnerAccess mdlRef2 = this.mdlRef.idxAggregatedModules.get(sRefNext);
          if(mdlRef2 == null) {
            J2Vhdl_GenExpr.vhdlError("In VhdlExpTerm.genSimpleValue - Reference not found: " + sRefNext + " searched in: " + this.mdlRef.nameInstance , ref);
          } else {
            this.bVarUsedForReference = bMaybeVarUsedForReference;
            this.mdlRef = mdlRef2.mdl;
            this.sNameRefIfcAccess = mdlRef2.sAccess;             // set if a interface agent is used to access, 
            assert(this.sNameRefIfcAccess == null || this.sNameRefIfcAccess.length() >0);  //null if the interface is implemented in the module.
          }
          this.bReferencedModule = true;
          this.sNameIclass = "";
          bRefNextUsed = true;
        } else if(this.sRef.equals("modules")) {                // access to an own sub modules
          final String sRefUse;
  //        if(mdlRef.nameInstance ==null || mdlRef.type.isTopLevelType) { sRefUse =  sRefNext; }
  //        else { sRefUse =  mdlRef.nameInstance + "_" + sRefNext; }
  //        mdlRef = VhdlConv.d.fdata.idxModules.get(sRefUse);
          this.mdlRef = this.mdlRef.idxSubModules.get(sRefNext);
          if(this.mdlRef == null) {
            J2Vhdl_GenExpr.vhdlError("In VhdlExpTerm.genSimpleValue - Reference not found: " + sRefNext + " searched in: " + this.mdlRef.nameInstance , ref);
          } else {
            this.bVarUsedForReference = bMaybeVarUsedForReference;
            this.sNameRefIfcAccess = null;                        // set if a interface agent is used to access, 
          }
          this.bReferencedModule = true;
          this.sNameIclass = "";
          bRefNextUsed = true;
        } else if(this.sRef.equals("Fpga")) {     // static reference Fpga...
        } else  {                            // any other: use this as inner class
          if(this.sNameIclass!=null && this.sNameIclass.length() >0) {
            if(this.sNameIclass.equals("YRxSpeData"))
              Debugutil.stop();
            if(Character.isUpperCase(this.sNameIclass.charAt(0)) && Character.isUpperCase(this.sRef.charAt(0))) {
              this.sNameIclass += "_" + this.sRef;
              this.bRefToType = true;                           // ClassType.Enumtype as global access
            } else {
              this.sNameIclass += "." + this.sRef;
            }
          } else {
            this.sNameIclass = this.sRef;
          }
          this.bRefIclass = true;
        }
        if(bRefNextUsed && refNext !=null) {
          refNext = refNext.get_reference();
        }
        ref = refNext;
        
      } // while ref2 !=null                                 // ^^^^^^ end reference evaluated ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }
  
  
  }
  
  
  static RefModuleInfo getReferencedModule ( JavaSrc.SimpleValue val, J2Vhdl_ModuleInstance mdlArg, String nameIclassArg ) {
    RefModuleInfo info;
    JavaSrc.Reference ref = val.get_reference();
    if(ref !=null) {
      info = new RefModuleInfo();
      info.getReference(val, mdlArg, nameIclassArg);
    } else {
      info = null;
    }
    return info;
  }
  
  
  
  
  /**Only inner operation of {@link #addPartValue(org.vishia.java2Vhdl.parseJava.JavaSrc.SimpleValue, boolean, J2Vhdl_ModuleInstance, String)}.
   * Generates a simple value as a part of this expression term.
   * It regards here also references and interface operations. 
   * @param val parsed result for the SimpleValue
   * @param genBool true if the expression should be boolean type, e.g. as IF condition. 
   *   false if the expression is an operation for example for BIT types. 
   * @param mdlArg module from the context. 
   * @param nameIclassArg name of the process class from the context.
   * @param indent it is necessary on a construct "( cond ? trueValue : falseValue)"
   *   because for that an intermediate variable should be created in the line before.   
   * @throws Exception 
   */
  private J2Vhdl_Variable genSimpleValue(JavaSrc.SimpleValue val, boolean genBool, J2Vhdl_ModuleInstance mdlArg, String nameIclassArg
    , CharSequence indent, boolean dbgStop) throws Exception {
    String s = val.toString();
    J2Vhdl_Variable varDescr = null;   //only set if a variable was found.
    if(dbgStop){
      Debugutil.stop();
    }
    RefModuleInfo mdlRefInfo = getReferencedModule(val, mdlArg, nameIclassArg);
    J2Vhdl_ModuleInstance mdlRef = mdlRefInfo == null ? mdlArg : mdlRefInfo.mdlRef;
    //inner variable without any reference, also without this .... not supported
    String sNameIclass = mdlRefInfo !=null ? mdlRefInfo.sNameIclass : nameIclassArg;
//    String sNameIclass = null;  //nameIclassArg;                    // Generally use nameIclass, maybe other referenced process class in the module or in another module. 
//        if((mdlRefInfo ==null || !mdlRefInfo.bReferencedModule) && sNameIclass ==null && nameIclassArg !=null && nameIclassArg.length()>0) {
//      sNameIclass = nameIclassArg;  //for simple variables, necessary if first an operation is called without sNameIclass of course.
//    }
    //
    JavaSrc.ConstNumber constNr = val.get_constNumber();
    JavaSrc.SimpleVariable var = val.get_simpleVariable();
    if(val.get_parenthesisExpression()!=null) {
      this.b.append(" ( ");
      JavaSrc.Expression expr1 = val.get_Expression();
      VhdlExprTerm termSimpleValue = J2Vhdl_GenExpr.d.genExpression(this.b, expr1, genBool, false, mdlRef, sNameIclass, indent, null, null);
      this.b.append(" ) ");
      this.exprType_.set(termSimpleValue.exprType_);
    }
    else if(var !=null) {                                // --- variable --------------------------------------------------
      String varName = var.get_variableName();
      if(varName.equals("fast"))
        Debugutil.stop();

      if(  this.precedSegm.sJava.equals("==")
        || this.precedSegm.sJava.equals("!=")  ) {         // If a equate operator is given, check whether the right side is a State constant:
        final String sNameBit;
        if(mdlRefInfo !=null && mdlRefInfo.bRefToType) {                                 
          sNameBit = sNameIclass + "_" + varName;          // to another Type
        } else {
          String mdlType = mdlRef.type.nameType;           // The own type
          sNameBit = mdlType + "_" + sNameIclass + "_" + varName;
        }
        String snrBit = J2Vhdl_GenExpr.d.fdata.idxEnumBitDef.get(sNameBit);  //search the right var, check ident is a state enum const
        if(snrBit !=null) {
          Debugutil.stop();
          this.b.append(snrBit);
          this.bNot = this.precedSegm.sJava.equals("!=");
          this.exprType_.etype = VhdlExprTerm.ExprTypeEnum.stateBit;  // makr it with stateBit, check and generate access in calling level
        }
      }
      if(this.exprType_.etype != VhdlExprTerm.ExprTypeEnum.stateBit) {  // not a state bit in succession of the branch immediately above.
        boolean bMaskVar = varName.startsWith("m_");
        boolean bTimeMaskVar = sNameIclass !=null && (sNameIclass.endsWith("time") || sNameIclass.endsWith("time_") ) 
                            || varName.startsWith("time_") || varName.startsWith("_time") || bMaskVar;
        if(bTimeMaskVar || bMaskVar) {
          varDescr = null;   //not necessary
//            varDescr = this.getVariableAccess(var, mdlRef, sNameIclass);  //vhdlConv.getVariableAccess(val, mdlRef, sNameIclass);
//            if(varDescr !=null) {
//              Debugutil.stop();  //Detection of time variables ....
//            }
        } else {
          if(mdlRefInfo !=null && mdlRefInfo.sRef !=null && mdlRefInfo.sRef.equals("Fpga")) {
            varDescr = J2Vhdl_GenExpr.d.fdata.varClk;
          } else {
            varDescr = this.getVariableAccess(var, mdlRef, sNameIclass);  //vhdlConv.getVariableAccess(val, mdlRef, sNameIclass);
          }
        }
        if(varDescr !=null) {
          if(varDescr.sElemJava.contains("ringMstLo_Pin"))
            Debugutil.stop();
          this.setVariable(varDescr);
          this.exprType_.set(varDescr.type);
          this.b.append(varDescr.sElemVhdl);
//            final boolean isBool = varDescr.type.etype == VhdlExprTerm.ExprTypeEnum.bittype;
//            if(isBool && genBool) { convBoolExpr(this.b, val); }     // appends = '0' or = '1'
        } else {                                             // Variable not found:
          if(bTimeMaskVar) {
            this.exprType_.etype = ExprTypeEnum.timeVar;
          } else if(bMaskVar) {
            this.exprType_.etype = ExprTypeEnum.timeVar;
          } else if(varName.startsWith("time")) {
            this.exprType_.etype = ExprTypeEnum.timeVar;
          } else {
            throw new IllegalArgumentException("variable not found: " + varName);
          }
        }
      }
    }
    else if(constNr !=null) {
      String src = constNr.get_sNumber();           // parsed <constNumber?""@> without leading and trailing spaces.
      if(src ==null) {
        
        Debugutil.stop();
      } else {
        Debugutil.stop();
      }
      if(constNr.get_booleanConst() !=null) {
        String sBool = val.get_booleanConst();
        assert(this.exprType_.etype == VhdlExprTerm.ExprTypeEnum.undef);
        if(sBool.equals("false")) { 
          this.b.append("'0'"); 
          if(this.exprType_.etype == VhdlExprTerm.ExprTypeEnum.undef) { 
            this.exprType_.etype = VhdlExprTerm.ExprTypeEnum.bitStdConst;
            this.exprType_.nrofElements = 1;
          } 
        }
        else if(sBool.equals("true")) { 
          this.b.append("'1'"); 
          if(this.exprType_.etype == VhdlExprTerm.ExprTypeEnum.undef) { 
            this.exprType_.etype = VhdlExprTerm.ExprTypeEnum.bitStdConst; 
            this.exprType_.nrofElements = 1;
          }
        }
        else if(sBool.startsWith("0b")) { 
          this.b.append(" \"").append(sBool.substring(2)).append("\""); 
          if(this.exprType_.etype == VhdlExprTerm.ExprTypeEnum.undef) { 
            this.exprType_.etype = VhdlExprTerm.ExprTypeEnum.bitStdVconst;
            this.exprType_.nrofElements = sBool.length()-2;  // without leading 0b
          } 
        }
        else {
          this.b.append(" ??boolExpr:").append(sBool);
        }
      }
      else if(constNr.get_intNumber() !=0 || constNr.get_sNumber().equals("0")) {
        this.b.append(Integer.toString(val.get_intNumber()));
        if(this.exprType_.etype == VhdlExprTerm.ExprTypeEnum.undef) { 
          this.exprType_.etype = VhdlExprTerm.ExprTypeEnum.numConst;
          this.exprType_.nrofElements = 1;
        }
      }
      else if(constNr.get_hexNumber() !=0 || src.startsWith("0x")) {
        this.b.append("x\"").append(src.substring(2)).append("\"");
        //this.b.append('\"').append(Integer.toBinaryString(val.get_hexNumber())).append('\"');
        if(this.exprType_.etype == VhdlExprTerm.ExprTypeEnum.undef) { 
          this.exprType_.etype = VhdlExprTerm.ExprTypeEnum.bitStdVconst;
          this.exprType_.nrofElements = (src.length()-2) *4;  //without "0x..."
        }
      }
      else {
        this.b.append(src);                  // it is usual "0", TODO save info for type of scanned number, as attribute
      }
    } //if constNr
    //    ------------------------------------------------- simpleMethodCall ----------------------------------------------
    else if(val.get_simpleMethodCall() !=null) {           // operation(), either Fpga.getBits(..) ... or interface()
      final JavaSrc.SimpleMethodCall sFn = val.get_simpleMethodCall();
      final JavaSrc.ActualArguments args = sFn.get_actualArguments();
      final Iterator <JavaSrc.Expression> iArgs = args ==null ? null: args.get_Expression().iterator();
      String name = sFn.get_methodName();
      if(dbgStop)
        Debugutil.stop();
      if(mdlRefInfo !=null && mdlRefInfo.sRef !=null && mdlRefInfo.sRef.equals("Fpga")) {             // static operation from the Fpga class
        if(name.equals("clk"))
          Debugutil.stop();
        try {                                              //Fpga.getBit(...) etc.
          J2Vhdl_GenExpr.GenOperation genOperation = J2Vhdl_GenExpr.d.idxFpgaOperations.get(name);
          if(genOperation ==null) {
            Debugutil.stop();
            this.b.append("??").append(name).append("??");
          } else {
            genOperation.genOperation(iArgs, this, mdlRef, nameIclassArg);
          }
        } catch(NoSuchElementException exc) {
          System.err.println(exc.getMessage());
        }
      } else if(name.equals("update")) {                   // do nothing, it is a update operation in the update operation.
        // Hint the update operation is evaluated to find assignments to the output.
        // operation of mdl level are for testing, not intent to be interface calls.
      } else if(mdlRefInfo !=null && mdlRefInfo.bReferencedModule) {                       // operation call via ref module is an interface operation
        String sIfcName;
        if(mdlRefInfo ==null || mdlRefInfo.sNameRefIfcAccess == null) {
          sIfcName = (sNameIclass !=null && sNameIclass.length() >0 ? sNameIclass + "." : "") + name;
          sNameIclass = null;                            // it was used to build the sIfcName, not part of the variable access.
        } else {
          sIfcName = mdlRefInfo.sNameRefIfcAccess + "." + name;
        }
        if(dbgStop) {
          Debugutil.stop();
        }
        J2Vhdl_ModuleType.IfcConstExpr ifcDef = mdlRef ==null ? null : mdlRef.type.idxIfcExpr.get(sIfcName);
        if(ifcDef == null) {
          J2Vhdl_GenExpr.vhdlError("VhdlExprTerm.genSimpleValue() - Interface operation not found: " + sIfcName + " in module: " + (mdlRef == null ? "??unknown" : mdlRef.nameInstance), val);
        } else if(ifcDef.constVal !=null) {
          J2Vhdl_Variable cvar = ifcDef.constVal.var;
          this.exprType_.etype = cvar.type.etype;
          this.exprType_.nrofElements = cvar.type.nrofElements;
          this.b.append(cvar.sElemVhdl);
        } else if(ifcDef.expr !=null){
          boolean bInsideProcess = true;
          //String sNameIclassOp = nameIclassArg;                   // because inside the operation the outside reference to the call is not relevant. 
          VhdlExprTerm ifcTerm = J2Vhdl_GenExpr.d.genExpression(null, ifcDef.expr, genBool, bInsideProcess, mdlRef, sNameIclass, indent, null, null);
          this.exprType_.etype = ifcTerm.exprType_.etype;
          this.exprType_.nrofElements = ifcTerm.exprType_.nrofElements;
          this.nrOperands += ifcTerm.nrOperands;
          if(ifcTerm.nrOperands >1) {                      // use parenthesis to clarify precedence problems.
            this.b.append(" (").append(ifcTerm.b).append(") ");
          } else {
            this.b.append(ifcTerm.b);
          }
        }
      }
      else {
        // do nothing on all other operation calls, without ref.module and without Fpga.
        // this operations are for simulation. (TODO what about functions in VHDL, solution, use specific annotation and build a list of functions. 
      }
    }
    else if(val.get_simpleCharLiteral() !=null) {          // it is especially to deal with STD_LOGIC
      char cc = val.get_simpleCharLiteral().charAt(0);
      if(this.exprType_.etype == VhdlExprTerm.ExprTypeEnum.undef) { 
        this.exprType_.etype = VhdlExprTerm.ExprTypeEnum.stdtype;  //'L' 'H' '0' etc. are standard types.
        this.exprType_.nrofElements = 0;  //without "0x..."
      } else {
        Debugutil.stop();
      }
      this.b.append('\'').append(cc).append('\'');
    }
    else {
      this.b.append("0");   //it seems to be an integer value with 0. Nothing is detected else.
    }
//    if(this.b.toString().contains("FpgaTop_SpeA_dataStateRx"))
//      Debugutil.stop();
    return varDescr;  //set if a variable was found. 
  }

  
  
  
  
  /**Search the proper VHDL variable due to the given Java expression.
   * @param var Variable for access: 
   *   The variable with only the name without regarding references is first search in the local variables.
   *   Then it is searched in the global ones.
   *   If the variable starts with "time_", is "time" or "_time_", not used.
   *   If the variable is not found and its name is _val_ then the path is shorten to the reference only and try to get. 
   *   This is an access to inner values of enums.
   * @param mdl In which module
   * @param nameInnerClassVariable The path to the variable for search. It is also possible the Typename. 
   * @return null on unnecessary variables. null with an error message on not found variables.
   * @throws IOException
   */
  static J2Vhdl_Variable getVariableAccess(JavaSrc.SimpleVariable var, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable) throws IOException {
    final String name;
    final String sRef;
    final String sElemJava;
    final String dbg;
    if(nameInnerClassVariable !=null && nameInnerClassVariable.equals("time_")) {
      return null;                                         // access to a time sub structure such as this.time.varName is not part of VHDL
    }
    name = var.get_variableName(); 
    if(  name.startsWith("m_")                           // variables m_ are masks for Java, not relevant. 
      || name.equals("time") || name.startsWith("time_") || name.equals("_time_")) {
      return null;                                       // all variables with time or not relevant
    }
    if(name.equals("value"))
      Debugutil.stop();
      //else { vhdlError("reference only with a variable possible", ref);  sRef = "??."; }
    dbg="refnull";
    sElemJava = name;                                      // especially for the ioPins module: use the type to find the variable.
    
    if(mdl.type instanceof J2Vhdl_ModuleVhdlType) {        // == called VHDL module
      J2Vhdl_ModuleVhdlType mdlt = (J2Vhdl_ModuleVhdlType) mdl.type;
      if(!nameInnerClassVariable.equals("input") && !nameInnerClassVariable.equals("output")) {
        J2Vhdl_GenExpr.vhdlError("called VHDL module, variables should be input.name or output.name", var);
      }
      J2Vhdl_Variable varDescr = mdlt.idxIOVars.get(sElemJava);
      return varDescr;
    }
    else {                                                 //== other module
    
      final String sMdlIdent = mdl.nameInstance.equals("ioPins") ? mdl.type.nameType 
          : mdl.nameInstance;                                // else: The name of the variable is built with the instance name.
      sRef = sMdlIdent + "." + ( nameInnerClassVariable == null || nameInnerClassVariable.length()==0 ? "" : nameInnerClassVariable + '.');          
      J2Vhdl_Variable varDescr = J2Vhdl_GenExpr.d.fdata.idxProcessVars.get(sElemJava);
      final String sElemJava2 = sRef + sElemJava;
      if(varDescr == null) {
        varDescr = J2Vhdl_GenExpr.d.fdata.idxVars.get(sElemJava2);
      } else {
        Debugutil.stop();                  // a local PROCESS variable
      }
      if(varDescr == null && sElemJava2.endsWith("._val_")) {// Pattern for a state value
        final String sElemJava3 = sElemJava2.substring(0, sElemJava2.length()-6);
        varDescr = J2Vhdl_GenExpr.d.fdata.idxVars.get(sElemJava3);
      }
      if(varDescr == null) {
        J2Vhdl_ConstDef cvar = J2Vhdl_GenExpr.d.fdata.idxConstDef.get(sElemJava2);
        if(cvar !=null) {
          varDescr = cvar.var;
        }
      }
      if(varDescr == null) {
        J2Vhdl_GenExpr.vhdlError("VhdlExprTerm.getVariableAccess() - unknown variable >>" + sElemJava2 + "<< :" + dbg, var);
        return null;
      } else {
        return varDescr;
      }
    }
  }



  @Override public String toString() { return this.b.toString() + ":" + this.exprType_.toString(); }

}
