package org.apache.maven.plugin.nar;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;

public class VisualStudioProjectTemplateModifier extends
        VisualStudioTemplateModifier
{
    private static final String RUNTIME_STATIC = "static";
    private static final String RUNTIME_DYNAMIC = "dynamic";
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
        modifiedContents = replace(modifiedContents, MAIN_PROJECT_GUID, info.getMainProjectGUID());
        modifiedContents = replace(modifiedContents, MAIN_PROJECT_PATH, info.getMainProjectRelativePath());
        modifiedContents = replace(modifiedContents, LIBRARY_TYPE, getProjectTypeString());
        modifiedContents = replace(modifiedContents, INCLUDES, getIncludesAsString());
        modifiedContents = replace(modifiedContents, LIBRARY_PATHS, getLibraryPathsAsString());
        modifiedContents = replace(modifiedContents, DEFINES, getDefinesAsString());
        modifiedContents = replace(modifiedContents, LIBRARIES, getLibrariesAsString(false));
        modifiedContents = replace(modifiedContents, LIBRARIES_DEBUG, getLibrariesAsString(true));
        modifiedContents = replace(modifiedContents, HEADER_FILE_ELEMENTS, getHeaderFileElementsAsString());
        modifiedContents = replace(modifiedContents, SOURCE_FILE_ELEMENTS, getSourceFileElementsAsString());
        modifiedContents = replace(modifiedContents, RUNTIME_LIBRARY, getRuntimeLibrary(false));
        modifiedContents = replace(modifiedContents, RUNTIME_LIBRARY_DEBUG, getRuntimeLibrary(true));


        return modifiedContents;
    }

    private String getRuntimeLibrary(boolean debug)
    {
        String runtimeLib = debug ? "Debug" : "";
        if(info.getRuntime().equals(RUNTIME_STATIC))
            return "MultiThreaded" + runtimeLib;
        return "MultiThreaded" + runtimeLib + "Dll";
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
        return convertToString(info.getLibraryFiles(), ";");
    }

    private String getDefinesAsString()
    {
      return convertToStringUsingPrefix(info.getDefines(), "/D ", " ") + " ";
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
