package com.example.stuid.api;

import static com.example.stuid.classes.PasswordHasher.hashPassword;

import android.util.Log;

import com.example.stuid.models.Employee;
import com.example.stuid.models.Project;
import com.example.stuid.models.Subtask;
import com.example.stuid.models.SubtaskColumn;
import com.example.stuid.models.Task;
import com.example.stuid.models.TaskColumn;

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
    private static final String BASE_URL = "http://10.0.2.2:5000/api/";
    public OkHttpClient client;
    private String authToken;

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    private Request.Builder addAuthHeader(Request.Builder builder) {
        if (authToken != null) {
            builder.addHeader("Authorization", "Bearer " + authToken);
        }
        return builder;
    }

    public ApiClient() {
        this.client = new OkHttpClient();
    }

    public void loginUser(String email, String password, final AuthCallback callback) {
        String hashedPassword = hashPassword(password);

        // Создаем JSON тело запроса
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("Email", email);
            jsonBody.put("Password", hashedPassword);
        } catch (JSONException e) {
            callback.onFailure("Error creating request: " + e.getMessage());
            return;
        }

        RequestBody requestBody = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json")
        );

        String LOGIN_URL = BASE_URL + "auth/login";
        Log.d("Request", "URL: " + LOGIN_URL);
        Log.d("Request", "Body: " + jsonBody.toString());

        Request request = addAuthHeader(new Request.Builder())
                .url(LOGIN_URL)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("NetworkError", "Error: " + e.getMessage());
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "empty body";
                    Log.e("ServerError", "Code: " + response.code() + ", Body: " + errorBody);
                    callback.onFailure("Server error: " + response.code() + ", " + errorBody);
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);

                    // Получаем токен из ответа
                    String token = json.getString("Token");

                    Employee employee = new Employee();
                    employee.setEmployeeId(json.getInt("EmployeeId"));
                    employee.setLastName(json.getString("LastName"));
                    employee.setFirstName(json.getString("FirstName"));
                    employee.setMiddleName(json.getString("MiddleName"));
                    employee.setEmail(json.getString("Email"));
                    employee.setDescription(json.getString("Description"));

                    callback.onSuccess(employee, token);
                } catch (Exception e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void registerUser(String firstName, String lastName, String middleName,
                             String email, String password,
                             final AuthCallback callback) {
        String hashedPassword = hashPassword(password);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("FirstName", firstName);
            jsonBody.put("LastName", lastName);
            jsonBody.put("MiddleName", middleName);
            jsonBody.put("Email", email);
            jsonBody.put("Password", hashedPassword);
        } catch (JSONException e) {
            callback.onFailure("Error creating request: " + e.getMessage());
            return;
        }

        RequestBody requestBody = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json")
        );

        String REGISTER_URL = BASE_URL + "auth/register";
        Log.d("Request", "URL: " + REGISTER_URL);
        Log.d("Request", "Body: " + jsonBody.toString());

        Request request = new Request.Builder()
                .url(REGISTER_URL)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("NetworkError", "Error: " + e.getMessage());
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "empty body";
                    Log.e("ServerError", "Code: " + response.code() + ", Body: " + errorBody);
                    callback.onFailure("Server error: " + response.code() + ", " + errorBody);
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);

                    // Получаем токен из ответа
                    String token = json.getString("Token");

                    Employee employee = new Employee();
                    employee.setEmployeeId(json.getInt("EmployeeId"));
                    employee.setLastName(json.getString("LastName"));
                    employee.setFirstName(json.getString("FirstName"));
                    employee.setMiddleName(json.getString("MiddleName"));
                    employee.setEmail(json.getString("Email"));
                    employee.setDescription(json.getString("Description"));

                    callback.onSuccess(employee, token);
                } catch (Exception e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void getEmployees(String authToken, final EmployeesCallback callback) {
        if (authToken == null || authToken.isEmpty()) {
            callback.onFailure("Authorization token is missing");
            return;
        }

        String EMPLOYEES_URL = BASE_URL + "Users/employees";

        Request request = new Request.Builder()
                .url(EMPLOYEES_URL)
                .addHeader("Authorization", "Bearer " + authToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 401) {
                    callback.onFailure("Unauthorized - please login again");
                    return;
                }

                if (!response.isSuccessful()) {
                    callback.onFailure("Server error: " + response.code());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONArray jsonArray = new JSONArray(responseData);
                    List<Employee> employees = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject json = jsonArray.getJSONObject(i);
                        Employee employee = new Employee();
                        employee.setEmployeeId(json.getInt("EmployeeId"));
                        employee.setLastName(json.getString("LastName"));
                        employee.setFirstName(json.getString("FirstName"));
                        employee.setMiddleName(json.getString("MiddleName"));
                        employee.setEmail(json.getString("Email"));
                        employee.setDescription(json.getString("Description"));
                        employee.setPhoto(json.getString("Photo"));
                        employees.add(employee);
                    }
                    callback.onSuccess(employees);
                } catch (Exception e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void createProject(String token, Project project, final ProjectCreateCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("Name", project.getName());
            jsonBody.put("Description", project.getDescription());
            jsonBody.put("IsPublic", project.isPublic());
            // Creator теперь не отправляем, сервер берет из токена

            RequestBody requestBody = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "Projects/createProject")
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            String errorBody = response.body() != null ?
                                    response.body().string() : "empty body";
                            callback.onFailure("Server error: " + response.code() + ": " + errorBody);
                            return;
                        }

                        // Получаем созданный проект из ответа
                        String responseData = response.body().string();
                        JSONObject json = new JSONObject(responseData);

                        Project createdProject = new Project(
                                json.getInt("Id"),
                                json.getString("Name"),
                                json.getString("Description"),
                                json.getBoolean("IsPublic"),
                                json.getInt("Creator")
                        );

                        callback.onSuccess(createdProject);
                    } catch (Exception e) {
                        callback.onFailure("Error parsing response: " + e.getMessage());
                    }
                }
            });
        } catch (JSONException e) {
            callback.onFailure("Error creating request: " + e.getMessage());
        }
    }

    public void updateProfile(int employeeId, String lastName, String firstName, String middleName,
                              String description, String token, final ProfileUpdateCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("LastName", lastName);
            jsonBody.put("FirstName", firstName);

            // Необязательные поля
            if (middleName != null) jsonBody.put("MiddleName", middleName);
            if (description != null) jsonBody.put("Description", description);

            Request request = new Request.Builder()
                    .url(BASE_URL + "Users/" + employeeId)
                    .put(RequestBody.create(jsonBody.toString(), MediaType.get("application/json")))
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "{}";
                    Log.d("API_RESPONSE", "Response: " + responseBody);

                    try {
                        JSONObject json = new JSONObject(responseBody);

                        if (response.isSuccessful()) {
                            // Проверяем наличие поля success
                            if (json.has("success") && json.getBoolean("success")) {
                                callback.onSuccess();
                            } else {
                                String errorMsg = json.optString("message", "Update failed");
                                callback.onFailure(errorMsg);
                            }
                        } else {
                            // Обработка ошибок сервера
                            String errorMsg = json.optString("error",
                                    "Server error: " + response.code());
                            callback.onFailure(errorMsg);
                        }
                    } catch (JSONException e) {
                        callback.onFailure("Invalid server response format");
                    }
                }
            });
        } catch (JSONException e) {
            callback.onFailure("Error creating request: " + e.getMessage());
        }
    }

    public void uploadProfilePhoto(int employeeId, String base64Image, String token,
                                   final ProfilePhotoCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("Photo", base64Image);

            RequestBody requestBody = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json")
            );

            String UPLOAD_PHOTO_URL = BASE_URL + "Users/" + employeeId + "/photo";
            Log.d("API Request", "Uploading photo to: " + UPLOAD_PHOTO_URL);

            Request request = new Request.Builder()
                    .url(UPLOAD_PHOTO_URL)
                    .put(requestBody)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        callback.onFailure("Server error: " + response.code());
                        return;
                    }
                    callback.onSuccess();
                }
            });
        } catch (JSONException e) {
            callback.onFailure("Error creating request");
        }
    }

    public void getProfilePhoto(int employeeId, String token, final ProfilePhotoDownloadCallback callback) {
        String GET_PHOTO_URL = BASE_URL + "Users/" + employeeId + "/photo";

        Request request = new Request.Builder()
                .url(GET_PHOTO_URL)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Server error: " + response.code());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);
                    String base64Image = json.getString("photo");
                    callback.onSuccess(base64Image);
                } catch (Exception e) {
                    callback.onFailure("Error parsing response");
                }
            }
        });
    }

    public void addParticipants(String token, int projectId, List<Integer> participantIds, ParticipantsCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            JSONArray participantsArray = new JSONArray();

            for (Integer participantId : participantIds) {
                participantsArray.put(participantId);
            }

            jsonBody.put("ProjectId", projectId);
            jsonBody.put("ParticipantIds", participantsArray);

            RequestBody requestBody = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "Participants/addParticipants")
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ?
                                response.body().string() : "empty body";
                        callback.onFailure("Server error: " + response.code() + ": " + errorBody);
                        return;
                    }
                    callback.onSuccess();
                }
            });
        } catch (JSONException e) {
            callback.onFailure("Error creating request: " + e.getMessage());
        }
    }

    public void getProjectParticipants(String token, int projectId, EmployeesCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "Participants/getParticipants/" + projectId)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ?
                            response.body().string() : "empty body";
                    callback.onFailure("Server error: " + response.code() + ": " + errorBody);
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    JSONArray jsonArray = new JSONArray(responseBody);
                    List<Employee> participants = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        Employee employee = new Employee();
                        employee.setEmployeeId(jsonObject.getInt("UserId"));
                        employee.setFirstName(jsonObject.getString("FirstName"));
                        employee.setLastName(jsonObject.getString("LastName"));
                        employee.setMiddleName(jsonObject.getString("MiddleName"));
                        employee.setEmail(jsonObject.getString("Email"));
                        participants.add(employee);
                    }

                    callback.onSuccess(participants);
                } catch (JSONException e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void updateProject(String token, Project project, ProjectCreateCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("Id", project.getId());
            jsonBody.put("Name", project.getName());
            jsonBody.put("Description", project.getDescription());
            jsonBody.put("IsPublic", project.isPublic());

            RequestBody requestBody = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "Projects/" + project.getId())
                    .put(requestBody)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            String errorBody = response.body() != null ?
                                    response.body().string() : "empty body";
                            callback.onFailure("Server error: " + response.code() + ": " + errorBody);
                            return;
                        }

                        String responseData = response.body().string();
                        JSONObject json = new JSONObject(responseData);

                        Project updatedProject = new Project(
                                json.getInt("Id"),
                                json.getString("Name"),
                                json.getString("Description"),
                                json.getBoolean("IsPublic"),
                                json.getInt("Creator")
                        );

                        callback.onSuccess(updatedProject);
                    } catch (Exception e) {
                        callback.onFailure("Error parsing response: " + e.getMessage());
                    }
                }
            });
        } catch (JSONException e) {
            callback.onFailure("Error creating request: " + e.getMessage());
        }
    }

    public void updateProjectParticipants(String token, int projectId, List<Integer> participantIds, ParticipantsCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            JSONArray participantsArray = new JSONArray();

            for (Integer participantId : participantIds) {
                participantsArray.put(participantId);
            }

            jsonBody.put("ProjectId", projectId);
            jsonBody.put("ParticipantIds", participantsArray);

            RequestBody requestBody = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "Participants/updateParticipants")
                    .put(requestBody)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ?
                                response.body().string() : "empty body";
                        callback.onFailure("Server error: " + response.code() + ": " + errorBody);
                        return;
                    }
                    callback.onSuccess();
                }
            });
        } catch (JSONException e) {
            callback.onFailure("Error creating request: " + e.getMessage());
        }
    }

    public void deleteProject(String token, int projectId, ParticipantsCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "Projects/" + projectId)
                .delete()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ?
                            response.body().string() : "empty body";
                    callback.onFailure("Server error: " + response.code() + ": " + errorBody);
                    return;
                }
                callback.onSuccess();
            }
        });
    }

    public void getUserProjects(String token, int userId, final ProjectsCallback callback) {
        String url = BASE_URL + "Projects/forUser/" + userId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Server error: " + response.code());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONArray jsonArray = new JSONArray(responseData);
                    List<Project> projects = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject json = jsonArray.getJSONObject(i);
                        projects.add(new Project(
                                json.getInt("Id"),
                                json.getString("Name"),
                                json.getString("Description"),
                                json.getBoolean("IsPublic"),
                                json.getInt("Creator")
                        ));
                    }
                    callback.onSuccess(projects);
                } catch (Exception e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void getProjectTasks(String token, int projectId, final TasksCallback callback) {
        String url = BASE_URL + "Tasks/project/" + projectId;
        Log.d("API_REQUEST", "Fetching tasks from: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("API_ERROR", "Network error", e);
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "null";
                    Log.e("API_ERROR", "Server error: " + response.code() + ", " + errorBody);
                    callback.onFailure("Server error: " + response.code());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    Log.d("API_RESPONSE", "Tasks response: " + responseData);

                    JSONArray jsonArray = new JSONArray(responseData);
                    List<Task> tasks = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject json = jsonArray.getJSONObject(i);
                        Task task = new Task(
                                json.getInt("Id"),
                                json.getInt("ProjectId"),
                                json.getString("Name"),
                                json.getString("Description"),
                                json.getInt("Chapter"),
                                json.getInt("CreatorId")
                        );
                        task.setPosition(json.getInt("Position"));
                        tasks.add(task);
                    }
                    callback.onSuccess(tasks);
                } catch (Exception e) {
                    Log.e("API_ERROR", "Parsing error", e);
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void getTaskSubtasks(String token, int taskId, final SubtasksCallback callback) {
        String url = BASE_URL + "Subtasks/task/" + taskId;
        Log.d("API_REQUEST", "Fetching tasks from: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("API_ERROR", "Network error", e);
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "null";
                    Log.e("API_ERROR", "Server error: " + response.code() + ", " + errorBody);
                    callback.onFailure("Server error: " + response.code());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    Log.d("API_RESPONSE", "Tasks response: " + responseData);

                    JSONArray jsonArray = new JSONArray(responseData);
                    List<Subtask> subtasks = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject json = jsonArray.getJSONObject(i);
                        Subtask subtask = new Subtask(
                                json.getInt("Id"),
                                json.getInt("TaskId"),
                                json.getString("Name"),
                                json.getString("Description"),
                                json.getInt("Responsible"),
                                json.getInt("CreatorId"),
                                json.getInt("ChapterId")

                        );
                        subtask.setPosition(json.getInt("Position"));
                        subtasks.add(subtask);
                    }
                    callback.onSuccess(subtasks);
                } catch (Exception e) {
                    Log.e("API_ERROR", "Parsing error", e);
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void createTask(String token, JSONObject taskData, TaskCreateCallback callback) {
        RequestBody body = RequestBody.create(
                taskData.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "Tasks")
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Server error: " + response.code());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);
                    Task task = new Task(
                            json.getInt("Id"),
                            json.getInt("ProjectId"),
                            json.getString("Name"),
                            json.getString("Description"),
                            json.getInt("Chapter")
                    );
                    task.setCreatorId(json.getInt("CreatorId")); // Устанавливаем creatorId
                    callback.onSuccess(task);
                } catch (Exception e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void createSubtask(String token, JSONObject subtaskData, SubtaskCreateCallback callback) {
        RequestBody body = RequestBody.create(
                subtaskData.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "Subtasks")
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Server error: " + response.code());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);
                    Subtask subtask = new Subtask(
                            json.getInt("Id"),
                            json.getInt("TaskId"),
                            json.getString("Name"),
                            json.getString("Description"),
                            json.getInt("Responsible"),
                            json.getInt("ChapterId")
                    );
                    subtask.setCreatorId(json.getInt("CreatorId"));
                    callback.onSuccess(subtask);
                } catch (Exception e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void getTaskAssignees(String token, int taskId, EmployeesCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "TaskResponsibles/task/" + taskId) // Исправленный URL
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Server error: " + response.code());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONArray jsonArray = new JSONArray(responseData);
                    List<Employee> employees = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject json = jsonArray.getJSONObject(i);
                        Employee employee = new Employee();
                        employee.setEmployeeId(json.getInt("EmployeeId"));
                        employee.setFirstName(json.getString("FirstName"));
                        employee.setLastName(json.getString("LastName"));
                        employee.setMiddleName(json.getString("MiddleName"));
                        employees.add(employee);
                    }
                    callback.onSuccess(employees);
                } catch (Exception e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void updateTask(String token, int taskId, JSONObject taskData, TaskCreateCallback callback) {
        RequestBody body = RequestBody.create(
                taskData.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "Tasks/" + taskId)
                .put(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "null";
                    Log.e("API_ERROR", "Server error: " + response.code() + ", " + errorBody);
                    callback.onFailure("Server error: " + response.code() + ": " + errorBody);
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);
                    Task task = new Task(
                            json.getInt("Id"),
                            json.getInt("ProjectId"),
                            json.getString("Name"),
                            json.getString("Description"),
                            json.getInt("Chapter")
                    );
                    task.setCreatorId(json.getInt("CreatorId")); // Устанавливаем creatorId
                    callback.onSuccess(task);
                } catch (Exception e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void updateSubtask(String token, int subtaskId, JSONObject subtaskData, SubtaskCreateCallback callback) {
        RequestBody body = RequestBody.create(
                subtaskData.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "Subtasks/" + subtaskId)
                .put(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "null";
                    Log.e("API_ERROR", "Server error: " + response.code() + ", " + errorBody);
                    callback.onFailure("Server error: " + response.code() + ": " + errorBody);
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);
                    Subtask subtask = new Subtask(
                            json.getInt("Id"),
                            json.getInt("TaskId"),
                            json.getString("Name"),
                            json.getString("Description"),
                            json.getInt("Responsible"),
                            json.getInt("ChapterId")
                    );
                    subtask.setCreatorId(json.getInt("CreatorId")); // Устанавливаем creatorId
                    callback.onSuccess(subtask);
                } catch (Exception e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void deleteTask(String token, int taskId, TaskDeleteCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "Tasks/" + taskId)
                .delete()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Server error: " + response.code());
                    return;
                }
                callback.onSuccess();
            }
        });
    }

    public void deleteSubtask(String token, int subtaskId, TaskDeleteCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "Subtasks/" + subtaskId)
                .delete()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Server error: " + response.code());
                    return;
                }
                callback.onSuccess();
            }
        });
    }

    public void getEmployeeInfo(String token, int employeeId, EmployeeCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "Users/" + employeeId)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        callback.onFailure("Server error: " + response.code());
                        return;
                    }

                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);

                    Employee employee = new Employee();
                    employee.setEmployeeId(json.getInt("Id"));
                    employee.setLastName(json.getString("LastName"));
                    employee.setFirstName(json.getString("FirstName"));
                    employee.setMiddleName(json.getString("MiddleName"));
                    employee.setEmail(json.getString("Email"));
                    employee.setDescription(json.getString("Description"));
                    employee.setPhoto(json.getString("Photo"));

                    callback.onSuccess(employee);
                } catch (Exception e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void updateTaskChapter(String token, int taskId, int chapterId, ProfileUpdateCallback callback) {
        try {
            // 1. Формируем JSON-тело
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("ChapterId", chapterId);

            // 2. Создаём запрос
            Request request = new Request.Builder()
                    .url(BASE_URL + "Tasks/" + taskId + "/status")
                    .put(RequestBody.create(
                            jsonBody.toString(),
                            MediaType.parse("application/json")
                    ))
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            // 3. Отправляем асинхронно
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        if (response.isSuccessful()) {
                            callback.onSuccess();
                        } else {
                            String errorBody = response.body() != null ?
                                    response.body().string() : "Empty error body";
                            callback.onFailure("Server error: " + response.code() + " - " + errorBody);
                        }
                    } catch (IOException e) {
                        callback.onFailure("Error parsing response: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            callback.onFailure("Request creation error: " + e.getMessage());
        }
    }

    public void updateSubtaskChapter(String token, int subtaskId, int chapterId, ProfileUpdateCallback callback) {
        try {
            // 1. Формируем JSON-тело
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("ChapterId", chapterId);

            // 2. Создаём запрос
            Request request = new Request.Builder()
                    .url(BASE_URL + "Subtasks/" + subtaskId + "/status")
                    .put(RequestBody.create(
                            jsonBody.toString(),
                            MediaType.parse("application/json")
                    ))
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            // 3. Отправляем асинхронно
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        if (response.isSuccessful()) {
                            callback.onSuccess();
                        } else {
                            String errorBody = response.body() != null ?
                                    response.body().string() : "Empty error body";
                            callback.onFailure("Server error: " + response.code() + " - " + errorBody);
                        }
                    } catch (IOException e) {
                        callback.onFailure("Error parsing response: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            callback.onFailure("Request creation error: " + e.getMessage());
        }
    }

    public void updateTaskOrder(String token, int projectId, int columnId, List<Task> orderedTasks, ParticipantsCallback callback) {
        JSONObject json = new JSONObject();
        try {
            json.put("projectId", projectId);
            json.put("columnId", columnId);

            JSONArray taskArray = new JSONArray();
            for (int i = 0; i < orderedTasks.size(); i++) {
                JSONObject taskJson = new JSONObject();
                taskJson.put("taskId", orderedTasks.get(i).getId());
                taskJson.put("position", i);
                taskArray.put(taskJson);
            }
            json.put("taskOrder", taskArray);

            RequestBody body = RequestBody.create(
                    MediaType.get("application/json; charset=utf-8"),
                    json.toString()
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "Tasks/update-order")
                    .put(body)
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure("Server error: " + response.code());
                    }
                }
            });

        } catch (JSONException e) {
            callback.onFailure("JSON error: " + e.getMessage());
}
    }

    public void updateSubtaskOrder(String token, int taskId, int columnId, List<Subtask> orderedTasks, ParticipantsCallback callback) {
        JSONObject json = new JSONObject();
        try {
            json.put("taskId", taskId);
            json.put("columnId", columnId);

            JSONArray taskArray = new JSONArray();
            for (int i = 0; i < orderedTasks.size(); i++) {
                JSONObject taskJson = new JSONObject();
                taskJson.put("subtaskId", orderedTasks.get(i).getId());
                taskJson.put("position", i);
                taskArray.put(taskJson);
            }
            json.put("taskOrder", taskArray);

            RequestBody body = RequestBody.create(
                    MediaType.get("application/json; charset=utf-8"),
                    json.toString()
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "Subtasks/update-order")
                    .put(body)
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure("Server error: " + response.code());
                    }
                }
            });

        } catch (JSONException e) {
            callback.onFailure("JSON error: " + e.getMessage());
        }
    }

    public void getColumnsForProject(String token, int projectId, ColumnsCallback callback) {
        String url = BASE_URL + "ChaptersTask/project/" + projectId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Server error: " + response.code());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONArray jsonArray = new JSONArray(responseData);
                    List<TaskColumn> columns = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject json = jsonArray.getJSONObject(i);
                        int id = json.getInt("Id");
                        String name = json.getString("Name");
                        columns.add(new TaskColumn(id,projectId, name, new ArrayList<>()));
                    }

                    callback.onSuccess(columns);
                } catch (JSONException e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void getSubtaskColumnsForTask(String token, int taskId, SubtaskColumnsCallback callback) {
        String url = BASE_URL + "ChaptersSubtask/task/" + taskId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Server error: " + response.code());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONArray jsonArray = new JSONArray(responseData);
                    List<SubtaskColumn> columns = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject json = jsonArray.getJSONObject(i);
                        int id = json.getInt("Id");
                        String name = json.getString("Name");
                        columns.add(new SubtaskColumn(id,taskId, name, new ArrayList<>()));
                    }

                    callback.onSuccess(columns);
                } catch (JSONException e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void createColumn(String token, String name, int projectId, ColumnCreateCallback callback) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("Name", name);
            jsonBody.put("ProjectId", projectId);
        } catch (JSONException e) {
            callback.onFailure("JSON error: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.get("application/json")
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "ChaptersTask")
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Server error: " + response.code());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);
                    int id = json.getInt("Id");
                    TaskColumn column = new TaskColumn(id,projectId, name, new ArrayList<>());
                    callback.onSuccess(column);
                } catch (JSONException e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void createSubtaskColumn(String token, String name, int taskId, SubtaskColumnCreateCallback callback) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("Name", name);
            jsonBody.put("TaskId", taskId);
        } catch (JSONException e) {
            callback.onFailure("JSON error: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.get("application/json")
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "ChaptersSubtask")
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Server error: " + response.code());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);
                    int id = json.getInt("Id");
                    SubtaskColumn column = new SubtaskColumn(id,taskId, name, new ArrayList<>());
                    callback.onSuccess(column);
                } catch (JSONException e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void deleteSubtaskColumn(String token, int columnId, TaskDeleteCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "ChaptersSubtask/" + columnId)
                .addHeader("Authorization", "Bearer " + token)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onFailure("Server error: " + response.code());
                }
            }
        });
    }

    public void updateSubtaskColumn(String token, int columnId, JSONObject jsonBody, SubtaskColumnCreateCallback callback) {
        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "ChaptersSubtask/" + columnId)
                .put(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Server error: " + response.code());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);

                    // Предположим, что сервер возвращает обновлённую колонку
                    SubtaskColumn column = new SubtaskColumn(
                            json.getInt("Id"),
                            json.getInt("TaskId"),
                            json.getString("Name"),
                            new ArrayList<>()
                    );

                    callback.onSuccess(column);
                } catch (Exception e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void updateColumn(String token, int columnId, JSONObject jsonBody, ColumnCreateCallback callback) {
        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "ChaptersTask/" + columnId)
                .put(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Server error: " + response.code());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);
                    TaskColumn column = new TaskColumn(
                            json.getInt("Id"),
                            json.getInt("ProjectId"),
                            json.getString("Name"),
                            new ArrayList<>()
                    );
                    callback.onSuccess(column);
                } catch (Exception e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void deleteColumn(String token, int columnId, TaskDeleteCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "ChaptersTask/" + columnId)
                .delete()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onFailure("Server error: " + response.code());
                }
            }
        });
    }

    public void getTasksByAssignee(String token, int assigneeId, TasksCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "Tasks/assignee/" + assigneeId)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Server error: " + response.code());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONArray jsonArray = new JSONArray(responseData);
                    List<Task> tasks = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject json = jsonArray.getJSONObject(i);
                        Task task = new Task(
                                json.getInt("Id"),
                                json.getInt("ProjectId"),
                                json.getString("Name"),
                                json.getString("Description"),
                                json.getInt("Chapter"),
                                json.getInt("CreatorId")
                        );
                        tasks.add(task);
                    }

                    callback.onSuccess(tasks);
                } catch (Exception e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void sendPasswordResetEmail(String email, int code, AuthPasswordResetCallback callback) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("Email", email);
            jsonBody.put("Code", code);
        } catch (JSONException e) {
            callback.onFailure("JSON error: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "auth/forgot-password")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Server error: " + response.code());
                    return;
                }
                callback.onSuccess("Код отправлен на email", null);
            }
        });
    }

    public void resetPassword(String email, String newPassword, AuthPasswordResetCallback callback) {
        String hashedPassword = hashPassword(newPassword);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", email);
            jsonBody.put("newPassword", hashedPassword);
        } catch (JSONException e) {
            callback.onFailure("JSON error: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "auth/reset-password")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Ошибка сброса: " + response.code());
                    return;
                }
                callback.onSuccess("Пароль успешно изменён", null);
            }
        });
    }
}
