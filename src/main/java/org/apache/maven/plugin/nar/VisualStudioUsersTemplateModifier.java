package org.apache.maven.plugin.nar;

import java.io.File;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;

public class VisualStudioUsersTemplateModifier extends
		VisualStudioTemplateModifier {

	private Set libraryPaths;

	public VisualStudioUsersTemplateModifier(String templateFileName, File destinationFile,
			Set libraryPaths)
	{
		super(templateFileName, destinationFile);
		this.libraryPaths = libraryPaths;
	}

	protected String replacePlaceholders(String contents)
			throws MojoExecutionException
	{
		return replace(contents, LIBRARY_PATHS, getLibraryPathsAsString());
	}

	private String getLibraryPathsAsString()
	{
		return convertToString(libraryPaths, ";");
	}

}
