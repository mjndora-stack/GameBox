package com.example.ui.engines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.SportsMma
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.audio.GameSfx
import com.example.audio.VaultAudioEngine
import com.example.data.model.FighterStat
import com.example.data.model.FighterUpgradeLevels
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class FighterArchetype(
  val id: String,
  val name: String,
  val title: String,
  val primaryColor: Color,
  val accentColor: Color,
  val baseHp: Int,
  val baseDamage: Int,
  val baseSpeed: Float,
  val specialName: String,
)

val STREET_FIGHTERS =
  listOf(
    FighterArchetype(
      id = "kai",
      name = "KAI",
      title = "Flame Striker",
      primaryColor = Color(0xFFFF5C7C),
      accentColor = Color(0xFFFFC857),
      baseHp = 110,
      baseDamage = 13,
      baseSpeed = 300f,
      specialName = "Hadou Flare",
    ),
    FighterArchetype(
      id = "mira",
      name = "MIRA",
      title = "Volt Kicker",
      primaryColor = Color(0xFF38BDF8),
      accentColor = Color(0xFF4CE8C8),
      baseHp = 98,
      baseDamage = 12,
      baseSpeed = 360f,
      specialName = "Sonic Arc",
    ),
    FighterArchetype(
      id = "titan",
      name = "TITAN",
      title = "Iron Grappler",
      primaryColor = Color(0xFFF59E0B),
      accentColor = Color(0xFFEF4444),
      baseHp = 135,
      baseDamage = 16,
      baseSpeed = 245f,
      specialName = "Shockwave",
    ),
  )

private enum class MoveAction { IDLE, PUNCH, KICK, SPECIAL, HURT }

private data class ArenaFighterState(
  val archetype: FighterArchetype,
  val x: Float,
  val y: Float, // 0 = ground, negative = airborne
  val vx: Float = 0f,
  val vy: Float = 0f,
  val walkPhase: Float = 0f,
  val facing: Int = 1, // 1 right, -1 left
  val hp: Float,
  val maxHp: Float,
  val meter: Float = 25f, // 0..100
  val action: MoveAction = MoveAction.IDLE,
  val actionTimer: Float = 0f,
  val actionTotal: Float = 0.25f,
  val hitAppliedThisSwing: Boolean = false,
  val blocking: Boolean = false,
  val speedMul: Float = 1f,
  val atkMul: Float = 1f,
)

private data class ArenaProjectile(
  val ownerIsPlayer: Boolean,
  val x: Float,
  val y: Float,
  val vx: Float,
  val damage: Float,
  val color: Color,
  val age: Float = 0f,
)

private data class HitSpark(val x: Float, val y: Float, val color: Color, val life: Float)

@Composable
fun StreetFighter2DGame(
  vaultCredits: Int,
  fighterUpgrades: Map<String, FighterUpgradeLevels>,
  leftHandedHud: Boolean,
  onUpgradeStat: (String, FighterStat, Int) -> Unit,
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  var friendMode by remember { mutableStateOf(false) }
  var selectedFighterId by remember { mutableStateOf("kai") }
  var p2FighterId by remember { mutableStateOf("mira") }
  var inArena by remember { mutableStateOf(false) }

  if (!inArena) {
    FighterSelectAndUpgradeScreen(
      friendMode = friendMode,
      selectedFighterId = selectedFighterId,
      p2FighterId = p2FighterId,
      vaultCredits = vaultCredits,
      fighterUpgrades = fighterUpgrades,
      onToggleFriendMode = { friendMode = it },
      onSelectFighter = { selectedFighterId = it },
      onSelectP2Fighter = { p2FighterId = it },
      onUpgradeStat = onUpgradeStat,
      onStartFight = { inArena = true },
      modifier = modifier,
    )
  } else {
    val playerArch = STREET_FIGHTERS.first { it.id == selectedFighterId }
    val enemyArch =
      if (friendMode) STREET_FIGHTERS.first { it.id == p2FighterId }
      else remember(selectedFighterId) { STREET_FIGHTERS.first { it.id != selectedFighterId } }
    val upgrades = fighterUpgrades[selectedFighterId] ?: FighterUpgradeLevels()
    val p2Upgrades = fighterUpgrades[p2FighterId] ?: FighterUpgradeLevels()

    StreetFighterArena(
      friendMode = friendMode,
      playerArch = playerArch,
      enemyArch = enemyArch,
      upgrades = upgrades,
      p2Upgrades = p2Upgrades,
      leftHandedHud = leftHandedHud,
      onAwardXp = onAwardXp,
      onExitToArmory = { inArena = false },
      modifier = modifier,
    )
  }
}

@Composable
private fun FighterSelectAndUpgradeScreen(
  friendMode: Boolean,
  selectedFighterId: String,
  p2FighterId: String,
  vaultCredits: Int,
  fighterUpgrades: Map<String, FighterUpgradeLevels>,
  onToggleFriendMode: (Boolean) -> Unit,
  onSelectFighter: (String) -> Unit,
  onSelectP2Fighter: (String) -> Unit,
  onUpgradeStat: (String, FighterStat, Int) -> Unit,
  onStartFight: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val selectedArch = STREET_FIGHTERS.first { it.id == selectedFighterId }
  val levels = fighterUpgrades[selectedFighterId] ?: FighterUpgradeLevels()

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("STREET FIGHTER 2D", style = MaterialTheme.typography.titleLarge)
        Text("Select & upgrade your fighter with Vault Credits", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Text(
          text = "$vaultCredits CR",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
      }
    }

    // Mode Selector: VS AI CPU or Play with Friend (2P)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp)) {
      FilterChip(
        selected = !friendMode,
        onClick = { onToggleFriendMode(false) },
        leadingIcon = { Icon(Icons.Rounded.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp)) },
        label = { Text("VS AI CPU") },
        modifier = Modifier.weight(1f).testTag("sf_mode_ai"),
      )
      FilterChip(
        selected = friendMode,
        onClick = { onToggleFriendMode(true) },
        leadingIcon = { Icon(Icons.Rounded.Group, contentDescription = null, modifier = Modifier.size(16.dp)) },
        label = { Text("PLAY WITH FRIEND (2P)") },
        modifier = Modifier.weight(1f).testTag("sf_mode_friend"),
      )
    }

    if (friendMode) {
      Text("PLAYER 1 FIGHTER (BOTTOM CONTROLS)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp)) {
      STREET_FIGHTERS.forEach { arch ->
        FilterChip(
          selected = selectedFighterId == arch.id,
          onClick = { onSelectFighter(arch.id) },
          label = { Text("${arch.name} · ${arch.title}") },
          modifier = Modifier.weight(1f).testTag("sf_pick_${arch.id}"),
        )
      }
    }

    if (friendMode) {
      Text("PLAYER 2 FIGHTER (TOP CONTROLS)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp)) {
        STREET_FIGHTERS.forEach { arch ->
          FilterChip(
            selected = p2FighterId == arch.id,
            onClick = { onSelectP2Fighter(arch.id) },
            label = { Text("${arch.name} · ${arch.title}") },
            modifier = Modifier.weight(1f).testTag("sf_p2_pick_${arch.id}"),
          )
        }
      }
    }

    // Fighter Stat, Animated Character Preview & Upgrade Card
    Surface(
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.5.dp, selectedArch.primaryColor.copy(alpha = 0.65f)),
      modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("${selectedArch.name} — ${selectedArch.title}", style = MaterialTheme.typography.titleMedium, color = selectedArch.primaryColor)
            Text("Special Move: ${selectedArch.specialName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF0E131D)) {
            Canvas(modifier = Modifier.size(68.dp)) {
              drawArticulatedFighter(
                f =
                  ArenaFighterState(
                    archetype = selectedArch,
                    x = 0f,
                    y = 0f,
                    facing = 1,
                    hp = 100f,
                    maxHp = 100f,
                  ),
                cx = size.width * 0.48f,
                groundY = size.height * 0.90f,
                bodyH = size.height * 0.72f,
                animTime = 0.4f,
                isP2MirrorColor = false,
              )
            }
          }
        }

        UpgradeStatRow(
          label = "Attack Power",
          detail = "${(selectedArch.baseDamage * (1f + (levels.attackLevel - 1) * 0.18f)).toInt()} DMG",
          level = levels.attackLevel,
          cost = levels.upgradeCost(FighterStat.ATTACK),
          credits = vaultCredits,
          onUpgrade = { onUpgradeStat(selectedFighterId, FighterStat.ATTACK, levels.upgradeCost(FighterStat.ATTACK)) },
          tag = "sf_upg_atk",
        )
        UpgradeStatRow(
          label = "Vitality (Max HP)",
          detail = "${selectedArch.baseHp + (levels.healthLevel - 1) * 18} HP",
          level = levels.healthLevel,
          cost = levels.upgradeCost(FighterStat.HEALTH),
          credits = vaultCredits,
          onUpgrade = { onUpgradeStat(selectedFighterId, FighterStat.HEALTH, levels.upgradeCost(FighterStat.HEALTH)) },
          tag = "sf_upg_hp",
        )
        UpgradeStatRow(
          label = "Agility & Meter",
          detail = "${(selectedArch.baseSpeed * (1f + (levels.speedLevel - 1) * 0.10f)).toInt()} SPD",
          level = levels.speedLevel,
          cost = levels.upgradeCost(FighterStat.SPEED),
          credits = vaultCredits,
          onUpgrade = { onUpgradeStat(selectedFighterId, FighterStat.SPEED, levels.upgradeCost(FighterStat.SPEED)) },
          tag = "sf_upg_spd",
        )
      }
    }

    Button(
      onClick = onStartFight,
      modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp).height(52.dp).testTag("sf_start_fight"),
    ) {
      Icon(Icons.Rounded.SportsMma, contentDescription = null)
      Spacer(Modifier.size(8.dp))
      Text(if (friendMode) "START 2P FRIEND DUEL · BEST OF 3" else "ENTER ARENA · BEST OF 3")
    }
  }
}

@Composable
private fun UpgradeStatRow(
  label: String,
  detail: String,
  level: Int,
  cost: Int,
  credits: Int,
  onUpgrade: () -> Unit,
  tag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text("$label · Lv $level/5", style = MaterialTheme.typography.labelLarge)
      Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(Modifier.height(4.dp))
      LinearProgressIndicator(
        progress = { level / 5f },
        modifier = Modifier.fillMaxWidth(0.85f).height(5.dp),
      )
    }
    if (level >= 5) {
      Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Text("MAX", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
      }
    } else {
      OutlinedButton(
        onClick = onUpgrade,
        enabled = credits >= cost,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.testTag(tag),
      ) {
        Icon(Icons.Rounded.Upgrade, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(4.dp))
        Text("$cost CR")
      }
    }
  }
}

@Composable
private fun StreetFighterArena(
  friendMode: Boolean,
  playerArch: FighterArchetype,
  enemyArch: FighterArchetype,
  upgrades: FighterUpgradeLevels,
  p2Upgrades: FighterUpgradeLevels,
  leftHandedHud: Boolean,
  onAwardXp: (Int, Int?, String?) -> Unit,
  onExitToArmory: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val playerMaxHp = (playerArch.baseHp + (upgrades.healthLevel - 1) * 18).toFloat()
  val playerAtkMul = 1f + (upgrades.attackLevel - 1) * 0.18f
  val playerSpdMul = 1f + (upgrades.speedLevel - 1) * 0.10f

  val p2MaxHp = (enemyArch.baseHp + (p2Upgrades.healthLevel - 1) * 18).toFloat()
  val p2AtkMul = 1f + (p2Upgrades.attackLevel - 1) * 0.18f
  val p2SpdMul = 1f + (p2Upgrades.speedLevel - 1) * 0.10f

  var playerWins by remember { mutableIntStateOf(0) }
  var enemyWins by remember { mutableIntStateOf(0) }
  var roundNumber by remember { mutableIntStateOf(1) }
  var matchBanner by remember { mutableStateOf("ROUND 1 · FIGHT!") }
  var matchOver by remember { mutableStateOf(false) }
  var animTime by remember { mutableFloatStateOf(0f) }

  fun initialPlayer() =
    ArenaFighterState(
      archetype = playerArch,
      x = 220f,
      y = 0f,
      facing = 1,
      hp = playerMaxHp,
      maxHp = playerMaxHp,
      speedMul = playerSpdMul,
      atkMul = playerAtkMul,
    )

  fun initialEnemy(round: Int): ArenaFighterState {
    return if (friendMode) {
      ArenaFighterState(
        archetype = enemyArch,
        x = 780f,
        y = 0f,
        facing = -1,
        hp = p2MaxHp,
        maxHp = p2MaxHp,
        speedMul = p2SpdMul,
        atkMul = p2AtkMul,
      )
    } else {
      val hp = (enemyArch.baseHp + (round - 1) * 14).toFloat()
      ArenaFighterState(
        archetype = enemyArch,
        x = 780f,
        y = 0f,
        facing = -1,
        hp = hp,
        maxHp = hp,
        atkMul = 0.9f + (round - 1) * 0.12f,
      )
    }
  }

  var pState by remember { mutableStateOf(initialPlayer()) }
  var eState by remember { mutableStateOf(initialEnemy(1)) }
  val projectiles = remember { mutableStateListOf<ArenaProjectile>() }
  val sparks = remember { mutableStateListOf<HitSpark>() }

  var holdLeft by remember { mutableStateOf(false) }
  var holdRight by remember { mutableStateOf(false) }
  var holdBlock by remember { mutableStateOf(false) }

  var p2HoldLeft by remember { mutableStateOf(false) }
  var p2HoldRight by remember { mutableStateOf(false) }
  var p2HoldBlock by remember { mutableStateOf(false) }

  var aiDecisionTimer by remember { mutableFloatStateOf(0.25f) }
  var aiDesiredDir by remember { mutableIntStateOf(-1) }
  var roundPauseTimer by remember { mutableFloatStateOf(0f) }

  fun resetMatch() {
    playerWins = 0
    enemyWins = 0
    roundNumber = 1
    pState = initialPlayer()
    eState = initialEnemy(1)
    projectiles.clear()
    sparks.clear()
    matchOver = false
    roundPauseTimer = 0f
    matchBanner = "ROUND 1 · FIGHT!"
  }

  fun triggerFighterAction(isPlayerOne: Boolean, move: MoveAction) {
    if (matchOver || roundPauseTimer > 0f) return
    val current = if (isPlayerOne) pState else eState
    if (current.action != MoveAction.IDLE) return
    if (move == MoveAction.SPECIAL && current.meter < 50f) return
    val dur =
      when (move) {
        MoveAction.PUNCH -> 0.24f
        MoveAction.KICK -> 0.32f
        MoveAction.SPECIAL -> 0.38f
        else -> 0.25f
      }
    val updated =
      if (move == MoveAction.SPECIAL) {
        VaultAudioEngine.playGameSfx(GameSfx.DASH)
        projectiles.add(
          ArenaProjectile(
            ownerIsPlayer = isPlayerOne,
            x = current.x + current.facing * 48f,
            y = -58f + current.y,
            vx = current.facing * 520f,
            damage = current.archetype.baseDamage * 1.9f * current.atkMul,
            color = current.archetype.accentColor,
          )
        )
        current.copy(action = move, actionTimer = dur, actionTotal = dur, meter = (current.meter - 50f).coerceAtLeast(0f), hitAppliedThisSwing = true)
      } else {
        current.copy(action = move, actionTimer = dur, actionTotal = dur, hitAppliedThisSwing = false)
      }
    if (isPlayerOne) pState = updated else eState = updated
  }

  // 60fps Game Loop
  LaunchedEffect(friendMode) {
    var lastNanos = 0L
    while (true) {
      withFrameNanos { now ->
        if (lastNanos == 0L) {
          lastNanos = now
          return@withFrameNanos
        }
        val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.04f)
        lastNanos = now
        animTime += dt

        for (i in sparks.lastIndex downTo 0) {
          val s = sparks[i]
          if (s.life - dt <= 0f) sparks.removeAt(i) else sparks[i] = s.copy(life = s.life - dt)
        }

        if (matchOver) return@withFrameNanos

        if (roundPauseTimer > 0f) {
          roundPauseTimer -= dt
          if (roundPauseTimer <= 0f) {
            roundNumber++
            pState = initialPlayer()
            eState = initialEnemy(roundNumber)
            projectiles.clear()
            matchBanner = "ROUND $roundNumber · FIGHT!"
          }
          return@withFrameNanos
        }

        var p = pState
        var e = eState

        p = p.copy(facing = if (p.x <= e.x) 1 else -1, blocking = holdBlock && p.action == MoveAction.IDLE)
        e =
          if (friendMode) {
            e.copy(facing = if (e.x >= p.x) -1 else 1, blocking = p2HoldBlock && e.action == MoveAction.IDLE)
          } else {
            e.copy(facing = if (e.x >= p.x) -1 else 1)
          }

        val pVx =
          if (p.action == MoveAction.IDLE && !p.blocking) {
            when {
              holdLeft && !holdRight -> -p.archetype.baseSpeed * p.speedMul
              holdRight && !holdLeft -> p.archetype.baseSpeed * p.speedMul
              else -> 0f
            }
          } else {
            0f
          }
        var py = p.y + p.vy * dt
        var pVy = p.vy + 1350f * dt
        if (py >= 0f) { py = 0f; pVy = 0f }

        var pAct = p.action
        var pTimer = p.actionTimer - dt
        if (pAct != MoveAction.IDLE && pTimer <= 0f) {
          pAct = MoveAction.IDLE
          pTimer = 0f
        }
        p = p.copy(
          x = (p.x + pVx * dt).coerceIn(60f, 940f),
          y = py,
          vx = pVx,
          vy = pVy,
          walkPhase = if (abs(pVx) > 5f && py >= 0f) p.walkPhase + dt * 11f else p.walkPhase,
          action = pAct,
          actionTimer = pTimer,
          meter = (p.meter + dt * 6f * p.speedMul).coerceAtMost(100f),
        )

        val dist = abs(p.x - e.x)
        val eVx: Float
        if (friendMode) {
          eVx =
            if (e.action == MoveAction.IDLE && !e.blocking) {
              when {
                p2HoldLeft && !p2HoldRight -> -e.archetype.baseSpeed * e.speedMul
                p2HoldRight && !p2HoldLeft -> e.archetype.baseSpeed * e.speedMul
                else -> 0f
              }
            } else {
              0f
            }
        } else {
          aiDecisionTimer -= dt
          if (aiDecisionTimer <= 0f && e.action == MoveAction.IDLE) {
            aiDecisionTimer = 0.22f + Random.nextFloat() * 0.25f
            val roll = Random.nextFloat()
            if (dist < 95f) {
              when {
                roll < 0.35f -> e = e.copy(action = MoveAction.PUNCH, actionTimer = 0.24f, actionTotal = 0.24f, hitAppliedThisSwing = false, blocking = false)
                roll < 0.62f -> e = e.copy(action = MoveAction.KICK, actionTimer = 0.32f, actionTotal = 0.32f, hitAppliedThisSwing = false, blocking = false)
                roll < 0.80f -> e = e.copy(blocking = true)
                else -> { aiDesiredDir = if (e.x < p.x) -1 else 1; e = e.copy(blocking = false) }
              }
            } else if (e.meter >= 50f && dist > 180f && roll < 0.35f) {
              VaultAudioEngine.playGameSfx(GameSfx.DASH)
              projectiles.add(
                ArenaProjectile(
                  ownerIsPlayer = false,
                  x = e.x + e.facing * 48f,
                  y = -58f + e.y,
                  vx = e.facing * 460f,
                  damage = e.archetype.baseDamage * 1.6f * e.atkMul,
                  color = e.archetype.accentColor,
                )
              )
              e = e.copy(action = MoveAction.SPECIAL, actionTimer = 0.38f, actionTotal = 0.38f, meter = e.meter - 50f, hitAppliedThisSwing = true, blocking = false)
            } else {
              aiDesiredDir = if (e.x > p.x) -1 else 1
              e = e.copy(blocking = false)
            }
          }
          eVx = if (e.action == MoveAction.IDLE && !e.blocking && dist > 75f) aiDesiredDir * e.archetype.baseSpeed * 0.72f else 0f
        }

        var ey = e.y + e.vy * dt
        var eVy = e.vy + 1350f * dt
        if (ey >= 0f) { ey = 0f; eVy = 0f }
        var eAct = e.action
        var eTimer = e.actionTimer - dt
        if (eAct != MoveAction.IDLE && eTimer <= 0f) {
          eAct = MoveAction.IDLE
          eTimer = 0f
        }
        val meterGainRate = if (friendMode) 6f * e.speedMul else 5f
        e = e.copy(
          x = (e.x + eVx * dt).coerceIn(60f, 940f),
          y = ey,
          vx = eVx,
          vy = eVy,
          walkPhase = if (abs(eVx) > 5f && ey >= 0f) e.walkPhase + dt * 11f else e.walkPhase,
          action = eAct,
          actionTimer = eTimer,
          meter = (e.meter + dt * meterGainRate).coerceAtMost(100f),
        )

        // Melee Hit Checks
        if ((p.action == MoveAction.PUNCH || p.action == MoveAction.KICK) && !p.hitAppliedThisSwing) {
          val reach = if (p.action == MoveAction.KICK) 105f else 84f
          if (abs(e.x - p.x) <= reach && abs(e.y - p.y) < 70f) {
            VaultAudioEngine.playGameSfx(GameSfx.HIT)
            val raw = p.archetype.baseDamage * (if (p.action == MoveAction.KICK) 1.35f else 1f) * p.atkMul
            val dmg = if (e.blocking) raw * 0.25f else raw
            e = e.copy(
              hp = (e.hp - dmg).coerceAtLeast(0f),
              x = (e.x + p.facing * 26f).coerceIn(60f, 940f),
              action = if (e.blocking) e.action else MoveAction.HURT,
              actionTimer = if (e.blocking) e.actionTimer else 0.16f,
              actionTotal = 0.16f,
            )
            p = p.copy(hitAppliedThisSwing = true, meter = (p.meter + 14f * p.speedMul).coerceAtMost(100f))
            sparks.add(HitSpark(e.x, e.y - 55f, p.archetype.accentColor, 0.24f))
          }
        }

        if ((e.action == MoveAction.PUNCH || e.action == MoveAction.KICK) && !e.hitAppliedThisSwing) {
          val reach = if (e.action == MoveAction.KICK) 102f else 82f
          if (abs(p.x - e.x) <= reach && abs(p.y - e.y) < 70f) {
            VaultAudioEngine.playGameSfx(GameSfx.HIT)
            val raw = e.archetype.baseDamage * (if (e.action == MoveAction.KICK) 1.3f else 1f) * e.atkMul
            val dmg = if (p.blocking) raw * 0.25f else raw
            p = p.copy(
              hp = (p.hp - dmg).coerceAtLeast(0f),
              x = (p.x + e.facing * 22f).coerceIn(60f, 940f),
              action = if (p.blocking) p.action else MoveAction.HURT,
              actionTimer = if (p.blocking) p.actionTimer else 0.16f,
              actionTotal = 0.16f,
              meter = (p.meter + 8f).coerceAtMost(100f),
            )
            e = e.copy(hitAppliedThisSwing = true, meter = (e.meter + 12f).coerceAtMost(100f))
            sparks.add(HitSpark(p.x, p.y - 55f, e.archetype.accentColor, 0.24f))
          }
        }

        // Projectiles
        for (i in projectiles.lastIndex downTo 0) {
          val pr = projectiles[i]
          val nx = pr.x + pr.vx * dt
          if (nx < 0f || nx > 1000f) {
            projectiles.removeAt(i)
            continue
          }
          if (pr.ownerIsPlayer && abs(nx - e.x) < 38f && abs(pr.y - (e.y - 55f)) < 60f) {
            VaultAudioEngine.playGameSfx(GameSfx.HIT)
            val dmg = if (e.blocking) pr.damage * 0.3f else pr.damage
            e = e.copy(hp = (e.hp - dmg).coerceAtLeast(0f), action = MoveAction.HURT, actionTimer = 0.18f, actionTotal = 0.18f)
            sparks.add(HitSpark(e.x, e.y - 55f, pr.color, 0.30f))
            projectiles.removeAt(i)
          } else if (!pr.ownerIsPlayer && abs(nx - p.x) < 38f && abs(pr.y - (p.y - 55f)) < 60f) {
            VaultAudioEngine.playGameSfx(GameSfx.HIT)
            val dmg = if (p.blocking) pr.damage * 0.3f else pr.damage
            p = p.copy(hp = (p.hp - dmg).coerceAtLeast(0f), action = MoveAction.HURT, actionTimer = 0.18f, actionTotal = 0.18f)
            sparks.add(HitSpark(p.x, p.y - 55f, pr.color, 0.30f))
            projectiles.removeAt(i)
          } else {
            projectiles[i] = pr.copy(x = nx, age = pr.age + dt)
          }
        }

        pState = p
        eState = e

        // Round End Detection
        if (e.hp <= 0f || p.hp <= 0f) {
          if (e.hp <= 0f) {
            playerWins++
            if (playerWins >= 2) {
              matchOver = true
              VaultAudioEngine.playGameSfx(GameSfx.VICTORY)
              if (friendMode) {
                matchBanner = "P1 (${p.archetype.name}) WINS 2P DUEL! (+100 XP)"
                onAwardXp(100, 600, null)
              } else {
                val pts = (900 + p.hp.toInt() * 5 + roundNumber * 120)
                matchBanner = "YOU WIN THE MATCH! (+220 XP · +44 CR)"
                onAwardXp(220, pts, "sf_champion")
              }
            } else {
              roundPauseTimer = 1.6f
              VaultAudioEngine.playGameSfx(GameSfx.VICTORY)
              matchBanner = "KO! ${p.archetype.name} TAKES ROUND $roundNumber!"
              onAwardXp(if (friendMode) 40 else 65, null, null)
            }
          } else {
            enemyWins++
            if (enemyWins >= 2) {
              matchOver = true
              if (friendMode) {
                VaultAudioEngine.playGameSfx(GameSfx.VICTORY)
                matchBanner = "P2 (${e.archetype.name}) WINS 2P DUEL! (+100 XP)"
                onAwardXp(100, 600, null)
              } else {
                VaultAudioEngine.playGameSfx(GameSfx.HIT)
                matchBanner = "DEFEATED BY ${e.archetype.name} (+40 XP Practice)"
                onAwardXp(40, 250, null)
              }
            } else {
              roundPauseTimer = 1.6f
              VaultAudioEngine.playGameSfx(GameSfx.HIT)
              matchBanner = "KO! ${e.archetype.name} TAKES ROUND $roundNumber!"
            }
          }
        }
      }
    }
  }

  Column(
    modifier = modifier.fillMaxSize().padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // Health & Round HUD
    Surface(
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              if (friendMode) "P1 · ${pState.archetype.name} (${playerWins}/2)" else "${pState.archetype.name} (${playerWins}/2)",
              style = MaterialTheme.typography.labelLarge,
              color = pState.archetype.primaryColor,
            )
            LinearProgressIndicator(progress = { (pState.hp / pState.maxHp).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(9.dp), color = pState.archetype.primaryColor)
            Spacer(Modifier.height(3.dp))
            LinearProgressIndicator(progress = { (pState.meter / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(4.dp), color = Color(0xFFFFC857))
          }
          Text("VS", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 10.dp))
          Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(
              if (friendMode) "P2 · ${eState.archetype.name} (${enemyWins}/2)" else "${eState.archetype.name} (${enemyWins}/2)",
              style = MaterialTheme.typography.labelLarge,
              color = eState.archetype.primaryColor,
            )
            LinearProgressIndicator(progress = { (eState.hp / eState.maxHp).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(9.dp), color = eState.archetype.primaryColor)
            Spacer(Modifier.height(3.dp))
            LinearProgressIndicator(progress = { (eState.meter / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(4.dp), color = Color(0xFFFFC857))
          }
        }
        Text(matchBanner, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
      }
    }

    if (friendMode) {
      FighterControlRow(
        label = "P2 CONTROLS (${eState.archetype.name})",
        leftHandedHud = leftHandedHud,
        meter = eState.meter,
        onHoldLeft = { p2HoldLeft = it },
        onHoldRight = { p2HoldRight = it },
        onHoldGuard = { p2HoldBlock = it },
        onJump = {
          if (!matchOver && eState.y >= 0f) {
            VaultAudioEngine.playGameSfx(GameSfx.JUMP)
            eState = eState.copy(vy = -560f)
          }
        },
        onPunch = { triggerFighterAction(false, MoveAction.PUNCH) },
        onKick = { triggerFighterAction(false, MoveAction.KICK) },
        onSpecial = { triggerFighterAction(false, MoveAction.SPECIAL) },
        tagPrefix = "sf_p2",
      )
    }

    // 2D Dojo Canvas
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFF0E131D), RoundedCornerShape(14.dp))) {
        val sx = size.width / 1000f
        val groundY = size.height * 0.80f

        // Background dojo sky & crescent moon / sun + torii silhouette
        drawRect(Brush.verticalGradient(listOf(Color(0xFF101524), Color(0xFF1E2238), Color(0xFF0E131D))), size = Size(size.width, groundY))
        drawCircle(Color(0xFFFF5C7C).copy(alpha = 0.24f), size.minDimension * 0.20f, Offset(size.width * 0.5f, groundY * 0.42f))
        // Dojo pillars & beam
        drawRect(Color(0xFF161A29), Offset(size.width * 0.14f, groundY * 0.18f), Size(14f, groundY * 0.82f))
        drawRect(Color(0xFF161A29), Offset(size.width * 0.84f, groundY * 0.18f), Size(14f, groundY * 0.82f))
        drawRoundRect(Color(0xFF21263B), Offset(size.width * 0.08f, groundY * 0.16f), Size(size.width * 0.84f, 12f), CornerRadius(6f))
        // Swaying Paper Lanterns
        val sway = sin(animTime * 2.2f) * 6f
        listOf(0.24f, 0.76f).forEach { xf ->
          val lx = size.width * xf + sway
          drawLine(Color(0xFF94A3B8), Offset(size.width * xf, groundY * 0.19f), Offset(lx, groundY * 0.28f), 2f)
          drawOval(Color(0xFFFF9F43).copy(alpha = 0.85f), Offset(lx - 11f, groundY * 0.28f), Size(22f, 28f))
        }

        // Dojo wooden floor with perspective planks
        drawRect(Color(0xFF1B202E), Offset(0f, groundY), Size(size.width, size.height - groundY))
        drawLine(Color(0xFF4CE8C8).copy(alpha = 0.55f), Offset(0f, groundY), Offset(size.width, groundY), 4f)
        for (i in 1..7) {
          val xTop = size.width * (i / 8f)
          val xBot = size.width * 0.5f + (xTop - size.width * 0.5f) * 1.45f
          drawLine(Color(0xFF2B3347), Offset(xTop, groundY), Offset(xBot, size.height), 2f)
        }

        val bodyH = size.height * 0.30f
        drawArticulatedFighter(
          f = pState,
          cx = pState.x * sx,
          groundY = groundY + pState.y * (size.height / 500f),
          bodyH = bodyH,
          animTime = animTime,
          isP2MirrorColor = false,
          floorY = groundY,
        )
        drawArticulatedFighter(
          f = eState,
          cx = eState.x * sx,
          groundY = groundY + eState.y * (size.height / 500f),
          bodyH = bodyH,
          animTime = animTime,
          isP2MirrorColor = friendMode && pState.archetype.id == eState.archetype.id,
          floorY = groundY,
        )

        // Animated Projectiles (Energy Fireball / Volt Bolt / Shockwave Orb)
        projectiles.forEach { pr ->
          val px = pr.x * sx
          val py = groundY + pr.y * (size.height / 500f)
          val dir = if (pr.vx >= 0f) 1f else -1f
          val pulse = 1f + sin(pr.age * 24f) * 0.16f
          // Flame / trail tail
          val tail = Path().apply {
            moveTo(px + dir * 18f, py)
            lineTo(px - dir * 36f, py - 14f * pulse)
            lineTo(px - dir * 24f, py)
            lineTo(px - dir * 36f, py + 14f * pulse)
            close()
          }
          drawPath(tail, pr.color.copy(alpha = 0.65f))
          drawCircle(pr.color.copy(alpha = 0.32f), 26f * pulse, Offset(px, py))
          drawCircle(pr.color, 14f * pulse, Offset(px, py))
          drawCircle(Color.White, 6f, Offset(px + dir * 4f, py))
        }

        // Hit Sparks star-bursts
        sparks.forEach { sp ->
          val alpha = (sp.life / 0.30f).coerceIn(0f, 1f)
          val cx = sp.x * sx
          val cy = groundY + sp.y * (size.height / 500f)
          val r = 28f * (1.2f - alpha * 0.4f)
          for (a in 0 until 6) {
            val ang = a * (Math.PI / 3.0) + alpha * 1.5
            drawLine(
              sp.color.copy(alpha = alpha),
              Offset(cx, cy),
              Offset(cx + (cos(ang) * r).toFloat(), cy + (sin(ang) * r).toFloat()),
              strokeWidth = 4f,
              cap = StrokeCap.Round,
            )
          }
          drawCircle(Color.White.copy(alpha = alpha), 8f, Offset(cx, cy))
        }
      }

      if (matchOver) {
        Row(
          modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(onClick = { resetMatch() }) { Text("Rematch") }
          OutlinedButton(onClick = onExitToArmory) { Text("Upgrade / Roster") }
        }
      }
    }

    // Player 1 Touch Controls Row (bottom)
    FighterControlRow(
      label = if (friendMode) "P1 CONTROLS (${pState.archetype.name})" else null,
      leftHandedHud = leftHandedHud,
      meter = pState.meter,
      onHoldLeft = { holdLeft = it },
      onHoldRight = { holdRight = it },
      onHoldGuard = { holdBlock = it },
      onJump = {
        if (!matchOver && pState.y >= 0f) {
          VaultAudioEngine.playGameSfx(GameSfx.JUMP)
          pState = pState.copy(vy = -560f)
        }
      },
      onPunch = { triggerFighterAction(true, MoveAction.PUNCH) },
      onKick = { triggerFighterAction(true, MoveAction.KICK) },
      onSpecial = { triggerFighterAction(true, MoveAction.SPECIAL) },
      tagPrefix = "sf",
    )
  }
}

private fun DrawScope.drawArticulatedFighter(
  f: ArenaFighterState,
  cx: Float,
  groundY: Float,
  bodyH: Float,
  animTime: Float,
  isP2MirrorColor: Boolean,
  floorY: Float = groundY,
) {
  val dir = f.facing.toFloat()
  val isDead = f.hp <= 0f
  val primary =
    when {
      f.action == MoveAction.HURT -> Color.White
      isP2MirrorColor -> f.archetype.accentColor
      else -> f.archetype.primaryColor
    }
  val accent = if (isP2MirrorColor) f.archetype.primaryColor else f.archetype.accentColor
  val skin = Color(0xFFF3D2B3)
  val darkArmor = Color(0xFF131926)

  // Ground shadow stays on floor even when jumping
  drawOval(
    Color.Black.copy(alpha = 0.42f),
    Offset(cx - bodyH * 0.26f, floorY - 7f),
    Size(bodyH * 0.52f, 14f),
  )

  val breath = sin(animTime * 5f + if (dir > 0) 0f else 1.5f) * (bodyH * 0.018f)
  val isAir = f.y < -4f
  val isMoving = abs(f.vx) > 5f && !isAir
  val stride = if (isMoving) sin(f.walkPhase) else 0f
  val swingProgress =
    if (f.action != MoveAction.IDLE && f.actionTotal > 0f) {
      1f - (f.actionTimer / f.actionTotal).coerceIn(0f, 1f)
    } else 0f
  val attackSnap = sin(swingProgress * Math.PI.toFloat())

  val hipY = groundY - bodyH * 0.38f + breath
  val shoulderY = hipY - bodyH * 0.34f
  val headY = shoulderY - bodyH * 0.15f
  val limbW = bodyH * 0.105f

  val leanAngle =
    when {
      isDead -> -86f * dir
      f.action == MoveAction.HURT -> -18f * dir
      f.action == MoveAction.KICK -> -14f * dir * attackSnap
      f.action == MoveAction.PUNCH -> 12f * dir * attackSnap
      isMoving -> (f.vx / 350f) * 8f
      else -> 0f
    }

  rotate(degrees = leanAngle, pivot = Offset(cx, groundY - bodyH * 0.1f)) {
    // 1. Back Leg
    val backFootX = cx - dir * bodyH * 0.14f - stride * bodyH * 0.18f * dir
    val backFootY = if (isAir) groundY - bodyH * 0.14f else groundY
    val backKneeX = (cx + backFootX) * 0.5f + dir * bodyH * 0.04f
    val backKneeY = (hipY + backFootY) * 0.5f - (if (isAir) bodyH * 0.06f else 0f)
    drawLine(darkArmor, Offset(cx - dir * 4f, hipY), Offset(backKneeX, backKneeY), limbW, StrokeCap.Round)
    drawLine(darkArmor.copy(alpha = 0.9f), Offset(backKneeX, backKneeY), Offset(backFootX, backFootY - 4f), limbW * 0.92f, StrokeCap.Round)
    drawRoundRect(accent, Offset(backFootX - 8f, backFootY - 9f), Size(18f, 9f), CornerRadius(4f))

    // 2. Back Arm (punches or braces)
    val bShoulder = Offset(cx - dir * bodyH * 0.08f, shoulderY + 4f)
    val bHand =
      when {
        f.blocking -> Offset(cx + dir * bodyH * 0.16f, shoulderY - bodyH * 0.08f)
        f.action == MoveAction.SPECIAL -> Offset(cx + dir * bodyH * 0.30f, shoulderY + 4f)
        else -> Offset(cx + dir * bodyH * 0.14f - stride * bodyH * 0.14f * dir, shoulderY + bodyH * 0.11f)
      }
    drawLine(skin, bShoulder, bHand, limbW * 0.78f, StrokeCap.Round)
    drawCircle(accent, limbW * 0.58f, bHand)

    // 3. Front Leg (or High Side-Kick!)
    if (f.action == MoveAction.KICK) {
      val kickReach = bodyH * (0.28f + 0.38f * attackSnap)
      val kneePos = Offset(cx + dir * kickReach * 0.52f, hipY - bodyH * 0.08f)
      val kickFoot = Offset(cx + dir * kickReach, hipY - bodyH * 0.15f)
      drawLine(primary, Offset(cx, hipY), kneePos, limbW * 1.05f, StrokeCap.Round)
      drawLine(darkArmor, kneePos, kickFoot, limbW * 0.96f, StrokeCap.Round)
      drawCircle(accent, limbW * 0.65f, kickFoot)
      // Kick slash arc
      drawArc(
        color = accent.copy(alpha = 0.65f * attackSnap),
        startAngle = if (dir > 0) -35f else 145f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = Offset(cx - kickReach * 0.8f, hipY - bodyH * 0.35f),
        size = Size(kickReach * 1.6f, bodyH * 0.45f),
        style = Stroke(width = 5f, cap = StrokeCap.Round),
      )
    } else {
      val frontFootX = cx + dir * bodyH * 0.15f + stride * bodyH * 0.18f * dir
      val frontFootY = if (isAir) groundY - bodyH * 0.18f else groundY
      val frontKneeX = (cx + frontFootX) * 0.5f + dir * bodyH * 0.06f
      val frontKneeY = (hipY + frontFootY) * 0.5f - (if (isAir) bodyH * 0.08f else 0f)
      drawLine(darkArmor, Offset(cx + dir * 4f, hipY), Offset(frontKneeX, frontKneeY), limbW, StrokeCap.Round)
      drawLine(primary.copy(alpha = 0.85f), Offset(frontKneeX, frontKneeY), Offset(frontFootX, frontFootY - 4f), limbW * 0.92f, StrokeCap.Round)
      drawRoundRect(accent, Offset(frontFootX - 8f, frontFootY - 9f), Size(18f, 9f), CornerRadius(4f))
    }

    // 4. Sculpted Torso / Character Armor
    val torsoW = if (f.archetype.id == "titan") bodyH * 0.30f else bodyH * 0.23f
    val torsoPath = Path().apply {
      moveTo(cx - torsoW * 0.56f, shoulderY)
      lineTo(cx + torsoW * 0.56f, shoulderY)
      lineTo(cx + torsoW * 0.40f, hipY + 4f)
      lineTo(cx - torsoW * 0.40f, hipY + 4f)
      close()
    }
    drawPath(torsoPath, primary)

    // Character-specific outfit details
    when (f.archetype.id) {
      "kai" -> {
        // Martial arts V-lapel & black belt sash
        drawLine(Color(0xFF0F172A), Offset(cx - torsoW * 0.22f, shoulderY), Offset(cx + dir * 2f, hipY - 4f), 4f)
        drawLine(Color(0xFF0F172A), Offset(cx + torsoW * 0.22f, shoulderY), Offset(cx + dir * 2f, hipY - 4f), 4f)
        drawRect(Color(0xFF0F172A), Offset(cx - torsoW * 0.42f, hipY - 6f), Size(torsoW * 0.84f, 7f))
      }
      "mira" -> {
        // Cyber-ninja core stripe & glowing chest node
        drawRect(darkArmor, Offset(cx - torsoW * 0.35f, shoulderY + 6f), Size(torsoW * 0.70f, bodyH * 0.20f))
        drawCircle(accent, bodyH * 0.045f, Offset(cx + dir * 3f, shoulderY + bodyH * 0.14f))
      }
      "titan" -> {
        // Heavy X-harness chestplate & spiked pauldrons
        drawLine(accent, Offset(cx - torsoW * 0.48f, shoulderY + 2f), Offset(cx + torsoW * 0.38f, hipY), 5f)
        drawLine(accent, Offset(cx + torsoW * 0.48f, shoulderY + 2f), Offset(cx - torsoW * 0.38f, hipY), 5f)
        drawCircle(darkArmor, bodyH * 0.085f, Offset(cx - torsoW * 0.52f, shoulderY + 4f))
        drawCircle(darkArmor, bodyH * 0.085f, Offset(cx + torsoW * 0.52f, shoulderY + 4f))
      }
    }

    // 5. Head, Hair/Helmet, Headband/Visor, Eyes
    val headR = bodyH * 0.115f
    drawCircle(skin, headR, Offset(cx, headY))

    when (f.archetype.id) {
      "kai" -> {
        // Spiky flame hair + fluttering headband ribbons
        val hair = Path().apply {
          moveTo(cx - headR * 1.1f, headY - headR * 0.2f)
          lineTo(cx - headR * 0.9f, headY - headR * 1.25f)
          lineTo(cx - headR * 0.3f, headY - headR * 0.9f)
          lineTo(cx + headR * 0.2f, headY - headR * 1.38f)
          lineTo(cx + headR * 0.7f, headY - headR * 0.85f)
          lineTo(cx + headR * 1.05f, headY - headR * 0.35f)
          close()
        }
        drawPath(hair, Color(0xFF1E1B24))
        drawRect(accent, Offset(cx - headR, headY - headR * 0.45f), Size(headR * 2f, headR * 0.34f))
        val ribbonWave = sin(animTime * 11f) * 6f
        drawLine(accent, Offset(cx - dir * headR, headY - headR * 0.3f), Offset(cx - dir * (headR + 18f), headY - 4f + ribbonWave), 4f, StrokeCap.Round)
      }
      "mira" -> {
        // Cyber-visor goggles + animated ponytail
        drawArc(Color(0xFF0F172A), 180f, 180f, true, Offset(cx - headR * 1.02f, headY - headR * 1.05f), Size(headR * 2.04f, headR * 1.4f))
        val tailWave = sin(animTime * 9f + f.walkPhase) * 6f
        drawLine(accent, Offset(cx - dir * headR * 0.9f, headY - headR * 0.4f), Offset(cx - dir * (headR + 20f), headY + 4f + tailWave), 6f, StrokeCap.Round)
        drawRoundRect(accent, Offset(cx + (if (dir > 0) 0f else -headR * 0.95f), headY - headR * 0.25f), Size(headR * 0.95f, headR * 0.36f), CornerRadius(4f))
      }
      "titan" -> {
        // Horned juggernaut helmet + glowing visor slit
        drawCircle(darkArmor, headR * 1.08f, Offset(cx, headY - 2f))
        val lHorn = Path().apply {
          moveTo(cx - headR * 0.7f, headY - headR * 0.6f)
          lineTo(cx - headR * 1.3f, headY - headR * 1.55f)
          lineTo(cx - headR * 0.2f, headY - headR * 0.9f)
          close()
        }
        val rHorn = Path().apply {
          moveTo(cx + headR * 0.7f, headY - headR * 0.6f)
          lineTo(cx + headR * 1.3f, headY - headR * 1.55f)
          lineTo(cx + headR * 0.2f, headY - headR * 0.9f)
          close()
        }
        drawPath(lHorn, accent)
        drawPath(rHorn, accent)
        drawRect(accent, Offset(cx + dir * headR * 0.1f - (if (dir < 0) headR * 0.7f else 0f), headY - 2f), Size(headR * 0.7f, 4f))
      }
    }

    // Fierce eyes for non-visored Kai
    if (f.archetype.id == "kai") {
      drawCircle(Color(0xFF0F172A), 2.5f, Offset(cx + dir * headR * 0.45f, headY - 1f))
    }

    // 6. Front Arm (Idle Guard, Punch, Special Cast, or Cross-Shield Block)
    val fShoulder = Offset(cx + dir * bodyH * 0.06f, shoulderY + 4f)
    when {
      f.action == MoveAction.PUNCH -> {
        val punchX = cx + dir * bodyH * (0.20f + 0.36f * attackSnap)
        val fist = Offset(punchX, shoulderY + 2f)
        drawLine(skin, fShoulder, fist, limbW * 0.85f, StrokeCap.Round)
        drawCircle(accent, limbW * 0.78f, fist)
        if (attackSnap > 0.4f) {
          drawLine(Color.White.copy(alpha = 0.7f), Offset(cx + dir * bodyH * 0.18f, shoulderY + 2f), fist, 3f)
        }
      }
      f.action == MoveAction.SPECIAL -> {
        val thrust = Offset(cx + dir * bodyH * 0.36f, shoulderY + 6f)
        drawLine(skin, fShoulder, thrust, limbW * 0.85f, StrokeCap.Round)
        drawCircle(accent, limbW * 0.85f, thrust)
      }
      f.blocking -> {
        val guardHand = Offset(cx + dir * bodyH * 0.18f, shoulderY - bodyH * 0.06f)
        drawLine(skin, fShoulder, guardHand, limbW * 0.82f, StrokeCap.Round)
        drawCircle(accent, limbW * 0.64f, guardHand)
      }
      else -> {
        val elbow = Offset(cx + dir * bodyH * 0.10f, shoulderY + bodyH * 0.15f)
        val guardFist = Offset(cx + dir * bodyH * 0.22f, shoulderY + bodyH * 0.06f - breath * 0.8f)
        drawLine(skin, fShoulder, elbow, limbW * 0.8f, StrokeCap.Round)
        drawLine(skin, elbow, guardFist, limbW * 0.8f, StrokeCap.Round)
        drawCircle(accent, limbW * 0.66f, guardFist)
      }
    }
  }

  // Energy Shield Hex Ring on Block
  if (f.blocking) {
    drawCircle(
      color = Color(0xFF38BDF8).copy(alpha = 0.25f),
      radius = bodyH * 0.48f,
      center = Offset(cx + dir * 12f, groundY - bodyH * 0.46f),
    )
    drawCircle(
      color = Color(0xFF7DD3FC).copy(alpha = 0.8f),
      radius = bodyH * 0.48f,
      center = Offset(cx + dir * 12f, groundY - bodyH * 0.46f),
      style = Stroke(width = 3.5f),
    )
  }
}

@Composable
private fun FighterControlRow(
  label: String?,
  leftHandedHud: Boolean,
  meter: Float,
  onHoldLeft: (Boolean) -> Unit,
  onHoldRight: (Boolean) -> Unit,
  onHoldGuard: (Boolean) -> Unit,
  onJump: () -> Unit,
  onPunch: () -> Unit,
  onKick: () -> Unit,
  onSpecial: () -> Unit,
  tagPrefix: String,
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    if (label != null) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    val movePad: @Composable () -> Unit = {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        HoldControlPad(label = "◀", onHoldChanged = onHoldLeft, tag = "${tagPrefix}_left")
        HoldControlPad(label = "▶", onHoldChanged = onHoldRight, tag = "${tagPrefix}_right")
        HoldControlPad(label = "GUARD", onHoldChanged = onHoldGuard, tag = "${tagPrefix}_guard")
      }
    }

    val actionPad: @Composable () -> Unit = {
      Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
          onClick = onJump,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
          modifier = Modifier.testTag("${tagPrefix}_jump"),
        ) { Text("JUMP") }
        Button(
          onClick = onPunch,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
          modifier = Modifier.testTag("${tagPrefix}_punch"),
        ) { Text("PUNCH") }
        Button(
          onClick = onKick,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
          modifier = Modifier.testTag("${tagPrefix}_kick"),
        ) { Text("KICK") }
        Button(
          onClick = onSpecial,
          enabled = meter >= 50f,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
          modifier = Modifier.testTag("${tagPrefix}_special"),
        ) { Text("SP") }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (leftHandedHud) {
        actionPad()
        movePad()
      } else {
        movePad()
        actionPad()
      }
    }
  }
}

@Composable
private fun HoldControlPad(
  label: String,
  onHoldChanged: (Boolean) -> Unit,
  tag: String,
) {
  var pressed by remember { mutableStateOf(false) }
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = if (pressed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier =
      Modifier
        .height(48.dp)
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            pressed = true
            onHoldChanged(true)
            do {
              val event = awaitPointerEvent()
            } while (event.changes.any { it.pressed })
            pressed = false
            onHoldChanged(false)
          }
        }
        .testTag(tag),
  ) {
    Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
      Text(label, style = MaterialTheme.typography.labelLarge)
    }
  }
}
