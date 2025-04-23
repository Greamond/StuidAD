package com.example.stuid.api;

import com.example.stuid.models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {
    private static final String AUTHURL = "http://10.0.2.2:8081/api/auth/login";
    private static final String TASKSURL = "http://10.0.2.2:8081/api/task/getNewTasks";
    private static final String TASKADDSURL = "http://10.0.2.2:8081/api/task/addNewTask";

    public static void loginUser(String login, String password, final AuthCallback callback) {
        OkHttpClient client = new OkHttpClient();

        // Создаем JSON-тело запроса
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("login", login);
            jsonBody.put("password", password);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Формируем запрос
        RequestBody requestBody = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(AUTHURL)
                .post(requestBody)
                .build();

        // Отправляем запрос асинхронно
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        callback.onSuccess(responseData, parseUserRole(responseData));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    callback.onFailure("Ошибка: " + response.code());
                }
            }
        });
    }

    private static User parseUserRole(String json) throws JSONException {
        JSONObject jsonTask = new JSONObject(json);
        User user = new User();
        return user;
    }
}
