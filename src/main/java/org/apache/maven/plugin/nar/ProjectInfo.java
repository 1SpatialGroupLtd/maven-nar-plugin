package org.apache.maven.plugin.nar;

import java.io.File;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;

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

    public ProjectInfo(String projectTemplate, String binding, Set defines, Set includes,
            Set libraryPaths, Set libraryFiles, Set headerFiles,
            Set sourceFiles)
    {
        this.projectTemplate = projectTemplate;
        this.binding = binding;
        this.defines = defines;
        this.includes = includes;
        this.libraryPaths = libraryPaths;
        this.libraryFiles = libraryFiles;
        this.headerFiles = headerFiles;
        this.sourceFiles = sourceFiles;
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
}
