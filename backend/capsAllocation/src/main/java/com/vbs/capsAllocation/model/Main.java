package com.vbs.capsAllocation.model;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.json.JSONObject;

public class Main {
    private static final String API_URL = "https://judge0-ce.p.rapidapi.com";
    private static final String API_KEY = "44523e6ea8msheb05eb0b74fec52p153f7fjsnc04cea80a289";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your JavaScript code (type 'exit' on a new line to run):");

        StringBuilder jsCode = new StringBuilder();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.equalsIgnoreCase("exit")) {
                break;
            }
            jsCode.append(line).append("\n");
        }
        scanner.close();

        System.out.println("Executing your JavaScript code...\n");
        executeJavaScript(jsCode.toString());
    }

    public static void executeJavaScript(String jsSourceCode) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            JSONObject requestBody = new JSONObject();
            requestBody.put("language_id", 63); // 63 is JavaScript
            requestBody.put("source_code", jsSourceCode);
            requestBody.put("stdin", "");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/submissions?base64_encoded=false&wait=false&fields=*"))
                    .header("x-rapidapi-key", API_KEY)
                    .header("x-rapidapi-host", "judge0-ce.p.rapidapi.com")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject jsonResponse = new JSONObject(response.body());

            if (!jsonResponse.has("token")) {
                System.err.println("Error: Failed to retrieve token from submission response.");
                return;
            }

            String token = jsonResponse.getString("token");

            // Step 2: Fetch the result
            String result = fetchResult(client, token);
            System.out.println("Execution Output: \n" + result);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String fetchResult(HttpClient client, String token) throws Exception {
        String resultUrl = API_URL + "/submissions/" + token + "?base64_encoded=false&fields=*";

        for (int i = 0; i < 10; i++) { // Polling with retries
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resultUrl))
                    .header("x-rapidapi-key", API_KEY)
                    .header("x-rapidapi-host", "judge0-ce.p.rapidapi.com")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject jsonResponse = new JSONObject(response.body());

            if (jsonResponse.has("status") && jsonResponse.getJSONObject("status").getInt("id") == 3) {
                return jsonResponse.optString("stdout", "No output").replace("\\n", "\n");
            }
            Thread.sleep(2000); // Wait before retrying
        }
        return "Execution timed out.";
    }
}
