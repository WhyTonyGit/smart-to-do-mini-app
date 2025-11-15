package com.smarttodo.app.service;

import com.smarttodo.app.dto.WeeklySummaryDto;
import com.smarttodo.app.entity.UserEntity;
import com.smarttodo.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WeeklySummaryScheduler {
    private final UserRepository userRepository;
    private final MetricsService metricsService;
    private final MetricsManager metricsManager;

    @Scheduled(cron = "0 0 20 ? * SUN")
    public void sendWeeklySummaries() {
        List<UserEntity> allUsers = userRepository.findAll();

        if (allUsers.isEmpty()) {
            return;
        }

        for (UserEntity user : allUsers) {
            try {
                WeeklySummaryDto weeklySummary = metricsService.getWeeklySummary(user.getChatId());
                metricsManager.sendWeeklySummary(user.getChatId(), weeklySummary);

                // Небольшая задержка чтобы не перегружать систему
                Thread.sleep(50);
            } catch (Exception ignored) {}
        }
    }
}