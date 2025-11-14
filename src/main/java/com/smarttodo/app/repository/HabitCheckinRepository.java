package com.smarttodo.app.repository;

import com.smarttodo.app.entity.HabitCheckinEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HabitCheckinRepository extends JpaRepository<HabitCheckinEntity, Long> {

    List<HabitCheckinEntity> findAllByHabit_IdAndDayBetween(Long habitId, LocalDate startDate, LocalDate endDate);

    List<HabitCheckinEntity> findAllByHabit_IdOrderByDayAsc(Long habitId);

    boolean existsByHabit_IdAndDay(Long habitId, LocalDate day);
}
