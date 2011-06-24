/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Copyright (C) 2010 isandlaTech.com, France
 */
package org.psem2m.eclipse.ipojo.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

public class CompositeFile extends File {

	/** Serializable interface value */
	private static final long serialVersionUID = 1L;

	/** Eclipse representation of the resource */
	private IResource pEclipseResource;

	/** Eclipse Workspace root */
	private IWorkspaceRoot pWorkspaceRoot;

	/**
	 * Constructs a File compatible object from an Eclipse path
	 * 
	 * @param aWorkspaceRoot
	 *            Eclipse Workspace root
	 * @param aPath
	 *            Full path of a workspace member
	 */
	public CompositeFile(final IWorkspaceRoot aWorkspaceRoot, final IPath aPath) {
		super(aPath.toFile().getPath());

		pWorkspaceRoot = aWorkspaceRoot;
		pEclipseResource = pWorkspaceRoot.findMember(aPath);
	}

	@Override
	public boolean exists() {
		if (pEclipseResource == null) {
			return false;
		}

		return pEclipseResource.exists();
	}

	@Override
	public String getAbsolutePath() {
		if (pEclipseResource == null) {
			return "";
		}

		return pWorkspaceRoot.getLocation()
				.append(pEclipseResource.getFullPath()).toOSString();
	}

	/**
	 * @return The file input stream
	 * @throws CoreException
	 *             An error occurred while opening the stream
	 * @throws IOException
	 *             This file is not a regular file
	 */
	public InputStream getInputStream() throws CoreException, IOException {
		if (pEclipseResource == null) {
			throw new IOException("Can't find file");
		}

		if (pEclipseResource.getType() != IResource.FILE) {
			throw new IOException(pEclipseResource.getFullPath().toOSString()
					+ " is not a file");
		}

		return ((IFile) pEclipseResource).getContents(true);
	}

	/**
	 * @return The underlying Eclipse resource
	 */
	public IResource getResource() {
		return pEclipseResource;
	}

	@Override
	public boolean isDirectory() {
		return pEclipseResource.getType() == IResource.FOLDER;
	}

	/**
	 * Calls the
	 * {@link IFile#setContents(InputStream, boolean, boolean, org.eclipse.core.runtime.IProgressMonitor)}
	 * method
	 * 
	 * @param aSource
	 *            Source input stream
	 * @throws IOException
	 *             This file is not a regular file
	 * @throws CoreException
	 *             An error occurred while writing the stream
	 */
	public void setContent(final InputStream aSource) throws IOException,
			CoreException {

		if (pEclipseResource == null) {
			throw new IOException("Invalid resource");
		}

		if (pEclipseResource.getType() != IResource.FILE) {
			throw new IOException(pEclipseResource.getFullPath().toOSString()
					+ " is not a file");
		}

		IFile outFile = ((IFile) pEclipseResource);

		if (outFile.exists()) {
			outFile.setContents(aSource, true, false, null);
		} else {
			outFile.create(aSource, true, null);
		}
	}

	@Override
	public URI toURI() {
		if (pEclipseResource == null) {
			return null;
		}

		return pWorkspaceRoot.getLocationURI().resolve(
				pEclipseResource.getLocationURI());
	}
}
