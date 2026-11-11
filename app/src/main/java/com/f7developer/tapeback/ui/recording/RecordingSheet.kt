package com.f7developer.tapeback.ui.recording

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.f7developer.tapeback.R
import com.f7developer.tapeback.domain.model.TapeSide
import com.f7developer.tapeback.domain.policy.CapsulePolicy
import com.f7developer.tapeback.domain.policy.TimeFormat
import com.f7developer.tapeback.ui.appContainer
import com.f7developer.tapeback.ui.capsule.CapsuleCountdownText
import com.f7developer.tapeback.ui.components.CassetteArt
import com.f7developer.tapeback.ui.components.MiniWaveform
import com.f7developer.tapeback.ui.theme.Retro
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * Details & actions for one recording: rename, favorite, tags, move, play, share, save to device,
 * seal as time capsule, break seal, delete.
 */
@Composable
fun RecordingSheet(
    recordingId: Long,
    onDismiss: () -> Unit,
    onPlayInDeck: () -> Unit,
    openSealOnStart: Boolean = false,
) {
    val container = appContainer()
    val vm: RecordingSheetViewModel = viewModel(key = "recording-$recordingId") { RecordingSheetViewModel(container, recordingId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val palette = Retro.palette
    val zone = remember { ZoneId.systemDefault() }

    var showSeal by rememberSaveable { mutableStateOf(openSealOnStart) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmBreak by remember { mutableStateOf(false) }

    val savedText = stringResource(R.string.msg_saved_to_device)
    val saveFailedText = stringResource(R.string.msg_save_failed)
    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mp4")) { uri ->
        if (uri != null) {
            vm.saveTo(uri) { ok -> Toast.makeText(context, if (ok) savedText else saveFailedText, Toast.LENGTH_SHORT).show() }
        }
    }

    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    val item = state.item
    if (!state.loading && item == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = palette.surface) {
        if (item == null) {
            Spacer(Modifier.height(240.dp))
            return@ModalBottomSheet
        }
        val r = item.recording
        val now = System.currentTimeMillis()
        val locked = CapsulePolicy.isLocked(r, now)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CassetteArt(
                    skin = item.cassetteSkin,
                    labelColor = Color(item.labelColor),
                    title = item.cassetteTitle,
                    sideLetter = r.side.letter,
                    sealedText = if (locked) stringResource(R.string.sealed) else null,
                    modifier = Modifier.width(118.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = TimeFormat.dayHeader(r.createdAt, zone),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accent,
                    )
                    Text(
                        text = stringResource(
                            R.string.recording_meta,
                            TimeFormat.clock(r.createdAt, zone),
                            TimeFormat.duration(r.durationMs),
                            TimeFormat.fileSize(r.sizeBytes),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.cassette_side_format, item.cassetteTitle, r.side.letter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconToggleButton(checked = r.favorite, onCheckedChange = { vm.setFavorite(it) }) {
                    Icon(
                        if (r.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = stringResource(if (r.favorite) R.string.cd_unfavorite else R.string.cd_favorite),
                        tint = if (r.favorite) palette.accent2 else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            TitleField(initial = r.title, onCommit = vm::rename)

            if (!locked) MiniWaveform(r.waveform, palette.accent.copy(alpha = 0.7f), Modifier.fillMaxWidth().height(34.dp), bars = 72)

            if (locked) {
                val unlockAt = r.capsuleUnlockAt ?: 0L
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        stringResource(R.string.capsule_sealed_until, TimeFormat.shortDate(unlockAt, zone)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    CapsuleCountdownText(unlockAt)
                    Text(
                        stringResource(R.string.capsule_locked_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = { confirmBreak = true }) {
                        Icon(Icons.Rounded.LockOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.capsule_break_seal))
                    }
                }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = {
                        vm.play()
                        onPlayInDeck()
                        close()
                    }) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_play))
                    }
                    OutlinedButton(onClick = {
                        vm.shareIntent()?.let { context.startActivity(it) }
                            ?: Toast.makeText(context, R.string.msg_tape_damaged, Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_share))
                    }
                    OutlinedButton(onClick = { createDoc.launch(vm.suggestedFileName()) }) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_save_device))
                    }
                    OutlinedButton(onClick = { showSeal = true }) {
                        Icon(Icons.Rounded.HourglassTop, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_seal_capsule))
                    }
                }
                if (r.capsuleUnlockAt != null && !r.capsuleNote.isNullOrBlank()) {
                    Text(stringResource(R.string.capsule_note_label), style = MaterialTheme.typography.labelMedium, color = palette.accent)
                    Text("“${r.capsuleNote}”", style = MaterialTheme.typography.bodyLarge)
                }
            }

            TagEditor(
                tags = r.tags,
                suggestions = state.tagSuggestions,
                onAdd = { vm.addTag(it) },
                onRemove = { vm.removeTag(it) },
            )

            MoveSection(
                cassettes = state.cassettes.map { it.cassette.id to it.cassette.title },
                cassetteId = r.cassetteId,
                side = r.side,
                onMove = { id, side -> vm.move(id, side) },
            )

            TextButton(onClick = { confirmDelete = true }, colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = palette.rec)) {
                Icon(Icons.Rounded.Delete, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_delete_recording))
            }
        }
    }

    if (showSeal) {
        SealCapsuleDialog(
            onDismiss = { showSeal = false },
            onSeal = { unlockAt, note ->
                vm.seal(unlockAt, note)
                showSeal = false
                Toast.makeText(context, R.string.msg_capsule_sealed, Toast.LENGTH_LONG).show()
            },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_recording_title)) },
            text = { Text(stringResource(R.string.delete_recording_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete { close() }
                }) { Text(stringResource(R.string.action_delete), color = palette.rec) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (confirmBreak) {
        AlertDialog(
            onDismissRequest = { confirmBreak = false },
            title = { Text(stringResource(R.string.break_seal_title)) },
            text = { Text(stringResource(R.string.break_seal_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmBreak = false
                    vm.breakSeal()
                }) { Text(stringResource(R.string.capsule_break_seal), color = palette.rec) }
            },
            dismissButton = { TextButton(onClick = { confirmBreak = false }) { Text(stringResource(R.string.capsule_keep_sealed)) } },
        )
    }
}

@Composable
private fun TitleField(initial: String, onCommit: (String) -> Unit) {
    var text by remember(initial) { mutableStateOf(initial) }
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = text,
        onValueChange = { if (it.length <= 80) text = it },
        label = { Text(stringResource(R.string.field_title)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            onCommit(text)
            focus.clearFocus()
        }),
        trailingIcon = {
            if (text != initial && text.isNotBlank()) {
                IconButton(onClick = {
                    onCommit(text)
                    focus.clearFocus()
                }) { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = stringResource(R.string.cd_save_title)) }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TagEditor(tags: List<String>, suggestions: List<String>, onAdd: (String) -> Unit, onRemove: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.tags), style = MaterialTheme.typography.labelMedium, color = Retro.palette.accent)
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tags.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { onRemove(tag) },
                        label = { Text("#$tag") },
                        trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_remove_tag, tag), modifier = Modifier.size(16.dp)) },
                    )
                }
            }
        }
        OutlinedTextField(
            value = input,
            onValueChange = { if (it.length <= 24) input = it },
            placeholder = { Text(stringResource(R.string.tag_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (input.isNotBlank()) onAdd(input)
                input = ""
            }),
            modifier = Modifier.fillMaxWidth(),
        )
        if (suggestions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                suggestions.forEach { s ->
                    AssistChip(
                        onClick = { onAdd(s) },
                        label = { Text("+ #$s") },
                        colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
            }
        }
    }
}

@Composable
private fun MoveSection(cassettes: List<Pair<Long, String>>, cassetteId: Long, side: TapeSide, onMove: (Long, TapeSide) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val currentTitle = cassettes.firstOrNull { it.first == cassetteId }?.second.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.move_to), style = MaterialTheme.typography.labelMedium, color = Retro.palette.accent)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = currentTitle,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.cassette)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                cassettes.forEach { (id, title) ->
                    DropdownMenuItem(text = { Text(title) }, onClick = {
                        expanded = false
                        if (id != cassetteId) onMove(id, side)
                    })
                }
            }
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            TapeSide.entries.forEachIndexed { i, s ->
                SegmentedButton(
                    selected = s == side,
                    onClick = { if (s != side) onMove(cassetteId, s) },
                    shape = SegmentedButtonDefaults.itemShape(i, TapeSide.entries.size),
                ) { Text(stringResource(R.string.side_format, s.letter)) }
            }
        }
    }
}
