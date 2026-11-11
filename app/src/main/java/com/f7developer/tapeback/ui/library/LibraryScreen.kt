package com.f7developer.tapeback.ui.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.f7developer.tapeback.R
import com.f7developer.tapeback.domain.model.CassetteSummary
import com.f7developer.tapeback.domain.policy.TimeFormat
import com.f7developer.tapeback.ui.appContainer
import com.f7developer.tapeback.ui.components.CassetteArt
import com.f7developer.tapeback.ui.components.EmptyState
import com.f7developer.tapeback.ui.components.ScreenHeader
import com.f7developer.tapeback.ui.components.rememberCoverBitmap
import com.f7developer.tapeback.ui.theme.Retro

@Composable
fun LibraryScreen(onOpenCassette: (Long) -> Unit, contentPadding: PaddingValues) {
    val container = appContainer()
    val vm: LibraryViewModel = viewModel { LibraryViewModel(container) }
    val cassettes by vm.cassettes.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<CassetteDraft?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 96.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val list = cassettes.orEmpty()
                ScreenHeader(
                    title = stringResource(R.string.tab_library),
                    subtitle = stringResource(
                        R.string.library_subtitle,
                        list.size,
                        list.sumOf { it.totalCount },
                        TimeFormat.humanTotal(list.sumOf { it.totalDurationMs }),
                    ),
                )
            }
            val list = cassettes
            if (list != null && list.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        icon = Icons.Rounded.LibraryMusic,
                        title = stringResource(R.string.library_empty_title),
                        body = stringResource(R.string.library_empty_body),
                    )
                }
            }
            items(list.orEmpty(), key = { it.cassette.id }) { summary ->
                CassetteCard(summary, onClick = { onOpenCassette(summary.cassette.id) })
            }
        }
        ExtendedFloatingActionButton(
            onClick = { editing = CassetteDraft() },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.library_new_tape)) },
            containerColor = Retro.palette.accent,
            contentColor = Retro.palette.onAccent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
        )
    }

    editing?.let { draft ->
        CassetteEditorSheet(initial = draft, editor = vm, onDismiss = { editing = null })
    }
}

@Composable
fun CassetteCard(summary: CassetteSummary, onClick: () -> Unit) {
    val c = summary.cassette
    val palette = Retro.palette
    val cover = rememberCoverBitmap(c.coverPath, 384)
    val description = stringResource(R.string.cd_cassette_card, c.title, summary.totalCount)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.outline.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.25f)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(c.labelColor).copy(alpha = 0.55f), palette.surfaceRaised),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (cover != null) {
                Image(
                    bitmap = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()
                            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))))
                        },
                )
            }
            CassetteArt(
                skin = c.skin,
                labelColor = Color(c.labelColor),
                title = c.title,
                sideLetter = "A",
                progress = { 0.3f },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            )
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(c.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                c.category.ifBlank { "—" },
                style = MaterialTheme.typography.labelMedium,
                color = palette.accent,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.cassette_stats, summary.sideACount, summary.sideBCount, TimeFormat.humanTotal(summary.totalDurationMs)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
