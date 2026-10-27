package com.budzet.global.config;

import com.budzet.domain.budget.repository.BudgetChangeRepository;
import com.budzet.domain.budget.repository.BudgetRequestRepository;
import com.budzet.domain.invite.repository.InviteRepository;
import com.budzet.domain.room.dto.RoomCreateRequest;
import com.budzet.domain.room.dto.RoomCreateResponse;
import com.budzet.domain.room.entity.Currency;
import com.budzet.domain.room.entity.Room;
import com.budzet.domain.room.entity.UserRoomConnection;
import com.budzet.domain.room.repository.RoomRepository;
import com.budzet.domain.room.repository.UserRoomConnectionRepository;
import com.budzet.domain.room.service.RoomService;
import com.budzet.domain.user.entity.User;
import com.budzet.domain.user.repository.UserRepository;
import com.budzet.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Profile("!prod & !test")
@RequiredArgsConstructor
public class TestBedInitializer {

    private static final String TEST_PASSWORD = "12341234";

    private final UserService userService;
    private final RoomService roomService;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final UserRoomConnectionRepository userRoomConnectionRepository;
    private final BudgetRequestRepository budgetRequestRepository;
    private final BudgetChangeRepository budgetChangeRepository;
    private final InviteRepository inviteRepository;

    @Transactional
    public void seed() {
        if (hasExistingData()) {
            return;
        }

        Map<Integer, User> users = createUsers();
        Map<Integer, Room> rooms = createRooms(users);
        createMemberConnections(users, rooms);
    }

    private boolean hasExistingData() {
        return userRepository.count() > 0
                || roomRepository.count() > 0
                || userRoomConnectionRepository.count() > 0
                || budgetRequestRepository.count() > 0
                || budgetChangeRepository.count() > 0
                || inviteRepository.count() > 0;
    }

    private Map<Integer, User> createUsers() {
        Map<Integer, User> users = new HashMap<>();

        for (int number = 1; number <= 21; number++) {
            User user = userService.join(
                    "user" + number + "@example.com",
                    TEST_PASSWORD,
                    "테스트 사용자 " + number
            );
            users.put(number, user);
        }

        return users;
    }

    private Map<Integer, Room> createRooms(Map<Integer, User> users) {
        Map<Integer, Room> rooms = new HashMap<>();

        rooms.put(1, createRoom(users.get(1), "프로젝트 회식", 500_000L, Currency.KRW));
        rooms.put(2, createRoom(users.get(11), "일본 여행", 100_000L, Currency.JPY));
        rooms.put(3, createRoom(users.get(16), "미국 여행", 10_000L, Currency.USD));

        return rooms;
    }

    private Room createRoom(User owner, String name, Long totalBudget, Currency currency) {
        RoomCreateResponse response = roomService.createRoom(
                owner.getId(),
                new RoomCreateRequest(name, totalBudget, currency)
        );

        return roomRepository.getReferenceById(response.getId());
    }

    private void createMemberConnections(Map<Integer, User> users, Map<Integer, Room> rooms) {
        List<UserRoomConnection> connections = new ArrayList<>();
        addMembers(connections, users, rooms.get(1), 2, 10);
        addMembers(connections, users, rooms.get(2), 12, 15);
        addMembers(connections, users, rooms.get(3), 17, 18);

        userRoomConnectionRepository.saveAll(connections);
    }

    private void addMembers(
            List<UserRoomConnection> connections,
            Map<Integer, User> users,
            Room room,
            int from,
            int to
    ) {
        for (int number = from; number <= to; number++) {
            connections.add(UserRoomConnection.createMember(users.get(number), room));
        }
    }
}
