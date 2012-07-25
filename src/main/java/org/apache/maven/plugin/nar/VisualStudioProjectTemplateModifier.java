package org.apache.maven.plugin.nar;

import java.io.File;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;

public class VisualStudioProjectTemplateModifier extends
		VisualStudioTemplateModifier
{

	private String projectGUID;
	private String projectName;
	private String binding;
	private Set includes;
	private Set libraryPaths;
	private Set defines;
	private Set libraries;
	private Set headerFiles;
	private Set sourceFiles;

	public VisualStudioProjectTemplateModifier(String templateFileName,
			File destinationFile, String projectGUID, String projectName,
			String binding, Set includes, Set libraryPaths,
			Set defines, Set libraries, Set headerFiles, Set sourceFiles)
	{
		super(templateFileName, destinationFile);
		this.projectGUID = projectGUID;
		this.projectName = projectName;
		this.binding = binding;
		this.includes = includes;
		this.libraryPaths = libraryPaths;
		this.defines = defines;
		this.libraries = libraries;
		this.headerFiles = headerFiles;
		this.sourceFiles = sourceFiles;
	}

	protected String replacePlaceholders(String contents) throws MojoExecutionException
	{
		String modifiedContents = replace(contents, PROJECT_GUID, projectGUID);
		modifiedContents = replace(modifiedContents, PROJECT_NAME, projectName);
		modifiedContents = replace(modifiedContents, LIBRARY_TYPE, getProjectTypeString());
		modifiedContents = replace(modifiedContents, INCLUDES, getIncludesAsString());
		modifiedContents = replace(modifiedContents, LIBRARY_PATHS, getLibraryPathsAsString());
		modifiedContents = replace(modifiedContents, UPPER_CASE_PROJECT_NAME, projectName.toUpperCase());
		modifiedContents = replace(modifiedContents, DEFINES, getDefinesAsString());
		modifiedContents = replace(modifiedContents, LIBRARIES, getLibrariesAsString());
		modifiedContents = replace(modifiedContents, HEADER_FILE_ELEMENTS, getHeaderFileElementsAsString());
		modifiedContents = replace(modifiedContents, SOURCE_FILE_ELEMENTS, getSourceFileElementsAsString());

		return modifiedContents;
	}

	private String getHeaderFileElementsAsString()
	{
		return convertToStringUsingPrefix(headerFiles,
				"    <ClInclude Include=\"", "\" />\n");
	}

	private String getSourceFileElementsAsString()
	{
		return convertToStringUsingPrefix(sourceFiles,
				"    <ClCompile Include=\"", "\" />\n");
	}

	private String getLibrariesAsString()
	{
		return convertToString(libraries, ";");
	}

	private String getDefinesAsString()
	{
		return convertToStringUsingPrefix(defines, "/D ", " ");
	}

	private String getLibraryPathsAsString()
	{
		return convertToString(libraryPaths, ";");
	}

	private String getIncludesAsString()
	{
		return convertToString(includes, ";");
	}

	private String getProjectTypeString() throws MojoExecutionException
	{
		if(binding.equals(Library.SHARED))
			return "DynamicLibrary";
		if(binding.equals(Library.EXECUTABLE))
			return "Application";
		if(binding.equals(Library.STATIC))
			return "StaticLibrary";

		throw new MojoExecutionException("Project of type " + binding + " not supported for vcproj generation");
	}
}
