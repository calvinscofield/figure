package com.calvin.figure.repository;

import java.util.Optional;

import com.calvin.figure.entity.File;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Integer> {

	Optional<File> findByUrl(String url);

}
