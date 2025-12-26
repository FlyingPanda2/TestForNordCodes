import io.qameta.allure.*;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Тестирование эндпоинта /endpoint")
class ApplicationEndpointTest extends BaseTest {

    private static final String BASE_URL = "http://localhost:8080/endpoint";
    private static final String API_KEY = "qazWSXedc";

    // Генерирует уникальный токен для каждого вызова
    private String generateValidToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32).toUpperCase();
    }

    // Вспомогательный метод для создания запроса (без отправки)
    private HttpRequest buildRequest(String token, String action, String apiKey) {
        String body = "token=" + URLEncoder.encode(token, StandardCharsets.UTF_8) +
                "&action=" + URLEncoder.encode(action, StandardCharsets.UTF_8);

        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("X-Api-Key", apiKey)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
    }

    // Единый метод отправки с отображением шагов (@Step)
    @Step("Отправить запрос: token={token}, action={action}, X-Api-Key={apiKey}")
    private HttpResponse<String> sendRequest(String token, String action, String apiKey) throws Exception {
        HttpRequest request = buildRequest(token, action, apiKey);
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    // Упрощённая версия с правильным ключом по умолчанию
    private HttpResponse<String> sendRequest(String token, String action) throws Exception {
        return sendRequest(token, action, API_KEY);
    }

    // Метод проверки статуса с отображением шагов (@Step)
    @Step("Проверить, что статус ответа = {expectedStatus}")
    private void assertStatusCode(HttpResponse<String> response, int expectedStatus) {
        assertEquals(expectedStatus, response.statusCode());
    }

    // Метод проверки тела ответа с отображением шагов (@Step)
    @Step("Проверить, что result = '{expectedResult}'")
    private void assertResult(HttpResponse<String> response, String expectedResult) {
        JSONObject json = new JSONObject(response.body());
        assertEquals(expectedResult, json.getString("result"));
    }

    // =============== УСПЕШНЫЕ СЦЕНАРИИ ===============

    @DisplayName("Успешный вход: LOGIN")
    @Description("""
    Запрос:
    POST /endpoint
    Headers:
      X-Api-Key: qazWSXedc
      Content-Type: application/x-www-form-urlencoded; charset=UTF-8
      Accept: application/json
    Body:
      token=A1B2C3D4E5F67890ABCDEF1234567890&action=LOGIN

    Поведение:
    Приложение отправляет запрос на /auth внешнего сервиса.
    Если ответ 200 — токен сохраняется во внутреннем хранилище.

    Ожидаемый ответ:
    Статус: 200 OK
    Тело: {"result": "OK"}
    """)
    @Test
    void testLoginSuccess() throws Exception {
        String token = generateValidToken();

        HttpResponse<String> response = sendRequest(token, "LOGIN");

        assertStatusCode(response, 200);
        assertResult(response, "OK");
    }

    @DisplayName("Действие после входа: ACTION")
    @Description("""
    Запрос:
    1. Сначала: LOGIN с валидным токеном
    2. Затем: ACTION с тем же токеном

    Поведение:
    После LOGIN токен сохраняется.
    ACTION доступен только для зарегистрированных токенов.

    Ожидаемый ответ:
    Статус: 200 OK
    Тело: {"result": "OK"}
    """)
    @Test
    void testActionSuccess() throws Exception {
        String token = generateValidToken();

        sendRequest(token, "LOGIN");
        HttpResponse<String> response = sendRequest(token, "ACTION");

        assertStatusCode(response, 200);
        assertResult(response, "OK");
    }

    @DisplayName("Завершение сессии: LOGOUT")
    @Description("""
    Запрос:
    1. Сначала: LOGIN с валидным токеном
    2. Затем: LOGOUT с тем же токеном

    Поведение:
    LOGOUT удаляет токен из внутреннего хранилища.

    Ожидаемый ответ:
    Статус: 200 OK
    Тело: {"result": "OK"}
    """)
    @Test
    void testLogoutSuccess() throws Exception {
        String token = generateValidToken();

        sendRequest(token, "LOGIN");
        HttpResponse<String> response = sendRequest(token, "LOGOUT");

        assertStatusCode(response, 200);
        assertResult(response, "OK");
    }

    // =============== НЕГАТИВНЫЕ СЦЕНАРИИ ===============

    @DisplayName("Неверный API-ключ: 'WRONG_KEY'")
    @Description("""
    Запрос:
    POST /endpoint
    Headers:
      X-Api-Key: WRONG_KEY  ← не совпадает с qazWSXedc
      Content-Type: application/x-www-form-urlencoded; charset=UTF-8
      Accept: application/json
    Body:
      token=A1B2C3D4E5F67890ABCDEF1234567890&action=LOGIN

    Поведение:
    Приложение отклоняет запрос на этапе аутентификации.

    Ожидаемый ответ:
    Статус: 401 Unauthorized
    """)
    @Test
    void testInvalidApiKey() throws Exception {
        String token = generateValidToken();

        HttpResponse<String> response = sendRequest(token, "LOGIN", "WRONG_KEY");

        assertStatusCode(response, 401);
    }

    @DisplayName("Некорректный токен: 'SHORT'")
    @Description("""
    Запрос:
    POST /endpoint
    Headers:
      X-Api-Key: qazWSXedc
      Content-Type: application/x-www-form-urlencoded; charset=UTF-8
      Accept: application/json
    Body:
      token=SHORT&action=LOGIN  ← токен короче 32 символов

    Поведение:
    Токен не соответствует шаблону ^[0-9A-F]{32}$

    Ожидаемый ответ:
    Статус: 400 Bad Request
    """)
    @Test
    void testInvalidTokenLength() throws Exception {
        String token = "SHORT";

        HttpResponse<String> response = sendRequest(token, "LOGIN");

        assertStatusCode(response, 400);
    }

    @DisplayName("Неизвестная команда: 'HACK'")
    @Description("""
    Запрос:
    POST /endpoint
    Headers:
      X-Api-Key: qazWSXedc
      Content-Type: application/x-www-form-urlencoded; charset=UTF-8
      Accept: application/json
    Body:
      token=A1B2C3D4E5F67890ABCDEF1234567890&action=HACK  ← команда не существует

    Поведение:
    Система поддерживает только LOGIN, ACTION, LOGOUT.

    Ожидаемый ответ:
    Статус: 400 Bad Request
    """)
    @Test
    void testInvalidAction() throws Exception {
        String token = generateValidToken();

        HttpResponse<String> response = sendRequest(token, "HACK");

        assertStatusCode(response, 400);
    }

    @DisplayName("ACTION без предварительного LOGIN")
    @Description("""
    Запрос:
    POST /endpoint
    Headers:
      X-Api-Key: qazWSXedc
      Content-Type: application/x-www-form-urlencoded; charset=UTF-8
      Accept: application/json
    Body:
      token=A1B2C3D4E5F67890ABCDEF1234567890&action=ACTION

    Поведение:
    Токен валиден, но не зарегистрирован (нет предварительного LOGIN).

    Ожидаемый ответ:
    Статус: 403 Forbidden
    Тело: {"result": "ERROR", "message": "Token '...' not found"}
    """)
    @Test
    void testActionWithoutLogin() throws Exception {
        String token = generateValidToken();

        HttpResponse<String> response = sendRequest(token, "ACTION");

        assertStatusCode(response, 403);
    }

    @DisplayName("Удаление токена после LOGOUT")
    @Description("""
    Запрос:
    1. LOGIN → токен сохраняется
    2. LOGOUT → токен удаляется
    3. ACTION → попытка использовать удалённый токен

    Поведение:
    После LOGOUT токен больше не существует в системе.

    Ожидаемый ответ на шаге 3:
    Статус: 403 Forbidden
    Тело: {"result": "ERROR", "message": "Token '...' not found"}
    """)
    @Test
    void testLogoutRemovesTokenFromStorage() throws Exception {
        String token = generateValidToken();

        sendRequest(token, "LOGIN");   // Шаг 1
        sendRequest(token, "LOGOUT");  // Шаг 2
        HttpResponse<String> response = sendRequest(token, "ACTION"); // Шаг 3

        assertStatusCode(response, 403);

        // Дополнительная проверка тела
        JSONObject error = new JSONObject(response.body());
        assertEquals("ERROR", error.getString("result"));
        assertTrue(error.getString("message").contains("not found"), "Сообщение должно содержать 'not found'");
    }
}