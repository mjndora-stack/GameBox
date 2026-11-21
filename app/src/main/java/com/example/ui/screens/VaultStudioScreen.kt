package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.ControlConfig
import com.example.data.model.GameEngineType
import com.example.data.model.PlayerTier
import com.example.data.model.VaultThemePreset
import com.example.ui.viewmodel.VaultUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VaultStudioScreen(
  uiState: VaultUiState,
  onToggleDarkMode: () -> Unit,
  onSelectTheme: (VaultThemePreset) -> Unit,
  onUpdateControls: ((ControlConfig) -> ControlConfig) -> Unit,
  onUnlockCreator: (String) -> Boolean,
  onLockCreator: () -> Unit,
  onInspectPackage: (Uri) -> Unit,
  onClearInspected: () -> Unit,
  onAddCreatorGame: (String, String, String, GameEngineType, PlayerTier, String, String, String, String, String?) -> Unit,
  onRemoveCreatorGame: (String) -> Unit,
  onBuildBackupJson: suspend () -> String,
  onImportBackupJson: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var pinInput by remember { mutableStateOf("") }
  var titleInput by remember { mutableStateOf("") }
  var subtitleInput by remember { mutableStateOf("") }
  var versionInput by remember { mutableStateOf("v1.0.0") }
  var releaseNoteInput by remember { mutableStateOf("GitHub / Itch Vault Drop") }
  var descInput by remember { mutableStateOf("") }
  var selectedEngine by remember { mutableStateOf(GameEngineType.WEB_CANVAS) }
  var selectedTierGate by remember { mutableStateOf(PlayerTier.HOBBYIST) }

  // Pre-fill form if Creator inspected a local package
  val inspected = uiState.inspectedPackage
  val effectiveTitle = if (inspected != null && titleInput.isEmpty()) inspected.title else titleInput
  val effectiveSubtitle = if (inspected != null && subtitleInput.isEmpty()) inspected.subtitle else subtitleInput
  val effectiveVersion = if (inspected != null && versionInput == "v1.0.0") inspected.version else versionInput

  val exportLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
      if (uri != null) {
        scope.launch {
          val json = onBuildBackupJson()
          context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
          }
        }
      }
    }

  val importBackupLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (!text.isNullOrBlank()) onImportBackupJson(text)
      }
    }

  val pickPackageLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) onInspectPackage(uri)
    }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // 1. Appearance & Dark/Light Mode
    item {
      Text("APPEARANCE & THEME", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(8.dp))
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Icon(
                imageVector = if (uiState.preferences.isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Column {
                Text("Dark Mode", style = MaterialTheme.typography.titleMedium)
                Text("Minimalist low-glare traveler surface", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
            Switch(
              checked = uiState.preferences.isDarkMode,
              onCheckedChange = { onToggleDarkMode() },
              modifier = Modifier.testTag("settings_dark_switch"),
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text("Accent Palette", style = MaterialTheme.typography.titleMedium)
          }

          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VaultThemePreset.entries.forEach { preset ->
              val unlocked = uiState.currentTier.levelIndex >= preset.requiredTier.levelIndex
              val selected = uiState.preferences.themePreset == preset
              FilterChip(
                selected = selected,
                onClick = { onSelectTheme(preset) },
                leadingIcon = {
                  if (!unlocked) Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                },
                label = {
                  Text(if (unlocked) preset.displayName else "${preset.displayName} (${preset.requiredTier.displayName})")
                },
                modifier = Modifier.testTag("theme_chip_${preset.id}"),
              )
            }
          }
        }
      }
    }

    // 2. Device Controls Config
    item {
      Text("EMBEDDED ENGINE CONTROLS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(8.dp))
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          ControlSwitchRow(
            title = "Left-Handed HUD Layout",
            subtitle = "Swap D-pad / joystick and action triggers",
            checked = uiState.preferences.controlConfig.leftHandedHud,
            onToggle = { onUpdateControls { it.copy(leftHandedHud = !it.leftHandedHud) } },
            tag = "switch_left_handed",
          )
          ControlSwitchRow(
            title = "Compact Controls",
            subtitle = "Optimized button scale for tablets & foldables",
            checked = uiState.preferences.controlConfig.compactControls,
            onToggle = { onUpdateControls { it.copy(compactControls = !it.compactControls) } },
            tag = "switch_compact_controls",
          )
          ControlSwitchRow(
            title = "Tactile Haptics",
            subtitle = "Vibrate feedback on actions & tier promotions",
            checked = uiState.preferences.controlConfig.hapticsEnabled,
            onToggle = { onUpdateControls { it.copy(hapticsEnabled = !it.hapticsEnabled) } },
            tag = "switch_haptics",
          )
        }
      }
    }

    // 3. Offline Progress Backup / Sync
    item {
      Text("OFFLINE SAVE SYNC (ZERO NETWORK)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(8.dp))
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            "Export or restore your XP, unlocked badges, and high scores as a local JSON snapshot when moving between devices or updating APKs from GitHub / Itch.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
              onClick = { exportLauncher.launch("vaultplay_save_${System.currentTimeMillis()}.json") },
              modifier = Modifier.weight(1f).testTag("export_backup_btn"),
            ) {
              Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.size(6.dp))
              Text("Export Save")
            }
            OutlinedButton(
              onClick = { importBackupLauncher.launch(arrayOf("application/json", "text/plain")) },
              modifier = Modifier.weight(1f).testTag("import_backup_btn"),
            ) {
              Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.size(6.dp))
              Text("Import Save")
            }
          }
        }
      }
    }

    // 4. Creator Studio (Creator-Only Game Drop & Package Ingestion)
    item {
      Text("CREATOR STUDIO (CURATED DROPS)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(8.dp))
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          if (!uiState.creatorUnlocked) {
            Text(
              "Restricted to the vault creator for managing bi-monthly GitHub / Itch game drops and inspecting local HTML5 / APK packages.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
              value = pinInput,
              onValueChange = { pinInput = it },
              singleLine = true,
              visualTransformation = PasswordVisualTransformation(),
              label = { Text("Creator PIN (default: 2026)") },
              leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
              modifier = Modifier.fillMaxWidth().testTag("creator_pin_input"),
            )
            Button(
              onClick = {
                if (onUnlockCreator(pinInput)) pinInput = ""
              },
              modifier = Modifier.fillMaxWidth().testTag("unlock_creator_btn"),
            ) {
              Icon(Icons.Rounded.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.size(6.dp))
              Text("Unlock Creator Studio")
            }
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Creator Access Active", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
              OutlinedButton(onClick = onLockCreator) { Text("Lock") }
            }

            OutlinedButton(
              onClick = { pickPackageLauncher.launch(arrayOf("*/*")) },
              modifier = Modifier.fillMaxWidth().testTag("inspect_package_btn"),
            ) {
              Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.size(8.dp))
              Text("Load .HTML Cartridge / Inspect .APK or .JSON")
            }

            if (inspected != null) {
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text("Package Inspected: ${inspected.title} (${inspected.sizeLabel})", style = MaterialTheme.typography.labelLarge)
                  Text(inspected.notes, style = MaterialTheme.typography.bodyMedium)
                  OutlinedButton(onClick = onClearInspected) { Text("Clear Package") }
                }
              }
            }

            OutlinedTextField(
              value = effectiveTitle,
              onValueChange = { titleInput = it },
              singleLine = true,
              label = { Text("Game Title") },
              modifier = Modifier.fillMaxWidth().testTag("creator_title_input"),
            )
            OutlinedTextField(
              value = effectiveSubtitle,
              onValueChange = { subtitleInput = it },
              singleLine = true,
              label = { Text("Subtitle") },
              modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = effectiveVersion,
                onValueChange = { versionInput = it },
                singleLine = true,
                label = { Text("Release Tag") },
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = releaseNoteInput,
                onValueChange = { releaseNoteInput = it },
                singleLine = true,
                label = { Text("Release Note") },
                modifier = Modifier.weight(1.4f),
              )
            }
            OutlinedTextField(
              value = descInput,
              onValueChange = { descInput = it },
              label = { Text("Briefing Description") },
              modifier = Modifier.fillMaxWidth(),
            )

            Text("Embedded Engine Target", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              GameEngineType.entries.forEach { eng ->
                FilterChip(
                  selected = (inspected?.engineType ?: selectedEngine) == eng,
                  onClick = { selectedEngine = eng },
                  label = { Text(eng.label) },
                )
              }
            }

            Text("Minimum Player Tier Gate", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              PlayerTier.entries.forEach { tier ->
                FilterChip(
                  selected = selectedTierGate == tier,
                  onClick = { selectedTierGate = tier },
                  label = { Text(tier.displayName) },
                )
              }
            }

            Button(
              onClick = {
                val chosenEngine = inspected?.engineType ?: selectedEngine
                onAddCreatorGame(
                  effectiveTitle.ifBlank { "Vault Drop" },
                  effectiveSubtitle.ifBlank { "Curated Bi-Monthly Release" },
                  chosenEngine.label,
                  chosenEngine,
                  selectedTierGate,
                  effectiveVersion,
                  inspected?.sizeLabel ?: "Embedded",
                  descInput.ifBlank { "New curated release added via Creator Studio." },
                  releaseNoteInput,
                  inspected?.htmlContent,
                )
                titleInput = ""
                subtitleInput = ""
                descInput = ""
              },
              modifier = Modifier.fillMaxWidth().testTag("creator_publish_btn"),
            ) {
              Icon(Icons.Rounded.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.size(6.dp))
              Text("Publish to Local Library")
            }
          }
        }
      }
    }

    val creatorDrops = uiState.games.filter { it.isCreatorAdded }
    if (uiState.creatorUnlocked && creatorDrops.isNotEmpty()) {
      item {
        Text("CREATOR DROPS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
      }
      items(creatorDrops, key = { it.id }) { drop ->
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("${drop.title} (${drop.versionTag})", style = MaterialTheme.typography.titleMedium)
              Text(drop.releaseNote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onRemoveCreatorGame(drop.id) }) {
              Icon(Icons.Rounded.Delete, contentDescription = "Remove drop", tint = MaterialTheme.colorScheme.error)
            }
          }
        }
      }
    }

    item { Spacer(Modifier.height(16.dp)) }
  }
}

@Composable
private fun ControlSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onToggle: () -> Unit,
  tag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(checked = checked, onCheckedChange = { onToggle() }, modifier = Modifier.testTag(tag))
  }
}
