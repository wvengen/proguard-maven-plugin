ProGuard Maven Plugin (with WAR support)
----------------------------------------

This project is a fork of [wvengen's proguard maven plugin](https://github.com/wvengen/proguard-maven-plugin)
with focus on easier _war_ file processing with [ProGuard].

If plugin's input jar is a war file, the plugin uses the following workflow:
  
  1. The war file is extracted to a temporary directory 
     (in build directory, with _'_war_proguard_expanded'_ appended to base war file name)        
  1. Artifacts referenced by assembly inclusions section (with wildcard supported for artifactId)
     are used as ProGuard _injars_. All other artifacts are used as _libraryjars_.
     Input artifacts files that exist in _WEB-INF/lib_, are referenced using that location
     rather than location within user's maven repository. 
  1. Proguarded classes and resources will be out to a jar file in _WEB-INF/lib_ named by the artifact.
     Processed input jars located in _WEB-INF/lib_ are deleted.
  1. If _processWarClassesDir_ option is enabled, _WEB-INF/classes_ will be processed as _injars_
     and output separately (to replace existing _WEB-INF/classes_ directory).
  1. Finally, output war archive is created. 

Additional configuration parameters supported:

 - processWarClassesDir - if enabled, WEB-INF/classes will be processed as _injars_
 - attachMap - whether or not to attach proguard map file as an artifact
 - attachMapArtifactType - defaults to _txt_
 - attachMapArtifactClassifier - defaults to _proguard-map_
 - attachSeed - whether or not to attach proguard seed file as an artifact
 - attachSeedArtifactType - defaults to _txt_
 - attachSeedArtifactClassifier - defaults to _proguard-seed_


### Configuration example for war

    <plugin>
      <groupId>com.github.radomirml</groupId>
      <artifactId>proguard-maven-plugin</artifactId>
      <version>2.0.9</version>
      <executions>
        <execution>
          <phase>package</phase>
          <goals>
            <goal>proguard</goal>
          </goals>
        </execution>
      </executions>
      <configuration>
        <proguardVersion>4.8</proguardVersion>
        <obfuscate>true</obfuscate>
        <maxMemory>1024m</maxMemory>
        <includeDependency>true</includeDependency>
        <processWarClassesDir>true</processWarClassesDir>
        <options>    
          <option>-ignorewarnings</option>  
          <option>-repackageclasses xyz</option>
          <option>-dontoptimize</option>
        </options>
        <assembly>
          <inclusions>
            <inclusion>
              <groupId>${project.groupId}</groupId><artifactId>*</artifactId>
            </inclusion>
          </inclusions>
        </assembly>
        <exclusions>
          <exclusion>
            <groupId>commons-logging</groupId><artifactId>*</artifactId>
          </exclusion>
          <exclusion>
            <groupId>log4j</groupId><artifactId>*</artifactId>
          </exclusion>
        </exclusions>
        <injar>${project.build.finalName}.war</injar>
        <!--<outjar>${project.build.finalName}-proguarded.war</outjar> - not required (proguarded added by default) -->
        <outputDirectory>${project.build.directory}</outputDirectory>    
        <attach>true</attach>
        <appendClassifier>true</appendClassifier>
        <attachArtifactClassifier>proguarded</attachArtifactClassifier>
        <attachArtifactType>war</attachArtifactType>
        <attachMap>true</attachMap>
        <attachSeed>true</attachSeed>
        <proguardInclude>${basedir}/proguard.conf</proguardInclude>
        <libs>
          <lib>${java.home}/lib/rt.jar</lib>
        </libs>
        <addMavenDescriptor>false</addMavenDescriptor>
        <skip>false</skip>
      </configuration>
    </plugin>


## Base project description

Run [ProGuard] as part of your [Maven] build. For usage, please read the
generated [Documentation](http://wvengen.github.io/proguard-maven-plugin/).

Development happens at [Github](https://github.com/wvengen/proguard-maven-plugin).
This plugin is currently not under active development, but pull requests are
welcome.

This is the successor of the [ProGuard Maven Plugin by pyx4me](http://pyx4me.com/pyx4me-maven-plugins/proguard-maven-plugin/).


[![Build Status](https://travis-ci.org/wvengen/proguard-maven-plugin.svg?branch=master)](https://travis-ci.org/wvengen/proguard-maven-plugin)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.wvengen/proguard-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.wvengen/proguard-maven-plugin)


[ProGuard]: http://proguard.sourceforge.net/
[Maven]: http://apache.maven.org/
