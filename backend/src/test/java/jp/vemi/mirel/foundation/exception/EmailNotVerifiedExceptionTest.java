package jp.vemi.mirel.foundation.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmailNotVerifiedException テスト")
class EmailNotVerifiedExceptionTest {

    @Test
    @DisplayName("メールアドレスを正しく保持する")
    void shouldStoreEmailAddress() {
        String email = "unverified@example.com";
        String message = "メールアドレスが未検証です";
        EmailNotVerifiedException exception = new EmailNotVerifiedException(message, email);

        assertThat(exception.getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("適切なエラーメッセージを返す")
    void shouldReturnProperErrorMessage() {
        String email = "test@example.com";
        String message = "メールアドレスが未検証です: " + email;
        EmailNotVerifiedException exception = new EmailNotVerifiedException(message, email);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getMessage()).contains(email);
        assertThat(exception.getMessage()).contains("未検証");
    }
}
