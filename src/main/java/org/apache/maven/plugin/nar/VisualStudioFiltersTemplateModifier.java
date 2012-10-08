package org.apache.maven.plugin.nar;

import java.io.File;
import java.util.UUID;

import org.apache.maven.plugin.MojoExecutionException;

public class VisualStudioFiltersTemplateModifier extends
        VisualStudioTemplateModifier {

    private ProjectInfo info;

    public VisualStudioFiltersTemplateModifier(String templateFile, File destinationFile, ProjectInfo info)
    {
        super(templateFile, destinationFile);
        this.info = info;
    }

    protected String replacePlaceholders(String contents)
            throws MojoExecutionException
    {
        String modifiedContents = replace(contents, NEW_GUID, UUID.randomUUID().toString());
        modifiedContents = replace(modifiedContents, HEADER_FILE_ELEMENTS, getHeaderFileElementsAsString());
        modifiedContents = replace(modifiedContents, SOURCE_FILE_ELEMENTS, getSourceFileElementsAsString());
        return modifiedContents;
    }

    private String getHeaderFileElementsAsString() throws MojoExecutionException
    {
        return convertToStringUsingPrefix(info.getHeaderFiles(),
                "    <ClInclude Include=\"",
                "\">\n      <Filter>Header Files</Filter>\n    </ClInclude>\n");
    }

    private String getSourceFileElementsAsString() throws MojoExecutionException
    {
        return convertToStringUsingPrefix(info.getSourceFiles(),
                "    <ClCompile Include=\"",
                "\">\n      <Filter>Source Files</Filter>\n    </ClCompile>\n");
    }

}
