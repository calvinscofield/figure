package com.calvin.figure.repository;

import com.calvin.figure.entity.Figure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FigureRepository extends JpaRepository<Figure, Integer> {

}
