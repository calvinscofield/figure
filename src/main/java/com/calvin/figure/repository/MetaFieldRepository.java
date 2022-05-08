package com.calvin.figure.repository;

import java.util.Optional;

import com.calvin.figure.entity.MetaField;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MetaFieldRepository extends JpaRepository<MetaField, Integer> {

    Optional<MetaField> findByName(String metaFieldName);
}
