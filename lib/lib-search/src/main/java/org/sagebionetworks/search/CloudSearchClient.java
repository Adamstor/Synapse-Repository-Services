package org.sagebionetworks.search;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.beans.factory.annotation.Autowired;
import javax.annotation.PostConstruct;

/**
 * CloudSearch does not yet provide a Java SDK. This is the bare minimum needed
 * for Search Updates. If we find more uses for this, we'll move it to something
 * under platform/trunk/lib.
 * 
 * @author deflaux
 * 
 */
public class CloudSearchClient {

	private static final Map<String, String> SEND_DOCUMENTS_REQUEST_HEADERS;
	static {
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.put("Content-Type", "application/json;charset=UTF-8");
		SEND_DOCUMENTS_REQUEST_HEADERS = Collections.unmodifiableMap(requestHeaders);
	}

	@Autowired
	CloudSearchHttpClientProvider httpClientProvider;
	private final long MAX_BACKOFF_MS = 6400L;
	
	private HttpClient httpClient;
	private String searchServiceEndpoint;
	private String documentServiceEndpoint;

	public CloudSearchClient(String searchServiceEndpoint, String documentServiceEndpoint) {
		this.searchServiceEndpoint = searchServiceEndpoint;
		this.documentServiceEndpoint = documentServiceEndpoint;
	}
	
	@PostConstruct
	private void init() {
		this.httpClient = httpClientProvider.getHttpClient();
	}

	// For unit test
	public CloudSearchClient(CloudSearchHttpClientProvider httpClientProvider, String searchServiceEndpoint, String documentServiceEndpoint) {
		this.httpClient = httpClientProvider.getHttpClient();
		this.searchServiceEndpoint = searchServiceEndpoint;
		this.documentServiceEndpoint = documentServiceEndpoint;
	}

	public void sendDocuments(String documents) throws ClientProtocolException,
			IOException, HttpClientHelperException {
		HttpClientHelper.postContent(httpClient, documentServiceEndpoint,
				documents, SEND_DOCUMENTS_REQUEST_HEADERS);
	}

	public void sendDocuments(InputStream stream, long length) throws ClientProtocolException, IOException, HttpClientHelperException {
		HttpClientHelper.postStream(httpClient, documentServiceEndpoint,
				stream, length, SEND_DOCUMENTS_REQUEST_HEADERS);
	}

	public String performSearch(String searchQuery) throws ClientProtocolException, IOException, HttpClientHelperException {
		String url = searchServiceEndpoint + "?" + searchQuery;
		String s = null;
		long backoffMs = 100L;
		HttpClientHelperException e = null;
		do {
			try {
				s = HttpClientHelper.getContent(httpClient, url);
			} catch (HttpClientHelperException e1) {
				e = e1;
				if (e1.getHttpStatus() == 507) {
					try {
						Thread.sleep(backoffMs);
					} catch (InterruptedException e2) {
						// Continue
					}
				} else {
					// rethrow
					throw(e1);
				}
			}
			backoffMs *= 2;
		} while ((s == null) && (backoffMs < MAX_BACKOFF_MS));
		// If we're past the max backoff, throw the last 507 we got
		if (backoffMs >= MAX_BACKOFF_MS) {
			throw(e);
		}
		return s;
	}
	
}
