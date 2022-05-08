package com.calvin.figure.repository;

import java.util.Optional;

import com.calvin.figure.entity.MetaTable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetaTableRepository extends JpaRepository<MetaTable, Integer> {
    Optional<MetaTable> findByName(String name);
}
