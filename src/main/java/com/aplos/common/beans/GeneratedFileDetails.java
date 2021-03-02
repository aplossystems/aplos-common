package com.aplos.common.beans;

import com.aplos.common.annotations.PluralDisplayName;
import com.aplos.common.annotations.persistence.*;
import com.aplos.common.interfaces.AplosWorkingDirectoryInter;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.FormatUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

@Entity
@PluralDisplayName(name="generated file details")
public class GeneratedFileDetails extends FileDetails {
	private static final long serialVersionUID = -4487615806016031773L;

	private String fileExtension = "txt";

	@Column(columnDefinition="LONGTEXT")
	private String content;

	public GeneratedFileDetails() {

	}

	public GeneratedFileDetails(String name, String content,
								AplosWorkingDirectoryInter aplosWorkingDirectoryInter, String fileExtension) {
		setName(name);
		setContent(content);
		setFileDetailsOwner(aplosWorkingDirectoryInter.getAplosWorkingDirectory());
		if (fileExtension != null) {
			this.fileExtension = fileExtension;
		}
	}

	public GeneratedFileDetails generateAndSaveFile() {
		if (getContent() != null) {
			try {
				String directoryPath = determineFileDetailsDirectory(true);

				String nameEnd = getName().trim() + " - " + FormatUtil.formatDate(new Date());
				String fileName = nameEnd + "." + fileExtension;
				int i = 1;
				while (new File(directoryPath + fileName).exists()) {
					fileName = nameEnd + "_" + ++i + "." + fileExtension;
				}


				String outputFile = directoryPath + fileName;

				new File(directoryPath).mkdirs();
				Files.write(Paths.get(outputFile), getContent().getBytes());
				setFilename(fileName);
				saveDetails();
				return this;
			} catch (Exception e) {
				ApplicationUtil.getAplosContextListener().handleError(e);
			}
		}
		return null;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
