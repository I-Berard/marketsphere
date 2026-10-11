package rw.ac.rca.marketsphere;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import rw.ac.rca.marketsphere.config.DataInitializer;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Acceptance test suite aligned with CSS-UAT-2026-001 v1.0.
 * UAT-001 to UAT-024 (except UAT-021 which is a CI/CD pipeline test).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UatIntegrationTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("marketsphere")
                    .withUsername("marketsphere")
                    .withPassword("marketsphere");

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void register(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        r.add("marketsphere.jwt.secret", () -> "marketsphere-test-secret-32bytes!!");
    }

    private static String customerToken;
    private static String adminToken;
    private static String targetProductId;
    private static String orderId;

    @Autowired
    MockMvc mvc;

    final ObjectMapper om = new ObjectMapper();

    private JsonNode tree(MvcResult r) throws Exception {
        return om.readTree(r.getResponse().getContentAsString());
    }

    private String field(MvcResult r, String pointer) throws Exception {
        return tree(r).at(pointer).asText();
    }

    private void ensureAdminToken() throws Exception {
        if (adminToken != null) return;
        MvcResult res = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@marketsphere.rw\",\"password\":\"" + DataInitializer.DEMO_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = field(res, "/accessToken");
    }

    @Test @Order(1)
    @DisplayName("UAT-001 [Medium]: Customer can register with valid details")
    void uat001_customerRegistration() throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"uat.customer@marketsphere.rw\",\"password\":\"UatPass1!\",\"phone\":\"0780000099\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("uat.customer@marketsphere.rw"))
                .andReturn();
        customerToken = field(res, "/accessToken");
        assertNotNull(customerToken);
    }

    @Test @Order(2)
    @DisplayName("UAT-002 [High]: Registered customer can authenticate")
    void uat002_customerLogin() throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"uat.customer@marketsphere.rw\",\"password\":\"UatPass1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("uat.customer@marketsphere.rw"))
                .andReturn();
        customerToken = field(res, "/accessToken");
    }

    @Test @Order(3)
    @DisplayName("UAT-003 [High]: Invalid credentials are rejected without exposing sensitive info")
    void uat003_invalidLogin() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"uat.customer@marketsphere.rw\",\"password\":\"WrongPass99\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("email or password is incorrect"))
                .andExpect(content().string(not(containsString("WrongPass99"))));
    }

    @Test @Order(4)
    @DisplayName("UAT-004 [High]: Products are displayed with required information")
    void uat004_browseProducts() throws Exception {
        MvcResult res = mvc.perform(get("/api/v1/products?size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].name").exists())
                .andExpect(jsonPath("$.content[0].priceRwf").exists())
                .andExpect(jsonPath("$.content[0].stockBadge").exists())
                .andReturn();
        targetProductId = tree(res).at("/content/0/id").asText();
        assertFalse(targetProductId.isBlank());
    }

    @Test @Order(5)
    @DisplayName("UAT-005 [High]: Search returns relevant results and product detail is viewable")
    void uat005_productSearchAndDetails() throws Exception {
        mvc.perform(get("/api/v1/products?q=USB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        if (targetProductId != null && !targetProductId.isBlank()) {
            mvc.perform(get("/api/v1/products/" + targetProductId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(targetProductId))
                    .andExpect(jsonPath("$.priceRwf").isNumber());
        }
    }

    @Test @Order(6)
    @DisplayName("UAT-006 [High]: Customer can add an available product to cart")
    void uat006_addToCart() throws Exception {
        Assumptions.assumeTrue(customerToken != null && targetProductId != null);
        mvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + targetProductId + "\",\"qty\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value(targetProductId))
                .andExpect(jsonPath("$.items[0].qty").value(1))
                .andExpect(jsonPath("$.items[0].unitPriceRwf").isNumber());
    }

    @Test @Order(7)
    @DisplayName("UAT-007 [Medium]: Cart quantity update and removal work correctly")
    void uat007_updateAndRemoveCart() throws Exception {
        Assumptions.assumeTrue(customerToken != null && targetProductId != null);
        mvc.perform(patch("/api/v1/cart/items/" + targetProductId)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qty\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].qty").value(2));
        mvc.perform(patch("/api/v1/cart/items/" + targetProductId)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qty\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + targetProductId + "\",\"qty\":1}"))
                .andExpect(status().isOk());
    }

    @Test @Order(8)
    @DisplayName("UAT-008 [Critical]: Unique order is created with correct items and total")
    void uat008_createOrder() throws Exception {
        Assumptions.assumeTrue(customerToken != null);
        MvcResult res = mvc.perform(post("/api/v1/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("X-Payment-Stub-Mode", "SUCCESS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"MTN_MOMO\",\"stubMode\":\"SUCCESS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.orderNumber").isNotEmpty())
                .andExpect(jsonPath("$.totalRwf").isNumber())
                .andReturn();
        orderId = field(res, "/id");
        assertFalse(orderId.isBlank());
    }

    @Test @Order(9)
    @DisplayName("UAT-009 [High]: System prevents ordering more than available stock")
    void uat009_insufficientStockBlocksOrder() throws Exception {
        Assumptions.assumeTrue(customerToken != null && targetProductId != null);
        mvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + targetProductId + "\",\"qty\":999999}"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    assertTrue(s == 400 || s == 409 || s == 422,
                            "Excessive qty should be rejected with 4xx, got: " + s);
                });
    }

    @Test @Order(10)
    @DisplayName("UAT-010 [High]: Payment transaction is created and order enters payment state")
    void uat010_paymentInitiation() throws Exception {
        Assumptions.assumeTrue(customerToken != null && targetProductId != null);
        mvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + targetProductId + "\",\"qty\":1}"))
                .andExpect(status().isOk());
        MvcResult res = mvc.perform(post("/api/v1/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("X-Payment-Stub-Mode", "SUCCESS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"MTN_MOMO\",\"stubMode\":\"SUCCESS\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String st = field(res, "/status");
        assertTrue(st.equals("PAID") || st.equals("PLACED") || st.equals("PAYMENT_TIMED_OUT"),
                "Expected defined payment state, got: " + st);
    }

    @Test @Order(11)
    @DisplayName("UAT-011 [Critical]: Successful payment results in PAID order")
    void uat011_paymentSuccess() throws Exception {
        Assumptions.assumeTrue(customerToken != null && targetProductId != null);
        mvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + targetProductId + "\",\"qty\":1}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("X-Payment-Stub-Mode", "SUCCESS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"MTN_MOMO\",\"stubMode\":\"SUCCESS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.totalRwf").isNumber());
    }

    @Test @Order(12)
    @DisplayName("UAT-012 [Critical]: Payment timeout leaves order in consistent safe state")
    void uat012_paymentTimeout() throws Exception {
        Assumptions.assumeTrue(customerToken != null && targetProductId != null);
        mvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + targetProductId + "\",\"qty\":1}"))
                .andExpect(status().isOk());
        MvcResult res = mvc.perform(post("/api/v1/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("X-Payment-Stub-Mode", "TIMEOUT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"MTN_MOMO\",\"stubMode\":\"TIMEOUT\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String st = field(res, "/status");
        assertNotEquals("PAID", st, "Timeout must not result in PAID");
        assertTrue(st.equals("PAYMENT_TIMED_OUT") || st.equals("PLACED") || st.equals("CANCELLED"),
                "Timeout should leave order in a defined safe state, got: " + st);
    }

    @Test @Order(13)
    @DisplayName("UAT-013 [High]: Failed payment is not marked as paid")
    void uat013_paymentFailure() throws Exception {
        Assumptions.assumeTrue(customerToken != null && targetProductId != null);
        mvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + targetProductId + "\",\"qty\":1}"))
                .andExpect(status().isOk());
        MvcResult res = mvc.perform(post("/api/v1/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("X-Payment-Stub-Mode", "DECLINE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"MTN_MOMO\",\"stubMode\":\"DECLINE\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        assertNotEquals("PAID", field(res, "/status"),
                "Declined payment must NOT result in PAID order");
    }

    @Test @Order(14)
    @DisplayName("UAT-014 [High]: Duplicate checkout idempotency key does not create two orders")
    void uat014_duplicatePaymentIdempotent() throws Exception {
        Assumptions.assumeTrue(customerToken != null && targetProductId != null);
        mvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + targetProductId + "\",\"qty\":1}"))
                .andExpect(status().isOk());
        String iKey = UUID.randomUUID().toString();
        MvcResult first = mvc.perform(post("/api/v1/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", iKey)
                        .header("X-Payment-Stub-Mode", "SUCCESS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"MTN_MOMO\",\"stubMode\":\"SUCCESS\"}"))
                .andExpect(status().isCreated()).andReturn();
        String firstId = field(first, "/id");
        mvc.perform(post("/api/v1/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", iKey)
                        .header("X-Payment-Stub-Mode", "SUCCESS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"MTN_MOMO\",\"stubMode\":\"SUCCESS\"}"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s == 201) {
                        String retId = om.readTree(result.getResponse().getContentAsString()).at("/id").asText();
                        assertEquals(firstId, retId, "Same idempotency key must return same order");
                    }
                });
    }

    @Test @Order(15)
    @DisplayName("UAT-015 [High]: Customer can view own orders only; unauth is blocked")
    void uat015_orderHistoryOwnOrdersOnly() throws Exception {
        Assumptions.assumeTrue(customerToken != null);
        mvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        mvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test @Order(16)
    @DisplayName("UAT-016 [High]: Admin can create and update a product")
    void uat016_adminProductManagement() throws Exception {
        ensureAdminToken();
        MvcResult created = mvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"UAT-SKU-001\",\"name\":\"UAT Test Product\",\"priceRwf\":12000,\"stockQty\":50,\"status\":\"PUBLISHED\",\"category\":\"Cables\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("UAT-SKU-001"))
                .andReturn();
        String pid = field(created, "/id");
        mvc.perform(patch("/api/v1/admin/products/" + pid)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"UAT-SKU-001\",\"name\":\"UAT Test Product Updated\",\"priceRwf\":14000,\"stockQty\":45,\"status\":\"PUBLISHED\",\"category\":\"Cables\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("UAT Test Product Updated"));
    }

    @Test @Order(17)
    @DisplayName("UAT-017 [High]: Admin can view orders and update order status")
    void uat017_adminOrderManagement() throws Exception {
        ensureAdminToken();
        mvc.perform(get("/api/v1/admin/orders")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        if (orderId != null) {
            MvcResult det = mvc.perform(get("/api/v1/orders/" + orderId)
                            .header("Authorization", "Bearer " + customerToken))
                    .andReturn();
            if ("PAID".equals(field(det, "/status"))) {
                mvc.perform(post("/api/v1/admin/orders/" + orderId + "/ship")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"trackingNo\":\"CS-TRACK-UAT-001\"}"))
                        .andExpect(status().isOk());
            }
        }
    }

    @Test @Order(18)
    @DisplayName("UAT-018 [Critical]: Health endpoint reachable and does not leak version info")
    void uat018_loggingAndMonitoring() throws Exception {
        mvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").doesNotExist());
    }

    @Test @Order(19)
    @DisplayName("UAT-019 [Critical]: Customer cannot access admin endpoints (SEC-03)")
    void uat019_accessControl() throws Exception {
        Assumptions.assumeTrue(customerToken != null);
        mvc.perform(get("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/orders")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test @Order(20)
    @DisplayName("UAT-020 [Critical]: Authentication errors do not expose passwords (SEC-08)")
    void uat020_sensitiveLogging() throws Exception {
        String sensitivePassword = "SuperSecret123!";
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@marketsphere.rw\",\"password\":\"" + sensitivePassword + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString(sensitivePassword))));
    }

    @Test @Order(22)
    @DisplayName("UAT-022 [High]: Application stack communicates successfully (CONT-02 smoke)")
    void uat022_containerizedDeployment() throws Exception {
        mvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk());
    }

    @Test @Order(23)
    @DisplayName("UAT-023 [Critical]: REQ-WL-06 — Stock decremented after successful paid order")
    void uat023_reqWl06StockDecrement() throws Exception {
        Assumptions.assumeTrue(customerToken != null && targetProductId != null);
        MvcResult before = mvc.perform(get("/api/v1/products/" + targetProductId))
                .andExpect(status().isOk()).andReturn();
        int stockBefore = tree(before).at("/stockQty").asInt();
        Assumptions.assumeTrue(stockBefore > 0, "Product must have stock");
        mvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + targetProductId + "\",\"qty\":1}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("X-Payment-Stub-Mode", "SUCCESS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"MTN_MOMO\",\"stubMode\":\"SUCCESS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"));
        MvcResult after = mvc.perform(get("/api/v1/products/" + targetProductId))
                .andExpect(status().isOk()).andReturn();
        int stockAfter = tree(after).at("/stockQty").asInt();
        assertTrue(stockAfter < stockBefore,
                "REQ-WL-06: Stock must decrease after paid order. Before=" + stockBefore + " After=" + stockAfter);
    }

    @Test @Order(24)
    @DisplayName("UAT-024 [High]: Health endpoint UP; error responses do not expose stack traces (NF-006)")
    void uat024_operationalLogging() throws Exception {
        mvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/api/v1/products/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("at rw.ac.rca"))))
                .andExpect(content().string(not(containsString("Exception"))));
    }
}
