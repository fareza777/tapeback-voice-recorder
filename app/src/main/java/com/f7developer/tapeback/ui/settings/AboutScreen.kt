package com.f7developer.tapeback.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.f7developer.tapeback.BuildConfig
import com.f7developer.tapeback.R
import com.f7developer.tapeback.core.AppActions
import com.f7developer.tapeback.ui.components.AppIconArt
import com.f7developer.tapeback.ui.components.SectionLabel
import com.f7developer.tapeback.ui.theme.BungeeFamily
import com.f7developer.tapeback.ui.theme.Retro
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(onBack: () -> Unit, snackbar: SnackbarHostState, contentPadding: PaddingValues) {
    val palette = Retro.palette
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    var showPolicy by rememberSaveable { mutableStateOf(false) }
    val noStore = stringResource(R.string.msg_no_store)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = contentPadding.calculateBottomPadding() + 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back)) }
                Text(stringResource(R.string.about_title), style = MaterialTheme.typography.headlineMedium)
            }
        }

        item {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AppIconArt(size = 112.dp)
                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(R.string.brand_wordmark),
                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = BungeeFamily),
                    color = palette.accent,
                )
                Text(stringResource(R.string.tagline), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.about_version_build, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.surfaceRaised)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                )
                Spacer(Modifier.height(20.dp))
                Row(Modifier.widthIn(max = 420.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { if (!AppActions.rate(context)) scope.launch { snackbar.showSnackbar(noStore) } },
                        modifier = Modifier.weight(1f),
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    ) {
                        Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_rate), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { AppActions.share(context) },
                        modifier = Modifier.weight(1f),
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_share), maxLines = 1)
                    }
                }
            }
        }

        item { SectionLabel(stringResource(R.string.about_story_title)) }
        item {
            Group {
                Text(
                    stringResource(R.string.about_story_body),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        item { SectionLabel(stringResource(R.string.about_inside)) }
        item {
            Group {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Feature(Icons.Rounded.FiberManualRecord, R.string.about_feature_record)
                    Feature(Icons.Rounded.Radio, R.string.about_feature_deck)
                    Feature(Icons.Rounded.LibraryMusic, R.string.about_feature_library)
                    Feature(Icons.Rounded.AutoStories, R.string.about_feature_diary)
                    Feature(Icons.Rounded.HourglassTop, R.string.about_feature_capsule)
                    Feature(Icons.Rounded.Lock, R.string.about_feature_privacy)
                }
            }
        }

        item { SectionLabel(stringResource(R.string.about_credits)) }
        item {
            Group {
                NavRow(Icons.Rounded.Description, stringResource(R.string.about_licenses), stringResource(R.string.about_licenses_body), onClick = { showLicenses = true })
                NavRow(Icons.Rounded.Lock, stringResource(R.string.settings_privacy_policy), stringResource(R.string.settings_privacy_policy_body), onClick = { showPolicy = true })
            }
        }

        item {
            Column(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.about_made_by), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                Text(
                    stringResource(R.string.about_kiroween),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (showLicenses) {
        TextDialog(stringResource(R.string.about_licenses), stringResource(R.string.about_licenses_text), onDismiss = { showLicenses = false })
    }
    if (showPolicy) {
        TextDialog(stringResource(R.string.settings_privacy_policy), stringResource(R.string.privacy_policy_body), onDismiss = { showPolicy = false })
    }
}

@Composable
private fun Feature(icon: ImageVector, text: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LeadingIcon(icon)
        Spacer(Modifier.width(14.dp))
        Text(stringResource(text), style = MaterialTheme.typography.bodyMedium)
    }
}
