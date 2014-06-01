package loadtesting;

import java.util.LinkedList;

public class LoadTesting {
	// Server ip
	public static String CORE_ADDRESS = "mhadmin:8080";
	// REST Endpoint Username
	public static String USER_NAME = "matterhorn_system_account";
	// REST Endpoint Password
	public static String PASSWORD = "CHANGE_ME";
	// The main directory to run the tests from
	public static String BASE_DIR = "/processor/loadtest/";
	// The templated source media package to use, should be 1.0 or 1.1
	public static String SOURCE_MEDIA_PACKAGE = BASE_DIR + "source/2011-02-11.zip";
	// Where to dump the temporary captures
	public static String WORKSPACE = BASE_DIR + "workspace/";
	// Where to place the unzipped source files.
	public static String UNZIPPED_SOURCE = WORKSPACE + "source/";
	// How often the timer for job checking should go
	public static int JOB_CHECK_TIMER = 5;


	// The distribution of the number of packages to ingest
	//public long[] packageDistribution = {1};
	//public long[] packageDistributionTiming = {0};
	public int[] packageDistribution = {1,1};//,4,4,4,4,4};
	// public int[] packageDistribution = {2,4,8,8,4,2};
	// the amount of time in between each set of ingests in minutes
	public int[] packageDistributionTiming = {0,60};//60,60,60,60,60,60};

	public static int MILLISECONDS_IN_SECONDS = 1000;
	public static int SECONDS_IN_MINUTES = 60;

	public static void main(String args[]){
		Logger.print("Starting Load Testing");
		Logger.print("Create Workspace");
		String createWorkspaceCommand = "mkdir " + WORKSPACE;
		Execute.launch(createWorkspaceCommand);
		LoadTesting loadtesting = new LoadTesting();
		if(loadtesting.packageDistribution.length != loadtesting.packageDistributionTiming.length){
			System.out.println("The length of the distribution must be 1 greater than the number of package distribution timings. ");
			return;
		}
		long delay = 0;
		LinkedList<IngestionGroup> ingestGroups = new LinkedList<IngestionGroup>();
		for(int i = 0; i < loadtesting.packageDistribution.length; i++){
			delay += loadtesting.packageDistributionTiming[i];
			IngestionGroup ingestionGroup = new IngestionGroup(loadtesting.packageDistribution[i], delay);
			ingestGroups.add(ingestionGroup);
		}

		LinkedList<IngestJob> ingestJobs = new LinkedList<IngestJob>();

		for(IngestionGroup ingestionGroup : ingestGroups){
			ingestJobs.addAll(ingestionGroup.getJobs());
		}

		createJobChecker(ingestJobs);

		while(!ThreadCounter.allDone()){
			try {
				Thread.sleep(15 * MILLISECONDS_IN_SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Logger.print("There are still " + ThreadCounter.getCount() + " threads that are executing.");
		}

		Logger.print("Load Testing script is finished now just to wait for processing.");
	}

	private static void createJobChecker(LinkedList<IngestJob> ingestJobs) {
		JobChecker jobChecker = new JobChecker(ingestJobs);
		Thread thread = new Thread(jobChecker);
		thread.start();
	}
}
