package loadtesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Execute {
	public static void launch(String command) {
		try {
			String line;
			InputStream stderr = null;
			InputStream stdout = null;

			// launch EXE and grab stdin/stdout and stderr
			Process process = Runtime.getRuntime().exec(command);

			stderr = process.getErrorStream();
			stdout = process.getInputStream();

			// Printout any stdout messages
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(stdout));
			while ((line = bufferedReader.readLine()) != null) {
				Logger.print(line);
			}
			bufferedReader.close();

			// Printout any stderr messages
			bufferedReader = new BufferedReader(new InputStreamReader(stderr));
			while ((line = bufferedReader.readLine()) != null) {
				Logger.print("[ERROR]  " + line);
			}
			bufferedReader.close();
			// Printout the error code for the process
			process.waitFor();
			//System.out.println(process.exitValue());
			Logger.print("Finished Executing:\"" + command
					+ "\" with exit value " + process.exitValue());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
