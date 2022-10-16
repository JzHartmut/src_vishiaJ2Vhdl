package org.vishia.java2Vhdl;

import org.vishia.java2Vhdl.parseJava.JavaSrc;
import org.vishia.util.Debugutil;

public class J2Vhdl_GenStmnt {

  
  
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
  
  public CharSequence genStatement ( JavaSrc.Statement stmnt, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable, int indent, boolean bInsideProcess) throws Exception {
    StringBuilder out = new StringBuilder(2400);
    genStmnt(out, stmnt, mdl, nameInnerClassVariable, indent, bInsideProcess);
    return out;
  }
  
  
  
  
  final String indents = "\n                                                  ";

  void genStmntBlock ( StringBuilder out, JavaSrc.StatementBlock stblk, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable, int indent, boolean bInsideProcess) throws Exception {
    Debugutil.stop();
    Iterable<JavaSrc.Statement> stmnts = stblk.get_statement();
    if(stmnts !=null) {
      for(JavaSrc.Statement stmnt : stmnts) {
        genStmnt(out, stmnt, mdl, nameInnerClassVariable, indent+1, bInsideProcess);
      }
    }
  }
  
  
  
  
  
  void genStmnt ( StringBuilder out, JavaSrc.Statement stmnt, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable, int indent, boolean bInsideProcess) throws Exception {
    JavaSrc.If_statement ifStmnt = stmnt.get_if_statement();
    //VhdlExprTerm exprDst = new VhdlExprTerm( out, J2Vhdl_Operator.operatorMap.get("@"), VhdlExprTerm.ExprTypeEnum.undef, 0, this);
    if(ifStmnt !=null) {
      out.append(this.indents.substring(0, 2*indent+1)).append("IF ");
      JavaSrc.Expression cond = ifStmnt.get_Expression();
      genCondition(out, cond, mdl, nameInnerClassVariable);
      out.append(" THEN");
      JavaSrc.Statement stmnt1 = ifStmnt.get_statement();
      if(stmnt1 !=null) { genStmnt(out, stmnt1, mdl, nameInnerClassVariable, indent+1, bInsideProcess); }
      JavaSrc.Statement stmnt2 = ifStmnt.get_elseStatement();
      if(stmnt2 !=null) { 
        out.append(this.indents.substring(0, 2*indent+1)).append("ELSE");
        genStmnt(out, stmnt2, mdl, nameInnerClassVariable, indent+1, bInsideProcess); 
      }
      out.append(this.indents.substring(0, 2*indent+1)).append("END IF;");
      genExpr.appendLineColumn(out, ifStmnt);
    }
    else if(  stmnt.get_statementBlock() !=null) {
      genStmntBlock(out, stmnt.get_statementBlock(), mdl, nameInnerClassVariable, indent, bInsideProcess);
    }
    else if(  stmnt.get_Expression() !=null) {
      genAssignment(out, stmnt.get_Expression(), mdl, nameInnerClassVariable, indent, bInsideProcess);
    }
    else if( stmnt.get_variableDefinition() !=null) {
      for(JavaSrc.VariableInstance vdef: stmnt.get_variableDefinition()) {
        //the variable itself is already defined. 
        JavaSrc.Expression expr = vdef.get_Expression();
        if(expr !=null) {
          genAssignment(out, expr, mdl, nameInnerClassVariable, indent, bInsideProcess);
//          out.append(this.indents.substring(0, 2*indent+1)).append(vdef.get_variableName()).append(" := ");
//          genExpression(out, expr, false);
        }
      }
    }
    else {
      out.append(this.indents.substring(0, 2*indent+1)).append("  --unknown statement");
    }
  }


  
  VhdlExprTerm genAssignment ( Appendable out, JavaSrc.Expression asgn, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable, int indent, boolean bInsideProcess) throws Exception {
  
    return this.genExpr.genExpression(out, asgn, false, bInsideProcess, mdl, nameInnerClassVariable, this.indents.substring(0, 2*indent+1), null, null);
//    if(b !=null) { 
//      b = StringFunctions_B.removeLeadingWhiteSpaces(b);
//      if(b.length() >0) {                               // it is null if the same variable from z is assigned to this. 
//        out.append(indents.substring(0, 2*indent+1)).append(b).append(";");
//      }
//    }
  }
  
  
  private void genCondition(Appendable out, JavaSrc.Expression cond, J2Vhdl_ModuleInstance mdl, String nameInnerClassVariable) throws Exception {
    Debugutil.stop();
    //JavaSrc.SimpleValue value = cond.get_value();
    this.genExpr.genExpression(out, cond, true, false, mdl, nameInnerClassVariable, null, null, null);
  }


}
