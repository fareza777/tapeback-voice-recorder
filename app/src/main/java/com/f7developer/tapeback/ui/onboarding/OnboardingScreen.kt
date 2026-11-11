package com.f7developer.tapeback.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.f7developer.tapeback.R
import com.f7developer.tapeback.domain.model.CassetteSkin
import com.f7developer.tapeback.ui.components.CassetteArt
import com.f7developer.tapeback.ui.components.rememberReelMotion
import com.f7developer.tapeback.ui.theme.Retro
import kotlinx.coroutines.launch

private data class Page(val title: Int, val body: Int)

private val pages = listOf(
    Page(R.string.onb1_title, R.string.onb1_body),
    Page(R.string.onb2_title, R.string.onb2_body),
    Page(R.string.onb3_title, R.string.onb3_body),
    Page(R.string.onb4_title, R.string.onb4_body),
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val palette = Retro.palette
    val permissions = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onDone() }
    val last = pager.currentPage == pages.lastIndex
    val pageDescription = stringResource(R.string.cd_page_of, pager.currentPage + 1, pages.size)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.brand_wordmark), style = MaterialTheme.typography.titleLarge.copy(fontFamily = com.f7developer.tapeback.ui.theme.BungeeFamily), color = palette.accent, modifier = Modifier.weight(1f))
            if (!last) TextButton(onClick = onDone) { Text(stringResource(R.string.onb_skip)) }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                    when (index) {
                        0 -> SpinningCassette()
                        1 -> CassetteStack()
                        2 -> SealedCassette()
                        else -> PrivacyArt()
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    stringResource(pages[index].title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 480.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(pages[index].body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 480.dp),
                )
                if (index == pages.lastIndex) {
                    Spacer(Modifier.height(20.dp))
                    PermissionLine(Icons.Rounded.Mic, stringResource(R.string.onb_perm_mic))
                    PermissionLine(Icons.Rounded.Notifications, stringResource(R.string.onb_perm_notif))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).semantics { contentDescription = pageDescription },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                pages.indices.forEach { i ->
                    val selected = i == pager.currentPage
                    val w by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dot")
                    val c by animateColorAsState(if (selected) palette.accent else palette.outline, label = "dotColor")
                    Box(Modifier.height(8.dp).width(w).clip(CircleShape).background(c))
                }
            }
            Button(onClick = {
                if (last) launcher.launch(permissions) else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            }) {
                Text(stringResource(if (last) R.string.onb_start else R.string.onb_next))
            }
        }
    }
}

@Composable
private fun PermissionLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Retro.palette.accent)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
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
        CassetteArt(CassetteSkin.HOT_PINK, Color(0xFFF7C948), stringResource(R.string.art_summer), "B", modifier = Modifier.width(240.dp).rotate(-12f).offset(x = (-36).dp, y = (-30).dp))
        CassetteArt(CassetteSkin.OCEAN_BLUE, Color(0xFFFFFFFF), stringResource(R.string.art_road_trip), "A", modifier = Modifier.width(240.dp).rotate(8f).offset(x = 34.dp, y = (-8).dp))
        CassetteArt(CassetteSkin.CHROME_METAL, Color(0xFFFF8A5B), stringResource(R.string.art_ideas), "A", modifier = Modifier.width(250.dp).offset(y = 36.dp))
    }
}

@Composable
private fun SealedCassette() {
    CassetteArt(
        skin = CassetteSkin.GHOST_GLOW,
        labelColor = Color(0xFFB794F6),
        title = stringResource(R.string.art_dear_future),
        sideLetter = "A",
        sealedText = stringResource(R.string.art_sealed_until),
        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(0.88f),
    )
}

@Composable
private fun PrivacyArt() {
    val palette = Retro.palette
    Box(contentAlignment = Alignment.Center) {
        CassetteArt(CassetteSkin.CLASSIC_BLACK, Color(0xFF7AD3C2), stringResource(R.string.art_only_here), "A", modifier = Modifier.width(300.dp))
        Box(
            Modifier
                .size(84.dp)
                .offset(x = 110.dp, y = 70.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(palette.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(42.dp))
        }
    }
}
