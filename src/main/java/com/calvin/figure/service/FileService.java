package com.calvin.figure.service;

import java.io.File;
import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    com.calvin.figure.entity.File add(MultipartFile file, com.calvin.figure.entity.File value) throws IOException;

    File saveFile(MultipartFile file, String value) throws IOException;

    File saveFile(MultipartFile file) throws IOException;

    boolean delFile(String url);

    void constraint(Integer id);
}
