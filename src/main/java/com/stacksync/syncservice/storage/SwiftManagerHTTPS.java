package com.stacksync.syncservice.storage;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.gson.Gson;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.exceptions.storage.EndpointNotFoundException;
import com.stacksync.syncservice.exceptions.storage.ObjectNotFoundException;
import com.stacksync.syncservice.exceptions.storage.UnauthorizedException;
import com.stacksync.syncservice.exceptions.storage.UnexpectedStatusCodeException;
import com.stacksync.syncservice.storage.swift.LoginResponseObject;
import com.stacksync.syncservice.storage.swift.ServiceObject;
import com.stacksync.syncservice.util.Config;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.conn.SingleClientConnManager;

public class SwiftManagerHTTPS extends StorageManager {

    private static StorageManager instance = null;
    private String authUrl;
    private String user;
    private String tenant;
    private String password;
    private String storageUrl;
    private String authToken;
    private DateTime expirationDate;

    private SwiftManagerHTTPS() {

        this.authUrl = Config.getSwiftAuthUrl();
        this.user = Config.getSwiftUser();
        this.tenant = Config.getSwiftTenant();
        this.password = Config.getSwiftPassword();
        this.expirationDate = DateTime.now();
    }

    public static synchronized StorageManager getInstance() {
        if (instance == null) {
            instance = new SwiftManagerHTTPS();
        }

        return instance;
    }

    @Override
    public void login() throws EndpointNotFoundException, UnauthorizedException, UnexpectedStatusCodeException,
            IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {

        TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] certificate, String authType) {
                return true;
            }
        };

        SSLSocketFactory sf = new SSLSocketFactory(acceptingTrustStrategy, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("https", 5000, sf));
        ClientConnectionManager ccm = new SingleClientConnManager(registry);

        HttpClient httpClient = new DefaultHttpClient(ccm);

        try {
            HttpPost request = new HttpPost(authUrl);

            String body = String
                    .format("{\"auth\": {\"passwordCredentials\": {\"username\": \"%s\", \"password\": \"%s\"}, \"tenantName\":\"%s\"}}",
                    user, password, tenant);
            StringEntity entity = new StringEntity(body);
            entity.setContentType("application/json");
            request.setEntity(entity);
            HttpResponse response = httpClient.execute(request);

            SwiftResponse swiftResponse = new SwiftResponse(response);

            if (swiftResponse.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                throw new UnauthorizedException("404 User unauthorized");
            }

            if (swiftResponse.getStatusCode() < 200 || swiftResponse.getStatusCode() >= 300) {
                throw new UnexpectedStatusCodeException("Unexpected status code: " + swiftResponse.getStatusCode());
            }

            String responseBody = swiftResponse.getResponseBodyAsString();

            Gson gson = new Gson();
            LoginResponseObject loginResponse = gson.fromJson(responseBody, LoginResponseObject.class);

            this.authToken = loginResponse.getAccess().getToken().getId();

            Boolean endpointFound = false;

            for (ServiceObject service : loginResponse.getAccess().getServiceCatalog()) {

                if (service.getType().equals("object-store")) {
                    this.storageUrl = service.getEndpoints().get(0).getPublicURL();
                    endpointFound = true;
                    break;
                }
            }

            // get the token issue swift date
            DateTimeZone.setDefault(DateTimeZone.UTC);
            DateTimeFormatter dateStringFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
            DateTime issuedAt = dateStringFormat.parseDateTime(loginResponse.getAccess().getToken().getIssuedAt());

            // get the token expiration swift date
            dateStringFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ");
            DateTime expiresAt = dateStringFormat.parseDateTime(loginResponse.getAccess().getToken().getExpires());

            // calculate the period between these two dates and add it to our
            // current time because datetime can differ from Swift and this
            // device
            Period period = new Period(issuedAt, expiresAt);
            expirationDate = DateTime.now().plus(period);

            if (!endpointFound) {
                throw new EndpointNotFoundException();
            }

        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    @Override
    public void createNewWorkspace(Workspace workspace) throws Exception {

        if (!isTokenActive()) {
            login();
        }



        TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] certificate, String authType) {
                return true;
            }
        };

        SSLSocketFactory sf = new SSLSocketFactory(acceptingTrustStrategy, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("https", 5000, sf));
        ClientConnectionManager ccm = new SingleClientConnManager(registry);

        HttpClient httpClient = new DefaultHttpClient(ccm);

        String url = this.storageUrl + "/" + workspace.getSwiftContainer();

        try {

            HttpPut request = new HttpPut(url);
            request.setHeader(SwiftResponse.X_AUTH_TOKEN, authToken);

            HttpResponse response = httpClient.execute(request);

            SwiftResponse swiftResponse = new SwiftResponse(response);

            if (swiftResponse.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                throw new UnauthorizedException("401 User unauthorized");
            }

            if (swiftResponse.getStatusCode() < 200 || swiftResponse.getStatusCode() >= 300) {
                throw new UnexpectedStatusCodeException("Unexpected status code: " + swiftResponse.getStatusCode());
            }

        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    @Override
    public void removeUserToWorkspace(User owner, User user, Workspace workspace) throws Exception {

        if (!isTokenActive()) {
            login();
        }

        String permissions = getWorkspacePermissions(owner, workspace);

        String tenantUser = Config.getSwiftTenant() + ":" + user.getSwiftUser();

        if (permissions.contains("," + tenantUser)) {
            permissions.replace("," + tenantUser, "");
        } else if (permissions.contains(tenantUser)) {
            permissions.replace(tenantUser, "");
        } else {
            return;
        }

        TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] certificate, String authType) {
                return true;
            }
        };

        SSLSocketFactory sf = new SSLSocketFactory(acceptingTrustStrategy, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("https", 5000, sf));
        ClientConnectionManager ccm = new SingleClientConnManager(registry);

        HttpClient httpClient = new DefaultHttpClient(ccm);
        String url = this.storageUrl + "/" + workspace.getSwiftContainer();

        try {

            HttpPut request = new HttpPut(url);
            request.setHeader(SwiftResponse.X_AUTH_TOKEN, authToken);
            request.setHeader(SwiftResponse.X_CONTAINER_READ, permissions);
            request.setHeader(SwiftResponse.X_CONTAINER_WRITE, permissions);

            HttpResponse response = httpClient.execute(request);

            SwiftResponse swiftResponse = new SwiftResponse(response);

            if (swiftResponse.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                throw new UnauthorizedException("404 User unauthorized");
            }

            if (swiftResponse.getStatusCode() < 200 || swiftResponse.getStatusCode() >= 300) {
                throw new UnexpectedStatusCodeException("Unexpected status code: " + swiftResponse.getStatusCode());
            }

        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    @Override
    public void grantUserToWorkspace(User owner, User user, Workspace workspace) throws Exception {

        if (!isTokenActive()) {
            login();
        }

        String permissions = getWorkspacePermissions(owner, workspace);

        String tenantUser = Config.getSwiftTenant() + ":" + user.getSwiftUser();

        if (permissions.contains(tenantUser)) {
            return;
        }

        permissions += "," + tenantUser;

        TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] certificate, String authType) {
                return true;
            }
        };

        SSLSocketFactory sf = new SSLSocketFactory(acceptingTrustStrategy, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("https", 5000, sf));
        ClientConnectionManager ccm = new SingleClientConnManager(registry);

        HttpClient httpClient = new DefaultHttpClient(ccm);
        String url = this.storageUrl + "/" + workspace.getSwiftContainer();

        try {

            HttpPut request = new HttpPut(url);
            request.setHeader(SwiftResponse.X_AUTH_TOKEN, authToken);
            request.setHeader(SwiftResponse.X_CONTAINER_READ, permissions);
            request.setHeader(SwiftResponse.X_CONTAINER_WRITE, permissions);

            HttpResponse response = httpClient.execute(request);

            SwiftResponse swiftResponse = new SwiftResponse(response);

            if (swiftResponse.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                throw new UnauthorizedException("404 User unauthorized");
            }

            if (swiftResponse.getStatusCode() < 200 || swiftResponse.getStatusCode() >= 300) {
                throw new UnexpectedStatusCodeException("Unexpected status code: " + swiftResponse.getStatusCode());
            }

        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    @Override
    public void copyChunk(Workspace sourceWorkspace, Workspace destinationWorkspace, String chunkName) throws Exception {

        if (!isTokenActive()) {
            login();
        }

        chunkName = "chk-" + chunkName;

        TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] certificate, String authType) {
                return true;
            }
        };

        SSLSocketFactory sf = new SSLSocketFactory(acceptingTrustStrategy, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("https", 5000, sf));
        ClientConnectionManager ccm = new SingleClientConnManager(registry);

        HttpClient httpClient = new DefaultHttpClient(ccm);

        String url = this.storageUrl + "/" + destinationWorkspace.getSwiftContainer() + "/"
                + chunkName;

        String copyFrom = "/" + sourceWorkspace.getSwiftContainer() + "/" + chunkName;

        try {

            HttpPut request = new HttpPut(url);
            request.setHeader(SwiftResponse.X_AUTH_TOKEN, authToken);
            request.setHeader(SwiftResponse.X_COPY_FROM, copyFrom);
            //request.setHeader("Content-Length", "0");                        

            HttpResponse response = httpClient.execute(request);

            SwiftResponse swiftResponse = new SwiftResponse(response);

            if (swiftResponse.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                throw new UnauthorizedException("401 User unauthorized");
            }

            if (swiftResponse.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                throw new ObjectNotFoundException("404 Not Found");
            }

            if (swiftResponse.getStatusCode() < 200 || swiftResponse.getStatusCode() >= 300) {
                throw new UnexpectedStatusCodeException("Unexpected status code: " + swiftResponse.getStatusCode());
            }

        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    @Override
    public void deleteWorkspace(Workspace workspace) throws Exception {

        if (!isTokenActive()) {
            login();
        }

        TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] certificate, String authType) {
                return true;
            }
        };

        SSLSocketFactory sf = new SSLSocketFactory(acceptingTrustStrategy, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("https", 5000, sf));
        ClientConnectionManager ccm = new SingleClientConnManager(registry);

        HttpClient httpClient = new DefaultHttpClient(ccm);

        String url = this.storageUrl + "/" + workspace.getSwiftContainer();

        try {

            HttpDelete request = new HttpDelete(url);
            request.setHeader(SwiftResponse.X_AUTH_TOKEN, authToken);

            HttpResponse response = httpClient.execute(request);

            SwiftResponse swiftResponse = new SwiftResponse(response);

            if (swiftResponse.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                throw new UnauthorizedException("401 User unauthorized");
            }

            if (swiftResponse.getStatusCode() < 200 || swiftResponse.getStatusCode() >= 300) {
                throw new UnexpectedStatusCodeException("Unexpected status code: " + swiftResponse.getStatusCode());
            }

        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    private String getWorkspacePermissions(User user, Workspace workspace) throws Exception {

        if (!isTokenActive()) {
            login();
        }

        TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] certificate, String authType) {
                return true;
            }
        };

        SSLSocketFactory sf = new SSLSocketFactory(acceptingTrustStrategy, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("https", 5000, sf));
        ClientConnectionManager ccm = new SingleClientConnManager(registry);

        HttpClient httpClient = new DefaultHttpClient(ccm);

        String url = this.storageUrl + "/" + workspace.getSwiftContainer();

        try {

            HttpHead request = new HttpHead(url);
            request.setHeader(SwiftResponse.X_AUTH_TOKEN, authToken);

            HttpResponse response = httpClient.execute(request);

            SwiftResponse swiftResponse = new SwiftResponse(response);

            if (swiftResponse.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                throw new UnauthorizedException("404 User unauthorized");
            }

            if (swiftResponse.getStatusCode() < 200 || swiftResponse.getStatusCode() >= 300) {
                throw new UnexpectedStatusCodeException("Unexpected status code: " + swiftResponse.getStatusCode());
            }

            // We suppose there are the same permissions for read and write
            Header containerWriteHeader = swiftResponse.getResponseHeader(SwiftResponse.X_CONTAINER_WRITE);

            if (containerWriteHeader == null) {
                return "";
            }

            return containerWriteHeader.getValue();

        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    private boolean isTokenActive() {
        return DateTime.now().isBefore(expirationDate);
    }
}