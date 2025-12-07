package jp.vemi.mirel.apps.mira.domain.service;

import java.io.StringWriter;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraConversation;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraMessage;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraConversationRepository;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraMessageRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiraDataExportService {

    private final MiraConversationRepository conversationRepository;
    private final MiraMessageRepository messageRepository;

    @Transactional(readOnly = true)
    public String exportConversationsToCsv(String tenantId) {
        try {
            // Limit to 100 recent conversations for MVP to avoid timeouts
            Pageable limit = PageRequest.of(0, 100, Sort.by("lastActivityAt").descending());

            Page<MiraConversation> conversationsPage = conversationRepository
                    .findByTenantIdOrderByLastActivityAtDesc(tenantId, limit);

            List<ExportRow> rows = new ArrayList<>();
            DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

            for (MiraConversation conv : conversationsPage.getContent()) {
                List<MiraMessage> messages = messageRepository
                        .findByConversationIdOrderByCreatedAtAsc(conv.getId());

                for (MiraMessage msg : messages) {
                    rows.add(ExportRow.builder()
                            .conversationId(conv.getId())
                            .date(msg.getCreatedAt() != null ? msg.getCreatedAt().format(dtf) : "")
                            .mode(conv.getMode().name())
                            .userId(conv.getUserId())
                            .sender(msg.getSenderType().name())
                            .content(msg.getContent())
                            .build());
                }
            }

            CsvMapper mapper = new CsvMapper();
            // Need to configure mapper to handle Japanese text correctly if writing to
            // file,
            // but for StringWriter it's just characters. Controller handles encoding.
            CsvSchema schema = mapper.schemaFor(ExportRow.class).withHeader();

            StringWriter writer = new StringWriter();
            mapper.writer(schema).writeValue(writer, rows);

            return writer.toString();
        } catch (Exception e) {
            log.error("CSV Export failed", e);
            throw new RuntimeException("Export failed", e);
        }
    }

    @Data
    @Builder
    @JsonPropertyOrder({ "conversationId", "date", "mode", "userId", "sender", "content" })
    public static class ExportRow {
        @JsonProperty("Conversation ID")
        private String conversationId;

        @JsonProperty("Date")
        private String date;

        @JsonProperty("Mode")
        private String mode;

        @JsonProperty("User ID")
        private String userId;

        @JsonProperty("Sender")
        private String sender;

        @JsonProperty("Content")
        private String content;
    }
}
