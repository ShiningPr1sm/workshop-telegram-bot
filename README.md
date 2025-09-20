
## 🚀 Getting Started

To get a local copy up and running, follow these steps.

### Prerequisites

*   **Java Development Kit (JDK) 21**
*   **Apache Maven** (correctly installed and configured in PATH)
*   **PostgreSQL Database Server**
*   An IDE (e.g., IntelliJ IDEA, VS Code)
*   **Telegram Bot Token and Username:** Obtain from [@BotFather](https://t.me/BotFather).
*   **OpenAI API Key:** Obtain from [platform.openai.com](https://platform.openai.com/).
*   **Google Cloud Project & Sheets API Credentials:**
    *   Create a GCP project.
    *   Enable **"Google Sheets API"**.
    *   Create a **Service Account** with "Editor" permissions for the Google Sheet.
    *   Generate and download the **JSON key file** for the Service Account.
    *   Share your target Google Sheet with the Service Account email address (grant "Editor" access).
    *   Get the **Spreadsheet ID** from the Google Sheet's URL.

### Installation & Configuration

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/ShiningPr1sm/workshot-telegram-bot.git
    ```

2.  **Configure `application.properties`:**
    Edit [`src/main/resources/application.properties`](https://github.com/ShiningPr1sm/workshot-telegram-bot/blob/master/src/main/resources/application.properties) with your credentials:

3.  **Place Google Credentials:**
    *   Place your downloaded Google Service Account JSON key file (e.g., `credentials.json`) into `src/main/resources/`.
    *   Ensure the `google.sheets.credentials-path` in `application.properties` matches the filename (e.g., `src/main/resources/credentials.json`).

4.  **Create PostgreSQL Database & Schema:**
    *   Connect to your PostgreSQL server.
    *   Create the database (e.g., `feedback_db`) with `UTF8` encoding.
    ```sql
    CREATE DATABASE feedback_db WITH ENCODING 'UTF8' LC_COLLATE='uk_UA.UTF-8' LC_CTYPE='uk_UA.UTF-8' TEMPLATE=template0;
    GRANT ALL PRIVILEGES ON DATABASE feedback_db TO postgres;
    ```
    *   The application will automatically create the `feedback` schema (if specified in `application.properties`) and tables on first run. Ensure your `postgres` user has `CREATE` privileges on the `feedback_db`.

5.  **Build the project:**
    ```bash
    mvn clean install
    ```

### Running the Application

1.  **Run the Spring Boot application:**
    ```bash
    mvn spring-boot:run
    ```
    Or, run the `FeedbackBotApplication.java` from your IDE.

2.  **Interact with the Telegram Bot:**
    *   Open Telegram, search for your bot username (e.g., `@MyFeedback_bot`).
    *   Send `/start` to begin the interaction.
    *   Follow the prompts to provide feedback.

3.  **Access the Admin Panel:**
    *   Open your web browser and navigate to: `http://localhost:8080/admin.html`
    *   You will see a table of collected feedback and filtering options.

<img width="1275" height="569" alt="image" src="https://github.com/user-attachments/assets/fcb37aac-919a-4dd9-8fd9-014b8ee2e1ba" />


## ⚠️ Important Security Note

The admin panel is currently **not secured**. In a production environment, it would be crucial to implement authentication and authorization (e.g., using Spring Security) to restrict access to authorized administrators only.
