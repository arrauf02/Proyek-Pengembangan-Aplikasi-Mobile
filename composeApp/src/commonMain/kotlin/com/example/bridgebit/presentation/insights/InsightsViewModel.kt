package com.example.bridgebit.presentation.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bridgebit.domain.repository.AIRepository
import com.example.bridgebit.domain.usecase.GetAllHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InsightsUiState(
    val totalTranslations: Int = 0,
    val topicsDistribution: Map<String, Int> = emptyMap()
)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String
)

class InsightsViewModel(
    private val getAllHistoryUseCase: GetAllHistoryUseCase,
    private val aiRepository: AIRepository
) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> = getAllHistoryUseCase()
        .map { history ->
            val topics = history.groupBy { it.category }.mapValues { it.value.size }
            InsightsUiState(
                totalTranslations = history.size,
                topicsDistribution = topics
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InsightsUiState())

    // STATE KUIS & SKOR
    private val _quizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val quizQuestions = _quizQuestions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex = _currentQuestionIndex.asStateFlow()

    private val _selectedAnswerIndex = MutableStateFlow<Int?>(null)
    val selectedAnswerIndex = _selectedAnswerIndex.asStateFlow()

    private val _isQuizFinished = MutableStateFlow(false)
    val isQuizFinished = _isQuizFinished.asStateFlow()

    private val _correctAnswersCount = MutableStateFlow(0)
    val correctAnswersCount = _correctAnswersCount.asStateFlow()

    private val _quizError = MutableStateFlow<String?>(null)
    val quizError = _quizError.asStateFlow()

    private val _isLoadingQuiz = MutableStateFlow(false)
    val isLoadingQuiz = _isLoadingQuiz.asStateFlow()

    private val _selectedQuestionCount = MutableStateFlow(5)
    val selectedQuestionCount = _selectedQuestionCount.asStateFlow()

    fun setQuestionCount(count: Int) {
        _selectedQuestionCount.value = count
    }

    fun generateQuiz() {
        viewModelScope.launch {
            _isLoadingQuiz.value = true
            _quizError.value = null
            resetQuizState(keepQuestionCount = true)

            try {
                val history = getAllHistoryUseCase().first()
                if (history.isEmpty()) {
                    _quizError.value = "Tambahkan terjemahan ke riwayat terlebih dahulu!"
                    _isLoadingQuiz.value = false
                    return@launch
                }

                val numQuestions = _selectedQuestionCount.value

                // EKSTRAKSI KATA
                val wordPairs = history.flatMap { item ->
                    val words = item.sourceText.split(Regex("\\s+"))
                        .map { it.replace(Regex("[^a-zA-Z]"), "").lowercase() }
                        .filter { it.isNotBlank() }

                    words.map { "$it (ke ${item.targetLanguage})" }
                }.distinct()

                // Validasi jumlah kata (Peringatan tetap berjalan)
                if (wordPairs.size < numQuestions) {
                    _quizError.value = "Kosakata di riwayatmu tidak cukup untuk membuat $numQuestions soal (hanya ada ${wordPairs.size} kata unik). Silakan lakukan lebih banyak terjemahan!"
                    _isLoadingQuiz.value = false
                    return@launch
                }

                // LOGIKA BARU: Kita hanya memberikan tepat 1 kata untuk 1 soal agar AI fokus.
                val vocabularyList = wordPairs.shuffled().take(numQuestions).joinToString(", ")

                // PROMPT BARU: Instruksi tegas agar AI bebas ngarang jawaban salah (pengecoh)
                val prompt = """
                    Tugasmu adalah membuat TEPAT $numQuestions soal kuis pilihan ganda. 
                    Materi kuis: Uji arti dari kosakata berikut: $vocabularyList.
                    
                    ATURAN WAJIB:
                    1. Kamu harus menghasilkan tepat $numQuestions soal, tidak boleh kurang!
                    2. Untuk pilihan jawaban yang salah (pengecoh), kamu BEBAS ngarang/membuatnya sendiri dari kata-kata lain yang bersinonim atau mengecoh (tidak harus dari kata di atas).
                    3. Format output harus persis seperti di bawah ini untuk setiap soal, tanpa tambahan teks pengantar atau penutup apapun:
                    
                    Q: [Pertanyaan]
                    O: [Pilihan 1]
                    O: [Pilihan 2]
                    O: [Pilihan 3]
                    O: [Pilihan 4]
                    A: [Tulis HANYA angka 1, 2, 3, atau 4 untuk jawaban benar]
                    E: [Penjelasan singkat]
                """.trimIndent()

                aiRepository.chat(prompt).onSuccess { result ->
                    val questions = parseQuiz(result)

                    if (questions.isNotEmpty()) {
                        _quizQuestions.value = questions
                    } else {
                        _quizError.value = "Gagal memproses format. Jawaban Asli AI:\n\n$result"
                    }
                }.onFailure {
                    _quizError.value = "Gagal membuat kuis. Pastikan internet aktif."
                }
            } catch (e: Exception) {
                _quizError.value = "Terjadi kesalahan: ${e.message}"
            } finally {
                _isLoadingQuiz.value = false
            }
        }
    }

    private fun parseQuiz(text: String): List<QuizQuestion> {
        val questions = mutableListOf<QuizQuestion>()
        val blocks = text.split("Q:")

        for (block in blocks) {
            if (block.isBlank()) continue
            val cleanBlock = block.replace("*", "")
            val lines = cleanBlock.lines().map { it.trim() }.filter { it.isNotEmpty() }

            val q = lines.firstOrNull() ?: continue
            val ops = lines.filter { it.startsWith("O:") }.map { it.substringAfter("O:").trim() }
            val aText = lines.find { it.startsWith("A:") }?.substringAfter("A:") ?: "1"
            val eLine = lines.find { it.startsWith("E:") }?.substringAfter("E:")?.trim() ?: "Jawaban benar."

            if (ops.size >= 4) {
                val rawAnswer = aText.firstOrNull { it.isDigit() }?.digitToIntOrNull() ?: 1
                val answerIndex = (rawAnswer - 1).coerceIn(0, 3)
                questions.add(QuizQuestion(q, ops.take(4), answerIndex, eLine))
            }
        }
        return questions
    }

    fun answerQuestion(index: Int) {
        if (_selectedAnswerIndex.value == null) {
            _selectedAnswerIndex.value = index
            val currentQ = _quizQuestions.value[_currentQuestionIndex.value]
            if (index == currentQ.correctOptionIndex) {
                _correctAnswersCount.value += 1
            }
        }
    }

    fun nextQuestion() {
        if (_currentQuestionIndex.value < _quizQuestions.value.size - 1) {
            _currentQuestionIndex.value += 1
            _selectedAnswerIndex.value = null
        } else {
            _isQuizFinished.value = true
        }
    }

    private fun resetQuizState(keepQuestionCount: Boolean = false) {
        _quizQuestions.value = emptyList()
        _currentQuestionIndex.value = 0
        _selectedAnswerIndex.value = null
        _quizError.value = null
        _isQuizFinished.value = false
        _correctAnswersCount.value = 0
        if (!keepQuestionCount) {
            _selectedQuestionCount.value = 5
        }
    }

    fun resetQuiz() {
        resetQuizState()
    }
}