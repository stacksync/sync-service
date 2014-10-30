package com.stacksync.syncservice.storage;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpEntity;

import java.io.IOException;
import java.io.InputStream;

public class SwiftResponse
{
	/** HTTP header token that identifies the Storage Token after a successful user login to Cloud Files **/
    public static final String X_AUTH_TOKEN  = "X-Auth-Token";
    
    /** HTTP header token that identifies the Storage URL after a successful user login to Cloud Files **/
    public static final String X_STORAGE_URL    = "X-Storage-Url";
    
    /** HTTP Header used by Cloud Files for the MD5Sum of the object being created in a Container **/
    public static final String E_TAG = "ETag";
    
    public static final String X_CONTAINER_READ = "X-Container-Read";
    
    public static final String X_CONTAINER_WRITE = "X-Container-Write";
    
    public static final String X_COPY_FROM = "X-Copy-From";
    
    /** HTTP header token that is returned on a HEAD request against a Container.  The value of this header is the number of Objects in the Container **/
    public static final String X_CONTAINER_OBJECT_COUNT = "X-Container-Object-Count";
    
    /** HTTP header token that is returned on a HEAD request against a Container.  The value of this header is the number of Objects in the Container **/
    public static final String X_CONTAINER_BYTES_USED = "X-Container-Bytes-Used";
    
    /** HTTP header token that is returned on a HEAD request against an Account.  The value of this header is the number of Containers in the Account **/
    public static final String X_ACCOUNT_CONTAINER_COUNT = "X-Account-Container-Count";
    
    /** HTTP header token that is returned on a HEAD request against an Account.  The value of this header is the total size of the Objects in the Account **/
    public static final String X_ACCOUNT_BYTES_USED = "X-Account-Bytes-Used";
    
    private HttpResponse response = null;
    private HttpEntity entity = null;

    /**
     * @param method The HttpMethod that generated this response
     */
    public SwiftResponse (HttpResponse response)
    {
    	this.response = response;
    	entity = response.getEntity();
    	
    }

    /**
     * Checks to see if the user managed to login with their credentials.
     *
     * @return true is login succeeded false otherwise
     */
    public boolean loginSuccess ()
    {
	int statusCode = getStatusCode();

    	if (statusCode >= 200 && statusCode < 300)
    		return true;

    	return false;     
    }

    /**
     * This method makes no assumptions about the user having been logged in.  It simply looks for the Storage Token header
     * as defined by FilesConstants.X_STORAGE_TOKEN and if this exists it returns its value otherwise the value returned will be null.
     *
     * @return null if the user is not logged into Cloud FS or the Storage token
     */
    public String getAuthToken ()
    {
        return getResponseHeader(X_AUTH_TOKEN).getValue();
    }

    /**
     * This method makes no assumptions about the user having been logged in.  It simply looks for the Storage URL header
     * as defined by FilesConstants.X_STORAGE_URL and if this exists it returns its value otherwise the value returned will be null.
     *
     * @return null if the user is not logged into Cloud FS or the Storage URL
     */
    public String getStorageURL ()
    {
       return getResponseHeader(X_STORAGE_URL).getValue();
    }

    /**
     * Get the content type
     * 
     * @return The content type (e.g., MIME type) of the response
     */
    public String getContentType ()
    {
       return getResponseHeader("Content-Type").getValue();
    }

    /**
     * Get the content length of the response (as reported in the header)
     * 
     * @return the length of the content
     */
    public String getContentLength ()
    {
    	Header hdr = getResponseHeader("Content-Length");
    	if (hdr == null) return "0";
    	return hdr.getValue();
    }

    /**
     * The Etag is the same as the objects MD5SUM
     * 
     * @return The ETAG
     */
    public String getETag ()
    {
    	Header hdr = getResponseHeader(E_TAG);
    	if (hdr == null) return null;
    	return hdr.getValue(); 
    }

    /**
     * The last modified header
     * 
     * @return The last modified header
     */
    public String getLastModified ()
    {
       return getResponseHeader("Last-Modified").getValue(); 
    }

    /**
     * The HTTP headers from the response
     * 
     * @return The headers
     */
    public Header[] getResponseHeaders()
    {
        return response.getAllHeaders();
    }

    /**
     * The HTTP Status line (both the status code and the status message).
     * 
     * @return The status line
     */
    public StatusLine getStatusLine()
    {
        return response.getStatusLine();
    }

    /**
     * Get the HTTP status code
     * 
     * @return The status code
     */
    public int getStatusCode ()
    {
        return response.getStatusLine().getStatusCode();
    }

    /**
     * Get the HTTP status message
     * 
     * @return The message portion of the status line
     */
    public String getStatusMessage ()
    {
        return response.getStatusLine().getReasonPhrase();
    }

    /**
     * Returns the response body as text
     * 
     * @return The response body
     * @throws IOException
     */
    public String getResponseBodyAsString () throws IOException
    {
        return EntityUtils.toString(entity);
    }

    /**
     * Get the response body as a Stream
     * 
     * @return An input stream that will return the response body when read
     * @throws IOException
     */
    public InputStream getResponseBodyAsStream () throws IOException
    {
        return entity.getContent();
    }

    /**
     * Get the body of the response as a byte array
     *
     * @return The body of the response.
     * @throws IOException
     */
    public byte[] getResponseBody () throws IOException
    {
        return EntityUtils.toByteArray(entity);
    }

    /**
     * Returns the specified response header. Note that header-name matching is case insensitive. 
     *
     * @param headerName  - The name of the header to be returned. 
     * @return  The specified response header. If the response contained multiple instances of the header, its values will be combined using the ',' separator as specified by RFC2616.
     */
    public Header getResponseHeader(String headerName)
    {
        return response.getFirstHeader(headerName);
    }

    /**
     * Get the number of objects in the header
     * 
     * @return -1 if the header is not present or the correct value as defined by the header
     */
    public int getContainerObjectCount ()
    {
        Header contCountHeader = getResponseHeader (X_CONTAINER_OBJECT_COUNT);
        if (contCountHeader != null )
          return Integer.parseInt(contCountHeader.getValue());
        return -1;
    }

    /**
     * Get the number of bytes used by the container
     * 
     * @return -1 if the header is not present or the correct value as defined by the header
     */
    public long getContainerBytesUsed ()
    {
        Header contBytesUsedHeader = getResponseHeader (X_CONTAINER_BYTES_USED);
        if (contBytesUsedHeader != null )
          return Long.parseLong(contBytesUsedHeader.getValue());
        return -1;
    }

    /**
     * Get the number of objects in the header
     * 
     * @return -1 if the header is not present or the correct value as defined by the header
     */
    public int getAccountContainerCount ()
    {
        Header contCountHeader = getResponseHeader (X_ACCOUNT_CONTAINER_COUNT);
        if (contCountHeader != null )
          return Integer.parseInt(contCountHeader.getValue());
        return -1;
    }

    /**
     * Get the number of bytes used by the container
     * 
     * @return -1 if the header is not present or the correct value as defined by the header
     */
    public long getAccountBytesUsed ()
    {
        Header accountBytesUsedHeader = getResponseHeader (X_ACCOUNT_BYTES_USED);
        if (accountBytesUsedHeader != null )
          return Long.parseLong(accountBytesUsedHeader.getValue());
        return -1;
    }


    /**
     * Returns the response headers with the given name. Note that header-name matching is case insensitive.
     *
     * @param headerName - the name of the headers to be returned.
     * @return An array of zero or more headers
     */
    public Header[] getResponseHeaders(String headerName)
    {
        return response.getHeaders(headerName);
    }
    
    public String getContentEncoding() {
    	return entity.getContentEncoding().getValue();
    }
}