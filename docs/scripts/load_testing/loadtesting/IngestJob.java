package loadtesting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class IngestJob implements Runnable {
	private long startDelay = 0;
	private String id = "";
	public IngestJob(String id, long startDelay) {
		this.id = id;
		this.startDelay = startDelay;
	}

	@Override
	public void run() {
		ThreadCounter.add();
		try {
			Thread.sleep(startDelay * LoadTesting.MILLISECONDS_IN_SECONDS * LoadTesting.SECONDS_IN_MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Logger.print("Starting to run " + id);
		//Execute.execute(System.getenv("windir") +"\\system32\\"+"tree.com /A");
		String workingDirectory = LoadTesting.WORKSPACE + id + "/";
		// Create working directory
		String createWorkingDirectoryCommand = "mkdir " + workingDirectory;
		Execute.launch(createWorkingDirectoryCommand);
		// Copy source media package
		Logger.print("Beginning Copy of " + id);
		String copyCommand = "cp " + LoadTesting.SOURCE_MEDIA_PACKAGE + " " + workingDirectory + id + ".zip";
		Execute.launch(copyCommand);
		// Change id in manifest.xml
		updateManifestID(workingDirectory);
		// Change id in episode.xml
		updateEpisodeID(workingDirectory);
		// Update the new manifest.xml and episode.xml to the zip file.
		Logger.print("Beginning update of zipfile " + id);
		updateZip(workingDirectory);
		// Use curl to ingest media package
		ingestMediaPackage(workingDirectory, workingDirectory + id + ".zip");
		// Delete working directory for id

		Logger.print("Finished running " + id);
		ThreadCounter.subtract();
	}

	private void updateManifestID(String workingDirectory) {
		// Change id in manifest.xml
		String extractManifestCommand = "unzip " + workingDirectory + id + ".zip" + " manifest.xml " + "-d " + workingDirectory;
		Execute.launch(extractManifestCommand);
		changeManifestID(workingDirectory + "manifest.xml");
	}

	private void changeManifestID(String filepath) {
		try {
			Logger.print("Filepath for changing the manifest id is " + filepath);
			DocumentBuilderFactory docFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(filepath);

			// Get the staff element by tag name directly
			Node staff = doc.getElementsByTagName("mediapackage").item(0);

			// update staff attribute
			NamedNodeMap attr = staff.getAttributes();
			Node nodeAttr = attr.getNamedItem("id");
			nodeAttr.setTextContent(id);

			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(filepath));
			transformer.transform(source, result);
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (SAXException sae) {
			sae.printStackTrace();
		}
	}

	private void updateEpisodeID(String workingDirectory) {
		// Change id in manifest.xml
		String extractManifestCommand = "unzip " + workingDirectory + id + ".zip" + " episode.xml " + "-d " + workingDirectory;
		Execute.launch(extractManifestCommand);
		changeEpisodeID(workingDirectory + "episode.xml");
	}

	private void changeEpisodeID(String filepath) {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(filepath);

			// Get the staff element by tag name directly
			Node identifier = doc.getElementsByTagName("dcterms:identifier").item(0);
			identifier.setTextContent(id);

			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(filepath));
			transformer.transform(source, result);
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (SAXException sae) {
			sae.printStackTrace();
		}
	}

	private void updateZip(String workingDirectory) {
		// Inject manifest.xml and episode.xml into the media package
		String updateManifestCommand = "zip -fj "+ workingDirectory + id + ".zip " + workingDirectory + "manifest.xml" + " " + workingDirectory + "episode.xml";
		Execute.launch(updateManifestCommand);
	}

	private void ingestMediaPackage(String workingDirectory, String mediaPackageLocation) {
		Logger.print("Beginning Ingest of " + id);
		String curlCommand;
		curlCommand = "/usr/bin/curl";
		curlCommand += " --digest";
		curlCommand += " -u " + LoadTesting.USER_NAME + ":"
				+ LoadTesting.PASSWORD;
		curlCommand += " --header" + " \"X-Requested-Auth: Digest\"";
		curlCommand += " --form" + " BODY=@" + mediaPackageLocation;
		curlCommand += " --form" + " workflowDefinitionId=full";
		// workflowInstanceId is set to a non integer id so that a new one will
		// be assigned.(this will make the ingest service think it is an
		// Unscheduled capture)
		curlCommand += " --form" + " workflowInstanceId=" + id;
		curlCommand += " http://" + LoadTesting.CORE_ADDRESS
				+ "/ingest/addZippedMediaPackage";
		createBashFile(workingDirectory, curlCommand);
		Execute.launch("sh " + workingDirectory + "curl.sh");
	}

	private void createBashFile(String workingDirectory, String curlCommand) {
		try {
			// Create file
			FileWriter fstream = new FileWriter(workingDirectory + "curl.sh");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("#!/bin/bash\n");
			out.write(curlCommand);
			out.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}

	public String getID(){
		return id;
	}
	@Override
	public String toString(){
		return "IngestJob with id\"" + id + "\" and delay \"" + startDelay + "\" minutes";
	}
}
