package com.example.stuid.api;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;

public class SafeCallManager {
    private final List<Call> activeCalls = new ArrayList<>();

    // Добавить новый Call
    public void add(Call call) {
        activeCalls.add(call);
    }

    // Отменить один конкретный запрос
    public void cancel(Call call) {
        if (call != null && !call.isCanceled()) {
            call.cancel();
        }
        activeCalls.remove(call);
    }

    // Отменить все активные запросы
    public void cancelAll() {
        for (Call call : activeCalls) {
            if (!call.isCanceled()) {
                call.cancel();
            }
        }
        activeCalls.clear();
    }

    // Очистить список без отмены (если запросы уже завершились)
    public void clear() {
        activeCalls.clear();
    }

    // Получить количество активных запросов
    public int size() {
        return activeCalls.size();
    }
}