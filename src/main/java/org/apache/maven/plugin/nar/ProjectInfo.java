package org.apache.maven.plugin.nar;

import java.io.File;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.nar.NarVisualStudioSetupMojo.PchInfo;

public class ProjectInfo
{
    private String projectTemplate;
    private String binding;
    private Set includes;
    private Set libraryPaths;
    private Set defines;
    private Set libraryFiles;
    private Set headerFiles;
    private Set sourceFiles;
    private File directory;
    private PchInfo pchInfo;

    public ProjectInfo(String projectTemplate, String binding, Set defines, Set includes,
            Set libraryPaths, Set libraryFiles, Set headerFiles,
            Set sourceFiles, PchInfo pchInfo)
    {
        this.projectTemplate = projectTemplate;
        this.binding = binding;
        this.defines = defines;
        this.includes = includes;
        this.libraryPaths = libraryPaths;
        this.libraryFiles = libraryFiles;
        this.headerFiles = headerFiles;
        this.sourceFiles = sourceFiles;
        this.pchInfo = pchInfo;
    }

    public void setProjectDirectory(File directory)
    {
        this.directory = directory;
    }

    public String getPprojectTemplate()
    {
        return projectTemplate;
    }

    public String getBinding()
    {
        return binding;
    }

    public Set getIncludes() throws MojoExecutionException
    {
        return RelativePathUtils.getRelativePaths(directory, includes);
    }

    public Set getHeaderFiles() throws MojoExecutionException
    {
        return RelativePathUtils.getRelativePaths(directory, headerFiles);
    }

    public Set getSourceFiles() throws MojoExecutionException
    {
        return RelativePathUtils.getRelativePaths(directory, sourceFiles);
    }

    public Set getLibraryPaths() throws MojoExecutionException
    {
        return RelativePathUtils.getRelativePaths(directory, libraryPaths);
    }

    public Set getDefines()
    {
        return defines;
    }

    public Set getLibraryFiles() throws MojoExecutionException
    {
        return RelativePathUtils.getRelativePaths(directory, libraryFiles);
    }

    public String getRelativePchDirectory(boolean debug) throws MojoExecutionException
    {
        if(!pchInfo.usePch)
            return null;
        String absolutePath = pchInfo.directory + File.separator + (debug ? "debug" : "release");
        return RelativePathUtils.getRelativePath(directory, absolutePath);
    }

    public String getPchFileName() throws MojoExecutionException
    {
        return pchInfo.pchName;
    }

    public String getPchBaseDirectory() throws MojoExecutionException
    {
        if(!pchInfo.usePch)
            return null;
        File baseDir = pchInfo.directory;
        while(!baseDir.getName().contains(pchInfo.artifactId))
        {
            baseDir = baseDir.getParentFile();
        }
        return RelativePathUtils.getRelativePath(directory, baseDir.getPath());
    }

    public boolean usePch()
    {
        return pchInfo.usePch;
    }
}
