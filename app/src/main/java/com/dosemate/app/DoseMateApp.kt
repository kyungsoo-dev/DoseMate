package com.dosemate.app

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

@Composable
fun DoseMateApp() {
    val ocrItems = remember { mutableStateListOf<String>() }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("아래 버튼을 눌러 카메라로 글자를 읽어오세요.") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap == null) {
            statusMessage = "사진을 가져오지 못했습니다. 다시 시도해 주세요."
            return@rememberLauncherForActivityResult
        }
        capturedBitmap = bitmap
    }

    LaunchedEffect(capturedBitmap) {
        val bitmap = capturedBitmap ?: return@LaunchedEffect
        isProcessing = true
        statusMessage = "글자를 읽는 중..."

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { result ->
                val lines = result.textBlocks
                    .flatMap { block -> block.lines }
                    .map { line -> line.text.trim() }
                    .filter { it.isNotBlank() }

                if (lines.isEmpty()) {
                    statusMessage = "인식된 글자가 없습니다."
                } else {
                    ocrItems.addAll(lines)
                    statusMessage = "${lines.size}개의 항목을 목록에 추가했습니다."
                }
            }
            .addOnFailureListener {
                statusMessage = "글자 인식에 실패했습니다. 조금 더 밝은 곳에서 다시 시도해 주세요."
            }
            .addOnCompleteListener {
                isProcessing = false
                capturedBitmap = null
                recognizer.close()
            }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Button(
                    onClick = { cameraLauncher.launch(null) },
                    enabled = !isProcessing
                ) {
                    Text(text = if (isProcessing) "인식 중..." else "카메라로 글자 읽기")
                }

                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "읽어온 목록",
                    style = MaterialTheme.typography.titleMedium
                )

                if (ocrItems.isEmpty()) {
                    Text(
                        text = "아직 읽어온 항목이 없습니다.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        itemsIndexed(ocrItems) { index, item ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "${index + 1}. $item",
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DoseMatePreview() {
    DoseMateApp()
}
