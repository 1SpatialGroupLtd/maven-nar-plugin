package org.apache.maven.plugin.nar.test;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.nar.AbstractNarMojo;

public class DummyNarMojo extends AbstractNarMojo 
{
    public DummyNarMojo()
    {}

    //Constructor allowing us to set he layout.
    public DummyNarMojo(String layoutName)
    {
        this.layout = layoutName;
    }

    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    public void narExecute() throws MojoFailureException,
            MojoExecutionException 
    {
        //Dummy method
    }
}
