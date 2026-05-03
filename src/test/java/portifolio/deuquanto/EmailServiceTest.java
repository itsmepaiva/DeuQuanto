package portifolio.deuquanto;

import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import portifolio.deuquanto.service.EmailService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MimeMessage emptyMessage = new JavaMailSenderImpl().createMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(emptyMessage);
        ReflectionTestUtils.setField(emailService, "senderEmail", "test@deuquanto.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:5500");
    }

    @Test
    @DisplayName("Happy Path: Deve disparar o e-mail de Boas-vindas formatado corretamente")
    void shouldSendWelcomeEmailSuccessfully() throws Exception {
        // ARRANGE
        String destinatario = "joao@email.com";
        String nome = "João";

        // Simula o Thymeleaf transformando o contexto em um HTML pronto
        when(templateEngine.process(eq("welcome-email"), any(Context.class)))
                .thenReturn("<html><body>Bem-vindo João!</body></html>");

        // ACT
        // Nota: O @Async é ignorado no teste unitário, então o metodo roda instantaneamente
        emailService.sendWelcomeEmail(destinatario, nome);

        // ASSERT: O Capturador de Argumentos!
        // Nós pedimos para o Mockito "capturar" a MimeMessage que foi enviada para o mailSender
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        // Extrai a mensagem que o Mockito capturou
        MimeMessage capturedMessage = messageCaptor.getValue();

        // Verifica se o JavaMailSender tentou enviar para a pessoa certa com o título certo
        assertEquals("Bem-vindo(a) ao Deu Quanto? 🚀", capturedMessage.getSubject());
        assertEquals(destinatario, capturedMessage.getRecipients(Message.RecipientType.TO)[0].toString());
    }

    @Test
    @DisplayName("Happy Path: Deve disparar o e-mail de Recuperação de Senha com o Link")
    void shouldSendPasswordResetEmailSuccessfully() throws Exception {
        // ARRANGE
        String destinatario = "maria@email.com";
        String nome = "Maria";
        String tokenMocado = "abc-123-token-secreto";

        when(templateEngine.process(eq("reset-password-email"), any(Context.class)))
                .thenReturn("<html><body>Clique no link mágico!</body></html>");

        // ACT
        emailService.sendPasswordResetEmail(destinatario, nome, tokenMocado);

        // ASSERT
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage capturedMessage = messageCaptor.getValue();

        assertEquals("Recuperação de Senha - Deu Quanto? 🔒", capturedMessage.getSubject());
        assertEquals(destinatario, capturedMessage.getRecipients(Message.RecipientType.TO)[0].toString());
    }
}
