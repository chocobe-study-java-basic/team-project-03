package com.budzet.global.config

import com.budzet.domain.budget.repository.BudgetChangeRepository
import com.budzet.domain.budget.repository.BudgetRequestRepository
import com.budzet.domain.invite.repository.InviteRepository
import com.budzet.domain.room.dto.RoomCreateRequest
import com.budzet.domain.room.entity.Currency
import com.budzet.domain.room.entity.Room
import com.budzet.domain.room.entity.UserRoomConnection
import com.budzet.domain.room.repository.RoomRepository
import com.budzet.domain.room.repository.UserRoomConnectionRepository
import com.budzet.domain.room.service.RoomService
import com.budzet.domain.user.entity.User
import com.budzet.domain.user.repository.UserRepository
import com.budzet.domain.user.service.UserService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Profile("!prod & !test")
class TestBedInitializer(
    private val userService: UserService,
    private val roomService: RoomService,
    private val userRepository: UserRepository,
    private val roomRepository: RoomRepository,
    private val userRoomConnectionRepository: UserRoomConnectionRepository,
    private val budgetRequestRepository: BudgetRequestRepository,
    private val budgetChangeRepository: BudgetChangeRepository,
    private val inviteRepository: InviteRepository,
) {
    @Transactional
    fun seed() {
        if (hasExistingData()) return
        val users = createUsers()
        val rooms = createRooms(users)
        createMemberConnections(users, rooms)
    }

    private fun hasExistingData(): Boolean =
        userRepository.count() > 0 || roomRepository.count() > 0 ||
            userRoomConnectionRepository.count() > 0 || budgetRequestRepository.count() > 0 ||
            budgetChangeRepository.count() > 0 || inviteRepository.count() > 0

    private fun createUsers(): Map<Int, User> = (1..21).associateWith { number ->
        userService.join("user$number@example.com", TEST_PASSWORD, "테스트 사용자 $number")
    }

    private fun createRooms(users: Map<Int, User>): Map<Int, Room> = mapOf(
        1 to createRoom(users.getValue(1), "프로젝트 회식", 500_000L, Currency.KRW),
        2 to createRoom(users.getValue(11), "일본 여행", 100_000L, Currency.JPY),
        3 to createRoom(users.getValue(16), "미국 여행", 10_000L, Currency.USD),
    )

    private fun createRoom(owner: User, name: String, totalBudget: Long, currency: Currency): Room {
        val response = roomService.createRoom(
            requireNotNull(owner.id), RoomCreateRequest(name, totalBudget, currency),
        )
        return roomRepository.getReferenceById(requireNotNull(response.id))
    }

    private fun createMemberConnections(users: Map<Int, User>, rooms: Map<Int, Room>) {
        val connections = mutableListOf<UserRoomConnection>()
        addMembers(connections, users, rooms.getValue(1), 2, 10)
        addMembers(connections, users, rooms.getValue(2), 12, 15)
        addMembers(connections, users, rooms.getValue(3), 17, 18)
        userRoomConnectionRepository.saveAll(connections)
    }

    private fun addMembers(
        connections: MutableList<UserRoomConnection>,
        users: Map<Int, User>,
        room: Room,
        from: Int,
        to: Int,
    ) {
        for (number in from..to) {
            connections.add(UserRoomConnection.createMember(users.getValue(number), room))
        }
    }

    companion object {
        private const val TEST_PASSWORD = "12341234"
    }
}
