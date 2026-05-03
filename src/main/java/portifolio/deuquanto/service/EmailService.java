package portifolio.deuquanto.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import portifolio.deuquanto.configuration.DataMasker;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${app.frontend.url:http://localhost:5500}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendWelcomeEmail(String to, String userName) {
        log.info("Iniciando montagem do e-mail de boas-vindas para: {}", DataMasker.maskEmail(to));

        Context context = new Context();
        context.setVariable("name", userName);
        context.setVariable("loginUrl", frontendUrl + "/login.html");

        String htmlBody = templateEngine.process("welcome-email", context);

        sendHtmlEmail(to, "Bem-vindo(a) ao Deu Quanto? 🚀", htmlBody);
    }

    @Async
    public void sendPasswordResetEmail(String to, String userName, String token) {
        log.info("Iniciando montagem do e-mail de recuperação para: {}", DataMasker.maskEmail(to));

        Context context = new Context();
        context.setVariable("name", userName);

        String resetLink = frontendUrl + "/reset-password.html?token=" + token;
        context.setVariable("resetLink", resetLink);

        String htmlBody = templateEngine.process("reset-password-email", context);

        sendHtmlEmail(to, "Recuperação de Senha - Deu Quanto? 🔒", htmlBody);
    }

    // Metodo auxiliar genérico para disparar o e-mail HTML
    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("E-mail [{}] enviado com sucesso para: {}", subject, DataMasker.maskEmail(to));

        } catch (MessagingException e) {
            log.error("FALHA CRÍTICA ao enviar e-mail [{}] para {}. Motivo: {}", subject, DataMasker.maskEmail(to), e.getMessage(), e);
            throw new RuntimeException("Erro ao processar o envio de e-mail");
        }
    }
}