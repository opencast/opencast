package loadtesting;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
	private static SimpleDateFormat sdf = new SimpleDateFormat("dd-HH:mm:ss");
	public static void print(String output){
		System.out.println(sdf.format(new Date()) + "  " + output);
	}
}
