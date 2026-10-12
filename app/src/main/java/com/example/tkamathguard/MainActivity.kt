
package com.example.tkamathguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

data class Question(
    val subject: String,
    val text: String,
    val options: List<String>,
    val answer: Int
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
    var started by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var index by remember { mutableIntStateOf(0) }
    var remaining by remember { mutableIntStateOf(3600) }
    var studentName by remember { mutableStateOf("") }
    var answers by remember { mutableStateOf(mapOf<Int, Int>()) }

    val questions = remember {
        createQuestions().shuffled(Random(System.currentTimeMillis()))
    }

    LaunchedEffect(started, submitted) {
        while (started && !submitted && remaining > 0) {
            delay(1000)
            remaining--
        }
        if (started && remaining <= 0) submitted = true
    }

    val score = questions.indices.count {
        answers[it] == questions[it].answer
    }
    val blank = questions.size - answers.size

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "TKA MathGuard SMP",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
            Text("Ujian Matematika & Bahasa Indonesia")

            if (!started) {
                Text("Selamat datang!", fontSize = 22.sp)
                Text(
                    "20 soal • 60 menit • Soal diacak"
                )
                OutlinedTextField(
                    value = studentName,
                    onValueChange = { studentName = it },
                    label = { Text("Nama peserta") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (studentName.isNotBlank()) started = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mulai Ujian")
                }
            } else if (submitted) {
                Text("Ujian selesai", fontSize = 25.sp)
                Text("Peserta: $studentName")
                Text("Jawaban benar: $score dari 20")
                Text("Jawaban salah: ${answers.size - score}")
                Text("Tidak dijawab: $blank")
                Text("Nilai: ${score * 5} / 100")
            } else {
                val q = questions[index]
                val minutes = remaining / 60
                val seconds = remaining % 60

                Text(
                    "Sisa waktu: %02d:%02d".format(minutes, seconds),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(
                    progress = { (index + 1) / 20f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Soal ${index + 1} dari ${questions.size}",
                    fontSize = 18.sp
                )
                Text(
                    q.subject,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(q.text, fontSize = 19.sp)

                q.options.forEachIndexed { optionIndex, option ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            answers = answers + (index to optionIndex)
                        },
                        colors = CardDefaults.cardColors(
                            containerColor =
                                if (answers[index] == optionIndex)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            option,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (index > 0) index-- },
                        enabled = index > 0
                    ) {
                        Text("Sebelumnya")
                    }
                    Button(
                        onClick = {
                            if (index < questions.lastIndex) index++
                            else submitted = true
                        }
                    ) {
                        Text(
                            if (index == questions.lastIndex)
                                "Selesai"
                            else
                                "Berikutnya"
                        )
                    }
                }

                OutlinedButton(
                    onClick = { submitted = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kumpulkan Ujian")
                }
            }
        }
    }
}

fun createQuestions(): List<Question> {
    val math = listOf(
        Triple("Hasil dari 12 + 8 × 2 adalah …", listOf("40", "28", "32", "20"), 1),
        Triple("Nilai 3² + 4² adalah …", listOf("12", "25", "49", "7"), 1),
        Triple("Hasil ¾ + ½ adalah …", listOf("1¼", "1", "¾", "2"), 0),
        Triple("Jika x + 7 = 15, nilai x adalah …", listOf("7", "8", "9", "22"), 1),
        Triple("Keliling persegi sisi 6 cm adalah …", listOf("12 cm", "24 cm", "36 cm", "18 cm"), 1),
        Triple("25% dari 200 adalah …", listOf("25", "40", "50", "75"), 2),
        Triple("Rata-rata 4, 6, dan 8 adalah …", listOf("5", "6", "7", "8"), 1),
        Triple("Luas persegi panjang 8 × 5 cm adalah …", listOf("13 cm²", "26 cm²", "40 cm²", "80 cm²"), 2),
        Triple("Hasil 2(5 + 3) adalah …", listOf("13", "16", "10", "8"), 1),
        Triple("Bilangan prima berikut adalah …", listOf("9", "15", "17", "21"), 2)
    ).map {
        Question("Matematika", it.first, it.second, it.third)
    }

    val bahasa = listOf(
        Triple("Sinonim kata 'cermat' adalah …", listOf("Teliti", "Lalai", "Lambat", "Bingung"), 0),
        Triple("Antonim kata 'optimis' adalah …", listOf("Yakin", "Pesimis", "Semangat", "Gembira"), 1),
        Triple("Gagasan utama paragraf disebut …", listOf("Kalimat penutup", "Ide pokok", "Contoh", "Kesimpulan saja"), 1),
        Triple("Kalimat efektif adalah kalimat yang …", listOf("Bertele-tele", "Jelas dan hemat", "Selalu panjang", "Tanpa predikat"), 1),
        Triple("Penulisan kata baku yang benar adalah …", listOf("Resiko", "Risiko", "Resikko", "Rissiko"), 1),
        Triple("Teks yang berisi langkah kerja disebut …", listOf("Narasi", "Prosedur", "Puisi", "Deskripsi"), 1),
        Triple("Tanda baca untuk mengakhiri kalimat tanya adalah …", listOf(".", ",", "?", "!"), 2),
        Triple("Tokoh yang menentang tokoh utama disebut …", listOf("Protagonis", "Antagonis", "Narator", "Figuran utama"), 1),
        Triple("Informasi faktual bersifat …", listOf("Khayalan", "Berdasarkan kenyataan", "Dugaan saja", "Pendapat pribadi"), 1),
        Triple("Ringkasan yang baik memuat …", listOf("Semua detail", "Inti bacaan", "Opini baru", "Dialog tambahan"), 1)
    ).map {
        Question("Bahasa Indonesia", it.first, it.second, it.third)
    }

    return math + bahasa
}
