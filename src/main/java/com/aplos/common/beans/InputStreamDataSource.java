package com.aplos.common.beans;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

public class InputStreamDataSource implements DataSource {
	private String filename;
	private String contentType;
	private ByteArrayOutputStream baos;

	public InputStreamDataSource(InputStream content, String filename, String contentType) throws IOException {
		this.filename = filename;
		this.contentType = contentType;

		baos = new ByteArrayOutputStream();

        int read;
        byte[] buff = new byte[256];
        while((read = content.read(buff)) != -1) {
            baos.write(buff, 0, read);
        }
	}

    @Override
	public String getContentType() {
        return contentType;
    }

    @Override
	public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(baos.toByteArray());
    }

    @Override
	public String getName() {
        return filename;
    }

    @Override
	public OutputStream getOutputStream() throws IOException {
        throw new IOException("Cannot write to this read-only resource");
    }
}
