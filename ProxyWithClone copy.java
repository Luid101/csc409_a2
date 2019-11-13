import java.io.*;
import java.net.*;
import java.util.*;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyWithClone {
  
  static final File WEB_ROOT = new File(".");
	static final String OUT_OF_SERVICE = "outOfService.html";
  static final String STATUS = "./serverMonitoring/monitoringLogs.txt";
  static final int CLONE_PORT = 8088;
  static final int remoteport = 8087;
  static final int localport = 4008;

  public static void main(String[] args) throws IOException {
    try {

      String host = "localhost";
      
      // Print a start-up message
      System.out.println("Starting proxy on port " + localport);
      //creating HashTable for hosts
      System.out.println("Creating the HashTable");
      ArrayList<String> hosts = getHosts("hosts");
      System.out.println(hosts);

      

      // And start running the server
      runServer(hosts); // never returns
    } catch (Exception e) {
      System.err.println(e);
    }
  }
  /**
   function will read from hosts to create a list of hosts
   */
   public static ArrayList<String> getHosts(String hostfile){
        ArrayList<String> hosts = new ArrayList<String>();
        try {
          BufferedReader reader = new BufferedReader(new FileReader(hostfile));
          String line = reader.readLine();
          while(line != null){
            hosts.add(line);
            line = reader.readLine();
          }  
        } catch (Exception e) {
          System.out.println("Error getting hosts");
        }
        
        return hosts;
   }

   public static String[] getArguments(String input) {
      String arguments[] = new String[3];
      arguments[0] = "";
      arguments[1] = "";
      arguments[2] = "";
      String lines[] = input.split("\\r?\\n");
      String line = "";
      if (lines.length != 0) {
          line = lines[0];
      }
      System.out.println("first line: " + line);

      // look for status page link
      Pattern pStatus = Pattern.compile("^GET\\s+/(\\S+)\\s+(\\S+)$");
      Matcher mStatus = pStatus.matcher(line);
      if(mStatus.matches()){
        //System.out.println("Matches Status"); 
        //System.out.println("g1: '" + mStatus.group(1) + "'");

        if ( mStatus.group(1).equals("status") ){
        
          //System.out.println("Group is status");
          //System.out.println("Found status page");
          arguments[0] = "status";
          return arguments;
        
        } 
      }

      Pattern pput = Pattern.compile("^PUT\\s+/\\?short=(\\S+)&long=(\\S+)\\s+(\\S+)$");
      Matcher mput = pput.matcher(line);
      if(mput.matches()){
        arguments[0] = mput.group(1);
        arguments[1] = mput.group(2);
        arguments[2] = "PUT";
      } else {
        System.out.println("GET");
        Pattern pget = Pattern.compile("^(\\S+)\\s+/(\\S+)\\s+(\\S+)$");
        Matcher mget = pget.matcher(line);

        if(mget.matches()){
          arguments[0] = mget.group(2);
          arguments[1] = "";
          arguments[2] = "GET";
        }    
      }
      return arguments;
   }



  private static byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];
		
		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null) 
				fileIn.close();
		}
		
		return fileData;
	}

  /**
   * runs a single-threaded proxy server on
   * the specified local port. It never returns.
   */
  public static void runServer(ArrayList<String> hosts)
      throws IOException {
    // Create a ServerSocket to listen for connections with
    ServerSocket ss = new ServerSocket(localport);

    final byte[] request = new byte[1024];
    byte[] reply = new byte[4096];
    byte[] cloneReply = new byte[4096];

    String host = "";
    String cloneHost = "";

    boolean verbose = true;



    while (true) {
      Socket client = null, server = null, cloneServer = null;
      BufferedReader in = null; PrintWriter out = null; BufferedOutputStream dataOut = null;
      try {
        // Wait for a connection on the local port
        client = ss.accept();

        File file;
        byte[] fileData = new byte[0];
        int fileLength;
        String contentMimeType = "text/html";

        final InputStream streamFromClient = client.getInputStream();
        final OutputStream streamToClient = client.getOutputStream();

        in = new BufferedReader(new InputStreamReader(streamFromClient));
        out = new PrintWriter(streamToClient);
        dataOut = new BufferedOutputStream(streamToClient);

        String[] arguments = new String[3];
        String input = in.readLine();
        arguments = getArguments(input);
        
        int bucket = Math.abs(arguments[0].hashCode()) % hosts.size();
        host = hosts.get(bucket);
        cloneHost = hosts.get((bucket + 1) % hosts.size());

        // check if we are just asking for status
        if (arguments[0] == "status"){
          file = new File(WEB_ROOT, STATUS);
          fileLength = (int) file.length();
          fileData = readFileData(file, fileLength);
			    dataOut.write(fileData, 0, fileLength);
			    dataOut.flush();
          

          client.close();
          continue;
        }

        // Make a connection to the real server.
        // If we cannot connect to the server, send an error to the
        // client, disconnect, and continue waiting for connections.
        try {
          if (verbose) System.out.println("Trying to connect to " + host + ":" + remoteport);
          
          server = new Socket(host, remoteport);
          

          if (verbose) System.out.println("Connected to " + host + ":" + remoteport + "!");
          if (arguments[2].compareTo("PUT") == 0) {
            //This is a PUT request, send url to the clone as well
            if (verbose) System.out.println("This is a PUT request so Trying to connect to clone: " + cloneHost + ":" + CLONE_PORT);
            try{ 

              cloneServer = new Socket(cloneHost, CLONE_PORT);
              
              if (verbose) System.out.println("Connected to " + cloneHost + ":" + CLONE_PORT + "!");
            }catch(IOException e){
                System.out.println("Error connecting to clone: " + cloneHost + ":" + CLONE_PORT);
                // put request
                // main server works, but clone is offline, 
                // so return back unavailable to client
                // TODO: send proper page to client
                 file = new File(WEB_ROOT, OUT_OF_SERVICE);
                 fileLength = (int) file.length();
                 out.println("HTTP/1.1 404 Proxy server cannot connect to " + cloneHost + ":"
                 + CLONE_PORT + ":\n" + e + "\n");
                 out.println("Server: Java HTTP Server/shortURLner : 1.0");
                 out.println("Date: " + new Date());
                 out.println("Content-type: " + contentMimeType);
                 out.println("Content-length: " + fileLength);
                 out.println(); 
                 out.flush();
                 fileData = readFileData(file, fileLength);
                 dataOut.write(fileData, 0, fileLength);
                 dataOut.flush();

                 client.close();

                 continue;

            }
          }
        } catch (IOException e) {
          //TODO: handle failure of the main server
          System.out.println("Exception: " + e.toString());

          
          if (arguments[2].compareTo("PUT") == 0) {
            //TODO: handle write failure of main host
            //TODO: send proper page to client
            //This is a PUT request, main server is down
            file = new File(WEB_ROOT, OUT_OF_SERVICE);
            fileLength = (int) file.length();
            out.println("HTTP/1.1 404 Proxy server cannot connect to " + cloneHost + ":"
            + CLONE_PORT + ":\n" + e + "\n");
            out.println("Server: Java HTTP Server/shortURLner : 1.0");
            out.println("Date: " + new Date());
            out.println("Content-type: " + contentMimeType);
            out.println("Content-length: " + fileLength);
            out.println(); 
            out.flush();
            fileData = readFileData(file, fileLength);
            dataOut.write(fileData, 0, fileLength);
            dataOut.flush();

            client.close();
            
            continue;
            
          } else {
            //TODO: handle read the failure of main host
            //TODO: send proper page to client
            //This is a GET request, try to get the key from a clone
            try {
              System.out.println("Unable to connect to " + host + ". Trying to connect to its clone " + cloneHost);
              
              server = new Socket(cloneHost, CLONE_PORT);

            } catch (IOException eClone) {
              file = new File(WEB_ROOT, OUT_OF_SERVICE);
              fileLength = (int) file.length();
              out.println("Proxy server cannot connect to " + cloneHost + ":"
              + CLONE_PORT + ":\n" + e + "\n");
              out.println("Server: Java HTTP Server/shortURLner : 1.0");
              out.println("Date: " + new Date());
              out.println("Content-type: " + contentMimeType);
              out.println("Content-length: " + fileLength);
              out.println(); 
              out.flush();
              client.close();
              //fileData = readFileData(file, fileLength);
              //dataOut.write(fileData, 0, fileLength);
              //dataOut.flush();
              continue;

            
            }
          }
          
        }

        final BufferedOutputStream streamToServer = new BufferedOutputStream(server.getOutputStream());
        final InputStream streamFromServer = server.getInputStream();
        final String[] arg = arguments;

        Thread threadToServer = new Thread() {
          public void run() {
            try {
              String test = "short=" + arg[0] + "&long=" + arg[1] + "&requestType=" + arg[2] + "\n";
              byte[] req = test.getBytes();
              streamToServer.write(req, 0, test.length());
              streamToServer.flush();
              //Makes sure that the stream from client was fully read
              int bytesRead;
              while ((bytesRead = streamFromClient.read(request)) != -1) {}
            } catch (IOException e) {
              System.out.println("Client closed connection: " + e.toString());
            }

            // the client closed the connection to us, so close our
            // connection to the server.
            try {
              streamToServer.close();
            } catch (IOException e) { }
          }
        };

        // define thread to client and server stuff here
        // make final variables for server thread
        final Socket threadClient = client; 
        final Socket threadServer = server; 

        // start a thread to handle the stuff below
        // TODO: Wait for threadToClone before serviceServer Writes to client
        Thread serverToClient = new Thread() {
          public void run(){
            
            // Read the server's responses
            // and pass them back to the client.
            int bytesRead;
            try {
              while ((bytesRead = streamFromServer.read(reply)) != -1) {
                System.out.println(bytesRead);
                streamToClient.write(reply, 0, bytesRead);
                streamToClient.flush();
              }
            } catch (IOException e) {
              System.out.println("Error response from server: " + e.toString());
            } finally {
              try {
                // The server closed its connection to us, so we close our
                // connection to our client.
                streamToClient.close();

                if (threadServer != null)
                threadServer.close();
                if (threadClient != null)
                threadClient.close();
              } catch (IOException e) {}
            }
          }
        }; 

        if (arg[2].compareTo("PUT") == 0) {
          //Send the url to clone as well
          final BufferedOutputStream streamToCloneServer = new BufferedOutputStream(cloneServer.getOutputStream());
          final InputStream streamFromCloneServer = cloneServer.getInputStream();

    

          Thread threadToCloneServer = new Thread() {
            public void run() {
              
              try {
              
                String test = "short=" + arg[0] + "&long=" + arg[1] + "&requestType=" + arg[2] + "\n";
                byte[] req = test.getBytes();
                streamToCloneServer.write(req, 0, test.length());
                streamToCloneServer.flush();
                //Makes sure that the stream from client was fully read
                //int bytesRead;
                //while ((bytesRead = streamFromClient.read(request)) != -1) {}
              
              } catch (Exception e) {
              
                System.out.println("Error response from client: " + e.toString());
              
              }
              
              // Read the result from clone server
              try{
                System.out.println("Trying to Read from clone server in thread");
                BufferedReader in = new BufferedReader(new InputStreamReader(streamFromCloneServer));
                String cloneLine = in.readLine();
                System.out.println("Clone server says: " + cloneLine);

                String[] cloneLineArray = cloneLine.split(" ");
                
                if ( cloneLineArray[1].compareTo("200") == 0 ){
                  System.out.println("Clone was written to");

                  threadToServer.start();
                  serverToClient.start();

                }else{
                  System.out.println("Error writing to clone");
                  serverToClient.start();
                }

              }
              catch (Exception e){
                System.out.println("Error reading Clone server response:" + e.toString());
              }
              finally{

                // close conenction to clone
                try {
                  streamToCloneServer.close();
                  streamFromCloneServer.close();
                  System.out.println("Close clone socket");
                } catch (IOException e) { }

              }

            }
          };

          threadToCloneServer.start();
        }
        else{
          // start threads to server and 
          // client only if its a get request
          threadToServer.start();
          serverToClient.start();
        }

      } catch (Exception e) {
        System.err.println(e);
      } 

    }



  }
}