# 🌸 Bliss – Mood Monitoring & Mental Wellness Mobile Application

> WIA2007 Mobile Application Development  
> Semester 1 Session 2025/2026  
> MAD SUPER GROUP  

Bliss is a native Android application developed to support **SDG 3 – Good Health and Well-being (Target 3.4)** by promoting emotional awareness, stress management, and healthy self-care habits among students and young adults.

---

# 📌 Project Overview

Bliss provides a structured emotional support ecosystem that integrates:

- Mood Tracking  
- AI-assisted Journal Analysis  
- Mood-based Music Recommendation  
- Relaxation & Goal Tracking  
- Emotional Analytics Dashboard  
- Emergency Mental Health Support  

The system focuses on **preventive mental health care** through consistent reflection and emotional pattern recognition.

---

# 🛠 Technical Implementation Overview

## 🔹 Development Stack

| Layer | Technology |
|-------|------------|
| Platform | Android (Native) |
| Language | Java |
| UI | XML Layout System |
| Database | Firebase Firestore |
| Authentication | Firebase Authentication |
| Media Storage | Cloudinary |
| Notifications | Android Notification API |
| Testing | User Acceptance Testing (UAT) |

---

## 🔹 Google Technologies Used

### 1️⃣ Firebase Authentication
- Secure login and registration  
- Password recovery  
- Session management  
- Protection of sensitive emotional data  

### 2️⃣ Firebase Firestore
- Cloud-based NoSQL database  
- Real-time data synchronization  
- Stores:
  - Users  
  - Mood history  
  - Journal entries  
  - Goals  
- Timestamp-based querying for weekly summaries  
- Asynchronous listener architecture  

### 3️⃣ Firebase Cloud Messaging
- Daily 8:00 PM mood reminder  
- Encourages consistent habit building  

### 4️⃣ Android Notification System
- Scheduled reminders  
- Background notification management  

---

# 🏗 System Architecture

Bliss follows a modular architecture to ensure scalability and maintainability:

```
app/src/main/java/com/example/

├── bliss/               # Authentication & Profile
├── music/               # Mood Tracking & Recommendation
├── mooddistribution/    # Analytics & Visualization
├── relaxation/          # Meditation & Goal Tracking
├── chatbox/             # AI Chatbot
├── support/             # Emergency Support
└── model/               # Data Models
```

### Architecture Goals
- Separation of concerns  
- Modular feature isolation  
- Clean Firestore data structure  
- Lifecycle-aware activity handling  

---

# ⚙ Core Implementation Details

## 1️⃣ Mood Tracking System

- Emoji-based mood selection  
- Stored with timestamp in Firestore  
- Weekly mood summaries generated dynamically  
- Mood distribution visualized via charts  
- RecyclerView used for history display  

---

## 2️⃣ Journal with AI Mood Detection

- Users create daily journal entries  
- Text analyzed for emotional keywords  
- AI generates personalized suggestions  
- Calendar-based journal retrieval  
- Media stored via Cloudinary URLs  

---

## 3️⃣ Mood-Based Music Recommendation

- Songs tagged with mood category  
- Firestore filtered query by mood  
- Audio streamed via Cloudinary  
- Optimized app size via external hosting  

---

## 4️⃣ Relaxation & Goal Tracking

- Guided meditation with timer  
- Interactive breathing animation  
- Goal tracking including:
  - Target count  
  - Completion state  
  - Progress percentage  
- Visual progress feedback  

---

## 5️⃣ AI Chatbot

- Rule-based emotional response system  
- Detects stress-related keywords  
- Suggests coping strategies  
- Provides crisis resource recommendations  
- Includes ethical disclaimer  

---

# 🌟 Innovation Highlights

### 🔹 Integrated Emotional Ecosystem
Bliss combines:
- Tracking  
- Analysis  
- Reflection  
- Relaxation  
- Crisis support  

All within one structured platform.

---

### 🔹 AI-Enhanced Emotional Reflection
Journal entries are actively analyzed to provide personalized well-being suggestions.

---

### 🔹 Preventive Mental Health Model
Encourages:
- Early emotional awareness  
- Pattern recognition  
- Habit formation  
- Proactive self-care  

---

### 🔹 Privacy-Centered Design
- No public sharing features  
- Secure authentication  
- Protected Firestore rules  
- Crisis access without data exposure  

---

# ⚠ Challenges & Solutions

## 1️⃣ Asynchronous Firebase Data Handling

**Challenge:** Firestore retrieval caused delayed UI updates  

**Solution:**  
- Implemented proper listeners and callbacks  
- Updated UI after data loading completion  
- Managed lifecycle-aware data loading  

---

## 2️⃣ AI Mood Detection Limitations

**Challenge:** Keyword-based detection may misinterpret complex emotions  

**Solution:**  
- Expanded emotional keyword database  
- Added fallback suggestion logic  
- Included transparent disclaimer  

---

## 3️⃣ Media Optimization

**Challenge:** Large local audio files increased APK size  

**Solution:**  
- Offloaded media to Cloudinary  
- Streamed audio via URL  
- Improved performance and scalability  

---

## 4️⃣ Data Consistency Across Modules

**Challenge:** Synchronizing mood history, journal detection, and analytics  

**Solution:**  
- Standardized timestamp usage  
- Unified mood string constants  
- Structured Firestore collections  

---

# 🧪 Testing Strategy

Bliss was evaluated using **User Acceptance Testing (UAT)**.

## Functional Testing
All modules validated:
- Authentication  
- Mood Tracking  
- Journal AI  
- Relaxation  
- Chatbot  
- Emergency Support  

All passed.

---

## Non-Functional Testing

| Aspect | Result |
|--------|--------|
| Performance | < 2 seconds loading time |
| Stability | No crashes observed |
| Usability | Rated 4–5/5 |
| AI Satisfaction | Rated 4–5/5 |
| Security | Firebase protected |

---

# 🔒 Security & Privacy

- Firebase Authentication  
- Encrypted data transmission  
- Firestore security rules  
- Secure password reset  
- Crisis hotline visibility  

Bliss does not replace professional therapy.

---

# 🌍 SDG Contribution

Bliss contributes to:

**SDG 3.4 – Promote mental health and well-being**

By:
- Increasing emotional awareness  
- Supporting stress reduction  
- Encouraging healthy routines  
- Providing crisis access  

---

# 👥 Team – MAD SUPER GROUP

- Tang Yong Chun – Project Manager  
- Chua Xin Yi – Software Architect  
- Ee Si Ying – Lead Developer  
- Joseph Lau Tiew Jung – Developer  
- Kuan Hui Min – UI/UX Designer  
- Selina Sia Hui Yi – Tester  

---

# 📌 Academic Purpose

This project was developed for academic evaluation under  
WIA2007 Mobile Application Development.
