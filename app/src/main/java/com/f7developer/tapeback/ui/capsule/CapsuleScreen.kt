package com.f7developer.tapeback.ui.capsule

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.f7developer.tapeback.R
import com.f7developer.tapeback.di.AppContainer
import com.f7developer.tapeback.domain.model.RecordingItem
import com.f7developer.tapeback.domain.policy.CapsulePolicy
import com.f7developer.tapeback.domain.policy.TimeFormat
import com.f7developer.tapeback.ui.appContainer
import com.f7developer.tapeback.ui.components.CassetteArt
import com.f7developer.tapeback.ui.components.EmptyState
import com.f7developer.tapeback.ui.components.ScreenHeader
import com.f7developer.tapeback.ui.components.SectionLabel
import com.f7developer.tapeback.ui.recording.RecordingSheet
import com.f7developer.tapeback.ui.theme.LcdFamily
import com.f7developer.tapeback.ui.theme.Retro
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

class CapsuleViewModel(private val container: AppContainer) : ViewModel() {
    val capsules: StateFlow<List<RecordingItem>?> = container.recordingRepository.observeItems()
        .map { items -> items.filter { it.recording.isCapsule } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun recordCapsule() {
        container.capsuleModeRequest.value = true
    }

    fun play(item: RecordingItem) = viewModelScope.launch {
        container.playerConnection.load(listOf(item), 0, playWhenReady = true)
    }
}

/** Ticks once per second for live countdowns. */
@Composable
fun rememberNow(periodMs: Long = 1_000L): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(periodMs) {
        while (true) {
            now = System.currentTimeMillis()
            delay(periodMs - (now % periodMs))
        }
    }
    return now
}

@Composable
fun CapsuleCountdownText(unlockAt: Long, modifier: Modifier = Modifier) {
    val now = rememberNow()
    val c = CapsulePolicy.countdown(unlockAt, now)
    val text = String.format(java.util.Locale.ENGLISH, "%dd %02dh %02dm %02ds", c.days, c.hours, c.minutes, c.seconds)
    val description = stringResource(R.string.cd_countdown, pluralStringResource(R.plurals.days_count, c.days.toInt(), c.days.toInt()), c.hours, c.minutes)
    Text(
        text = text,
        style = TextStyle(fontFamily = LcdFamily, fontSize = 26.sp, color = Retro.palette.lcdText),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Retro.palette.lcdBg)
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .semantics { contentDescription = description },
    )
}

@Composable
fun CapsuleScreen(onRecordCapsule: () -> Unit, onOpenDeck: () -> Unit, contentPadding: PaddingValues) {
    val container = appContainer()
    val vm: CapsuleViewModel = viewModel { CapsuleViewModel(container) }
    val capsules by vm.capsules.collectAsStateWithLifecycle()
    val now = rememberNow()
    val zone = remember { ZoneId.systemDefault() }
    var sheetFor by remember { mutableStateOf<Long?>(null) }
    val palette = Retro.palette

    val all = capsules.orEmpty()
    val sealed = all.filter { CapsulePolicy.isLocked(it.recording, now) }.sortedBy { it.recording.capsuleUnlockAt }
    val opened = all.filter { !CapsulePolicy.isLocked(it.recording, now) }.sortedByDescending { it.recording.capsuleUnlockAt }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(title = stringResource(R.string.tab_capsules), subtitle = stringResource(R.string.capsules_subtitle))
        }
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(palette.accent2.copy(alpha = 0.35f), palette.accent.copy(alpha = 0.25f))))
                    .border(1.dp, palette.accent.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.capsules_hero_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.capsules_hero_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = {
                        vm.recordCapsule()
                        onRecordCapsule()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.rec, contentColor = Color.White),
                ) {
                    Icon(Icons.Rounded.FiberManualRecord, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.capsules_record_new))
                }
            }
        }
        if (capsules != null && all.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.HourglassTop,
                    title = stringResource(R.string.capsules_empty_title),
                    body = stringResource(R.string.capsules_empty_body),
                )
            }
        }
        if (sealed.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.capsules_sealed_section, sealed.size)) }
            items(sealed, key = { it.recording.id }) { item ->
                SealedCapsuleCard(item, now, zone, onClick = { sheetFor = item.recording.id })
            }
        }
        if (opened.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.capsules_opened_section, opened.size)) }
            items(opened, key = { it.recording.id }) { item ->
                OpenedCapsuleCard(
                    item, zone,
                    onPlay = {
                        vm.play(item)
                        onOpenDeck()
                    },
                    onClick = { sheetFor = item.recording.id },
                )
            }
        }
    }

    sheetFor?.let { id ->
        RecordingSheet(recordingId = id, onDismiss = { sheetFor = null }, onPlayInDeck = onOpenDeck)
    }
}

@Composable
private fun SealedCapsuleCard(item: RecordingItem, now: Long, zone: ZoneId, onClick: () -> Unit) {
    val r = item.recording
    val unlockAt = r.capsuleUnlockAt ?: return
    val palette = Retro.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.surfaceRaised)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CassetteArt(
            skin = item.cassetteSkin,
            labelColor = Color(item.labelColor),
            title = item.cassetteTitle,
            sideLetter = r.side.letter,
            sealedText = stringResource(R.string.sealed),
            modifier = Modifier.width(112.dp),
            contentDescription = stringResource(R.string.cd_sealed_capsule, r.title),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(r.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
            Text(
                stringResource(R.string.capsule_opens_on, TimeFormat.shortDate(unlockAt, zone)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CapsuleCountdownText(unlockAt)
            val sealedAt = r.capsuleSealedAt ?: r.createdAt
            LinearProgressIndicator(
                progress = { CapsulePolicy.sealProgress(sealedAt, unlockAt, now) },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                color = palette.accent,
                trackColor = palette.outline.copy(alpha = 0.4f),
                drawStopIndicator = {},
            )
        }
    }
}

@Composable
private fun OpenedCapsuleCard(item: RecordingItem, zone: ZoneId, onPlay: () -> Unit, onClick: () -> Unit) {
    val r = item.recording
    val palette = Retro.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.surface)
            .border(1.dp, palette.play.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CassetteArt(
                skin = item.cassetteSkin,
                labelColor = Color(item.labelColor),
                title = item.cassetteTitle,
                sideLetter = r.side.letter,
                modifier = Modifier.width(96.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.capsule_opened_badge), style = MaterialTheme.typography.labelSmall, color = palette.play)
                Text(r.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                Text(
                    stringResource(R.string.capsule_recorded_on, TimeFormat.shortDate(r.createdAt, zone)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = onPlay) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Text(stringResource(R.string.action_play))
            }
        }
        if (!r.capsuleNote.isNullOrBlank()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surfaceRaised)
                    .padding(12.dp),
            ) {
                Text("“${r.capsuleNote}”", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.widthIn(max = 560.dp))
            }
        }
    }
}
