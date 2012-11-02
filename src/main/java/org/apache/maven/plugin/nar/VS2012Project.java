package org.apache.maven.plugin.nar;

import java.io.File;
import java.util.UUID;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

public class VS2012Project
{
    private static final String VS2012_USER_TEMPLATE = "VS2012UserTemplate.txt";
    private static final String VS2012_FILTERS_TEMPLATE = "VS2012FiltersTemplate.txt";
    private static final String VCXPROJ_USER_EXTENSION = ".vcxproj.user";
    private static final String VCXPROJ_FILTERS_EXTENSION = ".vcxproj.filters";
    private static final String PROJECT_FILE_EXTENSION = ".vcxproj";
    private String GUID;
    private String name;
    private File directory;
    private File projectFile;
    private File filtersFile;
    private File userFile;

    public VS2012Project(File solutionDir, String projectName)
    {
        name = projectName;
        directory = new File(solutionDir, projectName);
        directory.mkdirs(); //TODO check this worked.
        GUID = UUID.randomUUID().toString();
        projectFile = new File(directory,  name + PROJECT_FILE_EXTENSION);
        filtersFile = new File(directory, name + VCXPROJ_FILTERS_EXTENSION);
        userFile = new File(directory, name + VCXPROJ_USER_EXTENSION);
    }

    public void createProjectFiles(ProjectInfo info, String narPrecompiledHeaderFilePath) throws MojoExecutionException, MojoFailureException
    {
        info.setProjectDirectory(directory);
        createProjectFile(info, narPrecompiledHeaderFilePath);
        createFiltersFile(info);
        createUserFile(info);
    }

    private void createProjectFile(ProjectInfo info ,String narPrecompiledHeaderFilePath) throws MojoExecutionException, MojoFailureException
    {
        VisualStudioTemplateModifier modifier =
            new VisualStudioProjectTemplateModifier(info, projectFile, GUID, name, narPrecompiledHeaderFilePath);
        modifier.createPopulatedOutput();
    }

    private void createFiltersFile(ProjectInfo info) throws MojoExecutionException, MojoFailureException
    {
        VisualStudioTemplateModifier modifier =
            new VisualStudioFiltersTemplateModifier(VS2012_FILTERS_TEMPLATE, filtersFile, info);
        modifier.createPopulatedOutput();
    }

    private void createUserFile(ProjectInfo info) throws MojoExecutionException, MojoFailureException
    {
        VisualStudioTemplateModifier modifier =
            new VisualStudioUsersTemplateModifier(VS2012_USER_TEMPLATE, userFile, info);
        modifier.createPopulatedOutput();
    }

    public String getGUID()
    {
        return GUID;
    }

    public String getName()
    {
        return name;
    }

    public File getProjectFile()
    {
        return projectFile;
    }

    public String getRelativeProjectPath() throws MojoExecutionException
    {
        return RelativePathUtils.getRelativePath(directory.getParentFile(), projectFile.getPath());
    }

}
