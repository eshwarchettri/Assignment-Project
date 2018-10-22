/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cloudelemets.assignment.util;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import java.io.IOException;
import java.text.NumberFormat;

/**
 *
 * @author netelixir
 */
public class FileUploadProgressListener implements MediaHttpUploaderProgressListener {

  @Override
  public void progressChanged(MediaHttpUploader uploader) throws IOException {
    switch (uploader.getUploadState()) {
      case INITIATION_STARTED:
        View.uploadStatus("Upload Initiation has started.");
        break;
      case INITIATION_COMPLETE:
        View.uploadStatus("Upload Initiation is Complete.");
        break;
      case MEDIA_IN_PROGRESS:
        View.uploadStatus("Upload is In Progress: "
            + NumberFormat.getPercentInstance().format(uploader.getProgress()));
        break;
      case MEDIA_COMPLETE:
        View.uploadStatus("Upload is Complete!");
        break;
    }
  }
}
