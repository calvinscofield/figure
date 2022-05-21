package com.calvin.figure.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.transaction.Transactional;

import com.calvin.figure.entity.User;
import com.calvin.figure.repository.FileRepository;
import com.calvin.figure.repository.UserRepository;
import com.calvin.figure.service.FileService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private UserRepository userRepository;

    @Override
    public File saveFile(MultipartFile file, String filename) throws IOException {
        File dir = new File("uploads");
        if (!dir.exists())
            dir.mkdir();
        File file1 = new File(dir.getAbsolutePath(), filename);
        file.transferTo(file1);
        return file1;
    }

    @Override
    public File saveFile(MultipartFile file) throws IOException {
        return saveFile(file, UUID.randomUUID().toString());
    }

    @Override
    public boolean delFile(String filename) {
        File file1 = new File("uploads" + File.separator + filename);
        return file1.delete();
    }

    @Override
    @Transactional
    public com.calvin.figure.entity.File add(MultipartFile file, com.calvin.figure.entity.File value)
            throws IOException {
        String filename = UUID.randomUUID().toString();
        value.setOriginalFilename(file.getOriginalFilename());
        value.setContentType(file.getContentType());
        value.setSize(file.getSize());
        value.setFilename(filename);
        var value1 = fileRepository.save(value);
        saveFile(file, filename);
        return value1;
    }

    @Override
    public void constraint(Integer id) {
        User user = new User();
        com.calvin.figure.entity.File avatar = new com.calvin.figure.entity.File();
        avatar.setId(id);
        user.setAvatar(avatar);
        if (userRepository.exists(Example.of(user)))
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "有用户头像使用该文件");
    }

}
