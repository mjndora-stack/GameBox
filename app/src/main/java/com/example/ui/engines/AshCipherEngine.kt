package com.example.ui.engines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.TextAdventureSaveEntity

private data class StoryChoice(
  val label: String,
  val nextNodeId: String,
  val requiredItem: String? = null,
  val grantItem: String? = null,
  val grantClue: String? = null,
  val hpDelta: Int = 0,
  val focusDelta: Int = 0,
  val xpGain: Int = 20,
)

private data class StoryNode(
  val id: String,
  val chapter: String,
  val body: String,
  val choices: List<StoryChoice>,
  val isEnding: Boolean = false,
  val endingScore: Int = 0,
)

private val STORY_NODES: Map<String, StoryNode> =
  listOf(
      StoryNode(
        id = "start",
        chapter = "PROLOGUE · THE SEA WALL",
        body =
          "Rain needles the basalt stairs beneath Observatory Nine. Your brass lantern throws a trembling amber halo across the locked iron gate and a half-buried surveyor's satchel.",
        choices =
          listOf(
            StoryChoice(
              label = "Search the surveyor's satchel",
              nextNodeId = "satchel",
              grantItem = "Brass Key",
              grantClue = "Entry Log #19",
              xpGain = 25,
            ),
            StoryChoice(
              label = "Force the rusted side-grate",
              nextNodeId = "courtyard_hurt",
              hpDelta = -2,
              focusDelta = -1,
              xpGain = 20,
            ),
            StoryChoice(
              label = "Study the carved cipher stone by the sea wall",
              nextNodeId = "cipher_stone",
              grantClue = "Solar Dial Ratio",
              focusDelta = 1,
              xpGain = 30,
            ),
          ),
      ),
      StoryNode(
        id = "satchel",
        chapter = "ACT I · IRON THRESHOLD",
        body =
          "Inside the oilskin flap rests a heavy Brass Key and a damp ledger noting: 'Do not wake the pendulum before aligning the prism.' The iron gate awaits.",
        choices =
          listOf(
            StoryChoice(
              label = "Unlock the iron gate with Brass Key",
              nextNodeId = "atrium",
              requiredItem = "Brass Key",
              xpGain = 30,
            ),
            StoryChoice(
              label = "Check the cipher stone before entering",
              nextNodeId = "cipher_stone",
              grantClue = "Solar Dial Ratio",
              xpGain = 25,
            ),
          ),
      ),
      StoryNode(
        id = "cipher_stone",
        chapter = "ACT I · CARVED MONOLITH",
        body =
          "Holding the lantern close reveals etched astronomical ratios: 'III — VII — IX'. A hollow alcove behind the stone clicks open, revealing an Obsidian Lens.",
        choices =
          listOf(
            StoryChoice(
              label = "Take the Obsidian Lens and head to the gate",
              nextNodeId = "atrium",
              grantItem = "Obsidian Lens",
              xpGain = 35,
            ),
            StoryChoice(
              label = "Search the nearby satchel first",
              nextNodeId = "satchel",
              grantItem = "Brass Key",
              xpGain = 25,
            ),
          ),
      ),
      StoryNode(
        id = "courtyard_hurt",
        chapter = "ACT I · THE FLOODED GRATE",
        body =
          "The corroded iron gives way with a jagged snap, slicing your palm (-2 HP). You tumble into the damp atrium beneath a dome of cracked glass.",
        choices =
          listOf(
            StoryChoice(
              label = "Bind your hand and step into the Atrium",
              nextNodeId = "atrium",
              xpGain = 20,
            )
          ),
      ),
      StoryNode(
        id = "atrium",
        chapter = "ACT II · THE SILENT ATRIUM",
        body =
          "A towering brass pendulum hangs motionless over a sunken star-map. Two stairwells branch out: West toward the Archive Vault, East up the Spiral Belltower.",
        choices =
          listOf(
            StoryChoice(
              label = "Descend into the Archive Vault",
              nextNodeId = "archive",
              xpGain = 25,
            ),
            StoryChoice(
              label = "Climb the Spiral Belltower",
              nextNodeId = "belltower",
              xpGain = 25,
            ),
            StoryChoice(
              label = "Mount the Obsidian Lens into the star-map",
              nextNodeId = "secret_shaft",
              requiredItem = "Obsidian Lens",
              grantClue = "Hidden Mechanism",
              xpGain = 45,
            ),
          ),
      ),
      StoryNode(
        id = "archive",
        chapter = "ACT II · ARCHIVE OF ASH",
        body =
          "Shelves of charred vellum surround a sealed desk. An amber phial of Restorative Tonic glints beside a silver astrolabe shard.",
        choices =
          listOf(
            StoryChoice(
              label = "Drink the Restorative Tonic (+3 HP, +2 Focus)",
              nextNodeId = "belltower",
              hpDelta = 3,
              focusDelta = 2,
              grantItem = "Astrolabe Shard",
              xpGain = 35,
            ),
            StoryChoice(
              label = "Decrypt the ash-covered ledger",
              nextNodeId = "belltower",
              grantClue = "Pendulum Sequence",
              focusDelta = 1,
              xpGain = 40,
            ),
          ),
      ),
      StoryNode(
        id = "belltower",
        chapter = "ACT III · THE DOME CHAMBER",
        body =
          "At the apex, wind howls through the open telescope slit. A shadow-warden blocks the Zenith Console, demanding the true alignment.",
        choices =
          listOf(
            StoryChoice(
              label = "Speak the Solar Dial Ratio (III - VII - IX)",
              nextNodeId = "ending_light",
              requiredItem = "Obsidian Lens",
              xpGain = 90,
            ),
            StoryChoice(
              label = "Insert the Astrolabe Shard into the console",
              nextNodeId = "ending_cipher",
              requiredItem = "Astrolabe Shard",
              xpGain = 90,
            ),
            StoryChoice(
              label = "Attempt a blind manual override",
              nextNodeId = "ending_ash",
              hpDelta = -3,
              xpGain = 55,
            ),
          ),
      ),
      StoryNode(
        id = "secret_shaft",
        chapter = "ACT III · THE PRISM CORE",
        body =
          "Refracted lantern light unlocks a spiral lift beneath the star-map, carrying you straight into the pristine Prism Core untouched by ash.",
        choices =
          listOf(
            StoryChoice(
              label = "Activate the Beacon of Dawn",
              nextNodeId = "ending_light",
              xpGain = 100,
            )
          ),
      ),
      StoryNode(
        id = "ending_light",
        chapter = "ENDING I · DAWN OF THE OBSERVATORY",
        body =
          "The great dome parts cleanly. A beam of golden light pierces the storm, decoding every monolith along the coast. You emerge as Master Archivist.",
        choices = emptyList(),
        isEnding = true,
        endingScore = 950,
      ),
      StoryNode(
        id = "ending_cipher",
        chapter = "ENDING II · KEEPER OF THE ASTROLABE",
        body =
          "The Astrolabe Shard locks into place with a resonant chime. The shadow-warden bows and fades into mist, leaving the observatory's vault keys in your care.",
        choices = emptyList(),
        isEnding = true,
        endingScore = 820,
      ),
      StoryNode(
        id = "ending_ash",
        chapter = "ENDING III · SURVIVOR IN THE ASH",
        body =
          "Sparks erupt from the console as the dome seals shut. You escape down the sea-wall stairs alive, carrying fragments of a mystery still unsolved.",
        choices = emptyList(),
        isEnding = true,
        endingScore = 600,
      ),
    )
    .associateBy { it.id }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AshCipherEngine(
  profileId: Long,
  savedState: TextAdventureSaveEntity?,
  onSaveState: (TextAdventureSaveEntity) -> Unit,
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  var nodeId by remember(savedState?.profileId) { mutableStateOf(savedState?.currentNodeId ?: "start") }
  var hp by remember(savedState?.profileId) { mutableIntStateOf(savedState?.hp ?: 10) }
  var focus by remember(savedState?.profileId) { mutableIntStateOf(savedState?.focus ?: 5) }
  var inventory by remember(savedState?.profileId) {
    mutableStateOf(
      savedState?.inventoryCsv?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: setOf("Brass Lantern")
    )
  }
  var clues by remember(savedState?.profileId) {
    mutableStateOf(savedState?.cluesCsv?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet())
  }
  var endingsCount by remember(savedState?.profileId) {
    mutableIntStateOf(savedState?.endingsCount ?: 0)
  }

  val currentNode = STORY_NODES[nodeId] ?: STORY_NODES.getValue("start")

  LaunchedEffect(nodeId) {
    if (currentNode.isEnding) {
      onAwardXp(60, currentNode.endingScore, "lorekeeper")
    }
  }

  fun persist(nextId: String, nextHp: Int, nextFocus: Int, nextInv: Set<String>, nextClues: Set<String>, nextEndings: Int) {
    onSaveState(
      TextAdventureSaveEntity(
        profileId = profileId,
        currentNodeId = nextId,
        hp = nextHp,
        focus = nextFocus,
        inventoryCsv = nextInv.joinToString(","),
        cluesCsv = nextClues.joinToString(","),
        visitedNodesCsv = nextId,
        endingsCount = nextEndings,
      )
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Top Stat Strip
    Surface(
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surfaceVariant,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Rounded.Favorite, contentDescription = "Health", tint = MaterialTheme.colorScheme.primary)
          Text("HP $hp/10", style = MaterialTheme.typography.labelMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Rounded.Psychology, contentDescription = "Focus", tint = MaterialTheme.colorScheme.secondary)
          Text("FOCUS $focus", style = MaterialTheme.typography.labelMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Rounded.Key, contentDescription = "Relics", tint = MaterialTheme.colorScheme.tertiary)
          Text("${inventory.size} ITEMS", style = MaterialTheme.typography.labelMedium)
        }
      }
    }

    // Story Card
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      shape = MaterialTheme.shapes.large,
    ) {
      Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Rounded.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text(
            text = currentNode.chapter,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Text(
          text = currentNode.body,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }

    // Inventory & Clues
    if (inventory.isNotEmpty() || clues.isNotEmpty()) {
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        inventory.forEach { item ->
          AssistChip(onClick = {}, label = { Text(item, style = MaterialTheme.typography.labelSmall) })
        }
        clues.forEach { clue ->
          AssistChip(onClick = {}, label = { Text("Clue: $clue", style = MaterialTheme.typography.labelSmall) })
        }
      }
    }

    // Choices or Restart
    if (!currentNode.isEnding) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        currentNode.choices.forEachIndexed { idx, choice ->
          val hasReq = choice.requiredItem == null || inventory.contains(choice.requiredItem)
          Button(
            onClick = {
              val nextHp = (hp + choice.hpDelta).coerceIn(1, 10)
              val nextFocus = (focus + choice.focusDelta).coerceIn(0, 10)
              val nextInv = if (choice.grantItem != null) inventory + choice.grantItem else inventory
              val nextClues = if (choice.grantClue != null) clues + choice.grantClue else clues
              val nextNode = STORY_NODES[choice.nextNodeId]
              val nextEndings = if (nextNode?.isEnding == true) endingsCount + 1 else endingsCount

              hp = nextHp
              focus = nextFocus
              inventory = nextInv
              clues = nextClues
              endingsCount = nextEndings
              nodeId = choice.nextNodeId

              onAwardXp(choice.xpGain, null, null)
              persist(choice.nextNodeId, nextHp, nextFocus, nextInv, nextClues, nextEndings)
            },
            enabled = hasReq,
            modifier = Modifier.fillMaxWidth().testTag("story_choice_$idx"),
            shape = MaterialTheme.shapes.medium,
          ) {
            Text(
              text =
                if (hasReq) choice.label
                else "${choice.label} (Requires ${choice.requiredItem})",
              modifier = Modifier.padding(vertical = 6.dp),
            )
          }
        }
      }
    } else {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Chapter Complete · Score ${currentNode.endingScore}", style = MaterialTheme.typography.titleMedium)
          Text("Shared XP & Lorekeeper Badge synced to offline Vault.", style = MaterialTheme.typography.bodyMedium)
        }
      }
      OutlinedButton(
        onClick = {
          nodeId = "start"
          hp = 10
          focus = 5
          inventory = setOf("Brass Lantern")
          clues = emptySet()
          persist("start", 10, 5, inventory, emptySet(), endingsCount)
        },
        modifier = Modifier.fillMaxWidth().testTag("story_restart_button"),
      ) {
        Icon(Icons.Rounded.Refresh, contentDescription = null)
        Spacer(Modifier.padding(4.dp))
        Text("Begin New Expedition")
      }
    }
    Spacer(Modifier.height(24.dp))
  }
}
