package network.kioq.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val services = listOf("Djezzy", "Ooredoo", "Mobilis", "IDoom Fibre", "IDoom 4G")
private val quickAmounts = listOf("50", "100", "200", "500", "1000", "1500", "2000")
private data class RechargeRecord(val customer: String, val number: String, val service: String, val amount: String, val status: String)
private val sampleRecords = listOf(
    RechargeRecord("محمد بن علي", "0770 ••• ••56", "Djezzy", "1000 دج", "ناجحة"),
    RechargeRecord("أحمد قادري", "0550 ••• ••44", "Ooredoo", "500 دج", "قيد المعالجة"),
    RechargeRecord("زبون غير محفوظ", "0660 ••• ••22", "Mobilis", "100 دج", "فاشلة")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme { Surface(Modifier.fillMaxSize()) { RechargeScreen() } }
            }
        }
    }
}

@Composable
private fun RechargeScreen() {
    var selectedService by remember { mutableStateOf("Djezzy") }
    var rechargeKind by remember { mutableStateOf("شحن عادي") }
    var phone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("هذه واجهة أولية؛ لا يتم إرسال أي طلب تعبئة.") }
    val isIdoom = selectedService.startsWith("IDoom")
    val prefix = when (selectedService) {
        "Djezzy" -> "07"
        "Ooredoo" -> "05"
        "Mobilis" -> "06"
        "IDoom Fibre" -> "0"
        else -> "213"
    }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(Modifier.padding(top = 22.dp)) {
                    Text("KIOQ.network", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("منصة تعبئة الهاتف والإنترنت لتجار التجزئة")
                    Spacer(Modifier.height(8.dp))
                    Text("نسخة تطوير أولية · بيانات تجريبية فقط", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("1. اختر الخدمة", fontWeight = FontWeight.Bold)
                        services.chunked(3).forEach { serviceRow ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                serviceRow.forEach { service ->
                                    FilterChip(
                                        selected = selectedService == service,
                                        onClick = { selectedService = service; phone = ""; amount = "" },
                                        label = { Text(service) }
                                    )
                                }
                            }
                        }
                        Text("2. نوع التعبئة", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("شحن عادي", "العروض المفعّلة").forEach { kind ->
                                FilterChip(selected = rechargeKind == kind, onClick = { rechargeKind = kind }, label = { Text(kind) })
                            }
                        }
                        Text(if (isIdoom) "3. رقم الاشتراك" else "3. رقم الهاتف", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { input ->
                                val digits = input.filter(Char::isDigit)
                                phone = if (digits.isEmpty()) "" else if (digits.startsWith(prefix)) digits else prefix + digits
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("يبدأ بـ " + prefix) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        if (rechargeKind == "شحن عادي") {
                            Text("4. مبلغ الشحن بالدينار", fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = amount,
                                onValueChange = { amount = it.filter(Char::isDigit) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("أدخل المبلغ") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                quickAmounts.take(4).forEach { value ->
                                    FilterChip(selected = amount == value, onClick = { amount = value }, label = { Text(value) })
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                quickAmounts.drop(4).forEach { value ->
                                    FilterChip(selected = amount == value, onClick = { amount = value }, label = { Text(value) })
                                }
                            }
                        } else {
                            Text("ستظهر العروض الفعلية بعد ربط OneClick والتحقق من البيانات المتاحة.")
                        }
                        Button(
                            onClick = { message = "نموذج فقط: لم يُرسل طلب ولم يُخصم رصيد. يلزم ربط الخادم والتحقق قبل التعبئة." },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("تأكيد تجريبي") }
                        Text(message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Text("سجل التعبئة الكلي", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("سجل توضيحي فقط — سيُربط لاحقاً بحساب التاجر والخادم المركزي.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            items(sampleRecords) { record ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(record.customer, fontWeight = FontWeight.Bold)
                            Text(record.status, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(record.number + " · " + record.service)
                        Text(record.amount + " · مثال توضيحي", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                TextButton(onClick = { message = "دفتر جهات الاتصال سيُضاف في المرحلة التالية." }, modifier = Modifier.fillMaxWidth()) {
                    Text("جهات الاتصال — قيد التطوير")
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}
