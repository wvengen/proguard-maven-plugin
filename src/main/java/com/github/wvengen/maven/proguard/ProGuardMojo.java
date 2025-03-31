/**
 * Pyx4me framework
 * Copyright (C) 2006-2008 pyx4j.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * @author vlads
 * @version $Id$
 */
package com.github.wvengen.maven.proguard;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.codehaus.plexus.archiver.jar.JarArchiver;

/**
 * Runs ProGuard as part of the build.
 *
 * @goal proguard
 * @phase package
 * @description Create small jar files using ProGuard
 * @requiresDependencyResolution compile
 * @threadSafe
 */

public class ProGuardMojo extends AbstractMojo {

	/**
	 * Set this to 'true' to bypass ProGuard processing entirely.
	 *
	 * @parameter property="proguard.skip"
	 */
	private boolean skip;

	/**
	 * Recursively reads configuration options from the given file.
	 *
	 * @parameter default-value="${basedir}/proguard.conf"
	 */
	private File proguardInclude;

	/**
	 * Select specific ProGuard version from plugin dependencies.
	 *
	 * @parameter
	 */
	private String proguardVersion;

	/**
	 * To run DexGuard instead of ProGuard, set this to 'true'.
	 *
	 * @parameter default-value="false"
	 */
	private boolean useDexGuard;

	/**
	 * ProGuard configuration options
	 *
	 * @parameter
	 */
	private String[] options;

	/**
	 * Specifies whether to obfuscate the input class files.
	 *
	 * @parameter default-value="true"
	 */
	private boolean obfuscate;

	/**
	 * Specifies that project compile dependencies be added as {@code -libraryjars} to ProGuard arguments. Dependency itself is
	 * not included in resulting jar unless you set {@link #includeDependencyInjar} to 'true'.
	 *
	 * @parameter default-value="true"
	 */
	private boolean includeDependency;

	/**
	 * @parameter expression="${project.build.directory}/tempLibraryjars"
	 * @readonly
	 */
	private File tempLibraryjarsDir;

	/**
	 * Specifies to copy all the {@code -libraryjars} dependencies into a temporary directory and pass that directory
	 * as the only {@code -libraryjars} argument to ProGuard.
	 *
	 * @parameter default-value="false"
	 */
	private boolean putLibraryJarsInTempDir;

	/**
	 * Use this parameter if your command line arguments become too long and execution fails.
     *
     * <p>If this parameter is 'true', the configuration is passed to the ProGuard process through a file, instead of through
     * command line arguments. This bypasses the operating system restrictions on the length of the command line arguments.
	 *
	 * @parameter default-value="false"
	 */
	private boolean generateTemporaryConfigurationFile;

	/**
	 * @parameter expression="${project.build.directory}/generated-proguard.conf"
	 * @readonly
	 */
	private File temporaryConfigurationFile;

	/**
	 * Specifies that project compile dependencies should be added as {@code -injars}.
	 *
	 * @parameter default-value="false"
	 */
	private boolean includeDependencyInjar;

	/**
	 * Bundle project dependency to resulting jar. Specifies list of artifact inclusions.
	 *
	 * @parameter
	 */
	private Assembly assembly;

	/**
	 * Additional {@code -libraryjars} e.g. <code>${java.home}/lib/rt.jar</code>. Project compile dependency are added automatically,
	 * see {@link #exclusions}.
	 *
	 * @parameter
	 */
	private List<String> libs;

	/**
	 * List of dependency exclusions
	 *
	 * @parameter
	 */
	private List<Exclusion> exclusions;

	/**
	 * Specifies the input jar name (or wars, ears, zips) of the application to be
	 * processed.
	 *
	 * <p>You may specify a classes directory e.g. 'classes'. This way the plugin will process
	 * the classes instead of the jar. You would need to bind the execution to phase 'compile'
	 * or 'process-classes' in this case.
	 *
	 * @parameter expression="${project.build.finalName}.jar"
	 * @required
	 */
	protected String injar;

	/**
	 * Set this to 'true' to bypass ProGuard processing when injar does not exists.
	 *
	 * @parameter default-value="false"
	 */
	private boolean injarNotExistsSkip;

	/**
	 * Apply ProGuard classpathentry filters to input jar. e.g. <code>!**.gif,!**&#47;tests/**</code>
	 *
	 * @parameter
	 */
	protected String inFilter;

	/**
	 * Apply ProGuard classpathentry filters to all input lib jars. e.g. {@code !META-INF/**,!META-INF/versions/9/**.class}
	 *
	 * @parameter
	 */
	protected String inLibsFilter;

	/**
	 * Specifies the names of the output jars. If not set, the input jar is overwritten.
	 *
	 * <p>If {@link #attach} is 'true' the value is ignored and the name is constructed based on classifier.
	 *
	 * @parameter
	 */
	protected String outjar;

	/**
	 * Apply ProGuard classpathentry filters to output jar. e.g. <code>!**.gif,!**&#47;tests/**</code>
	 *
	 * @parameter
	 */
	protected String outFilter;

	/**
	 * Specifies whether to attach the created artifact to the project.
	 *
	 * @parameter default-value="false"
	 */
	private boolean attach;

	/**
	 * Determines if {@link #attach} also attaches the {@link #mappingFileName} file.
	 *
	 * @parameter default-value="false"
	 */
	private boolean attachMap;

	/**
	 * Determines if {@link #attach} also attaches the {@link #seedFileName} file.
	 *
	 * @parameter default-value="false"
	 */
	private boolean attachSeed;

	/**
	 * Specifies attach artifact type.
	 *
	 * @parameter default-value="jar"
	 */
	private String attachArtifactType;

	/**
	 * Specifies attach artifact Classifier, ignored if {@link #attach} is 'false'.
	 *
	 * @parameter default-value="small"
	 */
	private String attachArtifactClassifier;

	/**
	 * Whether to append the {@link #attachArtifactClassifier} to the artifact final name.
	 *
	 * @parameter default-value="true"
	 */
	private boolean appendClassifier;

	/**
	 * Whether to include {@code META-INF/MANIFEST.MF} file
	 *
	 * @parameter default-value="false"
	 */
	private boolean addManifest;

	/**
	 * Whether to include {@code META-INF/maven/**} Maven descriptor.
	 *
	 * @parameter default-value="false"
	 */
	private boolean addMavenDescriptor;

	/**
	 * Directory containing the input and generated JAR.
	 *
	 * @parameter property="project.build.directory"
	 * @required
	 */
	protected File outputDirectory;

	/**
	 * The Maven project reference where the plugin is currently being executed. The default value is populated from
	 * Maven.
	 *
	 * @parameter property="project"
	 * @readonly
	 * @required
	 */
	protected MavenProject mavenProject;

	/**
	 * The plugin dependencies.
	 *
	 * @parameter property="plugin.artifacts"
	 * @required
	 * @readonly
	 */
	protected List<Artifact> pluginArtifacts;

	/**
	 * @component
	 */
	private MavenProjectHelper projectHelper;

	/**
	 * The Jar archiver.
	 *
	 * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
	 * @required
	 */
	private JarArchiver jarArchiver;

	/**
	 * The Maven archive configuration to use. Only if {@link #assembly} is used.
	 *
	 * @parameter
	 */
	protected MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

	/**
	 * The max memory the forked Java process should use, e.g. 256m
	 *
	 * @parameter
	 */
	protected String maxMemory;

	/**
	 * ProGuard main class name.
	 *
	 * @parameter default-value="proguard.ProGuard"
	 */
	protected String proguardMainClass = "proguard.ProGuard";

	/**
	 * Sets the name of the ProGuard mapping file.
	 *
	 * @parameter default-value="proguard_map.txt"
	 */
	protected String mappingFileName = "proguard_map.txt";

	/**
	 * Sets the name of the ProGuard seed file.
	 *
	 * @parameter default-value="proguard_seed.txt"
	 */
	protected String seedFileName = "proguard_seeds.txt";

	/**
	 * Sets the name of the ProGuard {@code -applymapping} file.
	 *
	 * @parameter
	 */
	protected File applyMappingFile;

	/**
	 * Specifies whether to enable <a href=
	 * "https://www.guardsquare.com/en/proguard/manual/examples#incremental">
	 * incremental obfuscation</a>
	 *
	 * @parameter default-value="false"
	 */
	private boolean incremental;

	/**
	 * The ProGuard jar to use. Useful for using beta versions of
	 * ProGuard that aren't yet on Maven Central.
	 *
	 * @parameter
	 */
	protected File proguardJar;

	/**
	 * If the plugin should be silent.
	 *
	 * @parameter default-value="false"
	 */
	private boolean silent;

	/**
	 * Bind ProGuard output to Maven plugin logging.
	 *
	 * @parameter default-value="false"
	 */
	private boolean bindToMavenLogging;

	private Log log;

	/**
	 * ProGuard filter which excludes the {@code MANIFEST.MF} file
	 */
	private static final String MANIFEST_FILTER = "!META-INF/MANIFEST.MF";
	/**
	 * ProGuard filter which excludes the Maven descriptors in the {@code META-INF/maven/} directory
	 */
	private static final String MAVEN_DESCRIPTORS_FILTER = "!META-INF/maven/**";

	/**
	 * ProGuard docs: Names with special characters like spaces and parentheses must be quoted with single or double
	 * quotes.
	 */
	private String fileNameToString(String fileName) {
		return "'" + fileName + "'";
	}

	private String fileToString(File file) {
		return fileNameToString(file.toString());
	}

	/**
	 * Creates a ProGuard classpath filter string.
	 */
	private String createFilterString(List<String> names) {
		if (names.isEmpty()) {
			return "";
		}

		return "(" + String.join(",", names) + ")";
	}

	private String createFilterString(String... names) {
		return createFilterString(Arrays.asList(names));
	}

	private String libFileToStringWithInLibsFilter(File file) {
		return libFileToStringWithInLibsFilter(file.toString());
	}

	private String libFileToStringWithInLibsFilter(String file) {
		StringBuilder filter = new StringBuilder(fileNameToString(file));
		if ((inLibsFilter != null)) {
			filter.append(createFilterString(inLibsFilter));
		}
		return filter.toString();
	}

	private boolean useArtifactClassifier() {
		return appendClassifier && ((attachArtifactClassifier != null) && (attachArtifactClassifier.length() > 0));
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		log = getLog();

		if (skip) {
			log.info("Bypass ProGuard processing because \"proguard.skip=true\"");
			return;
		}

		boolean mainIsJar = mavenProject.getPackaging().equals("jar");

		File inJarFile = new File(outputDirectory, injar);
		if (!inJarFile.exists()) {
			if (injarNotExistsSkip) {
				log.info("Bypass ProGuard processing because \"injar\" dos not exist");
				return;
			} else if (mainIsJar) {
				throw new MojoFailureException("Can't find file " + inJarFile);
			}
		}

		if (!outputDirectory.exists()) {
			if (!outputDirectory.mkdirs()) {
				throw new MojoFailureException("Can't create " + outputDirectory);
			}
		}

		File outJarFile;
		boolean sameArtifact;

		if (attach) {
			outjar = nameNoType(injar);
			if (useArtifactClassifier()) {
				outjar += "-" + attachArtifactClassifier;
			}
			outjar += "." + attachArtifactType;
		}

		if ((outjar != null) && (!outjar.equals(injar))) {
			sameArtifact = false;
			outJarFile = (new File(outputDirectory, outjar)).getAbsoluteFile();
			if (outJarFile.exists()) {
				if (!deleteFileOrDirectory(outJarFile)) {
					throw new MojoFailureException("Can't delete " + outJarFile);
				}
			}
		} else {
			sameArtifact = true;
			outJarFile = inJarFile.getAbsoluteFile();
			File baseFile;
			if (inJarFile.isDirectory()) {
				baseFile = new File(outputDirectory, nameNoType(injar) + "_proguard_base");
			} else {
				baseFile = new File(outputDirectory, nameNoType(injar) + "_proguard_base.jar");
			}
			if (baseFile.exists()) {
				if (!deleteFileOrDirectory(baseFile)) {
					throw new MojoFailureException("Can't delete " + baseFile);
				}
			}
			if (inJarFile.exists()) {
				if (!inJarFile.renameTo(baseFile)) {
					throw new MojoFailureException("Can't rename " + inJarFile);
				}
			}
			inJarFile = baseFile;
		}

		ArrayList<String> args = new ArrayList<String>();
		ArrayList<File> libraryJars = new ArrayList<File>();

		if (log.isDebugEnabled()) {
			@SuppressWarnings("unchecked")
			List<Artifact> dependancy = mavenProject.getCompileArtifacts();
			for (Artifact artifact : dependancy) {
				log.debug("--- compile artifact " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":"
						+ artifact.getType() + ":" + artifact.getClassifier() + " Scope:" + artifact.getScope());
			}

			@SuppressWarnings("unchecked")
			final Set<Artifact> artifacts = mavenProject.getArtifacts();
			for (Artifact artifact : artifacts) {
				log.debug("--- artifact " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":"
						+ artifact.getType() + ":" + artifact.getClassifier() + " Scope:" + artifact.getScope());
			}
			@SuppressWarnings("unchecked")
			final List<Dependency> dependencies = mavenProject.getDependencies();
			for (Dependency artifact : dependencies) {
				log.debug("--- dependency " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":"
						+ artifact.getType() + ":" + artifact.getClassifier() + " Scope:" + artifact.getScope());
			}
		}

		Set<String> inPath = new HashSet<String>();
		Map<Artifact, Inclusion> injars = new HashMap<Artifact, Inclusion>();
		Map<Artifact, Inclusion> libraryjars = new HashMap<Artifact, Inclusion>();
		boolean hasInclusionLibrary = false;
		if (assembly != null && assembly.inclusions != null) {
			for (Inclusion inc : assembly.inclusions) {
				for (Artifact artifact : getDependencies(inc, mavenProject)) {
					if (inc.library) {
						if (!injars.containsKey(artifact)) {
							libraryjars.put(artifact, inc);
						}
					} else {
						injars.put(artifact, inc);
						if (libraryjars.containsKey(artifact)) {
							libraryjars.remove(artifact);
						}
					}
				}
			}

			for (Entry<Artifact, Inclusion> entry : injars.entrySet()) {
				log.info("--- ADD injars:" + entry.getKey().getArtifactId());
				File file = getClasspathElement(entry.getKey(), mavenProject);
				inPath.add(file.toString());
				StringBuilder filter = new StringBuilder(fileToString(file));
				List<String> filterList = new ArrayList<>();
				if (!addManifest) {
					filterList.add(MANIFEST_FILTER);
				}
				if (!addMavenDescriptor) {
					filterList.add(MAVEN_DESCRIPTORS_FILTER);
				}
				if (entry.getValue().filter != null) {
					filterList.add(entry.getValue().filter);
				}
				filter.append(createFilterString(filterList));
				args.add("-injars");
				args.add(filter.toString());
			}

			for (Entry<Artifact, Inclusion> entry : libraryjars.entrySet()) {
				log.info("--- ADD libraryjars:" + entry.getKey().getArtifactId());
				File file = getClasspathElement(entry.getKey(), mavenProject);
				hasInclusionLibrary = true;
				inPath.add(file.toString());
				if (putLibraryJarsInTempDir) {
					libraryJars.add(file);
				} else {
					args.add("-libraryjars");
					args.add(libFileToStringWithInLibsFilter(file));
				}
			}
		}

		if (inJarFile.exists()) {
			args.add("-injars");
			StringBuilder filter = new StringBuilder(fileToString(inJarFile));
			if ((inFilter != null) || (!addMavenDescriptor)) {
				List<String> filterList = new ArrayList<>();

				if (!addMavenDescriptor) {
					filterList.add(MAVEN_DESCRIPTORS_FILTER);
				}

				if (inFilter != null) {
					filterList.add(inFilter);
				}

				filter.append(createFilterString(filterList));
			}
			args.add(filter.toString());
		}

		if (includeDependency) {
			List<String> dependencyInjarFilterList = new ArrayList<>();
			if (!addManifest) {
				dependencyInjarFilterList.add(MANIFEST_FILTER);
			}
			if (!addMavenDescriptor) {
				dependencyInjarFilterList.add(MAVEN_DESCRIPTORS_FILTER);
			}
			if (inFilter != null) {
				dependencyInjarFilterList.add(inFilter);
			}
			String dependencyInjarFilter = createFilterString(dependencyInjarFilterList);

			@SuppressWarnings("unchecked")
			List<Artifact> dependency = this.mavenProject.getCompileArtifacts();
			for (Artifact artifact : dependency) {
				// dependency filter
				if (isExclusion(artifact)) {
					continue;
				}
				File file = getClasspathElement(artifact, mavenProject);

				if (inPath.contains(file.toString())) {
					log.debug("--- ignore library since one in injar:" + artifact.getArtifactId());
					continue;
				}
				if (includeDependencyInjar) {
					log.debug("--- ADD library as injars:" + artifact.getArtifactId());
					args.add("-injars");
					args.add(fileToString(file) + dependencyInjarFilter);
				} else {
					log.debug("--- ADD libraryjars:" + artifact.getArtifactId());
					if (putLibraryJarsInTempDir) {
						libraryJars.add(file);
					} else {
						args.add("-libraryjars");
						args.add(libFileToStringWithInLibsFilter(file));
					}
				}
			}
		}

		if (args.contains("-injars")) {
			args.add("-outjars");
			StringBuilder filter = new StringBuilder(fileToString(outJarFile));
			if (outFilter != null) {
				filter.append(createFilterString(outFilter));
			}
			args.add(filter.toString());
		}

		if (!obfuscate) {
			args.add("-dontobfuscate");
		}

		if (proguardInclude != null) {
			if (proguardInclude.exists()) {
				args.add("-include");
				args.add(fileToString(proguardInclude));
				log.debug("proguardInclude " + proguardInclude);
			} else {
				log.debug("proguardInclude config does not exists " + proguardInclude);
			}
		}

		if (libs != null) {
			for (String lib : libs) {
				if (putLibraryJarsInTempDir) {
					libraryJars.add(new File(lib));
				} else {
					args.add("-libraryjars");
					args.add(libFileToStringWithInLibsFilter(lib));
				}
			}
		}

		if (!libraryJars.isEmpty()) {
			log.debug("Copy libraryJars to temporary directory");
			log.debug("Temporary directory: " + tempLibraryjarsDir);
			if (tempLibraryjarsDir.exists()) {
				try {
					FileUtils.deleteDirectory(tempLibraryjarsDir);
				} catch (IOException ignored) {
					throw new MojoFailureException("Deleting failed libraryJars directory", ignored);
				}
			}
			tempLibraryjarsDir.mkdir();
			if (!tempLibraryjarsDir.exists()) {
				throw new MojoFailureException(
						"Can't create temporary libraryJars directory: " + tempLibraryjarsDir.getAbsolutePath());
			}
			// Use this subdirectory for all libraries that are files, and not directories themselves
			File commonDir = new File(tempLibraryjarsDir, "0");
			commonDir.mkdir();

			int directoryIndex = 1;
			for (File libraryJar : libraryJars) {
				try {
					log.debug("Copying library: " + libraryJar);
					if (libraryJar.isFile()) {
						FileUtils.copyFileToDirectory(libraryJar, commonDir);
					} else {
						File subDir = new File(tempLibraryjarsDir, String.valueOf(directoryIndex));
						FileUtils.copyDirectory(libraryJar, subDir);
						args.add("-libraryjars");
						args.add(libFileToStringWithInLibsFilter(subDir));
					}
				} catch (IOException e) {
					throw new MojoFailureException("Can't copy to temporary libraryJars directory", e);
				}
				directoryIndex++;
			}
			args.add("-libraryjars");
			args.add(libFileToStringWithInLibsFilter(commonDir));
		}

		File mappingFile = new File(outputDirectory, mappingFileName);
		args.add("-printmapping");
		args.add(fileToString(mappingFile.getAbsoluteFile()));

		args.add("-printseeds");
		args.add(fileToString((new File(outputDirectory, seedFileName).getAbsoluteFile())));

		if (incremental && applyMappingFile == null) {
			throw new MojoFailureException("applyMappingFile is required if incremental is true");
		}

		if (applyMappingFile != null && (!incremental || applyMappingFile.exists())) {
			args.add("-applymapping");
			args.add(fileToString(applyMappingFile.getAbsoluteFile()));
		}

		if (log.isDebugEnabled()) {
			args.add("-verbose");
		}

		if (options != null) {
			Collections.addAll(args, options);
		}


		if(generateTemporaryConfigurationFile) {
			log.info("building config file");

			StringBuilder stringBuilder = new StringBuilder();
			for (String arg : args) {
				if (arg.startsWith("-")) {
					stringBuilder.append("\n");
				} else {
					stringBuilder.append(" ");
				}
				stringBuilder.append(arg);
			}

			try (FileWriter writer = new FileWriter(temporaryConfigurationFile);) {
				IOUtils.write(stringBuilder.toString(), writer);
			} catch (IOException e) {
				throw new MojoFailureException("cannot write to temporary configuration file " + temporaryConfigurationFile, e);
			}

			args = new ArrayList<String>();
			args.add("-include");
			args.add(fileToString(temporaryConfigurationFile));
		}

		log.info("execute ProGuard " + args.toString());
		proguardMain(getProguardJars(this), args, this);

		if (!libraryJars.isEmpty()) {
			deleteFileOrDirectory(tempLibraryjarsDir);
		}

		if ((assembly != null) && (hasInclusionLibrary)) {

			log.info("creating assembly");

			File baseFile = new File(outputDirectory, nameNoType(injar) + "_proguard_result.jar");
			if (baseFile.exists()) {
				if (!baseFile.delete()) {
					throw new MojoFailureException("Can't delete " + baseFile);
				}
			}
			File archiverFile = outJarFile.getAbsoluteFile();
			if (!outJarFile.renameTo(baseFile)) {
				throw new MojoFailureException("Can't rename " + outJarFile);
			}

			MavenArchiver archiver = new MavenArchiver();
			archiver.setArchiver(jarArchiver);
			archiver.setOutputFile(archiverFile);
			archive.setAddMavenDescriptor(addMavenDescriptor);

			try {
				jarArchiver.addArchivedFileSet(baseFile);
				for (Entry<Artifact, Inclusion> entry : libraryjars.entrySet()) {
					File file;
					file = getClasspathElement(entry.getKey(), mavenProject);
					if (file.isDirectory()) {
						getLog().info("merge project: " + entry.getKey() + " " + file);
						jarArchiver.addDirectory(file);
					} else {
						getLog().info("merge artifact: " + entry.getKey());
						
						// Respect filter if set
						String filter = entry.getValue().filter;
						if(filter == null) {
							jarArchiver.addArchivedFileSet(file);
						} else {
						    
						    // Filter elements must be separated int two lists
						    List<String> includes = new ArrayList<String>();
						    List<String> excludes = new ArrayList<String>();

						    // Elements starting with ! should be excluded while others should be included
						    for(String element : filter.split(",")) {
							if(element.startsWith("!")) {
							    excludes.add(element.substring(1));
							}else {
							    includes.add(element);
							}
						    }

						    // Null is important on empty includes otherwise nothing gets included
						    jarArchiver.addArchivedFileSet(file,
							    (includes.isEmpty() ? null : includes.toArray(new String[0])),
							    (excludes.isEmpty() ? null : excludes.toArray(new String[0])));
						}
					}
				}

				archiver.createArchive(mavenProject, archive);

			} catch (Exception e) {
				throw new MojoExecutionException("Unable to create jar", e);
			}

		}

		if (incremental) {
			log.info("Merging mapping file into " + applyMappingFile);

			try {
				FileInputStream mappingFileIn = new FileInputStream(mappingFile);
				try {
					applyMappingFile.getParentFile().mkdirs();
					FileOutputStream mappingFileOut = new FileOutputStream(applyMappingFile, true);
					try {
						IOUtils.copy(mappingFileIn, mappingFileOut);
					} finally {
						mappingFileOut.close();
					}
				} finally {
					mappingFileIn.close();
				}
			} catch (IOException e) {
				throw new MojoExecutionException("Unable to merge mapping file", e);
			}
		}

		if (attach) {
			if (!sameArtifact) {
				final String classifier;
				if (useArtifactClassifier()) {
					classifier = attachArtifactClassifier;
				} else {
					classifier = null;
				}

				projectHelper.attachArtifact(mavenProject, attachArtifactType, classifier, outJarFile);
			}

			final String mainClassifier = useArtifactClassifier() ? attachArtifactClassifier : null;
			final File buildOutput = new File(mavenProject.getBuild().getDirectory());
			if (attachMap) {
				attachTextFile(new File(buildOutput, mappingFileName), mainClassifier, "map");
			}
			if (attachSeed) {
				attachTextFile(new File(buildOutput, seedFileName), mainClassifier, "seed");
			}
		}
	}

	private void attachTextFile(File theFile, String mainClassifier, String suffix) {
		final String classifier = (null == mainClassifier ? "" : mainClassifier + "-") + suffix;
		log.info("Attempting to attach " + suffix + " artifact");
		if (theFile.exists()) {
			if (theFile.isFile()) {
				projectHelper.attachArtifact(mavenProject, "txt", classifier, theFile);
			} else {
				log.warn("Cannot attach file because it is not a file: " + theFile);
			}
		} else {
			log.warn("Cannot attach file because it does not exist: " + theFile);

		}
	}
	
	private Set<File> getAllPluginArtifactDependencies(ProGuardMojo mojo) throws MojoExecutionException {
		Set<File> files = new HashSet<>(getProguardJars(mojo));
		for (Artifact artifact : mojo.pluginArtifacts) {
			files.add(artifact.getFile().getAbsoluteFile());
			files.addAll(getChildArtifacts(artifact));
		}
		
		return files;
	}

	private Set<File> getChildArtifacts(Artifact artifact) {
		Set<File> files = new HashSet<>();
		for (Object child : artifact.getDependencyTrail()) {
			if (child instanceof Artifact) {
				files.add(((Artifact) child).getFile().getAbsoluteFile());
				files.addAll(getChildArtifacts((Artifact) child));
			}
		}
		return files;
	}

	private List<File> getProguardJars(ProGuardMojo mojo) throws MojoExecutionException {

		if (proguardJar != null) {
			if (proguardJar.exists()) {
				if (proguardJar.isFile()) {
					return Collections.singletonList(proguardJar);
				} else {
					mojo.getLog().warn("proguard jar (" + proguardJar + ") is not a file");
					throw new MojoExecutionException("proguard jar (" + proguardJar + ") is not a file");
				}
			} else {
				mojo.getLog().warn("proguard jar (" + proguardJar + ") does not exist");
				throw new MojoExecutionException("proguard jar (" + proguardJar + ") does not exist");
			}
		}

		List<Artifact> proguardArtifacts = new ArrayList<Artifact>();
		int proguardArtifactDistance = -1;
		// This should be solved in Maven 2.1
		//Starting in v. 7.0.0., proguard got split up in proguard-base and proguard-core,
		//both of which need to be on the classpath.
		for (Artifact artifact : mojo.pluginArtifacts) {
			mojo.getLog().debug("pluginArtifact: " + artifact.getFile());

			final String artifactId = artifact.getArtifactId();
			if (artifactId.startsWith((useDexGuard ? "dexguard" : "proguard"))
					&& !artifactId.startsWith("proguard-maven-plugin")) {
				int distance = artifact.getDependencyTrail().size();
				mojo.getLog().debug("proguard DependencyTrail: " + distance);

				// Skip dependency if proguardVersion is defined and dependency does not match given version
				if ((mojo.proguardVersion != null) && (!mojo.proguardVersion.equals(artifact.getVersion()))) {
				    continue;
				}

				/*
				 *  Check if artifact has been defined twice - eg. no proguardVersion given but dependency for proguard
				 *  defined in plugin config
				 */
				for (Artifact existingArtifact : proguardArtifacts) {
				    if(existingArtifact.getArtifactId().equals(artifactId)) {
					mojo.getLog().warn("Dependency for proguard defined twice! This may lead to unexpected results: "
						+ existingArtifact.getArtifactId() + ":" + existingArtifact.getVersion()
						+ " | "
						+ artifactId + ":" + artifact.getVersion());
					break;
				    }
				}

				if ((mojo.proguardVersion != null) && (mojo.proguardVersion.equals(artifact.getVersion()))) {
					proguardArtifacts.add(artifact);
				} else if (proguardArtifactDistance == -1) {
					proguardArtifacts.add(artifact);
					proguardArtifactDistance = distance;
				} else if (distance <= proguardArtifactDistance) {
					Iterator<Artifact> it = proguardArtifacts.iterator();
					while (it.hasNext()) {
						Artifact art = it.next();
						if (distance < art.getDependencyTrail().size())
							it.remove();
					}
					proguardArtifacts.add(artifact);
					proguardArtifactDistance = distance;
				}
			}
		}
		if (!proguardArtifacts.isEmpty()) {
			List<File> resList = new ArrayList<File>(proguardArtifacts.size());
			for (Artifact p : proguardArtifacts) {
				mojo.getLog().debug("proguardArtifact: " + p.getFile());
				resList.add(p.getFile().getAbsoluteFile());
			}
			return resList;
		}
		mojo.getLog().info((useDexGuard ? "dexguard" : "proguard") + " jar not found in pluginArtifacts");

		ClassLoader cl;
		cl = mojo.getClass().getClassLoader();
		// cl = Thread.currentThread().getContextClassLoader();
		String classResource = "/" + mojo.proguardMainClass.replace('.', '/') + ".class";
		URL url = cl.getResource(classResource);
		if (url == null) {
			throw new MojoExecutionException(
					"Obfuscation failed ProGuard (" + mojo.proguardMainClass + ") not found in classpath");
		}
		String proguardJar = url.toExternalForm();
		if (proguardJar.startsWith("jar:file:")) {
			proguardJar = proguardJar.substring("jar:file:".length());
			proguardJar = proguardJar.substring(0, proguardJar.indexOf('!'));
		} else {
			throw new MojoExecutionException("Unrecognized location (" + proguardJar + ") in classpath");
		}
		return Collections.singletonList(new File(proguardJar));
	}

	private void proguardMain(Collection<File> proguardJars, List<String> argsList, ProGuardMojo mojo)
			throws MojoExecutionException {

		Java java = new Java();

		Project antProject = new Project();
		antProject.setName(mojo.mavenProject.getName());
		antProject.init();

		DefaultLogger antLogger;
		if (bindToMavenLogging) {
			antLogger = new MavenloggingBinder(mojo.log);
		} else {
			antLogger = new DefaultLogger();
			antLogger.setOutputPrintStream(System.out);
			antLogger.setErrorPrintStream(System.err);
		}
		int logLevel = mojo.log.isDebugEnabled() ? Project.MSG_DEBUG : Project.MSG_INFO;
		antLogger.setMessageOutputLevel(silent ? Project.MSG_ERR : logLevel);

		antProject.addBuildListener(antLogger);
		antProject.setBaseDir(mojo.mavenProject.getBasedir());

		java.setProject(antProject);
		java.setTaskName("proguard");

		mojo.getLog().info("proguard jar: " + proguardJars);
		
		Set<File> allDependencyFiles = getAllPluginArtifactDependencies(mojo);

		for (File p : allDependencyFiles)
			java.createClasspath().createPathElement().setLocation(p);
		// java.createClasspath().setPath(System.getProperty("java.class.path"));
		java.setClassname(mojo.proguardMainClass);

		java.setFailonerror(true);

		java.setFork(true);

		// get the maxMemory setting
		if (mojo.maxMemory != null) {
			java.setMaxmemory(mojo.maxMemory);
		}

		for (String arg : argsList) {
			java.createArg().setValue(arg);
		}

		int result = java.executeJava();
		if (result != 0) {
			throw new MojoExecutionException("Obfuscation failed (result=" + result + ")");
		}
	}

	private String nameNoType(String fileName) {
		int extStart = fileName.lastIndexOf('.');
		if (extStart == -1) {
			return fileName;
		}
		return fileName.substring(0, extStart);
	}

	private boolean deleteFileOrDirectory(File path) throws MojoFailureException {
		if (path.isDirectory()) {
			File[] files = path.listFiles();
			if (null != files) {
				for (File file : files) {
					if (file.isDirectory()) {
						if (!deleteFileOrDirectory(file)) {
							throw new MojoFailureException("Can't delete dir " + file);
						}
					} else {
						if (!file.delete()) {
							throw new MojoFailureException("Can't delete file " + file);
						}
					}
				}
			}
			return path.delete();
		} else {
			return path.delete();
		}
	}

	private Set<Artifact> getDependencies(final Inclusion inc, MavenProject mavenProject)
			throws MojoExecutionException {
		@SuppressWarnings("unchecked")
		Set<Artifact> dependencies = mavenProject.getArtifacts();
		Set<Artifact> result = new HashSet<Artifact>();
		for (Artifact artifact : dependencies) {
			if (inc.match(artifact)) {
				result.add(artifact);
			}
		}
		if (result.isEmpty()) {
			log.warn(String.format("No artifact found : %s:%s", inc.artifactId, inc.groupId));
		}
		return result;
	}

	private boolean isExclusion(Artifact artifact) {
		if (exclusions == null) {
			return false;
		}
		for (Exclusion excl : exclusions) {
			if (excl.match(artifact)) {
				return true;
			}
		}
		return false;
	}

	private File getClasspathElement(Artifact artifact, MavenProject mavenProject) throws MojoExecutionException {
		if (artifact.getClassifier() != null) {
			return artifact.getFile();
		}
		String refId = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
		MavenProject project = (MavenProject) mavenProject.getProjectReferences().get(refId);
		if (project == null) {
			refId = artifact.getGroupId() + ":" + artifact.getArtifactId();
			project = (MavenProject) mavenProject.getProjectReferences().get(refId);
		}
		if (project != null) {
			File file = new File(project.getBuild().getOutputDirectory());
			log.debug("Found directory: " + file.getAbsolutePath());
			return file;
		} else {
			File file = artifact.getFile();
			log.debug("Found file: " + file.getAbsolutePath());
			if ((file == null) || (!file.exists())) {
				throw new MojoExecutionException("Dependency Resolution Required " + artifact);
			}
			return file;
		}
	}
}
