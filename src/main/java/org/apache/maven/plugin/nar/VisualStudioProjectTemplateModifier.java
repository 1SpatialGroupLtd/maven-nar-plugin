package org.apache.maven.plugin.nar;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;

public class VisualStudioProjectTemplateModifier extends
        VisualStudioTemplateModifier
{
    private static final String RUNTIME_STATIC = "static";
    private static final String RUNTIME_DYNAMIC = "dynamic";
    private static final String PCHHEADERINCLUDE = "PCHHEADERFILE";
    private String precompiledHeaderFilePath = "";
    private String projectGUID;
    private String projectName;
    private ProjectInfo info;

    public VisualStudioProjectTemplateModifier(ProjectInfo info,
            File destinationFile, String projectGUID, String projectName, String narPrecompiledHeaderFilePath) throws MojoExecutionException
    {
        super(info.getPprojectTemplate(), destinationFile);
        this.projectGUID = projectGUID;
        this.projectName = projectName;
        this.info = info;
        this.precompiledHeaderFilePath = narPrecompiledHeaderFilePath;
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
        modifiedContents = replace(modifiedContents, USE_PRE_COMPILED_HEADERS, getUsePreCompiledHeaders());
        modifiedContents = replace(modifiedContents, CLEAN_PRE_COMPILED_HEADERS, getCleanPreCompiledHeadersCommand());
        modifiedContents = replace(modifiedContents, COPY_PRE_COMPILED_HEADER_FILE, getPrecompiledHeaderFileCopyCommand());
        modifiedContents = replace(modifiedContents, PRE_COMPILED_HEADER_H, getPreCompiledHeaderH(false));
        modifiedContents = replace(modifiedContents, PRE_COMPILED_HEADER_PDB, getPreCompiledHeaderPdb(false));
        modifiedContents = replace(modifiedContents, FORCED_INCLUDES, getForcedIncludes(false));
        modifiedContents = replace(modifiedContents, PRE_COMPILED_HEADER_H_DEBUG, getPreCompiledHeaderH(true));
        modifiedContents = replace(modifiedContents, PRE_COMPILED_HEADER_PDB_DEBUG, getProjectPrecompiledHeaderPdb(true));
        modifiedContents = replace(modifiedContents, FORCED_INCLUDES_DEBUG, getForcedIncludes(true));
        modifiedContents = replace(modifiedContents, RUNTIME_LIBRARY, getRuntimeLibrary(false));
        modifiedContents = replace(modifiedContents, RUNTIME_LIBRARY_DEBUG, getRuntimeLibrary(true));


        return modifiedContents;
    }

    private String getForcedIncludes(boolean debug) throws MojoExecutionException
    {
        if(info.usePch())
            return "/FI" + getPreCompiledHeaderH(debug);
        return "";
    }

    private String getRuntimeLibrary(boolean debug)
    {
        String runtimeLib = debug ? "Debug" : "";
        if(info.getRuntime().equals(RUNTIME_STATIC))
            return "MultiThreaded" + runtimeLib;
        return "MultiThreaded" + runtimeLib + "Dll";
    }

    private String getPrecompiledHeaderFileCopyCommand() throws MojoExecutionException
    {
        if(info.usePch())
        {
            if (!precompiledHeaderFilePath.equals(""))
            {
                String precompiledHeaderFile = precompiledHeaderFilePath + File.separator + info.getPchFileName() + ".h";
                File file = new File(precompiledHeaderFile);
                if (!file.exists())
                    throw new MojoExecutionException("Please specify a valid path to the pch header file using maven option -DnarPrecompiledHeader.path\r\nRelative paths should start from the project base directory.");
                else
                {
                    // Relative path problem, file exists check basedir is two directories higher than the project location
                    if (precompiledHeaderFile.startsWith(".."))
                    {
                        precompiledHeaderFile = "..\\..\\" + precompiledHeaderFile;
                    }
                    StringBuilder builder = new StringBuilder();
                    builder.append("\r\ncopy ");
                    builder.append(precompiledHeaderFile);
                    builder.append(" ");
                    builder.append(info.getRelativePchDirectory(true) + File.separator + info.getPchFileName() + ".h");
                    return builder.toString();
                }
            }
        }
        return "";
    }

    private String getCleanPreCompiledHeadersCommand() throws MojoExecutionException
    {
        if(info.usePch())
        {
            StringBuilder builder = new StringBuilder();
            builder.append("copy ");
            builder.append(getPreCompiledHeaderPdb(true));
            builder.append(" ");
            builder.append(getProjectPrecompiledHeaderPdb(true));
            return builder.toString();
        }
        return "";
    }

    private String getProjectPrecompiledHeaderPdb(boolean debug) throws MojoExecutionException
    {
        String folderName = debug ? "Debug" : "Release";
        if(info.usePch())
            return folderName + File.separator + info.getPchFileName() + ".pdb";
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
        String pchHeader = "";
        if (!precompiledHeaderFilePath.equals(""))
        {
            pchHeader = "/D" + PCHHEADERINCLUDE;
        }
        return convertToStringUsingPrefix(info.getDefines(), "/D ", " ") + pchHeader + " ";
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
