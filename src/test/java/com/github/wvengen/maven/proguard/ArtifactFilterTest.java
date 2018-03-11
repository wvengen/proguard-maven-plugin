package com.github.wvengen.maven.proguard;


import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.Assert;
import org.junit.Test;

public class ArtifactFilterTest {

    private ArtifactFilter artifactFilter = new ArtifactFilter();

    @Test
    public void noMatch() {
        artifactFilter.groupId = "com.mahifx";
        artifactFilter.artifactId = "libB";
        Assert.assertFalse(artifactFilter.match(getArtifact()));
    }

    @Test
    public void emptyArtifactDoesNotMatch() {
        artifactFilter.groupId = "com.mahifx";
        artifactFilter.artifactId = "";
        Assert.assertFalse(artifactFilter.match(getArtifact()));
    }

    @Test
    public void wildcardMatch_allArtifact() {
        artifactFilter.groupId = "com.mahifx";
        artifactFilter.artifactId = "*";
        Assert.assertTrue(artifactFilter.match(getArtifact()));
    }

    @Test
    public void wildcardMatch_partOfArtifact() {
        artifactFilter.groupId = "com.mahifx";
        artifactFilter.artifactId = "lib*";
        Assert.assertTrue(artifactFilter.match(getArtifact()));
    }

    @Test
    public void wildcardMatch_escapeArtifactDots() {
        artifactFilter.groupId = "com.mahifx";
        artifactFilter.artifactId = "li.*";
        Artifact artifact = getArtifact();
        artifact.setArtifactId("li.b");
        Assert.assertTrue(artifactFilter.match(artifact));
    }

    @Test
    public void wildcardNoMatch_escapeArtifactDots() {
        artifactFilter.groupId = "com.mahifx";
        artifactFilter.artifactId = "li.b";
        Assert.assertFalse(artifactFilter.match(getArtifact("com.mahifx", "liTb")));
    }

    @Test
    public void noMatchWithRegexTokens() {
        artifactFilter.groupId = "com.mahifx";
        artifactFilter.artifactId = "libB-utils";
        Assert.assertFalse(artifactFilter.match(getArtifact()));
    }

    @Test
    public void wildcardMatch_partOfGroupId() {
        artifactFilter.groupId = "com.ma*";
        artifactFilter.artifactId = "libA";
        Assert.assertTrue(artifactFilter.match(getArtifact()));
    }

    @Test
    public void simpleMatch() {
        artifactFilter.groupId = "com.mahifx";
        artifactFilter.artifactId = "libA";
        Assert.assertTrue(artifactFilter.match(getArtifact()));
    }

    @Test
    public void wildcardMatch_subGroup() {
        artifactFilter.groupId = "com.mahifx.*";
        artifactFilter.artifactId = "libA";
        Assert.assertTrue(artifactFilter.match(getArtifact("com.mahifx.subgroup", "libA")));
        Assert.assertTrue(artifactFilter.match(getArtifact("com.mahifx.subgroup.subsubgroup", "libA")));
    }

    private DefaultArtifact getArtifact() {
        return getArtifact("com.mahifx", "libA");
    }
    
    private DefaultArtifact getArtifact(String groupId, String artifactId) {
        return new DefaultArtifact(groupId, artifactId, VersionRange.createFromVersion("1.0.0"), "compile", "jar", null, new DefaultArtifactHandler());
    }
}
