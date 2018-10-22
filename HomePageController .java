/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cloudelements.assignment.controllers;

import com.cloudelemets.assignment.util.FileDownloadProgressListener;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.Drive.Files.Get;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;

/**
 *
 * @author Eshwar chettri
 */
@Controller
public class HomePageController {

    @Value("${google.secret.key.path}")
    private Resource gdSecretKey;

    @Value("${google.oauth.callback.url}")
    private String REDIRECT_URI;

    @Value("${google.credentials.folder.path}")
    private Resource credentialsFolder;

    private static final String APPLICATION_NAME = "Assignment Project";
    private static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();//Httptransport is used by the google api in order to make rest api calls
    private static JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();//JsonFactory  is used to serialize and deserialize the responses
    private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE);//Drive Scope 
    private static final String USER_IDENTIFIER_KEY = "MY_DUMMY_USER";//Used as a identifier for user

    private static final String UPLOAD_FILE_PATH = "C:\\Users\\Eesha chettri\\Downloads\\JashnBanquates\\src\\main\\webapp\\images";
    private static final String DIR_FOR_DOWNLOADS = "D:\\download\\";
    private static final java.io.File UPLOAD_FILE = new java.io.File(UPLOAD_FILE_PATH);

    GoogleAuthorizationCodeFlow flow;//It should be initialized only once when application is started

    @PostConstruct
    public void init() throws IOException {
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(gdSecretKey.getInputStream()));
        flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES).setDataStoreFactory(new FileDataStoreFactory(credentialsFolder.getFile())).build();
    }

    @GetMapping("/")
    public String showHomePage() throws Exception {
        boolean isUserAuthenticated = false;
        Credential credential = flow.loadCredential(USER_IDENTIFIER_KEY);
        if (credential != null) {
            boolean isValidToken = credential.refreshToken();
            if (isValidToken) {
                isUserAuthenticated = true;
            }
        }

        return isUserAuthenticated ? "dashboard.html" : "index.html";

    }

    @GetMapping("/googlesignin")
    public void doGoogleSignIn(HttpServletResponse response) throws IOException {
        GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
        String redirectUrl = url.setRedirectUri(REDIRECT_URI).setAccessType("offline").setApprovalPrompt("force").build();
        response.sendRedirect(redirectUrl);

    }

    @GetMapping("/oauth")
    public String saveTokens(HttpServletRequest request) throws IOException {
        String code = request.getParameter("code");
        if (code != null) {
            saveTokens(code);
            return "dashboard.html";
        }
        return "index.html";
    }

    private void saveTokens(String code) throws IOException {
        GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
        flow.createAndStoreCredential(response, USER_IDENTIFIER_KEY);
    }

    @GetMapping("/downloadfile")

    public ResponseEntity downloadFile() throws Exception {
        Drive service = new Drive(HTTP_TRANSPORT, JSON_FACTORY, flow.loadCredential(USER_IDENTIFIER_KEY));
        flow.loadCredential(USER_IDENTIFIER_KEY).getAccessToken();
        // Print the names and IDs for up to 10 files.
        FileList result = service.files().list()
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name, fileExtension,mimeType)")
                .execute();
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            System.out.println("Files:");
            for (File file : files) {
                System.out.printf("%s (%s)\n", file.getName(), file.getId());
                String fname = file.getMimeType();
                String ex = fname.substring(fname.lastIndexOf(".") + 1);
                try {
                    Files f = service.files();
                    HttpResponse httpResponse = null;
                    if (ex.contains("spreadsheet")) {
                        httpResponse = f
                                .export(file.getId(),
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                .executeMedia();

                    } else if (ex.contains("document")) {
                        httpResponse = f
                                .export(file.getId(),
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                                .executeMedia();
                    } else if (ex.contains("presentation")) {
                        httpResponse = f
                                .export(file.getId(),
                                        "application/vnd.openxmlformats-officedocument.presentationml.presentation")
                                .executeMedia();

                    } else if (ex.contains("pdf")
                            || ex.contains("jpg")
                            || ex.contains("png")) {

                        Get get = f.get(file.getId());
                        httpResponse = get.executeMedia();

                    }
                    if (null != httpResponse) {
                        InputStream instream = httpResponse.getContent();
                        FileOutputStream output = new FileOutputStream(
                                DIR_FOR_DOWNLOADS+file.getName());
                        try {
                            int l;
                            byte[] tmp = new byte[2048];
                            while ((l = instream.read(tmp)) != -1) {
                                output.write(tmp, 0, l);
                            }
                        } finally {
                            output.close();
                            instream.close();
                        }
                    }
                } catch (IOException e) {
                }
            }
        }

        return new ResponseEntity(result.getFiles(), HttpStatus.OK);
    }

    /**
     * Authorizes the installed application to access user's protected data.
     */
    private Credential authorize() throws Exception {
        LocalServerReceiver receier = new LocalServerReceiver.Builder().setPort(8081).build();

        return new AuthorizationCodeInstalledApp(flow, receier).authorize("user");
    }
}
