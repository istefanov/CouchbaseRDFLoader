package load;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.internal.OperationFuture;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;

import com.couchbase.client.CouchbaseClient;
import com.google.gson.Gson;
import common.ConnectionManager;
import common.FileUtils;

public class DataLoader {

	public static long insertCount = 0;
	public static long updateCount = 0;
	public static long insertOrUpdateCount = 0;
	public static long backLinksCount = 0;
	static Node[] nxx = null;
	static Node[] nxx2 = null;
	static PrintStream pr = null;
	static long lines;
	static long lines2;
	static long tt1;
	static long lineOfNotInsertedEntity;
	static long linesOfNotInsertedBackLink;
	static PrintStream pr2 = null;
	static String[] uris;
	static String bucketName;
	static CouchbaseClient client;
	static JSONParser parser = new JSONParser();

	public static void main(String[] args) {
		
		Console c = System.console();
		System.out.println("Enter bucket name:");
		String bucketName = c.readLine(/*"Enter bucket name:"*/);
		bucketName = bucketName.trim();
		
		System.out.println("Enter URIs of nodes separated by spaces, like this http://192.168.214.153:8091/pools");
		String urisSt = c.readLine();
		urisSt = urisSt.trim();
		String[] uris = urisSt.split(" ");
		
		System.out.println("Enter fielpath to datasets ordered by subject.");
		String filesSt = c.readLine();
		filesSt = filesSt.trim();
		String [] files = filesSt.split(" ");
		
		System.out.println("Enter line number from which the loading should start, if nothing it will start form 1-st line.");
		String lineNumberSt = c.readLine();
		long fromLine = 1;
		if(lineNumberSt!=null && !lineNumberSt.equals("")){
			filesSt = lineNumberSt.trim();
			fromLine = Long.parseLong(filesSt);
		}
		
		
		client = ConnectionManager
				.getInstance(bucketName, uris);
		DataLoader.bucketName = bucketName;
		DataLoader.uris = uris;
		try {

			loadRDFNtriples(files[0], client, bucketName, fromLine);

		}catch (Exception e){
			if(pr!=null && pr2 == null){
				
			pr.println(e.getStackTrace());
			String s = nxx[0].toString();
			String p = nxx[1].toString();
			String o = nxx[2].toN3();
			pr.println("EXPLOSION!");
			pr.println("Line\n");
			pr.println(s+" "+" "+p+" "+o);
			
			pr.println("Happened on line "+lines);
			pr.println("This is the line of not inserted entity: "+lineOfNotInsertedEntity);
			long tt = System.currentTimeMillis();
			pr.println("Time so far: "+(tt-tt1)+" ms.");
			pr.println("Already so many inserts or updates: "+insertOrUpdateCount);
			pr.println("Real updates: "+updateCount);
			pr.println("Real inserts: "+ insertCount);
			}
			if(pr2!=null){
				pr2.println(e.getStackTrace());
				String s = nxx2[0].toString();
				String p = nxx2[1].toString();
				String o = nxx2[2].toN3();
				pr2.println("EXPLOSION in backlinks!");
				pr2.println("Line\n");
				pr2.println(s+" "+" "+p+" "+o);
				pr2.println("Happened in line: "+ lines2);
				pr2.println("Line of not inserted entity "+ lineOfNotInsertedEntity);
			}
//			pr.close();
		} finally {
			
			
			if(pr!=null){
				pr.close();
			}
			if(pr2!=null){
				pr2.close();
			}
			client.shutdown(3, TimeUnit.SECONDS);
			System.exit(0);
		}

//		CouchbaseClient client = ConnectionManager
////				.getInstance("dataset_5mCorrect", "http://192.168.214.153:8091/pools");
////		.getInstance("dataset_10m", "http://diufpc302:8091/pools");
//		.getInstance("dataset_light", "http://192.168.214.153:8091/pools");
//		
//		try {
////			loadRDFNtriples("D:/work/uni-assistent/berlin benchmark/dataset_05m_Sorted.nt", client,"dataset_5mCorrect",1l);
//			loadRDFNtriples("D:/work/uni-assistent/berlin benchmark/dataset_05m.nt", client,"dataset_light ",1l);
////			loadRDFNtriples("D:/work/uni-assistent/couchbase/BSBM10M/dataset.nt", client,"dataset_10m",1l);
////			loadRDFNtriplesBackLink("D:/work/uni-assistent/berlin benchmark/dataset_05mAltbzObj.nt", client,1l);
//		} finally {
//			client.shutdown(3, TimeUnit.SECONDS);
//			System.exit(0);
//		}
	}
	
	
	public static void loadRDFNtriples(String datasetFilePath, CouchbaseClient client, String bucketName, long fromLine) {

		FileInputStream is = null;
		try {
			is = new FileInputStream(datasetFilePath);
		} catch (FileNotFoundException e) {
			System.err.println("Eba se 4eteneto na faila.");
			e.printStackTrace();
		}
		File file = new File("./results");
		file.mkdir();
		pr = FileUtils.getPrintStream(
				"./results/Filling Stats for " + bucketName + " ", new Date());
		
		
		
		pr.println("Reading from: "+ datasetFilePath);
		System.out.println("Reading from: "+ datasetFilePath);
		NxParser nxp = new NxParser(is);

		
		tt1 = System.currentTimeMillis();
		lines = 0;
		
		//Initialise with first line
		while(nxp.hasNext() && lines<fromLine){
			nxx = nxp.next();
			lines++;
		}
		String priorSubject = nxx[0].toString();
		
		JSONArray entity = new JSONArray();
		JSONArray pAr = new JSONArray();
		JSONArray oAr = new JSONArray();
		pAr.add(nxx[1].toString());
		String obj = objectify(nxx[2].toN3());
		oAr.add(obj);
		
		System.out.println("Starting");
		
		while (nxp.hasNext()) {
			try{
			nxx = nxp.next();
			lines++;
			}catch (Exception e){
				System.err.println(e.getStackTrace());
				continue;
			}
//			if(lines)
			
//			ii++;
//			if(ii==5000){
//				break;
//			}
			String s = nxx[0].toString();
			String p = nxx[1].toString();
			
			
			String o = objectify(nxx[2].toN3());
			
			if(s.equals(priorSubject)){
				//append to the current Triple in preparation
				pAr.add(p);
				oAr.add(o);
			} else {
				//store the generated Entity and start again with building a new Entity
//				if(insertOrUpdateCount>6039901){
				boolean updated = false;
				while(!updated){
					try{
						entity.add(pAr);
						entity.add(oAr);
						createOrUpdate(client, entity, priorSubject);	
						updated = true;
					}catch(Exception e) {
						updated = false;
						e.printStackTrace();
						
						System.out.println("Will sleep now for 7 min");
						pr.println("Will sleep now for 7 min");
						try {
							Thread.sleep(420000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
						client = ConnectionManager.getInstance(bucketName, uris);
					}
					
				}
//				}
				insertOrUpdateCount++;
				priorSubject = s;
				
				entity = new JSONArray();
				pAr = new JSONArray();
				oAr = new JSONArray();
				pAr.add(p);
				oAr.add(o);
			}
			if(lines%10000000==0){
				long tt = System.currentTimeMillis();
				System.out.println("Already filled "+lines+" lines.");
				System.out.println("Time so far: "+(tt-tt1)+" ms.");
				System.out.println("Already so many inserts or updates: "+insertOrUpdateCount);
				pr.println("Already filled "+lines+" lines.");
				pr.println("Time so far: "+(tt-tt1)+" ms.");
				pr.println("Already so many inserts or updates: "+insertOrUpdateCount);
				pr.println("Real updates: "+updateCount);
				pr.println("Real inserts: "+ insertCount);
			}
			
		}
		//this is for the last line and last entity.
		entity.add(pAr);
		entity.add(oAr);
		createOrUpdate(client, entity, priorSubject);
		insertOrUpdateCount++;

		long tt2 = System.currentTimeMillis();
		System.out.println("Entities filled in: " + (tt2 - tt1) + " ms.");
		pr.println("Entities filled in: " + (tt2 - tt1) + " ms.");
		pr.println("Inserted entities: " + insertCount);
		pr.println("Updated entities: "+updateCount);
		pr.println("Insert or updates: "+insertOrUpdateCount);
//		pr.close();


		// Shutdown and wait a maximum of three seconds to finish up operations
//		client.shutdown(3, TimeUnit.SECONDS);
//		System.exit(0);

	}
	
	private static String objectify(String input){
		int length = input.length();
		if(input.charAt(0) == '<'){
			return input.substring(1,length-1);
		} else {
			int roofIndex = input.indexOf("^^");
			if(roofIndex>-1){
				return input.substring(0, roofIndex+2)+input.substring(roofIndex+3,length-1);
			} else {
				return input;
			}
		}
	}
	
	public static void createOrUpdate(CouchbaseClient client, JSONArray tr,
			String key) {
		Gson gson = new Gson();
		CASValue<Object> status = client.gets(key);

		StoreHandler sh = new StoreHandler(client);

		// if the entity is not yet present in the database we set it
		if (status == null) {
			String jsonDoc = gson.toJson(tr);
			OperationFuture<Boolean> setOp = sh.contSet(key, jsonDoc, 40);
			// client.set(tr.s, 0, jsonDoc);

			// Check to see if our set succeeded
			try {
				if (setOp.get().booleanValue()) {
					insertCount++;
					lineOfNotInsertedEntity = lines;
					// System.out.println("Set Succeeded");
				} else {
					System.err.println("Set failed: "
							+ setOp.getStatus().getMessage());
				}
			} catch (InterruptedException e) {
				System.err.println("InterruptedException while doing set: "
						+ e.getMessage());
			} catch (ExecutionException e) {
				System.err.println("ExecutionException while doing set: "
						+ e.getMessage());
			}
		} else 
		//else we update the entity
		{
			long casValue = status.getCas();
			JSONArray alreadyExistingEntity = null;
			try {
				alreadyExistingEntity = (JSONArray) parser
						.parse((String) status.getValue());
			} catch (ParseException e) {
				e.printStackTrace();
			}
			// System.out.println(gson.toJson(triple));
			JSONArray allP = (JSONArray) alreadyExistingEntity.get(0);
			if (allP == null) {
				allP = new JSONArray();
			}
			allP.addAll((JSONArray) tr.get(0));
			// alreadyExistingEntity. = allP;

			List<String> allO = (JSONArray) alreadyExistingEntity.get(1);
			if (allO == null) {
				allO = new JSONArray();
			}
			allO.addAll((JSONArray) tr.get(1));

			alreadyExistingEntity = new JSONArray();
			alreadyExistingEntity.add(allP);
			alreadyExistingEntity.add(allO);
			CASResponse casr = client.cas(key, casValue,
					gson.toJson(alreadyExistingEntity));

			if (casr.equals(CASResponse.OK)) {
				updateCount++;
				// System.out.println("Value was updated");
			} else if (casr.equals(CASResponse.NOT_FOUND)) {
				System.out.println("Value is not found");
			} else if (casr.equals(CASResponse.EXISTS)) {
				System.out.println("Value exists, but CAS didn't match");
			}
		}
	}
}
