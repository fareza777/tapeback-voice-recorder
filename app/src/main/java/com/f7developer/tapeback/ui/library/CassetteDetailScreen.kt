package com.f7developer.tapeback.ui.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.f7developer.tapeback.R
import com.f7developer.tapeback.domain.model.Cassette
import com.f7developer.tapeback.domain.model.RecordingItem
import com.f7developer.tapeback.domain.model.TapeSide
import com.f7developer.tapeback.domain.policy.CapsulePolicy
import com.f7developer.tapeback.domain.policy.TimeFormat
import com.f7developer.tapeback.ui.appContainer
import com.f7developer.tapeback.ui.components.CassetteArt
import com.f7developer.tapeback.ui.components.EmptyState
import com.f7developer.tapeback.ui.components.MiniWaveform
import com.f7developer.tapeback.ui.components.rememberCoverBitmap
import com.f7developer.tapeback.ui.recording.RecordingSheet
import com.f7developer.tapeback.ui.theme.BungeeFamily
import com.f7developer.tapeback.ui.theme.MarkerFamily
import com.f7developer.tapeback.ui.theme.Retro
import java.time.ZoneId

@Composable
fun CassetteDetailScreen(
    cassetteId: Long,
    onBack: () -> Unit,
    onOpenDeck: () -> Unit,
    contentPadding: PaddingValues,
) {
    val container = appContainer()
    val vm: CassetteDetailViewModel = viewModel(key = "cassette-$cassetteId") { CassetteDetailViewModel(container, cassetteId) }
    val state by vm.state.collectAsStateWithLifecycle()
    var side by rememberSaveable { mutableStateOf(TapeSide.A) }
    var editing by remember { mutableStateOf<CassetteDraft?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var sheetFor by remember { mutableStateOf<Long?>(null) }
    val palette = Retro.palette
    val zone = remember { ZoneId.systemDefault() }

    val cassette = state.cassette
    if (!state.loading && cassette == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onBack() }
        return
    }
    val tracks = if (side == TapeSide.A) state.sideA else state.sideB

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back)) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { cassette?.let { editing = CassetteDraft.from(it) } }) {
                    Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.cd_edit_cassette))
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.cd_delete_cassette))
                }
            }
        }
        if (cassette != null) {
            item { JCardHeader(cassette, state.sideA.size + state.sideB.size, (state.sideA + state.sideB).sumOf { it.recording.durationMs }) }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = {
                            vm.playSide(side)
                            onOpenDeck()
                        },
                        enabled = tracks.any { !CapsulePolicy.isLocked(it.recording, System.currentTimeMillis()) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.play_side, side.letter))
                    }
                    OutlinedButton(
                        onClick = {
                            vm.recordOnSide(side)
                            onOpenDeck()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.rec),
                    ) {
                        Icon(Icons.Rounded.FiberManualRecord, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.record_on_side, side.letter))
                    }
                }
            }
            item {
                SecondaryTabRow(selectedTabIndex = side.ordinal, containerColor = Color.Transparent) {
                    TapeSide.entries.forEach { s ->
                        val count = if (s == TapeSide.A) state.sideA.size else state.sideB.size
                        Tab(
                            selected = side == s,
                            onClick = { side = s },
                            text = { Text(stringResource(R.string.side_tab, s.letter, count)) },
                        )
                    }
                }
            }
            if (tracks.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.FiberManualRecord,
                        title = stringResource(R.string.side_blank_title, side.letter),
                        body = stringResource(R.string.side_blank_body),
                    )
                }
            }
            itemsIndexed(tracks, key = { _, it -> it.recording.id }) { index, item ->
                TrackRow(
                    index = index,
                    item = item,
                    zone = zone,
                    onPlay = {
                        vm.playSide(side, index)
                        onOpenDeck()
                    },
                    onMore = { sheetFor = item.recording.id },
                )
            }
        }
    }

    editing?.let { draft -> CassetteEditorSheet(initial = draft, editor = vm, onDismiss = { editing = null }) }
    sheetFor?.let { id -> RecordingSheet(recordingId = id, onDismiss = { sheetFor = null }, onPlayInDeck = onOpenDeck) }
    if (confirmDelete && cassette != null) {
        val count = state.sideA.size + state.sideB.size
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_cassette_title, cassette.title)) },
            text = { Text(stringResource(R.string.delete_cassette_body, pluralStringResource(R.plurals.recordings_count, count, count))) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete(onBack)
                }) { Text(stringResource(R.string.action_delete), color = palette.rec) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** The cassette case insert ("J-card"): cover artwork on the spine side, cassette in the case. */
@Composable
private fun JCardHeader(cassette: Cassette, count: Int, totalMs: Long) {
    val palette = Retro.palette
    val cover = rememberCoverBitmap(cassette.coverPath, 768)
    val label = Color(cassette.labelColor)
    val ink = if (label.luminance() > 0.45f) Color(0xFF1E1A16) else Color(0xFFF7F3EA)
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(label, label.copy(alpha = 0.6f), palette.accent.copy(alpha = 0.5f))))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp)),
        ) {
            if (cover != null) {
                Image(cover, contentDescription = stringResource(R.string.cd_cover_art), contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.1f), Color.Black.copy(alpha = 0.65f)))))
            }
            Column(Modifier.padding(18.dp)) {
                Text(
                    cassette.category.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (cover != null) Color.White.copy(alpha = 0.85f) else ink.copy(alpha = 0.75f),
                )
                Text(
                    cassette.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = MarkerFamily),
                    color = if (cover != null) Color.White else ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                CassetteArt(
                    skin = cassette.skin,
                    labelColor = label,
                    title = cassette.title,
                    sideLetter = "A",
                    subtitle = cassette.category,
                    progress = { 0.3f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.cassette_summary, pluralStringResource(R.plurals.recordings_count, count, count), TimeFormat.humanTotal(totalMs)),
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = BungeeFamily),
                    color = if (cover != null) Color.White else ink,
                )
            }
        }
    }
}

@Composable
private fun TrackRow(index: Int, item: RecordingItem, zone: ZoneId, onPlay: () -> Unit, onMore: () -> Unit) {
    val r = item.recording
    val palette = Retro.palette
    val locked = CapsulePolicy.isLocked(r, System.currentTimeMillis())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = if (locked) onMore else onPlay)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = (index + 1).toString().padStart(2, '0'),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = BungeeFamily),
            color = palette.accent,
            modifier = Modifier.width(40.dp),
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (locked) Icon(Icons.Rounded.Lock, contentDescription = stringResource(R.string.sealed), tint = palette.accent, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                if (r.favorite) Icon(Icons.Rounded.Star, contentDescription = stringResource(R.string.cd_favorite_badge), tint = palette.accent2, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                Text(r.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                "${TimeFormat.shortDate(r.createdAt, zone)} · ${TimeFormat.duration(r.durationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!locked) MiniWaveform(r.waveform, palette.accent.copy(alpha = 0.55f), Modifier.fillMaxWidth().height(18.dp).padding(top = 4.dp))
        }
        IconButton(onClick = onMore) { Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.cd_recording_options, r.title)) }
    }
}
