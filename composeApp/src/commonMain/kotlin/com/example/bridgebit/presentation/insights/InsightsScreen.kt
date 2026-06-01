package com.example.bridgebit.presentation.screens.insights

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showStatsPopup by remember { mutableStateOf(false) }
    var showQuizPopup by remember { mutableStateOf(false) }

    // STATE BARU untuk fitur "Lainnya"
    var isCustomCount by remember { mutableStateOf(false) }
    var customCountText by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Learning Insights") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Pilih Menu Insights", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier.fillMaxWidth().clickable { showStatsPopup = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(24.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Column {
                        Text("Statistik Belajar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Lihat grafik kategori terjemahanmu", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().clickable { showQuizPopup = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(24.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Column {
                        Text("Uji Kosakata (AI Quiz)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Kuis per kata dari riwayat terjemahan", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }

    if (showStatsPopup) {
        Dialog(onDismissRequest = { showStatsPopup = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(16.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Statistik Kategori", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showStatsPopup = false }) { Icon(Icons.Default.Close, contentDescription = "Tutup") }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    if (state.topicsDistribution.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Belum ada data.") }
                    } else {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            val maxCount = state.topicsDistribution.values.maxOrNull() ?: 1
                            state.topicsDistribution.entries.sortedByDescending { it.value }.forEach { (category, count) ->
                                val progress = count.toFloat() / maxCount.toFloat()
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(category, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                        Text("$count", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))) {
                                        Box(modifier = Modifier.fillMaxWidth(fraction = progress).fillMaxHeight().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showQuizPopup) {
        val questions by viewModel.quizQuestions.collectAsState()
        val currentIndex by viewModel.currentQuestionIndex.collectAsState()
        val selectedAnswer by viewModel.selectedAnswerIndex.collectAsState()
        val isFinished by viewModel.isQuizFinished.collectAsState()
        val correctCount by viewModel.correctAnswersCount.collectAsState()
        val quizError by viewModel.quizError.collectAsState()
        val isLoadingQuiz by viewModel.isLoadingQuiz.collectAsState()
        val selectedQuestionCount by viewModel.selectedQuestionCount.collectAsState()

        Dialog(
            onDismissRequest = {
                showQuizPopup = false
                viewModel.resetQuiz()
                isCustomCount = false
                customCountText = ""
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.95f).padding(16.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isFinished) "Hasil Kuis" else "Vocabulary Quiz", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        IconButton(onClick = {
                            showQuizPopup = false
                            viewModel.resetQuiz()
                            isCustomCount = false
                            customCountText = ""
                        }) { Icon(Icons.Default.Close, contentDescription = "Tutup") }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (questions.isEmpty()) {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {

                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Pilih Jumlah Soal:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))

                            // BARIS TOMBOL PILIHAN SOAL + "Lainnya"
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf(3, 5, 10).forEach { count ->
                                    val isSelected = count == selectedQuestionCount && !isCustomCount
                                    Button(
                                        onClick = {
                                            isCustomCount = false
                                            viewModel.setQuestionCount(count)
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        enabled = !isLoadingQuiz
                                    ) { Text("$count") }
                                }

                                // TOMBOL LAINNYA (Custom)
                                Button(
                                    onClick = { isCustomCount = true },
                                    modifier = Modifier.weight(1.5f),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCustomCount) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isCustomCount) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    enabled = !isLoadingQuiz
                                ) { Text("Lainnya") }
                            }

                            // TEXTFIELD MUNCUL JIKA "Lainnya" DIKLIK
                            if (isCustomCount) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = customCountText,
                                    onValueChange = { text ->
                                        // Filter agar pengguna hanya bisa mengetik angka
                                        val filteredText = text.filter { it.isDigit() }
                                        customCountText = filteredText

                                        // Kirim angka ke ViewModel
                                        val count = filteredText.toIntOrNull()
                                        if (count != null && count > 0) {
                                            viewModel.setQuestionCount(count)
                                        }
                                    },
                                    label = { Text("Ketik jumlah soal...") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    enabled = !isLoadingQuiz
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(onClick = { viewModel.generateQuiz() }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = !isLoadingQuiz, shape = RoundedCornerShape(12.dp)) {
                                if (isLoadingQuiz) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Menyiapkan Kosakata...")
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Mulai Kuis")
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            if (quizError != null) {
                                Text(text = quizError ?: "", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else if (isFinished) {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(24.dp))

                            val percentage = if (questions.isEmpty()) 0f else correctCount.toFloat() / questions.size.toFloat()
                            val scoreText = (percentage * 100).toInt()
                            val gaugeColor = when {
                                percentage < 0.5f -> Color(0xFFE53935)
                                percentage < 0.8f -> Color(0xFFFFB300)
                                else -> Color(0xFF43A047)
                            }

                            Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.BottomCenter) {
                                Canvas(modifier = Modifier.size(240.dp, 120.dp)) {
                                    drawArc(
                                        color = Color.LightGray.copy(alpha = 0.3f),
                                        startAngle = 180f, sweepAngle = 180f, useCenter = false,
                                        style = Stroke(width = 50f, cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = gaugeColor,
                                        startAngle = 180f, sweepAngle = percentage * 180f, useCenter = false,
                                        style = Stroke(width = 50f, cap = StrokeCap.Round)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = 10.dp)) {
                                    Text(text = "$scoreText%", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, color = gaugeColor)
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF43A047).copy(alpha = 0.1f))) {
                                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Benar", tint = Color(0xFF43A047), modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Benar", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                        Text("$correctCount Soal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFE53935).copy(alpha = 0.1f))) {
                                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Cancel, contentDescription = "Salah", tint = Color(0xFFE53935), modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Salah", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                        Text("${questions.size - correctCount} Soal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                            Button(onClick = {
                                viewModel.resetQuiz()
                                isCustomCount = false
                                customCountText = ""
                            }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                                Text("Kembali ke Menu")
                            }
                        }
                    } else {
                        val currentQ = questions[currentIndex]
                        val isAnswered = selectedAnswer != null

                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Text(text = "Soal ${currentIndex + 1} dari ${questions.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = currentQ.question, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(24.dp))

                            currentQ.options.forEachIndexed { index, option ->
                                val isCorrect = index == currentQ.correctOptionIndex
                                val isSelected = index == selectedAnswer

                                val containerColor = when {
                                    !isAnswered -> MaterialTheme.colorScheme.surfaceVariant
                                    isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                val contentColor = when {
                                    !isAnswered -> MaterialTheme.colorScheme.onSurfaceVariant
                                    isCorrect -> Color(0xFF2E7D32)
                                    isSelected && !isCorrect -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                }
                                val borderColor = when {
                                    isAnswered && isCorrect -> Color(0xFF4CAF50)
                                    isAnswered && isSelected && !isCorrect -> MaterialTheme.colorScheme.error
                                    else -> Color.Transparent
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable(enabled = !isAnswered) { viewModel.answerQuestion(index) },
                                    colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
                                    border = BorderStroke(1.dp, borderColor)
                                ) {
                                    Text(
                                        text = "${'A' + index}. $option",
                                        modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isAnswered && (isCorrect || isSelected)) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }

                            if (isAnswered) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Penjelasan:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(currentQ.explanation, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { viewModel.nextQuestion() },
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Text(if (currentIndex < questions.size - 1) "Soal Selanjutnya" else "Lihat Hasil")
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}