package org.apache.maven.plugin.nar;

import java.io.File;

public class NarFileLayout11 extends NarFileLayout10
{
    private boolean debug;

    public NarFileLayout11(boolean debug)
    {
        this.debug = debug;
    }

    //Replace with enum when updated to java 6.
    public String getConfigString()
    {
        return debug ? "debug" : "release";
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.plugin.nar.NarFileLayout#getIncludeDirectory()
     */
    public String getIncludeDirectory()
    {
        return "include";
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.plugin.nar.NarFileLayout#getLibDirectory(java.lang.String, java.lang.String)
     */
    public String getLibDirectory( String aol, String type )
    {
        return super.getLibDirectory(aol, type) + File.separator + getConfigString();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.plugin.nar.NarFileLayout#getBinDirectory(java.lang.String)
     */
    public String getBinDirectory( String aol )
    {
        return super.getBinDirectory(aol) + File.separator + getConfigString();
    }
}
