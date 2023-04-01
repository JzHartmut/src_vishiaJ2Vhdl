package org.vishia.java2Vhdl;

import org.vishia.java2Vhdl.parseJava.JavaSrc;
import org.vishia.util.Debugutil;

public class J2Vhdl_GenStmnt {

  /**Version, history and license.
   * <ul>
   * <li>2023-04-01 sTimeGroup forwarded, genStatement(...) returns true if has contributed,
   *   for time Group detection. 
   * <li>2023-03-30 genStmnt(...) now accepts and ignores a <code>new throw ...</code> effective for Java test, 
   *   not to use for Java2Vhdl. (Before change a value "0" was generated because of the string expression in the throw statement.) 
   * <li>2023-03-28 {@link #genStmnt(StringBuilder, org.vishia.java2Vhdl.parseJava.JavaSrc.Statement, J2Vhdl_ModuleInstance, String, int, boolean)}
   *   Now a variable definition initial assignment is translated correct. 
   * <li>2022-10-16 Hartmut created, only formal refactoring: 
   * Now J2Vhdl_GenExpr and J2Vhdl_GenStmnt divided from GenStmntExpr
   * With them the statement generation and expression generation is well able to distinguish,  an overdue refactoring.
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
  public final static String sVersion = "2023-04-01"; 

  
  J2Vhdl_GenExpr genExpr = J2Vhdl_GenExpr.d;  //access to the singleton
  
  
  /**Search the appropriate ctor of the given class which is designated with @{@link Fpga.VHDL_PROCESS}
   * @param clazz from this inner class
   * @return the parse result for that.
   */
  public JavaSrc.ConstructorDefinition getCtorProcess ( JavaSrc.ClassDefinition clazz, String nameInnerClassVariable) {
    String namePrc = clazz.get_classident();     // name of the process
    //this.nameInnerClassVariable = Character.toLowerCase(namePrc.charAt(0))+ namePrc.substring(1);
    JavaSrc.ClassContent clazzC = clazz.get_classContent();
    if(clazzC.getSize_constructorDefinition()>0) {
      for(JavaSrc.ConstructorDefinition ctor: clazzC.get_constructorDefinition()) {
        JavaSrc.ModifierMethod modif = ctor.get_ModifierMethod();
        if(modif !=null) {
          String annot = modif.get_Annotation();
          if(annot !=null && annot.equals("Fpga.VHDL_PROCESS")) {
//            Class<?>[] classes = this.classMdl.getDeclaredClasses();        // search the class in the Java module Class
//            for(Class<?> class1: classes) {
//              if(class1.getSimpleName().equals(namePrc)) { // found the inner class as java.lang.Class
//                classPrc = class1;
//                break;
//              }
//            }
//            for(JavaSrc.Statement stmnt: ctor.get_statement()) {
//              Debugutil.stop();
//            }
            return ctor;
          }
        }
    } }
    return null;
  }
  
  public CharSequence genStatement ( JavaSrc.Statement stmnt, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable, int indent, boolean bInsideProcess, String[] sTimeGroup) throws Exception {
    StringBuilder out = new StringBuilder(2400);
    genStmnt(out, stmnt, mdl, nameInnerClassVariable, indent, bInsideProcess, sTimeGroup);
    return out;
  }
  
  
  
  
  final String indents = "\n                                                  ";

  void genStmntBlock ( StringBuilder out, JavaSrc.StatementBlock stblk, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable, int indent, boolean bInsideProcess) throws Exception {
    Debugutil.stop();
    Iterable<JavaSrc.Statement> stmnts = stblk.get_statement();
    if(stmnts !=null) {
      for(JavaSrc.Statement stmnt : stmnts) {
        genStmnt(out, stmnt, mdl, nameInnerClassVariable, indent+1, bInsideProcess, null);
      }
    }
  }
  
  
  
  
  
  /**Genrates the VHDL statement from the given Java statement. 
   * @param out to this buffer
   * @param stmnt given statement
   * @param mdl the module instance 
   * @param nameInnerClassVariable 
   * @param indent
   * @param bInsideProcess true then statements in a Fpga.VHDL_PROCESS
   * @param sTimeGroup not null for the first statement in a process. 
   *   Then it may be filled if the first statement if a if( ... access to CeTime_ifc)
   * @return true if an assignment to a not local variable is done. Uses for detect if(..ce()..) for time Groups.
   * @throws Exception
   */
  boolean genStmnt ( StringBuilder out, JavaSrc.Statement stmnt, J2Vhdl_ModuleInstance mdl
      , String nameInnerClassVariable, int indent, boolean bInsideProcess, String[] sTimeGroup) throws Exception {
    boolean bGenerated = true;
    JavaSrc.If_statement ifStmnt = stmnt.get_if_statement();
    //VhdlExprTerm exprDst = new VhdlExprTerm( out, J2Vhdl_Operator.operatorMap.get("@"), VhdlExprTerm.ExprTypeEnum.undef, 0, this);
    if(ifStmnt !=null) {
      out.append(this.indents.substring(0, 2*indent+1)).append("IF ");
      JavaSrc.Expression cond = ifStmnt.get_Expression();
      genCondition(out, cond, mdl, nameInnerClassVariable, sTimeGroup);
      out.append(" THEN");
      JavaSrc.Statement stmnt1 = ifStmnt.get_statement();
      if(stmnt1 !=null) { genStmnt(out, stmnt1, mdl, nameInnerClassVariable, indent+1, bInsideProcess, null); }
      JavaSrc.Statement stmnt2 = ifStmnt.get_elseStatement();
      if(stmnt2 !=null) { 
        out.append(this.indents.substring(0, 2*indent+1)).append("ELSE");
        genStmnt(out, stmnt2, mdl, nameInnerClassVariable, indent+1, bInsideProcess, null); 
      }
      out.append(this.indents.substring(0, 2*indent+1)).append("END IF;");
      genExpr.appendLineColumn(out, ifStmnt);
    }
    else if(  stmnt.get_statementBlock() !=null) {
      genStmntBlock(out, stmnt.get_statementBlock(), mdl, nameInnerClassVariable, indent, bInsideProcess);
    }
    else if( stmnt.get_exceptionClass() !=null) {
      // it is a throw new(Exception), do not create content in VHDL, it is for test.
      bGenerated = false;
    }
    else if(  stmnt.get_Expression() !=null) {
      VhdlExprTerm term = genAssignment(out, stmnt.get_Expression(), mdl, nameInnerClassVariable, indent, bInsideProcess);
      bGenerated = term.b.length() >0;
    }
    else if( stmnt.get_variableDefinition() !=null) {
      for(JavaSrc.VariableInstance vdef: stmnt.get_variableDefinition()) {
        //the variable itself is already defined. 
        JavaSrc.Expression expr = vdef.get_Expression();
        if(expr !=null) {                        // The expression does not contain the left variable, it is the vdef
          VhdlExprTerm term = genAssignment(null, expr, mdl, nameInnerClassVariable, indent, bInsideProcess);
          if(term.b.length() >0) {
            out.append(this.indents.substring(0, 2*indent+1)).append(vdef.get_variableName()).append(" := ")
               .append(term.b).append(";");
          } else {
            bGenerated = false;
          }
        }
      }
      bGenerated = false;  // no assignments to PROCESS variable.
    }
    else {
      out.append(this.indents.substring(0, 2*indent+1)).append("  --unknown statement");
    }
    return bGenerated;
  }


  
  VhdlExprTerm genAssignment ( Appendable out, JavaSrc.Expression asgn, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable, int indent, boolean bInsideProcess) throws Exception {
  
    return this.genExpr.genExpression(out, asgn, false, bInsideProcess, mdl, nameInnerClassVariable, this.indents.substring(0, 2*indent+1), null, null, null);
//    if(b !=null) { 
//      b = StringFunctions_B.removeLeadingWhiteSpaces(b);
//      if(b.length() >0) {                               // it is null if the same variable from z is assigned to this. 
//        out.append(indents.substring(0, 2*indent+1)).append(b).append(";");
//      }
//    }
  }
  
  
  private void genCondition(Appendable out, JavaSrc.Expression cond, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable, String[] sTimeGroup) throws Exception {
    Debugutil.stop();
    //JavaSrc.SimpleValue value = cond.get_value();
    this.genExpr.genExpression(out, cond, true, false, mdl, nameInnerClassVariable, null, null, null, sTimeGroup);
  }


}
