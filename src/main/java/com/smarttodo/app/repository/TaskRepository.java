package com.smarttodo.app.repository;

import com.smarttodo.app.entity.TaskEntity;
import com.smarttodo.app.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    List<TaskEntity> findAllByUser_Id(Long userId);

    List<TaskEntity> findAllByChatId(Long chatId);

    List<TaskEntity> findAllByChatIdAndStatus(Long chatId, TaskStatus status);

    List<TaskEntity> findAllByChatIdAndDeadlineBetween(Long chatId, LocalDateTime start, LocalDateTime end);
}
