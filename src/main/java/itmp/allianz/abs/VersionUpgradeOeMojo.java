package itmp.allianz.abs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import itmp.allianz.abs.data.DataCollector;
import itmp.allianz.abs.files.FileUpdater;
import itmp.allianz.abs.files.FileUtils;

@Mojo(name = "updatePomOe", defaultPhase = LifecyclePhase.COMPILE)
public class VersionUpgradeOeMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;

	@Parameter(property = "outputDirectory", defaultValue = "${project.build.outputDirectory}", required = true)
	private File outputDirectory;

	@Parameter(property = "application", required = true)
	private String application;

	@Parameter(property = "newVersion", required = true)
	private String newVersion;

	@Parameter(property = "updateTarget", defaultValue = "true")
	private boolean updateTarget;

	private String verPlaceholder;

	private String applicationTargetFile;

	private Log log;
	
	private ArrayList<String> dockerApplications = new ArrayList<String>() {
		{
			add(application);
		}
	};

	public Log getLog() {

		log = super.getLog();

		return this.log;
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (project.getArtifactId().equals(project.getGroupId() + ".core")) {
			// TODO Auto-generated method stub
			Properties mavenProperties = project.getProperties();

			Path currentRelativePath = Paths.get("");
			String currentPath = currentRelativePath.toAbsolutePath().toString();
			System.out.println(project.getGroupId());
			setApplicationTargetFile(application, project.getGroupId());

			switch (application) {
			case "rap":
				setVerPlaceholder("itmp.rap.version");
				break;
			case "cisl-adapter":
				setVerPlaceholder("itmp.adapter.version");
				break;
			case "rcp":
				setVerPlaceholder("itmp.rcp.version");
				break;
			case "configsuite":
				setVerPlaceholder("itmp.configsuite.version");
				break;
			default:
				break;
			}

			MavenXpp3Reader reader = new MavenXpp3Reader();
			try {
				Model model = reader.read(new FileReader(project.getFile()));
				Properties modelProperties = model.getProperties();

				List<String> targetP2List = new ArrayList<>();
				targetP2List.add(getVerPlaceholder());
				DataCollector collector = new DataCollector();

				// mavenProperties.setProperty(getVerPlaceholder(), newVersion);
				File corePomFile = project.getFile();
				System.out.println(corePomFile.getAbsolutePath());

				FileUpdater updateCorePomFile = new FileUpdater(getLog());
				HashMap<String, String> targetP2VersionList = new HashMap<>();
				targetP2VersionList.put(getVerPlaceholder(), newVersion);

				updateCorePomFile.updateCorePomFile(corePomFile, targetP2VersionList);
				// System.out.println(model.getProperties());
				FileUpdater updateConfigFile = new FileUpdater(getLog());

				ArrayList<File> dockerFiles = new ArrayList<>();
				FileUtils utility = new FileUtils();
				System.out.println("Print varapps" + application);
				//System.out.println("Docker apps" + dockerApplications);
				/*for (String app : dockerApplications) {
					if (utility.FileUtils(app, project.getArtifactId(), log).get("docker").exists()) {
						File dockerFile = utility.FileUtils(app, project.getArtifactId(), log).get("docker");
						dockerFiles.add(dockerFile);
					}
				}*/
			//	for (String app : dockerApplications) {
					if (utility.FileUtils(application, project.getArtifactId(), log).get("docker").exists()) {
						File dockerFile = utility.FileUtils(application, project.getArtifactId(), log).get("docker");
						dockerFiles.add(dockerFile);
					}
			//	}
				System.out.println("Docker Files" + dockerFiles);
				updateConfigFile.updateoeDockerFile(dockerFiles, newVersion, application);
				

			} catch (

			Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println(getApplicationTargetFile());

			// System.out.println(project.getRepositories());

		}
	}

	public String getApplication() {
		return application;
	}

	public String getVerPlaceholder() {
		return verPlaceholder;
	}

	public void setVerPlaceholder(String verPlaceholder) {
		this.verPlaceholder = verPlaceholder;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getApplicationTargetFile() {
		return applicationTargetFile;
	}

	public void setApplicationTargetFile(String application, String groupID) {
		Path currentRelativePath = Paths.get("");
		String currentPath = currentRelativePath.toAbsolutePath().toString();
		String applicationParentDirName = groupID.concat("." + application.replace("-", "."));

		String applicationDirectoryPath;

		if (application.equals("cisl-adapter")) {

			applicationDirectoryPath = currentPath + File.separator + ".." + File.separator + ".." + File.separator
					+ application.replace("-", File.separator) + File.separator;
		} else {
			applicationDirectoryPath = currentPath + File.separator + ".." + File.separator + ".." + File.separator
					+ application + File.separator;
		}
		applicationDirectoryPath = applicationDirectoryPath + applicationParentDirName + File.separator
				+ applicationParentDirName + ".target";
		applicationTargetFile = applicationDirectoryPath;
	}

}
