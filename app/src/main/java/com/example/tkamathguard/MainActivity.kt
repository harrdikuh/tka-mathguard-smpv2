
package com.example.tkamathguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Navy = Color(0xFF142744)
private val Blue = Color(0xFF2864E8)
private val Cyan = Color(0xFF27C4D9)
private val Background = Color(0xFFF4F7FC)
private val Ink = Color(0xFF192B45)
private val Muted = Color(0xFF7B899D)
private val Green = Color(0xFF159B72)

data class Question(
    val subject: String,
    val text: String,
    val options: List<String>,
    val answer: Int
)

private enum class Page {
    REGISTER, HOME, EXAM, RESULT, TEACHER
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Blue,
                    secondary = Cyan,
                    background = Background,
                    surface = Color.White
                )
            ) {
                MathGuardApp()
            }
        }
    }
}

@Composable
fun MathGuardApp() {
    var page by remember { mutableStateOf(Page.REGISTER) }
    var studentName by remember { mutableStateOf("") }
    var school by remember { mutableStateOf("") }
    var studentClass by remember { mutableStateOf("VII") }
    var selectedSubject by remember { mutableStateOf("Semua") }
    var questionIndex by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(3600) }
    var examQuestions by remember { mutableStateOf(emptyList<Question>()) }
    var answers by remember { mutableStateOf(emptyMap<Int, Int>()) }
    var warning by remember { mutableStateOf("") }
    var showSubmitDialog by remember { mutableStateOf(false) }

    val questions = remember { createQuestions() }

    LaunchedEffect(page) {
        while (page == Page.EXAM && remainingSeconds > 0) {
            delay(1000)
            if (page == Page.EXAM) {
                remainingSeconds--
            }
        }
        if (page == Page.EXAM && remainingSeconds == 0) {
            page = Page.RESULT
        }
    }

    fun startExam() {
        examQuestions = questions.shuffled()
        answers = emptyMap()
        questionIndex = 0
        remainingSeconds = 3600
        page = Page.EXAM
    }

    fun finishExam() {
        showSubmitDialog = false
        page = Page.RESULT
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            if (page != Page.REGISTER) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Navy)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Cyan, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", color = Navy, fontSize = 28.sp,
                            fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("TKA MathGuard", color = Color.White,
                            fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        Text("SMP • Ujian lebih terarah",
                            color = Color(0xFFB9D4F5), fontSize = 11.sp)
                    }
                    if (page == Page.HOME) {
                        Text(
                            "Guru",
                            color = Color.White,
                            modifier = Modifier
                                .background(Color(0xFF304563), RoundedCornerShape(12.dp))
                                .clickable { page = Page.TEACHER }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (page == Page.HOME) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text("⌂  Beranda", color = Blue, fontWeight = FontWeight.Bold)
                    Text("▤  Ujian", color = Muted,
                        modifier = Modifier.clickable { startExam() })
                    Text("◉  Profil", color = Muted,
                        modifier = Modifier.clickable { page = Page.REGISTER })
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (page) {
                Page.REGISTER -> RegisterScreen(
                    name = studentName,
                    onNameChange = { studentName = it },
                    school = school,
                    onSchoolChange = { school = it },
                    studentClass = studentClass,
                    onClassChange = { studentClass = it },
                    onContinue = {
                        if (studentName.isNotBlank() && school.isNotBlank()) {
                            page = Page.HOME
                        } else {
                            warning = "Isi nama dan nama sekolah terlebih dahulu."
                        }
                    },
                    warning = warning
                )

                Page.HOME -> HomeScreen(
                    name = studentName,
                    school = school,
                    studentClass = studentClass,
                    selectedSubject = selectedSubject,
                    onSubjectChange = { selectedSubject = it },
                    onStart = { startExam() }
                )

                Page.EXAM -> {
                    val q = examQuestions.getOrNull(questionIndex)
                    if (q != null) {
                        ExamScreen(
                            question = q,
                            index = questionIndex,
                            total = examQuestions.size,
                            answers = answers,
                            seconds = remainingSeconds,
                            onAnswer = { choice ->
                                answers = answers + (questionIndex to choice)
                            },
                            onPrevious = {
                                if (questionIndex > 0) questionIndex--
                            },
                            onNext = {
                                if (questionIndex < examQuestions.lastIndex) {
                                    questionIndex++
                                }
                            },
                            onJump = { questionIndex = it },
                            onSubmit = { showSubmitDialog = true }
                        )
                    }
                }

                Page.RESULT -> {
                    val correct = answers.count { (index, choice) ->
                        examQuestions.getOrNull(index)?.answer == choice
                    }
                    ResultScreen(
                        name = studentName,
                        correct = correct,
                        total = examQuestions.size,
                        answered = answers.size,
                        onHome = { page = Page.HOME },
                        onRetry = { startExam() }
                    )
                }

                Page.TEACHER -> TeacherScreen(
                    onBack = { page = Page.HOME }
                )
            }
        }
    }

    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("Kumpulkan ujian?") },
            text = {
                Text(
                    "Jawaban terisi ${answers.size} dari ${examQuestions.size} soal. " +
                    "Pastikan kamu siap mengakhiri ujian."
                )
            },
            confirmButton = {
                TextButton(onClick = { finishExam() }) {
                    Text("Ya, kumpulkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text("Periksa lagi")
                }
            }
        )
    }
}

@Composable
private fun RegisterScreen(
    name: String,
    onNameChange: (String) -> Unit,
    school: String,
    onSchoolChange: (String) -> Unit,
    studentClass: String,
    onClassChange: (String) -> Unit,
    onContinue: () -> Unit,
    warning: String
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))
        Box(
            Modifier
                .size(90.dp)
                .background(Navy, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", fontSize = 52.sp, color = Cyan,
                fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(20.dp))
        Text("TKA MathGuard", fontSize = 29.sp,
            fontWeight = FontWeight.ExtraBold, color = Navy)
        Text("Teman ujianmu, penjaga fokusmu.",
            color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(32.dp))
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(Modifier.padding(22.dp)) {
                Text("Kenalan dulu, yuk! 👋",
                    fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("Lengkapi identitas sebelum memulai ujian.",
                    color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Nama lengkap") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = school,
                    onValueChange = onSchoolChange,
                    label = { Text("Nama sekolah") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Text("Kelas", fontWeight = FontWeight.SemiBold, color = Ink)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("VII", "VIII", "IX").forEach { grade ->
                        val active = studentClass == grade
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onClassChange(grade) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) Blue else Background
                        ) {
                            Text(
                                grade,
                                modifier = Modifier.padding(13.dp),
                                textAlign = TextAlign.Center,
                                color = if (active) Color.White else Ink,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (warning.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(warning, color = Color.Red, fontSize = 12.sp)
                }
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Text("Masuk ke beranda  →", fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("TKA MathGuard SMP • Versi 1.0",
            color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun HomeScreen(
    name: String,
    school: String,
    studentClass: String,
    selectedSubject: String,
    onSubjectChange: (String) -> Unit,
    onStart: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text("Halo, ${name.ifBlank { "Pejuang TKA" }}! 👋",
                fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Navy)
            Text("$school • Kelas $studentClass", color = Muted, fontSize = 13.sp)
        }
        item {
            Surface(
                color = Navy,
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(22.dp)) {
                    Text("SIAPKAN DIRIMU", color = Cyan,
                        fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Satu langkah lebih dekat menuju hasil terbaik.",
                        color = Color.White, fontSize = 22.sp,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Fokus • Teliti • Percaya diri",
                        color = Color(0xFFCAD9EE), fontSize = 13.sp)
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoChip("20 Soal")
                        InfoChip("60 Menit")
                        InfoChip("2 Mapel")
                    }
                }
            }
        }
        item {
            Text("Ringkasan ujian", fontSize = 18.sp,
                fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SubjectCard("🔢", "Matematika", "10 soal",
                    Modifier.weight(1f), Color(0xFFEAF0FF))
                SubjectCard("📚", "B. Indonesia", "10 soal",
                    Modifier.weight(1f), Color(0xFFE5F9F5))
            }
        }
        item {
            Text("Pilih paket ujian", fontSize = 18.sp,
                fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(8.dp))
            listOf("Semua", "Matematika", "Bahasa Indonesia").forEach { subject ->
                val active = selectedSubject == subject
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSubjectChange(subject) },
                    color = if (active) Color(0xFFE7EEFF) else Color.White,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, if (active) Blue else Color(0xFFE8EDF5)
                    )
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(subject, modifier = Modifier.weight(1f),
                            color = Ink, fontWeight = FontWeight.SemiBold)
                        if (active) Text("✓", color = Blue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Mulai ujian sekarang  →", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold)
            }
            Text("Pastikan koneksi dan perangkatmu siap.",
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = TextAlign.Center, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(color = Color(0xFF304563), shape = RoundedCornerShape(10.dp)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SubjectCard(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    color: Color
) {
    Surface(modifier, color = color, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(emoji, fontSize = 27.sp)
            Spacer(Modifier.height(10.dp))
            Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ExamScreen(
    question: Question,
    index: Int,
    total: Int,
    answers: Map<Int, Int>,
    seconds: Int,
    onAnswer: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onJump: (Int) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("UJIAN BERLANGSUNG", color = Blue,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Soal ${index + 1} dari $total", color = Navy,
                    fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            Surface(
                color = if (seconds < 300) Color(0xFFFFE9E7) else Color.White,
                shape = RoundedCornerShape(13.dp)
            ) {
                Text(
                    "%02d:%02d".format(seconds / 60, seconds % 60),
                    modifier = Modifier.padding(12.dp),
                    color = if (seconds < 300) Color.Red else Navy,
                    fontWeight = FontWeight.Bold, fontSize = 17.sp
                )
      
