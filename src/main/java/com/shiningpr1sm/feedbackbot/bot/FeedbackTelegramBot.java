package com.shiningpr1sm.feedbackbot.bot;

import com.shiningpr1sm.feedbackbot.model.EmployeeRole;
import com.shiningpr1sm.feedbackbot.model.Feedback;
import com.shiningpr1sm.feedbackbot.model.UserSession;
import com.shiningpr1sm.feedbackbot.model.UserState;
import com.shiningpr1sm.feedbackbot.repository.FeedbackRepository;
import com.shiningpr1sm.feedbackbot.repository.UserSessionRepository;
import com.shiningpr1sm.feedbackbot.service.GoogleSheetsService;
import com.shiningpr1sm.feedbackbot.service.OpenAIService;
import com.shiningpr1sm.feedbackbot.service.OpenAIService.AnalysisResult;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class FeedbackTelegramBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final UserSessionRepository userSessionRepository;
    private final FeedbackRepository feedbackRepository;
    private final OpenAIService openAIService;
    private final GoogleSheetsService googleSheetsService;

    public FeedbackTelegramBot(@Value("${telegram.bot.token}") String botToken,
                               @Value("${telegram.bot.username}") String botUsername,
                               UserSessionRepository userSessionRepository,
                               FeedbackRepository feedbackRepository,
                               OpenAIService openAIService,
                               GoogleSheetsService googleSheetsService) {
        super(botToken);
        this.botUsername = botUsername;
        this.userSessionRepository = userSessionRepository;
        this.feedbackRepository = feedbackRepository;
        this.openAIService = openAIService;
        this.googleSheetsService = googleSheetsService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();

            Optional<UserSession> optionalUserSession = userSessionRepository.findByChatId(chatId);
            UserSession userSession = optionalUserSession.orElseGet(() -> new UserSession(chatId, UserState.START));

            if (messageText.equals("/start")) {
                handleStartCommand(chatId, userSession);
            } else {
                handleMessageByState(chatId, messageText, userSession);
            }
        }
    }

    private void handleStartCommand(Long chatId, UserSession userSession) throws TelegramApiException {
        userSession.setState(UserState.AWAITING_ROLE);
        userSessionRepository.save(userSession);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Вітаємо в боті анонімних відгуків! Будь ласка, оберіть вашу посаду:");
        message.setReplyMarkup(createRoleKeyboard());
        execute(message);
    }

    private void handleMessageByState(Long chatId, String messageText, UserSession userSession) throws TelegramApiException {
        switch (userSession.getState()) {
            case AWAITING_ROLE:
                try {
                    EmployeeRole role = EmployeeRole.valueOf(messageText.toUpperCase());
                    userSession.setRole(role);
                    userSession.setState(UserState.AWAITING_BRANCH);
                    userSessionRepository.save(userSession);

                    SendMessage message = new SendMessage();
                    message.setChatId(chatId.toString());
                    message.setText("Ви обрали: " + role.name() + ". Тепер, будь ласка, введіть назву вашої філії (наприклад, 'Філія_1', 'Сервісний_Центр'):");
                    execute(message);
                } catch (IllegalArgumentException e) {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId.toString());
                    message.setText("Невірна посада. Будь ласка, оберіть одну з кнопок або введіть: МЕХАНІК, ЕЛЕКТРИК, МЕНЕДЖЕР.");
                    message.setReplyMarkup(createRoleKeyboard());
                    execute(message);
                }
                break;
            case AWAITING_BRANCH:
                userSession.setBranch(messageText.trim());
                userSession.setState(UserState.READY_FOR_FEEDBACK);
                userSessionRepository.save(userSession);

                SendMessage message = new SendMessage();
                message.setChatId(chatId.toString());
                message.setText("Дякуємо! Ваша посада " + userSession.getRole().name() + " та ваша філія " + userSession.getBranch() + ".\n" +
                        "Тепер ви можете надсилати свій анонімний відгук у будь-який час. Напишіть ваше повідомлення:");
                execute(message);
                break;
            case READY_FOR_FEEDBACK:
                if (userSession.getRole() == null || userSession.getBranch() == null) {
                    message = new SendMessage();
                    message.setChatId(chatId.toString());
                    message.setText("Помилка сесії. Будь ласка, надішліть /start, щоб розпочати заново.");
                    execute(message);
                    userSession.setState(UserState.START);
                    userSessionRepository.save(userSession);
                    return;
                }

                SendMessage thankYouMessage = new SendMessage();
                thankYouMessage.setChatId(chatId.toString());
                thankYouMessage.setText("Дякуємо за ваш відгук. Аналізуємо повідомлення та зберігаємо...");
                execute(thankYouMessage);

                CompletableFuture.supplyAsync(() -> {
                    return openAIService.analyzeFeedback(messageText).block();
                }).thenAccept(analysisResult -> {
                    if (analysisResult != null) {
                        Feedback feedback = Feedback.builder()
                                .chatId(chatId)
                                .employeeRole(userSession.getRole())
                                .branch(userSession.getBranch())
                                .message(messageText)
                                .sentiment(analysisResult.getSentiment())
                                .criticalityLevel(analysisResult.getCriticalityLevel())
                                .resolutionSuggestion(analysisResult.getResolutionSuggestion())
                                .trelloCardCreated(false)
                                .build();
                        feedbackRepository.save(feedback);

                        googleSheetsService.appendFeedback(feedback)
                                .exceptionally(ex -> {
                                    System.err.println("Error appending feedback to Google Sheet: " + ex.getMessage());
                                    return null;
                                });

                        SendMessage resultMessage = new SendMessage();
                        resultMessage.setChatId(chatId.toString());
                        resultMessage.setText("Ваш відгук проаналізовано та збережено:\n" +
                                "Настрій: " + getSentimentText(feedback.getSentiment()) + "\n" +
                                "Критичність: " + feedback.getCriticalityLevel() + " (з 5)\n" +
                                "Можливе вирішення: " + feedback.getResolutionSuggestion());
                        try {
                            execute(resultMessage);
                        } catch (TelegramApiException e) {
                            System.err.println("Error sending analysis result to user: " + e.getMessage());
                        }

                        System.out.println("Feedback processed, saved to DB and Google Sheet: " + feedback.getId());
                    } else {
                        SendMessage errorMessage = new SendMessage();
                        errorMessage.setChatId(chatId.toString());
                        errorMessage.setText("Не вдалося проаналізувати відгук. Спробуйте пізніше.");
                        try {
                            execute(errorMessage);
                        } catch (TelegramApiException e) {
                            System.err.println("Error sending API analysis error to user: " + e.getMessage());
                        }
                    }
                }).exceptionally(ex -> {
                    System.err.println("Unexpected error during OpenAI analysis: " + ex.getMessage());
                    SendMessage errorMessage = new SendMessage();
                    errorMessage.setChatId(chatId.toString());
                    errorMessage.setText("Виникла неочікувана помилка під час аналізу відгуку. Спробуйте пізніше.");
                    try {
                        execute(errorMessage);
                    } catch (TelegramApiException e) {
                        System.err.println("Error sending unexpected analysis error to user: " + e.getMessage());
                    }
                    return null;
                });
                break;
            case START:
            default:
                message = new SendMessage();
                message.setChatId(chatId.toString());
                message.setText("Будь ла ласка, надішліть /start, щоб розпочати, або відправте ваш відгук.");
                execute(message);
                break;
        }
    }

    private ReplyKeyboardMarkup createRoleKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        for (EmployeeRole role : EmployeeRole.values()) {
            row.add(role.name());
        }
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private String getSentimentText(com.shiningpr1sm.feedbackbot.model.FeedbackSentiment sentiment) {
        if (Objects.nonNull(sentiment)) {
            switch (sentiment) {
                case POSITIVE: return "Позитивний";
                case NEUTRAL: return "Нейтральний";
                case NEGATIVE: return "Негативний";
                default: return "Невідомий";
            }
        }
        return "Невідомий";
    }
}