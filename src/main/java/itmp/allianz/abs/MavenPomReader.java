package itmp.allianz.abs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class MavenPomReader {

	MavenXpp3Reader reader;
	Model model;
	List<String> pomProfiles = null;

	MavenPomReader(File pom) throws FileNotFoundException, IOException, XmlPullParserException {
		reader = new MavenXpp3Reader();

		this.model = reader.read(new FileReader(pom));
		this.model.removeModule("../itmp.allianz.abs.release");
		// System.out.println(this.model.getModules());
		// this.model.setPomFile(pom);
		/*
		 * System.out.println(model.getProfiles()); for (Profile profile :
		 * model.getProfiles()) { pomProfiles.add(profile.toString()); }
		 */
		// model.removeModule("bgb.allianz.abs.release");
	}

	List<String> getProfiles() {
		List<String> pomProfiles = null;

		return pomProfiles;

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
