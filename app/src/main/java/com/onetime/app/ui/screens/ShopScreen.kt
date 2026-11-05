package com.onetime.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetime.app.data.AppData
import com.onetime.app.data.Redemption
import com.onetime.app.data.ShopItem
import com.onetime.app.data.coinText
import com.onetime.app.data.formatStamp
import com.onetime.app.ui.LiveTimer
import com.onetime.app.ui.chrome.RowIcon
import com.onetime.app.ui.chrome.SecondaryHeader
import com.onetime.app.ui.theme.CoinIcon
import com.onetime.app.ui.theme.InsetBox
import com.onetime.app.ui.theme.KeyCap
import com.onetime.app.ui.theme.Paper
import com.onetime.app.ui.theme.SoftCard

@Composable
fun ShopScreen(
    data: AppData,
    timer: LiveTimer,
    onBack: () -> Unit,
    onRedeem: (ShopItem) -> Unit,
    onAddItem: (String, Double) -> Unit,
    onDeleteItem: (String) -> Unit,
) {
    val palette = Paper.current
    var name by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var pending by remember { mutableStateOf<ShopItem?>(null) }
    var twoCol by rememberSaveable { mutableStateOf(false) }

    val pauseTickets = data.shopItems.firstOrNull { it.voucher == "pause" }?.stock ?: 0
    val stopTickets = data.shopItems.firstOrNull { it.voucher == "stop" }?.stock ?: 0
    val history = data.redemptions.take(4)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp),
    ) {
        item {
            SecondaryHeader(
                title = "金币商城",
                onBack = onBack,
                timerChip = timer,
                extra = {
                    RowIcon(
                        icon = if (twoCol) Icons.Filled.List else Icons.Filled.List,
                        description = "切换布局",
                        onClick = { twoCol = !twoCol },
                    )
                },
            )
        }

        item {
            SoftCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = 3.dp) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InsetBox(Modifier.weight(1.3f).heightIn(min = 40.dp), shape = RoundedCornerShape(11.dp)) {
                        Box(Modifier.padding(horizontal = 10.dp, vertical = 11.dp)) {
                            if (name.isEmpty()) Text("商品名称", fontSize = 12.5.sp, color = palette.muted)
                            BasicTextField(
                                value = name,
                                onValueChange = { if (it.length <= 40) name = it },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 13.sp, color = palette.ink),
                                cursorBrush = SolidColor(palette.coral),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    InsetBox(Modifier.weight(0.7f).heightIn(min = 40.dp), shape = RoundedCornerShape(11.dp)) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CoinIcon(size = 11.dp)
                            Spacer(Modifier.width(4.dp))
                            Box(Modifier.weight(1f)) {
                                if (price.isEmpty()) Text("金币", fontSize = 12.sp, color = palette.muted)
                                BasicTextField(
                                    value = price,
                                    onValueChange = { input -> price = input.filter { it.isDigit() }.take(4) },
                                    singleLine = true,
                                    textStyle = TextStyle(fontSize = 13.sp, color = palette.ink),
                                    cursorBrush = SolidColor(palette.coral),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    KeyCap(
                        onClick = {
                            val p = price.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank() && p > 0) {
                                onAddItem(name, p)
                                name = ""
                                price = ""
                            }
                        },
                        color = palette.goldWash,
                        shape = RoundedCornerShape(11.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    ) {
                        Text("添加商品", fontSize = 12.sp, color = palette.goldDeep, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (twoCol) {
            items(data.shopItems.chunked(2), key = { it.joinToString("-") { i -> i.id } }) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { item ->
                        Box(Modifier.weight(1f)) {
                            CompactProductCard(
                                item = item,
                                coins = data.coins,
                                onClick = { pending = item },
                                onDelete = if (item.custom) ({ onDeleteItem(item.id) }) else null,
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else {
            items(data.shopItems, key = { it.id }) { item ->
                ProductCard(
                    item = item,
                    coins = data.coins,
                    onRedeem = { pending = item },
                    onDelete = if (item.custom) ({ onDeleteItem(item.id) }) else null,
                )
            }
        }

        if (history.isNotEmpty()) {
            item {
                SoftCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = 3.dp) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("兑换历史", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = palette.ink)
                            Spacer(Modifier.weight(1f))
                            Text("最近 ${history.size} 条", fontSize = 10.5.sp, color = palette.muted)
                        }
                        Spacer(Modifier.height(8.dp))
                        history.forEach { HistoryRow(it) }
                    }
                }
            }
        }
    }

    pending?.let { item ->
        RedeemDialog(
            item = item,
            coins = data.coins,
            onDismiss = { pending = null },
            onConfirm = {
                onRedeem(item)
                pending = null
            },
        )
    }
}

@Composable
private fun VoucherChip(label: String, count: Int, modifier: Modifier) {
    val palette = Paper.current
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(50))
            .background(palette.card)
            .border(1.dp, palette.line, RoundedCornerShape(50))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(label, fontSize = 11.5.sp, color = palette.inkSoft)
        Text("× $count", fontSize = 11.5.sp, color = palette.ink, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProductCard(
    item: ShopItem,
    coins: Double,
    onRedeem: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val palette = Paper.current
    val affordable = coins >= item.price
    SoftCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), elevation = 4.dp) {
        Column {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 2.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.inset)
                        .border(1.dp, palette.line, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(item.emoji, fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.name, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = palette.ink)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CoinIcon(size = 12.dp)
                        Spacer(Modifier.width(3.dp))
                        Text(item.price.coinText(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = palette.goldDeep)
                    }
                    if (item.stock > 0) {
                        Text("持有 ${item.stock}", fontSize = 10.5.sp, color = palette.goldDeep)
                    }
                }
                if (onDelete != null) {
                    Box(
                        Modifier.size(26.dp).clickable(onClick = onDelete),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Filled.Delete,
                            contentDescription = "删除商品",
                            tint = palette.muted.copy(alpha = 0.7f),
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clickable(enabled = affordable, onClick = onRedeem),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (affordable) "兑换" else "金币不足",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (affordable) palette.coralDeep else palette.muted,
                )
            }
        }
    }
}

@Composable
private fun CompactProductCard(
    item: ShopItem,
    coins: Double,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val palette = Paper.current
    SoftCard(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp,
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(item.emoji, fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = palette.ink, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(size = 11.dp)
                Spacer(Modifier.width(3.dp))
                Text(item.price.coinText(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = palette.goldDeep)
                Spacer(Modifier.weight(1f))
                if (item.stock > 0) Text("×${item.stock}", fontSize = 10.5.sp, color = palette.muted)
            }
            if (coins < item.price) {
                Text("金币不足", fontSize = 10.sp, color = palette.muted)
            }
        }
    }
}

@Composable
private fun RedeemDialog(
    item: ShopItem,
    coins: Double,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val palette = Paper.current
    val affordable = coins >= item.price
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.emoji, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text("兑换「${item.name}」", fontSize = 16.sp, color = palette.ink, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                DialogLine("单价", "${item.price.coinText()} 金币")
                DialogLine("当前余额", "${coins.coinText()} 金币")
                DialogLine("本次支出", "${item.price.coinText()} 金币")
                DialogLine("兑换后余额", "${(coins - item.price).coerceAtLeast(0.0).coinText()} 金币")
                if (!affordable) {
                    Spacer(Modifier.height(8.dp))
                    Text("金币不足，去专注赚一点吧", fontSize = 12.sp, color = palette.coralDeep)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = affordable) {
                Text("确认兑换", color = if (affordable) palette.coralDeep else palette.muted, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = palette.muted) } },
    )
}

@Composable
private fun DialogLine(label: String, value: String) {
    val palette = Paper.current
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, fontSize = 12.5.sp, color = palette.muted)
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 12.5.sp, color = palette.ink, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HistoryRow(record: Redemption) {
    val palette = Paper.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(record.emoji, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(record.name, fontSize = 12.5.sp, color = palette.ink)
            Text(formatStamp(record.at), fontSize = 9.5.sp, color = palette.muted)
        }
        Text("-${record.price.coinText()}", fontSize = 11.5.sp, color = palette.coralDeep, fontWeight = FontWeight.SemiBold)
    }
}
