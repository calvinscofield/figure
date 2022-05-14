package com.calvin.figure.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    List<com.calvin.figure.entity.File> add(MultipartFile[] file, com.calvin.figure.entity.File value) throws IOException;

    void delete(Integer id);

    File saveFile(MultipartFile file, com.calvin.figure.entity.File value) throws IOException;

    boolean delFile(String url);
}
