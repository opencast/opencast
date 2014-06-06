package loadtesting;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.LinkedList;

public class JobChecker implements Runnable {
	LinkedList<IngestJob> ingestJobs = new LinkedList<IngestJob>();

	public JobChecker(LinkedList<IngestJob> ingestJobs){
		this.ingestJobs = ingestJobs;
	}

	private void checkJob(String id) {
		String curlCommand = "/usr/bin/curl";
		curlCommand += " --get";
		curlCommand += " --digest";
		curlCommand += " -u " + LoadTesting.USER_NAME + ":"
				+ LoadTesting.PASSWORD;
		curlCommand += " --header" + " \"X-Requested-Auth: Digest\"";
		curlCommand += " http://" + LoadTesting.CORE_ADDRESS
				+ "/workflow/instance/" + id + ".xml";

		try {
			// Create file
			FileWriter fstream = new FileWriter(LoadTesting.WORKSPACE + "curl.sh");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("#!/bin/bash\n");
			out.write(curlCommand);
			out.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		//Execute.launch("sh " + LoadTesting.WORKSPACE + "curl.sh");
	}

	@Override
	public void run() {
		while(!ThreadCounter.allDone()){
			try {
				Thread.sleep(LoadTesting.JOB_CHECK_TIMER * LoadTesting.MILLISECONDS_IN_SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			for(IngestJob ingestJob : ingestJobs){
				checkJob(ingestJob.getID());
			}

		}
	}
}
