package se.kth.castor.jdbl.app.coverage;

import java.io.File;

import org.apache.maven.project.MavenProject;

import se.kth.castor.jdbl.app.debloat.DebloatTypeEnum;

public abstract class AbstractCoverage
{
    protected MavenProject mavenProject;
    protected File mavenHome;
    protected DebloatTypeEnum debloatTypeEnum;
    protected String entryClass;
    protected String entryMethod;
    protected String entryParameters;

    public AbstractCoverage(MavenProject mavenProject, File mavenHome, DebloatTypeEnum debloatTypeEnum,
        String entryClass, String entryMethod, String entryParameters)
    {
        this.mavenProject = mavenProject;
        this.mavenHome = mavenHome;
        this.debloatTypeEnum = debloatTypeEnum;
        this.entryClass = entryClass;
        this.entryMethod = entryMethod;
        this.entryParameters = entryParameters;
    }

    public AbstractCoverage(MavenProject mavenProject, File mavenHome, DebloatTypeEnum debloatTypeEnum)
    {
        this.mavenProject = mavenProject;
        this.mavenHome = mavenHome;
        this.debloatTypeEnum = debloatTypeEnum;
    }
}
