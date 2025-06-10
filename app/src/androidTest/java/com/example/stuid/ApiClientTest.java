package com.example.stuid;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.example.stuid.api.ApiClient;
import com.example.stuid.api.EmployeesCallback;
import com.example.stuid.models.Employee;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import java.io.IOException;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ApiClientTest {

    @Mock
    private OkHttpClient mockClient;

    @Mock
    private Call mockCall;

    private ApiClient apiClient;
    private final String testToken = "test_token";
    private final String testUrl = "http://10.0.2.2:5000/api/Users/employees";

    @Before
    public void setUp() {
        apiClient = new ApiClient();
        apiClient.client = mockClient; // Подменяем реальный клиент на mock
        apiClient.setAuthToken(testToken);
    }

    @Test
    public void getEmployees_Success() throws Exception {
        // Подготовка тестовых данных
        String jsonResponse = "[{" +
                "\"EmployeeId\":1," +
                "\"LastName\":\"Ivanov\"," +
                "\"FirstName\":\"Ivan\"," +
                "\"MiddleName\":\"Ivanovich\"," +
                "\"Email\":\"ivanov@example.com\"," +
                "\"Description\":\"Developer\"," +
                "\"Photo\":\"photo1.jpg\"}]";
        Response successResponse = new Response.Builder()
                .request(new Request.Builder().url(testUrl).build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(
                        MediaType.get("application/json"),
                        jsonResponse))
                .build();
        // Настройка mock
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        // Захват callback
        ArgumentCaptor<Callback> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
        // Вызов тестируемого метода
        TestEmployeesCallback testCallback = new TestEmployeesCallback();
        apiClient.getEmployees(testToken, testCallback);
        // Имитация успешного ответа
        verify(mockCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(mockCall, successResponse);
        // Проверки
        assertTrue(testCallback.successCalled);
        assertEquals(1, testCallback.employees.size());
        assertEquals("Ivanov", testCallback.employees.get(0).getLastName());
    }

    @Test
    public void getEmployees_NetworkError() {
        // Настройка mock
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        // Захват callback
        ArgumentCaptor<Callback> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
        // Вызов тестируемого метода
        TestEmployeesCallback testCallback = new TestEmployeesCallback();
        apiClient.getEmployees(testToken, testCallback);
        // Имитация ошибки сети
        verify(mockCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(mockCall, new IOException("Network error"));
        // Проверки
        assertTrue(testCallback.failureCalled);
        assertEquals("Network error: Network error", testCallback.errorMessage);
    }

    @Test
    public void getEmployees_Unauthorized() throws Exception {
        // Подготовка ответа с ошибкой 401
        Response unauthorizedResponse = new Response.Builder()
                .request(new Request.Builder().url(testUrl).build())
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .body(ResponseBody.create(
                        MediaType.get("application/json"),
                        "{\"error\":\"Unauthorized\"}"))
                .build();
        // Настройка mock
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        // Захват callback
        ArgumentCaptor<Callback> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
        // Вызов тестируемого метода
        TestEmployeesCallback testCallback = new TestEmployeesCallback();
        apiClient.getEmployees(testToken, testCallback);
        // Имитация ответа
        verify(mockCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(mockCall, unauthorizedResponse);
        // Проверки
        assertTrue(testCallback.failureCalled);
        assertEquals("Unauthorized - please login again", testCallback.errorMessage);
    }

    // Вспомогательный класс для проверки callback
    private static class TestEmployeesCallback implements EmployeesCallback {
        boolean successCalled = false;
        boolean failureCalled = false;
        List<Employee> employees;
        String errorMessage;

        @Override
        public void onSuccess(List<Employee> employees) {
            this.successCalled = true;
            this.employees = employees;
        }

        @Override
        public void onFailure(String error) {
            this.failureCalled = true;
            this.errorMessage = error;
        }
    }
}