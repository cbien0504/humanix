package com.hrm.dao;

import com.hrm.model.Position;
import com.hrm.model.PositionForm;

import java.util.List;
import java.util.Optional;

public interface PositionDao {

    List<Position> findAll();

    Optional<Position> findById(long id);

    Optional<Position> findByCode(String code);

    long insert(PositionForm form);

    void update(long id, PositionForm form);

    void delete(long id);
}
