package com.onetime.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onetime.app.data.MAX_NOTE_LENGTH
import com.onetime.app.data.coinText
import com.onetime.app.ui.chrome.Dock
import com.onetime.app.ui.chrome.DockTab
import com.onetime.app.ui.chrome.MiniDock
import com.onetime.app.ui.chrome.TopBar
import com.onetime.app.ui.screens.FocusScreen
import com.onetime.app.ui.screens.NoteScreen
import com.onetime.app.ui.screens.ReviewScreen
import com.onetime.app.ui.screens.ShopScreen
import com.onetime.app.ui.screens.TodoScreen
import com.onetime.app.ui.theme.InsetBox
import com.onetime.app.ui.theme.KeyCap
import com.onetime.app.ui.theme.OneTimeTheme
import com.onetime.app.ui.theme.Paper
import com.onetime.app.ui.theme.PaperBackground
import kotlinx.coroutines.launch

private enum class ShellMode { CARD, FULLSCREEN, COLLAPSED }

@Composable
fun AppRoot(vm: AppViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()
    val taskInput by vm.taskInput.collectAsStateWithLifecycle()
    val settlement by vm.settlement.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()
    val selectedMinutes by vm.selectedMinutes.collectAsStateWithLifecycle()

    val timer = remember(data.timer, now, taskInput) { vm.liveTimer() }
    var tabIndex by rememberSaveable { mutableStateOf(0) }
    var modeName by rememberSaveable { mutableStateOf(ShellMode.CARD.name) }
    var showBackup by remember { mutableStateOf(false) }
    var showSettlement by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(toast) {
        val text = toast ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        vm.consumeToast()
    }
    LaunchedEffect(settlement) { if (settlement != null) showSettlement = true }

    val mode = ShellMode.valueOf(modeName)
    val dockTab = when (tabIndex) {
        1 -> DockTab.TODO
        2 -> DockTab.REVIEW
        3 -> DockTab.NOTE
        else -> null
    }

    OneTimeTheme(night = data.settings.night) {
        PaperBackground(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                if (mode == ShellMode.COLLAPSED) {
                    Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.BottomCenter) {
                        MiniDock(
                            timer = timer,
                            coins = data.coins,
                            onExpand = { modeName = ShellMode.CARD.name },
                            onToggleTimer = {
                                if (!timer.active) vm.start() else if (timer.paused) vm.resume()
                            },
                        )
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .padding(top = 4.dp),
                    ) {
                        TopBar(
                            data = data,
                            now = now,
                            onBrandClick = { showBackup = true },
                            onTimeClick = { vm.toggleSeconds() },
                            onThemeToggle = { vm.toggleTheme() },
                            onCoinsClick = { tabIndex = 4 },
                        )

                        Spacer(Modifier.height(8.dp))

                        Box(Modifier.weight(1f)) {
                            when (tabIndex) {
                                0 -> FocusScreen(
                                    data = data,
                                    timer = timer,
                                    selectedMinutes = selectedMinutes,
                                    taskInput = taskInput,
                                    onTaskInput = vm::setTaskInput,
                                    onSelectMinutes = vm::setPlannedMinutes,
                                    onToggleUnit = vm::toggleUnit,
                                    onToggleTimer = {
                                        when {
                                            !timer.active -> vm.start()
                                            timer.paused -> vm.resume()
                                            else -> vm.pause("")
                                        }
                                    },
                                    onPause = { reason -> vm.pause(reason) },
                                    onPauseOrComplete = { reason -> vm.pauseOrComplete(reason) },
                                    onStop = { reason -> vm.manualFinish(reason) },
                                    onResume = { vm.resume() },
                                    onSaveSlot = { selectedMinutes?.let { vm.saveSlotFromMinutes(it) } },
                                    onUpdateSlot = vm::updateSlot,
                                    onRemoveSlot = vm::removeSlot,
                                    onPickTodo = { todo, sub -> vm.applyTodoSelection(todo, sub) },
                                    playIntro = !data.settings.needleIntroPlayed,
                                    onIntroFinished = vm::markNeedleIntroPlayed,
                                )

                                1 -> TodoScreen(
                                    data = data,
                                    timer = timer,
                                    onBack = { tabIndex = 0 },
                                    onAdd = vm::addTodo,
                                    onAddGoal = vm::addGoal,
                                    onToggle = vm::toggleTodo,
                                    onToggleSub = vm::toggleSubtask,
                                    onPlayTodo = { todo -> vm.startFocusFromTodo(todo); tabIndex = 0 },
                                    onPlaySub = { todo, sub -> vm.startFocusFromTodo(todo, sub); tabIndex = 0 },
                                    onDelete = vm::deleteTodo,
                                    onSetDue = vm::setTodoDueDate,
                                    onEditSplit = { todo, subs -> vm.replaceSubtasks(todo.id, subs) },
                                    onMoveSub = vm::moveSubtask,
                                    onAddQuickDay = vm::addQuickDay,
                                )

                                2 -> ReviewScreen(
                                    data = data,
                                    timer = timer,
                                    onBack = { tabIndex = 0 },
                                )

                                3 -> NoteScreen(
                                    data = data,
                                    timer = timer,
                                    onBack = { tabIndex = 0 },
                                    onAdd = vm::addNote,
                                    onUpdate = vm::updateNote,
                                )

                                else -> ShopScreen(
                                    data = data,
                                    timer = timer,
                                    onBack = { tabIndex = 0 },
                                    onRedeem = vm::redeem,
                                    onAddItem = vm::addCustomItem,
                                    onDeleteItem = vm::deleteCustomItem,
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Dock(selected = dockTab, onSelect = { tab -> tabIndex = tab.ordinal })
                        Spacer(Modifier.height(8.dp))
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                )
            }

            if (showBackup) {
                BackupDialog(data = data, vm = vm, onDismiss = { showBackup = false })
            }

            val s = settlement
            if (s != null && showSettlement) {
                SettlementDialog(
                    settlement = s,
                    onDismiss = { save, note ->
                        vm.dismissSettlement(save, note)
                        showSettlement = false
                    },
                )
            }
        }
    }
}

@Composable
private fun FullscreenNav(selected: Int, onSelect: (Int) -> Unit) {
    val palette = Paper.current
    val labels = listOf("专注", "待办", "复盘", "随记", "商城")
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) palette.goldWash else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 12.sp,
                    color = if (isSelected) palette.goldDeep else palette.muted,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun SettlementDialog(
    settlement: Settlement,
    onDismiss: (Boolean, String) -> Unit,
) {
    val palette = Paper.current
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onDismiss(false, "") },
        containerColor = palette.card,
        title = {
            Text(
                if (settlement.completed) "专注完成" else "专注结束",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = palette.ink,
            )
        },
        text = {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    com.onetime.app.ui.theme.CoinIcon(size = 18.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (settlement.coins > 0) "+${settlement.coins.coinText()}" else "+0",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (settlement.coins > 0) palette.goldDeep else palette.muted,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("金币", fontSize = 13.sp, color = palette.muted, modifier = Modifier.padding(bottom = 5.dp))
                }
                Spacer(Modifier.height(6.dp))
                SettlementLine("专注时长", "${settlement.minutes} 分钟")
                SettlementLine("专注事项", settlement.taskName)
                if (settlement.reason.isNotBlank()) {
                    SettlementLine("备注", settlement.reason)
                }
                Spacer(Modifier.height(10.dp))
                Text("本次心得（可选，最多 $MAX_NOTE_LENGTH 字）", fontSize = 11.5.sp, color = palette.muted)
                Spacer(Modifier.height(4.dp))
                InsetBox(Modifier.fillMaxWidth().heightIn(min = 70.dp), shape = RoundedCornerShape(10.dp)) {
                    Box(Modifier.padding(10.dp)) {
                        if (note.isEmpty()) Text("记下这次专注的感受…", fontSize = 12.5.sp, color = palette.muted)
                        BasicTextField(
                            value = note,
                            onValueChange = { if (it.length <= MAX_NOTE_LENGTH) note = it },
                            textStyle = TextStyle(fontSize = 13.sp, color = palette.ink, lineHeight = 19.sp),
                            cursorBrush = SolidColor(palette.coral),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        )
                    }
                }
                Text("${note.length} / $MAX_NOTE_LENGTH", fontSize = 10.sp, color = palette.muted)
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(true, note) }) {
                Text("保存到随记本", color = palette.coralDeep, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss(false, "") }) { Text("跳过", color = palette.muted) }
        },
    )
}

@Composable
private fun SettlementLine(label: String, value: String) {
    val palette = Paper.current
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, fontSize = 12.sp, color = palette.muted, modifier = Modifier.width(60.dp))
        Text(value, fontSize = 12.5.sp, color = palette.ink, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BackupDialog(data: com.onetime.app.data.AppData, vm: AppViewModel, onDismiss: () -> Unit) {
    val palette = Paper.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importedJson by remember { mutableStateOf<String?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.onSuccess { importedJson = it }.onFailure { importError = "读取失败" }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        title = { Text("数据备份", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = palette.ink) },
        text = {
            Column {
                BackupLine("待办", "${data.todos.size}")
                BackupLine("随记", "${data.notes.size}")
                BackupLine("专注记录", "${data.sessions.size}")
                BackupLine("商品", "${data.shopItems.size}")
                BackupLine("金币余额", data.coins.coinText())
                Spacer(Modifier.height(8.dp))
                Text("数据只保存在本机，不会上传。", fontSize = 11.sp, color = palette.muted)

                if (importedJson != null) {
                    Spacer(Modifier.height(10.dp))
                    val preview = remember(importedJson) {
                        runCatching {
                            val text = importedJson!!
                            val match = Regex("\"todos\"\\s*:\\s*\\[").findAll(text).count()
                            text.length
                        }
                    }
                    Text(
                        "已选择备份文件（${preview.getOrNull() ?: 0} 字节）。导入会覆盖当前全部数据，且无法撤销。",
                        fontSize = 11.5.sp,
                        color = palette.coralDeep,
                    )
                }
                importError?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, fontSize = 11.5.sp, color = palette.danger)
                }
            }
        },
        confirmButton = {
            Row {
                if (importedJson != null) {
                    TextButton(onClick = {
                        if (vm.importJson(importedJson!!)) {
                            importedJson = null
                            onDismiss()
                        } else {
                            importError = "文件格式不正确（需要 schemaVersion 2）"
                        }
                    }) { Text("确认导入", color = palette.coralDeep, fontWeight = FontWeight.Bold) }
                } else {
                    TextButton(onClick = {
                        val text = vm.exportJson()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("ONE TIME 备份", text))
                        scope.launch { SnackbarHostState().showSnackbar("已复制 JSON") }
                    }) { Text("复制 JSON", color = palette.coralDeep) }
                    TextButton(onClick = {
                        val text = vm.exportJson()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, vm.exportFileName())
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "保存 / 分享备份"))
                    }) { Text("保存 / 分享备份", color = palette.coralDeep, fontWeight = FontWeight.Bold) }
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { picker.launch(arrayOf("application/json", "*/*")) }) {
                    Text("导入备份", color = palette.muted)
                }
                TextButton(onClick = onDismiss) { Text("关闭", color = palette.muted) }
            }
        },
    )
}

@Composable
private fun BackupLine(label: String, value: String) {
    val palette = Paper.current
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, fontSize = 12.5.sp, color = palette.muted)
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 12.5.sp, color = palette.ink, fontWeight = FontWeight.Medium)
    }
}
