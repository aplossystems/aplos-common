package com.aplos.common.compression.uglify;

import java.io.InputStreamReader;
import java.io.Reader;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.aplos.common.utils.ApplicationUtil;

public class UglifyJs {

	public Reader getResourceReader(String url) {

		Reader reader = null;
		
		try {
			if(reader == null) {
				reader = new InputStreamReader(getClass().getResourceAsStream(url));
			}
		} catch (Exception e) {
			ApplicationUtil.handleError(e);
		}

		return reader;
	}

	public void exec(String[] args) {

		ScriptEngine engine = EcmaScriptEngineFactory.getECMAScriptEngine();
		engine.put("uglify_args", args);
		engine.put("uglify_no_output", false);
		run(engine);
		
		try {
			engine.eval("uglify();");
		} catch (ScriptException e) {
			e.printStackTrace();
		}
	}
	
	
	private void run(ScriptEngine engine) {
		try {
			String filePathRoot = "/com/aplos/common/compression/uglify/";
			
			Reader parsejsReader = getResourceReader( filePathRoot + "javascript/parse-js.js");
			Reader processjsReader = getResourceReader( filePathRoot + "javascript/process.js");
			Reader sysjsReader = getResourceReader( filePathRoot + "javascript/adapter/sys.js");
			Reader jsonjsReader = getResourceReader( filePathRoot + "javascript/adapter/JSON.js");
			Reader arrayjsReader = getResourceReader( filePathRoot + "javascript/adapter/Array.js");
			Reader uglifyjsReader = getResourceReader( filePathRoot + "javascript/uglifyjs.js");

			engine.eval(arrayjsReader);
			engine.eval(sysjsReader);
			engine.eval(parsejsReader);
			engine.eval(processjsReader);
			engine.eval(jsonjsReader);
			engine.eval(uglifyjsReader);

		} catch (ScriptException e) {
			ApplicationUtil.handleError(e);
		}
		
	}

	public String uglify(String[] args){	
		ScriptEngine engine = EcmaScriptEngineFactory.getECMAScriptEngine();
		engine.put("uglify_args", args);
		engine.put("uglify_no_output", true);
		run(engine);
		
		String result = null;
		
		try {
			result = (String)engine.eval("uglify();");
		} catch (ScriptException e) {
			ApplicationUtil.handleError( e );
		}		
		
		return result;
	}
	
	
	

}
