package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Data Models
data class Hospital(
    val id: String,
    val name: String,
    val address: String,
    val contact: String,
    val generalBeds: Int,
    val maxGeneralBeds: Int,
    val icuBeds: Int,
    val maxIcuBeds: Int,
    val oxygenBeds: Int,
    val maxOxygenBeds: Int,
    val specializations: List<String>,
    val acceptsAbPmjay: Boolean,
    val rating: Int, // crowdsourced verification down/up count
    val opdFee: Int,
    val wardRentPerDay: Int,
    val icuRentPerDay: Int,
    val diagnosticTestCost: Int, // Cost for standard chest X-Ray/Blood panel
    val distanceKm: Double,
    val area: String
)

data class LiveAmbulance(
    val id: String,
    val serviceName: String,
    val type: String, // ALS, BLS, Cardiac
    val distanceMin: Int,
    val status: String, // Available, Busy, OnWay
    val contact: String
)

data class BloodBank(
    val id: String,
    val name: String,
    val area: String,
    val aPositive: Int,
    val bPositive: Int,
    val oNegative: Int,
    val oPositive: Int,
    val abPositive: Int,
    val verifiedHoursAgo: Int
)

data class Booking(
    val id: String,
    val type: String, // "OPD", "Ambulance", "Lab", "Pharmacy"
    val mainDetail: String,
    val secondaryDetail: String,
    val hospitalName: String,
    val liveToken: String = "",
    val queueStatus: String = "",
    val waitTimeMin: Int = 0,
    val status: String // "Confirmed", "En Route", "Completed"
)

// Main Activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Mock Databases (Stateful)
    val hospitalsList = remember {
        mutableStateListOf(
            Hospital(
                id = "h1",
                name = "AIIMS New Delhi",
                address = "Ansari Nagar East, New Delhi",
                contact = "+91-11-26588500",
                generalBeds = 142,
                maxGeneralBeds = 150,
                icuBeds = 8,
                maxIcuBeds = 25,
                oxygenBeds = 65,
                maxOxygenBeds = 70,
                specializations = listOf("Cardiology", "Neurology", "Oncology", "Pediatrics", "Emergency Medicine"),
                acceptsAbPmjay = true,
                rating = 324,
                opdFee = 10,
                wardRentPerDay = 0,
                icuRentPerDay = 0,
                diagnosticTestCost = 150,
                distanceKm = 1.2,
                area = "South Delhi"
            ),
            Hospital(
                id = "h2",
                name = "Max Super Speciality, Saket",
                address = "Press Enclave Road, Saket, New Delhi",
                contact = "+91-11-26515050",
                generalBeds = 45,
                maxGeneralBeds = 90,
                icuBeds = 3,
                maxIcuBeds = 20,
                oxygenBeds = 15,
                maxOxygenBeds = 40,
                specializations = listOf("Cardiology", "Orthopedics", "Urology", "Organ Transplant"),
                acceptsAbPmjay = false, // Private tier
                rating = 189,
                opdFee = 1000,
                wardRentPerDay = 8000,
                icuRentPerDay = 25000,
                diagnosticTestCost = 1800,
                distanceKm = 2.4,
                area = "South Delhi"
            ),
            Hospital(
                id = "h3",
                name = "Guru Teg Bahadur Hospital",
                address = "Dilshad Garden, East Delhi",
                contact = "+91-11-22586262",
                generalBeds = 115,
                maxGeneralBeds = 180,
                icuBeds = 0, // No ICU vacant currently
                maxIcuBeds = 30,
                oxygenBeds = 35,
                maxOxygenBeds = 80,
                specializations = listOf("General Medicine", "General Surgery", "Pediatrics", "Obstetrics"),
                acceptsAbPmjay = true,
                rating = 142,
                opdFee = 10,
                wardRentPerDay = 0,
                icuRentPerDay = 0,
                diagnosticTestCost = 100,
                distanceKm = 8.5,
                area = "East Delhi"
            ),
            Hospital(
                id = "h4",
                name = "Fortis Escorts Heart Institute",
                address = "Okhla Road, New Delhi",
                contact = "+91-11-42111111",
                generalBeds = 28,
                maxGeneralBeds = 80,
                icuBeds = 5,
                maxIcuBeds = 15,
                oxygenBeds = 12,
                maxOxygenBeds = 30,
                specializations = listOf("Cardiology", "Cardiac Surgery", "Emergency Medicine"),
                acceptsAbPmjay = true,
                rating = 98,
                opdFee = 1200,
                wardRentPerDay = 10000,
                icuRentPerDay = 30000,
                diagnosticTestCost = 2200,
                distanceKm = 4.1,
                area = "South Delhi"
            ),
            Hospital(
                id = "h5",
                name = "Safdarjung Hospital",
                address = "Ansari Nagar West, New Delhi",
                contact = "+91-11-26730000",
                generalBeds = 210,
                maxGeneralBeds = 350,
                icuBeds = 4,
                maxIcuBeds = 40,
                oxygenBeds = 82,
                maxOxygenBeds = 100,
                specializations = listOf("General Medicine", "Orthopedics", "Burns & Plastic", "Trauma Care"),
                acceptsAbPmjay = true,
                rating = 245,
                opdFee = 10,
                wardRentPerDay = 0,
                icuRentPerDay = 0,
                diagnosticTestCost = 120,
                distanceKm = 1.5,
                area = "South Delhi"
            )
        )
    }

    val liveAmbulances = remember {
        mutableStateListOf(
            LiveAmbulance("a1", "Dharma Indian Ambulance", "ALS - Advanced Life Support", 2, "Available", "+91-9812345678"),
            LiveAmbulance("a2", "Medilift Emergency Service", "BLS - Basic Life Support", 5, "Busy", "+91-9876543210"),
            LiveAmbulance("a3", "Red Plus Cardiac Wheels", "ICU Mobile Resuscitation", 8, "Available", "+91-9911991199")
        )
    }

    val bloodBanksList = remember {
        mutableStateListOf(
            BloodBank("b1", "Indian Red Cross Depot", "South Delhi", 12, 8, 3, 15, 6, 2),
            BloodBank("b2", "AIIMS Blood Donation Centre", "Ansari Nagar", 28, 14, 0, 18, 5, 1),
            BloodBank("b3", "Rotary Blood Bank, Okhla", "South Delhi", 5, 22, 1, 32, 11, 4)
        )
    }

    val myBookingsList = remember {
        mutableStateListOf(
            Booking(
                id = "b001",
                type = "Lab",
                mainDetail = "CBC and Serum Creatinine Panel",
                secondaryDetail = "Scheduled: Today, 3:30 PM",
                hospitalName = "Apollo Diagnostics Lab",
                status = "Confirmed"
            ),
            Booking(
                id = "b002",
                type = "OPD",
                mainDetail = "Cardiology OPD Doctor consultation",
                secondaryDetail = "Token: AB-109 | Gate No. 4",
                hospitalName = "AIIMS New Delhi",
                liveToken = "AB-109",
                queueStatus = "4 Patients Ahead",
                waitTimeMin = 15,
                status = "Confirmed"
            )
        )
    }

    // Shared UI States
    var currentTab by remember { mutableIntStateOf(0) } // 0: Home, 1: Directory, 2: Bookings, 3: Settings/Locker
    var searchQuery by remember { mutableStateOf("") }
    var currentLanguage by remember { mutableStateOf("EN") } // EN, HN (Hindi), BN (Bengali)
    var abPmjayOnlyFilter by remember { mutableStateOf(false) }
    var directoryBedTypeFilter by remember { mutableStateOf("All") } // All, General, ICU, Oxygen
    var voluntaryDonorRegistered by remember { mutableStateOf(false) }
    var voiceSearchPulsing by remember { mutableStateOf(false) }
    var showSosModal by remember { mutableStateOf(false) }
    val showCostDialog = remember { mutableStateOf<Hospital?>(null) }
    val showVerifyDialog = remember { mutableStateOf<Hospital?>(null) }
    var triageSymptomIndex by remember { mutableStateOf<Int?>(null) }
    var isTriageChatbotOpen by remember { mutableStateOf(false) }
    var triageChatbotInitialQuery by remember { mutableStateOf<String?>(null) }

    if (isTriageChatbotOpen) {
        TriageChatbotScreen(
            initialSymptomQuery = triageChatbotInitialQuery,
            onClose = {
                isTriageChatbotOpen = false
                triageChatbotInitialQuery = null
            }
        )
        return
    }

    // Health Locker State fields
    var lockerName by remember { mutableStateOf("Rajesh Kumar") }
    var lockerAge by remember { mutableStateOf("45") }
    var lockerBloodGroup by remember { mutableStateOf("B+") }
    var lockerAadhar by remember { mutableStateOf("xxxx-xxxx-5621") }
    var lockerPmjayStatus by remember { mutableStateOf(true) }

    // Translations Dict
    val locationText = when(currentLanguage) {
        "HN" -> "दक्षिण दिल्ली, भारत"
        "BN" -> "দক্ষিণ দিল্লি, ভারত"
        else -> "South Delhi, India"
    }

    val searchTextHint = when(currentLanguage) {
        "HN" -> "अस्पताल, बिस्तर या ब्लड बैंक खोजें..."
        "BN" -> "হাসপাতাল, বেড বা রক্ত ব্যাঙ্ক খুঁজুন..."
        else -> "Search hospitals, beds, or blood banks..."
    }

    val emergencyTitle = when(currentLanguage) {
        "HN" -> "आपातकालीन एसओएस"
        "BN" -> "জরুরি এসওএস"
        else -> "Emergency SOS"
    }

    val emergencySub = when(currentLanguage) {
        "HN" -> "निकटतम आईसीयू और एम्बुलेंस"
        "BN" -> "নিকটতম আইসিইউ এবং অ্যাম্বুলেন্স"
        else -> "Nearest ICU & Ambulance"
    }

    val realTimeHeading = when(currentLanguage) {
        "HN" -> "लाइव बेड उपलब्धता स्थिति"
        "BN" -> "রিয়েল-টাইম বেড স্ট্যাটাস"
        else -> "REAL-TIME BED STATUS"
    }

    val tabHomeLabel = when(currentLanguage) {
        "HN" -> "होम"
        "BN" -> "হোম"
        else -> "Home"
    }

    val tabDirLabel = when(currentLanguage) {
        "HN" -> "अस्पताल सूची"
        "BN" -> "হাসপাতাল"
        else -> "Directory"
    }

    val tabBookingsLabel = when(currentLanguage) {
        "HN" -> "मेरी बुकिंग्स"
        "BN" -> "বুকিং সমূহ"
        else -> "Bookings"
    }

    val tabLockerLabel = when(currentLanguage) {
        "HN" -> "पॉकेट लॉकर"
        "BN" -> "লকার সেটিংস"
        else -> "Locker & Profile"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = GeoBackground,
        bottomBar = {
            // Stylized Material 3 Navigation Bar (As instructed in "Geometric Balance")
            NavigationBar(
                containerColor = GeoNavBg,
                modifier = Modifier.navigationBarsPadding().height(84.dp)
            ) {
                // Tab Home
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(24.dp)) },
                    label = { Text(tabHomeLabel, fontSize = 11.sp, fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GeoNavTextActive,
                        selectedTextColor = GeoNavTextActive,
                        indicatorColor = GeoNavIndicator,
                        unselectedIconColor = GeoNavTextInactive,
                        unselectedTextColor = GeoNavTextInactive
                    ),
                    modifier = Modifier.testTag("nav_tab_home")
                )
                // Tab Directory
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Directory", modifier = Modifier.size(24.dp)) },
                    label = { Text(tabDirLabel, fontSize = 11.sp, fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GeoNavTextActive,
                        selectedTextColor = GeoNavTextActive,
                        indicatorColor = GeoNavIndicator,
                        unselectedIconColor = GeoNavTextInactive,
                        unselectedTextColor = GeoNavTextInactive
                    ),
                    modifier = Modifier.testTag("nav_tab_directory")
                )
                // Tab Bookings
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Bookings", modifier = Modifier.size(24.dp)) },
                    label = { Text(tabBookingsLabel, fontSize = 11.sp, fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GeoNavTextActive,
                        selectedTextColor = GeoNavTextActive,
                        indicatorColor = GeoNavIndicator,
                        unselectedIconColor = GeoNavTextInactive,
                        unselectedTextColor = GeoNavTextInactive
                    ),
                    modifier = Modifier.testTag("nav_tab_bookings")
                )
                // Tab Locker
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Health Locker", modifier = Modifier.size(24.dp)) },
                    label = { Text(tabLockerLabel, fontSize = 11.sp, fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GeoNavTextActive,
                        selectedTextColor = GeoNavTextActive,
                        indicatorColor = GeoNavIndicator,
                        unselectedIconColor = GeoNavTextInactive,
                        unselectedTextColor = GeoNavTextInactive
                    ),
                    modifier = Modifier.testTag("nav_tab_locker")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // App Bar section
            HeaderSection(
                locationText = locationText,
                currentLanguage = currentLanguage,
                onLangChange = { currentLanguage = it }
            )

            // Dynamic Body based on Current Tab with slide-in animations
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    slideInHorizontally { width -> if (targetState > initialState) width else -width } togetherWith
                            slideOutHorizontally { width -> if (targetState > initialState) -width else width }
                },
                label = "tabChange"
            ) { targetTab ->
                when (targetTab) {
                    0 -> HomeTabScreen(
                        searchTextHint = searchTextHint,
                        emergencyTitle = emergencyTitle,
                        emergencySub = emergencySub,
                        realTimeHeading = realTimeHeading,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        voiceSearchPulsing = voiceSearchPulsing,
                        onVoiceSearchPress = {
                            coroutineScope.launch {
                                voiceSearchPulsing = true
                                Toast.makeText(context, "Sensing voice input in regional dialect...", Toast.LENGTH_SHORT).show()
                                delay(2200)
                                voiceSearchPulsing = false
                                searchQuery = "ICU beds AIIMS"
                                Toast.makeText(context, "Recognized: \"ICU beds AIIMS\"", Toast.LENGTH_LONG).show()
                            }
                        },
                        onSosClick = { showSosModal = true },
                        hospitalGeneralAvailableCount = hospitalsList.sumOf { it.generalBeds },
                        hospitalIcuAvailableCount = hospitalsList.sumOf { it.icuBeds },
                        hospitalOxygenAvailableCount = hospitalsList.sumOf { it.oxygenBeds },
                        onQuickBedFilterClick = { bedType ->
                            directoryBedTypeFilter = bedType
                            currentTab = 1 // Switch to directory automatically
                        },
                        onNavigateToService = { serviceIndex ->
                            if (serviceIndex == 1) {
                                // Health Locker Quick Link
                                currentTab = 3
                            } else if (serviceIndex == 0) {
                                // OPD Quick Link
                                directoryBedTypeFilter = "All"
                                currentTab = 1
                            } else {
                                // Triggers corresponding overlay state on Home tab
                                if (serviceIndex == 2) {
                                    triageSymptomIndex = null // resets symptom panel to Blood Bank overview
                                    Toast.makeText(context, "Scroll down to check Live Blood Availability", Toast.LENGTH_SHORT).show()
                                } else if (serviceIndex == 3) {
                                    // Direct open AI Triage Chatbot
                                    triageChatbotInitialQuery = null
                                    isTriageChatbotOpen = true
                                }
                            }
                        },
                        onOpenChatbot = { query ->
                            triageChatbotInitialQuery = query
                            isTriageChatbotOpen = true
                        },
                        triageSelectedSymptom = triageSymptomIndex,
                        onSymptomSelect = { triageSymptomIndex = it },
                        bloodBanks = bloodBanksList,
                        voluntaryDonor = voluntaryDonorRegistered,
                        onRegisterDonor = {
                            voluntaryDonorRegistered = true
                            Toast.makeText(context, "Registered successfully to National Voluntary Red Cross network!", Toast.LENGTH_LONG).show()
                        }
                    )
                    1 -> DirectoryTabScreen(
                        hospitals = hospitalsList,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        bedTypeFilter = directoryBedTypeFilter,
                        onBedTypeFilterChange = { directoryBedTypeFilter = it },
                        abPmjayFilter = abPmjayOnlyFilter,
                        onAbPmjayFilterChange = { abPmjayOnlyFilter = it },
                        onVerifyClick = { showVerifyDialog.value = it },
                        onCostCalcClick = { showCostDialog.value = it },
                        onBookOpdClick = { hospital ->
                            val newId = "bOpd${myBookingsList.size + 1}"
                            val userBooked = Booking(
                                id = newId,
                                type = "OPD",
                                mainDetail = "General OPD Token for Consultation",
                                secondaryDetail = "Token: BK-${100 + myBookingsList.size} | Ayushman PMJAY scheme applied",
                                hospitalName = hospital.name,
                                liveToken = "BK-${100 + myBookingsList.size}",
                                queueStatus = "3 Patients Ahead",
                                waitTimeMin = 12,
                                status = "Confirmed"
                            )
                            myBookingsList.add(userBooked)
                            currentTab = 2 // Switch tab to Bookings and show success
                            Toast.makeText(context, "OPD Appointment Booked at ${hospital.name}!", Toast.LENGTH_LONG).show()
                        }
                    )
                    2 -> BookingsTabScreen(
                        bookings = myBookingsList,
                        onCancelBooking = { id ->
                            myBookingsList.removeAll { it.id == id }
                            Toast.makeText(context, "Appointment booking retracted.", Toast.LENGTH_SHORT).show()
                        }
                    )
                    3 -> LockerAndSettingsScreen(
                        name = lockerName,
                        onNameChange = { lockerName = it },
                        age = lockerAge,
                        onAgeChange = { lockerAge = it },
                        bloodGroup = lockerBloodGroup,
                        onBloodChange = { lockerBloodGroup = it },
                        aadhar = lockerAadhar,
                        onAadharChange = { lockerAadhar = it },
                        pmjayAccept = lockerPmjayStatus,
                        onPmjayAcceptChange = { lockerPmjayStatus = it },
                        voluntaryRegistered = voluntaryDonorRegistered,
                        onToggleVoluntaryRegister = { voluntaryDonorRegistered = it }
                    )
                }
            }
        }
    }

    // Emergency SOS Modal
    if (showSosModal) {
        Dialog(onDismissRequest = { showSosModal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GeoEmergencyBg),
                border = BorderStroke(2.dp, GeoEmergencyBorder),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(16.dp).testTag("sos_alert_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(GeoEmergencyAccent, CircleShape)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "CRITICAL EMERGENCY GPS DISTRESS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GeoEmergencyAccent,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Nearest Vacant ICU identified at AIIMS Delhi (1.2 km).",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = GeoEmergencyText,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "8 ICU Beds currently verified by system crowdsourcing.",
                        fontSize = 12.sp,
                        color = GeoEmergencySubtext,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated Ambulance Tracking List in Dialogue
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        border = BorderStroke(1.dp, GeoEmergencyBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Live Dispatch Network:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = GeoTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            liveAmbulances.forEach { amb ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(amb.serviceName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GeoTextPrimary)
                                        Text("${amb.type} • ${amb.distanceMin} mins away", fontSize = 10.sp, color = GeoNavTextInactive)
                                    }
                                    Button(
                                        onClick = {
                                            val newDispatched = Booking(
                                                id = "bAmbulance${myBookingsList.size}",
                                                type = "Ambulance",
                                                mainDetail = "${amb.serviceName} Dispatch",
                                                secondaryDetail = "Assigned Vehicle: DL-1LC-3419 (${amb.type})",
                                                hospitalName = "AIIMS New Delhi Emergency Room",
                                                liveToken = "DISPATCHED",
                                                queueStatus = "En Route - GPS Activated",
                                                waitTimeMin = amb.distanceMin,
                                                status = "En Route"
                                            )
                                            myBookingsList.add(newDispatched)
                                            showSosModal = false
                                            currentTab = 2 // Go to bookings
                                            Toast.makeText(context, "${amb.serviceName} Dispatched immediately to your GPS location!", Toast.LENGTH_LONG).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GeoEmergencyAccent),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text("Dispatch", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { showSosModal = false },
                            border = BorderStroke(1.dp, GeoEmergencyAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoEmergencyAccent),
                            modifier = Modifier.weight(1f).padding(end = 4.dp).height(48.dp)
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "Placing Emergency Red Dial: ${hospitalsList[0].contact}", Toast.LENGTH_SHORT).show()
                                showSosModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GeoEmergencyAccent),
                            modifier = Modifier.weight(1f).padding(start = 4.dp).height(48.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Phone, contentDescription = "Dial", modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call ICU Desk", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // Cost Calculator Dialog
    val clickedCostHospital = showCostDialog.value
    if (clickedCostHospital != null) {
        Dialog(onDismissRequest = { showCostDialog.value = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFC0C9C8)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(16.dp).testTag("cost_calculator_dialog")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Cost Transparency Calculator",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = GeoPrimaryTeal
                    )
                    Text(
                        text = clickedCostHospital.name,
                        fontSize = 13.sp,
                        color = GeoTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Scheme Details
                    if (clickedCostHospital.acceptsAbPmjay) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE2F3E3)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Eligible", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ayushman Bharat PMJAY Eligible (100% Free Treatment up to ₹5 Lakhs for cover holders)", fontSize = 10.sp, color = Color(0xFF1B5E20), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2CC)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Private Non-Govt Rates: Requires Private TPAs or Out-of-Pocket pays.", fontSize = 10.sp, color = Color(0xFF6E5600))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // General Cost Ticker
                    CostItemRow("OPD Registration Consultation Fee", if (clickedCostHospital.opdFee == 10) "₹10 (Subsidized)" else "₹${clickedCostHospital.opdFee}")
                    CostItemRow("General Ward Stay (Per day)", if (clickedCostHospital.wardRentPerDay == 0) "Free (Govt Cover)" else "₹${clickedCostHospital.wardRentPerDay}")
                    CostItemRow("ICU Stay with Ventilation (Per day)", if (clickedCostHospital.icuRentPerDay == 0) "Free (Govt Cover)" else "₹${clickedCostHospital.icuRentPerDay}")
                    CostItemRow("Standard Diagnostic X-Ray/Panel", "₹${clickedCostHospital.diagnosticTestCost}")

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "* No-surprise guarantee: Hospital bound to declare standard tariffs under Central Clinical Establishments Rules, India.",
                        fontSize = 11.sp,
                        color = GeoTextSecondary,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showCostDialog.value = null },
                        colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryTeal),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            }
        }
    }

    // Crowdsourced Verification Dialog
    val clickedVerifyHospital = showVerifyDialog.value
    if (clickedVerifyHospital != null) {
        Dialog(onDismissRequest = { showVerifyDialog.value = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFC0C9C8)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(16.dp).testTag("verification_dialog")
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Crowdsourced Live Verification",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = GeoPrimaryTeal,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = clickedVerifyHospital.name,
                        fontSize = 13.sp,
                        color = GeoTextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Real-time verification prevents stale data! Does ${clickedVerifyHospital.name} currently have [${clickedVerifyHospital.icuBeds} ICU / ${clickedVerifyHospital.oxygenBeds} Oxygen] beds vacant?",
                        fontSize = 12.sp,
                        color = GeoTextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Thumbs down
                        Button(
                            onClick = {
                                val idx = hospitalsList.indexOfFirst { it.id == clickedVerifyHospital.id }
                                if (idx != -1) {
                                    val h = hospitalsList[idx]
                                    hospitalsList[idx] = h.copy(
                                        rating = h.rating - 1,
                                        icuBeds = (h.icuBeds - 1).coerceAtLeast(0)
                                    )
                                }
                                Toast.makeText(context, "Reported as stale. Live bed capacity marked for rapid audit!", Toast.LENGTH_LONG).show()
                                showVerifyDialog.value = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDAD6)),
                            modifier = Modifier.weight(1f).padding(end = 6.dp).height(48.dp)
                        ) {
                            Text("No, Stale", color = Color(0xFF93000A), fontSize = 12.sp)
                        }

                        // Thumbs up
                        Button(
                            onClick = {
                                val idx = hospitalsList.indexOfFirst { it.id == clickedVerifyHospital.id }
                                if (idx != -1) {
                                    val h = hospitalsList[idx]
                                    hospitalsList[idx] = h.copy(rating = h.rating + 1)
                                }
                                Toast.makeText(context, "Thank you! Live status verified - metadata timestamp updated to Just Now.", Toast.LENGTH_LONG).show()
                                showVerifyDialog.value = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2F3E3)),
                            modifier = Modifier.weight(1f).padding(start = 6.dp).height(48.dp)
                        ) {
                            Text("Yes, Accurate", color = Color(0xFF1B5E20), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CostItemRow(label: String, cost: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = GeoTextSecondary, modifier = Modifier.weight(0.7f))
        Text(cost, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GeoTextPrimary, modifier = Modifier.weight(0.3f), textAlign = TextAlign.End)
    }
}

// Subcomponents
@Composable
fun HeaderSection(
    locationText: String,
    currentLanguage: String,
    onLangChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "LOCATION IN INDIA",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = GeoTextSecondary,
                modifier = Modifier.testTag("location_label")
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location Pin",
                    tint = GeoPrimaryTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = locationText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GeoTextPrimary
                )
            }
        }

        // Segmented Language buttons
        Row(
            modifier = Modifier
                .background(Color(0xFFE0E0E0), RoundedCornerShape(20.dp))
                .padding(2.dp)
        ) {
            val languages = listOf("EN", "HN", "BN")
            languages.forEach { lang ->
                val isSelected = currentLanguage == lang
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) GeoPrimaryTeal else Color.Transparent)
                        .clickable { onLangChange(lang) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = lang,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun HomeTabScreen(
    searchTextHint: String,
    emergencyTitle: String,
    emergencySub: String,
    realTimeHeading: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    voiceSearchPulsing: Boolean,
    onVoiceSearchPress: () -> Unit,
    onSosClick: () -> Unit,
    hospitalGeneralAvailableCount: Int,
    hospitalIcuAvailableCount: Int,
    hospitalOxygenAvailableCount: Int,
    onQuickBedFilterClick: (String) -> Unit,
    onNavigateToService: (Int) -> Unit,
    onOpenChatbot: (String?) -> Unit,
    triageSelectedSymptom: Int?,
    onSymptomSelect: (Int) -> Unit,
    bloodBanks: List<BloodBank>,
    voluntaryDonor: Boolean,
    onRegisterDonor: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search & Voice Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFC0C9C8)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = GeoTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text(searchTextHint, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f).testTag("home_search_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // Multilingual Voice Search Pulsing Controller
                    IconButton(
                        onClick = onVoiceSearchPress,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (voiceSearchPulsing) GeoEmergencyBg else GeoPrimaryTealContainer,
                                CircleShape
                            ).testTag("voice_search_mic")
                    ) {
                        Icon(
                            imageVector = if (voiceSearchPulsing) Icons.Default.Warning else Icons.Default.Warning,
                            contentDescription = "Voice dictation",
                            tint = if (voiceSearchPulsing) GeoEmergencyAccent else GeoPrimaryTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 1-Click GPS SOS Emergency Button
        item {
            Card(
                onClick = onSosClick,
                colors = CardDefaults.cardColors(containerColor = GeoEmergencyBg),
                border = BorderStroke(1.dp, GeoEmergencyBorder),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sos_trigger_banner")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(GeoEmergencyAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Emergency SOS",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = emergencyTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = GeoEmergencyText
                            )
                            Text(
                                text = emergencySub,
                                fontSize = 12.sp,
                                color = GeoEmergencySubtext,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.6f), CircleShape)
                            .padding(8.dp)
                    ) {
                        Text(
                            "CLICK SOS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = GeoEmergencyAccent
                        )
                    }
                }
            }
        }

        // Live Bed Availability Counter Row (Interlocks with Directory)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = realTimeHeading,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GeoTextPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GeoPrimaryTealContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "LIVE UPDATES",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = GeoPrimaryTeal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // General
                    BedCounterPill(
                        title = "General",
                        count = hospitalGeneralAvailableCount,
                        modifier = Modifier.weight(1f).fillMaxHeight().testTag("quick_filter_general"),
                        onClick = { onQuickBedFilterClick("General") }
                    )
                    // ICU
                    BedCounterPill(
                        title = "Vacant ICU",
                        count = hospitalIcuAvailableCount,
                        modifier = Modifier.weight(1f).fillMaxHeight().testTag("quick_filter_icu"),
                        onClick = { onQuickBedFilterClick("ICU") },
                        isCritical = true
                    )
                    // Oxygen
                    BedCounterPill(
                        title = "Oxygen Support",
                        count = hospitalOxygenAvailableCount,
                        modifier = Modifier.weight(1f).fillMaxHeight().testTag("quick_filter_oxygen"),
                        onClick = { onQuickBedFilterClick("Oxygen") }
                    )
                }
            }
        }

        // Services Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "HEALTHCARE CONVENIENCE MODULES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GeoTextSecondary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // OPD Bed
                    ServiceGridCard(
                        title = "OPD Appointment",
                        subtitle = "Live token wait-times",
                        colorBg = GeoOpdCardBg,
                        colorBorder = GeoOpdCardBorder,
                        iconBg = GeoPrimaryTeal,
                        textColor = GeoOpdText,
                        subColor = GeoOpdSubtext,
                        icon = Icons.Default.DateRange,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToService(0) }
                    )

                    // Health Locker
                    ServiceGridCard(
                        title = "Health Locker",
                        subtitle = "Secure QR Access",
                        colorBg = GeoLockerCardBg,
                        colorBorder = GeoLockerCardBorder,
                        iconBg = GeoNavTextInactive,
                        textColor = GeoTextPrimary,
                        subColor = GeoTextSecondary,
                        icon = Icons.Default.Lock,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToService(1) }
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Blood Finder
                    ServiceGridCard(
                        title = "Blood Finder",
                        subtitle = "Live Delhi Bank Status",
                        colorBg = GeoBloodCardBg,
                        colorBorder = GeoBloodCardBorder,
                        iconBg = GeoBloodAccent,
                        textColor = GeoBloodText,
                        subColor = GeoBloodAccent,
                        icon = Icons.Default.Favorite,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToService(2) }
                    )

                    // AI Triage
                    ServiceGridCard(
                        title = "AI Symptom Triage",
                        subtitle = "Symptom evaluator",
                        colorBg = GeoTriageCardBg,
                        colorBorder = GeoTriageCardBorder,
                        iconBg = GeoTriageAccent,
                        textColor = GeoTriageText,
                        subColor = GeoTriageAccent,
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToService(3) }
                    )
                }
            }
        }

        // Conditional Section: Smart AI Symptom Triage Panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GeoTriageCardBg),
                border = BorderStroke(1.dp, GeoTriageCardBorder),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier.size(36.dp).background(GeoTriageAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Done, contentDescription = "AI Specialist", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Smart AI Symptom Checker & Specialist Triage", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GeoTriageText)
                                Text("Instant recommendation for emergency medical routing", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = GeoTextPrimary)
                            }
                        }

                        FilledTonalButton(
                            onClick = { onOpenChatbot(null) },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = GeoPrimaryTealContainer),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp).testTag("open_chatbot_header_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Chatbot", tint = GeoPrimaryTeal, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chatbot", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GeoPrimaryTeal)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Select major discomfort category:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GeoTextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))

                    val symptoms = listOf("Breathlessness & Chest Pain", "High Fever / Chills", "Abdominal Pain", "Migraine & Confusion")
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        symptoms.forEachIndexed { idx, sym ->
                            val selected = triageSelectedSymptom == idx
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selected) GeoTriageAccent else MaterialTheme.colorScheme.surface)
                                    .border(1.dp, if (selected) GeoTriageAccent else GeoTriageCardBorder, RoundedCornerShape(16.dp))
                                    .clickable { onSymptomSelect(idx) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(sym, fontSize = 11.sp, color = if (selected) Color.White else GeoTextPrimary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Outlined simulated Triage response
                    Crossfade(targetState = triageSelectedSymptom, label = "triageCross") { sIdx ->
                        when(sIdx) {
                            0 -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = GeoEmergencyBg),
                                    border = BorderStroke(1.dp, GeoEmergencyBorder),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("AI ANALYSIS: RED ALERT (CRITICAL ACTION)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GeoEmergencyAccent)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Cardiology / Pulmonology assessment is needed within 30 minutes. High risk of cardiovascular emergency.", fontSize = 11.sp, color = GeoEmergencyText)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Nearest Hospital: AIIMS New Delhi (1.2 km, 8 ICU Beds vacant).", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = GeoEmergencySubtext)
                                    }
                                }
                            }
                            1 -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7E6)),
                                    border = BorderStroke(1.dp, Color(0xFFFFE3B3)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("AI ANALYSIS: FEVER PROTOCOL (PEDIATRIC/GENERAL)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFD46200))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Possible Typhoid, Dengue, or Malaria. Monitor vitals. Non-critical but consult General Physician OPD within 12 hours.", fontSize = 11.sp, color = Color(0xFF5C2B00))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Recommended Route: Schedule OPD Consultation under Ayushman scheme.", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF5C2B00))
                                    }
                                }
                            }
                            2 -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F7FB)),
                                    border = BorderStroke(1.dp, Color(0xFFD6E4EE)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("AI ANALYSIS: GASTROENTEROLOGY TRIAGE (YELLOW ALERT)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GeoTriageText)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Could indicate appendicitis or acute gastritis. If pain localized to right lower abdomen, report to surgery clinic immediately.", fontSize = 11.sp, color = GeoTextPrimary)
                                    }
                                }
                            }
                            3 -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("AI ANALYSIS: MILD SYMPTOMATIC ADVICE (GREEN ALERT)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GeoTextSecondary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Stress or dehydration induced migraine. Rest in dark room, maintain electrolytes, monitor blood pressure.", fontSize = 12.sp, color = GeoTextSecondary)
                                    }
                                }
                            }
                            else -> {
                                Text("Tip: Tap a symptom to trigger national clinical AI specialist triage recommendations directly based on Indian Hospital codes.", fontSize = 12.sp, color = GeoTextSecondary, lineHeight = 16.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Launch Full Chatbot Action
                    Button(
                        onClick = {
                            val prompt = if (triageSelectedSymptom != null && triageSelectedSymptom in symptoms.indices) {
                                symptoms[triageSelectedSymptom]
                            } else null
                            onOpenChatbot(prompt)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryTeal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp).testTag("start_ai_triage_chat_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Send, contentDescription = "Chat", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (triageSelectedSymptom != null && triageSelectedSymptom in symptoms.indices)
                                    "Chat with AI Doctor about ${symptoms[triageSelectedSymptom]}"
                                else
                                    "Open AI Symptom Triage Chatbot",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Live Blood Banks Directory section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFC0C9C8)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = "Blood Icon", tint = GeoBloodAccent, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Live Delhi Blood Bank Balances", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GeoTextPrimary)
                                Text("Voluntary red cross and emergency units available", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GeoTextPrimary)
                            }
                        }

                        if (!voluntaryDonor) {
                            OutlinedButton(
                                onClick = onRegisterDonor,
                                border = BorderStroke(1.dp, GeoBloodAccent),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoBloodAccent),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Register Donor", fontSize = 11.sp)
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE2F3E3)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Donating!", fontSize = 11.sp, color = Color(0xFF1B5E20), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    bloodBanks.forEach { bank ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .border(1.dp, Color(0xFFE9E9E9), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(0.5f)) {
                                Text(bank.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GeoTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${bank.area} • Verified ${bank.verifiedHoursAgo}h ago", fontSize = 11.sp, color = GeoTextSecondary)
                            }

                            // Horizontal list of blood counts
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(0.5f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BloodPill("A+", bank.aPositive)
                                BloodPill("B+", bank.bPositive)
                                BloodPill("O-", bank.oNegative, isRare = true)
                                BloodPill("O+", bank.oPositive)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BloodPill(group: String, count: Int, isRare: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isRare) Color(0xFFFFDAD6) else Color(0xFFF9F9F9)
        ),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, if (isRare) GeoBloodAccent else Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(group, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isRare) GeoBloodAccent else GeoTextSecondary)
            Text("$count u", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GeoTextPrimary)
        }
    }
}

@Composable
fun BedCounterPill(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isCritical: Boolean = false
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isCritical) GeoEmergencyBorder else Color(0xFFC0C9C8)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (count < 10) "0$count" else "$count",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCritical) GeoEmergencyAccent else GeoPrimaryTeal
            )
            Text(
                text = title.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCritical) GeoEmergencySubtext else GeoTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ServiceGridCard(
    title: String,
    subtitle: String,
    colorBg: Color,
    colorBorder: Color,
    iconBg: Color,
    textColor: Color,
    subColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = colorBg),
        border = BorderStroke(1.dp, colorBorder),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.height(130.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(20.dp))
            }

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = textColor,
                    lineHeight = 16.sp
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = subColor.copy(alpha = 0.8f),
                    lineHeight = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun DirectoryTabScreen(
    hospitals: List<Hospital>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    bedTypeFilter: String,
    onBedTypeFilterChange: (String) -> Unit,
    abPmjayFilter: Boolean,
    onAbPmjayFilterChange: (Boolean) -> Unit,
    onVerifyClick: (Hospital) -> Unit,
    onCostCalcClick: (Hospital) -> Unit,
    onBookOpdClick: (Hospital) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Upper search & toggles
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search specializations, name or areas... (e.g., Delhi, Saket)", fontSize = 12.sp) },
            prefix = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("directory_search_bar"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Bed Type Segmented Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val filters = listOf("All", "General", "ICU", "Oxygen")
            filters.forEach { filter ->
                val selected = bedTypeFilter == filter
                Card(
                    onClick = { onBedTypeFilterChange(filter) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) GeoPrimaryTeal else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, if (selected) GeoPrimaryTeal else Color(0xFFC0C9C8)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(32.dp).testTag("filter_btn_$filter")
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            filter,
                            fontSize = 10.sp,
                            color = if (selected) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // PMJAY acceptance toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFC0C9C8), RoundedCornerShape(12.dp))
                .clickable { onAbPmjayFilterChange(!abPmjayFilter) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Ayushman Bharat (AB-PMJAY) Covered", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GeoTextPrimary)
                    Text("Show only hospitals accepting government medical cards", fontSize = 11.sp, color = GeoTextSecondary)
                }
            }
            Switch(
                checked = abPmjayFilter,
                onCheckedChange = onAbPmjayFilterChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF2E7D32), checkedTrackColor = Color(0xFFC8E6C9)),
                modifier = Modifier.scale(0.8f).testTag("pmjay_filter_switch")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hospital list
        val filteredHospitals = hospitals.filter { hosp ->
            val matchesSearch = hosp.name.contains(searchQuery, ignoreCase = true) ||
                    hosp.address.contains(searchQuery, ignoreCase = true) ||
                    hosp.specializations.any { it.contains(searchQuery, ignoreCase = true) } ||
                    hosp.area.contains(searchQuery, ignoreCase = true)

            val matchesPmjay = if (abPmjayFilter) hosp.acceptsAbPmjay else true

            val matchesBed = when (bedTypeFilter) {
                "General" -> hosp.generalBeds > 0
                "ICU" -> hosp.icuBeds > 0
                "Oxygen" -> hosp.oxygenBeds > 0
                else -> true
            }

            matchesSearch && matchesPmjay && matchesBed
        }

        if (filteredHospitals.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, contentDescription = "No hospitals", tint = GeoTextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Indian hospitals match your search criteria.", fontWeight = FontWeight.Bold, color = GeoTextSecondary, fontSize = 14.sp)
                    Text("Try looking up a different area or specialization.", color = GeoTextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredHospitals) { hospital ->
                    HospitalListItemCard(
                        hospital = hospital,
                        onVerify = { onVerifyClick(hospital) },
                        onCostCalc = { onCostCalcClick(hospital) },
                        onBookOpd = { onBookOpdClick(hospital) }
                    )
                }
            }
        }
    }
}

@Composable
fun HospitalListItemCard(
    hospital: Hospital,
    onVerify: () -> Unit,
    onCostCalc: () -> Unit,
    onBookOpd: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().testTag("hospital_card_${hospital.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // General Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(0.7f)) {
                    Text(
                        hospital.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = GeoTextPrimary
                    )
                    Text(
                        hospital.address,
                        fontSize = 12.sp,
                        color = GeoTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Distance and Cost toggle indicator
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(0.3f)) {
                    Text("${hospital.distanceKm} km", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GeoPrimaryTeal)
                    if (hospital.acceptsAbPmjay) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE2F3E3)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text("Govt Subsidy", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Specialization labels
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                hospital.specializations.forEach { spec ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF2F2F2), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(spec, fontSize = 11.sp, color = GeoTextSecondary, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bed Availabilities details row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFCFCFC), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // General
                BedMiniIndicator("General", hospital.generalBeds, hospital.maxGeneralBeds)
                // ICU
                BedMiniIndicator("ICU vacant", hospital.icuBeds, hospital.maxIcuBeds, isIcu = true)
                // Oxygen
                BedMiniIndicator("Oxygen", hospital.oxygenBeds, hospital.maxOxygenBeds)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Crowdsource Verifier metrics and Rate trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Verified status", tint = Color(0xFF006A6A), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Verified by ${hospital.rating} citizens", fontSize = 10.sp, color = GeoPrimaryTeal, fontWeight = FontWeight.Bold)
                }

                Text(
                    "Verify Live Capacity",
                    fontSize = 10.sp,
                    color = GeoPrimaryTeal,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onVerify() }
                        .padding(4.dp)
                        .testTag("verify_action_${hospital.id}")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onCostCalc,
                    border = BorderStroke(1.dp, Color(0xFFC0C9C8)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoTextPrimary),
                    modifier = Modifier.weight(1f).padding(end = 4.dp).height(40.dp).testTag("calc_cost_${hospital.id}")
                ) {
                    Text("Tariffs & Costs", fontSize = 11.sp)
                }

                Button(
                    onClick = onBookOpd,
                    colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryTeal),
                    modifier = Modifier.weight(1f).padding(start = 4.dp).height(40.dp).testTag("book_opd_${hospital.id}")
                ) {
                    Text("Register OPD", fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun BedMiniIndicator(title: String, vacant: Int, max: Int, isIcu: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title.uppercase(), fontSize = 10.sp, color = GeoTextSecondary, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$vacant",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (vacant == 0 && isIcu) GeoEmergencyAccent else if (isIcu) GeoEmergencyAccent else GeoPrimaryTeal
            )
            Text(
                text = "/$max",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GeoTextPrimary
            )
        }
    }
}

@Composable
fun BookingsTabScreen(
    bookings: List<Booking>,
    onCancelBooking: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "YOUR ACTIVE MEDICAL BOOKINGS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = GeoTextSecondary,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (bookings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DateRange, contentDescription = "Empty", tint = GeoTextSecondary, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No ongoing treatments or ambulance bookings.", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GeoTextSecondary)
                    Text("Visit the Directory or SOS panel to register instant bookings.", fontSize = 12.sp, color = GeoTextSecondary)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                items(bookings) { booking ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (booking.type) {
                                "Ambulance" -> GeoEmergencyBg
                                "OPD" -> GeoOpdCardBg
                                "Lab" -> GeoTriageCardBg
                                else -> Color.White
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            when (booking.type) {
                                "Ambulance" -> GeoEmergencyBorder
                                "OPD" -> GeoOpdCardBorder
                                "Lab" -> GeoTriageCardBorder
                                else -> Color(0xFFC0C9C8)
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("booking_card_${booking.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                when(booking.type) {
                                                    "Ambulance" -> GeoEmergencyAccent
                                                    "OPD" -> GeoPrimaryTeal
                                                    else -> GeoTriageAccent
                                                },
                                                RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when(booking.type) {
                                                "Ambulance" -> Icons.Default.Warning
                                                "OPD" -> Icons.Default.DateRange
                                                "Lab" -> Icons.Default.CheckCircle
                                                else -> Icons.Default.Home
                                            },
                                            contentDescription = booking.type,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(booking.mainDetail, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GeoTextPrimary)
                                        Text(booking.hospitalName, fontSize = 12.sp, color = GeoTextSecondary)
                                    }
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        booking.status.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GeoPrimaryTeal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(booking.secondaryDetail, fontSize = 12.sp, color = GeoTextSecondary, fontWeight = FontWeight.Medium)

                            // Show live updates details for queuing token and time
                            if (booking.liveToken.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Queue Wait Duration: ~${booking.waitTimeMin} mins", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GeoTextPrimary)
                                    Text(booking.queueStatus, fontSize = 12.sp, color = GeoEmergencyAccent, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { onCancelBooking(booking.id) },
                                    border = BorderStroke(1.dp, GeoTextSecondary),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoTextSecondary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Retract / Cancel", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LockerAndSettingsScreen(
    name: String,
    onNameChange: (String) -> Unit,
    age: String,
    onAgeChange: (String) -> Unit,
    bloodGroup: String,
    onBloodChange: (String) -> Unit,
    aadhar: String,
    onAadharChange: (String) -> Unit,
    pmjayAccept: Boolean,
    onPmjayAcceptChange: (Boolean) -> Unit,
    voluntaryRegistered: Boolean,
    onToggleVoluntaryRegister: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Digital Health Locker
        Card(
            colors = CardDefaults.cardColors(containerColor = GeoLockerCardBg),
            border = BorderStroke(1.dp, GeoLockerCardBorder),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().testTag("health_locker_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = "Digital Locker", tint = GeoPrimaryTeal, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("24/7 Digital Health Locker (QR Code Access)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GeoTextPrimary)
                        Text("Save record for Instant QR Scan at ICU reception", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GeoTextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Canvas Drawn QR Code Representation! Beautiful.
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF9E9E9E), RoundedCornerShape(12.dp))
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(100.dp)) {
                        // Custom Geometric balance simulated QR matrix blocks
                        drawRect(Color.Black, Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(30f, 30f))
                        drawRect(Color.White, Offset(10f, 10f), size = androidx.compose.ui.geometry.Size(10f, 10f))

                        drawRect(Color.Black, Offset(70f, 0f), size = androidx.compose.ui.geometry.Size(30f, 30f))
                        drawRect(Color.White, Offset(80f, 10f), size = androidx.compose.ui.geometry.Size(10f, 10f))

                        drawRect(Color.Black, Offset(0f, 70f), size = androidx.compose.ui.geometry.Size(30f, 30f))
                        drawRect(Color.White, Offset(10f, 80f), size = androidx.compose.ui.geometry.Size(10f, 10f))

                        // Random bits
                        drawRect(Color.Black, Offset(40f, 20f), size = androidx.compose.ui.geometry.Size(10f, 10f))
                        drawRect(Color.Black, Offset(50f, 40f), size = androidx.compose.ui.geometry.Size(20f, 10f))
                        drawRect(Color.Black, Offset(20f, 50f), size = androidx.compose.ui.geometry.Size(10f, 20f))
                        drawRect(Color.Black, Offset(80f, 50f), size = androidx.compose.ui.geometry.Size(10f, 10f))
                        drawRect(Color.Black, Offset(50f, 80f), size = androidx.compose.ui.geometry.Size(20f, 20f))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Locker Owner: $name | Blood: $bloodGroup | Age: $age\nPMJAY Covered ID: ${if (pmjayAccept) "AB-54129210-DK" else "Ineligible (Private TPA)"}",
                    fontSize = 11.sp,
                    color = GeoTextPrimary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Profile details modifier fields
        val editFieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Modify Emergency Medical Card", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = editFieldColors,
                    textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = age,
                        onValueChange = onAgeChange,
                        label = { Text("Age", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = editFieldColors,
                        textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                    OutlinedTextField(
                        value = bloodGroup,
                        onValueChange = onBloodChange,
                        label = { Text("Blood Group", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = editFieldColors,
                        textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                }

                OutlinedTextField(
                    value = aadhar,
                    onValueChange = onAadharChange,
                    label = { Text("Aadhar No. (Last 4)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = editFieldColors,
                    textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Eligible for Ayushman Bharat (AB-PMJAY)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = pmjayAccept,
                        onCheckedChange = onPmjayAcceptChange,
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }
        }

        // Voluntary Blood Donor Registration panel
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFC0C9C8)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(0.7f)) {
                    Text("Voluntary Donor Registry", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GeoBloodAccent)
                    Text("Be flagged as a living life-saver for rare cases (A-, B-, O- groups) in Delhi NCR.", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GeoTextPrimary)
                }
                Switch(
                    checked = voluntaryRegistered,
                    onCheckedChange = { onToggleVoluntaryRegister(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = GeoBloodAccent, checkedTrackColor = Color(0xFFFFDAD6)),
                    modifier = Modifier.weight(0.3f).scale(0.8f)
                )
            }
        }
    }
}
