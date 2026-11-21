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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsMma
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FighterStat
import com.example.data.model.FighterUpgradeLevels
import kotlin.math.abs
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
  val vy: Float = 0f,
  val facing: Int = 1, // 1 right, -1 left
  val hp: Float,
  val maxHp: Float,
  val meter: Float = 25f, // 0..100
  val action: MoveAction = MoveAction.IDLE,
  val actionTimer: Float = 0f,
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
  var selectedFighterId by remember { mutableStateOf("kai") }
  var inArena by remember { mutableStateOf(false) }

  if (!inArena) {
    FighterSelectAndUpgradeScreen(
      selectedFighterId = selectedFighterId,
      vaultCredits = vaultCredits,
      fighterUpgrades = fighterUpgrades,
      onSelectFighter = { selectedFighterId = it },
      onUpgradeStat = onUpgradeStat,
      onStartFight = { inArena = true },
      modifier = modifier,
    )
  } else {
    val playerArch = STREET_FIGHTERS.first { it.id == selectedFighterId }
    val enemyArch = remember(selectedFighterId) {
      STREET_FIGHTERS.first { it.id != selectedFighterId }
    }
    val upgrades = fighterUpgrades[selectedFighterId] ?: FighterUpgradeLevels()

    StreetFighterArena(
      playerArch = playerArch,
      enemyArch = enemyArch,
      upgrades = upgrades,
      leftHandedHud = leftHandedHud,
      onAwardXp = onAwardXp,
      onExitToArmory = { inArena = false },
      modifier = modifier,
    )
  }
}

@Composable
private fun FighterSelectAndUpgradeScreen(
  selectedFighterId: String,
  vaultCredits: Int,
  fighterUpgrades: Map<String, FighterUpgradeLevels>,
  onSelectFighter: (String) -> Unit,
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
      Column {
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

    // Fighter Stat & Upgrade Card
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
          Column {
            Text("${selectedArch.name} — ${selectedArch.title}", style = MaterialTheme.typography.titleMedium, color = selectedArch.primaryColor)
            Text("Special Move: ${selectedArch.specialName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Surface(shape = RoundedCornerShape(10.dp), color = selectedArch.primaryColor.copy(alpha = 0.16f)) {
            Icon(Icons.Rounded.SportsMma, contentDescription = null, tint = selectedArch.primaryColor, modifier = Modifier.padding(10.dp).size(24.dp))
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
      Text("ENTER ARENA · BEST OF 3")
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
  playerArch: FighterArchetype,
  enemyArch: FighterArchetype,
  upgrades: FighterUpgradeLevels,
  leftHandedHud: Boolean,
  onAwardXp: (Int, Int?, String?) -> Unit,
  onExitToArmory: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val playerMaxHp = (playerArch.baseHp + (upgrades.healthLevel - 1) * 18).toFloat()
  val playerAtkMul = 1f + (upgrades.attackLevel - 1) * 0.18f
  val playerSpdMul = 1f + (upgrades.speedLevel - 1) * 0.10f

  var playerWins by remember { mutableIntStateOf(0) }
  var enemyWins by remember { mutableIntStateOf(0) }
  var roundNumber by remember { mutableIntStateOf(1) }
  var matchBanner by remember { mutableStateOf("ROUND 1 · FIGHT!") }
  var matchOver by remember { mutableStateOf(false) }

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
    val hp = (enemyArch.baseHp + (round - 1) * 14).toFloat()
    return ArenaFighterState(
      archetype = enemyArch,
      x = 780f,
      y = 0f,
      facing = -1,
      hp = hp,
      maxHp = hp,
      atkMul = 0.9f + (round - 1) * 0.12f,
    )
  }

  var pState by remember { mutableStateOf(initialPlayer()) }
  var eState by remember { mutableStateOf(initialEnemy(1)) }
  val projectiles = remember { mutableStateListOf<ArenaProjectile>() }
  val sparks = remember { mutableStateListOf<HitSpark>() }

  var holdLeft by remember { mutableStateOf(false) }
  var holdRight by remember { mutableStateOf(false) }
  var holdBlock by remember { mutableStateOf(false) }
  var aiDecisionTimer by remember { mutableStateOf(0.25f) }
  var aiDesiredDir by remember { mutableIntStateOf(-1) }
  var roundPauseTimer by remember { mutableStateOf(0f) }

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

  fun triggerPlayerAction(move: MoveAction) {
    if (matchOver || roundPauseTimer > 0f) return
    if (pState.action != MoveAction.IDLE) return
    if (move == MoveAction.SPECIAL && pState.meter < 50f) return
    val dur =
      when (move) {
        MoveAction.PUNCH -> 0.22f
        MoveAction.KICK -> 0.30f
        MoveAction.SPECIAL -> 0.36f
        else -> 0.25f
      }
    if (move == MoveAction.SPECIAL) {
      projectiles.add(
        ArenaProjectile(
          ownerIsPlayer = true,
          x = pState.x + pState.facing * 42f,
          y = -58f + pState.y,
          vx = pState.facing * 520f,
          damage = pState.archetype.baseDamage * 1.9f * pState.atkMul,
          color = pState.archetype.accentColor,
        )
      )
      pState = pState.copy(action = move, actionTimer = dur, meter = (pState.meter - 50f).coerceAtLeast(0f), hitAppliedThisSwing = true)
    } else {
      pState = pState.copy(action = move, actionTimer = dur, hitAppliedThisSwing = false)
    }
  }

  // 60fps Game Loop
  LaunchedEffect(Unit) {
    var lastNanos = 0L
    while (true) {
      withFrameNanos { now ->
        if (lastNanos == 0L) {
          lastNanos = now
          return@withFrameNanos
        }
        val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.04f)
        lastNanos = now

        // Fade sparks
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

        // Facing
        p = p.copy(facing = if (p.x <= e.x) 1 else -1, blocking = holdBlock && p.action == MoveAction.IDLE)
        e = e.copy(facing = if (e.x >= p.x) -1 else 1)

        // Player Movement & Gravity
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
          vy = pVy,
          action = pAct,
          actionTimer = pTimer,
          meter = (p.meter + dt * 6f * p.speedMul).coerceAtMost(100f),
        )

        // Enemy AI
        aiDecisionTimer -= dt
        val dist = abs(p.x - e.x)
        if (aiDecisionTimer <= 0f && e.action == MoveAction.IDLE) {
          aiDecisionTimer = 0.22f + Random.nextFloat() * 0.25f
          val roll = Random.nextFloat()
          if (dist < 95f) {
            when {
              roll < 0.35f -> e = e.copy(action = MoveAction.PUNCH, actionTimer = 0.24f, hitAppliedThisSwing = false, blocking = false)
              roll < 0.62f -> e = e.copy(action = MoveAction.KICK, actionTimer = 0.32f, hitAppliedThisSwing = false, blocking = false)
              roll < 0.80f -> e = e.copy(blocking = true)
              else -> { aiDesiredDir = if (e.x < p.x) -1 else 1; e = e.copy(blocking = false) }
            }
          } else if (e.meter >= 50f && dist > 180f && roll < 0.35f) {
            projectiles.add(
              ArenaProjectile(
                ownerIsPlayer = false,
                x = e.x + e.facing * 42f,
                y = -58f + e.y,
                vx = e.facing * 460f,
                damage = e.archetype.baseDamage * 1.6f * e.atkMul,
                color = e.archetype.accentColor,
              )
            )
            e = e.copy(action = MoveAction.SPECIAL, actionTimer = 0.36f, meter = e.meter - 50f, hitAppliedThisSwing = true, blocking = false)
          } else {
            aiDesiredDir = if (e.x > p.x) -1 else 1
            e = e.copy(blocking = false)
          }
        }

        val eVx = if (e.action == MoveAction.IDLE && !e.blocking && dist > 75f) aiDesiredDir * e.archetype.baseSpeed * 0.72f else 0f
        var ey = e.y + e.vy * dt
        var eVy = e.vy + 1350f * dt
        if (ey >= 0f) { ey = 0f; eVy = 0f }
        var eAct = e.action
        var eTimer = e.actionTimer - dt
        if (eAct != MoveAction.IDLE && eTimer <= 0f) {
          eAct = MoveAction.IDLE
          eTimer = 0f
        }
        e = e.copy(
          x = (e.x + eVx * dt).coerceIn(60f, 940f),
          y = ey,
          vy = eVy,
          action = eAct,
          actionTimer = eTimer,
          meter = (e.meter + dt * 5f).coerceAtMost(100f),
        )

        // Melee Hit Checks
        if ((p.action == MoveAction.PUNCH || p.action == MoveAction.KICK) && !p.hitAppliedThisSwing) {
          val reach = if (p.action == MoveAction.KICK) 105f else 84f
          if (abs(e.x - p.x) <= reach && abs(e.y - p.y) < 70f) {
            val raw = p.archetype.baseDamage * (if (p.action == MoveAction.KICK) 1.35f else 1f) * p.atkMul
            val dmg = if (e.blocking) raw * 0.25f else raw
            e = e.copy(
              hp = (e.hp - dmg).coerceAtLeast(0f),
              x = (e.x + p.facing * 26f).coerceIn(60f, 940f),
              action = if (e.blocking) e.action else MoveAction.HURT,
              actionTimer = if (e.blocking) e.actionTimer else 0.14f,
            )
            p = p.copy(hitAppliedThisSwing = true, meter = (p.meter + 14f * p.speedMul).coerceAtMost(100f))
            sparks.add(HitSpark(e.x, e.y - 55f, p.archetype.accentColor, 0.22f))
          }
        }

        if ((e.action == MoveAction.PUNCH || e.action == MoveAction.KICK) && !e.hitAppliedThisSwing) {
          val reach = if (e.action == MoveAction.KICK) 102f else 82f
          if (abs(p.x - e.x) <= reach && abs(p.y - e.y) < 70f) {
            val raw = e.archetype.baseDamage * (if (e.action == MoveAction.KICK) 1.3f else 1f) * e.atkMul
            val dmg = if (p.blocking) raw * 0.25f else raw
            p = p.copy(
              hp = (p.hp - dmg).coerceAtLeast(0f),
              x = (p.x + e.facing * 22f).coerceIn(60f, 940f),
              action = if (p.blocking) p.action else MoveAction.HURT,
              actionTimer = if (p.blocking) p.actionTimer else 0.14f,
              meter = (p.meter + 8f).coerceAtMost(100f),
            )
            e = e.copy(hitAppliedThisSwing = true, meter = (e.meter + 12f).coerceAtMost(100f))
            sparks.add(HitSpark(p.x, p.y - 55f, e.archetype.accentColor, 0.22f))
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
            val dmg = if (e.blocking) pr.damage * 0.3f else pr.damage
            e = e.copy(hp = (e.hp - dmg).coerceAtLeast(0f), action = MoveAction.HURT, actionTimer = 0.16f)
            sparks.add(HitSpark(e.x, e.y - 55f, pr.color, 0.28f))
            projectiles.removeAt(i)
          } else if (!pr.ownerIsPlayer && abs(nx - p.x) < 38f && abs(pr.y - (p.y - 55f)) < 60f) {
            val dmg = if (p.blocking) pr.damage * 0.3f else pr.damage
            p = p.copy(hp = (p.hp - dmg).coerceAtLeast(0f), action = MoveAction.HURT, actionTimer = 0.16f)
            sparks.add(HitSpark(p.x, p.y - 55f, pr.color, 0.28f))
            projectiles.removeAt(i)
          } else {
            projectiles[i] = pr.copy(x = nx)
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
              val pts = (900 + p.hp.toInt() * 5 + roundNumber * 120)
              matchBanner = "YOU WIN THE MATCH! (+220 XP · +44 CR)"
              onAwardXp(220, pts, "sf_champion")
            } else {
              roundPauseTimer = 1.6f
              matchBanner = "KO! ${p.archetype.name} TAKES ROUND $roundNumber!"
              onAwardXp(65, null, null)
            }
          } else {
            enemyWins++
            if (enemyWins >= 2) {
              matchOver = true
              matchBanner = "DEFEATED BY ${e.archetype.name} (+40 XP Practice)"
              onAwardXp(40, 250, null)
            } else {
              roundPauseTimer = 1.6f
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
            Text("${pState.archetype.name} (${playerWins}/2)", style = MaterialTheme.typography.labelLarge, color = pState.archetype.primaryColor)
            LinearProgressIndicator(progress = { (pState.hp / pState.maxHp).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(9.dp), color = pState.archetype.primaryColor)
            Spacer(Modifier.height(3.dp))
            LinearProgressIndicator(progress = { (pState.meter / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(4.dp), color = Color(0xFFFFC857))
          }
          Text("VS", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 10.dp))
          Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text("${eState.archetype.name} (${enemyWins}/2)", style = MaterialTheme.typography.labelLarge, color = eState.archetype.primaryColor)
            LinearProgressIndicator(progress = { (eState.hp / eState.maxHp).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(9.dp), color = eState.archetype.primaryColor)
            Spacer(Modifier.height(3.dp))
            LinearProgressIndicator(progress = { (eState.meter / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(4.dp), color = Color(0xFFFFC857))
          }
        }
        Text(matchBanner, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
      }
    }

    // 2D Dojo Canvas
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFF0E131D), RoundedCornerShape(14.dp))) {
        val sx = size.width / 1000f
        val groundY = size.height * 0.78f

        // Background dojo sky
        drawRect(Brush.verticalGradient(listOf(Color(0xFF131826), Color(0xFF1F273B), Color(0xFF0E131D))), size = Size(size.width, groundY))
        drawCircle(Color(0xFFFF5C7C).copy(alpha = 0.22f), size.minDimension * 0.22f, Offset(size.width * 0.5f, groundY * 0.45f))

        // Floor
        drawRect(Color(0xFF1C2331), Offset(0f, groundY), Size(size.width, size.height - groundY))
        drawLine(Color(0xFF4CE8C8).copy(alpha = 0.5f), Offset(0f, groundY), Offset(size.width, groundY), 4f)

        fun drawFighter(f: ArenaFighterState) {
          val cx = f.x * sx
          val fy = groundY + f.y * (size.height / 500f)
          val bodyH = size.height * 0.26f
          val bodyW = bodyH * 0.42f

          // Shadow
          drawOval(Color.Black.copy(alpha = 0.35f), Offset(cx - bodyW * 0.6f, groundY - 6f), Size(bodyW * 1.2f, 12f))

          val tint = if (f.action == MoveAction.HURT) Color.White else f.archetype.primaryColor
          // Torso
          drawRoundRect(tint, Offset(cx - bodyW / 2f, fy - bodyH * 0.72f), Size(bodyW, bodyH * 0.45f), CornerRadius(10f))
          // Legs
          drawRoundRect(Color(0xFF111827), Offset(cx - bodyW * 0.45f, fy - bodyH * 0.30f), Size(bodyW * 0.36f, bodyH * 0.30f), CornerRadius(6f))
          drawRoundRect(Color(0xFF111827), Offset(cx + bodyW * 0.09f, fy - bodyH * 0.30f), Size(bodyW * 0.36f, bodyH * 0.30f), CornerRadius(6f))
          // Head & Headband
          val headR = bodyW * 0.38f
          drawCircle(Color(0xFFF3D5B5), headR, Offset(cx, fy - bodyH * 0.84f))
          drawRect(f.archetype.accentColor, Offset(cx - headR, fy - bodyH * 0.90f), Size(headR * 2f, headR * 0.45f))

          // Extended limbs for attack
          if (f.action == MoveAction.PUNCH || f.action == MoveAction.SPECIAL) {
            drawRoundRect(f.archetype.accentColor, Offset(cx + f.facing * bodyW * 0.2f - (if (f.facing < 0) bodyW * 1.1f else 0f), fy - bodyH * 0.65f), Size(bodyW * 1.1f, bodyH * 0.14f), CornerRadius(8f))
          } else if (f.action == MoveAction.KICK) {
            drawRoundRect(f.archetype.accentColor, Offset(cx + f.facing * bodyW * 0.2f - (if (f.facing < 0) bodyW * 1.35f else 0f), fy - bodyH * 0.35f), Size(bodyW * 1.35f, bodyH * 0.15f), CornerRadius(8f))
          }

          if (f.blocking) {
            drawCircle(Color(0xFF38BDF8).copy(alpha = 0.35f), bodyH * 0.52f, Offset(cx + f.facing * 12f, fy - bodyH * 0.5f))
          }
        }

        drawFighter(pState)
        drawFighter(eState)

        // Projectiles
        projectiles.forEach { pr ->
          val px = pr.x * sx
          val py = groundY + pr.y * (size.height / 500f)
          drawCircle(pr.color.copy(alpha = 0.35f), 26f, Offset(px, py))
          drawCircle(pr.color, 15f, Offset(px, py))
        }

        // Sparks
        sparks.forEach { sp ->
          drawCircle(sp.color.copy(alpha = (sp.life / 0.28f).coerceIn(0f, 1f)), 22f, Offset(sp.x * sx, groundY + sp.y * (size.height / 500f)))
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

    // Touch Controls Row
    val movePad: @Composable () -> Unit = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        HoldControlPad(label = "◀", onHoldChanged = { holdLeft = it }, tag = "sf_left")
        HoldControlPad(label = "▶", onHoldChanged = { holdRight = it }, tag = "sf_right")
        HoldControlPad(label = "GUARD", onHoldChanged = { holdBlock = it }, tag = "sf_guard")
      }
    }

    val actionPad: @Composable () -> Unit = {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
          onClick = {
            if (!matchOver && pState.y >= 0f) pState = pState.copy(vy = -560f)
          },
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
        ) { Text("JUMP") }
        Button(
          onClick = { triggerPlayerAction(MoveAction.PUNCH) },
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
          modifier = Modifier.testTag("sf_punch"),
        ) { Text("PUNCH") }
        Button(
          onClick = { triggerPlayerAction(MoveAction.KICK) },
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
          modifier = Modifier.testTag("sf_kick"),
        ) { Text("KICK") }
        Button(
          onClick = { triggerPlayerAction(MoveAction.SPECIAL) },
          enabled = pState.meter >= 50f,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
          modifier = Modifier.testTag("sf_special"),
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
    Box(modifier = Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
      Text(label, style = MaterialTheme.typography.labelLarge)
    }
  }
}
