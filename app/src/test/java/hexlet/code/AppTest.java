package hexlet.code;

import hexlet.code.controllers.UrlController;
import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;
import io.ebean.DB;
import io.ebean.Transaction;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public final class AppTest {
    private static Javalin app;
    private static String baseUrl;
    private static Transaction transaction;
    private final int code200 = 200;
    private final int code302 = 302;

    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start();
        int port = app.port();
        baseUrl = "http://localhost:" + port;

        // add sample data
        new Url("https://www.test.com").save();
    }

    @AfterAll
    public static void afterAll() {
        app.stop();
    }

    @BeforeEach
    void beforeEach() {
        transaction = DB.beginTransaction();
    }

    @AfterEach
    void afterEach() {
        transaction.rollback();
    }

    @Nested
    class RootTest {
        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl)
                    .asString();

            assertThat(response.getStatus()).isEqualTo(code200);
            assertThat(response.getBody()).contains("Анализатор страниц");
        }
    }

    @Nested
    class UrlTest {
        @Test
        void testParseUrl() {
            String expected1 = "https://www.example.com";
            String expected2 = "https://www.example.com:8080";
            String actual1 = UrlController.parseUrl("https://www.example.com/one/two");
            String actual2 = UrlController.parseUrl("https://www.example.com:8080/one/two");
            String actual3 = UrlController.parseUrl("www.example.com");
            assertThat(actual1).isEqualTo(expected1);
            assertThat(actual2).isEqualTo(expected2);
            assertThat(actual3).isEqualTo(null);
        }

        @Test
        void testListUrl() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();

            String content = response.getBody();

            assertThat(response.getStatus()).isEqualTo(code200);
            assertThat(content).contains("Последняя проверка");
        }

        @Test
        void testAddUrl() {
            String urlName = "https://www.example.com";

            HttpResponse responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", urlName)
                    .asEmpty();

            assertThat(responsePost.getStatus()).isEqualTo(code302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();

            String content = response.getBody();

            assertThat(response.getStatus()).isEqualTo(code200);
            assertThat(content).contains(urlName);
            assertThat(content).contains("Страница успешно добавлена");

            Url actualUrl = new QUrl()
                    .name.equalTo(urlName)
                    .findOne();

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getName()).isEqualTo(urlName);
        }

        @Test
        void testAddWrongUrl() {
            String urlName = "www.example.com";

            HttpResponse responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", urlName)
                    .asEmpty();

            assertThat(responsePost.getStatus()).isEqualTo(code302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/")
                    .asString();

            String content = response.getBody();

            assertThat(response.getStatus()).isEqualTo(code200);
            assertThat(content).contains(urlName);
            assertThat(content).contains("Некорректный URL");
        }

        @Test
        void testAddExistingUrl() {
            String urlName = "https://www.test.com";

            HttpResponse responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", urlName)
                    .asEmpty();

            assertThat(responsePost.getStatus()).isEqualTo(code302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();

            String content = response.getBody();

            assertThat(response.getStatus()).isEqualTo(code200);
            assertThat(content).contains(urlName);
            assertThat(content).contains("Страница уже существует");
        }

        @Test
        void testShowUrl() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls/1")
                    .asString();

            String content = response.getBody();

            assertThat(response.getStatus()).isEqualTo(code200);
            assertThat(content).contains("https://www.test.com");
            assertThat(content).contains("description");
            assertThat(content).contains("Запустить проверку");
        }

        @Test
        void testCheckUrl() throws IOException {
            String expectedBody = Files.readString(Path.of("src/test/resources/test.html"));

            MockWebServer server = new MockWebServer();
            String serverUrl = server.url("/").toString();
            String correctServerUrl = serverUrl.substring(0, serverUrl.length() - 1);

            server.enqueue(new MockResponse());
            server.enqueue(new MockResponse().setBody(expectedBody));

            HttpResponse response = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", correctServerUrl)
                    .asEmpty();

            Url url = new QUrl()
                    .name.equalTo(correctServerUrl)
                    .findOne();

            assert url != null;
            long urlId = url.getId();

            assertThat(url).isNotNull();

            HttpResponse responseToCheck = Unirest
                    .post(baseUrl + "/urls/" + urlId + "/checks")
                    .asEmpty();

            HttpResponse<String> responseResult = Unirest
                    .get(baseUrl + "/urls/" + urlId)
                    .asString();


            List<UrlCheck> actualCheck = new QUrlCheck()
                    .findList();

            assertThat(actualCheck).isNotEmpty();

            String content = responseResult.getBody();

            assertThat(content).contains("Хекслет");
            assertThat(content).contains("Живое онлайн сообщество");
            assertThat(content).contains("Это заголовок h1");

            server.shutdown();
        }
    }
}
