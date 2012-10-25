package org.apache.maven.plugin.nar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;

import org.apache.maven.plugin.MojoExecutionException;

public abstract class VisualStudioTemplateModifier implements NarVisualStudioSetupTags
{
    private String templateFileName;
    private File destinationFile;

    public VisualStudioTemplateModifier(String templateFileName, File destinationFile)
    {
        this.templateFileName = templateFileName;
        this.destinationFile = destinationFile;
    }

    public void createPopulatedOutput() throws MojoExecutionException
    {
        try
        {
            BufferedReader input = null;//getTemplateReader();
            BufferedWriter output = null;//getOutputWriter();
            try
            {
                destinationFile.createNewFile();
                input = getTemplateReader();
                output = getOutputWriter();
                modifyTemplate(input, output);
                output.flush();
            }
            finally
            {
                if(input != null)
                    input.close();
                if(output != null)
                    output.close();
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
            throw new MojoExecutionException(e.getMessage());
        }
    }

    private void modifyTemplate(BufferedReader input, BufferedWriter output) throws MojoExecutionException, IOException
    {
        String line = null;
        while (( line = input.readLine()) != null)
        {
            output.write(replacePlaceholders(line));
            output.write(System.getProperty("line.separator"));
        }
    }

    private BufferedWriter getOutputWriter() throws IOException
    {
        return new BufferedWriter(new FileWriter(destinationFile));
    }

    private BufferedReader getTemplateReader() throws MojoExecutionException
    {
        InputStream resourceIS = NarUtil.class.getResourceAsStream(templateFileName);
        if(resourceIS == null)
            throw new MojoExecutionException("Could not read resource " + templateFileName);

        return new BufferedReader(new InputStreamReader(resourceIS));
    }

    protected abstract String replacePlaceholders(String contents) throws MojoExecutionException;

    protected String replace(String textToParse, String tag, String newContents)
    {
        if(textToParse.contains(tag))
            return textToParse.replace(tag, newContents);
        return textToParse;
    }

    protected String convertToString(Collection stringCollection, String suffix)
    {
        return convertToStringUsingPrefix(stringCollection, "", suffix);
    }

    protected String convertToStringUsingPrefix(Collection stringCollection, String prefix,
            String suffix)
    {
        StringBuilder builder = new StringBuilder();
        for(Iterator i = stringCollection.iterator(); i.hasNext();)
        {
            builder.append(prefix);
            builder.append((String) i.next());
            builder.append(suffix);
        }
        return builder.toString();
    }
}
