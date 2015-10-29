// implicit script variable: File localRepositoryPath
final String groupId = "com.example"
final String artifactId = "simple"
final String version = "1"

final File alteredJarFile = new File((File) localRepositoryPath,
        String.format('%1$s/%2$s/%3$s/%2$s-%3$s-small.jar', groupId.replaceAll("\\.", "/"), artifactId, version))
if (! alteredJarFile.exists()) {
    final String msg = "Expected to find proguard-ed file at \"" + alteredJarFile + "\""
    throw new AssertionError(msg)
}