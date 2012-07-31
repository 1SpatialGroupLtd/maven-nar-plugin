/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.maven.plugin.nar.test;

import java.io.File;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugin.nar.AbstractNarLayout;
import org.apache.maven.plugin.nar.Library;
import org.apache.maven.plugin.nar.NarConstants;
import org.apache.maven.plugin.nar.NarFileLayout;
import org.apache.maven.plugin.nar.NarFileLayout10;
import org.apache.maven.plugin.nar.NarLayout;
import org.apache.maven.plugin.nar.NarLayout22;

/**
 * @author Mike Boyd
 */
public class TestNarLayout22
    extends TestCase
{
    private NarFileLayout fileLayout;

    private NarLayout layout;

    private File baseDir;

    private String artifactId;

    private String version;

    private String aol;

    private String type;

    private DummyNarMojo dummy;

    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        fileLayout = new NarFileLayout10();
        artifactId = "artifactId";
        version = "version";
        baseDir = new File( "/Users/maven" );
        aol = "x86_64-MacOSX-g++";
        type = Library.SHARED;

        new SystemStreamLog();
        dummy = new DummyNarMojo("NarLayout22");
    }

    public final void testGetLayout()
        throws MojoExecutionException
    {
        AbstractNarLayout.getLayout( dummy );
        dummy.setDebug(true);
        AbstractNarLayout.getLayout( dummy );
    }

    /**
     * Test method for {@link org.apache.maven.plugin.nar.NarLayout22#getIncludeDirectory(java.io.File)}.
     *
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    public final void testGetIncludeDirectory()
        throws MojoExecutionException, MojoFailureException
    {
    	File expected = new File( baseDir, artifactId + "-" + version + "-" + NarConstants.NAR_NO_ARCH
                + File.separator + fileLayout.getIncludeDirectory() );

        layout = new NarLayout22(dummy);
        Assert.assertEquals( expected, layout.getIncludeDirectory( baseDir, artifactId, version ) );
        dummy.setDebug(true);
        layout = new NarLayout22(dummy);
        Assert.assertEquals( expected, layout.getIncludeDirectory( baseDir, artifactId, version ) );
    }

    /**
     * Test method for
     * {@link org.apache.maven.plugin.nar.NarLayout22#getLibDirectory(java.io.File, java.lang.String, java.lang.String)}
     *
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    public final void testGetLibDirectory()
        throws MojoExecutionException, MojoFailureException
    {
        File expectedLibDir = new File( baseDir, artifactId + "-" + version + "-" + aol + "-" + type + File.separator
            + fileLayout.getLibDirectory( aol, type ) );
        layout = new NarLayout22(dummy);
        Assert.assertEquals( new File(expectedLibDir, "release"), layout.getLibDirectory( baseDir, artifactId, version, aol, type ) );
        dummy.setDebug(true);
        layout = new NarLayout22(dummy);
        Assert.assertEquals( new File(expectedLibDir, "debug"), layout.getLibDirectory( baseDir, artifactId, version, aol, type ) );
    }

    /**
     * Test method for {@link org.apache.maven.plugin.nar.NarLayout22#getBinDirectory(java.io.File, java.lang.String)}.
     *
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    public final void testGetBinDirectory()
        throws MojoExecutionException, MojoFailureException
    {
        File expectedBindir = new File( baseDir, artifactId + "-" + version + "-" + aol + "-" + "executable"
            + File.separator + fileLayout.getBinDirectory( aol ) );

        layout = new NarLayout22(dummy);
        Assert.assertEquals( new File(expectedBindir, "release"), layout.getBinDirectory( baseDir, artifactId, version, aol ) );
        dummy.setDebug(true);
        layout = new NarLayout22(dummy);
        Assert.assertEquals( new File(expectedBindir, "debug"), layout.getBinDirectory( baseDir, artifactId, version, aol ) );
    }
}
