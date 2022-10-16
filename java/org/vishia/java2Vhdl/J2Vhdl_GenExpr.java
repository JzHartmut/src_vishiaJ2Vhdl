package org.vishia.java2Vhdl;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.vishia.genJavaOutClass.SrcInfo;
import org.vishia.java2Vhdl.parseJava.JavaSrc;
import org.vishia.java2Vhdl.parseJava.JavaSrc.ExprPart;
import org.vishia.util.Debugutil;
import org.vishia.util.StringFunctions;

/**This class is responsible to generation of Statements and Expressions.
 * This class is instantiated only one time.
 * Note: {@link VhdlExprTerm} generates parts of an expression, called here, 
 * with more as one instance proper for left part, right part. 
 * <br>
 * Search for ::: dbgStop ::: for debug stop on specific lines in java source file.
 * @author hartmut Schorrig
 *
 */
public class J2Vhdl_GenExpr {
  
  /**Version, history and license.
   * <ul>
   * <li>2022-08-10 Now obsolete double operations getVariableAccess(...), genAggregation(...) and evalIfcOper(...) removed.
   *   This operations were replaced by some stuff in {@link VhdlExprTerm} and yet used only for getBitShL/R and setBit,
   *   with a twice effort of maintenance. Now replaced.  
   * <li>2022-08-06 see comment in {@link VhdlExprTerm}, type conversions. 
   * <li>2022-07-06 in {@link #genAssignment(Appendable, VhdlExprTerm, J2Vhdl_Operator, VhdlExprTerm, ExprPart, org.vishia.java2Vhdl.parseJava.JavaSrc.ExprPartTrueFalse, J2Vhdl_ModuleInstance, String, CharSequence, boolean)}:
   *   now uses TO_BIT and TO_STDULOGIC instead IF and WHEN for the assignments, important for assign to called Vhdl
   * <li>2022-07-06 Now supports char as STD_LOGIC. 
   *   <ul>
   *   <li>{@link #getOperator(ExprPart, VhdlExprTerm, boolean)} not using XNOR on = for STD_LOGIC, TODO document there
   *   <li>{@link #genTrueFalse(Appendable, CharSequence, ExprPart, J2Vhdl_ModuleInstance, String, boolean, CharSequence, CharSequence)}
   *     distinct between in/outside process using IF or WHEN, elsewhere also necessary, but for STD_LOGIC substantial (assignment '0' or '1' with ?-operator necessary).
   *   <li>{@link #createVariable(org.vishia.java2Vhdl.parseJava.JavaSrc.VariableInstance, String, String, String, Map, Map)}:
   *     Here the type char is regarded, and also yet int, short, byte, long   
   *   </ul> 
   * <li>2022-04-29 {@link Fpga#setBit(int, int, boolean)} and setBits(...) implemented in another way.
   *   inside {@link #genAssignment(Appendable, VhdlExprTerm, J2Vhdl_Operator, VhdlExprTerm, ExprPart, org.vishia.parseJava.JavaSrc.ExprPartTrueFalse, CharSequence, boolean)}
   *   because it is an assignment. Regard types on assign.
   * <li>2022-04-29 {@link #setBit} same as for {@link #setBits}: regarding local variable, use := instead <= 
   * <li>2022-04-28 Hartmut {@link #idxRecordVars} separated from {@link #idxVars}, more clearly.  
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
  public final static String sVersion = "2022-04-28"; 

  public final J2Vhdl_FpgaData fdata;
  
  static public final J2Vhdl_GenExpr d = new J2Vhdl_GenExpr(null, new J2Vhdl_FpgaData());
  
  public boolean XXXdbgStopEnable = true;
  
  /**If not null, then checks whether the line(s) in this file are translated by an expression. 
   * See search-hit ::: dbgStop ::: to set a breakpoint for specific positions of translation code.
   * 
   */
  public String dbgStopExprFile = "SpiData.java";
  
  public int dbgStopLine1 = 506, dbgStopLine2 = 512;
  
  public boolean bAppendLineColumn = false;




  






  
  
  
  /**This interface is used to generate the {@link Fpga#setBit(int, int, boolean)} etc. operations.
   */
  interface GenOperation {
    /**This operation is called with the parsed content of the called operation.
     * @param iArgs Iterator over the arguments, in case of setBit: vector, bit, value. 
     * @param exprDst The operation should be written in this destination.
     *  <ul> 
     *  <li>Especially the {@link VhdlExprTerm#b} is used to store the generated content. 
     *  <li>A {@link VhdlExprTerm#variable()} may be already set, especially for assignement.
     *  <li>The {@link VhdlExprTerm#exprType_} can be used and changed due to the generated result.
     *  </ul> 
     * @return The variable associated to the operation with the type. But it is not used yet. 
     * @throws Exception
     */
    J2Vhdl_Variable genOperation(final Iterator <JavaSrc.Expression> iArgs, VhdlExprTerm exprDst, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable) throws Exception;
  }
  
  
  




  /**The whole module class (this VHDL module). */
  final java.lang.Class<?> classMdl;
  
  /**Obligate specific Helper classes. */
  final java.lang.Class<?> classIn, classOut, classRef;
  
  
  
  /**This is set on entry to a process in JZtxtcmd calling {@link #getCtorProcess(org.vishia.parseJava.JavaSrc.ClassDefinition)}.
   * It is derived from the name of the class which describes the Process or Record, 
   * with same name starting in lowerCase. It is used for the "z." reference. 
   * In VHDL this is the same as the PROCESS related RECORD.
   * 
   */
  String XXXnameInnerClassVariable;
  
  
  
  /**From the converted class of Module:
   * It is the association between the inner name in the Ref class
   * with the used instance which sets the reference, the absolute record name.
   */
  //Map<String, J2Vhdl_ModuleInstance> idxAggregatedModules = new TreeMap<String, J2Vhdl_ModuleInstance>();

  
  
  
  /**The class of the current process. Set in {@link #getCtorProcess(org.vishia.parseJava.JavaSrc.ClassDefinition)}
   * 
   */
  java.lang.Class<?> classPrc;


  //final J2Vhdl_FpgaData fdata;

  /**The instance is created only one time for all sources and PROCESSes
   * @param classMdl
   */
  private J2Vhdl_GenExpr(Class<?> classMdl, J2Vhdl_FpgaData fdata) {
    this.fdata = fdata;
    this.classMdl = classMdl;
    if(classMdl !=null) {
      Class<?>[] classes = classMdl.getClasses();        // search the class in the Java module Class
      Class<?> classIn = null, classOut = null, classRef = null;
      for(Class<?> class1: classes) {
        if(class1.getSimpleName().equals("In")) { // found the inner class as java.lang.Class
          classIn = class1;
        }
        else if(class1.getSimpleName().equals("Out")) { // found the inner class as java.lang.Class
          classOut = class1;
        }
        else if(class1.getSimpleName().equals("Ref")) { // found the inner class as java.lang.Class
          classRef = class1;
        }
      }
      //if(classIn ==null || classOut == null) throw new IllegalArgumentException ("VhdlConv PROCESS: In and Out are Missing - are obligate. ");
      this.classIn = classIn;    //no used furthermore
      this.classOut = classOut;
      this.classRef = classRef;
    } else {
      this.classIn = null;     //never more necessary, all info come from the parser. 
      this.classOut = null;
      this.classRef = null;
    }
  }



  void appendLineColumn(Appendable out, SrcInfo info) throws IOException {
    if(this.bAppendLineColumn) {
      int[] lineCol = new int[2];
      String file = info.getSrcInfo(lineCol);
      int posFile = file.lastIndexOf('/') +1;  //0 if not found
      out.append("            -- ").append(file.substring(posFile)).append(", line:").append(Integer.toString(lineCol[0]));
    }
  }
  
  



  private static J2Vhdl_Operator getOperator ( JavaSrc.ExprPart part, VhdlExprTerm exprLeft, boolean genBool) {
    String sOperator = part.get_operator();
    if(sOperator ==null) { sOperator = "@"; }            // the start of the expression.
    //
    J2Vhdl_Operator opPreced = J2Vhdl_Operator.operatorMap.get(sOperator);       // check the operator
    if(opPreced.sCheckEqXor!=null && (!genBool && !exprLeft.exprType_.etype.bVector && exprLeft.exprType_.etype != VhdlExprTerm.ExprTypeEnum.stdtype )) {
      opPreced = J2Vhdl_Operator.operatorMap.get(opPreced.sCheckEqXor);   //document when this is used TODO
    }
    return opPreced;
  }
  
  
  
  
  //tag::genExpressionHead[]
  /**Generates a VHDL expression from the parsed Java expression.
   * The Java expression is given or prepared here as Revers Polish Notation by post preparing, 
   * calling {@link JavaSrc.Expression#prep(org.vishia.parseJava.JavaSrc.Expression, Appendable)}. 
   * It means it is the natural execution order, not the writing order of operations.
   * The precedence of operations in Java are not relevant. 
   * But the operands are given in writing order.
   * The result is a valid VHDL source code with the specific VHDL precedence. It is not the same as in Java. 
   * @param out to write out, may be a StringBuffer. For simple expressions the same is written as contained in the return expression. 
   *   But for assignments or truefalseValues more lines are created.
   * @param exprArg
   * @param genBool true if the expression is necessary in a VHDL boolean environment (IF)
   * @param indent
   * @param assignTerm null or the left assign expression if called in an assign context. Then this expression is the right side expression to assign.
   * @param assignType used in case of truefalseExpr, type of the left variable.
   * @return The built expression with type information. 
   * @throws Exception 
   */
  public VhdlExprTerm genExpression ( Appendable out, JavaSrc.Expression exprRpn, boolean genBool, boolean bInsideProcess
      , J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable
      , CharSequence indent, CharSequence assignTerm, VhdlExprTerm.ExprType assignType) 
      throws Exception {
    //
    VhdlExprTerm exprLeft = null;                          // left side expression segment from stack popped.
    try {
      boolean dbgStop = false;
      if(this.dbgStopExprFile !=null) { 
        int[] lineColumn = new int[2];
        String file = exprRpn.getSrcInfo(lineColumn);  // TxSpe BlinkingLedCt ClockDivider BlinkingLed_Fpga
        if(file.contains(this.dbgStopExprFile) && lineColumn[0] >= this.dbgStopLine1 && lineColumn[0] <= this.dbgStopLine2) {
          Debugutil.stop();
          dbgStop = true;
      } }
      if( ! exprRpn.isPrepared()) { exprRpn.prep(null); }
      Deque<VhdlExprTerm> uStack = new ArrayDeque<VhdlExprTerm>(); 
      int nrAllOperands = 0;
      boolean bLastWasAssignment = false;
      JavaSrc.ExprPart lastPart = null;                      // the last part can contain a trueFalseExpression, check afterwards 
      JavaSrc.ExprPartTrueFalse partTrueFalse = null;
      boolean bUseTrueFalse = false;
      for(JavaSrc.ExprPart part : exprRpn.get_ExprPart()) {
        //end::genExpressionHead[]
        //tag::genExpressionParts[]
        lastPart = part;
        //partTrueFalse = part instanceof JavaSrc.ExprPartTrueFalse ? (JavaSrc.ExprPartTrueFalse) part : null;
        if(part instanceof JavaSrc.ExprPartTrueFalse) {
          partTrueFalse = (JavaSrc.ExprPartTrueFalse) part;  // store till assignment or end
        }
        if(exprLeft !=null && StringFunctions.startsWith(exprLeft.b, "ringMstLo_Pin"))
          Debugutil.stop();
        J2Vhdl_Operator opPreced = getOperator(part, exprLeft, genBool);
        //
        //pop
        final VhdlExprTerm exprRight;                      // right side expression segment from left for operation in stack.
        if(part.get_value() ==null) {                      // "@"  // use the accu as operand, pop leftExpr from stack:
          exprRight = exprLeft;                            // then the current expression is the right part, sOperand
          exprLeft = uStack.pop();                         // pop: continue with pushed expression as left part
          bUseTrueFalse = partTrueFalse !=null;            // after pop a last partTrueFalse should be evaluated
        } else { 
          exprRight = null;                                // no pop, then exprRight is empty.
          //push
          if(opPreced.sJava.equals("@")) {                 // start of a new expression segment. @a
            if(exprLeft !=null) {
              uStack.push(exprLeft);                       // push the current expression in stack, use later
            }
            exprLeft = new VhdlExprTerm(this);             // new empty exprLeft
          }
        }
        // Write subordinate expression terms in parenthesis if they have a lower precedence
        // or also on equal precedence in VHDL. 
        // Writing equal precedence in parenthesis is then necessary if they come from own terms
        // of for a special VHDL problem: Because some VHDL Translators tests (...AND  ...)
        // (parenthesis surround AND terms though there are unnecessary in VHDL).
        // Example: b1 OR b2 AND b3 is clarified and correct in VHDL: Execute it in order, OR first, then AND.
        //          But the tools tests whether it is written (b1 OR b2) AND b3 because supposing of an writing error
        if(exprRight !=null                                
          && ( opPreced.precedVhdl > exprRight.precedSegm.precedVhdl   
            || opPreced.precedVhdl == exprRight.precedSegm.precedVhdl && opPreced != exprRight.precedSegm //&& opPreced.bAnd
          )  ) {                                            
          exprRight.b.insert(0, " (").append(") ");        // This part in parenthesis 
        }
        if(exprLeft !=null && exprLeft.b.length() >0 
          && ( opPreced.precedVhdl > exprLeft.precedSegm.precedVhdl
            || opPreced.precedVhdl == exprLeft.precedSegm.precedVhdl && opPreced != exprLeft.precedSegm //&& opPreced.bAnd 
          )  ) { // if the precedence is greater or also equal
          exprLeft.b.insert(0, " (").append(") ");         // then set both expressions in parenthesis to clarify lesser precedence
        }
        //
        if(opPreced.opBool.bAssign) {                      // assignment
          bLastWasAssignment = true;
          //
          //======>>>>>>>
          if(dbgStop)
            Debugutil.stop();
          if(exprLeft.variable() !=null) {                 // elsewhere it is a time variable assignment
            genAssignInExpression(out, exprLeft, opPreced, exprRight, part, partTrueFalse, mdl, nameInnerClassVariable, indent, bInsideProcess, dbgStop);
          }
          partTrueFalse = null;
          bUseTrueFalse = false;
        }
        else {
          if(bUseTrueFalse) {
            Debugutil.stop();     //what TODO
          }
          bLastWasAssignment = false;                      // add operator operand
          //
          //======>>>>>>>
          if(dbgStop)
            Debugutil.stop();
          boolean bOk = exprLeft.addOperand(exprRight, opPreced, part, genBool, mdl, nameInnerClassVariable, dbgStop); 
          if(!bOk) {                                       // addOperand makes also some type adaption.
            if(nrAllOperands == 0) {break; }               // first variable unknown, not necessary statement (time assignment etc.).
          }
          nrAllOperands +=1;
        }
      } //-------------------------------------------------- //for parts.
      //end::genExpressionParts[]
      //tag::genExpressionEnd[]
      //
      if( ! bLastWasAssignment) {                            // on assign it is written already in the string builder.
        boolean bTrueFalse = lastPart instanceof JavaSrc.ExprPartTrueFalse;
        exprLeft.fulfillNeedBool(bTrueFalse || genBool);
        //TODO necessary?
        if( (genBool || bTrueFalse) && exprLeft.exprType_.etype != VhdlExprTerm.ExprTypeEnum.booltype) {
          assert(false);
          if(nrAllOperands >1) { exprLeft.b.insert(0, "(").append(")");}
          exprLeft.b.append("='1'");                        // builds a boolean in VHDL
          exprLeft.exprType_.etype = VhdlExprTerm.ExprTypeEnum.booltype;
          exprLeft.exprType_.nrofElements = 1;
        }
        
        if(bTrueFalse) {
          String cond = exprLeft.b.toString();             // The exprLeft contains till now the condition term. 
          exprLeft.b.setLength(0);                         // clear exprLeft, use as destination
          VhdlExprTerm.ExprType assignType1 = assignType !=null ? assignType : exprLeft.exprType_;
          genTrueFalse(exprLeft.b, assignType1, cond, lastPart, mdl, nameInnerClassVariable, bInsideProcess, indent, assignTerm, dbgStop);
          exprLeft.exprType_.set(assignType1);             // The expression represents the trueFalseValue, with this type.   
          if(out !=null) { out.append(indent).append(exprLeft.b); }
        }
        else {
          if(out !=null) {                                     // produce output if given
            if(assignTerm !=null) {
              if(exprLeft.precedSegm  != J2Vhdl_Operator.operatorMap.get("=")) {
                out.append(assignTerm);                      // do not append the assignTerm on setBits.
              }                                              // setBits contains the assign term in the proper bit selection
              out.append(exprLeft.b).append(";");
            } else {
              out.append(exprLeft.b);
            }
          }
        }
      }
      exprLeft.nrOperands = nrAllOperands;                 // documents whether parenthesis may be necessary in a greater association.
    } catch(Throwable exc) {
      vhdlError("Exception: " + exc.getMessage(), exprRpn);
    }
    return exprLeft;      //on assign the assigned variable remains in the exprLeft. 
  }
  //end::genExpressionEnd[]
    
  
  
  private void genTrueFalse ( Appendable out, VhdlExprTerm.ExprType typeLeft, CharSequence cond, JavaSrc.ExprPart partTrueFalse_a
      , J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable
      , boolean bInsideProcess, CharSequence indent
      , CharSequence assignTerm, boolean dbgStop
      ) throws Exception {
    if(dbgStop)
      Debugutil.stop();
    JavaSrc.ExprPartTrueFalse partTrueFalse = (JavaSrc.ExprPartTrueFalse) partTrueFalse_a;
    StringBuilder sExprTrue = new StringBuilder();        // Note: assignTerm is the left side assignment variable with assign operator
    StringBuilder sExprFalse = new StringBuilder();
    VhdlExprTerm exprTrue = genExpression(null, partTrueFalse.get_trueExpr(), false, bInsideProcess, mdl, nameInnerClassVariable, null, null, typeLeft);
    VhdlExprTerm exprFalse = genExpression(null, partTrueFalse.get_falseExpr(), false, bInsideProcess, mdl, nameInnerClassVariable, null, null, typeLeft);
    if(!VhdlExprTerm.adjustType(sExprTrue, exprTrue.b, typeLeft, exprTrue.exprType_)) {
      J2Vhdl_GenExpr.vhdlError("non proper types in expression, ", partTrueFalse);
    }
    if(!VhdlExprTerm.adjustType(sExprFalse, exprFalse.b, typeLeft, exprFalse.exprType_)) {
      J2Vhdl_GenExpr.vhdlError("non proper types in expression, ", partTrueFalse);
    }
    //
    if(bInsideProcess) {                                   // inside of a process:
      //TODO use exprTrue, exprFalse but there is a nested problem, not evaluated yet.
      sExprTrue = new StringBuilder();        // Note: assignTerm is the left side assignment variable with assign operator
      sExprFalse = new StringBuilder();
      genExpression(sExprTrue, partTrueFalse.get_trueExpr(), false, true, mdl, nameInnerClassVariable, indent, assignTerm, typeLeft);
      genExpression(sExprFalse, partTrueFalse.get_falseExpr(), false, true, mdl, nameInnerClassVariable, indent, assignTerm, typeLeft);
      out.append("IF ").append(cond).append(" THEN ");
      out/*.append(assignTerm)*/.append(sExprTrue)/*.append(";")*/.append(indent).append("ELSE ");
      if(sExprFalse.charAt(0) == '\n') {
        sExprFalse.delete(0, 1);                    // delete \n which comes from a nested IF
      }
      out/*.append(assignTerm)*/.append(sExprFalse)/*.append(";")*/.append(" END IF;");
    }
    else {
      if(StringFunctions.startsWith(cond, "data_Data.cmd(13)='1'"))
        Debugutil.stop();
      //TODO maybe check of assignTerm-type and exprLeft, exprRight-Type and convert.
      if(assignTerm !=null) { out.append(indent).append(assignTerm); }
      out.append(sExprTrue).append(" WHEN ").append(cond).append(" ELSE ").append(sExprFalse);
      if(assignTerm !=null) { out.append(";"); }
    }
  }
  
  
  
  
  /**Associates the exprRight or the immediately value in part to the exprLeft which is a variable to assign. 
   * Whereby adjustes assignment type.
   * @param out The assignment is generated into here. Should be given. 
   *   This can be also the {@link VhdlExprTerm#b}, the buffer of the exprLeft.
   *   If it is not so, out will get the indentation and the content of the exprLeft.
   *   If it is so, the assign operator and the exprRightArg is stored there. 
   * @param exprLeft It contains the assign variable only. Due to operator precedence.
   * @param oper the assign operator
   * @param exprRight either complete term. or null
   * @param part if exprRight ==null then this is the value to assign from only one element.
   * @return the assigned variable only if it is a concatenated expression with assign in mid.
   * @throws Exception
   */
  VhdlExprTerm genAssignInExpression ( Appendable out, VhdlExprTerm exprLeft, J2Vhdl_Operator oper
    , VhdlExprTerm exprRightArg, JavaSrc.ExprPart part, JavaSrc.ExprPartTrueFalse partTrueFalse
    , J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable
    , CharSequence indent
    , boolean bInsideProcess
    , boolean dbgStopArg
    ) throws Exception {
    boolean dbgStop = dbgStopArg;
    VhdlExprTerm exprRight;
    if(StringFunctions.startsWith(exprLeft.b, "ringMstLo_Pin")) { dbgStop = true; }
    if(dbgStop) {
      Debugutil.stop();
    }
    if(exprRightArg !=null) {
      exprRight = exprRightArg;
    } else {
      exprRight = null;                  // create the right expression from the part.
      //--------------------------------------------       // check whether it is setBit(...) or setBits(...):
      JavaSrc.SimpleValue value = part.get_value();
      if(value !=null) {                                   // then the part should have a value - simple method call
        JavaSrc.SimpleMethodCall valueOper = value.get_simpleMethodCall();
        if(valueOper !=null) {
          JavaSrc.Reference ref = value.get_reference();   // ... with a reference "Fpga.
          if(ref !=null) {
            JavaSrc.SimpleVariable refVar = ref.get_referenceAssociation();
            if(refVar !=null && refVar.get_variableName().equals("Fpga")) { // static operation Fpga.setBit....?
              JavaSrc.ActualArguments args = valueOper.get_actualArguments();
              if(args !=null) {                            // both operations has actual arguments.
                Iterator<JavaSrc.Expression> iArgs = args.get_Expression().iterator();
                String operName = valueOper.get_methodName();
                int zOperName = operName.length();
                if(operName.startsWith("setBit") && zOperName <=7) {
                  final JavaSrc.Expression exprLeftVar = iArgs.next(); // Left variable both as first argument. 
                  if(dbgStop) { 
                    Debugutil.stop();
                  }
                  VhdlExprTerm variableExpr = genExprOnePart(exprLeftVar, mdl, nameInnerClassVariable);     // first argument variable
                  J2Vhdl_Variable leftVar = variableExpr.variable();
//                  assert(exprLeftVar.getSize_ExprPart() ==1);  // should contain the left assign variable.
//                  J2Vhdl_Variable leftVar = XXXgetVariableAccess(exprLeftVar.get_value(), mdl, nameInnerClassVariable);
                  assert(leftVar !=null);                  // the destination J2Vhdl_Variable for setBit or setBits.
                  if(exprLeft.variable() != leftVar) {
                    vhdlError("leftVar = setBit(leftVar, ... should be the same", exprLeft );
                    exprLeft.b.append( "??? ");
                  }
                  final JavaSrc.SimpleValue exprMsBit = iArgs.next().get_value();
                  int msBit = exprMsBit.get_intNumber();
                  exprLeft.b.append("(").append(Integer.toString(msBit));
                  //exprLeft.b.setLength(0);                 // exprLeft: replace the contained assign variable with the bit selection. 
                  if(zOperName == 7) {                     // setBits(var, msb, lsb, value, valuelsb
                    final JavaSrc.SimpleValue exprLsBit = iArgs.next().get_value();
                    int lsBit = exprLsBit.get_intNumber();
                    exprLeft.b.append(" DOWNTO ").append(Integer.toString(lsBit));
                    exprLeft.exprType_.nrofElements = msBit - lsBit +1;
                  } else {
                    if(exprLeft.exprType_.etype == VhdlExprTerm.ExprTypeEnum.bitVtype) {
                      exprLeft.exprType_.etype = VhdlExprTerm.ExprTypeEnum.bittype;
                    } else if(exprLeft.exprType_.etype == VhdlExprTerm.ExprTypeEnum.stdVtype) {
                      exprLeft.exprType_.etype = VhdlExprTerm.ExprTypeEnum.stdtype;
                    } else {
                      vhdlError("Warning: unexpected exprType", exprLeft);
                    }
                    exprLeft.exprType_.nrofElements = 1;
                  }
                  exprLeft.b.append(")");
                  exprRight = genExpression(null, iArgs.next(), false, bInsideProcess, mdl, nameInnerClassVariable, indent, " <= ", exprLeft.exprType_);
                  if(iArgs.hasNext()) {
                    final JavaSrc.SimpleValue exprLsBitValue = iArgs.next().get_value();
                    int lsBitValue = exprLsBitValue.get_intNumber();
                    if(lsBitValue !=0) {
                      Debugutil.stop(); 
                    }
                  }  
              } }
            }
          }
        }
      }
      if(exprRight ==null) {
        //======>>>>
        exprRight = VhdlExprTerm.genExprPartValue(part.get_value(), oper, false, mdl, nameInnerClassVariable, dbgStop);
        //if(varAssign !=null && varAssign == exprLeft.variable() && nrAllOperands ==2)
        if(exprRight !=null && exprRight.variable() == exprLeft.variable()) {  // assign of same variable as first check
          if(StringFunctions.equals(exprLeft.b, exprRight.b)) { // then check expressions equal, then not necessary in VHDL
            exprRight = null;
          }
        }
      }
    }
    if(exprRight !=null) {
      out.append(indent); 
      
      
      
      String sOpVhdl = oper.sVhdlVal;
      if(exprLeft.variable().location == J2Vhdl_Variable.Location.local) {
        sOpVhdl = " := ";
      }
      if(exprLeft.variable().name.equals("FpgaIO_SpeAcard_22_02.output.ringMstLo_Pin"))
        Debugutil.stop();
      J2Vhdl_Variable varAssign = exprLeft.variable();
      if(varAssign ==null) {
        return null;                                   // varTypeCurr is null on time variables (not known)
      }
      VhdlExprTerm.ExprType typeVar = exprLeft.exprType_; // varAssign.type;
      //note: following assertion is false on setBit..(...)
      //assert(exprLeft.exprType_.etype == typeVar.etype && exprLeft.exprType_.nrofElements == typeVar.nrofElements);
      //if(part instanceof JavaSrc.ExprPartTrueFalse) {
      if(partTrueFalse !=null) {
        exprRight.convertToBool();                         // need bool in IF() or WHEN()
        StringBuilder assignTerm = new StringBuilder(exprLeft.b);
        assignTerm.append(" ").append(sOpVhdl);
        genTrueFalse(out, typeVar, exprRight.b, partTrueFalse, mdl, nameInnerClassVariable, bInsideProcess, indent, assignTerm, dbgStop);
        if(out != exprLeft.b) { appendLineColumn(out, exprLeft); }
      }
      else if(typeVar.etype == VhdlExprTerm.ExprTypeEnum.stdtype && exprRight.exprType_.etype == VhdlExprTerm.ExprTypeEnum.bittype) {
        out.append(exprLeft.b).append(sOpVhdl).append("TO_STDULOGIC(").append(exprRight.b).append(");");
        appendLineColumn(out, exprLeft);
      }
      else if(typeVar.etype == VhdlExprTerm.ExprTypeEnum.bittype && exprRight.exprType_.etype == VhdlExprTerm.ExprTypeEnum.stdtype) {
        out.append(exprLeft.b).append(sOpVhdl).append("TO_BIT(").append(exprRight.b).append(");");
        appendLineColumn(out, exprLeft);
      }
      else if(typeVar.etype == VhdlExprTerm.ExprTypeEnum.stdVtype && exprRight.exprType_.etype == VhdlExprTerm.ExprTypeEnum.bitVtype) {
        out.append(exprLeft.b).append(sOpVhdl).append("TO_STDLOGICVECTOR(").append(exprRight.b).append(");");
        appendLineColumn(out, exprLeft);
      }
      else if(typeVar.etype == VhdlExprTerm.ExprTypeEnum.bitVtype && exprRight.exprType_.etype == VhdlExprTerm.ExprTypeEnum.stdVtype) {
        out.append(exprLeft.b).append(sOpVhdl).append("TO_BITVECTOR(").append(exprRight.b).append(");");
        appendLineColumn(out, exprLeft);
      }
      else if(typeVar.etype != exprRight.exprType_.etype 
          && exprRight.exprType_.etype != VhdlExprTerm.ExprTypeEnum.bitStdConst   //on const do nothing, it is ok BIT or STD_ is not distinguihed.
          && exprRight.exprType_.etype != VhdlExprTerm.ExprTypeEnum.bitStdVconst 
          
          ) {         // bool result to assign, it is comparison etc, not a boolean Java variable.
        //                                                 // the assign variable is usual BIT or STD_VECTOR, assign '1' or '0'.
        if(exprRight.exprType_.etype != VhdlExprTerm.ExprTypeEnum.booltype) { // it is boolean if fullfillNeedBool was executed and boolean was need. 
          exprRight.convertToBool();                   // complete to boolean if necessary but do nothing if not boolean and not boolean required
          //assert(false);
          //exprRight.convertToBool();                       //but now boolean is required, convert whole expression to boolean if not boolean
        }                                                  // it means both is necessary, fulfill for boolean exists and convert because boolean required
        if(bInsideProcess) {                                   //outside of a process:
          out.append("IF ").append(exprRight.b);
          out.append(" THEN ").append(exprLeft.b).append(" ").append(sOpVhdl).append(" '1'; ELSE ");
          out.append(exprLeft.b).append( " ").append(sOpVhdl).append(" '0'; END IF;");
        } else {
          out.append(exprLeft.b).append(" ").append(sOpVhdl).append(" '1' WHEN ");
          out.append(exprRight.b).append(" ELSE '0';");
        }
        appendLineColumn(out, exprLeft);
      }
      else {
        out.append(exprLeft.b).append(sOpVhdl).append(' ');
        out.append(exprRight.b);        // "+ @" right side expression used, it is the before prepared one.
        out.append(";");
        exprRight = null;   //was used.
        appendLineColumn(out, exprLeft);
      }
    }
    return exprLeft;
  }




  
  /**Used typical for the arguments of the Fpga.operation getBit etc.
   * @param expr argument value from parser.
   * @return prepared argument
   * @throws Exception
   */
  VhdlExprTerm genExprOnePart(JavaSrc.Expression expr, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable) throws Exception {
    if(expr.getSize_ExprPart()!=1) {
      vhdlError("The expression should only contain one part. Only the first part is used", expr);
    }
    VhdlExprTerm dstTerm = null;
    JavaSrc.ExprPart part = expr.get_ExprPart().iterator().next();
    dstTerm = VhdlExprTerm.genExprPartValue(part.get_value(), J2Vhdl_Operator.operatorMap.get("@"),  false, mdl, nameInnerClassVariable, false);
    return dstTerm;
  }
  
  


  
  
  
  
  
  
  
  
  
  
  
  
  final Map<String, String> idxBadOperations = new TreeMap<String, String>();
  { this.idxBadOperations.put("update", "bad");
    this.idxBadOperations.put("assert", "bad");
    this.idxBadOperations.put("stop", "bad");
  
  }
  
  
  
  GenOperation getBitsShR = new GenOperation() {
    @Override public J2Vhdl_Variable genOperation(final Iterator <JavaSrc.Expression> iArgs, VhdlExprTerm exprDst, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable) throws Exception {
      final JavaSrc.Expression exprLeftBit = iArgs.next();
      exprDst.b.append("(");
      genExpression(exprDst.b, exprLeftBit, false, false, mdl, nameInnerClassVariable, null, null, null);
      final JavaSrc.Expression exprLeftBitPos = iArgs.next();
      int leftBitPos = getIntFromExpr(exprLeftBitPos);
      final JavaSrc.Expression exprSrc = iArgs.next(); // it has 2 arguments, get first
      VhdlExprTerm variable = genExprOnePart(exprSrc, mdl, nameInnerClassVariable);     // first argument variable
      J2Vhdl_Variable descrVar = variable.variable();
      //J2Vhdl_Variable descrVar = XXXgetVariableAccess(exprSrc.get_value(), mdl, nameInnerClassVariable);
      if(descrVar == null) {
        vhdlError("variable not found: " + exprSrc.toString(), exprSrc);
      }
      assert(descrVar !=null);                             // ( 15 DOWNTO 1) if leftBitPos = 15 to shift 16 bits
      exprDst.b.append(") & ").append(descrVar.sElemVhdl);
      exprDst.b.append("(").append(Integer.toString(leftBitPos)).append(" DOWNTO 1)");
      exprDst.exprType_.set(descrVar.type);                   //The type comes from the variable which is accessed for shift bits.
      return descrVar;
  } };  
  
  
  GenOperation getBits = new GenOperation() {
    @Override public J2Vhdl_Variable genOperation(final Iterator <JavaSrc.Expression> iArgs, VhdlExprTerm exprDst, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable) throws Exception {
      final JavaSrc.Expression exprSrc = iArgs.next(); // it has 2 arguments, get first
      VhdlExprTerm srcTerm = genExpression(null, exprSrc, false, false, mdl, nameInnerClassVariable, "", null, null);
//      assert(exprSrc.getSize_ExprPart() ==1);
//      J2Vhdl_Variable descrVar = getVariableAccess(exprSrc.get_value());
//      assert(descrVar !=null);
      final JavaSrc.SimpleValue exprIndex1 = iArgs.next().get_value();
      final JavaSrc.SimpleValue exprIndex2 = iArgs.next().get_value();
      if(exprIndex1 ==null || exprIndex2 ==null) {
        vhdlError("getbits(ix, ix) needs two simple values as ix", exprSrc);
      }
      if(exprDst.exprType_.etype == VhdlExprTerm.ExprTypeEnum.undef) { exprDst.exprType_.set(srcTerm.exprType_); }
      exprDst.b.append(srcTerm.b).append("(").append(Integer.toString(exprIndex1.get_intNumber()))
         .append(" DOWNTO ").append(Integer.toString(exprIndex2.get_intNumber())).append(")");
      return null;
    } };  
  
  
  GenOperation concatbits = new GenOperation() {
    @Override public J2Vhdl_Variable genOperation(final Iterator <JavaSrc.Expression> iArgs, VhdlExprTerm exprDst, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable) throws Exception {
      //StringBuilder arg1 = new StringBuilder(20);
      final JavaSrc.Expression exprLeft = iArgs.next(); // it has 2 arguments, get first
      final JavaSrc.Expression exprNrBits = iArgs.next(); // it has 2 arguments, get first
      final JavaSrc.Expression exprRight = iArgs.next(); // it has 2 arguments, get first
      if(exprLeft ==null || exprRight ==null) {
        vhdlError("concatbits(left, bits, right) needs two expressions", exprLeft);
      }
      VhdlExprTerm exprLeftPart = genExpression(exprDst.b, exprLeft, false, false, mdl, nameInnerClassVariable, null, null, null);
      exprDst.b.append(" & ");
      VhdlExprTerm exprRightPart = genExpression(exprDst.b, exprRight, false, false, mdl, nameInnerClassVariable, null, null, null);
      if(  exprLeftPart.exprType_.etype == VhdlExprTerm.ExprTypeEnum.bitVtype || exprLeftPart.exprType_.etype == VhdlExprTerm.ExprTypeEnum.bittype
        || exprRightPart.exprType_.etype == VhdlExprTerm.ExprTypeEnum.bitVtype || exprRightPart.exprType_.etype == VhdlExprTerm.ExprTypeEnum.bittype) {
        exprDst.exprType_.etype = VhdlExprTerm.ExprTypeEnum.bitVtype;  // maybe bitStdVconst for one of left or right, hence test all
      } else {
        exprDst.exprType_.etype = VhdlExprTerm.ExprTypeEnum.stdVtype;
      }
      return null;  //TODO exprLeft.
  } };  
  
  
  GenOperation getBitsShL = new GenOperation() {
    @Override public J2Vhdl_Variable genOperation(final Iterator <JavaSrc.Expression> iArgs, VhdlExprTerm exprDst, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable) throws Exception {
      final JavaSrc.Expression exprSrc = iArgs.next(); // it has 2 arguments, get first
      VhdlExprTerm variable = genExprOnePart(exprSrc, mdl, nameInnerClassVariable);     // first argument variable
      J2Vhdl_Variable descrVar = variable.variable();
      //J2Vhdl_Variable descrVar = XXXgetVariableAccess(exprSrc.get_value(), mdl, nameInnerClassVariable);
      assert(descrVar !=null);                         // ( 15 DOWNTO 0) if nrofBits = 17 for ex. vector(16 DOWNTO 0)
      exprDst.b.append(descrVar.sElemVhdl);
      final JavaSrc.Expression exprNrBitsShift = iArgs.next();
      int nrBitsShift = getIntFromExpr(exprNrBitsShift);
      exprDst.b.append("(").append(Integer.toString(nrBitsShift-1)).append(" DOWNTO 0) & (");
      final JavaSrc.Expression exprRightBit = iArgs.next();
      genExpression(exprDst.b, exprRightBit, false, false, mdl, nameInnerClassVariable, null, null, null);
      exprDst.b.append(")");
      exprDst.exprType_.set(descrVar.type);                   //The type comes from the variable which is accessed for shift bits.
      return descrVar;
  } };  
  
  
  /**Operation getBit(var, 5) adds "var(5)" to exprDst.
   * If the exprDst is a boolean one, then "var(5)='1'" is produced to add also a boolean. 
   * 
   */
  GenOperation getBit = new GenOperation() {
    @Override public J2Vhdl_Variable genOperation(final Iterator <JavaSrc.Expression> iArgs, VhdlExprTerm exprDst, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable) throws Exception {
      
      VhdlExprTerm.ExprType type0 = exprDst.exprType_;
      final JavaSrc.Expression exprSrc = iArgs.next();     // it has 2 arguments, get first
      VhdlExprTerm variable = genExprOnePart(exprSrc, mdl, nameInnerClassVariable);     // first argument variable
      exprDst.b.append(variable.b).append("(");
      final JavaSrc.Expression exprIndex = iArgs.next();
      VhdlExprTerm bitIndex = genExprOnePart(exprIndex, mdl, nameInnerClassVariable);
      exprDst.b.append(bitIndex.b).append(")");
      if(type0.etype == VhdlExprTerm.ExprTypeEnum.undef) { 
        if(variable.exprType_.etype == VhdlExprTerm.ExprTypeEnum.bitVtype) {
          exprDst.exprType_.etype = VhdlExprTerm.ExprTypeEnum.bittype;
          exprDst.exprType_.nrofElements = 1;
        }
        else if(variable.exprType_.etype == VhdlExprTerm.ExprTypeEnum.stdVtype) {
          exprDst.exprType_.etype = VhdlExprTerm.ExprTypeEnum.stdtype;
          exprDst.exprType_.nrofElements = 1;
        } else {
          exprDst.exprType_.etype = VhdlExprTerm.ExprTypeEnum.undef;                            // type from the vector expression.
        }
      }
      return null;  //TODO type
  } };  
  
  
  
  GenOperation emptyOperation = new GenOperation() {
    @Override public J2Vhdl_Variable genOperation(final Iterator <JavaSrc.Expression> iArgs, VhdlExprTerm exprDst, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable) throws Exception {
      return null;
  } };  
  
  
  Map<String, GenOperation> idxFpgaOperations = new TreeMap<String, GenOperation>();
  {
    this.idxFpgaOperations.put("getBit", this.getBit);
    this.idxFpgaOperations.put("getBits", this.getBits);
    this.idxFpgaOperations.put("getBitsShL", this.getBitsShL);
    this.idxFpgaOperations.put("getBitsShR", this.getBitsShR);
    this.idxFpgaOperations.put("concatbits", this.concatbits);
    this.idxFpgaOperations.put("measTime", this.emptyOperation);
    this.idxFpgaOperations.put("checkTime", this.emptyOperation);
  }
  
  
  
  
  
  public static int getIntFromExpr(JavaSrc.Expression expr) {
    JavaSrc.SimpleValue exprVal = expr.get_value();
    int val = exprVal.get_intNumber();
    if (val !=0) return val;
    else { 
      val = exprVal.get_hexNumber(); 
      return val;
    }
  }
  

  
  static void vhdlError(String msg, SrcInfo srcInfo) {
    System.err.println(msg + " in " + srcInfo.getFileName() + " line: " + srcInfo.getLine());
  }
  
  
  /**Inform which class is processed for build the names.
   * @param nameInnerClass The name of the PROCESS building instance, (usual same as inner class name, lower case).
   * @param nameModule The name of the module (main class of Java file, but not the type name, the instance name from a Module class).
   *   This information can only be given from enrollment outside. 
   */
  public void setInnerClass(String nameInnerClassVariable, String nameModule) {
    //This is no more necessary because the context information are given as calling arguments.
    //this.nameInnerClassVariable = nameInnerClassVariable;
    //mdl.nameInstance = nameModule;
  }
  
  
  
  
  
  /**Gather or variable of this module instance for common usage.
   * @param nameModule Name of the instance of the clazz. Not the type name.
   * @param nameOuterClass The name of the module, it is the only one class on file level of a src.java
   * @param clazz parse result of a src.java for the only one file level class.
   * @return {@link #idxVars} useable for JZtxtcmd if necessary.
   */
  public Map<String, J2Vhdl_Variable> mapVariables(String nameModule, String nameOuterClass, JavaSrc.ClassDefinition clazz) {
    final String className = clazz.get_classident();   // An inner class of the Java module class
    final String varClassName = Character.toLowerCase(className.charAt(0)) + className.substring(1);
    final String sRecVhdl = nameModule == null ? className : nameModule + "_" + className + ".";
    final String sObjJava = nameModule == null ? className : nameModule + "." + varClassName + ".";
    final String namefulliClass = nameOuterClass == null ? className : nameOuterClass + "_" + className;
    final J2Vhdl_Variable.Location location = J2Vhdl_Variable.Location.record;
    //
    JavaSrc.ClassContent clazzC = clazz.get_classContent();
    if(clazzC.getSize_variableDefinition() >0) {
      for( JavaSrc.VariableInstance varzp: clazzC.get_variableDefinition()) {
        createVariable(varzp, location, sRecVhdl, sObjJava, namefulliClass, this.fdata.idxVars, this.fdata.idxRecordVars);
    } }
    return this.fdata.idxVars;
  }


  /**Gather or variable of this module instance for common usage.
   * @param nameModule Name of the instance of the clazz. Not the type name.
   * @param nameOuterClass The name of the module, it is the only one class on file level of a src.java
   * @param clazz parse result of a src.java for the only one file level class.
   * @return {@link #idxVars} useable for JZtxtcmd if necessary.
   */
  public Map<String, J2Vhdl_Variable> mapInOutVariables(JavaSrc.ClassDefinition clazz, J2Vhdl_ModuleInstance mdl) {
//    final String className = clazz.get_classident();   // An inner class of the Java module class
//    final String varClassName = Character.toLowerCase(className.charAt(0)) + className.substring(1);
    final String sRecVhdl = "";
//    final String sObjJava = varClassName + ".";
//    final String namefulliClass = className;
    //
    JavaSrc.ClassContent clazzC = clazz.get_classContent();
    for( JavaSrc.VariableInstance varzp: clazzC.get_variableDefinition()) {
      //createVariable(varzp, sRecVhdl, sObjJava, namefulliClass, this.fdata.idxVars);
      createVariable(varzp, J2Vhdl_Variable.Location.record, sRecVhdl, mdl.nameInstance + ".", mdl.nameInstance, this.fdata.idxVars, this.fdata.idxRecordVars);
    }
    return this.fdata.idxVars;
  }



  
  static J2Vhdl_Variable createVariable ( JavaSrc.VariableInstance varzp
      , J2Vhdl_Variable.Location location
      , String sRecVhdl, String sObjJava, String namefulliClass
      , Map<String, J2Vhdl_Variable> dstIx, Map<String, J2Vhdl_Variable> dstTypeIx) {
    String name = varzp.get_variableName();
    if(name.equals("state"))
      Debugutil.stop();
    if( ! name.equals("_time_") && ! name.startsWith("m_") && !name.startsWith("time_") && ! name.equals("time")) {
      JavaSrc.Type typezp = varzp.get_type();
      String sNrBits = null;
      final VhdlExprTerm.ExprType eType = new VhdlExprTerm.ExprType();
      JavaSrc.ModifierVariable modzp = varzp.get_ModifierVariable();
      boolean bTypeAnnot = false;
      if(modzp !=null && modzp.getSize_Annotation() >0) //...for
      for(String annot: modzp.get_Annotation()) {          // The type of int or long is clarified with a Annotation
        if(annot !=null) {
          if(annot.startsWith("Fpga.BITVECTOR")) {
            eType.etype = VhdlExprTerm.ExprTypeEnum.bitVtype;
            int pos1 = annot.indexOf('(')+1;        // after "
            int pose = annot.lastIndexOf(')');      // this characters should be present by Java syntax check.
            sNrBits = annot.substring(pos1, pose).trim();
            eType.nrofElements = Integer.parseInt(sNrBits);
            bTypeAnnot = true;
          }
          else if(annot.startsWith("Fpga.STDVECTOR")) {
            eType.etype = VhdlExprTerm.ExprTypeEnum.stdVtype;
            int pos1 = annot.indexOf('(')+1;        // after "
            int pose = annot.lastIndexOf(')');      // this characters should be present by Java syntax check.
            sNrBits = annot.substring(pos1, pose).trim();
            eType.nrofElements = Integer.parseInt(sNrBits);
            bTypeAnnot = true;
          }
          else if(annot.startsWith("Fpga.STD_LOGIC")) {
            eType.etype = VhdlExprTerm.ExprTypeEnum.stdtype;
            eType.nrofElements = 0;
            bTypeAnnot = true;
          }
          else {
            J2Vhdl_GenExpr.vhdlError("faulty annotation: " + annot, varzp);
            eType.etype = VhdlExprTerm.ExprTypeEnum.bitVtype;
            sNrBits = "32";
            eType.nrofElements = 32;
            bTypeAnnot = true;
          }
        }
      }// for annotation
      if(!bTypeAnnot) {                                    // Annotation not given:
        String sTypeJava = typezp.get_name();
        if(sTypeJava.equals("boolean")) {
          eType.etype = VhdlExprTerm.ExprTypeEnum.bittype; // boolean is BIT 
        } else if(sTypeJava.equals("char")) {
          eType.etype = VhdlExprTerm.ExprTypeEnum.stdtype; // char is STD_ with the known possible values.
        } else if(sTypeJava.equals("short") || sTypeJava.equals("int") || sTypeJava.equals("long") || sTypeJava.equals("byte")){
          eType.etype = VhdlExprTerm.ExprTypeEnum.inttype; // all integer numeric types are  INTEGER.
        } else {
          vhdlError("Variable type not supported in J2VHDL: " + sTypeJava, varzp);
        }
      }
      final String sElemVhdl = sRecVhdl==null ? name :  sRecVhdl + name;
      final String sElemJava = sObjJava +name;
      final String sVarName = sObjJava.length() >0 ? sObjJava : name;
      final int nrBits = sNrBits == null? 0 : Integer.parseInt(sNrBits);
      final String nameInClass;
      if(namefulliClass ==null) {                          // local process varibale
        nameInClass = name;
      } else {
        nameInClass = namefulliClass + "." + name;
      }
      final J2Vhdl_Variable var = new J2Vhdl_Variable(sVarName, location, eType, nrBits, sElemJava, sElemVhdl);
      dstIx.put(sElemJava, var);                  // access to var from a process
      if(dstTypeIx !=null) {
        dstTypeIx.put(nameInClass, var);            // access to var for Record definition
      }
      return var;
    }  
    else { return null; }   //not supported variable, time etc.
  }

  
  
  
  public String assembleType(JavaSrc.VariableInstance var, String nameInnerClassVariable) {
//      final JavaSrc.Type type = var.get_type();
//      final String typeName = type.get_name();
      final String varName = var.get_variableName();
      final String sElemJava = nameInnerClassVariable + "." + varName;
      J2Vhdl_Variable varDescr = this.fdata.idxProcessVars.get(sElemJava);
      if(varDescr == null) {
        varDescr = this.fdata.idxRecordVars.get(sElemJava);
      }
      if(varDescr ==null) {
        Debugutil.stop();
        return null;
      }
      if(varDescr.type.etype == VhdlExprTerm.ExprTypeEnum.bittype) { //typeName.equals("boolean")) {
        return "BIT";
      } else if(varDescr.type.etype == VhdlExprTerm.ExprTypeEnum.bitVtype) { //typeName.equals("boolean")) {
          return "BIT_VECTOR(" + (varDescr.nrBits-1) + " DOWNTO 0)";
      } else if(varDescr.type.etype == VhdlExprTerm.ExprTypeEnum.stdVtype) { //typeName.equals("boolean")) {
        return "STD_LOGIC_VECTOR(" + (varDescr.nrBits-1) + " DOWNTO 0)";
      } else { // if(typeName.equals("int")) {
        //JavaSrc.VariableInstance maskVar = variables.get("m_"+ varName);
        //String sBitRange = varDescr.downto;
  //      String varAnnotation = null; //var.get_Annotation();
  //      if(varAnnotation !=null && varAnnotation.startsWith("BITRANGE")) {
  //        int posVal = varAnnotation.indexOf('(') +1;
  //        int posValEnd = varAnnotation.indexOf(')');   //is contained, elsewhere Java compiling error.
  //        sBitRange = varAnnotation.substring(posVal, posValEnd);
  //      } else {
  //        sBitRange = "31 DOWNTO 0";
  //      }
        Debugutil.stop();
        //int mask = DataAccess.access(varName, var, false, false, false, null)
        return "STD_LOGIC_VECTOR( 31 DOWNTO 0 )";
      }
  //    else {
  //      return "??unknownType??";
  //    }
    }
  

  
  
  /**Creates all local variables in the ctor as PROCESS variables.
   * @param wOut VHDL file writer
   * @param ctor from a inner class which is a process
   * @return The Map of loval variables
   * @throws IOException
   */
  public Map<String, J2Vhdl_Variable> createProcessVar(Appendable wOut, JavaSrc.GetStatement_ifc ctor) throws IOException {
    //Map<String, J2Vhdl_Variable> mapret = null;
    for(JavaSrc.Statement stmnt: ctor.get_statement()) {                   // all first level statements in the ctor
      if(stmnt.get_variableDefinition()!=null) {
        for(JavaSrc.VariableInstance vdef: stmnt.get_variableDefinition()) {
          J2Vhdl_Variable var = createVariable(vdef, J2Vhdl_Variable.Location.local, "", "", null, this.fdata.idxProcessVars, null);
          if(var !=null) {
            wOut.append("\n  VARIABLE ").append(var.name).append(" : ").append(var.getVhdlType()).append(";");
          }
//          String varName = vdef.get_variableName();
//          if( !varName.equals("_time_") && !varName.startsWith("m_")) {
//            final JavaSrc.Type type = vdef.get_type();
//            final String typeName = type.get_name();
//            final String sVhdlType;
//            final VhdlExprTerm.ExprType etype;
//            if(typeName.equals("boolean")) {
//              sVhdlType = "BIT";
//              etype = VhdlExprTerm.ExprTypeEnum.bittype;
//            }
//            else {
//              sVhdlType = "??" + typeName + "??";
//              etype = VhdlExprTerm.ExprTypeEnum.bittype;
//            }
//            J2Vhdl_Variable var = new J2Vhdl_Variable(varName, true, etype, 1, varName, varName);
//            this.fdata.idxProcessVars.put(varName, var);
//          }
        }
      }
      Debugutil.stop();
    }
    
    return null;
  }
  
  
  public void cleanProcessVar() {
    this.fdata.idxProcessVars.clear();
  }
  
  
  
}
