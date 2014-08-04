package loadtesting;

public class ThreadCounter {
	private static int threadCount = 0;
	private static boolean hasChanged = false;
	public synchronized static void add(){
		threadCount++;
		hasChanged = true;
	}

	public synchronized static void subtract(){
		threadCount--;
	}

	public synchronized static boolean allDone(){
		return hasChanged && threadCount <= 0;
	}

	public synchronized static int getCount(){
		return threadCount;
	}
}
