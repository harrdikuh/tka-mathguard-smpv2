
package com.example.tkamathguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Ink = Color(0xFF34345F)
private val Purple = Color(0xFF7667F5)
private val Cream = Color(0xFFFFFAF3)
private val Mint = Color(0xFFDDF8EA)
private val Pink = Color(0xFFFFE2EC)
private val Lemon = Color(0xFFFFF1B8)
private val Lavender = Color(0xFFECE7FF)
private val Sky = Color(0xFFE0F3FF)
private val Coral = Color(0xFFFF966E)
private val Muted = Color(0xFF77778E)

data class Question(
    val subject: String,
    val text: String,
    val options: List<String>,
    val answer: Int
)

private enum class Page { REGISTER, HOME, EXAM, RESULT, TEACHER }

class MainActivity : ComponentActivity() {
    private var examActive by mutableStateOf(false)
    private var leaveWarning by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MathGuardApp(
                leaveWarning = leaveWarning,
                onDismissWarning = { leaveWarning = false },
                onRequestWarning = { leaveWarning = true },
                onExamActiveChange = { examActive = it }
            )
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (examActive) leaveWarning = true
    }
}

@Composable
private fun MathGuardApp(
    leaveWarning: Boolean,
    onDismissWarning: () -> Unit,
    onRequestWarning: () -> Unit,
    onExamActiveChange: (Boolean) -> Unit
) {
    var page by remember { mutableStateOf(Page.REGISTER) }
    var studentName by remember { mutableStateOf("") }
    var school by remember { mutableStateOf("") }
    var studentClass by remember { mutableStateOf("") }
    var teacherPin by remember { mutableStateOf("123456") }

    var score by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var wrong by remember { mutableIntStateOf(0) }
    var violations by remember { mutableIntStateOf(0) }
    var syncStatus by remember { mutableStateOf("Menunggu hasil ujian") }

    val questions = remember { sampleQuestions() }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Purple,
            secondary = Coral,
            tertiary = Color(0xFF20AD83),
            background = Cream,
            surface = Color.White
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Cream
        ) {
            when (page) {
                Page.REGISTER -> RegisterScreen(
                    name = studentName,
                    onName = { studentName = it },
                    school = school,
                    onSchool = { school = it },
                    studentClass = studentClass,
                    onClass = { studentClass = it },
                    onContinue = {
                        if (
                            studentName.isNotBlank() &&
                            school.isNotBlank() &&
                            studentClass.isNotBlank()
                        ) {
                            page = Page.HOME
                        }
                    }
                )

                Page.HOME -> HomeScreen(
                    name = studentName,
                    school = school,
                    studentClass = studentClass,
                    onStart = {
                        onExamActiveChange(true)
                        page = Page.EXAM
                    },
                    onTeacher = { page = Page.TEACHER }
                )

                Page.EXAM -> ExamScreen(
                    questions = questions,
                    leaveWarning = leaveWarning,
                    onDismissWarning = onDismissWarning,
                    onRequestWarning = onRequestWarning,
                    onFinish = { c, w, v ->
                        onExamActiveChange(false)
                        correct = c
                        wrong = w
                        violations = v
                        score = c * 5
                        syncStatus = "Sedang mengirim hasil..."

                        CloudRepository.saveResult(
                            name = studentName,
                            school = school,
                            studentClass = studentClass,
                            score = score,
                            correct = c,
                            wrong = w,
                            violations = v
                        ) { _, message ->
                            syncStatus = message
                        }

                        page = Page.RESULT
                    }
                )

                Page.RESULT -> ResultScreen(
                    name = studentName,
                    score = score,
                    correct = correct,
                    wrong = wrong,
                    violations = violations,
                    syncStatus = syncStatus,
                    onHome = { page = Page.HOME }
                )

                Page.TEACHER -> TeacherScreen(
                    pin = teacherPin,
                    onPinChange = { teacherPin = it },
                    onBack = { page = Page.HOME }
                )
            }
        }
    }
}

@Composable
private fun PageFrame(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        content = content
    )
}

@Composable
private fun Brand() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Purple, RoundedCornerShape(17.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("🛡️", fontSize = 26.sp)
        }

        Column {
            Text(
                "MATHGUARD",
                color = Ink,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp,
                letterSpacing = 1.sp
            )
            Text(
                "Sahabat belajar SMP 🌈",
                color = Muted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun Header(
    eyebrow: String,
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            eyebrow.uppercase(),
            color = Purple,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp
        )
        Text(
            title,
            color = Ink,
            fontSize = 27.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 33.sp
        )
        Text(
            subtitle,
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
    }
}

@Composable
private fun Panel(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
            content = content
        )
    }
}

@Composable
private fun MainButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Purple)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun RegisterScreen(
    name: String,
    onName: (String) -> Unit,
    school: String,
    onSchool: (String) -> Unit,
    studentClass: String,
    onClass: (String) -> Unit,
    onContinue: () -> Unit
) {
    PageFrame {
        Spacer(Modifier.height(8.dp))
        Brand()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Lavender, RoundedCornerShape(28.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🌟  HALO, CALON JUARA!", color = Purple,
                    fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                Text(
                    "Belajar jadi\nlebih seru! 🎨",
                    color = Ink,
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Isi profilmu dulu, yuk. Kamu pasti bisa!",
                    color = Ink,
                    fontSize = 14.sp
                )
                Text("📚 ✏️ 🧠 ⭐", fontSize = 30.sp)
            }
        }

        Panel {
            Text(
                "👋 Kenalan dulu, yuk!",
                color = Ink,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Masukkan identitas sesuai data sekolah.",
                color = Muted,
                fontSize = 13.sp
            )

            OutlinedTextField(
                value = name,
                onValueChange = onName,
                label = { Text("Nama lengkap") },
                placeholder = { Text("Nama peserta") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = school,
                onValueChange = onSchool,
                label = { Text("Nama sekolah") },
                placeholder = { Text("Nama SMP") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = studentClass,
                onValueChange = onClass,
                label = { Text("Kelas") },
                placeholder = { Text("Contoh: 7A, 8B, atau 9C") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            MainButton(
                text = "Ayo, mulai! 🚀",
                onClick = onContinue
            )

            Text(
                "Nama, sekolah, dan kelas wajib diisi.",
                color = Muted,
                fontSize = 12.sp
            )
        }

        Text(
            "Dibuat untuk menemani perjuanganmu belajar 💜",
            color = Muted,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun HomeScreen(
    name: String,
    school: String,
    studentClass: String,
    onStart: () -> Unit,
    onTeacher: () -> Unit
) {
    PageFrame {
        Brand()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Purple, RoundedCornerShape(28.dp))
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "HAI, ${name.uppercase()}! 👋",
                    color = Lemon,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
                Text(
                    "Hari ini kamu\nbisa jadi lebih hebat!",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    lineHeight = 32.sp
                )
                Text(
                    "$school • Kelas $studentClass",
                    color = Color.White,
                    fontSize = 12.sp
                )
                Text("🌈 ⭐ 📚 ✨", fontSize = 28.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            StatTile("20", "Soal", Sky, Modifier.weight(1f))
            StatTile("60", "Menit", Lemon, Modifier.weight(1f))
            StatTile("2", "Mapel", Pink, Modifier.weight(1f))
        }

        Header(
            "Siap beraksi?",
            "Misi belajarmu 🎯",
            "Kerjakan soal dengan tenang. Baca setiap pertanyaan baik-baik."
        )

        Panel {
            SubjectRow("🔢", "Matematika", "10 soal pilihan ganda", Lavender)
            HorizontalDivider(color = Cream)
            SubjectRow("📖", "Bahasa Indonesia", "10 soal pilihan ganda", Mint)

            Spacer(Modifier.height(2.dp))

            MainButton(
                text = "Mulai ujian  →",
                onClick = onStart
            )
        }

        Panel(color = Lemon) {
            Text("💡 Tips kecil", fontWeight = FontWeight.ExtraBold,
                color = Ink, fontSize = 16.sp)
            Text(
                "Pastikan baterai cukup, cari tempat yang nyaman, dan tetap berada di aplikasi selama ujian.",
                color = Ink,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }

        OutlinedButton(
            onClick = onTeacher,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("⚙️  Panel guru", fontWeight = FontWeight.Bold)
        }

        Text(
            "☁️ Hasil ujian dikirim melalui layanan Firebase yang dikonfigurasi.",
            color = Muted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SubjectRow(
    emoji: String,
    title: String,
    subtitle: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(color, RoundedCornerShape(17.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 26.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Ink, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, color = Ink, fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold)
            Text(label, color = Ink, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ExamScreen(
    questions: List<Question>,
    leaveWarning: Boolean,
    onDismissWarning: () -> Unit,
    onRequestWarning: () -> Unit,
    onFinish: (Int, Int, Int) -> Unit
) {
    var index by remember { mutableIntStateOf(0) }
    var violations by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    var remaining by remember { mutableIntStateOf(60 * 60) }

    val answers: SnapshotStateList<Int> = remember {
        mutableStateListOf<Int>().apply {
            repeat(questions.size) { add(-1) }
        }
    }

    val currentFinished by rememberUpdatedState(finished)

    BackHandler(enabled = !finished) {
        onRequestWarning()
    }

    LaunchedEffect(leaveWarning) {
        if (leaveWarning) violations++
    }

    if (leaveWarning) {
        AlertDialog(
            onDismissRequest = onDismissWarning,
            icon = { Text("🔒", fontSize = 32.sp) },
            title = { Text("Ups, tetap fokus ya!") },
            text = {
                Text(
                    "Kamu mencoba meninggalkan layar ujian. " +
                    "Percobaan ini dicatat. Tekan tombol untuk kembali mengerjakan soal."
                )
            },
            confirmButton = {
                Button(onClick = onDismissWarning) {
                    Text("Saya mengerti")
                }
            },
            containerColor = Color.White
        )
    }

    LaunchedEffect(Unit) {
        while (remaining > 0 && !currentFinished) {
            delay(1000)
            remaining--
        }

        if (!currentFinished && remaining <= 1) {
            finished = true
            val c = answers.indices.count {
                answers[it] == questions[it].answer
            }
            onFinish(c, questions.size - c, violations)
        }
    }

    val q = questions[index]
    val answeredCount = answers.count { it >= 0 }

    PageFrame {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("📝 MISI UJIAN", color = Purple,
                    fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text("Semangat, kamu pasti bisa!", color = Muted,
                    fontSize = 11.sp)
            }

            Surface(
                shape = RoundedCornerShape(15.dp),
                color = if (remaining < 300) Pink else Lemon
            ) {
                Text(
                    "%02d:%02d".format(remaining / 60, remaining % 60),
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                    color = Ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Progres pengerjaan", color = Muted, fontSize = 12.sp)
                Text("$answeredCount/${questions.size} terjawab",
                    color = Purple, fontWeight = FontWeight.Bold,
                    fontSize = 12.sp)
            }

            LinearProgressIndicator(
                progress = { (index + 1f) / questions.size },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Purple,
        
