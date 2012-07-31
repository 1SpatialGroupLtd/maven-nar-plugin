package org.apache.maven.plugin.nar;

public class NarLayout22 extends NarLayout21 
{
    public NarLayout22(AbstractNarMojo abstractNarMojo) 
    {
        super(abstractNarMojo);
        this.fileLayout = new NarFileLayout11(abstractNarMojo.getDebug());
    }

    public String getConfiguration() 
    {
        return fileLayout.getConfigString();
    }
}
