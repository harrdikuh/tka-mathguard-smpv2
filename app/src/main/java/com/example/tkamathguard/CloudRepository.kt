package com.example.tkamathguard

class CloudRepository {

    fun getQuestions(): List<Question> {
        return listOf(
            Question(
                subject = "Matematika",
                text = "Hasil dari 12 × 5 adalah ...",
                options = listOf("50", "60", "70", "80"),
                answer = 1
            ),
            Question(
                subject = "Matematika",
                text = "Hasil dari 144 ÷ 12 adalah ...",
                options = listOf("10", "11", "12", "13"),
                answer = 2
            ),
            Question(
                subject = "Matematika",
                text = "Jika 3x = 21, maka nilai x adalah ...",
                options = listOf("5", "6", "7", "8"),
                answer = 2
            )
        )
    }
}
