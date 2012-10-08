package org.apache.maven.plugin.nar;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;

public class VisualStudioProjectTemplateModifier extends
        VisualStudioTemplateModifier
{

    private String projectGUID;
    private String projectName;
    private ProjectInfo info;

    public VisualStudioProjectTemplateModifier(ProjectInfo info,
            File destinationFile, String projectGUID, String projectName) throws MojoExecutionException
    {
        super(info.getPprojectTemplate(), destinationFile);
        this.projectGUID = projectGUID;
        this.projectName = projectName;
        this.info = info;
    }

    protected String replacePlaceholders(String contents) throws MojoExecutionException
    {
        String modifiedContents = replace(contents, PROJECT_GUID, projectGUID);
        modifiedContents = replace(modifiedContents, PROJECT_NAME, projectName);
        modifiedContents = replace(modifiedContents, LIBRARY_TYPE, getProjectTypeString());
        modifiedContents = replace(modifiedContents, INCLUDES, getIncludesAsString());
        modifiedContents = replace(modifiedContents, LIBRARY_PATHS, getLibraryPathsAsString());
        modifiedContents = replace(modifiedContents, DEFINES, getDefinesAsString());
        modifiedContents = replace(modifiedContents, LIBRARIES, getLibrariesAsString(false));
        modifiedContents = replace(modifiedContents, LIBRARIES_DEBUG, getLibrariesAsString(true));
        modifiedContents = replace(modifiedContents, HEADER_FILE_ELEMENTS, getHeaderFileElementsAsString());
        modifiedContents = replace(modifiedContents, SOURCE_FILE_ELEMENTS, getSourceFileElementsAsString());
        modifiedContents = replace(modifiedContents, USE_PRE_COMPILED_HEADERS, getUsePreCompiledHeaders());
        modifiedContents = replace(modifiedContents, CLEAN_PRE_COMPILED_HEADERS, getCleanPreCompiledHeadersCommand());
        modifiedContents = replace(modifiedContents, PRE_COMPILED_HEADER_H, getPreCompiledHeaderH(false));
        modifiedContents = replace(modifiedContents, PRE_COMPILED_HEADER_PDB, getPreCompiledHeaderPdb(false));
        modifiedContents = replace(modifiedContents, FORCED_INCLUDES, getForcedIncludes(false));
        modifiedContents = replace(modifiedContents, PRE_COMPILED_HEADER_H_DEBUG, getPreCompiledHeaderH(true));
        modifiedContents = replace(modifiedContents, PRE_COMPILED_HEADER_PDB_DEBUG, getPreCompiledHeaderPdb(true));
        modifiedContents = replace(modifiedContents, FORCED_INCLUDES_DEBUG, getForcedIncludes(true));

        return modifiedContents;
    }

    private String getForcedIncludes(boolean debug) throws MojoExecutionException
    {
        if(info.usePch())
            return "/FI" + getPreCompiledHeaderH(debug);
        return "";
    }

    private String getCleanPreCompiledHeadersCommand() throws MojoExecutionException
    {
        if(info.usePch())
        {
            StringBuilder builder = new StringBuilder();
            builder.append("rmdir /Q/S " + info.getPchBaseDirectory() + "\r\n");
            builder.append("cd ..\\..\\\r\n");
            builder.append("mvn nar:nar-unpack");
            return builder.toString();
        }
        return "";
    }

    private String getPreCompiledHeaderPdb(boolean debug) throws MojoExecutionException
    {
        if(info.usePch())
            return info.getRelativePchDirectory(debug) + File.separator + info.getPchFileName() + ".pdb";
        return "";
    }

    private String getPreCompiledHeaderH(boolean debug) throws MojoExecutionException
    {
        if(info.usePch())
            return info.getRelativePchDirectory(debug) + File.separator + info.getPchFileName() + ".h";
        return "";
    }

    private String getUsePreCompiledHeaders() throws MojoExecutionException
    {
        return info.usePch() ? "Use" : "";
    }

    private String getHeaderFileElementsAsString() throws MojoExecutionException
    {
        return convertToStringUsingPrefix(info.getHeaderFiles(),
                "    <ClInclude Include=\"", "\" />\n");
    }

    private String getSourceFileElementsAsString() throws MojoExecutionException
    {
        return convertToStringUsingPrefix(info.getSourceFiles(),
                "    <ClCompile Include=\"", "\" />\n");
    }

    private String getLibrariesAsString(boolean debug) throws MojoExecutionException
    {
        String libraries = convertToString(info.getLibraryFiles(), ";");
        if(info.usePch())
            libraries += info.getRelativePchDirectory(debug) + File.separator + info.getPchFileName() + ".obj";
        return libraries;

    }

    private String getDefinesAsString()
    {
        return convertToStringUsingPrefix(info.getDefines(), "/D ", " ");
    }

    private String getLibraryPathsAsString() throws MojoExecutionException
    {
        return convertToString(info.getLibraryPaths(), ";");
    }

    private String getIncludesAsString() throws MojoExecutionException
    {
        return convertToString(info.getIncludes(), ";");
    }

    private String getProjectTypeString() throws MojoExecutionException
    {
        String binding = info.getBinding();
        if(binding.equals(Library.SHARED))
            return "DynamicLibrary";
        if(binding.equals(Library.EXECUTABLE))
            return "Application";
        if(binding.equals(Library.STATIC))
            return "StaticLibrary";

        throw new MojoExecutionException("Project of type " + binding + " not supported for vcproj generation");
    }
}
