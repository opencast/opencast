package loadtesting;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.LinkedList;

public class IngestionGroup {
	LinkedList<IngestJob> ingestJobs = new LinkedList<IngestJob>();
	
	public IngestionGroup(long numberOfIngests, long delayToIngest){
		IngestJob newJob;
		SecureRandom random = new SecureRandom();
		Thread thread;
		for(long i = 0; i < numberOfIngests; i++){
			newJob = new IngestJob(new BigInteger(130, random).toString(32), delayToIngest);
			Logger.print("Creating job " + newJob);
			ingestJobs.add(newJob);	
			thread = new Thread(newJob);
			thread.start();
		}
	}

	public Collection<? extends IngestJob> getJobs() {
		return ingestJobs;
	}
}
