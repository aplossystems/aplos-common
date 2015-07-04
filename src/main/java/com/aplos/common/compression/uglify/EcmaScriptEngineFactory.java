package com.aplos.common.compression.uglify;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class EcmaScriptEngineFactory {
	public static ScriptEngine getECMAScriptEngine(){
		ScriptEngine engine = null; 
		ScriptEngineManager manager = new ScriptEngineManager();
		
		engine = manager.getEngineByExtension("js");
		
		if(engine == null){		
			throw new RuntimeException("the java version do not install ECMAScipt engine, must be above java 1.6");
		}
		
		return engine;
	}
}
