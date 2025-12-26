import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class BaseTest {
    @BeforeAll
    static void setUp(){
        MockServer.start();
    }

    @AfterAll
    static void teatDown(){
        MockServer.stop();
    }
}
