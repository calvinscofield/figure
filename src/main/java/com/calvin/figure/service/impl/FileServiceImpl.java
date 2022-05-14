package com.calvin.figure.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.transaction.Transactional;

import com.calvin.figure.repository.FileRepository;
import com.calvin.figure.service.FileService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private FileRepository fileRepository;

    @Override
    public File saveFile(MultipartFile file, com.calvin.figure.entity.File value) throws IOException {
        value.setFilename(file.getOriginalFilename());
        value.setContentType(file.getContentType());
        value.setSize(file.getSize());
        File dir = new File("uploads");
        if (!dir.exists())
            dir.mkdir();
        String name = UUID.randomUUID().toString();
        value.setUrl(name);
        File file1 = new File("uploads" + File.separator + name);
        OutputStream os = new FileOutputStream(file1);
        os.write(file.getBytes());
        os.flush();
        os.close();
        return file1;
    }

    @Override
    public boolean delFile(String url) {
        File file1 = new File("uploads" + File.separator + url);
        return file1.delete();
    }

    @Override
    @Transactional
    public List<com.calvin.figure.entity.File> add(MultipartFile[] file, com.calvin.figure.entity.File value)
            throws IOException {
        List<com.calvin.figure.entity.File> values = new ArrayList<>();
        for (int i = 0; i < file.length; i++) {
            com.calvin.figure.entity.File value1 = new com.calvin.figure.entity.File();
            if (value != null) {
                value1.setName(value.getName());
                value1.setRemark(value.getRemark());
            }
            saveFile(file[i], value1);
            values.add(value1);
        }
        return fileRepository.saveAll(values);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        var file = fileRepository.findById(id).get();
        String url = file.getUrl();
        fileRepository.deleteById(id);
        delFile(url);
    }

}
