package com.aplos.common.utils;

import java.io.UnsupportedEncodingException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.codec.binary.Base64;

public class UrlEncrypter {
	private Cipher ecipher;
	private Cipher dcipher;

	public UrlEncrypter( String passPhrase, String salt, int iterationCount ) {
		try{
			char[] idChars = passPhrase.toCharArray();
			/*
			 * Salt must be 8 chars long otherwise the cipher initialisation fails
			 */
			byte[] saltBytes = ((String) salt.subSequence(0, 8)).getBytes();
            KeySpec keySpec = new PBEKeySpec(idChars, saltBytes, iterationCount);
            SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);

            ecipher = Cipher.getInstance(key.getAlgorithm());
            dcipher = Cipher.getInstance(key.getAlgorithm());

            AlgorithmParameterSpec paramSpec = new PBEParameterSpec(saltBytes, iterationCount);

            ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
            dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);


        } catch (java.security.InvalidAlgorithmParameterException e){
        	ApplicationUtil.handleError( e );
        } catch (java.security.spec.InvalidKeySpecException e){
        	ApplicationUtil.handleError( e );
        } catch (javax.crypto.NoSuchPaddingException e){
        	ApplicationUtil.handleError( e );
        } catch (java.security.NoSuchAlgorithmException e){
        	ApplicationUtil.handleError( e );
        } catch (java.security.InvalidKeyException e){
        	ApplicationUtil.handleError( e );
        }
	}


    public String encrypt(String str){

        try{

            byte[] utf8 = str.getBytes("UTF8");
            byte[] enc  = ecipher.doFinal(utf8);

            return Base64.encodeBase64URLSafeString(enc);

        } catch (javax.crypto.BadPaddingException e){
        } catch (IllegalBlockSizeException e){
        	ApplicationUtil.handleError( e );
        } catch (UnsupportedEncodingException e){
        	ApplicationUtil.handleError( e );
        }

        return null;
    }

    public String decrypt(String str){

        try{

            byte[] dec = Base64.decodeBase64(str);
            byte[] utf8 = dcipher.doFinal(dec);

            return new String(utf8,"UTF8");

        } catch (javax.crypto.BadPaddingException e){
        } catch (IllegalBlockSizeException e){
        	ApplicationUtil.handleError( e );
        } catch (UnsupportedEncodingException e){
        	ApplicationUtil.handleError( e );
        } catch (java.io.IOException e){
        	ApplicationUtil.handleError( e );
        }

        return null;
    }
}
