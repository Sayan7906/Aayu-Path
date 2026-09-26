package com.example

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class TriageUrgency(val label: String, val containerColor: Color, val contentColor: Color) {
    RED_ALERT("CRITICAL RED ALERT", Color(0xFFFFDAD6), Color(0xFFBA1A1A)),
    YELLOW_ALERT("URGENT YELLOW ALERT", Color(0xFFFFF2CC), Color(0xFFB25E00)),
    GREEN_ALERT("ROUTINE GREEN ALERT", Color(0xFFE2F3E3), Color(0xFF1B5E20)),
    INFO("AI CLINICAL TRIAGE", Color(0xFFE0F2F1), Color(0xFF006A6A))
}

enum class GeminiModel(
    val modelId: String,
    val displayName: String,
    val badge: String,
    val description: String
) {
    FLASH(
        modelId = "gemini-3.5-flash",
        displayName = "3.5 Flash",
        badge = "General",
        description = "Fast clinical assessment and general triage"
    ),
    PRO(
        modelId = "gemini-3.1-pro-preview",
        displayName = "3.1 Pro",
        badge = "Complex",
        description = "Advanced diagnostic reasoning for complex multi-symptoms"
    ),
    FLASH_LITE(
        modelId = "gemini-3.1-flash-lite-preview",
        displayName = "3.1 Flash-Lite",
        badge = "Fast",
        description = "High-speed initial symptom evaluation"
    )
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val urgency: TriageUrgency = TriageUrgency.INFO,
    val timestamp: Long = System.currentTimeMillis()
)

object GeminiTriageClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_PROMPT = """You are the Aayu Path AI Medical Triage Specialist, an authoritative, empathetic clinical triage assistant built for Indian healthcare and emergency systems.
Your goal is to evaluate symptoms reported by the patient and provide a structured clinical triage:
1. URGENCY ASSESSMENT:
- Categorize clearly into one of:
  * [RED ALERT - EMERGENCY]: Immediate ICU/ER/Ambulance (102/108) required within 30 minutes (e.g., acute chest pain with diaphoresis, stroke symptoms, acute respiratory distress, severe trauma).
  * [YELLOW ALERT - URGENT]: Clinical evaluation required within 6-12 hours (e.g., persistent high fever with dehydration/dengue signs, persistent abdominal pain, deep wound).
  * [GREEN ALERT - ROUTINE]: Mild, non-emergency conditions suitable for General OPD consultation or home symptomatic care.
2. RECOMMEND SPECIALTY: Name the doctor specialty needed (e.g. Cardiology, Pulmonology, Gastroenterology, General Physician, Pediatrics).
3. PRACTICAL ACTIONS IN INDIA: Mention actionable steps on Aayu Path (e.g., check vacant ICU/Oxygen beds, locate nearest PMJAY empaneled hospital, register for OPD token).
4. PRECAUTIONARY ADVICE & FIRST AID: Practical dos and don'ts while seeking medical care.
5. CLINICAL DISCLAIMER: Concisely remind that AI triage is supportive and does not replace examination by a licensed medical practitioner.
Keep your output structured, clear, empathetic, and easily readable with bullet points."""

    suspend fun generateTriageResponse(
        history: List<ChatMessage>,
        userPrompt: String,
        model: GeminiModel
    ): Pair<String, TriageUrgency> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        val isKeyConfigured = apiKey.isNotBlank() &&
                apiKey != "MY_GEMINI_API_KEY" &&
                !apiKey.contains("PLACEHOLDER", ignoreCase = true)

        if (isKeyConfigured) {
            try {
                val mediaType = "application/json; charset=utf-8".toMediaType()

                val rootJson = JSONObject().apply {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT)))
                    })

                    val contentsArray = JSONArray()

                    // Previous history turns (limited to last 8 turns for latency and token budget)
                    val relevantHistory = history.takeLast(8)
                    for (msg in relevantHistory) {
                        contentsArray.put(JSONObject().apply {
                            put("role", if (msg.isUser) "user" else "model")
                            put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
                        })
                    }

                    // Add current user prompt
                    contentsArray.put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
                    })

                    put("contents", contentsArray)

                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.4)
                        put("topP", 0.9)
                    })
                }

                val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/${model.modelId}:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(requestUrl)
                    .post(rootJson.toString().toRequestBody(mediaType))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()

                if (response.isSuccessful && responseBody.isNotEmpty()) {
                    val resJson = JSONObject(responseBody)
                    val candidates = resJson.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val contentObj = firstCandidate?.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text")

                    if (!text.isNullOrBlank()) {
                        val urgency = detectUrgency(text)
                        return@withContext Pair(text, urgency)
                    }
                }
            } catch (_: Exception) {
                // Network or API failure: fall through to clinical rule engine
            }
        }

        // Offline / Fallback Clinical Triage Engine
        val fallback = generateClinicalFallback(userPrompt)
        return@withContext fallback
    }

    private fun detectUrgency(text: String): TriageUrgency {
        val upper = text.uppercase()
        return when {
            upper.contains("RED ALERT") || upper.contains("CRITICAL") || upper.contains("EMERGENCY") || upper.contains("IMMEDIATE") -> TriageUrgency.RED_ALERT
            upper.contains("YELLOW ALERT") || upper.contains("URGENT") || upper.contains("WITHIN 12 HOURS") -> TriageUrgency.YELLOW_ALERT
            upper.contains("GREEN ALERT") || upper.contains("ROUTINE") || upper.contains("MILD") -> TriageUrgency.GREEN_ALERT
            else -> TriageUrgency.INFO
        }
    }

    private fun generateClinicalFallback(prompt: String): Pair<String, TriageUrgency> {
        val lower = prompt.lowercase()

        val isRed = lower.contains("chest pain") || lower.contains("heart") ||
                lower.contains("breathless") || lower.contains("breathing") ||
                lower.contains("unconscious") || lower.contains("stroke") ||
                lower.contains("bleeding heavily") || lower.contains("collapse")

        val isYellow = lower.contains("fever") || lower.contains("chills") ||
                lower.contains("stomach") || lower.contains("abdomen") ||
                lower.contains("vomit") || lower.contains("dengue") ||
                lower.contains("malaria") || lower.contains("pain") ||
                lower.contains("dizziness")

        val urgency = when {
            isRed -> TriageUrgency.RED_ALERT
            isYellow -> TriageUrgency.YELLOW_ALERT
            else -> TriageUrgency.GREEN_ALERT
        }

        val text = buildString {
            when (urgency) {
                TriageUrgency.RED_ALERT -> {
                    append("🚨 **[RED ALERT - EMERGENCY PROTOCOL]**\n\n")
                    append("• **Clinical Risk**: High risk of cardiopulmonary or acute neurological event.\n")
                    append("• **Recommended Specialty**: Cardiology / Pulmonology / Emergency Medicine.\n")
                    append("• **Immediate Actions**:\n")
                    append("  1. Do not exert. Keep patient in a comfortable seated position with loosened clothing.\n")
                    append("  2. Call 102/108 or use the **Emergency Red Dial** on the Aayu Path home screen.\n")
                    append("  3. Direct transport to nearest tertiary care facility (e.g., AIIMS New Delhi or nearest vacant ICU bed).\n")
                    append("• **Precaution**: If conscious and advised by doctor, keep Sorbitrate or Aspirin ready only under clinical protocol.\n\n")
                }
                TriageUrgency.YELLOW_ALERT -> {
                    append("⚠️ **[YELLOW ALERT - URGENT MEDICAL EVALUATION]**\n\n")
                    append("• **Clinical Assessment**: Significant acute distress requiring doctor examination within 6-12 hours.\n")
                    append("• **Recommended Specialty**: General Medicine / Gastroenterology / Infectious Diseases.\n")
                    append("• **Recommended Actions**:\n")
                    append("  1. Monitor temperature, pulse, and hydration status.\n")
                    append("  2. Schedule an urgent OPD token via Aayu Path (Ayushman PMJAY accepted at public centers).\n")
                    append("  3. Avoid self-prescribing NSAIDs or unverified antibiotics.\n")
                    append("  4. Stay hydrated with oral rehydration solution (ORS) or boiled lukewarm water.\n\n")
                }
                TriageUrgency.GREEN_ALERT, TriageUrgency.INFO -> {
                    append("✅ **[GREEN ALERT - ROUTINE CLINICAL TRIAGE]**\n\n")
                    append("• **Clinical Assessment**: Symptoms indicate non-critical, mild, or subacute condition.\n")
                    append("• **Recommended Specialty**: General Physician OPD Consultation.\n")
                    append("• **Self-Care Guidance**:\n")
                    append("  1. Adequate rest in a well-ventilated room.\n")
                    append("  2. Maintain hydration and light, easily digestible meals.\n")
                    append("  3. If symptoms worsen, fever exceeds 102°F, or breathlessness develops, seek emergency care immediately.\n\n")
                }
            }
            append("🩺 *Clinical Disclaimer: This automated assessment aids medical routing and does not substitute professional medical diagnosis.*")
        }

        return Pair(text, urgency)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriageChatbotScreen(
    initialSymptomQuery: String? = null,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val coroutineScope = rememberCoroutineScope()
    var selectedModel by remember { mutableStateOf(GeminiModel.FLASH) }
    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                text = "Namaste! I am your Aayu Path AI Triage Specialist. Please describe your symptoms, how long you've felt them, and any pre-existing conditions so I can guide your care.",
                isUser = false,
                urgency = TriageUrgency.INFO
            )
        )
    }

    val listState = rememberLazyListState()

    // Send initial query if provided from home cards
    LaunchedEffect(initialSymptomQuery) {
        if (!initialSymptomQuery.isNullOrBlank()) {
            val userMsg = ChatMessage(text = initialSymptomQuery, isUser = true)
            messages.add(userMsg)
            isSending = true
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
                val (reply, urgency) = GeminiTriageClient.generateTriageResponse(
                    history = messages.dropLast(1),
                    userPrompt = initialSymptomQuery,
                    model = selectedModel
                )
                messages.add(ChatMessage(text = reply, isUser = false, urgency = urgency))
                isSending = false
                delay(100)
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    val quickChips = listOf(
        "Chest tightness & shortness of breath",
        "High fever with severe chills for 2 days",
        "Acute pain in lower right abdomen",
        "Severe dizziness & throbbing headache",
        "Pediatric fever and continuous vomiting"
    )

    fun sendCurrentPrompt(promptText: String) {
        if (promptText.isBlank() || isSending) return
        val userMsg = ChatMessage(text = promptText.trim(), isUser = true)
        messages.add(userMsg)
        inputText = ""
        isSending = true

        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
            val (reply, urgency) = GeminiTriageClient.generateTriageResponse(
                history = messages.dropLast(1),
                userPrompt = promptText,
                model = selectedModel
            )
            messages.add(ChatMessage(text = reply, isUser = false, urgency = urgency))
            isSending = false
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("triage_chat_screen"),
        containerColor = GeoBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(GeoPrimaryTeal, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Triage AI",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Aayu Path AI Triage",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GeoTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE2F3E3), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        "LIVE AI",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                            }
                            Text(
                                "Clinical emergency routing & assessment",
                                fontSize = 11.sp,
                                color = GeoTextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_triage_chat")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GeoTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            messages.clear()
                            messages.add(
                                ChatMessage(
                                    text = "Chat cleared. Please describe your symptoms or concern.",
                                    isUser = false,
                                    urgency = TriageUrgency.INFO
                                )
                            )
                        },
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Clear Chat",
                            tint = GeoTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Quick Symptom Suggestions row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickChips.forEach { chip ->
                            SuggestionChip(
                                onClick = { sendCurrentPrompt(chip) },
                                label = { Text(chip, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = GeoTriageCardBg,
                                    labelColor = GeoTriageText
                                ),
                                border = BorderStroke(1.dp, GeoTriageCardBorder),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }

                    // Text Input Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    "Describe symptoms (e.g. fever, chest pain)...",
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("triage_chat_input"),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFFBFBFB),
                                unfocusedContainerColor = Color(0xFFFBFBFB),
                                focusedBorderColor = GeoPrimaryTeal,
                                unfocusedBorderColor = Color(0xFFD0D7D7),
                                focusedTextColor = GeoTextPrimary,
                                unfocusedTextColor = GeoTextPrimary,
                                cursorColor = GeoPrimaryTeal
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { sendCurrentPrompt(inputText) },
                            enabled = inputText.isNotBlank() && !isSending,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (inputText.isNotBlank() && !isSending) GeoPrimaryTeal else Color(0xFFCCCCCC),
                                    CircleShape
                                )
                                .testTag("triage_send_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Model Selector banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F5))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Model",
                        tint = GeoPrimaryTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "AI Engine:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GeoTextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    GeminiModel.values().forEach { model ->
                        val isSelected = selectedModel == model
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) GeoPrimaryTeal else Color.White)
                                .border(
                                    1.dp,
                                    if (isSelected) GeoPrimaryTeal else Color(0xFFD0D7D7),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedModel = model }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = model.displayName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else GeoTextPrimary
                            )
                        }
                    }
                }
            }

            // Message Thread
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubbleItem(msg = msg)
                }

                if (isSending) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color(0xFFC0C9C8)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(end = 40.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = GeoPrimaryTeal
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Analyzing clinical indicators with ${selectedModel.displayName}...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GeoTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(msg: ChatMessage) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(msg.timestamp) { timeFormat.format(Date(msg.timestamp)) }

    if (msg.isUser) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GeoPrimaryTeal),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    text = msg.text,
                    fontSize = 13.sp,
                    color = Color.White,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
            Text(
                text = formattedTime,
                fontSize = 10.sp,
                color = GeoTextSecondary,
                modifier = Modifier.padding(top = 2.dp, end = 4.dp)
            )
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            // Urgency badge if not generic info
            if (msg.urgency != TriageUrgency.INFO) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = msg.urgency.containerColor),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = msg.urgency.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = msg.urgency.contentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (msg.urgency == TriageUrgency.RED_ALERT) GeoEmergencyBorder else Color(0xFFC0C9C8)),
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Specialist",
                                tint = GeoPrimaryTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Aayu Path AI Specialist",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GeoPrimaryTeal
                            )
                        }
                        Text(
                            text = formattedTime,
                            fontSize = 10.sp,
                            color = GeoTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = msg.text,
                        fontSize = 13.sp,
                        color = GeoTextPrimary,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}
