package jp.vemi.mirel.apps.mira.infrastructure.ai;

import org.springframework.stereotype.Component;
import java.util.Optional;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import lombok.extern.slf4j.Slf4j;

/**
 * Token Counter.
 * <p>
 * Uses JTokkit for accurate token counting.
 * </p>
 */
@Slf4j
@Component
public class TokenCounter {

    private final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
    private final Encoding defaultEncoding = registry.getEncoding(EncodingType.CL100K_BASE);

    /**
     * Count tokens in text.
     * 
     * @param text
     *            Text content
     * @param model
     *            Model name (optional, defaults to CL100K_BASE if unknown)
     * @return Token count
     */
    public int count(String text, String model) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        try {
            Encoding encoding = defaultEncoding;
            if (model != null) {
                Optional<Encoding> encOpt = registry.getEncodingForModel(model);
                if (encOpt.isPresent()) {
                    encoding = encOpt.get();
                }
            }
            return encoding.countTokens(text);
        } catch (Exception e) {
            log.warn("Token counting failed, using fallback estimation. text length: {}", text.length(), e);
            // Fallback: length / 2
            return text.length() / 2;
        }
    }
}
