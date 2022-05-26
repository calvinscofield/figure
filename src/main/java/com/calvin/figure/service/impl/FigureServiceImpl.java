package com.calvin.figure.service.impl;

import java.io.IOException;

import javax.transaction.Transactional;

import com.calvin.figure.entity.Figure;
import com.calvin.figure.entity.File;
import com.calvin.figure.repository.FigureRepository;
import com.calvin.figure.service.FigureService;
import com.calvin.figure.service.FileService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FigureServiceImpl implements FigureService {

    private static final Logger logger = LoggerFactory.getLogger(FigureService.class);

    @Autowired
    private FigureRepository figureRepository;
    @Autowired
    private FileService fileService;

    @Override
    @Transactional
    public Figure add(MultipartFile portrait, Figure value) throws IOException {
        if (portrait != null) {
            File file = new File();
            var file1 = fileService.add(portrait, file);
            value.setPortrait(file1);
        }
        return figureRepository.save(value);
    }

    @Override
    @Transactional
    public Figure edit(MultipartFile portrait, Figure value) throws IOException {
        if (portrait != null) {
            File file = new File();
            var file1 = fileService.add(portrait, file);
            value.setPortrait(file1);
        }
        return figureRepository.save(value);
    }
}