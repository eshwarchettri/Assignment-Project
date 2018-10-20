/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cloudelements.assignment.controllers;

import com.cloudelements.assignment.model.UploadAndDownloadFiles;
import com.google.api.services.drive.Drive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Eesha chettri
 */
@RestController
public class HomePageRestController {
@PostMapping("/uploadFile")
public ResponseEntity<List<UploadAndDownloadFiles>> uploadFile(){

    
return new ResponseEntity(HttpStatus.CREATED);
}
}
