package se.kth.castor.jdbl.deptree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.io.FileUtils;

import se.kth.castor.jdbl.util.MavenUtils;

public class OptionalDependencyIgnorer
{
    private static final Logger LOGGER = LogManager.getLogger(OptionalDependencyIgnorer.class.getName());
    private final MavenProject mavenProject;

    public OptionalDependencyIgnorer(MavenProject mavenProject)
    {
        this.mavenProject = mavenProject;
    }

    public Set<Node> getOptionalDependencies()
    {
        List<Dependency> dependencyList = mavenProject.getDependencies();
        Set<Node> optionalDependencies = new HashSet<>();
        for (Dependency dependency : dependencyList) {
            if (dependency.isOptional()) {
                dependency.setScope("test");
                String groupId = dependency.getGroupId();
                String artifactId = dependency.getArtifactId();
                String packaging = "jar";
                String classifier = dependency.getClassifier();
                String version = dependency.getVersion();
                String scope = dependency.getScope();
                String description = dependency.getScope();
                optionalDependencies.add(new Node(groupId, artifactId, packaging, classifier,
                    version, scope, description, false));
            }
        }
        return optionalDependencies;
    }

    public void removeOptionalDependencies(final MavenUtils mavenUtils)
    {
        String dependencyTreeAbsolutePath = mavenProject.getBasedir().getAbsolutePath() + "/dependency-tree.txt";
        mavenUtils.dependencyTree(dependencyTreeAbsolutePath);
        InputType type = InputType.TEXT;
        try (Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(new File(dependencyTreeAbsolutePath)),
            StandardCharsets.UTF_8))) {
            Parser parser = type.newParser();
            Node tree = parser.parse(r);
            for (Node optionalDependency : getOptionalDependencies()) {
                processOptionalDependency(tree, optionalDependency);
            }
        } catch (IOException | RuntimeException | ParseException e) {
            LOGGER.error("Error getting dependency tree.");
        }
    }

    private void processOptionalDependency(final Node tree, final Node optionalDependency)
    {
        try {
            // fist, remove the dependencies of the direct optional dependency
            List<Node> nodeLinkedList = tree.getChildNodes();
            for (Node node : nodeLinkedList) {
                if (equalGAV(optionalDependency, node)) {
                    final String nodeGAV = node.getGroupId() + ":" + node.getArtifactId() + ":" + node.getVersion();
                    final String outputDirectory = mavenProject.getBuild().getOutputDirectory();
                    LOGGER.info("Ignoring optional dependency " + outputDirectory + "/" + nodeGAV);
                    deleteNodes(node);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error removing optional dependency.");
        }
    }

    private boolean equalGAV(final Node optionalDependency, final Node node)
    {
        return node.getGroupId().equals(optionalDependency.getGroupId()) &&
            node.getArtifactId().equals(optionalDependency.getArtifactId()) &&
            node.getVersion().equals(optionalDependency.getVersion());
    }

    private void deleteNodes(Node node) throws IOException
    {
        List<Node> nodes = node.getChildNodes();
        String outputDirectory = mavenProject.getBuild().getOutputDirectory();
        if (!nodes.isEmpty()) {
            for (Node child : nodes) {
                String optionalDependencyJar = child.getArtifactId() + "-" + child.getVersion() + ".jar";
                if (!child.getScope().equals("test")) {
                    FileUtils.forceDelete(new File(outputDirectory + "/" + optionalDependencyJar));
                }
                deleteNodes(child);
            }
        }
        String optionalDependencyJar = node.getArtifactId() +
            "-" +
            node.getVersion() +
            ".jar";
        if (!node.getScope().equals("test")) {
            FileUtils.forceDelete(new File(outputDirectory + "/" + optionalDependencyJar));
        }
    }
}
