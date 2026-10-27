
package com.budzet.domain.budget.integration;

import com.budzet.domain.budget.entity.BudgetRequest;
import com.budzet.domain.budget.entity.BudgetRequestType;
import com.budzet.domain.budget.repository.BudgetRequestRepository;
import com.budzet.domain.budget.service.BudgetRequestService;
import com.budzet.domain.room.entity.Currency;
import com.budzet.domain.room.entity.Room;
import com.budzet.domain.room.entity.UserRoomConnection;
import com.budzet.domain.room.repository.RoomRepository;
import com.budzet.domain.room.repository.UserRoomConnectionRepository;
import com.budzet.domain.user.entity.User;
import com.budzet.domain.user.repository.UserRepository;
import com.budzet.global.exception.BusinessException;
import com.budzet.global.exception.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class BudgetRequestConcurrencyIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("budzet_request_concurrency_test")
                    .withUsername("budzet_test")
                    .withPassword("budzet_test");

    @DynamicPropertySource
    static void overrideDataSource(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                MYSQL::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                MYSQL::getUsername
        );
        registry.add(
                "spring.datasource.password",
                MYSQL::getPassword
        );
        registry.add(
                "spring.datasource.driver-class-name",
                MYSQL::getDriverClassName
        );
        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "create-drop"
        );
        registry.add(
                "spring.jpa.database-platform",
                () -> "org.hibernate.dialect.MySQLDialect"
        );
    }

    @Autowired
    private BudgetRequestService budgetRequestService;

    @Autowired
    private BudgetRequestRepository budgetRequestRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRoomConnectionRepository userRoomConnectionRepository;

    @Autowired
    private UserRepository userRepository;

    // =========================================================
    // 테스트 1: 동일 신청을 동시에 승인
    // =========================================================

    @Test
    @DisplayName("동일신청 동시승인 테스트")
    void concurrentApprovalOfSameRequest_allowsOnlyOneApproval()
            throws Exception {

        User owner = createUser();
        Room room = createRoom(10_000L);
        createOwnerMembership(owner, room);

        BudgetRequest request = createRequest(
                room,
                owner,
                3_000L
        );

        Long roomId = room.getId();
        Long ownerId = owner.getId();
        Long requestId = request.getId();

        List<Object> results = executeConcurrently(
                () -> approveAndCapture(
                        roomId,
                        owner,
                        requestId
                ),
                () -> approveAndCapture(
                        roomId,
                        owner,
                        requestId
                )
        );

        long successCount = results.stream()
                .filter(result -> result == null)
                .count();

        long failureCount = results.stream()
                .filter(result -> result instanceof RuntimeException)
                .count();

        assertEquals(
                1L,
                successCount,
                "동일한 예산 신청은 한 번만 승인되어야 합니다."
        );

        assertEquals(
                1L,
                failureCount,
                "중복 승인 시도 중 한 건은 실패해야 합니다."
        );

        RuntimeException failure = results.stream()
                .filter(RuntimeException.class::isInstance)
                .map(RuntimeException.class::cast)
                .findFirst()
                .orElseThrow();

        assertInstanceOf(
                BusinessException.class,
                failure,
                "중복 승인은 비즈니스 예외로 처리되어야 합니다."
        );

        BusinessException businessException =
                (BusinessException) failure;

        assertEquals(
                ErrorCode.NOT_APPROVABLE,
                businessException.getErrorCode()
        );

        BudgetRequest updatedRequest =
                budgetRequestRepository.findById(requestId)
                        .orElseThrow();

        assertEquals(
                BudgetRequestType.APPROVE,
                updatedRequest.getStatus()
        );

        Room updatedRoom =
                roomRepository.findById(roomId).orElseThrow();

        assertEquals(
                7_000L,
                updatedRoom.getAvailableBudget(),
                "동일 신청의 예산은 한 번만 차감되어야 합니다."
        );
    }

    // =========================================================
    // 테스트 2: 가용 예산이 부족한 상태에서 두 신청을 동시 승인
    // =========================================================

    @Test
    @DisplayName("서로 다른 신청 동시승인 테스트")
    void concurrentApprovalOfDifferentRequests_doesNotExceedBudget()
            throws Exception {

        User owner = createUser();
        Room room = createRoom(10_000L);
        createOwnerMembership(owner, room);

        BudgetRequest firstRequest = createRequest(
                room,
                owner,
                6_000L
        );

        BudgetRequest secondRequest = createRequest(
                room,
                owner,
                6_000L
        );

        Long roomId = room.getId();

        List<Object> results = executeConcurrently(
                () -> approveAndCapture(
                        roomId,
                        owner,
                        firstRequest.getId()
                ),
                () -> approveAndCapture(
                        roomId,
                        owner,
                        secondRequest.getId()
                )
        );

        long successCount = results.stream()
                .filter(result -> result == null)
                .count();

        assertEquals(
                1L,
                successCount,
                "가용 예산 10,000원으로 6,000원 신청 두 건을 "
                        + "모두 승인할 수 없어야 합니다."
        );

        Room updatedRoom =
                roomRepository.findById(roomId).orElseThrow();

        assertTrue(
                updatedRoom.getAvailableBudget() >= 0,
                "가용 예산은 음수가 될 수 없습니다."
        );

        assertEquals(
                4_000L,
                updatedRoom.getAvailableBudget(),
                "한 건만 승인되면 가용 예산은 4,000원이어야 합니다."
        );

        BudgetRequest updatedFirst =
                budgetRequestRepository.findById(firstRequest.getId())
                        .orElseThrow();

        BudgetRequest updatedSecond =
                budgetRequestRepository.findById(secondRequest.getId())
                        .orElseThrow();

        long approvedCount = List.of(
                        updatedFirst,
                        updatedSecond
                )
                .stream()
                .filter(request ->
                        request.getStatus() == BudgetRequestType.APPROVE)
                .count();

        assertEquals(
                1L,
                approvedCount,
                "두 신청 중 하나만 승인 상태여야 합니다."
        );
    }

    // =========================================================
    // 테스트 데이터
    // =========================================================

    private User createUser() {
        String email =
                "c-" + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 16)
                        + "@test.com";

        User user = new User(
                email,
                "test-password",
                "동시성 테스트 사용자"
        );

        return userRepository.saveAndFlush(user);
    }

    private Room createRoom(long totalBudget) {
        Room room = Room.create(
                "예산 신청 동시성 테스트",
                totalBudget,
                Currency.KRW
        );

        return roomRepository.saveAndFlush(room);
    }

    private void createOwnerMembership(
            User user,
            Room room
    ) {
        UserRoomConnection connection =
                UserRoomConnection.createOwner(user, room);

        userRoomConnectionRepository.saveAndFlush(connection);
    }

    private BudgetRequest createRequest(
            Room room,
            User user,
            long requestedAmount
    ) {
        BudgetRequest request = new BudgetRequest(
                room,
                user,
                "동시성 테스트 신청",
                requestedAmount
        );

        return budgetRequestRepository.saveAndFlush(request);
    }

    // =========================================================
    // 동시 실행
    // =========================================================

    private List<Object> executeConcurrently(
            Callable<Object> first,
            Callable<Object> second
    ) throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Object> firstFuture = executor.submit(() -> {
                ready.countDown();

                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "첫 번째 작업 시작 대기 시간 초과"
                    );
                }

                return first.call();
            });

            Future<Object> secondFuture = executor.submit(() -> {
                ready.countDown();

                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "두 번째 작업 시작 대기 시간 초과"
                    );
                }

                return second.call();
            });

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "두 작업이 실행 준비를 마치지 못했습니다."
            );

            start.countDown();

            Object firstResult =
                    firstFuture.get(30, TimeUnit.SECONDS);

            Object secondResult =
                    secondFuture.get(30, TimeUnit.SECONDS);

            return java.util.Arrays.asList(
                    firstResult,
                    secondResult
            );
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Object approveAndCapture(
            Long roomId,
            User owner,
            Long requestId
    ) {
        try {
            budgetRequestService.approveBudgetRequest(
                    roomId,
                    owner,
                    requestId
            );

            return null;
        } catch (RuntimeException exception) {
            return exception;
        }
    }
}
