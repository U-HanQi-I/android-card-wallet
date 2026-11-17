package com.clowly.cardwallet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import java.io.File

internal val Paper = Color(0xFFF3F2F7)
internal val Ink = Color(0xFF171C26)
internal val Quiet = Color(0xFF5B6270)
internal val Line = Color(0xFFE8E8F0)
internal val CardWhite = Color(0xFFFCFCFF)
private val Mint = Color(0xFFAFE9DC)
private val Red = Color(0xFFAA3240)
private val PageWidth = 560.dp
private val SidePadding = 22.dp

@Composable
internal fun HomeScreen(
    modifier: Modifier, cards: List<SavedCard>, listState: LazyListState,
    imageFile: (SavedCard) -> File, onOpen: (SavedCard) -> Unit, onAdd: () -> Unit,
) {
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = PageWidth).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = SidePadding, end = SidePadding, top = 17.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("我的收藏", color = Quiet, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
                    Text("卡包", color = Ink, fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold)
                }
                FilledIconButton(
                    onClick = onAdd,
                    modifier = Modifier.size(54.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Ink, contentColor = CardWhite),
                ) { Icon(Icons.Default.Add, contentDescription = "添加卡片", modifier = Modifier.size(28.dp)) }
            }
            if (cards.isEmpty()) {
                EmptyWallet(Modifier.weight(1f).fillMaxWidth(), onAdd)
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(start = SidePadding, end = SidePadding, bottom = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("全部卡片", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("${cards.size} 张", color = Quiet, fontSize = 13.sp)
                }
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                    val cardHeight = (maxWidth - SidePadding * 2) / 1.586f
                    val reveal = (cardHeight * 0.36f).coerceIn(68.dp, 100.dp).coerceAtMost(cardHeight)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(start = SidePadding, end = SidePadding, top = 5.dp, bottom = 26.dp),
                        verticalArrangement = Arrangement.spacedBy(reveal - cardHeight),
                    ) {
                        itemsIndexed(cards, key = { _, card -> card.id }) { index, card ->
                            val name = card.note.lineSequence().firstOrNull { it.isNotBlank() }?.take(30)
                            val label = "第 ${index + 1} 张卡片" + if (name == null) "" else "，$name"
                            Box(
                                Modifier.fillMaxWidth().height(cardHeight).zIndex(index.toFloat()),
                            ) {
                                CardImage(imageFile(card), ContentScale.Crop, Modifier.fillMaxSize(), placeholderText = index == cards.lastIndex)
                                Box(
                                    Modifier.fillMaxWidth().height(if (index == cards.lastIndex) cardHeight else reveal)
                                        .clip(RoundedCornerShape(18.dp))
                                        .semantics { contentDescription = label }
                                        .clickable(role = Role.Button, onClickLabel = "查看卡片") { onOpen(card) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyWallet(modifier: Modifier, onAdd: () -> Unit) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = SidePadding, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.width(224.dp).height(186.dp), contentAlignment = Alignment.Center) {
            DecorativeCard(Color(0xFFA6B3F6), Color(0xFFD5DCFF), Modifier.offset(x = 14.dp, y = (-34).dp).rotate(13f))
            DecorativeCard(Color(0xFF429889), Mint, Modifier.offset(x = (-16).dp, y = (-14).dp).rotate(-12f))
            DecorativeCard(Color(0xFF212C46), Color(0xFF4C5879), Modifier.offset(x = 4.dp, y = 22.dp).rotate(3f))
        }
        Spacer(Modifier.height(22.dp))
        Text("还没有卡片", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("从相册选一张卡面，把它和备注一起收进卡包。", color = Quiet, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(22.dp))
        Button(onClick = onAdd, modifier = Modifier.heightIn(min = 48.dp)) { Text("添加第一张卡片") }
    }
}

@Composable
private fun DecorativeCard(start: Color, end: Color, modifier: Modifier) {
    val shape = RoundedCornerShape(17.dp)
    Box(
        modifier.size(174.dp, 108.dp).shadow(6.dp, shape).clip(shape)
            .background(Brush.linearGradient(listOf(start, end))).border(1.dp, Color.White.copy(alpha = .24f), shape),
    ) {
        Box(Modifier.align(Alignment.TopEnd).offset(x = 33.dp, y = (-22).dp).size(112.dp)
            .border(1.dp, Color.White.copy(alpha = .23f), CircleShape))
        Box(Modifier.align(Alignment.TopEnd).offset(x = 18.dp, y = (-9).dp).size(112.dp)
            .border(1.dp, Color.White.copy(alpha = .15f), CircleShape))
        Box(Modifier.padding(16.dp).size(23.dp, 17.dp).clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = .42f)))
        Row(Modifier.align(Alignment.BottomStart).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(4) { Box(Modifier.size(4.dp).background(Color.White.copy(alpha = .7f), CircleShape)) }
        }
    }
}

@Composable
internal fun DetailScreen(
    modifier: Modifier, model: File, note: String,
    onBack: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, busy: Boolean,
) {
    Page(modifier, "卡片详情", onBack, action = { TextButton(onClick = onEdit, enabled = !busy) { Text("编辑") } }) {
        CardImage(model, ContentScale.Fit, Modifier.fillMaxWidth().heightIn(max = 440.dp).aspectRatio(1.35f), "完整卡面")
        Surface(shape = RoundedCornerShape(20.dp), color = CardWhite, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("备注", fontWeight = FontWeight.SemiBold)
                Text(
                    if (note.isBlank()) "暂无备注" else note,
                    color = if (note.isBlank()) Quiet else Ink,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        TextButton(
            onClick = onDelete, enabled = !busy,
            colors = ButtonDefaults.textButtonColors(contentColor = Red),
        ) { Text(if (busy) "处理中…" else "删除卡片") }
    }
}

@Composable
internal fun EditScreen(
    modifier: Modifier, existing: Boolean, model: Any?, note: String, busy: Boolean,
    onNote: (String) -> Unit, onBack: () -> Unit, onChoose: () -> Unit, onSave: () -> Unit,
) {
    Page(
        modifier.imePadding(),
        if (existing) "编辑卡片" else "添加卡片", onBack, close = true,
        action = {
            TextButton(onClick = onSave, enabled = model != null && !busy) {
                Text(if (busy) "保存中…" else "保存", fontWeight = FontWeight.Bold)
            }
        },
    ) {
        if (model != null) {
            CardImage(model, ContentScale.Fit, Modifier.fillMaxWidth().heightIn(max = 350.dp).aspectRatio(1.586f), "卡面预览")
        }
        OutlinedButton(onClick = onChoose, enabled = !busy) {
            Text(if (model == null) "选择卡面" else "更换卡面")
        }
        OutlinedTextField(
            value = note,
            onValueChange = onNote,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("备注（选填）") },
            placeholder = { Text("写点你想记住的信息…") },
            shape = RoundedCornerShape(18.dp),
            minLines = 3,
            maxLines = 7,
            enabled = !busy,
            supportingText = if (note.length > 1800) {
                { Text("${note.length}/$MAX_NOTE_LENGTH") }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardWhite,
                unfocusedContainerColor = CardWhite,
            ),
        )
    }
}

@Composable
private fun Page(
    modifier: Modifier, title: String, back: () -> Unit, close: Boolean = false,
    action: @Composable () -> Unit, body: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = PageWidth).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = back) {
                    Icon(if (close) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, if (close) "关闭编辑" else "返回")
                }
                Text(title, Modifier.weight(1f).padding(start = 5.dp), fontSize = 21.sp, fontWeight = FontWeight.Bold)
                action()
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = SidePadding).padding(top = 12.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
                content = body,
            )
        }
    }
}

@Composable
internal fun LoadScreen(modifier: Modifier, failed: Boolean, retry: () -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        if (!failed) {
            CircularProgressIndicator(Modifier.size(28.dp))
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("无法读取卡包")
                OutlinedButton(onClick = retry) { Text("重试") }
            }
        }
    }
}

@Composable
internal fun WalletConfirmation(
    title: String, message: String, action: String, onDismiss: () -> Unit, onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = Red)) { Text(action) }
        },
    )
}
