package load;

import com.couchbase.client.CouchbaseClient;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import net.spy.memcached.CASResponse;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.OperationStatus;

/**
 *
   * The StoreHandler exists mainly to abstract the need to store things
   * to the Couchbase Cluster even in environments where we may receive
   * temporary failures.
 *
 * @author ingenthr
 */
public class StoreHandler {

  CouchbaseClient cbc;
  private List<URI> baselist;
  private String bucketname;
  private String password;

  /**
   *
   * Create a new StoreHandler.  This will not be ready until it's initialized
   * with the init() call.
   *
   * @param baselist
   * @param bucketname
   * @param password
   */
  public StoreHandler(List<URI> baselist, String bucketname, String password) {
    this.baselist = baselist; // TODO: maybe copy this?
    this.bucketname = bucketname;
    this.password = password;


  }
  public StoreHandler(CouchbaseClient cli){
	  this.cbc = cli;
  }

  /**
   * Initialize this StoreHandler.
   *
   * This will build the connections for the StoreHandler and prepare it
   * for use.  Initialization is separated from creation to ensure we would
   * not throw exceptions from the constructor.
   *
   *
   * @return StoreHandler
   * @throws IOException
   */
  public StoreHandler init() throws IOException {
    // I prefer to avoid exceptions from constructors, a legacy we're kind
    // of stuck with, so wrapped here
    cbc = new CouchbaseClient(baselist, bucketname, password);
    return this;
  }

  /**
   *
   * Perform a regular, asynchronous set.
   *
   * @param key
   * @param exp
   * @param value
   * @return the OperationFuture<Boolean> that wraps this set operation
   */
  public OperationFuture<Boolean> set(String key, int exp, Object value) {
    return cbc.set(key, exp, cbc);
  }

  /**
   * Continuously try a set with exponential backoff until number of tries or
   * successful.  The exponential backoff will wait a maximum of 1 second, or
   * whatever
   *
   * @param key
   * @param exp
   * @param value
   * @param tries number of tries before giving up
   * @return the OperationFuture<Boolean> that wraps this set operation
   */
  public OperationFuture<Boolean> contSet(String key, int exp, Object value,
          int tries) {
    OperationFuture<Boolean> result = null;
    OperationStatus status;
    int backoffexp = 0;

    try {
      do {
        if (backoffexp > tries) {
          throw new RuntimeException("Could not perform a set after "
                  + tries + " tries.");
        }
        result = cbc.set(key, exp, value);
        status = result.getStatus(); // blocking call, improve if needed
        if (status.isSuccess()) {
          break;
        }
        if (backoffexp > 0) {
          double backoffMillis = Math.pow(2, backoffexp);
          backoffMillis = Math.min(1000, backoffMillis); // 1 sec max
          Thread.sleep((int) backoffMillis);
          System.err.println("Backing off, tries so far: " + backoffexp);
        }
        backoffexp++;

        if (!status.isSuccess()) {
          System.err.println("Failed with status: " + status.getMessage());
        }

      } while (status.getMessage().equals("Temporary failure"));
    } catch (InterruptedException ex) {
      System.err.println("Interrupted while trying to set.  Exception:"
              + ex.getMessage());
    }

    if (result == null) {
      throw new RuntimeException("Could not carry out operation."); // rare
    }

    // note that other failure cases fall through.  status.isSuccess() can be
    // checked for success or failure or the message can be retrieved.
    return result;
  }
  
  
  /**
   * Continuously try a set with exponential backoff until number of tries or
   * successful.  The exponential backoff will wait a maximum of 1 second, or
   * whatever
   *
   * @param key
   * @param exp
   * @param value
   * @param tries number of tries before giving up
   * @return the OperationFuture<Boolean> that wraps this set operation
   */
  public OperationFuture<Boolean> contSet(String key, Object value,
          int tries) {
    OperationFuture<Boolean> result = null;
    OperationStatus status;
    int backoffexp = 0;

    try {
      do {
        if (backoffexp > tries) {
          throw new RuntimeException("Could not perform a set after "
                  + tries + " tries.");
        }
        result = cbc.set(key, 0, value);
        status = result.getStatus(); // blocking call, improve if needed
        if (status.isSuccess()) {
          break;
        }
        if (backoffexp > 0) {
          double backoffMillis = Math.pow(2, backoffexp);
          backoffMillis = Math.min(600000, backoffMillis); // 8 sec max
          Thread.sleep((int) backoffMillis);
          System.err.println("Backing off, tries so far: " + backoffexp);
        }
        backoffexp++;

        if (!status.isSuccess()) {
          System.err.println("Failed with status: " + status.getMessage());
        }

      } while (status.getMessage().equals("Temporary failure"));
    } catch (InterruptedException ex) {
      System.err.println("Interrupted while trying to set.  Exception:"
              + ex.getMessage());
    }

    if (result == null) {
      throw new RuntimeException("Could not carry out operation."); // rare
    }

    // note that other failure cases fall through.  status.isSuccess() can be
    // checked for success or failure or the message can be retrieved.
    return result;
  }
  
  
//  CASResponse casr = client.cas(entity.s, casValue, gson.toJson(tr));
  
  /**
   * Continuously try a set with exponential backoff until number of tries or
   * successful.  The exponential backoff will wait a maximum of 1 second, or
   * whatever
   *
   * @param key
   * @param exp
   * @param value
   * @param tries number of tries before giving up
   * @return the OperationFuture<Boolean> that wraps this set operation
   */
  public OperationFuture<CASResponse> contCas(String key, long casVal, Object value,
          int tries) {
	OperationFuture<CASResponse> result = null;
    OperationStatus status;
    int backoffexp = 0;

    try {
      do {
        if (backoffexp > tries) {
          throw new RuntimeException("Could not perform a set after "
                  + tries + " tries.");
        }
        result = cbc.asyncCAS(key, casVal, value);
        status = result.getStatus(); // blocking call, improve if needed
        if (status.isSuccess()) {
          break;
        }
        if (backoffexp > 0) {
          double backoffMillis = Math.pow(2, backoffexp);
          backoffMillis = Math.min(4000, backoffMillis); // 4 sec max
          Thread.sleep((int) backoffMillis);
          System.err.println("Backing off, tries so far: " + backoffexp);
        }
        backoffexp++;

        if (!status.isSuccess()) {
          System.err.println("Failed with status: " + status.getMessage());
        }

      } while (status.getMessage().equals("Temporary failure"));
    } catch (InterruptedException ex) {
      System.err.println("Interrupted while trying to set.  Exception:"
              + ex.getMessage());
    }

    if (result == null) {
      throw new RuntimeException("Could not carry out operation."); // rare
    }

    // note that other failure cases fall through.  status.isSuccess() can be
    // checked for success or failure or the message can be retrieved.
    return result;
  }
}