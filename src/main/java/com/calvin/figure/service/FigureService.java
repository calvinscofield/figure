package com.calvin.figure.service;

import java.io.IOException;

import com.calvin.figure.entity.Figure;

import org.springframework.web.multipart.MultipartFile;

public interface FigureService {

    Figure add(MultipartFile portrait, Figure value) throws IOException;

    Figure edit(MultipartFile portrait, Figure value) throws IOException;
}
