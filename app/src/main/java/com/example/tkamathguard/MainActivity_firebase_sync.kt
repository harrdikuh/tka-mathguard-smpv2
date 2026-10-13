package com.example.tkamathguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Bg = Color(0xFFFFF8F0)
private val Purple = Color(0xFF7864D8)
private val Pink = Color(0xFFFFD9E5)
private val Green = Color(0xFFD8F5DF)
private val Ink = Color(0xFF302C46)

data class Question(
    val text: String,
    val options: List<String>,
    val answer: Int
)

private val sampleQuestions = listOf(
    Question("Hasil dari 25 + 37 adalah ...", listOf("52", "62", "72", "57"), 1),
    Question("Hasil dari 12 × 8 adalah ...", listOf("86", "94", "96", "108"), 2),
    Question("Hasil dari 144 ÷ 12 adalah ...", listOf("10", "11", "12", "14"), 2),
    Question("Jika x + 7 = 15, nilai x adalah ...", listOf("6", "7", "8", "9"), 2),
    Question("Hasil dari 3² + 4² adalah ...", listOf("12", "25", "49", "14"), 1),
    Question("25% dari 200 adalah ...", listOf("25", "40", "50", "75"), 2),
    Question("Keliling persegi dengan sisi 9 cm adalah ...", listOf("18 cm", "27 cm", "36 cm", "81 cm"), 2),
    Question("Luas persegi panjang 12 cm × 5 cm adalah ...", listOf("17 cm²", "34 cm²", "60 cm²", "120 cm²"), 2),
    Question("Rata-rata dari 6, 8, dan 10 adalah ...", listOf("7", "8", "9", "10"), 1),
    Question("Hasil dari -8 + 13 adalah ...", listOf("-5", "3", "5", "21"), 2),
    Question("Jika 2x = 18, nilai x adalah ...", listOf("6", "8", "9", "16"), 2),
    Question("Bentuk pecahan dari 0,75 adalah ...", listOf("1/2", "2/3", "3/4", "4/5"), 2),
    Question("Sudut siku-siku besarnya ...", listOf("45°", "60°", "90°", "180°"), 2),
    Question("Volume kubus dengan rusuk 4 cm adalah ...", listOf("16 cm³", "32 cm³", "48 cm³", "64 cm³"), 3),
    Question("FPB dari 12 dan 18 adalah ...", listOf("2", "3", "6", "9"), 2),
    Question("KPK dari 4 dan 6 adalah ...", listOf("8", "10", "12", "24"), 2),
    Question("Jika y = 3x dan x = 4, nilai y adalah ...", listOf("7", "9", "12", "16"), 2),
    Question("Perbandingan 2 : 3, jika jumlahnya 25, bagian pertama adalah ...", listOf("5", "10", "15", "20"), 1),
    Question("Luas segitiga dengan alas 10 cm dan tinggi 6 cm adalah ...", listOf("16 cm²", "30 cm²", "60 cm²", "36 cm²"), 1),
    Question("Peluang muncul angka genap pada dadu adalah ...", listOf("1/6", "1/3", "1/2", "2/3"), 2)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MathGuardApp()
            }
        }
    }
}

@Composable
fun MathGuardApp() {
    var name by remember { mutableStateOf("") }
    var school by remember { mutableStateOf("") }
    var studentClass by remember { mutableStateOf("") }
    var started by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var teacher by remember { mutableStateOf(false) }
    var questionIndex by remember { mutableStateOf(0) }
    var answers by remember { mutableStateOf(List(sampleQuestions.size) { -1 }) }
    var seconds by remember { mutableStateOf(3600) }
    var violations by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    var teacherPin by remember { mutableStateOf("1234") }
    var pinInput by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var scoreSaved by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf(listOf<Map<String, Any>>()) }

    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, started, finished) {
        val observer = LifecycleEventObserver { _, event ->
            if (started && !finished && event == Lifecycle.Event.ON_STOP) {
                violations++
                message = "Peringatan: kamu meninggalkan aplikasi ujian!"
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(started, finished) {
        while (started && !finished && seconds > 0) {
            delay(1000)
            seconds--
        }
        if (started && seconds <= 0 && !finished) {
            finished = true
        }
    }

    LaunchedEffect(teacher) {
        if (teacher) {
            db.collection("settings").document("teacher")
                .get()
                .addOnSuccessListener { document ->
                    teacherPin = document.getString("pin") ?: "1234"
                }
            db.collection("exam_results")
                .get()
                .addOnSuccessListener { snapshot ->
                    results = snapshot.documents.mapNotNull { it.data }
                }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Bg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("🌈 TKA MathGuard", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = Purple)
            Text("Teman ujian matematika SMP ✨", color = Ink)

            if (!started && !finished && !teacher) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Halo, pejuang matematika! 🐣", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("Isi identitasmu sebelum memulai ujian.")
                        OutlinedTextField(name, { name = it }, label = { Text("Nama siswa") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(school, { school = it }, label = { Text("Nama sekolah") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(studentClass, { studentClass = it }, label = { Text("Kelas") }, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = {
                                if (name.isNotBlank() && school.isNotBlank() && studentClass.isNotBlank()) {
                                    started = true
                                    message = ""
                                } else {
                                    message = "Lengkapi semua identitas terlebih dahulu."
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Purple)
                        ) {
                            Text("Mulai Ujian 🚀")
                        }
                        OutlinedButton(
                            onClick = { teacher = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Panel Guru 👩‍🏫")
                        }
                    }
                }
            }

            if (started && !finished) {
                Text("⏱️ Sisa waktu: %02d:%02d".format(seconds / 60, seconds % 60), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(
                    progress = { (questionIndex + 1).toFloat() / sampleQuestions.size },
                    modifier = Modifier.fillMaxWidth(),
                    color = Purple
                )
                Card(colors = CardDefaults.cardColors(containerColor = Pink)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Soal ${questionIndex + 1} dari ${sampleQuestions.size}", fontWeight = FontWeight.Bold)
                        Text(sampleQuestions[questionIndex].text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        sampleQuestions[questionIndex].options.forEachIndexed { index, option ->
                            Button(
                                onClick = {
                                    answers = answers.toMutableList().also { it[questionIndex] = index }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (answers[questionIndex] == index) Purple else Color.White,
                                    contentColor = if (answers[questionIndex] == index) Color.White else Ink
                                )
                            ) {
                                Text("${'A' + index}. $option")
                            }
                        }
                    }
                }
                if (message.isNotBlank()) {
                    Text("⚠️ $message", color = Color(0xFFB3261E), fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { if (questionIndex > 0) questionIndex-- },
                        modifier = Modifier.weight(1f),
                        enabled = questionIndex > 0
                    ) { Text("Sebelumnya") }
                    Button(
                        onClick = {
                            if (questionIndex < sampleQuestions.lastIndex) {
                                questionIndex++
                            } else {
                                finished = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) {
                        Text(if (questionIndex == sampleQuestions.lastIndex) "Selesai" else "Berikutnya")
                    }
                }
                OutlinedButton(
                    onClick = { finished = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kumpulkan ujian")
                }
            }

            if (finished && !teacher) {
                val correct = answers.indices.count {
                    answers[it] == sampleQuestions[it].answer
                }
                val wrong = sampleQuestions.size - correct
                val finalScore = correct * 5

                LaunchedEffect(finished) {
                    if (!scoreSaved) {
                        scoreSaved = true
                        val record = hashMapOf<String, Any>(
                            "name" to name,
                            "school" to school,
                            "studentClass" to studentClass,
                            "score" to finalScore,
                            "correct" to correct,
                            "wrong" to wrong,
                            "violations" to violations,
                            "timestamp" to System.currentTimeMillis()
                        )
                        db.collection("exam_results").add(record)
                            .addOnSuccessListener {
                                message = "Hasil ujian berhasil disimpan ke Firebase."
                            }
                            .addOnFailureListener {
                                message = "Gagal menyimpan hasil. Periksa koneksi dan aturan Firebase."
                            }
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = Green)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🎉 Ujian selesai!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Nama: $name")
                        Text("Sekolah: $school")
                        Text("Kelas: $studentClass")
                        Text("Nilai: $finalScore / 100", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Benar: $correct")
                        Text("Salah atau kosong: $wrong")
                        Text("Peringatan keluar aplikasi: $violations")
                        Text(message)
                        Button(
                            onClick = {
                                started = false
                                finished = false
                                teacher = false
                                questionIndex = 0
                                answers = List(sampleQuestions.size) { -1 }
                                seconds = 3600
                                violations = 0
                                scoreSaved = false
                                message = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Purple)
                        ) { Text("Kembali ke Beranda") }
                    }
                }
            }

            if (teacher) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("👩‍🏫 Panel Guru", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { pinInput = it },
                            label = { Text("PIN guru") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (pinInput == teacherPin) {
                                    message = "PIN benar. Rekap hasil tersedia di bawah."
                                } else {
                                    message = "PIN salah."
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Purple)
                        ) { Text("Masuk") }

                        Text(message)
                        Text("Rekap nilai dari Firebase", fontWeight = FontWeight.Bold)
                        results.forEach { result ->
                            Text(
                                "${result["name"] ?: "-"} • ${result["studentClass"] ?: "-"} • Nilai ${result["score"] ?: "-"}"
                            )
                            HorizontalDivider()
                        }
                        Text("Ubah PIN guru", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { newPin = it },
                            label = { Text("PIN baru") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (pinInput == teacherPin && newPin.length >= 4) {
                                    db.collection("settings").document("teacher")
                                        .set(mapOf("pin" to newPin))
                                        .addOnSuccessListener {
                                            teacherPin = newPin
                                            pinInput = newPin
                                            newPin = ""
                                            message = "PIN guru berhasil diperbarui."
                                        }
                                        .addOnFailureListener {
                                            message = "PIN gagal disimpan. Periksa aturan Firebase."
                                        }
                                } else {
                                    message = "Masukkan PIN lama yang benar dan PIN baru minimal 4 karakter."
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Purple)
                        ) { Text("Simpan PIN baru") }

                        OutlinedButton(
                            onClick = {
                                teacher = false
                                pinInput = ""
                                message = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Kembali") }
                    }
                }
            }
        }
    }
}
