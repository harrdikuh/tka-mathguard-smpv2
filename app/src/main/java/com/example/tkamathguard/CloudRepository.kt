package com.example.tkamathguard

class CloudRepository {

    fun getQuestions(): List<Question> {
        return listOf(
            Question(
                number = 1,
                question = "Hasil dari 12 × 5 adalah ...",
                options = listOf("50", "60", "70", "80"),
                answer = 1
            ),
            Question(
                number = 2,
                question = "Hasil dari 144 ÷ 12 adalah ...",
                options = listOf("10", "11", "12", "13"),
                answer = 2
            ),
            Question(
                number = 3,
                question = "Jika 3x = 21, maka nilai x adalah ...",
                options = listOf("5", "6", "7", "8"),
                answer = 2
            )
        )
    }
}

data class Question(
    val number: Int,
    val question: String,
    val options: List<String>,
    val answer: Int
)
