package com.synapse.social.studioasinc.ui.chat.components

import android.media.MediaRecorder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.File

@Composable
fun VoiceRecordingDialog(
    onDismiss: () -> Unit,
    onSendVoiceNote: (String) -> Unit
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRecording) "Recording..." else "Voice Note",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Record/Stop Button
                IconButton(
                    onClick = {
                        if (isRecording) {
                            // Stop recording
                            mediaRecorder?.apply {
                                stop()
                                release()
                            }
                            mediaRecorder = null
                            isRecording = false
                        } else {
                            // Start recording
                            try {
                                audioFile = File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
                                @Suppress("DEPRECATION")
                                mediaRecorder = MediaRecorder().apply {
                                    setAudioSource(MediaRecorder.AudioSource.MIC)
                                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                    setOutputFile(audioFile?.absolutePath)
                                    prepare()
                                    start()
                                }
                                isRecording = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cancel Button
                    OutlinedButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel")
                    }

                    // Send Button (only show if recording is done)
                    if (!isRecording && audioFile != null) {
                        Button(
                            onClick = {
                                audioFile?.let { file ->
                                    onSendVoiceNote(file.absolutePath)
                                }
                                onDismiss()
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send")
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder?.apply {
                if (isRecording) {
                    stop()
                }
                release()
            }
        }
    }
}
