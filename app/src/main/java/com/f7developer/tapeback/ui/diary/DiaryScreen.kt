package com.f7developer.tapeback.ui.diary

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.f7developer.tapeback.R
import com.f7developer.tapeback.di.AppContainer
import com.f7developer.tapeback.domain.model.RecordingItem
import com.f7developer.tapeback.domain.policy.DiaryLogic
import com.f7developer.tapeback.domain.policy.TimeFormat
import com.f7developer.tapeback.ui.appContainer
import com.f7developer.tapeback.ui.components.CassetteArt
import com.f7developer.tapeback.ui.components.EmptyState
import com.f7developer.tapeback.ui.components.MiniWaveform
import com.f7developer.tapeback.ui.components.ScreenHeader
import com.f7developer.tapeback.ui.recording.RecordingSheet
import com.f7developer.tapeback.ui.theme.BungeeFamily
import com.f7developer.tapeback.ui.theme.Retro
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class DiaryUiState(
    val loading: Boolean = true,
    val filter: DiaryLogic.Filter = DiaryLogic.Filter(),
    val groups: List<DiaryLogic.DayGroup> = emptyList(),
    val stats: DiaryLogic.Stats = DiaryLogic.Stats(0, 0, 0),
    val tags: List<String> = emptyList(),
    val onThisDay: List<RecordingItem> = emptyList(),
    val hasAny: Boolean = false,
)

class DiaryViewModel(private val container: AppContainer, private val zone: ZoneId = ZoneId.systemDefault()) : ViewModel() {
    private val filter = MutableStateFlow(DiaryLogic.Filter())

    val state: StateFlow<DiaryUiState> = combine(container.recordingRepository.observeItems(), filter) { items, f ->
        val now = System.currentTimeMillis()
        val visible = DiaryLogic.apply(items, f, now)
        DiaryUiState(
            loading = false,
            filter = f,
            groups = DiaryLogic.groupByDay(visible, zone),
            stats = DiaryLogic.stats(items, now, zone),
            tags = DiaryLogic.allTags(items).take(20),
            onThisDay = if (f == DiaryLogic.Filter()) DiaryLogic.onThisDay(items, now, zone) else emptyList(),
            hasAny = items.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiaryUiState())

    fun setQuery(q: String) = filter.update { it.copy(query = q.take(60)) }
    fun toggleFavorites() = filter.update { it.copy(favoritesOnly = !it.favoritesOnly) }
    fun selectTag(tag: String?) = filter.update { it.copy(tag = if (it.tag == tag) null else tag) }
    fun clearFilters() = filter.update { DiaryLogic.Filter() }

    fun play(item: RecordingItem) = viewModelScope.launch {
        val side = container.recordingRepository.bySide(item.recording.cassetteId, item.recording.side)
        val index = side.indexOfFirst { it.recording.id == item.recording.id }.coerceAtLeast(0)
        container.playerConnection.load(side, index, playWhenReady = true)
    }
}

@Composable
fun DiaryScreen(onOpenDeck: () -> Unit, contentPadding: PaddingValues) {
    val container = appContainer()
    val vm: DiaryViewModel = viewModel { DiaryViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    var sheetFor by remember { mutableStateOf<Long?>(null) }
    val palette = Retro.palette

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { ScreenHeader(stringResource(R.string.tab_diary), stringResource(R.string.diary_subtitle)) }
        item { StatsRow(state.stats) }
        item {
            OutlinedTextField(
                value = state.filter.query,
                onValueChange = vm::setQuery,
                placeholder = { Text(stringResource(R.string.diary_search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.filter.query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_clear_search)) }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = palette.surface, focusedContainerColor = palette.surface),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.filter.favoritesOnly,
                        onClick = vm::toggleFavorites,
                        label = { Text(stringResource(R.string.diary_favorites)) },
                        leadingIcon = { Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = palette.accent.copy(alpha = 0.25f)),
                    )
                }
                items(state.tags) { tag ->
                    FilterChip(
                        selected = state.filter.tag == tag,
                        onClick = { vm.selectTag(tag) },
                        label = { Text("#$tag") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = palette.accent.copy(alpha = 0.25f)),
                    )
                }
            }
        }
        if (state.onThisDay.isNotEmpty()) {
            item { OnThisDayCard(state.onThisDay, zone, today, onPlay = { vm.play(it); onOpenDeck() }) }
        }
        if (!state.loading && state.groups.isEmpty()) {
            item {
                if (state.hasAny) {
                    EmptyState(
                        icon = Icons.Rounded.Search,
                        title = stringResource(R.string.diary_no_match_title),
                        body = stringResource(R.string.diary_no_match_body),
                        action = { androidx.compose.material3.TextButton(onClick = vm::clearFilters) { Text(stringResource(R.string.diary_clear_filters)) } },
                    )
                } else {
                    EmptyState(
                        icon = Icons.Rounded.AutoStories,
                        title = stringResource(R.string.diary_empty_title),
                        body = stringResource(R.string.diary_empty_body),
                        action = { androidx.compose.material3.Button(onClick = onOpenDeck) { Text(stringResource(R.string.diary_go_record)) } },
                    )
                }
            }
        }
        state.groups.forEach { group ->
            item(key = "h-${group.date}") {
                Text(
                    text = dayLabel(group.date, today),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accent,
                    modifier = Modifier
                        .padding(top = 14.dp, bottom = 2.dp)
                        .semantics { heading() },
                )
            }
            items(group.items, key = { it.recording.id }) { item ->
                DiaryRow(item, zone, onClick = { vm.play(item); onOpenDeck() }, onMore = { sheetFor = item.recording.id })
            }
        }
    }

    sheetFor?.let { id -> RecordingSheet(recordingId = id, onDismiss = { sheetFor = null }, onPlayInDeck = onOpenDeck) }
}

@Composable
private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.today)
    today.minusDays(1) -> stringResource(R.string.yesterday)
    else -> TimeFormat.dayHeader(date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), ZoneId.systemDefault())
}

@Composable
private fun StatsRow(stats: DiaryLogic.Stats) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StatTile(stats.entries.toString(), stringResource(R.string.stat_entries), Modifier.weight(1f))
        StatTile(TimeFormat.humanTotal(stats.totalDurationMs), stringResource(R.string.stat_recorded), Modifier.weight(1f))
        StatTile(stats.streakDays.toString(), stringResource(R.string.stat_streak), Modifier.weight(1f), highlight = stats.streakDays >= 2)
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier, highlight: Boolean = false) {
    val palette = Retro.palette
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (highlight) Brush.linearGradient(listOf(palette.accent.copy(alpha = 0.35f), palette.accent2.copy(alpha = 0.2f))) else Brush.linearGradient(listOf(palette.surfaceRaised, palette.surfaceRaised)))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontFamily = BungeeFamily), color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun OnThisDayCard(items: List<RecordingItem>, zone: ZoneId, today: LocalDate, onPlay: (RecordingItem) -> Unit) {
    val palette = Retro.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(palette.accent2.copy(alpha = 0.3f), palette.surfaceRaised)))
            .border(1.dp, palette.accent2.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.HourglassBottom, contentDescription = null, tint = palette.accent2)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.on_this_day), style = MaterialTheme.typography.titleSmall)
        }
        items.take(3).forEach { item ->
            val years = today.year - DiaryLogic.localDate(item.recording.createdAt, zone).year
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPlay(item) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    androidx.compose.ui.res.pluralStringResource(R.plurals.years_ago, years, years),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accent2,
                    modifier = Modifier.width(92.dp),
                )
                Text(item.recording.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun DiaryRow(item: RecordingItem, zone: ZoneId, onClick: () -> Unit, onMore: () -> Unit) {
    val r = item.recording
    val palette = Retro.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .border(1.dp, palette.outline.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(64.dp)) {
            CassetteArt(
                skin = item.cassetteSkin,
                labelColor = Color(item.labelColor),
                title = "",
                sideLetter = r.side.letter,
                modifier = Modifier.width(64.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (r.favorite) {
                    Icon(Icons.Rounded.Star, contentDescription = stringResource(R.string.cd_favorite_badge), tint = palette.accent2, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                if (r.isCapsule) {
                    Icon(Icons.Rounded.HourglassBottom, contentDescription = stringResource(R.string.cd_capsule_badge), tint = palette.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(r.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                stringResource(R.string.diary_row_meta, TimeFormat.clock(r.createdAt, zone), TimeFormat.duration(r.durationMs), item.cassetteTitle, r.side.letter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (r.tags.isNotEmpty()) {
                Text(
                    r.tags.joinToString("  ") { "#$it" },
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            MiniWaveform(r.waveform, palette.accent.copy(alpha = 0.5f), Modifier.fillMaxWidth().height(16.dp).padding(top = 4.dp))
        }
        IconButton(onClick = onMore) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.cd_recording_options, r.title))
        }
    }
}
