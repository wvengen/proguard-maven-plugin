package com.pyx4me.maven.proguard;

import org.apache.maven.artifact.Artifact;

public class ArtifactFilter {

	protected String groupId;
	
	protected String artifactId;
	
	protected String classifier;
	
	public boolean match(Artifact artifact) {
			return (artifact.getArtifactId().equals(this.artifactId) && artifact.getGroupId().equals(this.groupId)
					&& (((this.classifier == null) && (artifact.getClassifier() == null)) || 
							((this.classifier != null) && this.classifier.equals(artifact.getClassifier())))); 
	}
}
