import java.io.*;
import java.net.*;
import java.util.*;

public class wget
{
  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  public static class _rcm
  {
    private int rc;
    private String message;

    public _rcm(int _rc, String _message) {
      this.rc = _rc;
      this.message = _message;
    }
  }

  public static void getHttpResponseCode(HttpURLConnection huc, _rcm rcm) throws Exception
  {
    try {
      /* set timeout and user agent string */
      huc.setConnectTimeout(30000);
      huc.setReadTimeout(30000);
      huc.setRequestProperty("User-Agent",
        "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0)");
//    huc.setRequestProperty("User-Agent",
//      "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.9) Gecko/20100824 Firefox/3.6.9");
      huc.setRequestProperty("Connection", "close");

      rcm.rc = huc.getResponseCode();
      rcm.message = huc.getURL().toString();  // needed in case of 404
    }
    catch (MalformedURLException mue) {
      rcm.rc = 420;  // Malformed URL
      rcm.message = mue.getMessage();
    }
    catch (UnknownHostException uhe) {
      rcm.rc = 421;  // Unknown host
      rcm.message = uhe.getMessage();
    }
    catch (FileNotFoundException fnfe) {  // never thrown?!
      rcm.rc = 404;  // Not Found
      rcm.message = fnfe.getMessage();
    }
    catch (ConnectException ce) {
      rcm.rc = 408;  // Request timeout
      rcm.message = ce.getMessage();
      if (rcm.message.equals("Connection refused")) {
        rcm.rc = 422;  // Connection refused
      }
    }
    catch (SocketTimeoutException ste) {
      rcm.rc = 408;  // Request timeout
      rcm.message = ste.getMessage();
    }
    catch (IOException ioe) {
      rcm.rc = 423;  // Other I/O Exception
      rcm.message = ioe.getMessage();
    }
  } // getHttpResponseCode

  public static void main(String[] args) throws Exception
  {
    boolean head = false;

    for (String arg: args) {
      if (arg.equals("-h")) {
        head = true;

        /* remove switch from argument list */
        List<String> list = new ArrayList<String>(Arrays.asList(args));
        list.removeAll(Arrays.asList(arg));
        args = list.toArray(EMPTY_STRING_ARRAY);
      }
    }

    if (args.length < 1) {
      System.err.format("Usage: java %s [-h] <URL>\n", wget.class.getName());
      System.err.format("  -h  fetch headers only\n");
      System.exit(1);
    }

    HttpURLConnection huc;
    InputStream is = null;
    DataInputStream dis = null;
    _rcm rcm = new _rcm(-1, "");
    int rcc, ii, b;

    try {
      URL url = new URL(args[0]);  // take only the first argument
      huc = (HttpURLConnection)url.openConnection();

      /* Check for errors and repeat after sleep */
      getHttpResponseCode(huc, rcm); ii = 0;
      while (ii < 5) {
        rcc = rcm.rc / 100;
        if (rcm.rc == 408 || rcm.rc == 503) {
          System.err.println("WARNING: Service unavailable or timeout occurred - repeating...");
          Thread.currentThread().sleep(15000);

          huc = (HttpURLConnection)url.openConnection();

          getHttpResponseCode(huc, rcm); ii = ii + 1;
        }
        else if (rcm.rc < 0) {  // Response is not valid HTTP
          throw new IOException("Response is not valid HTTP");
        }
        else if (rcc == 1) break;  // Informational
        else if (rcc == 2) break;  // Success
        else if (rcc == 3) break;  // Redirection
        else if (rcc == 4) {  // Client Error
          if (rcm.rc == 420)
            throw new MalformedURLException(rcm.message);
          else if (rcm.rc == 421)
            throw new UnknownHostException(rcm.message);
          else if (rcm.rc == 404)
            throw new FileNotFoundException(rcm.message);
          else if (rcm.rc == 422)
            throw new ConnectException(rcm.message);
          else if (rcm.rc == 423)
            throw new IOException(rcm.message);
          else
            throw new IOException("Client Error (" + rcm.rc + ")");
        }
        else if (rcc == 5) {  // Server Error
          throw new IOException("Server Error (" + rcm.rc + ")");
        }
        else {  // Unknown Error
          throw new IOException("Unknown Error (" + rcm.rc + ")");
        }
      } // while

      is = huc.getInputStream();
      if (!head) {
        /* read stream and output it to stdout */
        dis = new DataInputStream(new BufferedInputStream(is));
        while ((b = dis.read()) != -1) {
          System.out.write(b);
        }
      }
    }
    catch (ClassCastException cce) {
      System.err.println("ERROR: Unsupported protocol");
      System.err.println(cce.toString());
      System.exit(102);
    }
    catch (MalformedURLException mue) {
      System.err.println("ERROR: Malformed URL specified");
      System.err.println(mue.toString());
      System.exit(3);
    }
    catch (UnknownHostException uhe) {
      System.err.println("ERROR: Unknown host");
      System.err.println(uhe.toString());
      System.exit(4);
    }
    catch (FileNotFoundException fnfe) {
      System.err.println("ERROR: 404 Not found");
      System.err.println(fnfe.toString());
      System.exit(5);
    }
    catch (ConnectException ce) {
      System.err.println("ERROR: " + ce.getMessage());
      System.err.println(ce.toString());
      System.exit(6);
    }
    catch (SocketTimeoutException ste) {
      System.err.println("ERROR: Socket timed out");
      System.err.println(ste.toString());
      System.exit(7);
    }
    catch (IOException ioe) {
      System.err.println("ERROR: I/O Exception occured");
      System.err.println(ioe.toString());
      System.exit(8);
    }
    catch (InterruptedException ie) {
      System.err.println("ERROR: Interrupted Exception occured");
      System.err.println(ie.toString());
      System.exit(9);
    }
    finally {
      try {
        is.close();
        System.out.close();
      }
      catch (Exception e) {
      }
    }
  } // main
} // class wget
