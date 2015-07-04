package com.aplos.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.apache.commons.codec.binary.Base64;

import com.aplos.common.beans.AplosBean;


public class TransientObjectConverter implements Converter {

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
	    Object obj = null;

	    try {
	      int underScoreIndice = value.indexOf("_");
	      //String className = value.substring(0, underScoreIndice);
	      String serializedObj = value.substring(underScoreIndice + 1, value.length());

	      byte[] byteObj = Base64.decodeBase64(serializedObj.getBytes());
	      ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(byteObj));
	      obj = ois.readObject();
	      ois.close();
	    }
	    catch (Exception e) {
	      throw new ConverterException("Transient object cannot be read from stream");
	    }

	    return obj;
	  }

	  @Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
	      String retValue = null;

	    if ((value != null) && (value instanceof AplosBean)) {
	    	AplosBean bean = (AplosBean) value;
	      bean.getClass().getName();

	      if (bean.getId() == null) {
	        try {
	          ByteArrayOutputStream bos = new ByteArrayOutputStream();
	          ObjectOutput output = new ObjectOutputStream(bos);
	          output.writeObject(bean);
	          byte[] byteArr = bos.toByteArray();
	          String stream = new String(Base64.encodeBase64(byteArr));
	            retValue = bean.getClass().getName() + "_" + stream;
	        }
	        catch (IOException e) {
		      e.printStackTrace();
	          throw new ConverterException("Transient object cannot be output to the stream: " + bean.getClass() + " because of the following error: " + e.getMessage());
	        }
	      }
	      return retValue;
	    }
	    throw new ConverterException("Object is null or not an AplosBean");
	  }

}
