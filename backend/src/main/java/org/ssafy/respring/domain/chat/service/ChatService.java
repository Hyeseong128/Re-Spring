package org.ssafy.respring.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.ssafy.respring.domain.challenge.repository.ChallengeRepository;
import org.ssafy.respring.domain.challenge.vo.Challenge;
import org.ssafy.respring.domain.chat.dto.request.ChatRoomRequest;
import org.ssafy.respring.domain.chat.dto.response.ChatMessageResponse;
import org.ssafy.respring.domain.chat.dto.response.ChatRoomResponse;
import org.ssafy.respring.domain.chat.repository.ChatRoomUserRepository;
import org.ssafy.respring.domain.chat.repository.MongoChatMessageRepository;
import org.ssafy.respring.domain.chat.repository.ChatRoomRepository;
import org.ssafy.respring.domain.chat.vo.ChatMessage;
import org.ssafy.respring.domain.chat.vo.ChatRoom;
import org.ssafy.respring.domain.chat.vo.ChatRoomUser;
import org.ssafy.respring.domain.user.repository.UserRepository;
import org.ssafy.respring.domain.user.vo.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MongoChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChallengeRepository challengeRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    private final String LAST_SEEN_KEY = "last_seen:";
    private final String LAST_SEEN_HUMAN_KEY = "last_seen_human:";
    private final String ROOM_USER_STATUS_KEY = "room_user_status:"; // 현재 입장 여부 키


    private final Path fileStoragePath = Paths.get("uploads");

    public ChatRoom createRoom(ChatRoomRequest request) {
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new IllegalArgumentException("유효한 유저 ID 리스트를 제공해야 합니다.");
        }

        List<User> users = request.getUserIds().stream()
                .map(userId -> userRepository.findById(UUID.fromString(userId))
                        .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId)))
                .collect(Collectors.toList());

        ChatRoom.ChatRoomBuilder chatRoomBuilder = ChatRoom.builder()
                .name(request.getName())
                .isOpenChat(request.isOpenChat())
                .isMentoring(request.isMentoring());

        if (request.isMentoring() && request.getMentorName() != null) {
            User mentor = userRepository.findByUserNickname(request.getMentorName())
                    .orElseThrow(() -> new IllegalArgumentException("❌ 멘토(강연자)를 찾을 수 없음"));
            chatRoomBuilder.mentor(mentor);
        }

        ChatRoom chatRoom = chatRoomBuilder.build();
        chatRoomRepository.save(chatRoom);

        // ✅ 채팅방-유저 관계 저장
        users.forEach(user -> {
            chatRoomUserRepository.save(ChatRoomUser.builder()
                    .chatRoom(chatRoom)
                    .user(user)
                    .isActive(true)  // 새로 생성된 방에서는 기본적으로 활성화
                    .build());
        });

        return chatRoom;
    }

    public List<ChatRoom> getAllRooms() {
        return chatRoomRepository.findAll();
    }

    public ChatRoom findRoomByName(String name) {
        return chatRoomRepository.findByName(name)
                .orElse(null); // 방이 없으면 null 반환
    }

    public ChatRoom addUserToRoom(Long roomId, UUID userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("❌ 채팅방이 존재하지 않습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("❌ 사용자를 찾을 수 없습니다. ID: " + userId));

        boolean alreadyJoined = chatRoom.getChatRoomUsers().stream()
                .anyMatch(chatRoomUser -> chatRoomUser.getUser().getId().equals(userId));

        if (alreadyJoined) {
            System.out.println("⚠️ 이미 채팅방에 참가한 유저: " + userId);
            return chatRoom;
        }

        ChatRoomUser chatRoomUser = ChatRoomUser.builder()
                .chatRoom(chatRoom)
                .user(user)
                .isActive(true)
                .build();

        chatRoomUserRepository.save(chatRoomUser);
        System.out.println("✅ 채팅방에 새 유저 추가: " + userId);

        return chatRoom;
    }


    public Optional<ChatRoom> findById(Long roomId) {
        return chatRoomRepository.findById(roomId);
    }

    public void saveChatRoom(ChatRoom chatRoom) {
        chatRoomRepository.save(chatRoom);
    }

    public ChatMessageResponse saveMessage(Long roomId, UUID userId, String receiver, String content) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("❌ 채팅방을 찾을 수 없습니다."));

        // 🔹 1:1 채팅방인지 확인 (isOpenChat = false이면 1:1 채팅방)
        if (!chatRoom.isOpenChat()) {
            System.out.println("✅ 1:1 채팅방 - 챌린지 조회 없이 메시지 저장");
        } else {
            // ✅ 오픈 채팅방일 경우에만 챌린지 조회
            Challenge challenge = challengeRepository.findByChatRoomId(chatRoom.getId())
                    .orElseThrow(() -> new IllegalArgumentException("❌ 해당 챌린지와 연결된 채팅방을 찾을 수 없습니다."));

            // ✅ 챌린지가 종료되었는지 확인
            if (challenge.getEndDate().isBefore(LocalDateTime.now())) {
                throw new IllegalStateException("❌ 챌린지가 종료되어 채팅을 보낼 수 없습니다.");
            }
        }

        // ✅ 메시지 저장
        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .sender(userId.toString())
                .receiver(receiver)
                .content(content)
                .timestamp(LocalDateTime.now())
                .read(false)
                .chatRoomId(roomId) // MySQL의 ChatRoom과 연결
                .build());

        return ChatMessageResponse.builder()
                .sender(message.getSender())
                .receiver(message.getReceiver())
                .content(message.getContent())
                .read(message.isRead())
                .timestamp(message.getTimestamp())
                .build();
    }

    public ChatMessage saveFileMessage(Long roomId, UUID userId, MultipartFile file) throws IOException {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found with id: " + roomId));

        // 🔹 User 엔티티 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("❌ 사용자를 찾을 수 없습니다. ID: " + userId));

        if (!Files.exists(fileStoragePath)) {
            Files.createDirectories(fileStoragePath);
        }

        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        Path targetPath = fileStoragePath.resolve(fileName);
        Files.copy(file.getInputStream(), targetPath);

        return chatMessageRepository.save(ChatMessage.builder()
                .sender(user.getUserNickname())
                .fileUrl(targetPath.toString())
                .timestamp(LocalDateTime.now())
                .read(false)
                .chatRoomId(chatRoom.getId())
                .build());
    }

    public List<ChatMessage> getMessages(Long roomId) {
        return chatMessageRepository.findByChatRoomId(roomId);
    }

    public List<ChatMessage> searchMessages(Long roomId, String keyword) {
        return chatMessageRepository.findByContentContainingAndChatRoomId(keyword, roomId);
    }

    public void deleteMessage(String messageId, UUID userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        if (!message.getSender().equals(userId.toString())) {
            throw new IllegalArgumentException("Cannot delete another user's message");
        }

        chatMessageRepository.delete(message);
    }

    public void markMessageAsRead(String messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        message.setRead(true);
        chatMessageRepository.save(message);
    }





    public List<ChatRoom> getUserRooms(UUID userId) {
        return chatRoomRepository.findRoomsByUserId(userId);
    }

//    public void removeUserFromRoom(ChatRoom chatRoom, UUID userId) {
//        List<User> updatedUsers = chatRoom.getUsers().stream()
//                .filter(user -> !user.getId().equals(userId)) // 유저 제외
//                .collect(Collectors.toList());
//
//        chatRoom.setUsers(updatedUsers); // 업데이트된 유저 목록 설정
//        chatRoomRepository.save(chatRoom);
//    }

    public ChatRoomResponse joinRoom(Long roomId, UUID userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("❌ 채팅방이 존재하지 않습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("❌ 사용자를 찾을 수 없습니다. ID: " + userId));

        // ✅ 멘토링 방인지 확인
        if (chatRoom.isMentoring()) {
            System.out.println("✅ 멘토링 방 참가 요청: " + chatRoom.getName());
        }

        boolean alreadyJoined = chatRoom.getChatRoomUsers().stream()
                .anyMatch(chatRoomUser -> chatRoomUser.getUser().getId().equals(userId));

        if (alreadyJoined) {
            System.out.println("⚠️ 이미 채팅방에 참가한 유저: " + userId);
            return ChatRoomResponse.from(chatRoom);
        }

        // ✅ 채팅방-유저 관계 저장
        ChatRoomUser chatRoomUser = ChatRoomUser.builder()
                .chatRoom(chatRoom)
                .user(user)
                .isActive(true)
                .build();

        chatRoomUserRepository.save(chatRoomUser);
        System.out.println("✅ 채팅방 참가 성공: " + userId);

        return ChatRoomResponse.from(chatRoom);
    }


    @Transactional
    public void leaveRoom(Long roomId, UUID userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("❌ 채팅방이 존재하지 않습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("❌ 사용자를 찾을 수 없습니다. "));

        Optional<ChatRoomUser> chatRoomUserOpt = chatRoomUserRepository.findByChatRoomAndUser(chatRoom, user);

        if (chatRoomUserOpt.isEmpty()) {
            throw new IllegalArgumentException("❌ 해당 유저가 채팅방에 없음");
        }

        ChatRoomUser chatRoomUser = chatRoomUserOpt.get();


        if (chatRoom.isMentoring()) {
            if(chatRoom.getMentor().getId().equals(user.getId())){
                chatRoomRepository.delete(chatRoom);
                System.out.println("🗑️ 멘토링 방 삭제됨: " + roomId);
            } else{
                // ✅ 멘토링 방이면 유저 삭제
                chatRoomUserRepository.deleteByRoomIdAndUserId(roomId, user.getId());
                System.out.println("🗑️ 멘토링 방 - 사용자 삭제됨: " + user.getUserNickname());
            }

            // ✅ 방에 남은 유저가 없으면 삭제
            if (!chatRoomUserRepository.existsByChatRoom(chatRoom)) {
                chatRoomRepository.delete(chatRoom);
                System.out.println("🔥 멘토링 방이 비어 삭제됨: " + roomId);
            }
        } else {
            // ✅ 일반 채팅방이면 `isActive`를 false로 변경
            chatRoomUser.setActive(false);
            chatRoomUserRepository.save(chatRoomUser);
            System.out.println("🚪 일반 채팅방 - isActive = false 설정됨: " + user.getUserNickname());
        }

        // ✅ WebSocket을 통해 나간 사실을 알림
        messagingTemplate.convertAndSend(
                "/topic/roomUpdates/" + roomId,
                "User " + user.getUserNickname() + " has left the room."
        );

        // ✅ 1:1 채팅방에서 모든 유저가 나가면 방 삭제
        if (!chatRoom.isOpenChat()) {
            boolean hasActiveUsers = chatRoom.getUsers().stream().anyMatch(u ->
                    chatRoomUserRepository.findByChatRoomAndUser(chatRoom, u)
                            .map(ChatRoomUser::isActive)
                            .orElse(false)
            );

            if (!hasActiveUsers) {
                chatRoomRepository.delete(chatRoom);
                System.out.println("✅ 1:1 채팅방 삭제됨: " + roomId);
            }
        }
    }




    public Optional<ChatRoom> findByName(String chatRoomUUID) {
        return chatRoomRepository.findByName(chatRoomUUID);
    }

    public void deleteRoom(Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다."));
        chatRoomRepository.delete(chatRoom);
    }

    @Transactional
    public ChatRoom getOrJoinPrivateRoom(String user1Name, String user2Name) {
        User user1 = userRepository.findByUserNickname(user1Name)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + user1Name));
        User user2 = userRepository.findByUserNickname(user2Name)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + user2Name));

        // ✅ 기존 1:1 채팅방 조회
        Optional<ChatRoom> existingRoomOpt = chatRoomRepository.findExactPrivateRoom(user1, user2);

        if (existingRoomOpt.isPresent()) {
            ChatRoom existingRoom = existingRoomOpt.get();
            System.out.println("✅ 기존 1:1 채팅방 재사용: " + existingRoom.getId());

            // ✅ 기존 사용자의 isActive를 다시 활성화 (isActive = true)
            existingRoom.getChatRoomUsers().forEach(chatRoomUser -> {
                if (chatRoomUser.getUser().equals(user1) || chatRoomUser.getUser().equals(user2)) {
                    chatRoomUser.setActive(true);
                    chatRoomUserRepository.save(chatRoomUser);
                }
            });

            return existingRoom;
        }

        // ✅ 기존 방이 없으면 새로운 1:1 채팅방 생성
        System.out.println("✅ 새로운 1:1 채팅방 생성");
        ChatRoom chatRoom = ChatRoom.builder()
                .name("Private Chat: " + user1.getUserNickname() + " & " + user2.getUserNickname())
                .isOpenChat(false)
                .build();
        chatRoomRepository.save(chatRoom);

        chatRoomUserRepository.save(ChatRoomUser.builder()
                .chatRoom(chatRoom)
                .user(user1)
                .isActive(true)
                .build());

        chatRoomUserRepository.save(ChatRoomUser.builder()
                .chatRoom(chatRoom)
                .user(user2)
                .isActive(true)
                .build());

        return chatRoom;
    }

    // ✅ 멘토링(강연자) 방 생성
    public ChatRoom createMentoringRoom(String name, UUID mentorId) {
        User mentor = userRepository.findById(mentorId)
                .orElseThrow(() -> new IllegalArgumentException("❌ 멘토(강연자)를 찾을 수 없음"));

        ChatRoom chatRoom = ChatRoom.builder()
                .name(name)
                .isMentoring(true)
                .mentor(mentor)
                .build();

        chatRoomRepository.save(chatRoom);

        // ✅ 방 생성 시, 강연자(멘토)를 자동 추가
        ChatRoomUser chatRoomUser = ChatRoomUser.builder()
                .chatRoom(chatRoom)
                .user(mentor)
                .isActive(true)
                .build();

        chatRoomUserRepository.save(chatRoomUser);

        return chatRoom;
    }

    // ✅ 특정 방 정보 조회 (멘토링 여부 포함)
    public ChatRoom getRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("❌ 채팅방을 찾을 수 없음"));
    }

    // ✅ 멘토링(발표자) 방 조회
    public List<ChatRoom> getMentoringRooms() {
        return chatRoomRepository.findByIsMentoring(true);
    }

    // ✅ 마지막 접속 시간 저장 (Timestamp + Human-readable format)
    public void saveLastSeenTime(Long roomId, UUID userId) {
        String key = LAST_SEEN_KEY + roomId + ":" + userId;
        String humanKey = LAST_SEEN_HUMAN_KEY + roomId + ":" + userId;

        long timestamp = System.currentTimeMillis();
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));

        redisTemplate.opsForValue().set(key, String.valueOf(timestamp), 24, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(humanKey, formattedDate, 24, TimeUnit.HOURS);

        System.out.println("✅ Redis 저장됨: " + key + " → " + timestamp);
        System.out.println("✅ Redis 저장됨: " + humanKey + " → " + formattedDate);
    }

    // ✅ 마지막 접속 시간 조회 (Timestamp)
    public Long getLastSeenTime(Long roomId, UUID userId) {
        String key = LAST_SEEN_KEY + roomId + ":" + userId;
        String timestamp = redisTemplate.opsForValue().get(key);
        return timestamp != null ? Long.parseLong(timestamp) : 0L;
    }

    // ✅ 마지막 접속 시간 조회 (Human-readable Format)
    public String getLastSeenTimeHuman(Long roomId, UUID userId) {
        String key = LAST_SEEN_HUMAN_KEY + roomId + ":" + userId;
        return redisTemplate.opsForValue().get(key);
    }

    // ✅ 사용자가 채팅방에 입장할 때 호출
    public void markUserAsInRoom(Long roomId, UUID userId) {
        String key = "room_user_status:" + roomId + ":" + userId;
        redisTemplate.opsForValue().set(key, "true", 24, TimeUnit.HOURS);  // 24시간 유지
        System.out.println("✅ Redis 저장됨: " + key);
    }


    // ✅ 사용자가 채팅방에서 나갈 때 호출
    public void markUserAsLeftRoom(Long roomId, UUID userId) {
        String key = ROOM_USER_STATUS_KEY + roomId + ":" + userId;
        redisTemplate.delete(key);
        System.out.println("🚪 사용자 방에서 나감: " + key);
    }

    // ✅ 사용자가 현재 방에 있는지 확인
    public boolean isUserCurrentlyInRoom(Long roomId, UUID userId) {
        String key = ROOM_USER_STATUS_KEY + roomId + ":" + userId;
        return redisTemplate.hasKey(key);
    }

}
