package com.smarttodo.app.service;

import com.smarttodo.app.entity.UserEntity;
import com.smarttodo.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void updateRealName(Long userId, String newName) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Не найден юзер с id: " + userId));

        user.setDisplayName(newName);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserEntity getUserByChatId(Long chatId) {
        return userRepository.findByChatId(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Не найден юзер с id чата: " + chatId));
    }

    @Transactional
    public UserEntity createUser(Long chatId, String displayName) {
        if (userRepository.existsByChatId(chatId)) {
            throw new IllegalArgumentException("Юзер с таким id чата уже существует: " + chatId);
        }

        UserEntity user = new UserEntity(chatId);
        user.setDisplayName(displayName);
        return userRepository.save(user);
    }
}
