package se.kth.jdbl.util;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.InvocationRequest;
import se.kth.jdbl.App;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ArtifactResolver {

    //--------------------------/
    //------ CONSTRUCTORS ------/
    //--------------------------/

    private ArtifactResolver() {
    }

    //--------------------------/
    //----- PUBLIC METHODS -----/
    //--------------------------/

    /**
     * This method downloads a pom file from the Maven Central repository.
     *
     * @param artifactDir Directory to put the pom file.
     * @param groupId     The artifact groupId.
     * @param artifactId  The artifact artifactId.
     * @param version     The artifact version.
     * @throws IOException
     */
    public static void downloadPom(String artifactDir, String groupId, String artifactId, String version) throws IOException {
        FileUtils.copyURLToFile(
                new URL("http://central.maven.org/maven2/" +
                        groupId.replace('.', '/') + "/" +
                        artifactId + "/" +
                        version + "/" +
                        artifactId + "-" + version + ".pom"),
                new File(artifactDir + "pom.xml"));
    }

    /**
     * This method resolve an artifact and all its dependencies from Maven Central.
     *
     * @param pomPath             Path to the artifact's pom file.
     * @param coordinates         Artifact's coordinates.
     * @param outputDirectoryPath The directory to put the artifact.
     */
    public void copyArtifact(String pomPath, String coordinates, String outputDirectoryPath, Properties properties) {
        File pom = new File(pomPath);
        List<String> goals = Arrays.asList("dependency:copy", "-DoutputDirectory=" + outputDirectoryPath, "-Dartifact=" + coordinates);
        File log = new File(pom.getParentFile(), "build.log");
        executeRequest(properties, pom, goals, log);
    }

    //--------------------------------/
    //------ PRIVATE METHOD/S -------/
    //------------------------------/

    private void executeRequest(Properties properties, File pom, List<String> goals, File log) {
//        InvocationRequest request = App.getBuildTool().createBasicInvocationRequest(pom, properties, goals, log);
//        request.setLocalRepositoryDirectory(App.getLocalRepo());
//        request.setPomFile(pom);
//        App.getBuildTool().executeMaven(request);
    }
}
