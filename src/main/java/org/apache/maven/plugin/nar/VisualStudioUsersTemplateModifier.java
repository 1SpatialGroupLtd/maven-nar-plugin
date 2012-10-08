package org.apache.maven.plugin.nar;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;

public class VisualStudioUsersTemplateModifier extends
        VisualStudioTemplateModifier {

    private ProjectInfo info;

    public VisualStudioUsersTemplateModifier(String templateFile, File destinationFile, ProjectInfo info)
    {
        super(templateFile, destinationFile);
        this.info = info;
    }

    protected String replacePlaceholders(String contents)
            throws MojoExecutionException
    {
        return replace(contents, LIBRARY_PATHS, getLibraryPathsAsString());
    }

    private String getLibraryPathsAsString() throws MojoExecutionException
    {
        return convertToString(info.getLibraryPaths(), ";");
    }
}
