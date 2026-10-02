package com.rindev.chat;

import com.rindev.chat.entity.BlockedUser;
import com.rindev.chat.entity.Conversation;
import com.rindev.chat.entity.ConversationMember;
import com.rindev.chat.entity.Device;
import com.rindev.chat.entity.Message;
import com.rindev.chat.entity.MessageAttachment;
import com.rindev.chat.entity.MessageReaction;
import com.rindev.chat.entity.MessageReceipt;
import com.rindev.chat.entity.Notification;
import com.rindev.chat.entity.PinnedMessage;
import com.rindev.chat.entity.Report;
import com.rindev.chat.entity.User;
import com.rindev.chat.entity.UserSetting;
import com.rindev.chat.entity.RefreshToken;
import com.rindev.chat.repository.RefreshTokenRepository;
import com.rindev.chat.enums.AttachmentType;
import com.rindev.chat.enums.ConversationType;
import com.rindev.chat.enums.DevicePlatform;
import com.rindev.chat.enums.MemberRole;
import com.rindev.chat.enums.MessageType;
import com.rindev.chat.enums.NotificationType;
import com.rindev.chat.enums.PrivacyLevel;
import com.rindev.chat.enums.ReportReason;
import com.rindev.chat.enums.ReportStatus;
import com.rindev.chat.enums.Theme;
import com.rindev.chat.enums.UserStatus;
import com.rindev.chat.repository.BlockedUserRepository;
import com.rindev.chat.repository.ConversationMemberRepository;
import com.rindev.chat.repository.ConversationRepository;
import com.rindev.chat.repository.DeviceRepository;
import com.rindev.chat.repository.MessageAttachmentRepository;
import com.rindev.chat.repository.MessageReactionRepository;
import com.rindev.chat.repository.MessageReceiptRepository;
import com.rindev.chat.repository.MessageRepository;
import com.rindev.chat.repository.NotificationRepository;
import com.rindev.chat.repository.PinnedMessageRepository;
import com.rindev.chat.repository.ReportRepository;
import com.rindev.chat.repository.UserRepository;
import com.rindev.chat.repository.UserSettingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Read-only integration checks against the configured, existing MySQL database.
 * Application startup also runs Hibernate schema validation.
 */
@SpringBootTest(classes = ChatApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional(readOnly = true)
class ChatBackendApplicationTests {

    @DynamicPropertySource
    static void jwtTestProperties(DynamicPropertyRegistry registry) {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        String secret = Base64.getEncoder().encodeToString(key);
        registry.add("security.jwt.secret", () -> secret);
    }

    private static final Set<Class<?>> ENTITY_TYPES = Set.of(
            User.class, Conversation.class, ConversationMember.class, Message.class,
            MessageAttachment.class, MessageReceipt.class, MessageReaction.class,
            PinnedMessage.class, BlockedUser.class, Device.class, Notification.class,
            UserSetting.class, Report.class, RefreshToken.class);

    private static final List<Class<?>> REPOSITORY_TYPES = List.of(
            UserRepository.class, ConversationRepository.class, ConversationMemberRepository.class,
            MessageRepository.class, MessageAttachmentRepository.class, MessageReceiptRepository.class,
            MessageReactionRepository.class, PinnedMessageRepository.class, BlockedUserRepository.class,
            DeviceRepository.class, NotificationRepository.class, UserSettingRepository.class,
            ReportRepository.class, RefreshTokenRepository.class);

    private static final Map<String, Class<? extends Enum<?>>> ENUM_COLUMNS = Map.ofEntries(
            Map.entry("users.status", UserStatus.class),
            Map.entry("conversations.type", ConversationType.class),
            Map.entry("conversation_members.role", MemberRole.class),
            Map.entry("messages.type", MessageType.class),
            Map.entry("message_attachments.type", AttachmentType.class),
            Map.entry("devices.platform", DevicePlatform.class),
            Map.entry("notifications.type", NotificationType.class),
            Map.entry("user_settings.theme", Theme.class),
            Map.entry("user_settings.last_seen_privacy", PrivacyLevel.class),
            Map.entry("user_settings.profile_photo_privacy", PrivacyLevel.class),
            Map.entry("user_settings.group_add_privacy", PrivacyLevel.class),
            Map.entry("reports.reason", ReportReason.class),
            Map.entry("reports.status", ReportStatus.class));

    private static final Pattern ENUM_LITERAL = Pattern.compile("'((?:''|[^'])*)'");

    private final EntityManager entityManager;
    private final ApplicationContext applicationContext;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    ChatBackendApplicationTests(EntityManager entityManager,
                                ApplicationContext applicationContext,
                                JdbcTemplate jdbcTemplate) {
        this.entityManager = entityManager;
        this.applicationContext = applicationContext;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void discoversAllOriginalEntitiesAndRepositoriesPlusRefreshTokens() {
        var mappedTypes = entityManager.getMetamodel().getEntities().stream()
                .<Class<?>>map(EntityType::getJavaType)
                .toList();

        assertThat(mappedTypes).containsExactlyInAnyOrderElementsOf(ENTITY_TYPES);
        assertThat(applicationContext.getBeansOfType(Repository.class)).hasSize(REPOSITORY_TYPES.size());
        for (var repositoryType : REPOSITORY_TYPES) {
            assertThat(applicationContext.getBean(repositoryType)).isInstanceOf(Repository.class);
        }
    }

    @Test
    void selectsEveryMappedEntityFromExistingTables() {
        for (var entityType : ENTITY_TYPES) {
            var entityName = entityManager.getMetamodel().entity(entityType).getName();
            var rowCount = entityManager.createQuery("select e from " + entityName + " e", entityType)
                    .setMaxResults(1)
                    .getResultList()
                    .size();

            assertThat(rowCount).as("bounded selection of %s", entityName).isBetween(0, 1);
        }
    }

    @Test
    void enumDatabaseValuesMatchEveryNativeMysqlEnumColumn() throws ReflectiveOperationException {
        var databaseColumns = jdbcTemplate.query("""
                SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND DATA_TYPE = 'enum'
                """, (resultSet, rowNumber) -> new EnumColumn(
                resultSet.getString("TABLE_NAME") + "." + resultSet.getString("COLUMN_NAME"),
                resultSet.getString("COLUMN_TYPE")));

        assertThat(databaseColumns).extracting(EnumColumn::name)
                .containsExactlyInAnyOrderElementsOf(ENUM_COLUMNS.keySet());

        for (var column : databaseColumns) {
            var enumType = ENUM_COLUMNS.get(column.name());
            var getter = enumType.getMethod("getDatabaseValue");
            var javaValues = new ArrayList<String>();
            for (var constant : enumType.getEnumConstants()) {
                javaValues.add((String) getter.invoke(constant));
            }
            var databaseValues = ENUM_LITERAL.matcher(column.definition()).results()
                    .map(match -> match.group(1).replace("''", "'"))
                    .toList();

            assertThat(javaValues).as("database values for %s", column.name())
                    .containsExactlyInAnyOrderElementsOf(databaseValues);
        }
    }

    private record EnumColumn(String name, String definition) {
    }
}
