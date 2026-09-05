package studio.cortex.thunderstack.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import studio.cortex.thunderstack.data.ProgressStore
import studio.cortex.thunderstack.feedback.FeedbackCue
import studio.cortex.thunderstack.feedback.FeedbackEvent
import studio.cortex.thunderstack.feedback.HapticStrength
import studio.cortex.thunderstack.logic.ProgressRules
import studio.cortex.thunderstack.logic.CrystalRushEngine
import studio.cortex.thunderstack.logic.StackEngine
import studio.cortex.thunderstack.model.AchievementDefinition
import studio.cortex.thunderstack.model.AppScreen
import studio.cortex.thunderstack.model.BoosterType
import studio.cortex.thunderstack.model.CrystalRushPhase
import studio.cortex.thunderstack.model.CrystalRushState
import studio.cortex.thunderstack.model.PlayerProgress
import studio.cortex.thunderstack.model.RunPhase
import studio.cortex.thunderstack.model.StackGameState
import studio.cortex.thunderstack.model.campaignLevels

class ThunderStackViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ProgressStore(application)
    private val engine = StackEngine()
    private val rushEngine = CrystalRushEngine()

    private val _progress = MutableStateFlow(PlayerProgress())
    val progress: StateFlow<PlayerProgress> = _progress.asStateFlow()

    private val _progressReady = MutableStateFlow(false)
    val progressReady: StateFlow<Boolean> = _progressReady.asStateFlow()

    private val _screen = MutableStateFlow(AppScreen.HOME)
    val screen: StateFlow<AppScreen> = _screen.asStateFlow()

    private val _game = MutableStateFlow<StackGameState?>(null)
    val game: StateFlow<StackGameState?> = _game.asStateFlow()

    private val _rush = MutableStateFlow<CrystalRushState?>(null)
    val rush: StateFlow<CrystalRushState?> = _rush.asStateFlow()

    private val _selectedLevel = MutableStateFlow(1)
    val selectedLevel: StateFlow<Int> = _selectedLevel.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _feedback = MutableSharedFlow<FeedbackEvent>(extraBufferCapacity = 24)
    val feedback = _feedback.asSharedFlow()

    private var lastActionAt = 0L

    init {
        viewModelScope.launch {
            _progress.value = store.load()
            _progressReady.value = true
        }
    }

    fun navigate(target: AppScreen) {
        if (!acceptAction()) return
        if (_screen.value == AppScreen.GAMEPLAY && target != AppScreen.GAMEPLAY) {
            _game.value = null
        }
        if (_screen.value == AppScreen.CRYSTAL_RUSH && target != AppScreen.CRYSTAL_RUSH) {
            _rush.value = null
        }
        _screen.value = target
        cue(FeedbackCue.SELECT)
    }

    fun startLevel(number: Int) {
        val progress = _progress.value
        if (number !in 1..progress.highestUnlocked) return
        val level = campaignLevels[number - 1]
        _game.value = engine.create(level)
        _screen.value = AppScreen.GAMEPLAY
        cue(FeedbackCue.SELECT)
    }

    fun selectLevel(number: Int) {
        if (number !in 1.._progress.value.highestUnlocked || !acceptAction()) return
        _selectedLevel.value = number
        _screen.value = AppScreen.PRELEVEL
        cue(FeedbackCue.SELECT)
    }

    fun startSelectedLevel() = startLevel(_selectedLevel.value)

    fun startEndless() {
        val level = campaignLevels[(progress.value.highestUnlocked - 1).coerceIn(0, campaignLevels.lastIndex)]
        _game.value = engine.create(level, endless = true)
        _screen.value = AppScreen.GAMEPLAY
        cue(FeedbackCue.SELECT)
    }

    fun advance(deltaSeconds: Float) {
        val current = _game.value ?: return
        val next = engine.advance(current, deltaSeconds)
        if (next != current) _game.value = commitIfFinished(next)
    }

    fun dropBlock() {
        val current = _game.value ?: return
        if (current.isPaused || current.isFinished || current.phase != RunPhase.PLAYING) return
        val next = engine.drop(current)
        _game.value = commitIfFinished(next)
        when {
            next.won && !current.won -> cue(FeedbackCue.VICTORY, HapticStrength.STRONG)
            next.lastGrade == studio.cortex.thunderstack.model.PlacementGrade.PERFECT -> cue(FeedbackCue.PERFECT, HapticStrength.MEDIUM)
            next.lastGrade == studio.cortex.thunderstack.model.PlacementGrade.CROOKED -> cue(FeedbackCue.CROOKED, HapticStrength.MEDIUM)
            next.lastGrade == studio.cortex.thunderstack.model.PlacementGrade.COLLAPSE -> cue(FeedbackCue.COLLAPSE, HapticStrength.STRONG)
            else -> cue(FeedbackCue.PLACE)
        }
    }

    fun useBooster(type: BoosterType) {
        val current = _game.value ?: return
        if (current.isPaused || current.isFinished || current.phase != RunPhase.PLAYING) return
        val paid = ProgressRules.consumeBooster(_progress.value, type) ?: run {
            showToast("No ${type.title} charges")
            cue(FeedbackCue.ERROR, HapticStrength.MEDIUM)
            return
        }
        save(paid)
        var next = engine.activate(current, type)
        if (type == BoosterType.THUNDER) next = engine.drop(next)
        _game.value = commitIfFinished(next)
        cue(FeedbackCue.BOOSTER, HapticStrength.MEDIUM)
    }

    fun togglePause() {
        val current = _game.value ?: return
        if (current.isFinished || current.phase != RunPhase.PLAYING) return
        _game.value = current.copy(isPaused = !current.isPaused)
        cue(FeedbackCue.SELECT)
    }

    fun restartGame() {
        val current = _game.value ?: return
        _game.value = engine.create(current.level, current.endless)
        cue(FeedbackCue.SELECT)
    }

    fun leaveGameplay(toLevels: Boolean = false) {
        _game.value = null
        _screen.value = if (toLevels) AppScreen.LEVELS else AppScreen.HOME
        cue(FeedbackCue.SELECT)
    }

    fun nextLevel() {
        val current = _game.value ?: return
        if (!current.won || current.endless) return
        val next = (current.level.number + 1).coerceAtMost(campaignLevels.size)
        startLevel(next)
    }

    fun buyBooster(type: BoosterType) {
        if (!acceptAction()) return
        val next = ProgressRules.buyBooster(_progress.value, type) ?: run {
            showToast(if (type.crystalPrice > 0) "Not enough crystals" else "Not enough coins")
            cue(FeedbackCue.ERROR, HapticStrength.MEDIUM)
            return
        }
        save(next)
        showToast("${type.title} added")
        cue(FeedbackCue.REWARD, HapticStrength.MEDIUM)
    }

    fun claimAchievement(definition: AchievementDefinition) {
        if (!acceptAction()) return
        val result = ProgressRules.claimAchievement(_progress.value, definition) ?: return
        save(result.progress)
        showToast(result.message)
        cue(FeedbackCue.REWARD, HapticStrength.STRONG)
    }

    fun claimDaily() {
        if (!acceptAction()) return
        val result = ProgressRules.claimDaily(_progress.value, localEpochDay()) ?: run {
            showToast("Already claimed — come back tomorrow")
            return
        }
        save(result.progress)
        showToast(result.message)
        cue(FeedbackCue.REWARD, HapticStrength.STRONG)
    }

    fun openCrystalRush() {
        if (!acceptAction()) return
        _rush.value = rushEngine.create(isCrystalRushRewardAvailable())
        _screen.value = AppScreen.CRYSTAL_RUSH
        cue(FeedbackCue.SELECT)
    }

    fun startCrystalRush() {
        val current = _rush.value ?: rushEngine.create(isCrystalRushRewardAvailable())
        _rush.value = rushEngine.start(current.copy(rewardEligible = isCrystalRushRewardAvailable()))
        cue(FeedbackCue.SELECT)
    }

    fun advanceCrystalRush(deltaSeconds: Float) {
        val current = _rush.value ?: return
        var next = rushEngine.advance(current, deltaSeconds)
        if (next.phase == CrystalRushPhase.FINISHED && !next.rewardCommitted) {
            val reward = ProgressRules.applyCrystalRushResult(_progress.value, next.score, localEpochDay())
            save(reward.progress)
            next = next.copy(rewardCommitted = true)
            if (reward.rewarded) {
                showToast("Crystal Rush: +${reward.coins} coins, +${reward.crystals} crystals")
                cue(FeedbackCue.REWARD, HapticStrength.STRONG)
            } else {
                showToast("Practice complete — best score saved")
            }
        }
        _rush.value = next
    }

    fun moveCrystalRushMagnet(normalizedX: Float) {
        _rush.value = _rush.value?.let { rushEngine.moveMagnet(it, normalizedX) }
    }

    fun toggleCrystalRushPause() {
        _rush.value = _rush.value?.let(rushEngine::togglePause)
        cue(FeedbackCue.SELECT)
    }

    fun restartCrystalRush() {
        _rush.value = rushEngine.create(isCrystalRushRewardAvailable())
        cue(FeedbackCue.SELECT)
    }

    fun leaveCrystalRush() {
        _rush.value = null
        _screen.value = AppScreen.HOME
        cue(FeedbackCue.SELECT)
    }

    fun setSound(enabled: Boolean) = save(_progress.value.copy(soundEnabled = enabled))
    fun setMusic(enabled: Boolean) = save(_progress.value.copy(musicEnabled = enabled))
    fun setHaptics(enabled: Boolean) = save(_progress.value.copy(hapticsEnabled = enabled))
    fun setReducedFlashes(enabled: Boolean) = save(_progress.value.copy(reducedFlashes = enabled))
    fun setHighContrast(enabled: Boolean) = save(_progress.value.copy(highContrast = enabled))

    fun handleBack() {
        when (_screen.value) {
            AppScreen.GAMEPLAY -> {
                val current = _game.value
                when {
                    current == null -> leaveGameplay()
                    current.isFinished -> leaveGameplay(toLevels = !current.endless)
                    current.isPaused -> togglePause()
                    else -> togglePause()
                }
            }
            AppScreen.CRYSTAL_RUSH -> {
                val current = _rush.value
                when {
                    current == null || current.phase != CrystalRushPhase.PLAYING -> leaveCrystalRush()
                    else -> toggleCrystalRushPause()
                }
            }
            AppScreen.HOME -> Unit
            else -> navigate(AppScreen.HOME)
        }
    }

    fun onAppBackground() {
        val current = _game.value
        if (current != null && !current.isFinished && !current.isPaused) _game.value = current.copy(isPaused = true)
        val rush = _rush.value
        if (rush?.phase == CrystalRushPhase.PLAYING && !rush.isPaused) {
            _rush.value = rushEngine.togglePause(rush)
        }
        viewModelScope.launch { store.save(_progress.value) }
    }

    fun dismissToast() { _toast.value = null }

    fun isDailyClaimedToday(): Boolean = _progress.value.lastDailyClaimEpochDay == localEpochDay()

    fun isCrystalRushRewardAvailable(): Boolean =
        _progress.value.lastCrystalRushRewardEpochDay != localEpochDay()

    fun starsFor(state: StackGameState): Int = engine.stars(state)

    private fun commitIfFinished(state: StackGameState): StackGameState {
        if (!state.isFinished || state.resultCommitted) return state
        val stars = engine.stars(state)
        save(ProgressRules.applyGameResult(_progress.value, state, stars))
        return state.copy(resultCommitted = true)
    }

    private fun save(progress: PlayerProgress) {
        val safe = ProgressRules.syncDerivedMetrics(progress)
        _progress.value = safe
        viewModelScope.launch { store.save(safe) }
    }

    private fun showToast(message: String) { _toast.value = message }

    private fun acceptAction(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastActionAt < 180L) return false
        lastActionAt = now
        return true
    }

    private fun cue(cue: FeedbackCue, strength: HapticStrength = HapticStrength.LIGHT) {
        _feedback.tryEmit(FeedbackEvent(cue, strength))
    }

    private fun localEpochDay(now: Long = System.currentTimeMillis()): Long {
        val offset = TimeZone.getDefault().getOffset(now)
        return TimeUnit.MILLISECONDS.toDays(now + offset)
    }
}
