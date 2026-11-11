package com.f7developer.tapeback.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.f7developer.tapeback.R
import com.f7developer.tapeback.domain.model.CassetteSkin
import com.f7developer.tapeback.ui.components.CassetteArt
import com.f7developer.tapeback.ui.components.CassetteSkins
import com.f7developer.tapeback.ui.components.rememberCoverBitmap
import com.f7developer.tapeback.ui.theme.Retro
import kotlinx.coroutines.launch

@Composable
fun CassetteEditorSheet(
    initial: CassetteDraft,
    editor: CassetteEditing,
    onDismiss: () -> Unit,
    onSaved: (Long) -> Unit = {},
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val palette = Retro.palette
    var draft by remember { mutableStateOf(initial) }
    val imported = remember { mutableStateListOf<String>() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                editor.importCover(uri)?.let { path ->
                    imported += path
                    draft = draft.copy(coverPath = path)
                }
            }
        }
    }

    fun dismissWithoutSaving() {
        imported.forEach(editor::discardCover)
        onDismiss()
    }

    ModalBottomSheet(onDismissRequest = ::dismissWithoutSaving, sheetState = sheet, containerColor = palette.surface) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(if (initial.id == null) R.string.editor_new_title else R.string.editor_edit_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CassetteArt(
                    skin = draft.skin,
                    labelColor = Color(draft.labelColor),
                    title = draft.title.ifBlank { stringResource(R.string.editor_title_placeholder) },
                    sideLetter = "A",
                    subtitle = draft.category,
                    progress = { 0.35f },
                    modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth(0.85f),
                )
            }
            OutlinedTextField(
                value = draft.title,
                onValueChange = { if (it.length <= 48) draft = draft.copy(title = it) },
                label = { Text(stringResource(R.string.field_title)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.editor_category), style = MaterialTheme.typography.labelMedium, color = palette.accent)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CassetteSkins.categories.forEach { cat ->
                    FilterChip(
                        selected = draft.category.equals(cat, ignoreCase = true),
                        onClick = { draft = draft.copy(category = cat) },
                        label = { Text(cat) },
                    )
                }
            }
            OutlinedTextField(
                value = draft.category,
                onValueChange = { if (it.length <= 24) draft = draft.copy(category = it) },
                label = { Text(stringResource(R.string.editor_custom_category)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.editor_label_color), style = MaterialTheme.typography.labelMedium, color = palette.accent)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CassetteSkins.labelColors.forEach { argb ->
                    val selected = draft.labelColor == argb
                    val color = Color(argb)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(if (selected) 3.dp else 1.dp, if (selected) palette.accent else palette.outline, CircleShape)
                            .semantics {
                                this.selected = selected
                                contentDescription = "Label color"
                            }
                            .clickable(role = Role.RadioButton) { draft = draft.copy(labelColor = argb) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = if (color.luminance() > 0.5f) Color.Black else Color.White)
                    }
                }
            }

            Text(stringResource(R.string.editor_shell), style = MaterialTheme.typography.labelMedium, color = palette.accent)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(CassetteSkin.entries) { skin ->
                    val selected = draft.skin == skin
                    val name = stringResource(CassetteSkins.nameRes(skin))
                    Column(
                        modifier = Modifier
                            .width(118.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) palette.accent.copy(alpha = 0.16f) else palette.surfaceRaised)
                            .border(if (selected) 2.dp else 0.dp, if (selected) palette.accent else Color.Transparent, RoundedCornerShape(16.dp))
                            .semantics { this.selected = selected }
                            .clickable(role = Role.RadioButton) { draft = draft.copy(skin = skin) }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CassetteArt(skin = skin, labelColor = Color(draft.labelColor), title = "", sideLetter = "A", progress = { 0.4f })
                        Spacer(Modifier.padding(3.dp))
                        Text(name, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }

            Text(stringResource(R.string.editor_cover), style = MaterialTheme.typography.labelMedium, color = palette.accent)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val cover = rememberCoverBitmap(draft.coverPath, 256)
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surfaceRaised),
                    contentAlignment = Alignment.Center,
                ) {
                    if (cover != null) {
                        Image(cover, contentDescription = stringResource(R.string.cd_cover_art), contentScale = ContentScale.Crop, modifier = Modifier.size(76.dp))
                    } else {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = palette.textMuted)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text(stringResource(if (draft.coverPath == null) R.string.editor_pick_cover else R.string.editor_change_cover)) }
                    if (draft.coverPath != null) {
                        TextButton(onClick = { draft = draft.copy(coverPath = null) }) { Text(stringResource(R.string.editor_remove_cover)) }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = ::dismissWithoutSaving, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_cancel)) }
                Button(
                    onClick = {
                        // Covers imported in this session but not kept are cleaned up.
                        imported.filter { it != draft.coverPath }.forEach(editor::discardCover)
                        editor.save(draft) { id -> onSaved(id) }
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}
