package org.apache.maven.plugin.nar;

import java.io.File;
import java.util.Set;
import java.util.UUID;

import org.apache.maven.plugin.MojoExecutionException;

public class VisualStudioFiltersTemplateModifier extends
		VisualStudioTemplateModifier {

	private Set headerFiles;
	private Set sourceFiles;

	public VisualStudioFiltersTemplateModifier(String templateFileName, File destinationFile,
			Set headerFiles, Set sourceFiles)
	{
		super(templateFileName, destinationFile);
		this.headerFiles = headerFiles;
		this.sourceFiles = sourceFiles;
	}

	protected String replacePlaceholders(String contents)
			throws MojoExecutionException
	{
		String modifiedContents = replace(contents, NEW_GUID, UUID.randomUUID().toString());
		modifiedContents = replace(modifiedContents, HEADER_FILE_ELEMENTS, getHeaderFileElementsAsString());
		modifiedContents = replace(modifiedContents, SOURCE_FILE_ELEMENTS, getSourceFileElementsAsString());
		return modifiedContents;
	}

	private String getHeaderFileElementsAsString()
	{
		return convertToStringUsingPrefix(headerFiles,
				"    <ClInclude Include=\"",
				"\">\n      <Filter>Header Files</Filter>\n    </ClInclude>\n");
	}

	private String getSourceFileElementsAsString()
	{
		return convertToStringUsingPrefix(sourceFiles,
				"    <ClCompile Include=\"",
				"\">\n      <Filter>Source Files</Filter>\n    </ClCompile>\n");
	}

}
