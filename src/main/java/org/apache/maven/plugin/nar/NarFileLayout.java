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

package org.apache.maven.plugin.nar;


/**
 * Defines the layout inside the nar file.
 * 
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 */
public interface NarFileLayout
{
    /**
     * Specifies where libraries are stored
     * 
     * @return
     */
    String getLibDirectory(String aol, String type );

    /**
     * Specifies where includes are stored
     * 
     * @return
     */
    String getIncludeDirectory();

    /**
     * Specifies where binaries are stored
     * 
     * @return
     */
    String getBinDirectory(String aol );

    /**
     * Specifies what configuration is in use.  An empty string means no configuration (introduced in NarFileLayout11)
     *
     * @return
     */
    String getConfigString();
}
