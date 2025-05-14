package com.example.stuid.api;

import android.util.Log;

import com.example.stuid.models.Employee;
import com.example.stuid.models.Project;

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
    private OkHttpClient client;
    private String authToken; // JWT токена

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
        // Создаем JSON тело запроса
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("Email", email);
            jsonBody.put("Password", password);
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
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("FirstName", firstName);
            jsonBody.put("LastName", lastName);
            jsonBody.put("MiddleName", middleName);
            jsonBody.put("Email", email);
            jsonBody.put("Password", password);
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
                        employees.add(employee);
                    }
                    callback.onSuccess(employees);
                } catch (Exception e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    public void getProjects(String authToken, final ProjectsCallback callback) {
        if (authToken == null || authToken.isEmpty()) {
            callback.onFailure("Authorization token is missing");
            return;
        }

        String PROJECTS_URL = BASE_URL + "Projects";

        Request request = new Request.Builder()
                .url(PROJECTS_URL)
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
                    callback.onFailure("Session expired. Please login again");
                    return;
                }

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
}
