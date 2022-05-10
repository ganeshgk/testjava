package itmp.allianz.abs;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import itmp.allianz.abs.repo.RepoOperations;

@Mojo(name = "updatePom", defaultPhase = LifecyclePhase.COMPILE)
public class VersionUpgradeMainMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;

	@Parameter(property = "outputDirectory", defaultValue = "${project.build.outputDirectory}", required = true)
	private File outputDirectory;

	@Parameter(property = "newVersion", required = true)
	private String newVersion;

	@Parameter(property = "nexus.host", defaultValue = "${nexus.host}")
	private String nexusHost;

	@Parameter(property = "nexusVersion", defaultValue = "3")
	private String nexusVersion;

	@Parameter(property = "existingCoreVersion", defaultValue = "${abs.core.version}")
	private String existingCoreVersion;

	private Log log;

	private ArrayList<String> dockerApplications = new ArrayList<String>() {
		{
			add("rss");
		}
	};

	public Log getLog() {

		log = super.getLog();

		return this.log;
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		// TODO Auto-generated method stub
		if (project.getArtifactId().equals(project.getGroupId() + ".core")) {
			// TODO Auto-generated method stub
			Properties mavenProperties = project.getProperties();

			MavenXpp3Reader reader = new MavenXpp3Reader();
			Model model = null;
			try {
				model = reader.read(new FileReader(project.getFile()));
			} catch (IOException | XmlPullParserException e1) {
				throw new MojoExecutionException("Problem reading " + project.getFile());
			}

			Properties mavenProp = project.getProperties();
			Properties pomProp = model.getProperties();

			RepoOperations checkDeliveryReport = new RepoOperations(nexusHost, nexusVersion, existingCoreVersion,
					newVersion);
			HashMap<String, String> changedTargetVersionPair = checkDeliveryReport.changedTargetVersionPair;

			HashMap<String, String> newTargetVersionPair = checkDeliveryReport.newTargetVersionPair;
			System.out.println(changedTargetVersionPair);
			List<String> targetP2List = new ArrayList<>(changedTargetVersionPair.keySet());

			DataCollector collector = new DataCollector();
			HashMap<String, String> p2MappingPlaceholderMap = collector.getP2MappingPlaceholder(targetP2List, mavenProp,
					pomProp);

			HashMap<String, String> targetP2VersionList = new HashMap<>();
			targetP2VersionList.put("abs.core.version", newVersion);
			targetP2VersionList.put("abs.core.airwave.source.version", newTargetVersionPair.get("at.allianz.abs.core.airwave.source"));

			for (Map.Entry<String, String> p2MappingPlaceholderMapEntry : p2MappingPlaceholderMap.entrySet()) {
				for (Map.Entry<String, String> changedTargetVersionPairEntry : changedTargetVersionPair.entrySet()) {
					if (changedTargetVersionPairEntry.getKey().equals(p2MappingPlaceholderMapEntry.getKey())) {
						targetP2VersionList.put(p2MappingPlaceholderMapEntry.getValue(),
								changedTargetVersionPairEntry.getValue());
					}
				}
			}
			getLog().info("Changed target platfrom list");
			getLog().info(targetP2VersionList.toString());

			File corePomFile = project.getFile();
			System.out.println(corePomFile.getAbsolutePath());

			FileUpdater updateConfigFile = new FileUpdater(getLog());

			updateConfigFile.updateCorePomFile(corePomFile, targetP2VersionList);

			ArrayList<File> dockerFiles = new ArrayList<>();
			FileUtils utility = new FileUtils();
			for (String app : dockerApplications) {
				if (utility.FileUtils(app, project.getArtifactId(), log).get("docker").exists()) {
					File dockerFile = utility.FileUtils(app, project.getArtifactId(), log).get("docker");
					dockerFiles.add(dockerFile);
				}
			}
			System.out.println("Docker Files" + dockerFiles);
			updateConfigFile.updateDockerFile(dockerFiles, newTargetVersionPair);

		}
	}

}
