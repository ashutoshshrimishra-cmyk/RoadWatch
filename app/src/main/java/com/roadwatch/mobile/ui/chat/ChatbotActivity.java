package com.roadwatch.mobile.ui.chat;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.roadwatch.mobile.R;
import com.roadwatch.mobile.ai.MistralChatClient;
import com.roadwatch.mobile.network.ApiClient;
import com.roadwatch.mobile.network.ApiService;
import com.roadwatch.mobile.network.dto.AiChatRequest;
import com.roadwatch.mobile.network.dto.AiChatResponse;
import com.roadwatch.mobile.ui.BaseActivity;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * AI Assistant screen powered by Mistral.
 *
 * Features:
 *  - Chat-bubble layout (user right / AI left) with typing indicator.
 *  - Voice-to-Text via Android SpeechRecognizer (mic button).
 *  - Backend chat endpoint POST /api/ai/chat with on-device Mistral fallback.
 *  - Top-right menu: Clear chat history.
 *  - Back button returns to dashboard via BaseActivity.
 */
public class ChatbotActivity extends BaseActivity {

    private static final String TAG = "ChatbotActivity";
    private static final int REQ_AUDIO_PERMISSION = 201;

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final MistralChatClient mistralFallback = new MistralChatClient();

    private ChatAdapter chatAdapter;
    private RecyclerView rvChat;
    private EditText etChatInput;
    private ImageButton btnSend;
    private ImageButton btnMic;

    private FrameLayout listeningOverlay;
    private TextView listeningText;
    private ImageView listeningPulse;
    private ObjectAnimator pulseAnimator;

    private SpeechRecognizer speechRecognizer;
    private boolean isListening;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);
        setupToolbar("AI Assistant");

        rvChat = findViewById(R.id.rvChat);
        etChatInput = findViewById(R.id.etChatInput);
        btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.btnMic);
        listeningOverlay = findViewById(R.id.listeningOverlay);
        listeningText = findViewById(R.id.listeningText);
        listeningPulse = findViewById(R.id.listeningPulse);

        chatAdapter = new ChatAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvChat.setLayoutManager(lm);
        rvChat.setAdapter(chatAdapter);

        showWelcomeMessage();

        btnSend.setOnClickListener(v -> {
            String text = etChatInput.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(this, "Type a question first", Toast.LENGTH_SHORT).show();
                return;
            }
            etChatInput.setText("");
            sendMessage(text);
        });

        btnMic.setOnClickListener(v -> {
            if (isListening) {
                stopListening(false);
            } else {
                requestAudioAndStartListening();
            }
        });

        findViewById(R.id.btnCancelListening).setOnClickListener(v -> stopListening(true));
    }

    private void showWelcomeMessage() {
        chatAdapter.addMessage(new ChatMessage(
                "Hello! I'm RoadWatch AI. Ask me about pothole reporting, road safety, "
                        + "or how to use the app. Tap the mic to speak instead of typing.",
                ChatMessage.Type.AI
        ));
    }

    // ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€ Backend / AI Chat ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    private void sendMessage(String userText) {
        Log.i(TAG, "User asked: " + userText);
        chatAdapter.addMessage(new ChatMessage(userText, ChatMessage.Type.USER));
        chatAdapter.showTypingIndicator();
        scrollToBottom();
        setSendEnabled(false);

        try {
            ApiService api = ApiClient.api(this);
            api.aiChat(new AiChatRequest(userText)).enqueue(new Callback<AiChatResponse>() {
                @Override
                public void onResponse(@NonNull Call<AiChatResponse> call,
                                       @NonNull Response<AiChatResponse> response) {
                    if (response.isSuccessful() && response.body() != null
                            && !TextUtils.isEmpty(response.body().getReply())) {
                        deliverAiReply(response.body().getReply());
                    } else {
                        Log.w(TAG, "Backend ai/chat failed http=" + response.code()
                                + " €” falling back to direct Mistral");
                        fallbackToDirectMistral(userText);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<AiChatResponse> call, @NonNull Throwable t) {
                    Log.w(TAG, "Backend ai/chat network error: " + t.getMessage()
                            + " €” falling back to direct Mistral");
                    fallbackToDirectMistral(userText);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize backend AI client", e);
            fallbackToDirectMistral(userText);
        }
    }

    private void fallbackToDirectMistral(String userText) {
        ioExecutor.execute(() -> {
            try {
                String reply = mistralFallback.ask(userText);
                runOnUiThread(() -> {
                    if (!isFinishing()) {
                        deliverAiReply(reply);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Mistral fallback failed", e);
                runOnUiThread(() -> {
                    if (!isFinishing()) {
                        chatAdapter.hideTypingIndicator();
                        chatAdapter.addMessage(new ChatMessage(
                                "Sorry, I couldn't reach the AI service right now. "
                                        + "Please check your connection and try again.",
                                ChatMessage.Type.AI));
                        scrollToBottom();
                        setSendEnabled(true);
                    }
                });
            }
        });
    }

    private void deliverAiReply(String reply) {
        chatAdapter.hideTypingIndicator();
        if (reply == null || reply.trim().isEmpty()) {
            reply = "Sorry, I couldn't understand that. Try asking about reporting a road issue or checking complaint status.";
        }
        chatAdapter.addMessage(new ChatMessage(ChatAdapter.formatChatText(reply), ChatMessage.Type.AI));
        scrollToBottom();
        setSendEnabled(true);
    }

    private void setSendEnabled(boolean enabled) {
        btnSend.setEnabled(enabled);
        btnSend.setAlpha(enabled ? 1f : 0.5f);
    }

    private void scrollToBottom() {
        rvChat.post(() -> {
            int last = chatAdapter.getItemCount() - 1;
            if (last >= 0) rvChat.smoothScrollToPosition(last);
        });
    }

    // ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€ Voice-to-Text ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    private void requestAudioAndStartListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQ_AUDIO_PERMISSION);
            return;
        }
        startListening();
    }

    private void startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this,
                    "Speech recognition is not available on this device",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Always destroy and recreate to avoid ERROR_CLIENT from stale state
        if (speechRecognizer != null) {
            try {
                speechRecognizer.cancel(); // Cancel first to avoid stale state
                speechRecognizer.destroy();
            } catch (Exception e) {
                Log.w(TAG, "Error cleaning up previous SpeechRecognizer", e);
            } finally {
                speechRecognizer = null;
            }
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(recognitionListener);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create SpeechRecognizer", e);
            Toast.makeText(this,
                    "Voice input is unavailable on this device",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        showListeningOverlay("Listening...");

        // Post with a short delay to let the recognizer fully initialize
        rvChat.postDelayed(() -> {
            try {
                if (speechRecognizer != null) {
                    speechRecognizer.startListening(intent);
                    isListening = true;
                    btnMic.setActivated(true);
                    Log.i(TAG, "Voice-to-text: started listening");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to start SpeechRecognizer", e);
                stopListening(true);
                Toast.makeText(ChatbotActivity.this,
                        "Could not start voice input: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }, 150);
    }

    private void stopListening(boolean cancelled) {
        if (speechRecognizer != null) {
            try {
                if (cancelled) {
                    speechRecognizer.cancel();
                } else {
                    speechRecognizer.stopListening();
                }
            } catch (Exception e) {
                Log.w(TAG, "Error stopping SpeechRecognizer: " + e.getMessage());
            }
        }
        isListening = false;
        btnMic.setActivated(false);
        hideListeningOverlay();
        Log.i(TAG, "Voice-to-text: stopped (cancelled=" + cancelled + ")");
    }

    private void showListeningOverlay(String message) {
        listeningText.setText(message);
        listeningOverlay.setVisibility(View.VISIBLE);
        startPulseAnimation();
    }

    private void hideListeningOverlay() {
        listeningOverlay.setVisibility(View.GONE);
        stopPulseAnimation();
    }

    private void startPulseAnimation() {
        stopPulseAnimation();
        pulseAnimator = ObjectAnimator.ofFloat(listeningPulse, "scaleX", 1f, 1.15f, 1f);
        ObjectAnimator pulseY = ObjectAnimator.ofFloat(listeningPulse, "scaleY", 1f, 1.15f, 1f);
        pulseAnimator.setDuration(1000);
        pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseY.setDuration(1000);
        pulseY.setRepeatCount(ObjectAnimator.INFINITE);
        pulseY.setInterpolator(new LinearInterpolator());
        pulseAnimator.start();
        pulseY.start();
    }

    private void stopPulseAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
        if (listeningPulse != null) {
            listeningPulse.setScaleX(1f);
            listeningPulse.setScaleY(1f);
        }
    }

    private final RecognitionListener recognitionListener = new RecognitionListener() {
        @Override public void onReadyForSpeech(Bundle params) {
            runOnUiThread(() -> listeningText.setText("Listening..."));
        }

        @Override public void onBeginningOfSpeech() {}
        @Override public void onRmsChanged(float rmsdB) {}
        @Override public void onBufferReceived(byte[] buffer) {}

        @Override public void onEndOfSpeech() {
            runOnUiThread(() -> listeningText.setText("Processing..."));
        }

        @Override public void onError(int error) {
            String msg = mapSpeechError(error);
            Log.w(TAG, "SpeechRecognizer error code=" + error + " (" + msg + ")");
            runOnUiThread(() -> {
                // Always fully stop on error. Never auto-restart €” the user must
                // tap the mic button again to listen. Auto-restarting here was
                // causing an infinite loop where background noise / errors
                // kept relaunching the recognizer and sending garbage text.
                stopListening(true);
                if (error == SpeechRecognizer.ERROR_NO_MATCH
                        || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    Toast.makeText(ChatbotActivity.this, msg, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ChatbotActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
            String spoken = (matches != null && !matches.isEmpty()) ? matches.get(0) : null;
            String trimmed = spoken == null ? "" : spoken.trim();

            // Garbage filter: ignore empty / 1-char results (background noise,
            // stray clicks, etc.) so they don't trigger a chat API call.
            if (trimmed.length() < 2) {
                Log.i(TAG, "Voice-to-text: ignoring noise result \"" + trimmed + "\"");
                runOnUiThread(() -> stopListening(true));
                return;
            }

            Log.i(TAG, "Voice-to-text result: " + trimmed);
            final String finalText = trimmed;
            runOnUiThread(() -> {
                // Fully stop. Do NOT auto-restart €” user must tap mic again.
                stopListening(false);
                String existing = etChatInput.getText().toString().trim();
                String combined = existing.isEmpty() ? finalText : existing + " " + finalText;
                etChatInput.setText(combined);
                etChatInput.setSelection(etChatInput.getText().length());
            });
        }

        @Override public void onPartialResults(Bundle partialResults) {
            ArrayList<String> matches = partialResults.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                runOnUiThread(() -> listeningText.setText("\"" + matches.get(0) + "\""));
            }
        }

        @Override public void onEvent(int eventType, Bundle params) {}
    };

    private String mapSpeechError(int code) {
        switch (code) {
            case SpeechRecognizer.ERROR_AUDIO: return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT: return "Speech client error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Microphone permission required";
            case SpeechRecognizer.ERROR_NETWORK: return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH: return "Didn't catch that €” try again";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Recognizer is busy";
            case SpeechRecognizer.ERROR_SERVER: return "Speech server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No speech detected";
            default: return "Speech error (" + code + ")";
        }
    }

    // ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€ Menu / Lifecycle ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_clear_chat) {
            confirmClearChat();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmClearChat() {
        new AlertDialog.Builder(this)
                .setTitle("Clear chat history?")
                .setMessage("This will remove all messages in this conversation.")
                .setPositiveButton("Clear", (d, w) -> {
                    chatAdapter.clearAll();
                    showWelcomeMessage();
                    Toast.makeText(this, "Chat cleared", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Chat history cleared");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                Toast.makeText(this,
                        "Microphone permission is required for voice input",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPulseAnimation();
        
        // FIXED: Proper SpeechRecognizer cleanup with error handling
        if (speechRecognizer != null) {
            try {
                speechRecognizer.cancel(); // Cancel current operations first
                speechRecognizer.destroy();
            } catch (Exception e) {
                Log.e(TAG, "Error destroying SpeechRecognizer", e);
            } finally {
                // FIXED: Always null the reference regardless of exceptions
                speechRecognizer = null;
                isListening = false;
            }
        }
        
        // FIXED: Proper ExecutorService shutdown with timeout
        if (ioExecutor != null && !ioExecutor.isShutdown()) {
            ioExecutor.shutdown();
            try {
                if (!ioExecutor.awaitTermination(800, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    ioExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                ioExecutor.shutdownNow();
            }
        }
    }
}
