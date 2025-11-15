package com.smarttodo.app.bot;

import com.smarttodo.app.dto.WeeklySummaryDto;
import com.smarttodo.app.entity.UserEntity;
import com.smarttodo.app.llm.motivation.MotivationService;
import com.smarttodo.app.repository.UserRepository;
import com.smarttodo.app.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DayMotivationScheduler {
    private final UserRepository userRepository;
    private final MetricsService metricsService;
    private final MotivationService motivationService;
    private final MessageSender messageSender;

    @Scheduled(cron = "0 0 16 * * *")
    public void sendDayMotivation() {
        List<UserEntity> allUsers = userRepository.findAll();

        if (allUsers.isEmpty()) {
            return;
        }

        for (UserEntity user : allUsers) {
            try {
                int streak = metricsService.calculateCurrentStreak(user.getChatId());
                motivationService.generateMotivation(streak)
                        .subscribe(response -> {
                            String text = response.message();
                            messageSender.sendText(user.getChatId(), text);
                        });

                Thread.sleep(50);
            } catch (Exception ignored) {}
        }
    }
}
