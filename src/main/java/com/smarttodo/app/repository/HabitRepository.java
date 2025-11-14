package com.smarttodo.app.repository;

import com.smarttodo.app.entity.HabitEntity;
import com.smarttodo.app.entity.HabitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HabitRepository extends JpaRepository<HabitEntity, Long> {

    List<HabitEntity> findAllByUser_Id(Long userId);

    List<HabitEntity> findAllByChatId(Long chatId);

    List<HabitEntity> findAllByChatIdAndStatus(Long chatId, HabitStatus status);

    List<HabitEntity> findAllByStatusAndGoalDateLessThanEqual(HabitStatus status, LocalDate date);

    List<HabitEntity> findAllByUser_IdAndStatus(Long userId, HabitStatus status);
}
