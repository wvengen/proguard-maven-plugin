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

import org.apache.maven.artifact.Artifact;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ArtifactFilter {

    private static final String WILDCARD = "*";

    protected String groupId;

    protected String artifactId;

    protected String classifier;

    public boolean match(Artifact artifact) {
        boolean artifactMatch = WILDCARD.equals(artifactId) || artifact.getArtifactId().equals(this.artifactId) ||
                (artifactId != null && getMatcher(artifact).matches());
        boolean groupMatch = artifact.getGroupId().equals(this.groupId);
        boolean classifierMatch = ((this.classifier == null) && (artifact.getClassifier() == null)) || ((this.classifier != null) && this.classifier.equals(artifact.getClassifier()));
        return artifactMatch && groupMatch && classifierMatch;
    }

    private Matcher getMatcher(Artifact artifact) {
        try {
            Pattern compile = Pattern.compile(artifactId);
            return compile.matcher(artifact.getArtifactId());
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid regex artifact filter: " + this, e);
        }
    }

    @Override
    public String toString() {
        return "groupId:" + groupId + ", artifactId:" + artifactId + ", classifier:" + classifier;
    }
}
