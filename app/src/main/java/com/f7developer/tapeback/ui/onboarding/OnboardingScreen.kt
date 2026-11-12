package com.f7developer.tapeback.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.f7developer.tapeback.R
import com.f7developer.tapeback.core.AppActions
import com.f7developer.tapeback.domain.model.AppSettings
import com.f7developer.tapeback.domain.model.CassetteSkin
import com.f7developer.tapeback.domain.model.DarkMode
import com.f7developer.tapeback.domain.model.RetroThemeId
import com.f7developer.tapeback.ui.components.CassetteArt
import com.f7developer.tapeback.ui.components.DarkModeSelector
import com.f7developer.tapeback.ui.components.DeckPanel
import com.f7developer.tapeback.ui.components.ThemePickerRow
import com.f7developer.tapeback.ui.components.rememberPermissionState
import com.f7developer.tapeback.ui.components.rememberReelMotion
import com.f7developer.tapeback.ui.theme.BungeeFamily
import com.f7developer.tapeback.ui.theme.Retro
import kotlinx.coroutines.launch

private enum class Page { WELCOME, KEYS, LIBRARY, DIARY, CAPSULES, STYLE, PERMISSIONS }

private val pages = Page.entries

/**
 * First-run tour. Seven short pages: what TapeBack is, how the deck keys work, cassettes, diary,
 * time capsules, a live theme picker and finally the two permissions, each with its own Allow button.
 */
@Composable
fun OnboardingScreen(
    settings: AppSettings,
    onTheme: (RetroThemeId) -> Unit,
    onDarkMode: (DarkMode) -> Unit,
    onDone: () -> Unit,
) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val palette = Retro.palette
    val context = LocalContext.current
    val activity = LocalActivity.current
    val permissions = rememberPermissionState()
    var micAsked by rememberSaveable { mutableStateOf(false) }
    var notifAsked by rememberSaveable { mutableStateOf(false) }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        micAsked = true
        permissions.refresh()
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifAsked = true
        permissions.refresh()
    }
    val finishLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onDone() }

    fun canAsk(permission: String, asked: Boolean): Boolean =
        !asked || (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, permission))

    fun allowMic() {
        if (canAsk(Manifest.permission.RECORD_AUDIO, micAsked)) micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        else AppActions.openAppSettings(context)
    }

    fun allowNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && canAsk(Manifest.permission.POST_NOTIFICATIONS, notifAsked)) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            AppActions.openNotificationSettings(context)
        }
    }

    fun finish() {
        val p = permissions.value
        val missing = buildList {
            if (!p.microphone && !micAsked) add(Manifest.permission.RECORD_AUDIO)
            if (!p.notifications && !notifAsked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (missing.isEmpty()) onDone() else finishLauncher.launch(missing.toTypedArray())
    }

    val current = pager.currentPage
    val last = current == pages.lastIndex
    val pageDescription = stringResource(R.string.cd_page_of, current + 1, pages.size)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.brand_wordmark),
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = BungeeFamily),
                color = palette.accent,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.onb_step, current + 1, pages.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (!last) {
                TextButton(onClick = { scope.launch { pager.animateScrollToPage(pages.lastIndex) } }) {
                    Text(stringResource(R.string.onb_skip))
                }
            }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f), beyondViewportPageCount = 1) { index ->
            BoxWithConstraints(Modifier.fillMaxSize()) {
            val pageHeight = maxHeight
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = pageHeight)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when (pages[index]) {
                    Page.WELCOME -> {
                        ArtBox(260.dp) { SpinningCassette() }
                        PageText(R.string.onb1_title, R.string.onb1_body)
                    }
                    Page.KEYS -> {
                        PageText(R.string.onb_keys_title, R.string.onb_keys_body)
                        Spacer(Modifier.height(16.dp))
                        KeyGuide()
                    }
                    Page.LIBRARY -> {
                        ArtBox(230.dp) { CassetteStack() }
                        PageText(R.string.onb2_title, R.string.onb2_body)
                        Points(
                            Icons.Rounded.SwapVert to R.string.onb2_point1,
                            Icons.Rounded.Edit to R.string.onb2_point2,
                            Icons.Rounded.Share to R.string.onb2_point3,
                        )
                    }
                    Page.DIARY -> {
                        ArtBox(200.dp) { DiaryCassette() }
                        PageText(R.string.onb_diary_title, R.string.onb_diary_body)
                        Points(
                            Icons.Rounded.Search to R.string.onb_diary_point1,
                            Icons.Rounded.Favorite to R.string.onb_diary_point2,
                            Icons.Rounded.Today to R.string.onb_diary_point3,
                        )
                    }
                    Page.CAPSULES -> {
                        ArtBox(200.dp) { SealedCassette() }
                        PageText(R.string.onb3_title, R.string.onb3_body)
                        Points(
                            Icons.Rounded.Lock to R.string.onb3_point1,
                            Icons.Rounded.NotificationsActive to R.string.onb3_point2,
                            Icons.Rounded.Mail to R.string.onb3_point3,
                        )
                    }
                    Page.STYLE -> {
                        StylePreview()
                        PageText(R.string.onb_style_title, R.string.onb_style_body)
                        Spacer(Modifier.height(16.dp))
                        ThemePickerRow(selected = settings.theme, onSelect = onTheme, modifier = Modifier.widthIn(max = 520.dp))
                        Spacer(Modifier.height(12.dp))
                        DarkModeSelector(settings.darkMode, onDarkMode, Modifier.widthIn(max = 420.dp))
                    }
                    Page.PERMISSIONS -> {
                        ArtBox(170.dp) { PrivacyArt() }
                        PageText(R.string.onb4_title, R.string.onb4_body)
                        Spacer(Modifier.height(16.dp))
                        val p = permissions.value
                        PermissionCard(Icons.Rounded.Mic, R.string.onb_perm_mic, R.string.onb_perm_mic_body, p.microphone, ::allowMic)
                        Spacer(Modifier.height(10.dp))
                        PermissionCard(
                            Icons.Rounded.Notifications,
                            R.string.onb_perm_notif,
                            R.string.onb_perm_notif_body,
                            p.notifications,
                            ::allowNotifications,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.onb_perm_later),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(96.dp)) {
                if (current > 0) {
                    TextButton(onClick = { scope.launch { pager.animateScrollToPage(current - 1) } }) {
                        Text(stringResource(R.string.onb_back))
                    }
                }
            }
            Row(
                Modifier.weight(1f).semantics { contentDescription = pageDescription },
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                pages.indices.forEach { i ->
                    val selected = i == current
                    val w by animateDpAsState(if (selected) 22.dp else 8.dp, label = "dot")
                    val c by animateColorAsState(if (selected) palette.accent else palette.outline, label = "dotColor")
                    Box(Modifier.height(8.dp).width(w).clip(CircleShape).background(c))
                }
            }
            Box(Modifier.width(if (last) 132.dp else 96.dp), contentAlignment = Alignment.CenterEnd) {
                Button(onClick = { if (last) finish() else scope.launch { pager.animateScrollToPage(current + 1) } }) {
                    Text(stringResource(if (last) R.string.onb_start else R.string.onb_next), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ArtBox(height: Dp, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) { content() }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun PageText(@StringRes title: Int, @StringRes body: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 480.dp).semantics { heading() },
    )
    Spacer(Modifier.height(10.dp))
    Text(
        stringResource(body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 480.dp),
    )
}

@Composable
private fun Points(vararg points: Pair<ImageVector, Int>) {
    Spacer(Modifier.height(16.dp))
    Column(Modifier.widthIn(max = 420.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        points.forEach { (icon, text) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Retro.palette.accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = Retro.palette.accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(stringResource(text), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private data class KeyInfo(val icon: ImageVector, val label: Int, val body: Int, val red: Boolean = false)

@Composable
private fun KeyGuide() {
    val keys = listOf(
        KeyInfo(Icons.Rounded.FiberManualRecord, R.string.key_rec, R.string.onb_key_rec, red = true),
        KeyInfo(Icons.Rounded.Pause, R.string.key_pause, R.string.onb_key_pause),
        KeyInfo(Icons.Rounded.Stop, R.string.key_stop, R.string.onb_key_stop),
        KeyInfo(Icons.Rounded.PlayArrow, R.string.key_play, R.string.onb_key_play),
        KeyInfo(Icons.Rounded.FastForward, R.string.onb_wind_label, R.string.onb_key_wind),
        KeyInfo(Icons.Rounded.SwapVert, R.string.onb_side_label, R.string.onb_key_side),
    )
    DeckPanel(Modifier.widthIn(max = 480.dp).fillMaxWidth(), contentPadding = 12.dp) {
        keys.forEachIndexed { i, k ->
            if (i > 0) Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                KeyCap(k.icon, k.red)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(k.label),
                        style = MaterialTheme.typography.labelLarge,
                        color = Retro.palette.panelText,
                    )
                    Text(
                        stringResource(k.body),
                        style = MaterialTheme.typography.bodySmall,
                        color = Retro.palette.panelText.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

/** A miniature piano key in the palette's key colors. */
@Composable
private fun KeyCap(icon: ImageVector, red: Boolean, size: Dp = 44.dp) {
    val palette = Retro.palette
    Box(
        modifier = Modifier
            .size(size)
            .shadow(4.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(palette.keyTop, palette.keyBottom)))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (red) palette.rec else palette.keyText, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
private fun StylePreview() {
    DeckPanel(Modifier.widthIn(max = 360.dp).fillMaxWidth(), contentPadding = 14.dp) {
        val reels = rememberReelMotion(speed = 1f, progress = { 0.6f }, reduceMotion = Retro.reduceMotion)
        CassetteArt(
            skin = CassetteSkin.SMOKED_CLEAR,
            labelColor = Retro.palette.accent2,
            title = stringResource(R.string.art_press_rec),
            sideLetter = "A",
            progress = { 0.6f },
            leftAngle = { reels.left },
            rightAngle = { reels.right },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            KeyCap(Icons.Rounded.FiberManualRecord, red = true, size = 48.dp)
            KeyCap(Icons.Rounded.PlayArrow, red = false, size = 48.dp)
            KeyCap(Icons.Rounded.Stop, red = false, size = 48.dp)
            KeyCap(Icons.Rounded.Pause, red = false, size = 48.dp)
        }
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun PermissionCard(icon: ImageVector, @StringRes title: Int, @StringRes body: Int, granted: Boolean, onAllow: () -> Unit) {
    val palette = Retro.palette
    Row(
        modifier = Modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, if (granted) palette.play.copy(alpha = 0.6f) else palette.outline, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(palette.accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = palette.accent)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        if (granted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = palette.play, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.onb_perm_granted), style = MaterialTheme.typography.labelLarge, color = palette.play)
            }
        } else {
            FilledTonalButton(onClick = onAllow) { Text(stringResource(R.string.onb_perm_allow)) }
        }
    }
}

@Composable
private fun SpinningCassette() {
    val reels = rememberReelMotion(speed = 1f, progress = { 0.35f }, reduceMotion = Retro.reduceMotion)
    CassetteArt(
        skin = CassetteSkin.SMOKED_CLEAR,
        labelColor = Color(0xFFF3E6C8),
        title = stringResource(R.string.art_press_rec),
        sideLetter = "A",
        subtitle = stringResource(R.string.art_voice_diary),
        progress = { 0.35f },
        leftAngle = { reels.left },
        rightAngle = { reels.right },
        modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth(0.92f),
    )
}

@Composable
private fun CassetteStack() {
    Box(contentAlignment = Alignment.Center) {
        CassetteArt(CassetteSkin.HOT_PINK, Color(0xFFF7C948), stringResource(R.string.art_summer), "B", modifier = Modifier.width(220.dp).rotate(-12f).offset(x = (-34).dp, y = (-28).dp))
        CassetteArt(CassetteSkin.OCEAN_BLUE, Color(0xFFFFFFFF), stringResource(R.string.art_road_trip), "A", modifier = Modifier.width(220.dp).rotate(8f).offset(x = 32.dp, y = (-6).dp))
        CassetteArt(CassetteSkin.CHROME_METAL, Color(0xFFFF8A5B), stringResource(R.string.art_ideas), "A", modifier = Modifier.width(230.dp).offset(y = 34.dp))
    }
}

@Composable
private fun DiaryCassette() {
    Box(contentAlignment = Alignment.Center) {
        CassetteArt(
            skin = CassetteSkin.CREAM_BEIGE,
            labelColor = Color(0xFF9AD1D4),
            title = stringResource(R.string.art_dear_diary),
            sideLetter = "A",
            subtitle = stringResource(R.string.art_diary_sub),
            modifier = Modifier.width(280.dp).rotate(-4f),
        )
        Box(
            Modifier
                .size(64.dp)
                .offset(x = 112.dp, y = 58.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Retro.palette.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.AutoStories, contentDescription = null, tint = Retro.palette.onAccent, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun SealedCassette() {
    Box(contentAlignment = Alignment.Center) {
        CassetteArt(
            skin = CassetteSkin.GHOST_GLOW,
            labelColor = Color(0xFFB794F6),
            title = stringResource(R.string.art_dear_future),
            sideLetter = "A",
            sealedText = stringResource(R.string.art_sealed_until),
            modifier = Modifier.width(290.dp),
        )
        Box(
            Modifier
                .size(64.dp)
                .offset(x = 116.dp, y = 58.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Retro.palette.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.HourglassTop, contentDescription = null, tint = Retro.palette.onAccent, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun PrivacyArt() {
    val palette = Retro.palette
    Box(contentAlignment = Alignment.Center) {
        CassetteArt(CassetteSkin.CLASSIC_BLACK, Color(0xFF7AD3C2), stringResource(R.string.art_only_here), "A", modifier = Modifier.width(230.dp))
        Box(
            Modifier
                .size(64.dp)
                .offset(x = 96.dp, y = 50.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(palette.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(32.dp))
        }
    }
}
