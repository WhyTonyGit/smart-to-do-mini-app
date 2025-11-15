package com.smarttodo.app.bot;

import com.smarttodo.app.client.MaxApi;
import com.smarttodo.app.dto.HabitCheckinDto;
import com.smarttodo.app.dto.HabitDto;
import com.smarttodo.app.entity.*;
import com.smarttodo.app.repository.LastActionRedisRepo;
import com.smarttodo.app.repository.PendingHabitRedisRepo;
import com.smarttodo.app.service.HabitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HabitManager {

    private final MaxApi maxApi;

    private final HabitService habitService;
    private final PendingHabitRedisRepo habitRedisRepo;
    private final LastActionRedisRepo lastActionRepo;

    private final MessageSender messageSender;

    public void createHabit(Update u) {
        messageSender.sendHabitCreateKeyboard(u.chatId());
    }

    public void confirmHabitCreating(Update u) {
        Optional<HabitCheckinDto> opt = habitRedisRepo.get(u.chatId());
        if (opt.isEmpty()) {
            messageSender.sendText(u.chatId(), "Создание привычки не завершено, заполните все необходимые поля.");
            return;
        }

        HabitCheckinDto habit = opt.get();
        if (habit.title() == null || habit.title().isBlank() || habit.interval() == null || habit.goalDate() == null) {
            messageSender.sendText(u.chatId(), "Создание привычки не завершено, заполните все необходимые поля.");
            return;
        }

        if (habitService.createHabit(u.chatId(), new HabitDto(
                habit.id(),
                habit.title(),
                habit.description(),
                habit.status(),
                habit.interval(),
                habit.priority(),
                habit.goalDate()
        )) != null) {
            habitRedisRepo.delete(u.chatId());
            messageSender.sendText(u.chatId(), "Привычка создана.");
        }
    }

    public void pickHabit(Update u) {
        Payload payload = Payload.from(u.getPayload());
        HabitCheckinDto habit = habitService.getHabitCheckinDtoById(payload.extractId(u.getPayload()));

        log.info("picking habit payload {}", u.getPayload());

        switch (payload) {
            case HABITS_ID -> {
                messageSender.sendHabit(u.chatId(), habit);
            }
            case HABITS_SET_STATUS_ARCHIVED -> {
                habitService.markHabitAsArchived(habit.id());
                messageSender.sendHabit(u.chatId(), habitService.getHabitCheckinDtoById(payload.extractId(u.getPayload())));
            }
            case HABITS_SET_STATUS_COMPLETED -> {
                habitService.markHabitAsCompleted(habit.id());
                messageSender.sendHabit(u.chatId(), habitService.getHabitCheckinDtoById(payload.extractId(u.getPayload())));
            }
            case HABITS_SET_STATUS_PAUSED -> {
                habitService.markHabitAsPaused(habit.id());
                messageSender.sendHabit(u.chatId(), habitService.getHabitCheckinDtoById(payload.extractId(u.getPayload())));
            }
            case HABITS_SET_STATUS_IN_PROGRESS -> {
                habitService.markHabitAsInProgress(habit.id());
                messageSender.sendHabit(u.chatId(), habitService.getHabitCheckinDtoById(payload.extractId(u.getPayload())));
            }
            case HABITS_MARK_AS_UNCOMPLETED -> {
                habitService.uncheckinHabit(habit.id(), LocalDate.now());
                messageSender.sendHabit(u.chatId(), habitService.getHabitCheckinDtoById(payload.extractId(u.getPayload())));
            }
            case HABITS_MARK_AS_COMPLETED -> {
                habitService.checkinHabit(habit.id(), LocalDate.now());
                messageSender.sendHabit(u.chatId(), habitService.getHabitCheckinDtoById(payload.extractId(u.getPayload())));
            }
            case HABITS_CHANGE_TITLE -> {
                habitRedisRepo.save(u.chatId(), habit);
                messageSender.sendHabitTitleInput(habit.id());
            }
            case HABITS_CHANGE_DESCRIPTION -> {
                habitRedisRepo.save(u.chatId(), habit);
                messageSender.sendHabitDescriptionInput(habit.id());
            }
            case HABITS_CHANGE_INTERVAL -> {
                habitRedisRepo.save(u.chatId(), habit);
                messageSender.sendHabitIntervalInput(habit.id());
            }
            case HABITS_CHANGE_GOAL_DATE -> {
                habitRedisRepo.save(u.chatId(), habit);
                messageSender.sendHabitGoalDateInput(habit.id());
            }
        }
    }

    public void changeHabitTitle(Update u) {
        String text = u.getText();
        if (text != null && !text.isBlank()) {
            Optional<HabitCheckinDto> habit = habitRedisRepo.get(u.chatId());

            HabitCheckinDto prevHabit = habit.orElse(null);
            if (prevHabit == null) {
                prevHabit = new HabitCheckinDto(
                        null,
                        text,
                        null,
                        HabitStatus.IN_PROGRESS,
                        null,
                        null,
                        null,
                        null,
                        false,
                        false
                );
            }

            HabitCheckinDto newHabit = new HabitCheckinDto(
                    prevHabit.id(),
                    text,
                    prevHabit.description(),
                    prevHabit.status(),
                    prevHabit.interval(),
                    prevHabit.priority(),
                    prevHabit.day(),
                    prevHabit.goalDate(),
                    prevHabit.isCompleted(),
                    prevHabit.isCompletedOnTime()
            );

            habitRedisRepo.save(u.chatId(), newHabit);

            maxApi.postMessage(u.chatId(), messageSender.createHabitCreateKeyboardBody(
                    newHabit.title(),
                    newHabit.description(),
                    newHabit.interval() != null ? newHabit.interval().getDisplayName() : null,
                    newHabit.goalDate() != null ? formatGoalDate(newHabit.goalDate()) : null
            )).block();
        }
    }

    public void changeHabitDescription(Update u) {
        String text = u.getText();
        if (text != null && !text.isBlank()) {
            Optional<HabitCheckinDto> habit = habitRedisRepo.get(u.chatId());

            HabitCheckinDto prevHabit = habit.orElse(null);
            if (prevHabit == null) {
                prevHabit = new HabitCheckinDto(
                        null,
                        null,
                        text,
                        HabitStatus.IN_PROGRESS,
                        null,
                        null,
                        null,
                        null,
                        false,
                        false
                );
            }

            HabitCheckinDto newHabit = new HabitCheckinDto(
                    prevHabit.id(),
                    prevHabit.title(),
                    text,
                    prevHabit.status(),
                    prevHabit.interval(),
                    prevHabit.priority(),
                    prevHabit.day(),
                    prevHabit.goalDate(),
                    prevHabit.isCompleted(),
                    prevHabit.isCompletedOnTime()
            );

            habitRedisRepo.save(u.chatId(), newHabit);
            maxApi.postMessage(u.chatId(), messageSender.createHabitCreateKeyboardBody(
                    newHabit.title(),
                    newHabit.description(),
                    newHabit.interval() != null ? newHabit.interval().getDisplayName() : null,
                    newHabit.goalDate() != null ? formatGoalDate(newHabit.goalDate()) : null
            )).block();
        }
    }

    public void getAllHabitsList(Update u) {
        messageSender.sendAllHabitsList(u.chatId(), habitService.getAllHabits(u.chatId()));
    }

    public void getTodayHabitsList(Update u) {
        messageSender.sendTodayHabitsList(u.chatId(), habitService.getHabitsForToday(u.chatId()));
    }

    public void getWeekHabitsList(Update u) {
        messageSender.sendTodayHabitsList(u.chatId(), habitService.getHabitsForWeek(u.chatId()));
    }

    public void changeHabitInterval(Update u) {
        String text = u.getText();
        if (text != null && !text.isBlank()) {
            HabitInterval interval = HabitInterval.fromString(text);
            if (interval == null) {
                return;
            }

            Optional<HabitCheckinDto> habit = habitRedisRepo.get(u.chatId());

            HabitCheckinDto prevHabit = habit.orElse(null);
            if (prevHabit == null) {
                prevHabit = new HabitCheckinDto(
                        null,
                        null,
                        null,
                        HabitStatus.IN_PROGRESS,
                        interval,
                        null,
                        null,
                        null,
                        false,
                        false
                );
            }

            HabitCheckinDto newHabit = new HabitCheckinDto(
                    prevHabit.id(),
                    prevHabit.title(),
                    prevHabit.description(),
                    prevHabit.status(),
                    interval,
                    prevHabit.priority(),
                    prevHabit.day(),
                    prevHabit.goalDate(),
                    prevHabit.isCompleted(),
                    prevHabit.isCompletedOnTime()
            );

            habitRedisRepo.save(u.chatId(), newHabit);
            maxApi.postMessage(u.chatId(), messageSender.createHabitCreateKeyboardBody(
                    newHabit.title(),
                    newHabit.description(),
                    newHabit.interval() != null ? newHabit.interval().getDisplayName() : null,
                    newHabit.goalDate() != null ? formatGoalDate(newHabit.goalDate()) : null
            )).block();
        }
    }

    public void changeHabitGoalDate(Update u) {
        String text = u.getText();
        if (text != null && !text.isBlank()) {
            LocalDate goalDate;
            try {
                goalDate = parseGoalDate(text);
            } catch (DateTimeParseException e) {
                messageSender.sendText(u.chatId(), "Неверный формат даты, попробуйте еще раз");
                return;
            }

            Optional<HabitCheckinDto> habit = habitRedisRepo.get(u.chatId());

            HabitCheckinDto prevHabit = habit.orElse(null);
            if (prevHabit == null) {
                prevHabit = new HabitCheckinDto(
                        null,
                        null,
                        null,
                        HabitStatus.IN_PROGRESS,
                        null,
                        null,
                        null,
                        goalDate,
                        false,
                        false
                );
            }

            HabitCheckinDto newHabit = new HabitCheckinDto(
                    prevHabit.id(),
                    prevHabit.title(),
                    prevHabit.description(),
                    prevHabit.status(),
                    prevHabit.interval(),
                    prevHabit.priority(),
                    prevHabit.day(),
                    goalDate,
                    prevHabit.isCompleted(),
                    prevHabit.isCompletedOnTime()
            );

            habitRedisRepo.save(u.chatId(), newHabit);
            maxApi.postMessage(u.chatId(), messageSender.createHabitCreateKeyboardBody(
                    newHabit.title(),
                    newHabit.description(),
                    newHabit.interval() != null ? newHabit.interval().getDisplayName() : null,
                    newHabit.goalDate() != null ? formatGoalDate(newHabit.goalDate()) : null
            )).block();
        }
    }

    public static boolean shouldDoToday(HabitCheckinDto habit) {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        switch (habit.interval()) {
            case EVERY_DAY:
                return true;
            case EVERY_WEEK: {
                if (habit.isCompletedOnTime()) {
                    return true;
                } else {
                    return false;
                }
            }
            case EVERY_WEEKEND:
                return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;

            case EVERY_WEEKDAY:
                return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;

            case EVERY_SUNDAY:
                return dayOfWeek == DayOfWeek.SUNDAY;

            case EVERY_MONDAY:
                return dayOfWeek == DayOfWeek.MONDAY;

            case EVERY_TUESDAY:
                return dayOfWeek == DayOfWeek.TUESDAY;

            case EVERY_WEDNESDAY:
                return dayOfWeek == DayOfWeek.WEDNESDAY;

            case EVERY_THURSDAY:
                return dayOfWeek == DayOfWeek.THURSDAY;

            case EVERY_FRIDAY:
                return dayOfWeek == DayOfWeek.FRIDAY;

            case EVERY_SATURDAY:
                return dayOfWeek == DayOfWeek.SATURDAY;

            default:
                return false;
        }
    }


    private LocalDate parseGoalDate(String raw) throws DateTimeParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return LocalDate.parse(raw.trim(), formatter);
    }

    private String formatGoalDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return date.format(formatter);
    }

}
