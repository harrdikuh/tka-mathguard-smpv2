
package com.example.tkamathguard

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
data class MathQuestion(
    val id: Int,
    val text: String,
    val options: List<String>,
    val answer: String
)

data class ExamResult(
    val name: String,
    val classroom: String,
    val score: Int,
    val correct: Int,
    val wrong: Int
)

private fun questionBank(): List<MathQuestion> = listOf(
    MathQuestion(1, "Jika 3x + 5 = 20, nilai x adalah ...",
        listOf("3", "4", "5", "6"), "5"),
    MathQuestion(2, "Hasil dari 2³ × 2² adalah ...",
        listOf("16", "24", "32", "64"), "32"),
    MathQuestion(3, "Akar kuadrat dari 144 adalah ...",
        listOf("10", "11", "12", "14"), "12"),
    MathQuestion(4, "Segitiga memiliki alas 12 cm dan tinggi 8 cm. Luasnya ...",
        listOf("24 cm²", "48 cm²", "96 cm²", "40 cm²"), "48 cm²"),
    MathQuestion(5, "Keliling lingkaran dengan jari-jari 7 cm adalah ... (π = 22/7)",
        listOf("22 cm", "44 cm", "49 cm", "154 cm"), "44 cm"),
    MathQuestion(6, "Perbandingan A : B = 2 : 3. Jika jumlahnya 25, nilai A adalah ...",
        listOf("5", "10", "15", "20"), "10"),
    MathQuestion(7, "Rata-rata dari 6, 8, 10, dan 12 adalah ...",
        listOf("8", "9", "10", "11"), "9"),
    MathQuestion(8, "15% dari 200 adalah ...",
        listOf("15", "20", "30", "40"), "30"),
    MathQuestion(9, "Segitiga siku-siku memiliki sisi tegak 6 cm dan 8 cm. Sisi miringnya ...",
        listOf("9 cm", "10 cm", "12 cm", "14 cm"), "10 cm"),
    MathQuestion(10, "Hasil dari 3/4 + 1/8 adalah ...",
        listOf("4/12", "5/8", "7/8", "1"), "7/8"),
    MathQuestion(11, "Jika 5x = -20, nilai x adalah ...",
        listOf("-5", "-4", "4", "5"), "-4"),
    MathQuestion(12, "Volume kubus dengan panjang rusuk 4 cm adalah ...",
        listOf("16 cm³", "32 cm³", "48 cm³", "64 cm³"), "64 cm³"),
    MathQuestion(13, "Dua sudut segitiga adalah 50° dan 60°. Sudut ketiganya ...",
        listOf("60°", "70°", "80°", "90°"), "70°"),
    MathQuestion(14, "Pola bilangan 3, 7, 11, 15, ... Angka berikutnya adalah ...",
        listOf("17", "18", "19", "20"), "19"),
    MathQuestion(15, "Peluang muncul angka genap saat melempar dadu adalah ...",
        listOf("1/6", "1/3", "1/2", "2/3"), "1/2"),
    MathQuestion(16, "Jika 2(x - 3) = 10, nilai x adalah ...",
        listOf("5", "6", "8", "10"), "8"),
    MathQuestion(17, "Skala peta 1 : 100.000. Jarak pada peta 3 cm. Jarak sebenarnya ...",
        listOf("300 m", "1 km", "3 km", "30 km"), "3 km"),
    MathQuestion(18, "Median dari 3, 5, 7, 9, 11 adalah ...",
        listOf("5", "6", "7", "9"), "7"),
    MathQuestion(19, "Bentuk pecahan paling sederhana dari 0,25 adalah ...",
        listOf("1/2", "1/3", "1/4", "3/4"), "1/4"),
    MathQuestion(20, "Persegi panjang panjangnya 10 cm dan lebarnya 4 cm. Kelilingnya ...",
        listOf("14 cm", "28 cm", "40 cm", "20 cm"), "28 cm")
)

private fun loadResults(context: Context): List<ExamResult> {
    val raw = context.getSharedPreferences(
        "mathguard", Context.MODE_PRIVATE
    ).getString("results", "[]") ?: "[]"

    return try {
        val array = JSONArray(raw)
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            ExamResult(
                obj.getString("name"),
                obj.getString("classroom"),
                obj.getInt("score"),
                obj.getInt("correct"),
                obj.getInt("wrong")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveResults(context: Context, results: List<ExamResult>) {
    val array = JSONArray()
    results.forEach { result ->
        array.put(JSONObject().apply {
            put("name", result.name)
            put("classroom", result.classroom)
            put("score", result.score)
            put("correct", result.correct)
            put("wrong", result.wrong)
        })
    }
    context.getSharedPreferences(
        "mathguard", Context.MODE_PRIVATE
    ).edit().putString("results", array.toString()).apply()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MathGuardApp()
                }
            }
        }
    }
}

@Composable
fun MathGuardApp() {
    val context = LocalContext.current

    var screen by remember { mutableStateOf("home") }
    
var focusWarnings by remember {
    mutableIntStateOf(0)
}

val lifecycleOwner = LocalLifecycleOwner.current

DisposableEffect(lifecycleOwner, screen) {
    val observer = LifecycleEventObserver { _, event ->
        if (
            screen == "exam" &&
            event == Lifecycle.Event.ON_PAUSE
        ) {
            focusWarnings++
        }
    }

    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
    }
}
    var studentName by remember { mutableStateOf("") }
    var classroom by remember { mutableStateOf("") }
    var teacherPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val questions = remember {
        mutableStateListOf<MathQuestion>()
    }
    val selectedAnswers = remember {
        mutableStateMapOf<Int, String>()
    }
    val results = remember {
        mutableStateListOf<ExamResult>().apply {
            addAll(loadResults(context))
        }
    }

    var currentQuestion by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(3600) }
    var finalScore by remember { mutableIntStateOf(0) }
    var finalCorrect by remember { mutableIntStateOf(0) }
    var finalWrong by remember { mutableIntStateOf(0) }

    val submitExam: () -> Unit = {
        if (screen == "exam") {
            val correct = questions.count {
                selectedAnswers[it.id] == it.answer
            }
            val wrong = questions.size - correct
            finalCorrect = correct
            finalWrong = wrong
            finalScore = correct * 5

            val result = ExamResult(
                studentName.trim(),
                classroom.trim(),
                finalScore,
                correct,
                wrong
            )
            results.add(result)
            saveResults(context, results.toList())
            screen = "result"
        }
    }

    LaunchedEffect(screen, remainingSeconds) {
        if (screen == "exam") {
            if (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
            } else {
                submitExam()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("TKA MathGuard SMP", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Ujian Matematika SMP",
            style = MaterialTheme.typography.titleMedium
        )

        when (screen) {
            "home" -> {
                Spacer(Modifier.height(16.dp))
                Text("Selamat datang!")
                Text("Latihan dan ujian Matematika SMP dengan penilaian otomatis.")

                Button(
                    onClick = {
                        errorMessage = ""
                        screen = "setup"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mulai Ujian")
                }

                OutlinedButton(
                    onClick = {
                        teacherPin = ""
                        errorMessage = ""
                        screen = "teacherLogin"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Menu Guru")
                }

                Text("20 soal • 60 menit • Nilai otomatis")
            }

            "setup" -> {
                Text("Identitas Peserta", style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    value = studentName,
                    onValueChange = { studentName = it },
                    label = { Text("Nama lengkap") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = classroom,
                    onValueChange = { classroom = it },
                    label = { Text("Kelas, contoh: 9A") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (errorMessage.isNotBlank()) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = {
                        if (studentName.isBlank() || classroom.isBlank()) {
                            errorMessage = "Isi nama dan kelas terlebih dahulu."
                        } else {
                            questions.clear()
                            questions.addAll(
                                questionBank().shuffled().map {
                                    it.copy(options = it.options.shuffled())
                                }
                            )
                            selectedAnswers.clear()
                            currentQuestion = 0
                            remainingSeconds = 3600
                            screen = "exam"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mulai Mengerjakan")
                }

                TextButton(onClick = { screen = "home" }) {
                    Text("Kembali")
                }
            }

            "exam" -> {
                val q = questions.getOrNull(currentQuestion)

                Text("Peserta: $studentName • Kelas $classroom")
                Text(
                    "Sisa waktu: %02d:%02d".format(
                        remainingSeconds / 60,
                        remainingSeconds % 60
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (remainingSeconds < 300)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )

                LinearProgressIndicator(
                    progress = {
                        (currentQuestion + 1).toFloat() /
                            questions.size.coerceAtLeast(1)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (q != null) {
                    Text(
                        "Soal ${currentQuestion + 1} dari ${questions.size}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(q.text, style = MaterialTheme.typography.bodyLarge)

                    q.options.forEach { option ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { selectedAnswers[q.id] = option }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = selectedAnswers[q.id] == option,
                                    onClick = { selectedAnswers[q.id] = option }
                                )
                                Text(
                                    option,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (currentQuestion > 0) currentQuestion--
                            },
                            enabled = currentQuestion > 0
                        ) {
                            Text("Sebelumnya")
                        }

                        if (currentQuestion < questions.lastIndex) {
                            Button(onClick = { currentQuestion++ }) {
                                Text("Berikutnya")
                            }
                        } else {
                            Button(onClick = submitExam) {
                                Text("Selesai Ujian")
                            }
                        }
                    }

                    Text("Pilih jawaban untuk setiap soal. Jawaban yang belum dipilih dianggap salah.")
                }
            }

            "result" -> {
                Text("Ujian Selesai!", style = MaterialTheme.typography.headlineSmall)
                Text("Nama: $studentName")
                Text("Kelas: $classroom")
                Text("Nilai: $finalScore / 100", style = MaterialTheme.typography.titleLarge)
                Text("Jawaban benar: $finalCorrect")
                Text("Jawaban salah atau kosong: $finalWrong")

                Button(
                    onClick = { screen = "home" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kembali ke Menu Utama")
                }
            }

            "teacherLogin" -> {
                Text("Akses Guru", style = MaterialTheme.typography.titleLarge)
                Text("Masukkan PIN guru.")
                OutlinedTextField(
                    value = teacherPin,
                    onValueChange = { teacherPin = it },
                    label = { Text("PIN guru") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("PIN awal: 1234. Ganti PIN di kode sebelum digunakan sungguhan.")

                if (errorMessage.isNotBlank()) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = {
                        if (teacherPin == "1234") {
                            errorMessage = ""
                            screen = "teacher"
                        } else {
                            errorMessage = "PIN salah."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Masuk")
                }

                TextButton(onClick = { screen = "home" }) {
                    Text("Kembali")
                }
            }

            "teacher" -> {
                Text("Rekap Nilai Peserta", style = MaterialTheme.typography.titleLarge)
                Text("Data tersimpan di perangkat ini saja.")

                if (results.isEmpty()) {
                    Text("Belum ada peserta yang menyelesaikan ujian.")
                } else {
                    val ranked = results.sortedByDescending { it.score }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(ranked) { index, result ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        "${index + 1}. ${result.name}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text("Kelas: ${result.classroom}")
                                    Text("Nilai: ${result.score}/100")
                                    Text("Benar: ${result.correct} • Salah/kosong: ${result.wrong}")
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        results.clear()
                        saveResults(context, results.toList())
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Hapus Semua Rekap")
                }

                Button(
                    onClick = { screen = "home" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kembali ke Menu Utama")
                }
            }
        }
    }
}
