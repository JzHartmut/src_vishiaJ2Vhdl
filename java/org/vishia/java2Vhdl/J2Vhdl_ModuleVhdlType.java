package org.vishia.java2Vhdl;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.java2Vhdl.parseJava.JavaSrc;
import org.vishia.java2Vhdl.parseJava.JavaSrc.ClassDefinition;

public class J2Vhdl_ModuleVhdlType extends J2Vhdl_ModuleType {

  
  /**If the type contains an <code>Input</code> inner class, its variables. */
  List<J2Vhdl_Variable> inputs = new LinkedList<J2Vhdl_Variable>();
  
  /**If the type contains an <code>Output</code> inner class, its variables. */
  List<J2Vhdl_Variable> outputs = new LinkedList<J2Vhdl_Variable>();
  
  /**If the type contains an <code>Input</code> or <code>Output</code> inner class, its variables. */
  Map<String, J2Vhdl_Variable> idxIOVars = new TreeMap<String, J2Vhdl_Variable>();

  /**List of variables in the definition order. */
  List<J2Vhdl_Variable> io = new LinkedList<J2Vhdl_Variable>();
  


  
  
  public J2Vhdl_ModuleVhdlType(String nameType, JavaSrc javaSrc, ClassDefinition moduleClass
    , boolean isTopLevel) {
    super(nameType, javaSrc, moduleClass, isTopLevel);
  }

  
  
  List<J2Vhdl_Variable> createInputs ( ) { 
    return this.inputs; 
  } 
  
  List<J2Vhdl_Variable> createOutputs ( ) { 
    return this.outputs; 
  } 

  
  public static class Assgn {
    public final String name, assgn;

    public Assgn(String name, String assgn) {
      this.name = name;
      this.assgn = assgn;
    }
    
  }
  
}
