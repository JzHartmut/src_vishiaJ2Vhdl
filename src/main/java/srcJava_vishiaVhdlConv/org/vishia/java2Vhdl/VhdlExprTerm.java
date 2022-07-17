package org.vishia.java2Vhdl;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.vishia.genJavaOutClass.SrcInfo;
import org.vishia.java2Vhdl.J2Vhdl_Variable;
import org.vishia.java2Vhdl.parseJava.JavaSrc;
import org.vishia.util.Debugutil;
import org.vishia.util.StringFunctions;

/**Helds info for the current Expression term.
 */
public final class VhdlExprTerm extends SrcInfo {

  /**Version, history and license.
   * <ul>
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
  public final static String sVersion = "2022-05-02"; 

  
  /**Type of a variable and a build expression.
   */
  enum ExprTypeEnum {
      undef   (0,0)
    , bittype (0,0)
    , bitStdConst (0,1)   //'1'
    , bitStdVconst(1,1)   //"1100"
    , numConst(0,1)
    , bitVtype(1,0)
    , stdtype (0,0)
    , stdVtype(1,0)
    , booltype(0,0)
    , boolUncompleteType(0,0)
    , inttype (1,0)
    , uinttype(1,0);
    
    boolean bVector, bConst;
    ExprTypeEnum(int bVector, int bConst){ this.bVector = bVector !=0; this.bConst = bConst !=0; }
  }

  static class ExprType {
    VhdlExprTerm.ExprTypeEnum etype;
    
    int nrofElements;
    
    void set(ExprType src) {
      this.etype = src.etype;
      this.nrofElements = src.nrofElements;
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
  public VhdlExprTerm(VhdlConv vhdlConv) {
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
  public VhdlExprTerm(StringBuilder b, J2Vhdl_Operator precedSegm, ExprType type, int nrOperands, VhdlConv vhdlConv) {
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
      , boolean genBool, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable) throws Exception {
    //
    if( ! super.containsInfo()) { 
      super.setSrcInfo(part);                              // store the source info from the first part
    }
    int posEnd = this.b.length();
    final VhdlExprTerm exprRight;
    if(exprRightArg !=null) { exprRight = exprRightArg; }  // use given exprRight, maybe a more complex term
    else if(this.b.length() ==0) { exprRight = null; }     // add part to the empty this ExprPart, for the first part of a term
    else {                                                 // prepare the exprRight from part to add to this.
      final boolean bNeedBoolRight = opPreced.opBool.bMaybeBool 
        && (genBool || this.exprType_.etype == VhdlExprTerm.ExprTypeEnum.booltype);
    
      exprRight = genExprPartValue(part.get_value(), bNeedBoolRight, mdl, nameInnerClassVariable);
      if(exprRight == null) {
        this.b.setLength(posEnd);                          // then remove the operator also again, 
        return true;                                      // faulty variable, especially mask, or time.
      }
      if( bNeedBoolRight) {
        exprRight.fulfillNeedBool(true);
      }
//      J2Vhdl_Variable varRight = exprRight.variable();
    }
    final boolean bNeedBoolLeft = (exprRight !=null && exprRight.exprType_.etype == VhdlExprTerm.ExprTypeEnum.booltype)
    || opPreced.opBool.bMaybeBool && genBool;              // The operator supports bool and bool is requested
    // 
    if(  !bNeedBoolLeft && exprRight !=null                    // if bNeedBool, then conversion to boolean is anyway done in exprLeftAppendOperator in immediately following block
      && !exprRight.exprType_.etype.bConst                 // typical case, right constant, should be proper, tested in VHDL
      && !exprRight.exprType_.etype.bVector                // vector types should be clarified in source, tested in VHDL
      && this.exprType_.etype != exprRight.exprType_.etype // the interest case is BIT vs. STD_LOGIC should be handled.
      ) {
      if( this.exprType_.etype == VhdlExprTerm.ExprTypeEnum.bittype) {
        if( exprRight.exprType_.etype == VhdlExprTerm.ExprTypeEnum.stdtype) {
          exprRight.b.insert(0, "TO_BIT(").append(")");    // use that type, which is given from left, either BIT or STD_LOGIC
        }
      } 
      else if( this.exprType_.etype == VhdlExprTerm.ExprTypeEnum.stdtype) {
        if( exprRight.exprType_.etype == VhdlExprTerm.ExprTypeEnum.bittype) {
          exprRight.b.insert(0, "TO_STDULOGIC(").append(")");  //TP_STD_LOGIC does not exists, only STDULOGIC, should be work always.
        }
      }
    }
    //    
    if(posEnd >0) {                                        // empty on first left operand. 
      this.exprLeftAppendOperator(opPreced, bNeedBoolLeft);// add operator to the expression term, maybe first left side convert to bool. 
    } else {
      if(!opPreced.sJava.equals("@")) {
        System.err.println("exprLeftAddOperand: Start expression faulty");
      }
      assert(opPreced.sJava.equals("@"));                  // the first for empty term is always the @ operator (set accu)
    }
    //-------------------------------------------------    // needBool for the next following operator:
    if(exprRight !=null) {
      this.b.append(exprRight.b).append(' ');              // "+ @" right side expression used, it is the before prepared one.
    } else { //only here if this.b is empty                // + part, then append the part from source expression to the term.
      assert(this.b.length() ==0);
      if(!addPartValue(part.get_value(), false, mdl, nameInnerClassVariable)) {    // false if the operand is not valid, a mask or time 
        this.b.setLength(posEnd);                          // then remove the operator also again, 
        return false;                                      // faulty variable, especially mask, or time.
      }
    }
    this.nrOperands +=1;

    return true;
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
    if(opPreced.opBool.bForceToBool ) { 
      this.exprType_.etype = VhdlExprTerm.ExprTypeEnum.booltype;
      this.exprType_.nrofElements = 1;
    }

  }



  public static VhdlExprTerm genExprPartValue (JavaSrc.SimpleValue val, boolean needBool, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable) 
      throws Exception {
    VhdlExprTerm thiz = new VhdlExprTerm(VhdlConv.d);
    boolean bOk = thiz.addPartValue(val, needBool, mdl, nameInnerClassVariable);
    return bOk ? thiz : null;
  }
  
  
  
  
  /**Writes the value of an ExprPart to the term
   * @param b
   * @param part
   * @param genBool
   * @return not null only if a valid variable was written.
   * @throws Exception 
   */
  private boolean addPartValue (JavaSrc.SimpleValue val, boolean needBool, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable) 
      throws Exception {
    if(VhdlConv.d.dbgStop) {
      int[] lineColumn = new int[2];
      String file = val.getSrcInfo(lineColumn); //BlinkingLed_Fpga
      if(file.contains("SpiData.java") && lineColumn[0] >= 391 && lineColumn[0] <= 391)
        Debugutil.stop();
    }
    String sUnaryOp = val.get_unaryOperator();           // unary operator
    if(sUnaryOp !=null && !needBool) {
      if(sUnaryOp.equals("!")) { sUnaryOp = " NOT "; }
      else if(sUnaryOp.equals("~")) { sUnaryOp = " NOT "; }  //bitwise NOT
      else;                                              // unary  + or - remains.
      this.b.append(sUnaryOp);
      this.posAfterUnary = this.b.length();
    }
    boolean bOk = genSimpleValue(val, false, mdl, nameInnerClassVariable, null);
    if(bOk && needBool) { // && this.exprType_.etype != VhdlExprTerm.ExprTypeEnum.booltype) {
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
    return bOk;
  }


  /**Generates a simple value as a part of this expression term.
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
  boolean genSimpleValue(JavaSrc.SimpleValue val, boolean genBool, J2Vhdl_ModuleInstance mdlArg, String nameIclassArg, CharSequence indent) throws Exception {
    try {
      String s = val.toString();
      boolean dbgStop = false;
      String s1 = val.toString();
      if(VhdlConv.d.dbgStop) {
        int[] lineColumn = new int[2];
        String file = val.getSrcInfo(lineColumn); //BlinkingLed_Fpga
        dbgStop = file.contains("BlinkingLedCt.java") && lineColumn[0] >= 71 && lineColumn[0] <= 170;
      }
      if(dbgStop){
        Debugutil.stop();
      }
      boolean bOk = true;
      J2Vhdl_ModuleInstance mdlRef = mdlArg;                 // Generally use mdlRef, maybe other referenced module
      //inner variable without any reference, also without this .... not supported
      String sNameIclass = null;  //nameIclassArg;                    // Generally use nameIclass, maybe other referenced process class in the module or in another module. 
      JavaSrc.Reference ref = val.get_reference();
      String sRef = null;                                    // String which is used to find the correct variables
      String sNameRefIfcAccess = null;
      boolean bRefIclass = false;                            // true then iClass is set per reference
      boolean bReferencedModule = false;
      while(ref !=null) {
        boolean bIsThis = ref.get_isThis()!=null;
        JavaSrc.SimpleVariable var = ref.get_referenceAssociation();
        JavaSrc.Reference refNext = ref.get_reference();
        JavaSrc.SimpleVariable varNext = refNext == null ? null : refNext.get_referenceAssociation();
        sRef = var == null ? null : var.get_variableName(); 
        String sRefNext = varNext == null ? null : varNext.get_variableName(); 
        boolean bRefNextUsed = false;
        
        if(var ==null && !bIsThis) { 
          VhdlConv.vhdlError("only a reference with variable is supported", ref);
          bOk = false;
        }
        //
        if(bIsThis) {                          // iclass before this is only the enclosing class name, remove it. 
          if(bRefIclass) {
            if(nameIclassArg == null || nameIclassArg.length()==0) {
              
            } else {
              Debugutil.stop();
            }
          }
          sNameIclass = nameIclassArg;         
          bReferencedModule = true;
        } else if(sRef ==null) {          //do nothing if sRef is not given (maybe only for bIsThis)
        } else if(sRef.equals("z")) {
          sNameIclass = nameIclassArg;
          bReferencedModule = true;
        } else if(sRef.equals("mdl")) {
          sNameIclass = null;             // maybe null if operation of the module is called.
          bReferencedModule = true;
          //bRefNextUsed = true;
        } else if(sRef.equals("ref")) {                      // get the referenced module, and maybe an inner sAccess
          J2Vhdl_ModuleInstance.InnerAccess mdlRef2 = mdlRef.idxAggregatedModules.get(sRefNext);
          if(mdlRef2 == null) {
            VhdlConv.vhdlError("In VhdlExpTerm.genSimpleValue - Reference not found: " + sRefNext + " searched in: " + mdlRef.nameInstance , ref);
          } else {
            mdlRef = mdlRef2.mdl;
            sNameRefIfcAccess = mdlRef2.sAccess;             // set if a interface agent is used to access, 
            assert(sNameRefIfcAccess == null || sNameRefIfcAccess.length() >0);  //null if the interface is implemented in the module.
          }
          bReferencedModule = true;
          sNameIclass = "";
          bRefNextUsed = true;
        } else if(sRef.equals("Fpga")) {     // static reference Fpga...
        } else  {                            // any other: use this as inner class
          if(sNameIclass!=null && sNameIclass.length() >0) {
            if(sNameIclass.equals("YRxSpeData"))
              Debugutil.stop();
            sNameIclass += "." + sRef;
          } else {
            sNameIclass = sRef;
          }
          bRefIclass = true;
        }
        if(bRefNextUsed && refNext !=null) {
          refNext = refNext.get_reference();
        }
        ref = refNext;
        
      } // while ref2 !=null                                 // ^^^^^^ end reference evaluated ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
      if(!bReferencedModule && sNameIclass ==null && nameIclassArg !=null && nameIclassArg.length()>0) {
        sNameIclass = nameIclassArg;  //for simple variables, necessary if first an operation is called without sNameIclass of course.
      }
      //
      JavaSrc.ConstNumber constNr = val.get_constNumber();
      JavaSrc.SimpleVariable var = val.get_simpleVariable();
      if(!bOk) {} // do nothing
      else if(val.get_parenthesisExpression()!=null) {
        this.b.append(" ( ");
        JavaSrc.Expression expr1 = val.get_Expression();
        VhdlExprTerm termSimpleValue = VhdlConv.d.genExpression(this.b, expr1, genBool, false, mdlRef, sNameIclass, indent, null);
        this.b.append(" ) ");
        this.exprType_.set(termSimpleValue.exprType_);
      }
      else if(var !=null) {
        J2Vhdl_Variable varDescr;
        String varName = var.get_variableName();
        boolean bTimeMaskVar = sNameIclass !=null && sNameIclass.endsWith("time") || varName.startsWith("time") || varName.startsWith("_time") || varName.startsWith("m_");
        if(bTimeMaskVar) {
          varDescr = this.getVariableAccess(var, mdlRef, sNameIclass);  //vhdlConv.getVariableAccess(val, mdlRef, sNameIclass);
          if(varDescr !=null) {
            Debugutil.stop();  //Detection of time variables ....
          }
        } else {
          varDescr = this.getVariableAccess(var, mdlRef, sNameIclass);  //vhdlConv.getVariableAccess(val, mdlRef, sNameIclass);
        }
        if(varDescr !=null) {
          if(varDescr.sElemJava.contains("ringMstLo_Pin"))
            Debugutil.stop();
          this.setVariable(varDescr);
          this.exprType_.set(varDescr.type);
          this.b.append(varDescr.sElemVhdl);
          final boolean isBool = varDescr.type.etype == VhdlExprTerm.ExprTypeEnum.bittype;
          if(isBool && genBool) { convBoolExpr(this.b, val); }     // appends = '0' or = '1'
        } else {                                             // Variable not found:
          bOk = false;
          if(!bTimeMaskVar) {
            Debugutil.stop();
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
        else if(constNr.get_intNumber() !=0) {
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
        if(sRef !=null && sRef.equals("Fpga")) {             // static operation from the Fpga class
          try {                                              //Fpga.getBit(...) etc.
            VhdlConv.GenOperation genOperation = VhdlConv.d.idxFpgaOperations.get(name);
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
        } else if(bReferencedModule) {                       // operation call via ref module is an interface operation
          String sIfcName;
          if(sNameRefIfcAccess == null) {
            sIfcName = (sNameIclass !=null && sNameIclass.length() >0 ? sNameIclass + "." : "") + name;
            sNameIclass = null;                            // it was used to build the sIfcName, not part of the variable access.
          } else {
            sIfcName = sNameRefIfcAccess + "." + name;
          }
          J2Vhdl_ModuleType.IfcConstExpr ifcDef = mdlRef ==null ? null : mdlRef.type.idxIfcExpr.get(sIfcName);
          if(ifcDef == null) {
            VhdlConv.vhdlError("VhdlExprTerm.genSimpleValue() - Interface operation not found: " + sIfcName + " in module: " + (mdlRef == null ? "??unknown" : mdlRef.nameInstance), val);
          } else if(ifcDef.constVal !=null) {
            J2Vhdl_Variable cvar = ifcDef.constVal.var;
            this.exprType_.etype = cvar.type.etype;
            this.exprType_.nrofElements = cvar.type.nrofElements;
            this.b.append(cvar.sElemVhdl);
          } else if(ifcDef.expr !=null){
            boolean bInsideProcess = true;
            //String sNameIclassOp = nameIclassArg;                   // because inside the operation the outside reference to the call is not relevant. 
            VhdlExprTerm ifcTerm = VhdlConv.d.genExpression(null, ifcDef.expr, genBool, bInsideProcess, mdlRef, sNameIclass, indent, null);
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
      return bOk;
    } catch(Throwable exc) {
      VhdlConv.vhdlError("XException: " + exc.getMessage(), val);
      return false;
    }
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
  J2Vhdl_Variable getVariableAccess(JavaSrc.SimpleVariable var, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable) throws IOException {
    final String name;
    final String sRef;
    final String sElemJava;
    final String dbg;
    if(nameInnerClassVariable !=null && nameInnerClassVariable.equals("time")) {
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
    final String sMdlIdent = mdl.nameInstance.equals("ioPins") ? mdl.type.nameType 
        : mdl.nameInstance;                                // else: The name of the variable is built with the instance name.
    //
    sRef = sMdlIdent + "." + ( nameInnerClassVariable == null || nameInnerClassVariable.length()==0 ? "" : nameInnerClassVariable + '.');          
    J2Vhdl_Variable varDescr = VhdlConv.d.fdata.idxProcessVars.get(sElemJava);
    final String sElemJava2 = sRef + sElemJava;
    if(varDescr == null) {
      varDescr = VhdlConv.d.fdata.idxVars.get(sElemJava2);
    } else {
      Debugutil.stop();                  // a local PROCESS variable
      
    }
    if(varDescr == null && sElemJava2.endsWith("._val_")) {// Pattern for a state value
      final String sElemJava3 = sElemJava2.substring(0, sElemJava2.length()-6);
      varDescr = VhdlConv.d.fdata.idxVars.get(sElemJava3);
    }
    if(varDescr == null) {
      J2Vhdl_ConstDef cvar = VhdlConv.d.fdata.idxConstDef.get(sElemJava2);
      if(cvar !=null) {
        varDescr = cvar.var;
      }
    }
    if(varDescr == null) {
      VhdlConv.vhdlError("VhdlExprTerm.getVariableAccess() - unknown variable >>" + sElemJava2 + "<< :" + dbg, var);
      return null;
    } else {
      return varDescr;
    }
  }



  @Override public String toString() { return this.b.toString() + ":" + this.exprType_.toString(); }

}
