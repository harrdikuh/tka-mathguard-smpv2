
package com.example.tkamathguard

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

private data class Question(
    val id: String,
    val text: String,
    val options: List<String>,
    val answer: String
)

private data class ExamResult(
    val name: String,
    val classroom: String,
    val score: Int,
    val correct: Int,
    val wrong: Int,
    val date: Long
)

private fun starterQuestions(): List<Question> {
    val data = listOf(
        arrayOf("12 + 8 = ?", "18", "20", "22", "24", "20"),
        arrayOf("7 × 6 = ?", "36", "40", "42", "48", "42"),
        arrayOf("45 ÷ 5 = ?", "8", "9", "10", "11", "9"),
        arrayOf("3² = ?", "6", "9", "12", "15", "9"),
        arrayOf("-5 + 12 = ?", "-17", "-7", "7", "17", "7"),
        arrayOf("2/3 + 1/3 = ?", "1/3", "2/3", "1", "2", "1"),
        arrayOf("25% dari 200 adalah ...", "25", "40", "50", "75", "50"),
        arrayOf("Jika x + 5 = 12, nilai x ...", "5", "6", "7", "8", "7"),
        arrayOf("Keliling persegi sisi 6 cm ...", "12 cm", "18 cm", "24 cm", "36 cm", "24 cm"),
        arrayOf("Luas persegi panjang 8 × 4 cm ...", "12 cm²", "24 cm²", "32 cm²", "36 cm²", "32 cm²"),
        arrayOf("Rata-rata 4, 6, dan 8 adalah ...", "5", "6", "7", "8", "6"),
        arrayOf("Sudut siku-siku besarnya ...", "45°", "60°", "90°", "180°", "90°"),
        arrayOf("Bilangan prima berikut adalah ...", "9", "15", "17", "21", "17"),
        arrayOf("3 × (4 + 2) = ?", "14", "18", "20", "24", "18"),
        arrayOf("KPK dari 4 dan 6 adalah ...", "8", "10", "12", "24", "12"),
        arrayOf("FPB dari 12 dan 18 adalah ...", "3", "6", "9", "12", "6"),
        arrayOf("Bentuk sederhana 6/12 adalah ...", "1/2", "1/3", "2/3", "3/4", "1/2"),
        arrayOf("Jika 2x = 18, nilai x ...", "6", "8", "9", "12", "9"),
        arrayOf("Volume kubus rusuk 3 cm ...", "9 cm³", "18 cm³", "27 cm³", "36 cm³", "27 cm³"),
        arrayOf("Sederhanakan perbandingan 2 : 6", "1 : 2", "1 : 3", "2 : 3", "3 : 1", "1 : 3")
    )

    return data.mapIndexed { index, row ->
        Question(
            id = "default_$index",
            text = row[0],
            options = listOf(row[1], row[2], row[3], row[4]),
            answer = row[5]
        )
    }
}

private fun questionsToJson(list: List<Question>): JSONArray =
    JSONArray().apply {
        list.forEach { q ->
            put(JSONObject().apply {
                put("id", q.id)
                put("text", q.text)
                put("options", JSONArray(q.options))
                put("answer", q.answer)
            })
        }
    }

private fun questionsFromJson(array: JSONArray?): List<Question> {
    if (array == null) return emptyList()

    return (0 until array.length()).mapNotNull { i ->
        runCatching {
            val obj = array.getJSONObject(i)
            val jsonOptions = obj.getJSONArray("options")
            val options = (0 until jsonOptions.length()).map {
                jsonOptions.getString(it)
            }

            Question(
                id = obj.getString("id"),
                text = obj.getString("text"),
                options = options,
                answer = obj.getString("answer")
            )
        }.getOrNull()
    }.filter {
        it.text.isNotBlank() &&
            it.options.size == 4 &&
            it.answer in it.options
    }
}

private fun loadBank(context: Context): List<Question> {
    val prefs = context.getSharedPreferences("mathguard", Context.MODE_PRIVATE)
    val raw = prefs.getString("bank", null) ?: return starterQuestions()

    return runCatching {
        questionsFromJson(JSONArray(raw)).ifEmpty { starterQuestions() }
    }.getOrDefault(starterQuestions())
}

private fun saveBank(context: Context, list: List<Question>) {
    context.getSharedPreferences("mathguard", Context.MODE_PRIVATE)
        .edit()
        .putString("bank", questionsToJson(list).toString())
        .apply()
}

private fun loadResults(context: Context): List<ExamResult> {
    val prefs = context.getSharedPreferences("mathguard", Context.MODE_PRIVATE)
    val raw = prefs.getString("results", null) ?: return emptyList()

    return runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            ExamResult(
                name = obj.optString("name"),
                classroom = obj.optString("classroom"),
                score = obj.optInt("score"),
                correct = obj.optInt("correct"),
                wrong = obj.optInt("wrong"),
                date = obj.optLong("date")
            )
        }
    }.getOrDefault(emptyList())
}

private fun saveResults(context: Context, list: List<ExamResult>) {
    val array = JSONArray()

    list.forEach { result ->
        array.put(JSONObject().apply {
            put("name", result.name)
            put("classroom", result.classroom)
            put("score", result.score)
            put("correct", result.correct)
            put("wrong", result.wrong)
            put("date", result.date)
        })
    }

    context.getSharedPreferences("mathguard", Context.MODE_PRIVATE)
        .edit()
        .putString("results", array.toString())
        .apply()
}

private enum class Screen {
    HOME, EXAM, RESULT, TEACHER_LOGIN, TEACHER
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            MaterialTheme {
                MathGuardApp()
            }
        }
    }
}

@Composable
private fun MathGuardApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var screen by remember { mutableStateOf(Screen.HOME) }
    var bank by remember { mutableStateOf(loadBank(context)) }
    var results by remember { mutableStateOf(loadResults(context)) }

    var studentName by remember { mutableStateOf("") }
    var classroom by remember { mutableStateOf("") }
    var questions by remember { mutableStateOf(emptyList<Question>()) }
    var answers by remember { mutableStateOf(mapOf<String, String>()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(3600) }
    var warningCount by remember { mutableIntStateOf(0) }
    var warningMessage by remember { mutableStateOf("") }
    var finalResult by remember { mutableStateOf<ExamResult?>(null) }

    var pin by remember { mutableStateOf("") }
    var newQuestionText by remember { mutableStateOf("") }
    var optionA by remember { mutableStateOf("") }
    var optionB by remember { mutableStateOf("") }
    var optionC by remember { mutableStateOf("") }
    var optionD by remember { mutableStateOf("") }
    var correctOption by remember { mutableStateOf("A") }
    var teacherMessage by remember { mutableStateOf("") }

    fun beginExam() {
        if (studentName.isBlank() || classroom.isBlank()) {
            warningMessage = "Isi nama dan kelas terlebih dahulu."
            return
        }

        questions = bank.shuffled().take(20)
        answers = emptyMap()
        currentIndex = 0
        remainingSeconds = 3600
        warningCount = 0
        warningMessage = ""
        finalResult = null
        screen = Screen.EXAM
    }

    fun finishExam() {
        if (questions.isEmpty()) return

        val correct = questions.count { q ->
            answers[q.id] == q.answer
        }
        val wrong = questions.size - correct

        val result = ExamResult(
            name = studentName.trim(),
            classroom = classroom.trim(),
            score = (correct * 100) / questions.size,
            correct = correct,
            wrong = wrong,
            date = System.currentTimeMillis()
        )

        results = results + result
        saveResults(context, results)
        finalResult = result
        screen = Screen.RESULT
    }

    BackHandler(enabled = screen != Screen.HOME) {
        when (screen) {
            Screen.EXAM -> {
                warningCount++
                warningMessage =
                    "Ujian belum selesai. Gunakan tombol di layar untuk melanjutkan."
            }
            Screen.TEACHER -> screen = Screen.HOME
            else -> screen = Screen.HOME
        }
    }

    DisposableEffect(lifecycleOwner, screen) {
        val observer = LifecycleEventObserver { _, event ->
            if (screen == Screen.EXAM && event == Lifecycle.Event.ON_STOP) {
                warningCount++
                warningMessage =
                    "Peringatan: kamu meninggalkan layar ujian. Jangan keluar dari aplikasi."
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(screen) {
        while (screen == Screen.EXAM && remainingSeconds > 0) {
            delay(1000)
            if (screen == Screen.EXAM) {
                remainingSeconds--
            }
        }

        if (screen == Screen.EXAM && remainingSeconds <= 0) {
            finishExam()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "TKA MathGuard SMP",
            style = MaterialTheme.typography.headlineMedium
        )

        when (screen) {
            Screen.HOME -> {
                Text("Aplikasi Ujian Matematika SMP")
                OutlinedTextField(
                    value = studentName,
                    onValueChange = { studentName = it },
                    label = { Text("Nama siswa") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = classroom,
                    onValueChange = { classroom = it },
                    label = { Text("Kelas") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (warningMessage.isNotBlank()) {
                    Text(warningMessage, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = { beginExam() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mulai Ujian")
                }

                OutlinedButton(
                    onClick = {
                        pin = ""
                        teacherMessage = ""
                        screen = Screen.TEACHER_LOGIN
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Panel Guru")
                }

                Text("Jumlah soal tersedia: ${bank.size}")
                Text("Durasi ujian: 60 menit")
            }

            Screen.EXAM -> {
                val question = questions.getOrNull(currentIndex)

                Text("Nama: $studentName | Kelas: $classroom")
                Text(
                    "Sisa waktu: %02d:%02d".format(
                        remainingSeconds / 60,
                        remainingSeconds % 60
                    ),
                    style = MaterialTheme.typography.titleLarge
                )

                LinearProgressIndicator(
                    progress = if (questions.isEmpty()) 0f
                    else (currentIndex + 1).toFloat() / questions.size,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Soal ${currentIndex + 1} dari ${questions.size}")

                if (warningMessage.isNotBlank()) {
                    Text(
                        warningMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (question != null) {
                    Text(
                        question.text,
                        style = MaterialTheme.typography.titleMedium
                    )

                    question.options.forEach { option ->
                        val selected = answers[question.id] == option

                        OutlinedButton(
                            onClick = {
                                answers = answers + (question.id to option)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                (if (selected) "✓ " else "") + option
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (currentIndex > 0) currentIndex--
                        },
                        enabled = currentIndex > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sebelumnya")
                    }

                    Button(
                        onClick = {
                            if (currentIndex < questions.lastIndex) {
                                currentIndex++
                            } else {
                                finishExam()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (currentIndex < questions.lastIndex)
                                "Berikutnya" else "Selesai"
                        )
                    }
                }

                Text("Terjawab: ${answers.size} dari ${questions.size}")
            }
            
            Screen.RESULT -> {
                val result = finalResult

                Text(
                    "Ujian Selesai!",
                    style = MaterialTheme.typography.headlineSmall
                )

                if (result != null) {
                    Text("Nama: ${result.name}")
                    Text("Kelas: ${result.classroom}")
                    Text(
                        "Nilai: ${result.score}",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text("Jawaban benar: ${result.correct}")
                    Text("Jawaban salah: ${result.wrong}")
                }

                Text("Hasil tersimpan di perangkat ini.")

                Button(
                    onClick = {
                        studentName = ""
                        classroom = ""
                        warningMessage = ""
                        screen = Screen.HOME
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kembali ke Beranda")
                }
            }

            Screen.TEACHER_LOGIN -> {
                Text(
                    "Login Panel Guru",
                    style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("PIN guru") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (teacherMessage.isNotBlank()) {
                    Text(
                        teacherMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        if (pin == "1234") {
                            teacherMessage = ""
                            screen = Screen.TEACHER
                        } else {
                            teacherMessage = "PIN salah. Coba lagi."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Masuk")
                }

                OutlinedButton(
                    onClick = {
                        screen = Screen.HOME
                        teacherMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kembali")
                }
            }

            Screen.TEACHER -> {
                Text(
                    "Panel Guru",
                    style = MaterialTheme.typography.titleLarge
                )

                Text("Tambah soal matematika")

                OutlinedTextField(
                    value = newQuestionText,
                    onValueChange = { newQuestionText = it },
                    label = { Text("Pertanyaan") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = optionA,
                    onValueChange = { optionA = it },
                    label = { Text("Pilihan A") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = optionB,
                    onValueChange = { optionB = it },
                    label = { Text("Pilihan B") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = optionC,
                    onValueChange = { optionC = it },
                    label = { Text("Pilihan C") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = optionD,
                    onValueChange = { optionD = it },
                    label = { Text("Pilihan D") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Pilih jawaban yang benar")

                listOf("A", "B", "C", "D").forEach { choice ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        RadioButton(
                            selected = correctOption == choice,
                            onClick = { correctOption = choice }
                        )
                        Text(
                            choice,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        val options = listOf(
                            optionA.trim(),
                            optionB.trim(),
                            optionC.trim(),
                            optionD.trim()
                        )

                        if (
                            newQuestionText.isBlank() ||
                            options.any { it.isBlank() } ||
                            options.distinct().size != 4
                        ) {
                            teacherMessage =
                                "Isi pertanyaan dan 4 pilihan yang berbeda."
                        } else {
                            val answer = when (correctOption) {
                                "A" -> options[0]
                                "B" -> options[1]
                                "C" -> options[2]
                                else -> options[3]
                            }

                            val newQuestion = Question(
                                id = "custom_${System.currentTimeMillis()}",
                                text = newQuestionText.trim(),
                                options = options,
                                answer = answer
                            )

                            bank = bank + newQuestion
                            saveBank(context, bank)

                            newQuestionText = ""
                            optionA = ""
                            optionB = ""
                            optionC = ""
                            optionD = ""
                            correctOption = "A"
                            teacherMessage = "Soal berhasil disimpan."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Simpan Soal")
                }

                if (teacherMessage.isNotBlank()) {
                    Text(teacherMessage)
                }

                Divider()

                Text(
                    "Daftar Hasil dan Peringkat",
                    style = MaterialTheme.typography.titleMedium
                )

                if (results.isEmpty()) {
                    Text("Belum ada hasil ujian di perangkat ini.")
                } else {
                    results
                        .sortedWith(
                            compareByDescending<ExamResult> { it.score }
                                .thenBy { it.date }
                        )
                        .forEachIndexed { index, result ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement =
                                        Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "Peringkat ${index + 1}: ${result.name}",
                                        style =
                                            MaterialTheme.typography.titleMedium
                                    )
                                    Text("Kelas: ${result.classroom}")
                                    Text("Nilai: ${result.score}")
                                    Text("Benar: ${result.correct}")
                                    Text("Salah: ${result.wrong}")
                                    Text(
                                        "Tanggal: ${
                                            java.text.SimpleDateFormat(
                                                "dd-MM-yyyy HH:mm",
                                                java.util.Locale.getDefault()
                                            ).format(
                                                java.util.Date(result.date)
                                            )
                                        }"
                                    )
                                }
                            }
                        }
                }

                Text("Jumlah soal tersimpan: ${bank.size}")

                OutlinedButton(
                    onClick = {
                        screen = Screen.HOME
                        teacherMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Keluar Panel Guru")
                }
            }
        }
    }
}
