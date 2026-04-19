package com.dosemate.app

import android.content.Context
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.File

@Composable
fun DoseMateApp() {
    val context = LocalContext.current
    val ocrItems = remember { mutableStateListOf<String>() }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("아래 버튼을 눌러 카메라로 글자를 읽어오세요.") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        val imageUri = pendingImageUri
        pendingImageUri = null

        if (!isSuccess || imageUri == null) {
            statusMessage = "사진을 가져오지 못했습니다. 다시 시도해 주세요."
            return@rememberLauncherForActivityResult
        }

        capturedImageUri = imageUri
    }

    LaunchedEffect(capturedImageUri) {
        val imageUri = capturedImageUri ?: return@LaunchedEffect
        isProcessing = true
        statusMessage = "글자를 읽는 중..."

        val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        val image = InputImage.fromFilePath(context, imageUri)

        recognizer.process(image)
            .addOnSuccessListener { result ->
                val lines = result.textBlocks
                    .flatMap { block -> block.lines }
                    .map { line -> line.text.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()

                if (lines.isEmpty()) {
                    statusMessage = "인식된 글자가 없습니다. 글자에 가까이 맞춰 다시 촬영해 주세요."
                } else {
                    ocrItems.addAll(lines)
                    statusMessage = "${lines.size}개의 항목을 목록에 추가했습니다."
                }
            }
            .addOnFailureListener {
                statusMessage = "글자 인식에 실패했습니다. 초점을 맞추고 밝은 곳에서 다시 시도해 주세요."
            }
            .addOnCompleteListener {
                isProcessing = false
                capturedImageUri = null
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
                    onClick = {
                        val outputUri = createTempImageUri(context)
                        pendingImageUri = outputUri
                        cameraLauncher.launch(outputUri)
                    },
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

private fun createTempImageUri(context: Context): Uri {
    val imageFile = File.createTempFile(
        "dosemate_ocr_",
        ".jpg",
        context.cacheDir
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

@Preview(showBackground = true)
@Composable
fun DoseMatePreview() {
    DoseMateApp()
}
