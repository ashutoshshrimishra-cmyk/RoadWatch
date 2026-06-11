package com.roadwatch.mobile.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.roadwatch.mobile.BuildConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MistralChatClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String API_URL = "https://api.mistral.ai/v1/chat/completions";
    private static final String MODEL = "mistral-small-latest";
    private static final String SYSTEM_PROMPT =
            "You are RoadWatch Assistant €” a strictly civic, road-infrastructure focused helper "
                    + "for Indian citizens using the RoadWatch mobile app. "
                    + "ONLY discuss: potholes, road cracks, broken dividers, faulty street lighting, "
                    + "road safety, how to file or track a road complaint inside the RoadWatch app, "
                    + "road maintenance budgets, public road infrastructure status, and traffic-safety advice. "
                    + "If the user asks about anything outside this domain (general chit-chat, coding, recipes, "
                    + "celebrities, politics unrelated to roads, math homework, jokes, weather, sports, etc.) "
                    + "POLITELY REFUSE in one sentence and steer them back to a road-infrastructure topic, "
                    + "for example: 'I can only help with road-related issues €” try asking about reporting a "
                    + "pothole or checking your complaint status.' "
                    + "Keep replies short (under 80 words), practical, and India-context aware. "
                    + "Reply in plain text only €” no markdown, no bullet symbols, no asterisks, no headings.";

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();

    public String ask(String userMessage) throws IOException {
        String apiKey = BuildConfig.MISTRAL_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("MISTRAL_API_KEY missing. Add it to mobile/local.properties");
        }

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", SYSTEM_PROMPT);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);

        JsonArray messages = new JsonArray();
        messages.add(systemMsg);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.add("messages", messages);

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(gson.toJson(body), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String raw = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Mistral API HTTP " + response.code() + ": " + raw);
            }
            JsonObject json = gson.fromJson(raw, JsonObject.class);
            if (json == null) {
                throw new IOException("Invalid JSON response from Mistral");
            }
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                throw new IOException("Empty response from Mistral");
            }
            JsonObject firstChoice = choices.get(0).getAsJsonObject();
            if (firstChoice == null || !firstChoice.has("message")) {
                throw new IOException("Missing message field in Mistral response");
            }
            JsonObject message = firstChoice.getAsJsonObject("message");
            if (message == null || !message.has("content")) {
                throw new IOException("Missing content field in Mistral response");
            }
            return message.get("content").getAsString();
        }
    }
}
