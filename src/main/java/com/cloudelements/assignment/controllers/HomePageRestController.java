/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cloudelements.assignment.controllers;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudelements.assignment.model.UploadAndDownloadFiles;
import com.cloudelemets.assignment.util.CustomProgressListener;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.FileOutputStream;
import java.io.InputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Eesha chettri
 */
@RestController
public class HomePageRestController {

    @Value("${google.secret.key.path}")
    private Resource gdSecretKey;

    @Value("${google.credentials.folder.path}")
    private Resource credentialsFolder;
    private static final String DIR_FOR_DOWNLOADS = "D:\\download\\";

    private static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();//Httptransport is used by the google api in order to make rest api calls
    private static JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();//JsonFactory  is used to serialize and deserialize the responses
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);//Drive Scope 
    private static final String USER_IDENTIFIER_KEY = "MY_DUMMY_USER";//Used as a identifier for user
    GoogleAuthorizationCodeFlow flow;//It should be initialized only once when application is started

    @PostConstruct
    public void init() throws IOException {
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(gdSecretKey.getInputStream()));
        flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES).setDataStoreFactory(new FileDataStoreFactory(credentialsFolder.getFile())).build();
    }

    @PostMapping("/uploadFile")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        Drive drive = new Drive(HTTP_TRANSPORT, JSON_FACTORY, flow.loadCredential(USER_IDENTIFIER_KEY));
        java.io.File filePath = convert(file);
        File fileMetadata = new File();
        fileMetadata.setName(file.getOriginalFilename());
        String s = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);
        if (s.contains("cvs")) {//If it is a CVS file then converting it to spreadsheet in drive 
            fileMetadata.setMimeType("application/vnd.google-apps.spreadsheet");
        }

        FileContent mediaContent = new FileContent(file.getContentType(), filePath);
        Drive.Files.Create insert = drive.files().create(fileMetadata, mediaContent).setFields("id");
        MediaHttpUploader uploader = insert.getMediaHttpUploader();
        uploader.setDirectUploadEnabled(true);
        uploader.setProgressListener(new CustomProgressListener());
        if (insert.execute().getId() != null) {
            return new ResponseEntity("Successfully uploaded - "
                    + file.getOriginalFilename(), new HttpHeaders(), HttpStatus.CREATED);
        } else {
            return new ResponseEntity("File Upload Failed - "
                    + file.getOriginalFilename(), new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }
    }

    

    public static java.io.File convert(MultipartFile file) throws IOException {
        java.io.File convFile = new java.io.File(file.getOriginalFilename());
        convFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }
}
