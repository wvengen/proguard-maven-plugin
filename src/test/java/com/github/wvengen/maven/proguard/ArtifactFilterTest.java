package com.github.wvengen.maven.proguard;


import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.Assert;
import org.junit.Test;

public class ArtifactFilterTest {

    private ArtifactFilter artifactFilter = new ArtifactFilter();

    @Test
    public void wildcardMatch() {
        artifactFilter.groupId = "com.mahifx";
        artifactFilter.artifactId = "*";
        Assert.assertTrue(artifactFilter.match(getArtifact()));
    }

    @Test
    public void noMatch() {
        artifactFilter.groupId = "com.mahifx";
        artifactFilter.artifactId = "libB";
        Assert.assertFalse(artifactFilter.match(getArtifact()));
    }

    @Test
    public void noMatchWithRegexTokens() {
        artifactFilter.groupId = "com.mahifx";
        artifactFilter.artifactId = "libB-utils";
        Assert.assertFalse(artifactFilter.match(getArtifact()));
    }

    @Test
    public void regexMatch() {
        artifactFilter.groupId = "com.mahifx";
        artifactFilter.artifactId = "lib.*";
        Assert.assertTrue(artifactFilter.match(getArtifact()));
    }

    @Test
    public void regexNoMatch() {
        artifactFilter.groupId = "com.mahifx";
        artifactFilter.artifactId = "foo.+";
        Assert.assertFalse(artifactFilter.match(getArtifact()));
    }

    @Test
    public void emptyArtifactDoesNotMatch() {
        artifactFilter.groupId = "com.mahifx";
        artifactFilter.artifactId = "";
        Assert.assertFalse(artifactFilter.match(getArtifact()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidRegex() {
        artifactFilter.groupId = "com.mahifx";
        artifactFilter.artifactId = "+";
        artifactFilter.match(getArtifact());
    }


    private DefaultArtifact getArtifact() {
        return new DefaultArtifact("com.mahifx", "libA", VersionRange.createFromVersion("1.0.0"), "compile", "jar", null, new DefaultArtifactHandler());
    }
}
