package com.fan.web.service.asyncimport;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.springframework.web.multipart.MultipartFile;

/** A bounded, database-staged file; no client-controlled path is used. */
final class StagedMultipart implements MultipartFile {
 private final byte[] bytes; private final String name;
 StagedMultipart(byte[] bytes,String name){this.bytes=bytes;this.name=name;}
 public String getName(){return "file";} public String getOriginalFilename(){return name;}
 public String getContentType(){return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";}
 public boolean isEmpty(){return bytes.length==0;} public long getSize(){return bytes.length;}
 public byte[] getBytes(){return bytes;} public InputStream getInputStream(){return new ByteArrayInputStream(bytes);}
 public void transferTo(File dest) throws IOException {Files.write(dest.toPath(),bytes);}
}
