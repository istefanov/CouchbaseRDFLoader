package common;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import com.couchbase.client.CouchbaseClient;

public class ConnectionManager {

	private static CouchbaseClient client;
	public static final String DB_URL = "http://192.168.214.153:8091/pools"; //192.168.214.153  diufpc301 diuflx01
	public static final String DB_BUCKET_NAME = "dataset_05m"; // dataset_05m
																// "bucket01"
	public static List<URI> URIS;

	static {
		URIS = new LinkedList<URI>();
		URIS.add(URI.create(DB_URL));
	}

	private ConnectionManager() {

	}

	public static CouchbaseClient getInstance() {

		if (client == null) {
			try {
				// Use the "default" bucket with no password
				client = new CouchbaseClient(URIS, DB_BUCKET_NAME, "");
				// client = new CouchbaseClient(uris, "default", "");
			} catch (IOException e) {
				System.err.println("IOException connecting to Couchbase: "
						+ e.getMessage());
				System.exit(1);
			}
		}
		return client;
	}

	public static CouchbaseClient getInstance(String bucketName, String ... uris) {
		List<URI> connections = new LinkedList<URI>();
		for(String uri:uris){
			connections.add(URI.create(uri));
		}
		
		
		if (client == null) {
			try {
				// Use the "default" bucket with no password
				client = new CouchbaseClient(connections, bucketName, "");
				// client = new CouchbaseClient(uris, "default", "");
			} catch (IOException e) {
				System.err.println("IOException connecting to Couchbase: "
						+ e.getMessage());
				System.exit(1);
			}
		}
		return client;
	}
	
	public static CouchbaseClient getInstance(String bucketName) {

		if (client == null) {
			try {
				// Use the "default" bucket with no password
				client = new CouchbaseClient(URIS, bucketName, "");
				// client = new CouchbaseClient(uris, "default", "");
			} catch (IOException e) {
				System.err.println("IOException connecting to Couchbase: "
						+ e.getMessage());
				System.exit(1);
			}
		}
		return client;
	}
	
	public static void clientShutDown() {
		if(client!=null){
			client.shutdown();
		}
	}
}
