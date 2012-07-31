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

import org.apache.maven.plugin.nar.Library;
import org.apache.maven.plugin.nar.NarFileLayout;
import org.apache.maven.plugin.nar.NarFileLayout11;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author Mike Boyd
 * @version $Id$
 */
public class TestNarFileLayout11
    extends TestCase
{
    private NarFileLayout fileLayout;

    protected String artifactId;

    protected String version;

    protected String aol;

    protected String type;

    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        artifactId = "artifactId";
        version = "version";
        aol = "x86_64-MacOSX-g++";
        type = Library.SHARED;
    }

    public final void testGetIncludeDirectoryDebug()
    {
        fileLayout = new NarFileLayout11(true);
        Assert.assertEquals( "include", fileLayout.getIncludeDirectory() );
    }

    public final void testGetIncludeDirectoryRelease()
    {
        fileLayout = new NarFileLayout11(false);
        Assert.assertEquals( "include", fileLayout.getIncludeDirectory() );
    }

    public final void testGetLibDirectoryDebug()
    {
        fileLayout = new NarFileLayout11(true);
        Assert.assertEquals( "lib" + File.separator + aol + File.separator + type + File.separator + "debug", fileLayout.getLibDirectory( aol, type ) );
    }

    public final void testGetLibDirectoryRelease()
    {
        fileLayout = new NarFileLayout11(false);
        Assert.assertEquals( "lib" + File.separator + aol + File.separator + type + File.separator + "release", fileLayout.getLibDirectory( aol, type ) );
    }

    public final void testGetBinDirectoryDebug()
    {
        fileLayout = new NarFileLayout11(true);
        Assert.assertEquals( "bin" + File.separator + aol + File.separator + "debug", fileLayout.getBinDirectory( aol ) );
    }

    public final void testGetBinDirectoryRelease()
    {
        fileLayout = new NarFileLayout11(false);
        Assert.assertEquals( "bin" + File.separator + aol + File.separator + "release", fileLayout.getBinDirectory( aol ) );
    }
}
