// imports for urlshortURLner
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
import java.util.ArrayList;
import java.util.*;

// imports for database
import java.sql.*;


public class MultiURLShortnerWithClone { 
	
	static final File WEB_ROOT = new File(".");
	static final String DEFAULT_FILE = "index.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String METHOD_NOT_SUPPORTED = "not_supported.html";
	static final String REDIRECT_RECORDED = "redirect_recorded.html";
	static final String REDIRECT = "redirect.html";
	static final String NOT_FOUND = "notfound.html";
	static final String DATABASE = "database.txt";
	static final String TEST_DATABASE = "test_database.txt";
	// port to listen connection
	static int PORT;
	
	// verbose mode
	static final boolean verbose = true;

	// host vars
	static String hostName;
	static String path;
	static final String HOST_FILE = "hosts";
	static String cloneName;

	public static void main(String[] args) {
		try {

			// get my host name
			hostName = args[0];
			path = args[1];
			PORT = Integer.parseInt(args[2]);

			ArrayList<String> hosts = getHosts(HOST_FILE);
			cloneName = getHostClone(hosts, hostName);
			System.out.println("My name is: " + hostName + ", and my Clone is: " + cloneName);
			System.out.println("And my purpose is to serve! :)");


			ServerSocket serverConnect = new ServerSocket(PORT);
			System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
			int id = 0;

			// open connection to sqlite server
			// TODO: specify correct url
			System.out.println("connecting to db");
			DataHandler conn = new DataHandler(path);
			System.out.println("My db is in: '" + path + "'");


			// we listen until user halts server execution
			while (true) {
				if (verbose) { System.out.println("Connecton opened. (" + new Date() + ")"); }
				final Socket socket = serverConnect.accept();
				final int fid = id;
				id += 1;
				System.out.println("connection accepted\n");
				Thread t = new Thread() {
          			public void run() {
						  handle(socket, fid, conn);
					};
				};

				t.start();
				
			}
		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}

	public static void handle(Socket connect, int id, DataHandler conn) {
		BufferedReader in = null; PrintWriter out = null; BufferedOutputStream dataOut = null;

		try {
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			out = new PrintWriter(connect.getOutputStream());
			dataOut = new BufferedOutputStream(connect.getOutputStream());
			
			String input = in.readLine();
			
			String shortURL = "";
			String longURL = "";
			String requestType = "";
			String isClone = "";
			
			if(verbose)System.out.println("first line: "+input);

			Pattern ps = Pattern.compile("short=([^&]+)");
			Matcher ms = ps.matcher(input);
			if (ms.find()) {
				shortURL = ms.group(1);
			} 

			Pattern pl = Pattern.compile("long=([^&]+)");
			Matcher ml = pl.matcher(input);
			if (ml.find()) {
				longURL = ml.group(1);
			} 

			Pattern pr = Pattern.compile("requestType=([^&]+)");
			Matcher mr = pr.matcher(input);
			if (mr.find()) {
				requestType = mr.group(1);
			} 
			
			Pattern pc = Pattern.compile("isClone=([^&]+)");
			Matcher mc = pc.matcher(input);
			if (mc.find()) {
				isClone = mc.group(1);
			} 

			System.out.println("shortURL: " + shortURL + ", longURL: " + longURL + ", request type: " + requestType + ", isClone: " + isClone);
			
			File file;
			byte[] fileData = new byte[0];
			int fileLength;
			String contentMimeType = "text/html";
			if (requestType.compareTo("PUT") == 0) {
				save(shortURL, longURL,conn);

				file = new File(WEB_ROOT, REDIRECT_RECORDED);
				fileLength = (int) file.length();
					
				out.println("HTTP/1.1 200 OK");
			} else if (requestType.compareTo("GET") == 0) {
				String longURLResource = findLong(shortURL, conn);
				if (longURLResource != null){
					file = new File(WEB_ROOT, REDIRECT);
					fileLength = (int) file.length();
					
					out.println("HTTP/1.1 307 Temporary Redirect");
					out.println("Location: "+ longURLResource);
				} else {
					file = new File(WEB_ROOT, FILE_NOT_FOUND);
					fileLength = (int) file.length();
					
					out.println("HTTP/1.1 404 File Not Found");
				}		
			} else {
				file = new File(WEB_ROOT, DEFAULT_FILE);
				fileLength = (int) file.length();
				
				out.println("HTTP/1.1 200 OK");
			}
			out.println("Server: Java HTTP Server/shortURLner : 1.0");
			out.println("Date: " + new Date());
			out.println("Content-type: " + contentMimeType);
			out.println("Content-length: " + fileLength);
			out.println(); 
			out.flush();
			fileData = readFileData(file, fileLength);
			dataOut.write(fileData, 0, fileLength);
			dataOut.flush();
		} catch (Exception e) {
			System.err.println("Server error: " + e.toString());
		} finally {
			try {
				in.close();
				out.close();
				connect.close(); // we close socket connection
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			} 
			
			if (verbose) {
				System.out.println("Connection closed.\n");
			}
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

	/**
		this function will return the clone of a host, 
		given the list of hosts in order and the name of the current host.
	*/
	public static String getHostClone(ArrayList<String> hostList, String host){

		int cloneLocation = (hostList.indexOf(host) + 1) % hostList.size();
		return hostList.get(cloneLocation);

	}

	private static String findLong(String shortURL, DataHandler conn){
		String longURL = null;

		longURL = conn.getLongResource(shortURL);

		return longURL;
	}

	private static void save(String shortURL,String longURL, DataHandler conn){
		conn.saveShortLong(shortURL, longURL);
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
}


/**
	Database server
 */
class DataHandler {

    private Connection conn = null;

     /**
     * Connect to a sample sqlite database
     */
    public DataHandler(String path) {
        try {
            // db parameters
            //String url = "jdbc:sqlite:C:/sqlite/db/chinook.db";
            String url = "jdbc:sqlite:" + path;

            // create a connection to the database
            this.conn = DriverManager.getConnection(url);
			this.conn.setAutoCommit(true);
			
            
            System.out.println("Connection to SQLite has been established.");
            
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

	/**
     Will query a long link given a short link 
     and handle any exceptions if needed
     * @param short the short link that will be redirected to a long link
     */
    public String getLongResource(String shortLink){
        String sql = "SELECT longResource from linkTable WHERE shortResource = ?";

        String result = null;
        try(
			PreparedStatement pstmt = conn.prepareStatement(sql);
        ){
			pstmt.setString(1, shortLink);
			ResultSet rs = pstmt.executeQuery();
			
			while(rs.next()){
				result = rs.getString("longResource");
			}

        } catch (SQLException e){
			System.out.println("Error");
            System.out.println(e.getMessage());
        }
        return result;    
    }


    /**
		Save a short, long pair to database
    */
    synchronized public void saveShortLong(String shortLink, String longLink){
        //DONE fixed error where inserting existing short breaks constraint
		String sql = "INSERT OR REPLACE INTO linkTable(shortResource, longResource) VALUES(?,?)";

        try (
			
        	PreparedStatement pstmt = conn.prepareStatement(sql);
		){
			pstmt.setString(1, shortLink);
			pstmt.setString(2, longLink);
			pstmt.executeUpdate();
			//conn.commit();
		} catch (SQLException e){
			System.out.println(e.getMessage());
		}
    }

	/**
		Close the connection in this object 
	 */
	public void close(){
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
     }
}