
package com.example.tkamathguard

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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Question(
    val id: Long,
    val text: String,
    val options: List<String>,
    val correct: Int
)

private data class ExamResult(
    val name: String,
    val classroom: String,
    val score: Int,
    val correct: Int,
    val wrong: Int,
    val date: Long
)

private data class ExamSession(
    val name: String,
    val classroom: String,
    val questions: List<Question>,
    val answers: Map<Int, Int>,
    val index: Int,
    val endTime: Long,
    val warnings: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mencegah screenshot biasa saat aplikasi aktif.
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

private fun defaultQuestions(): List<Question> {
    val data = listOf(
        Triple("Hasil dari 12 + 8 adalah ...", listOf("18", "20", "22", "24"), 1),
        Triple("Hasil dari 7 × 6 adalah ...", listOf("36", "40", "42", "48"), 2),
        Triple("Hasil dari 45 ÷ 5 adalah ...", listOf("8", "9", "10", "11"), 1),
        Triple("Hasil dari 3² adalah ...", listOf("6", "9", "12", "15"), 1),
        Triple("Nilai dari -5 + 12 adalah ...", listOf("-17", "-7", "7", "17"), 2),
        Triple("Hasil dari 2/3 + 1/3 adalah ...", listOf("1/3", "2/3", "1", "2"), 2),
        Triple("25% dari 200 adalah ...", listOf("25", "40", "50", "75"), 2),
        Triple("Jika x + 5 = 12, nilai x adalah ...", listOf("5", "6", "7", "8"), 2),
        Triple("Keliling persegi dengan sisi 6 cm adalah ...", listOf("12 cm", "18 cm", "24 cm", "36 cm"), 2),
        Triple("Luas persegi panjang 8 cm × 4 cm adalah ...", listOf("12 cm²", "24 cm²", "32 cm²", "36 cm²"), 2),
        Triple("Rata-rata dari 4, 6, dan 8 adalah ...", listOf("5", "6", "7", "8"), 1),
        Triple("Sudut siku-siku besarnya ...", listOf("45°", "60°", "90°", "180°"), 2),
        Triple("Bilangan prima berikut adalah ...", listOf("9", "15", "17", "21"), 2),
        Triple("Hasil dari 3 × (4 + 2) adalah ...", listOf("14", "18", "20", "24"), 1),
        Triple("KPK dari 4 dan 6 adalah ...", listOf("8", "10", "12", "24"), 2),
        Triple("FPB dari 12 dan 18 adalah ...", listOf("3", "6", "9", "12"), 1),
        Triple("Bentuk sederhana 6/12 adalah ...", listOf("1/2", "1/3", "2/3", "3/4"), 0),
        Triple("Jika 2x = 18, nilai x adalah ...", listOf("6", "8", "9", "12"), 2),
        Triple("Volume kubus dengan rusuk 3 cm adalah ...", listOf("9 cm³", "18 cm³", "27 cm³", "36 cm³"), 2),
        Triple("Perbandingan 2 : 6 dalam bentuk sederhana adalah ...", listOf("1 : 2", "1 : 3", "2 : 3", "3 : 1"), 1)
    )

    return data.mapIndexed { i, item ->
        Question(
            id = i + 1L,
            text = item.first,
            options = item.second,
            correct = item.third
        )
    }
}

private fun questionToJson(q: Question) = JSONObject().apply {
    put("id", q.id)
    put("text", q.text)
    put("options", JSONArray(q.options))
    put("correct", q.correct)
}

private fun questionFromJson(o: JSONObject) = Question(
    id = o.optLong("id"),
    text = o.optString("text"),
    options = o.optJSONArray("options").let { a ->
        (0 until (a?.length() ?: 0)).map { a!!.optString(it) }
    },
    correct = o.optInt("correct")
)

private fun questionsToJson(list: List<Question>) =
    JSONArray().apply {
        list.forEach { put(questionToJson(it)) }
    }

private fun questionsFromJson(a: JSONArray?): List<Question> {
    if (a == null) return emptyList()
    return (0 until a.length()).mapNotNull {
        runCatching { questionFromJson(a.getJSONObject(it)) }.getOrNull()
    }.filter { it.text.isNotBlank() && it.options.size == 4 }
}

private fun saveBank(
    prefs: android.content.SharedPreferences,
    questions: List<Question>
) {
    prefs.edit().putString("question_bank", questionsToJson(questions).toString()).apply()
}

private fun loadBank(prefs: android.content.SharedPreferences): List<Question> {
    val raw = prefs.getString("question_bank", null) ?: return defaultQuestions()
    return runCatching {
        questionsFromJson(JSONArray(raw)).ifEmpty { defaultQuestions() }
    }.getOrDefault(defaultQuestions())
}

private fun resultToJson(r: ExamResult) = JSONObject().apply {
    put("name", r.name)
    put("classroom", r.classroom)
    put("score", r.score)
    put("correct", r.correct)
    put("wrong", r.wrong)
    put("date", r.date)
}

private fun loadResults(prefs: android.content.SharedPreferences): List<ExamResult> {
    val raw = prefs.getString("results", null) ?: return emptyList()
    return runCatching {
        val a = JSONArray(raw)
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            ExamResult(
                name = o.optString("name"),
                classroom = o.optString("classroom"),
                score = o.optInt("score"),
                correct = o.optInt("correct"),
                wrong = o.optInt("wrong"),
                date = o.optLong("date")
            )
        }
    }.getOrDefault(emptyList())
}

private fun saveResults(
    prefs: android.content.SharedPreferences,
    results: List<ExamResult>
) {
    prefs.edit().putString(
        "results",
        JSONArray().apply { results.forEach { put(resultToJson(it)) } }.toString()
    ).apply()
}

private fun sessionToJson(s: ExamSession) = JSONObject().apply {
    put("name", s.name)
    put("classroom", s.classroom)
    put("questions", questionsToJson(s.questions))
    put("index", s.index)
    put("endTime", s.endTime)
    put("warnings", s.warnings)
    put("answers", JSONObject().apply {
        s.answers.forEach { (key, value) -> put(key.toString(), value) }
    })
}

private fun loadSession(prefs: android.content.SharedPreferences): ExamSession? {
    val raw = prefs.getString("active_session", null) ?: return null
    return runCatching {
        val o = JSONObject(raw)
        val answersObject = o.optJSONObject("answers") ?: JSONObject()
        val answers = mutableMapOf<Int, Int>()
        val keys = answersObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            answers[key.toInt()] = answersObject.optInt(key)
        }

        ExamSession(
            name = o.optString("name"),
            classroom = o.optString("classroom"),
            questions = questionsFromJson(o.optJSONArray("questions")),
            answers = answers,
            index = o.optInt("index"),
            endTime = o.optLong("endTime"),
            warnings = o.optInt("warnings")
        )
    }.getOrNull()
}

@Composable
private fun MathGuardApp() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("mathguard_data", android.content.Context.MODE_PRIVATE)
    }

    val bank = remember {
        mutableStateListOf<Question>().apply { addAll(loadBank(prefs)) }
    }
    val results = remember {
        mutableStateListOf<ExamResult>().apply { addAll(loadResults(prefs)) }
    }

    val restored = remember {
        loadSession(prefs)?.takeIf {
            it.endTime > System.currentTimeMillis() && it.questions.isNotEmpty()
        }
    }

    var screen by remember { mutableStateOf(if (restored != null) "exam" else "home") }
    var studentName by remember { mutableStateOf(restored?.name ?: "") }
    var classroom by remember { mutableStateOf(restored?.classroom ?: "") }
    var teacherPin by remember { mutableStateOf("") }

    val examQuestions = remember {
        mutableStateListOf<Question>().apply {
            addAll(restored?.questions ?: emptyList())
        }
    }
    val answers = remember {
        mutableStateMapOf<Int, Int>().apply {
            restored?.answers?.forEach { (k, v) -> put(k, v) }
        }
    }

    var questionIndex by remember { mutableIntStateOf(restored?.index ?: 0) }
    var endTime by remember { mutableLongStateOf(restored?.endTime ?: 0L) }
    var focusWarnings by remember { mutableIntStateOf(restored?.warnings ?: 0) }
    var remainingSeconds by remember {
        mutableLongStateOf(
            restored?.let { ((it.endTime - System.currentTimeMillis()) / 1000).coerceAtLeast(0) }
                ?: 3600L
        )
    }

    var newQuestionText by remember { mutableStateOf("") }
    var optionA by remember { mutableStateOf("") }
    var optionB by remember { mutableStateOf("") }
    var optionC by remember { mutableStateOf("") }
    var optionD by remember { mutableStateOf("") }
    var correctOption by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }

    fun persistSession() {
        if (screen == "exam" && examQuestions.isNotEmpty() && endTime > 0L) {
            val session = ExamSession(
                name = studentName,
                classroom = classroom,
                questions = examQuestions.toList(),
                answers = answers.toMap(),
                index = questionIndex,
                endTime = endTime,
                warnings = focusWarnings
            )
            prefs.edit().putString("active_session", sessionToJson(session).toString()).apply()
        }
    }

    fun finishExam() {
        val correctCount = answers.count { (index, selected) ->
            index in examQuestions.indices &&
                selected == examQuestions[index].correct
        }
        val wrongCount = examQuestions.size - correctCount
        val score = if (examQuestions.isEmpty()) 0
        else (correctCount * 100) / examQuestions.size

        val result = ExamResult(
            name = studentName,
            classroom = classroom,
            score = score,
            correct = correctCount,
            wrong = wrongCount,
            date = System.currentTimeMillis()
        )

        results.add(result)
        saveResults(prefs, results.toList())
        prefs.edit().remove("active_session").apply()
        screen = "result"
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, screen) {
        val observer = LifecycleEventObserver { _, event ->
            if (screen == "exam" && event == Lifecycle.Event.ON_PAUSE) {
                focusWarnings++
                persistSession()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(screen, endTime) {
        while (screen == "exam") {
            remainingSeconds = ((endTime - System.currentTimeMillis()) / 1000)
                .coerceAtLeast(0)
            if (remainingSeconds <= 0L) {
                finishExam()
                break
            }
            delay(1000)
        }
    }

    BackHandler(enabled = screen == "exam") {
        message = "Ujian sedang berlangsung. Gunakan tombol di dalam aplikasi."
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("TKA MathGuard SMP", style = MaterialTheme.typography.headlineMedium)
            Text("Aplikasi Ujian Matematika SMP")

            if (message.isNotBlank()) {
                Text(message, color = MaterialTheme.colorScheme.error)
            }

            when (screen) {
                "home" -> {
                    Text(
                        "Ujian matematika dengan penyimpanan sesi dan rekap nilai.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text("Nama siswa") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = classroom,
                        onValueChange = { classroom = it },
                        label = { Text("Kelas, contoh: 8A") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (studentName.isBlank() || classroom.isBlank()) {
                                message = "Isi nama siswa dan kelas terlebih dahulu."
                            } else {
                                examQuestions.clear()
                                examQuestions.addAll(bank.shuffled().take(20))
                                answers.clear()
                                questionIndex = 0
                                focusWarnings = 0
                                endTime = System.currentTimeMillis() + 60 * 60 * 1000L
                                remainingSeconds = 3600L
                                message = ""
                                screen = "exam"
                                persistSession()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mulai Ujian")
                    }
                    OutlinedButton(
                        onClick = {
                            teacherPin = ""
                            message = ""
                            screen = "teacherLogin"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Panel Guru")
                    }
                    Text("Bank soal tersedia: ${bank.size}")
                }

                "exam" -> {
                    if (examQuestions.isNotEmpty()) {
                        val q = examQuestions[questionIndex.coerceIn(examQuestions.indices)]
                        val minutes = remainingSeconds / 60
                        val seconds = remainingSeconds % 60

                        Text("Peserta: $studentName • Kelas $classroom")
                        Text(
                            "Sisa waktu: %02d:%02d".format(minutes, seconds),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text("Soal ${questionIndex + 1} dari ${examQuestions.size}")
                        LinearProgressIndicator(
                            progress = { (questionIndex + 1f) / examQuestions.size },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (focusWarnings > 0) {
                            Text(
                                "Peringatan: aplikasi ditinggalkan $focusWarnings kali. Tetap fokus pada ujian.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Text(q.text, style = MaterialTheme.typography.titleMedium)

                        q.options.forEachIndexed { index, option ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    answers[questionIndex] = index
                                    persistSession()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = answers[questionIndex] == index,
                                        onClick = {
                                            answers[questionIndex] = index
                                            persistSession()
                                        }
                                    )
                                    Text("${'A' + index}. $option")
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (questionIndex > 0) {
                                        questionIndex--
                                        persistSession()
                                    }
                                },
                                enabled = questionIndex > 0,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Sebelumnya")
                            }

                            Button(
                                onClick = {
                                    if (questionIndex < examQuestions.lastIndex) {
                                        questionIndex++
                                        persistSession()
                                    } else {
                                        finishExam()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (questionIndex == examQuestions.lastIndex) "Selesai" else "Berikutnya")
                            }
                        }

                        OutlinedButton(
                            onClick = { finishExam() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Kumpulkan Ujian")
                        }
                    } else {
                        Text("Tidak ada soal dalam sesi ujian.")
                        Button(onClick = {
                            prefs.edit().remove("active_session").apply()
                            screen = "home"
                        }) {
                            Text("Kembali")
                        }
                    }
                }

                "result" -> {
                    val last = results.lastOrNull()
                    Text("Ujian selesai!", style = MaterialTheme.typography.headlineSmall)
                    if (last != null) {
                        Text("Nama: ${last.name}")
                        Text("Kelas: ${last.classroom}")
                        Text("Nilai: ${last.score}")
                        Text("Jawaban benar: ${last.correct}")
                        Text("Jawaban salah/kosong: ${last.wrong}")
                    }
                    Button(
                        onClick = {
                            studentName = ""
                            classroom = ""
                            message = ""
                            screen = "home"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kembali ke Beranda")
                    }
                }

                "teacherLogin" -> {
                    Text("Login Guru", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = teacherPin,
                        onValueChange = { teacherPin = it },
                        label = { Text("PIN guru") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (teacherPin == "1234") {
                                message = ""
                                screen = "teacher"
                            } else {
                                message = "PIN salah."
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Masuk")
                    }
                    OutlinedButton(onClick = { screen = "home" }) {
                        Text("Kembali")
                    }
                }

                "teacher" -> {
                    Text("Panel Guru", style = MaterialTheme.typography.headlineSmall)
                    Text("Jumlah soal: ${bank.size}")
                    Text("Jumlah hasil ujian: ${results.size}")

                    Button(
                        onClick = {
                            newQuestionText = ""
                            optionA = ""
                            optionB = ""
                            optionC = ""
                            optionD = ""
                            correctOption = 0
                            screen = "addQuestion"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Buat Soal Baru")
                    }

                    Text("Bank Soal", style = MaterialTheme.typography.titleMedium)
                    bank.forEachIndexed { index, q ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("${index + 1}. ${q.text}")
                                Text("Kunci: ${'A' + q.correct}")
                                OutlinedButton(
                                    onClick = {
                                        bank.removeAt(index)
                                        saveBank(prefs, bank.toList())
                                    }
                                ) {
                                    Text("Hapus Soal")
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { screen = "teacherResults" },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Rekap dan Peringkat Siswa")
                    }

                    OutlinedButton(
                        onClick = { screen = "home" },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Keluar Panel Guru")
                    }
                }

                "addQuestion" -> {
                    Text("Buat Soal Baru", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = newQuestionText,
                        onValueChange = { newQuestionText = it },
                        label = { Text("Teks soal") },
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

                    Text("Pilih kunci jawaban:")
                    listOf("A", "B", "C", "D").forEachIndexed { index, label ->
                        Row {
                            RadioButton(
                                selected = correctOption == index,
                                onClick = { correctOption = index }
                            )
                            Text(label, modifier = Modifier.padding(top = 12.dp))
                        }
                    }

                    Button(
                        onClick = {
                            val options = listOf(optionA, optionB, optionC, optionD)
                            if (newQuestionText.isBlank() || options.any { it.isBlank() }) {
                                message = "Lengkapi teks soal dan keempat pilihan."
                            } else {
                                bank.add(
                                    Question(
                                        id = System.currentTimeMillis(),
                                        text = newQuestionText.trim(),
                                        options = options.map { it.trim() },
                                        correct = correctOption
                                    )
                                )
                                saveBank(prefs, bank.toList())
                                message = "Soal berhasil disimpan."
                                screen = "teacher"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simpan Soal")
                    }
                    OutlinedButton(onClick = { screen = "teacher" }) {
                        Text("Batal")
                    }
                }

                "teacherResults" -> {
                    Text("Rekap Nilai dan Peringkat", style = MaterialTheme.typography.titleLarge)
                    val ranked = results.sortedByDescending { it.score }

                    if (ranked.isEmpty()) {
                        Text("Belum ada hasil ujian.")
                    } else {
                        ranked.forEachIndexed { index, r ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("${index + 1}. ${r.name} — ${r.classroom}")
                                    Text("Nilai: ${r.score}")
                                    Text("Benar: ${r.correct} • Salah/kosong: ${r.wrong}")
                                    Text(
                                        SimpleDateFormat(
                                            "dd-MM-yyyy HH:mm",
                                            Locale.getDefault()
                                        ).format(Date(r.date))
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { screen = "teacher" },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kembali ke Panel Guru")
                    }
                }
            }
        }
    }
}
