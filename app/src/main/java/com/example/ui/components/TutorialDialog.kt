package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.TextSecondary

@Composable
fun TutorialDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Icon and Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFF9800).copy(alpha = 0.15f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier
                                .padding(10.dp)
                                .size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "آموزش استفاده از برنامه",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "راهنمای قدم به قدم دانلود سریع ویدیوها",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Scrollable Steps
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TutorialStepItem(
                        stepNumber = "۱",
                        title = "کپی یا اشتراک‌گذاری مستقیم لینک",
                        description = "پست یا ریلز مورد نظر را در اینستاگرام باز کرده، روی آیکون اشتراک‌گذاری (Share) بزنید. می‌توانید «کپی لینک» را بزنید یا مستقیماً این اپلیکیشن را انتخاب کنید.",
                        icon = Icons.Default.Share
                    )

                    TutorialStepItem(
                        stepNumber = "۲",
                        title = "جای‌گذاری لینک در برنامه",
                        description = "در صورت کپی کردن لینک، وارد برنامه شوید و دکمه «جای‌گذاری» را بزنید. (در صورت اشتراک‌گذاری مستقیم، لینک خودکار وارد می‌شود).",
                        icon = Icons.Default.ContentPaste
                    )

                    TutorialStepItem(
                        stepNumber = "۳",
                        title = "بررسی و استخراج ویدیو",
                        description = "روی دکمه «بررسی لینک» بزنید تا اطلاعات، تصویر کاور و گزینه‌های کیفیت ویدیو دریافت شوند.",
                        icon = Icons.Default.Search
                    )

                    TutorialStepItem(
                        stepNumber = "۴",
                        title = "دانلود و ذخیره مستقیم در گالری",
                        description = "کیفیت مورد نظر را انتخاب کرده و دکمه دانلود را بزنید. ویدیو به صورت خودکار در گالری گوشی ذخیره می‌گردد.",
                        icon = Icons.Default.FileDownload
                    )

                    TutorialStepItem(
                        stepNumber = "۵",
                        title = "مدیریت گالری، پوشه‌ها و علاقه‌مندی‌ها",
                        description = "از تب «گالری» پایین صفحه می‌توانید ویدیوها را مشاهده کنید، پوشه‌های دلخواه بسازید و فایل‌ها را علامت‌گذاری یا منتقل کنید.",
                        icon = Icons.Default.FolderSpecial
                    )

                    // Tip Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFFF8E1),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE082)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = Color(0xFFFF8F00),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "میانبر سریع دانلود:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5D4037)
                                )
                            }
                            Text(
                                text = "با اشتراک‌گذاری مستقیم هر پست یا ریلز از اینستاگرام با این برنامه (منوی Share)، لینک به صورت خودکار پردازش شده و نیازی به کپی-پیست دستی نیست!",
                                fontSize = 11.sp,
                                color = Color(0xFF5D4037),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Confirm Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text(
                        text = "متوجه شدم",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialStepItem(
    stepNumber: String,
    title: String,
    description: String,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFF9800)
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
