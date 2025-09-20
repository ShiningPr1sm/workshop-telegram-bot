package com.shiningpr1sm.feedbackbot.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.shiningpr1sm.feedbackbot.model.Feedback;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class GoogleSheetsService {

    @Value("${google.sheets.application-name}")
    private String applicationName;
    @Value("${google.sheets.spreadsheet-id}")
    private String spreadsheetId;
    @Value("${google.sheets.credentials-path}")
    private String credentialsPath;

    private Sheets sheetsService;
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static final List<Object> EXPECTED_HEADERS = Arrays.asList(
            "ID", "CHAT_ID", "ROLE", "BRANCH", "MESSAGE", "SENTIMENT", "CRITICALITY_LEVEL", "RESOLUTION_SUGGESTION", "SUBMITTED_AT"
    );
    private final String sheetName = "Sheet1"; // Имя вашего листа, которое вы переименовали

    @PostConstruct
    public void init() throws IOException, GeneralSecurityException, ExecutionException, InterruptedException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(credentialsPath))
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

        sheetsService = new Sheets.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(applicationName)
                .build();
        checkAndSetHeaders();
    }

    public CompletableFuture<Void> appendFeedback(Feedback feedback) {
        return CompletableFuture.runAsync(() -> {
            try {
                List<Object> rowData = Arrays.asList(
                        feedback.getId(),
                        feedback.getChatId(),
                        feedback.getEmployeeRole().name(),
                        feedback.getBranch(),
                        feedback.getMessage(),
                        feedback.getSentiment().name(),
                        feedback.getCriticalityLevel(),
                        feedback.getResolutionSuggestion(),
                        feedback.getSubmittedAt().toString()
                );

                ValueRange body = new ValueRange().setValues(Collections.singletonList(rowData));

                final String range = sheetName + "!A:I";

                sheetsService.spreadsheets().values()
                        .append(spreadsheetId, range, body)
                        .setValueInputOption("RAW")
                        .setInsertDataOption("INSERT_ROWS")
                        .execute();

                System.out.println("Feedback successfully appended to Google Sheet: " + feedback.getId());

            } catch (IOException e) {
                System.err.println("Error appending feedback to Google Sheet: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to append feedback to Google Sheet", e);
            }
        });
    }

    private void checkAndSetHeaders() throws IOException {
        final String headerRange = sheetName + "!1:1";
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, headerRange)
                .execute();
        List<List<Object>> values = response.getValues();

        boolean headersMatch = (values != null && !values.isEmpty() && values.get(0).equals(EXPECTED_HEADERS));

        if (!headersMatch) {
            System.out.println("Google Sheet headers are missing or incorrect. Setting headers...");
            ValueRange headerBody = new ValueRange().setValues(Collections.singletonList(EXPECTED_HEADERS));
            sheetsService.spreadsheets().values()
                    .update(spreadsheetId, headerRange, headerBody)
                    .setValueInputOption("RAW")
                    .execute();
            System.out.println("Google Sheet headers checked and set.");
        } else {
            System.out.println("Google Sheet headers are already correct.");
        }
    }
}