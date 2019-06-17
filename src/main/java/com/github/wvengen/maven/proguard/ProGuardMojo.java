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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

/**
 *
 * <p>
 * The Obfuscate task provides a stand-alone obfuscation task
 * </p>
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
	 * Recursively reads configuration options from the given file filename
	 *
	 * @parameter default-value="${basedir}/proguard.conf"
	 */
	private File proguardInclude;

	/**
	 * Select specific ProGuard version from plugin dependencies
	 *
	 * @parameter
	 */
	private String proguardVersion;

	/**
	 * To run DexGuard instead of ProGuard, set this to "true".
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
	 * Specifies not to obfuscate the input class files.
	 *
	 * @parameter default-value="true"
	 */
	private boolean obfuscate;

	/**
	 * Specifies that project compile dependencies be added as -libraryjars to proguard arguments. Dependency itself is
	 * not included in resulting jar unless you set includeDependencyInjar to true
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
	 * Specifies to copy all the -libraryjars dependencies into a temporary directory and pass that directory
	 * as the only -libraryjars argument to ProGuard.
	 *
	 * @parameter default-value="false"
	 */
	private boolean putLibraryJarsInTempDir;

	/**
	 * Sets an exclude for all library jars, eg: (!META-INF/versions/**)
	 *
	 * @parameter default-value=""
	 */
	private String libraryJarExclusion;

	/**
	 * Specifies that project compile dependencies should be added as injar.
	 *
	 * @parameter default-value="false"
	 */
	private boolean includeDependencyInjar;

	/**
	 * Bundle project dependency to resulting jar. Specifies list of artifact inclusions
	 *
	 * @parameter
	 */
	private Assembly assembly;

	/**
	 * Additional -libraryjars e.g. ${java.home}/lib/rt.jar Project compile dependency are added automatically. See
	 * exclusions
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
	 * You may specify a classes directory e.g. 'classes'. This way plugin will processed
	 * the classes instead of jar. You would need to bind the execution to phase 'compile'
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
	 * Apply ProGuard classpathentry Filters to input jar. e.g. <code>!**.gif,!**&#47;tests&#47;**'</code>
	 *
	 * @parameter
	 */
	protected String inFilter;

	/**
	 * Specifies the names of the output jars. If attach=true the value ignored and name constructed base on classifier
	 * If empty input jar would be overdriven.
	 *
	 * @parameter
	 */
	protected String outjar;

	/**
	 * Apply ProGuard classpathentry Filters to output jar. e.g. <code>!**.gif,!**&#47;tests&#47;**'</code>
	 *
	 * @parameter
	 */
	protected String outFilter;

	/**
	 * Specifies whether or not to attach the created artifact to the project
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
	 * Specifies attach artifact type
	 *
	 * @parameter default-value="jar"
	 */
	private String attachArtifactType;

	/**
	 * Specifies attach artifact Classifier, Ignored if attach=false
	 *
	 * @parameter default-value="small"
	 */
	private String attachArtifactClassifier;

	/**
	 * Set to false to exclude the attachArtifactClassifier from the Artifact final name. Default value is true.
	 *
	 * @parameter default-value="true"
	 */
	private boolean appendClassifier;

	/**
	 * Set to true to include META-INF/maven/** maven descriptord
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
	 * maven.
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
	 * The maven archive configuration to use. only if assembly is used.
	 *
	 * @parameter
	 */
	protected MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

	/**
	 * The max memory the forked java process should use, e.g. 256m
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
	 * Sets the name of the ProGuard applying mapping file.
	 *
	 * @parameter
	 */
	protected File applyMappingFile;

	/**
	 * Specifies whether or not to enable <a href=
	 * "https://www.guardsquare.com/en/proguard/manual/examples#incremental">
	 * incremental obfuscation</a>
	 *
	 * @parameter default-value="false"
	 */
	private boolean incremental;

	/**
	 * The proguard jar to use. useful for using beta versions of
	 * progaurd that aren't yet on Maven central.
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

	private Log log;

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

	private boolean useArtifactClassifier() {
		return appendClassifier && ((attachArtifactClassifier != null) && (attachArtifactClassifier.length() > 0));
	}

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
				filter.append("(!META-INF/MANIFEST.MF");
				if (!addMavenDescriptor) {
					filter.append(",");
					filter.append("!META-INF/maven/**");
				}
				if (entry.getValue().filter != null) {
					filter.append(",").append(entry.getValue().filter);
				}
				filter.append(")");
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
					args.add(fileToString(file));
				}
			}
		}

		if (inJarFile.exists()) {
			args.add("-injars");
			StringBuilder filter = new StringBuilder(fileToString(inJarFile));
			if ((inFilter != null) || (!addMavenDescriptor)) {
				filter.append("(");
				boolean coma = false;

				if (!addMavenDescriptor) {
					coma = true;
					filter.append("!META-INF/maven/**");
				}

				if (inFilter != null) {
					if (coma) {
						filter.append(",");
					}
					filter.append(inFilter);
				}

				filter.append(")");
			}
			args.add(filter.toString());
		}


		if (includeDependency) {
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
					args.add(fileToString(file));
				} else {
					log.debug("--- ADD libraryjars:" + artifact.getArtifactId());
					addLibraryJar(args, libraryJars, file);
				}
			}
		}

		if (args.contains("-injars")) {
			args.add("-outjars");
			StringBuilder filter = new StringBuilder(fileToString(outJarFile));
			if (outFilter != null) {
				filter.append("(").append(outFilter).append(")");
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
				addLibraryJar(args, libraryJars, new File(lib));
			}
		}

		if (!libraryJars.isEmpty()) {
			log.debug("Copy libraryJars to temporary directory");
			log.debug("Temporary directory: " + tempLibraryjarsDir);
			if (tempLibraryjarsDir.exists()) {
							try{
								FileUtils.deleteDirectory(tempLibraryjarsDir);
							} catch(IOException ignored){
								// NO-OP
							}
			}
			tempLibraryjarsDir.mkdir();
			if (!tempLibraryjarsDir.exists()) {
				throw new MojoFailureException("Can't create temporary libraryJars directory: " + tempLibraryjarsDir.getAbsolutePath());
			}
			for (File libraryJar : libraryJars) {
				try {
					FileUtils.copyFileToDirectory(libraryJar, tempLibraryjarsDir);
				} catch (IOException e) {
					throw new MojoFailureException("Can't copy to temporary libraryJars directory", e);
				}
			}
			args.add("-libraryjars");
			args.add(fileToString(tempLibraryjarsDir));
		}

		File mappingFile = new File(outputDirectory, mappingFileName);
		args.add("-printmapping");
		args.add(fileToString(mappingFile.getAbsoluteFile()));

		args.add("-printseeds");
		args.add(fileToString((new File(outputDirectory,seedFileName).getAbsoluteFile())));

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

		log.info("execute ProGuard " + args.toString());
		proguardMain(getProguardJar(this), args, this);


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
						jarArchiver.addArchivedFileSet(file);
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
						IOUtil.copy(mappingFileIn, mappingFileOut);
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

	private void addLibraryJar(ArrayList<String> args, ArrayList<File> libraryJars, File file)
	{
		if (putLibraryJarsInTempDir) {
			libraryJars.add(file);
		} else {
			args.add("-libraryjars");
			args.add(fileToString(file));
			if (libraryJarExclusion != null) {
				args.add(libraryJarExclusion);
			}
		}
	}

	private void attachTextFile(File theFile, String mainClassifier, String suffix) {
		final String classifier = (null == mainClassifier ? "" : mainClassifier+"-") + suffix;
		log.info("Attempting to attach "+suffix+" artifact");
		if (theFile.exists()) {
			if (theFile.isFile()) {
				projectHelper.attachArtifact(mavenProject, "txt", classifier, theFile);
			} else {
				log.warn("Cannot attach file because it is not a file: "+theFile);
			}
		} else {
			log.warn("Cannot attach file because it does not exist: "+theFile);

		}
	}

	private File getProguardJar(ProGuardMojo mojo) throws MojoExecutionException {

		if (proguardJar != null) {
			if (proguardJar.exists()) {
				if (proguardJar.isFile()) {
					return proguardJar;
				} else {
					mojo.getLog().warn("proguard jar (" + proguardJar + ") is not a file");
					throw new MojoExecutionException("proguard jar (" + proguardJar + ") is not a file");
				}
			} else {
				mojo.getLog().warn("proguard jar (" + proguardJar + ") does not exist");
				throw new MojoExecutionException("proguard jar (" + proguardJar + ") does not exist");
			}
		}

		Artifact proguardArtifact = null;
		int proguardArtifactDistance = -1;
		// This should be solved in Maven 2.1
		for (Artifact artifact : mojo.pluginArtifacts) {
			mojo.getLog().debug("pluginArtifact: " + artifact.getFile());
			final String artifactId = artifact.getArtifactId();
			if (artifactId.startsWith((useDexGuard?"dexguard":"proguard")) &&
				!artifactId.startsWith("proguard-maven-plugin")) {
				int distance = artifact.getDependencyTrail().size();
				mojo.getLog().debug("proguard DependencyTrail: " + distance);
				if ((mojo.proguardVersion != null) && (mojo.proguardVersion.equals(artifact.getVersion()))) {
					proguardArtifact = artifact;
					break;
				} else if (proguardArtifactDistance == -1) {
					proguardArtifact = artifact;
					proguardArtifactDistance = distance;
				} else if (distance < proguardArtifactDistance) {
					proguardArtifact = artifact;
					proguardArtifactDistance = distance;
				}
			}
		}
		if (proguardArtifact != null) {
			mojo.getLog().debug("proguardArtifact: " + proguardArtifact.getFile());
			return proguardArtifact.getFile().getAbsoluteFile();
		}
		mojo.getLog().info((useDexGuard?"dexguard":"proguard") + " jar not found in pluginArtifacts");

		ClassLoader cl;
		cl = mojo.getClass().getClassLoader();
		// cl = Thread.currentThread().getContextClassLoader();
		String classResource = "/" + mojo.proguardMainClass.replace('.', '/') + ".class";
		URL url = cl.getResource(classResource);
		if (url == null) {
			throw new MojoExecutionException("Obfuscation failed ProGuard (" + mojo.proguardMainClass
					+ ") not found in classpath");
		}
		String proguardJar = url.toExternalForm();
		if (proguardJar.startsWith("jar:file:")) {
			proguardJar = proguardJar.substring("jar:file:".length());
			proguardJar = proguardJar.substring(0, proguardJar.indexOf('!'));
		} else {
			throw new MojoExecutionException("Unrecognized location (" + proguardJar + ") in classpath");
		}
		return new File(proguardJar);
	}

	private void proguardMain(File proguardJar, List<String> argsList, ProGuardMojo mojo)
			throws MojoExecutionException {

		Java java = new Java();

		Project antProject = new Project();
		antProject.setName(mojo.mavenProject.getName());
		antProject.init();

		DefaultLogger antLogger = new DefaultLogger();
		antLogger.setOutputPrintStream(System.out);
		antLogger.setErrorPrintStream(System.err);
		int logLevel = mojo.log.isDebugEnabled() ? Project.MSG_DEBUG : Project.MSG_INFO;
		antLogger.setMessageOutputLevel(silent ? Project.MSG_ERR : logLevel);

		antProject.addBuildListener(antLogger);
		antProject.setBaseDir(mojo.mavenProject.getBasedir());

		java.setProject(antProject);
		java.setTaskName("proguard");

		mojo.getLog().info("proguard jar: " + proguardJar);

		java.createClasspath().setLocation(proguardJar);
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

	private Set<Artifact> getDependencies(final Inclusion inc, MavenProject mavenProject) throws MojoExecutionException {
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
			return new File(project.getBuild().getOutputDirectory());
		} else {
			File file = artifact.getFile();
			if ((file == null) || (!file.exists())) {
				throw new MojoExecutionException("Dependency Resolution Required " + artifact);
			}
			return file;
		}
	}
}
