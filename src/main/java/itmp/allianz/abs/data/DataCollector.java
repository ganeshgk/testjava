package itmp.allianz.abs.data;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;

public class DataCollector {

	public HashMap<String, String> getP2Mapping(List<String> targetP2List, Properties mavenProp, Properties pomProp) {

		HashMap<String, String> p2Mapping = new HashMap<>();

		// String placeHolderPrefix = new String();
		String repoIdPlaceHolder;
		String repoUrlPlaceHolder;

		for (Map.Entry<Object, Object> entry : pomProp.entrySet()) {
			// System.out.println(entry.getKey() + " = " + entry.getValue());

			for (String targetP2 : targetP2List) {

				// System.out.println("Prop Value: " + stringName);
				if (targetP2.equals(entry.getValue().toString())) {
					// System.out.println("P2 with placeholders: " + targetP2 + "::" +
					// entry.getKey());
					if (mavenProp.getProperty(entry.getKey().toString().replace("id", "").concat("url")) != null) {
						p2Mapping.put(targetP2,
								mavenProp.getProperty(entry.getKey().toString().replace("id", "").concat("url")));
					}
				}
			}
		}

//		System.out.println(p2Mapping);
		return p2Mapping;

	}

	public HashMap<String, String> getP2MappingPlaceholder(List<String> targetP2List, Properties mavenProp,
			Properties pomProp) {

		HashMap<String, String> p2Mapping = new HashMap<>();

		// String placeHolderPrefix = new String();
		String repoIdPlaceHolder;
		String repoUrlPlaceHolder;

		for (Map.Entry<Object, Object> entry : pomProp.entrySet()) {
			// System.out.println(entry.getKey() + " = " + entry.getValue());

			for (String targetP2 : targetP2List) {

				// System.out.println("Prop Value: " + stringName);
				if (targetP2.equals(entry.getValue().toString())) {
					// System.out.println("P2 with placeholders: " + targetP2 + "::" +
					// entry.getKey());
					p2Mapping.put(targetP2, entry.getKey().toString().replace("id", "version"));
				}
			}
		}

//		System.out.println(p2Mapping);
		return p2Mapping;

	}

}
