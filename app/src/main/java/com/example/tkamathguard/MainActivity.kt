package com.example.tkamathguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Navy = Color(0xFF172554)
private val Blue = Color(0xFF2563EB)
private val Pale = Color(0xFFF1F5F9)
private val Green = Color(0xFF16A34A)
private val Orange = Color(0xFFF97316)

data class Question(
    val subject: String,
    val text: String,
    val options: List<String>,
    val answer: Int
)

private enum class Page { REGISTER, HOME, EXAM, RESULT, TEACHER }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MathGuardApp() }
    }
}

@Composable
private fun MathGuardApp() {
    var page by remember { mutableStateOf(Page.REGISTER) }
    var studentName by remember { mutableStateOf("") }
    var school by remember { mutableStateOf("") }
    var studentClass by remember { mutableStateOf("VII") }
    var pin by remember { mutableStateOf("123456") }
    var score by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var wrong by remember { mutableIntStateOf(0) }
    val questions = remember { sampleQuestions() }

    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, background = Pale)) {
        Surface(Modifier.fillMaxSize(), color = Pale) {
            when (page) {
                Page.REGISTER -> RegisterScreen(
                    name = studentName, onName = { studentName = it },
                    school = school, onSchool = { school = it },
                    studentClass = studentClass, onClass = { studentClass = it },
                    onContinue = { if (studentName.isNotBlank() && school.isNotBlank()) page = Page.HOME }
                )
                Page.HOME -> HomeScreen(
                    name = studentName, school = school, studentClass = studentClass,
                    onStart = { page = Page.EXAM }, onTeacher = { page = Page.TEACHER }
                )
                Page.EXAM -> ExamScreen(questions = questions, onFinish = { c, w ->
                    correct = c; wrong = w; score = c * 5; page = Page.RESULT
                })
                Page.RESULT -> ResultScreen(
                    name = studentName, score = score, correct = correct, wrong = wrong,
                    onHome = { page = Page.HOME }
                )
                Page.TEACHER -> TeacherScreen(
                    pin = pin, onPinChange = { pin = it }, onBack = { page = Page.HOME }
                )
            }
        }
    }
}

@Composable
private fun PageFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

@Composable
private fun Header(eyebrow: String, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(eyebrow.uppercase(), color = Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(title, color = Navy, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 32.sp)
        Text(subtitle, color = Color(0xFF64748B), fontSize = 14.sp)
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun RegisterScreen(
    name: String, onName: (String) -> Unit,
    school: String, onSchool: (String) -> Unit,
    studentClass: String, onClass: (String) -> Unit,
    onContinue: () -> Unit
) {
    PageFrame {
        Spacer(Modifier.height(20.dp))
        Text("🛡️ TKA", color = Blue, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Header("MathGuard SMP", "Siap hadapi ujian?", "Isi identitasmu sebelum memulai simulasi ujian.")
        Panel {
            Text("Profil peserta", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy)
            OutlinedTextField(value = name, onValueChange = onName, label = { Text("Nama lengkap") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = school, onValueChange = onSchool, label = { Text("Nama sekolah") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Kelas", fontWeight = FontWeight.SemiBold, color = Navy)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("VII", "VIII", "IX").forEach { item ->
                    FilterChip(selected = studentClass == item, onClick = { onClass(item) }, label = { Text(item) })
                }
            }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                Text("Simpan & lanjutkan  →", fontWeight = FontWeight.Bold)
            }
            Text("Nama dan sekolah wajib diisi.", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun HomeScreen(name: String, school: String, studentClass: String, onStart: () -> Unit, onTeacher: () -> Unit) {
    PageFrame {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("MATHGUARD", color = Blue, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            Text("SMP • TKA", color = Color.Gray, fontSize = 12.sp)
        }
        Box(Modifier.fillMaxWidth().background(Navy, RoundedCornerShape(24.dp)).padding(22.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("HALO, ${name.uppercase()}! 👋", color = Color(0xFFBFDBFE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Saatnya tunjukkan\nkemampuan terbaikmu.", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp)
                Text("$school • Kelas $studentClass", color = Color(0xFFE2E8F0), fontSize = 13.sp)
            }
        }
        Header("Ruang ujian", "Paket latihan TKA", "Kerjakan dengan teliti dan gunakan waktu sebaik mungkin.")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("20", "Soal", Modifier.weight(1f))
            StatTile("60", "Menit", Modifier.weight(1f))
            StatTile("2", "Mapel", Modifier.weight(1f))
        }
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("📘", fontSize = 28.sp)
                Column(Modifier.weight(1f)) {
                    Text("Matematika", fontWeight = FontWeight.Bold, color = Navy)
                    Text("10 soal pilihan ganda", color = Color.Gray, fontSize = 12.sp)
                }
                Text("10", color = Blue, fontWeight = FontWeight.Bold)
            }
            Divider(color = Pale)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("📝", fontSize = 28.sp)
                Column(Modifier.weight(1f)) {
                    Text("Bahasa Indonesia", fontWeight = FontWeight.Bold, color = Navy)
                    Text("10 soal pilihan ganda", color = Color.Gray, fontSize = 12.sp)
                }
                Text("10", color = Blue, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                Text("Mulai ujian  →", fontWeight = FontWeight.Bold)
            }
        }
        OutlinedButton(onClick = onTeacher, modifier = Modifier.fillMaxWidth()) { Text("⚙ Panel guru") }
        Text("Versi prototipe: hasil belum tersinkron ke Firebase.", color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Blue, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ExamScreen(questions: List<Question>, onFinish: (Int, Int) -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    var remaining by remember { mutableIntStateOf(60 * 60) }
    val answers = remember { mutableStateListOf<Int>().apply { repeat(questions.size) { add(-1) } } }
    LaunchedEffect(Unit) {
        while (remaining > 0) { delay(1000); remaining-- }
        val c = answers.indices.count { answers[it] == questions[it].answer }
        onFinish(c, questions.size - c)
    }
    val q = questions[index]
    PageFrame {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("UJIAN TKA", color = Blue, fontWeight = FontWeight.ExtraBold)
            Text("%02d:%02d".format(remaining / 60, remaining % 60), color = if (remaining < 300) Orange else Navy, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(progress = { (index + 1f) / questions.size }, modifier = Modifier.fillMaxWidth())
        Text("Soal ${index + 1} dari ${questions.size} • ${q.subject}", color = Color.Gray, fontSize = 13.sp)
        Panel {
            Text(q.text, color = Navy, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 26.sp)
            q.options.forEachIndexed { optionIndex, option ->
                val selected = answers[index] == optionIndex
                OutlinedButton(
                    onClick = { answers[index] = optionIndex },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) Color(0xFFDBEAFE) else Color.White)
                ) {
                    Text("${'A' + optionIndex}. $option", color = Navy, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { if (index > 0) index-- }, enabled = index > 0, modifier = Modifier.weight(1f)) { Text("Sebelumnya") }
            Button(onClick = {
                if (index < questions.lastIndex) index++
                else {
                    val c = answers.indices.count { answers[it] == questions[it].answer }
                    onFinish(c, questions.size - c)
                }
            }, modifier = Modifier.weight(1f)) { Text(if (index == questions.lastIndex) "Selesai" else "Berikutnya") }
        }
        Text("Pilih satu jawaban untuk setiap soal. Pastikan kamu tidak menutup aplikasi selama ujian.", color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
private fun ResultScreen(name: String, score: Int, correct: Int, wrong: Int, onHome: () -> Unit) {
    PageFrame {
        Spacer(Modifier.height(18.dp))
        Header("Ujian selesai", "Kerja bagus, $name! 🎉", "Berikut ringkasan hasil latihanmu.")
        Box(Modifier.fillMaxWidth().background(Navy, RoundedCornerShape(24.dp)).padding(26.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("SKOR AKHIR", color = Color(0xFFBFDBFE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("$score", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold)
                Text("dari 100", color = Color(0xFFE2E8F0))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("$correct", "Benar", Modifier.weight(1f))
            StatTile("$wrong", "Salah", Modifier.weight(1f))
            StatTile("20", "Total", Modifier.weight(1f))
        }
        Button(onClick = onHome, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Kembali ke beranda") }
        Text("Hasil ini tersimpan sementara di perangkat dan belum masuk rekap guru.", color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
private fun TeacherScreen(pin: String, onPinChange: (String) -> Unit, onBack: () -> Unit) {
    var enteredPin by remember { mutableStateOf("") }
    var unlocked by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    PageFrame {
        Header("Area khusus", "Panel guru", "Akses prototipe untuk pengaturan PIN.")
        Panel {
            if (!unlocked) {
                OutlinedTextField(value = enteredPin, onValueChange = { enteredPin = it }, label = { Text("PIN guru") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { if (enteredPin == pin) { unlocked = true; message = "" } else message = "PIN belum sesuai." }, modifier = Modifier.fillMaxWidth()) { Text("Masuk") }
                if (message.isNotBlank()) Text(message, color = Orange)
                Text("PIN awal prototipe: 123456", color = Color.Gray, fontSize = 12.sp)
            } else {
                Text("Pengaturan PIN", fontWeight = FontWeight.Bold, color = Navy, fontSize = 18.sp)
                var newPin by remember { mutableStateOf("") }
                OutlinedTextField(value = newPin, onValueChange = { newPin = it.filter(Char::isDigit).take(8) }, label = { Text("PIN baru (4–8 angka)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { if (newPin.length in 4..8) { onPinChange(newPin); message = "PIN berubah di perangkat ini." } else message = "PIN harus 4–8 angka." }, modifier = Modifier.fillMaxWidth()) { Text("Simpan PIN") }
                if (message.isNotBlank()) Text(message, color = Green)
                Text("Perubahan PIN ini masih lokal, belum tersinkron antar-HP.", color = Color.Gray, fontSize = 12.sp)
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Kembali") }
    }
}

private fun sampleQuestions(): List<Question> {
    val math = listOf(
        Question("Matematika", "Hasil dari 3/4 + 1/2 adalah …", listOf("1/4", "1", "5/4", "3/2"), 2),
        Question("Matematika", "Jika 3x = 21, nilai x adalah …", listOf("6", "7", "8", "9"), 1),
        Question("Matematika", "Luas persegi dengan sisi 8 cm adalah …", listOf("16 cm²", "32 cm²", "64 cm²", "80 cm²"), 2),
        Question("Matematika", "Rata-rata dari 6, 8, dan 10 adalah …", listOf("7", "8", "9", "10"), 1),
        Question("Matematika", "25% dari 240 adalah …", listOf("40", "50", "60", "80"), 2),
        Question("Matematika", "Keliling persegi panjang 7 cm × 4 cm adalah …", listOf("11 cm", "22 cm", "28 cm", "44 cm"), 1),
        Question("Matematika", "Hasil dari −5 + 12 adalah …", listOf("−17", "−7", "7", "17"), 2),
        Question("Matematika", "Bentuk sederhana dari 6a + 2a adalah …", listOf("8", "8a", "12a", "8a²"), 1),
        Question("Matematika", "Sebuah dadu memiliki … sisi", listOf("4", "6", "8", "12"), 1),
        Question("Matematika", "Sudut siku-siku besarnya …", listOf("45°", "60°", "90°", "180°"), 2)
    )
    val language = listOf(
        Question("Bahasa Indonesia", "Gagasan utama dalam paragraf disebut …", listOf("Kalimat tanya", "Ide pokok", "Kata hubung", "Amanat"), 1),
        Question("Bahasa Indonesia", "Sinonim kata ‘cermat’ adalah …", listOf("Teliti", "Lalai", "Lambat", "Ragu"), 0),
        Question("Bahasa Indonesia", "Kalimat yang berisi ajakan disebut kalimat …", listOf("Tanya", "Perintah", "Persuasif", "Berita"), 2),
        Question("Bahasa Indonesia", "Tanda baca untuk mengakhiri kalimat tanya adalah …", listOf("Titik", "Koma", "Seru", "Tanya"), 3),
        Question("Bahasa Indonesia", "Lawan kata ‘optimis’ adalah …", listOf("Yakin", "Pesimis", "Semangat", "Berani"), 1),
        Question("Bahasa Indonesia", "Tokoh yang mengalami konflik utama disebut …", listOf("Tokoh utama", "Figuran", "Narator", "Pembaca"), 0),
        Question("Bahasa Indonesia", "Teks yang menjelaskan langkah membuat sesuatu adalah …", listOf("Narasi", "Prosedur", "Puisi", "Deskripsi"), 1),
        Question("Bahasa Indonesia", "Kata baku yang benar adalah …", listOf("Resiko", "Aktifitas", "Praktik", "Ijin"), 2),
        Question("Bahasa Indonesia", "Pesan yang ingin disampaikan dalam cerita disebut …", listOf("Tema", "Latar", "Amanat", "Alur"), 2),
        Question("Bahasa Indonesia", "Kalimat efektif harus bersifat …", listOf("Bertele-tele", "Jelas dan hemat", "Panjang", "Puitis"), 1)
    )
    return (math + language).shuffled()
}
