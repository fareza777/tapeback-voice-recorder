package com.f7developer.tapeback.ui.deck

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f7developer.tapeback.R
import com.f7developer.tapeback.audio.play.SleepTimer
import com.f7developer.tapeback.domain.model.RepeatMode
import com.f7developer.tapeback.domain.policy.TimeFormat
import com.f7developer.tapeback.ui.theme.BungeeFamily
import com.f7developer.tapeback.ui.theme.Retro
import kotlinx.coroutines.delay

/** Big pre-roll number over the cassette well; each tick pops in. */
@Composable
fun BoxScope.CountdownOverlay(seconds: Int) {
    val palette = Retro.palette
    val reduceMotion = Retro.reduceMotion
    val scale = remember { Animatable(1f) }
    LaunchedEffect(seconds) {
        if (!reduceMotion) {
            scale.snapTo(1.35f)
            scale.animateTo(1f, tween(380))
        }
    }
    val description = pluralStringResource(R.plurals.cd_countdown, seconds, seconds)
    Box(
        Modifier
            .matchParentSize()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .semantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            seconds.toString(),
            fontFamily = BungeeFamily,
            fontSize = 96.sp,
            color = palette.rec,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        )
    }
}

/** Repeat, sleep timer and skip-silence controls under the transport. */
@Composable
fun PlaybackOptions(
    repeat: RepeatMode,
    skipSilence: Boolean,
    sleep: SleepTimer?,
    onCycleRepeat: () -> Unit,
    onSkipSilence: (Boolean) -> Unit,
    onSleep: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Retro.palette
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val repeatLabel = stringResource(
            when (repeat) {
                RepeatMode.OFF -> R.string.repeat_off
                RepeatMode.SIDE -> R.string.repeat_side
                RepeatMode.ONE -> R.string.repeat_one
            },
        )
        FilterChip(
            selected = repeat != RepeatMode.OFF,
            onClick = onCycleRepeat,
            label = { Text(repeatLabel, maxLines = 1) },
            leadingIcon = {
                Icon(
                    if (repeat == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
        )

        Box {
            var open by remember { mutableStateOf(false) }
            var remaining by remember { mutableLongStateOf(sleep?.remainingMs() ?: 0L) }
            LaunchedEffect(sleep) {
                while (sleep?.endsAtElapsed != null) {
                    remaining = sleep.remainingMs()
                    delay(1_000)
                }
            }
            val label = when {
                sleep == null -> stringResource(R.string.sleep_timer)
                sleep.endOfTrack -> stringResource(R.string.sleep_end_short)
                else -> stringResource(R.string.sleep_remaining, TimeFormat.lcd(remaining))
            }
            FilterChip(
                selected = sleep != null,
                onClick = { open = true },
                label = { Text(label, maxLines = 1) },
                leadingIcon = { Icon(Icons.Rounded.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                Text(
                    stringResource(R.string.sleep_menu_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.accent,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                DeckViewModel.SLEEP_CHOICES.forEach { minutes ->
                    DropdownMenuItem(
                        text = { Text(pluralStringResource(R.plurals.minutes_count, minutes, minutes)) },
                        onClick = {
                            open = false
                            onSleep(minutes)
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sleep_end_of_track)) },
                    trailingIcon = { if (sleep?.endOfTrack == true) Icon(Icons.Rounded.Check, contentDescription = null) },
                    onClick = {
                        open = false
                        onSleep(DeckViewModel.SLEEP_END_OF_TRACK)
                    },
                )
                if (sleep != null) {
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sleep_off)) },
                        onClick = {
                            open = false
                            onSleep(0)
                        },
                    )
                }
            }
        }

        FilterChip(
            selected = skipSilence,
            onClick = { onSkipSilence(!skipSilence) },
            label = { Text(stringResource(R.string.skip_silence), maxLines = 1) },
            leadingIcon = {
                Icon(
                    if (skipSilence) Icons.Rounded.Check else Icons.Rounded.FastForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
    }
}
