package network.kioq.app

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val Green = Color(0xFF075B3D)
private val GreenDark = Color(0xFF03452F)
private val GreenSoft = Color(0xFFE7F2EB)
private val Red = Color(0xFFD83A45)
private val Page = Color(0xFFF4F7F5)
private val services = listOf("Djezzy", "Ooredoo", "Mobilis", "IDoom Fibre", "IDoom 4G")
private val quickAmounts = listOf("50", "100", "200", "500", "1000", "1500", "2000")
private val menuItems = listOf("الرئيسية", "تعبئة الهاتف", "تعبئة IDoom", "سجل التعبئات", "الزبائن والتجار", "الديون والمدفوعات", "الأرباح والمداخيل", "الصندوق والسحوبات", "الإشعارات", "المحادثات", "التقارير والإحصائيات", "الإعدادات")
private data class Contact(val id:Long=0,val first:String,val last:String,val number:String,val service:String) { val fullName:String get()="$first $last" }
private data class RechargeRecord(val id:String,val name:String,val number:String,val service:String,val kind:String,val amount:Int,val deducted:Int,val status:String,val date:String)
private val demoRecords = listOf(
 RechargeRecord("KQ-1003","محمد بن علي","0770123456","Djezzy","شحن عادي",1000,970,"ناجحة","16/11/2026 · 10:35"),
 RechargeRecord("KQ-1002","أحمد قادري","0550223344","Ooredoo","عرض مفعّل",500,485,"قيد المعالجة","16/11/2026 · 10:20"),
 RechargeRecord("KQ-1001","زبون غير محفوظ","0660111222","Mobilis","شحن عادي",100,0,"فاشلة","16/11/2026 · 09:50")
)
private class ContactDb(ctx:Context):SQLiteOpenHelper(ctx,"kioq_contacts.db",null,1) {
 override fun onCreate(db:SQLiteDatabase) { db.execSQL("CREATE TABLE contacts(id INTEGER PRIMARY KEY AUTOINCREMENT,first TEXT NOT NULL,last TEXT NOT NULL,number TEXT NOT NULL,service TEXT NOT NULL,UNIQUE(number,service))") }
 override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int)=Unit
 fun all():List<Contact> {
  val out=mutableListOf<Contact>()
  readableDatabase.query("contacts",null,null,null,null,null,"first COLLATE NOCASE").use { c ->
   while(c.moveToNext()) out+=Contact(c.getLong(c.getColumnIndexOrThrow("id")),c.getString(c.getColumnIndexOrThrow("first")),c.getString(c.getColumnIndexOrThrow("last")),c.getString(c.getColumnIndexOrThrow("number")),c.getString(c.getColumnIndexOrThrow("service")))
  }; return out
 }
 fun save(x:Contact) {
  val v=ContentValues().apply { put("first",x.first);put("last",x.last);put("number",x.number);put("service",x.service) }
  if(x.id>0) writableDatabase.update("contacts",v,"id=?",arrayOf(x.id.toString()))
  else writableDatabase.insertWithOnConflict("contacts",null,v,SQLiteDatabase.CONFLICT_REPLACE)
 }
 fun delete(id:Long) { writableDatabase.delete("contacts","id=?",arrayOf(id.toString())) }
}
class MainActivity:ComponentActivity() {
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState); setContent {
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
   MaterialTheme(colorScheme=lightColorScheme(primary=Green,onPrimary=Color.White,secondary=Red,onSecondary=Color.White,tertiary=Color(0xFFB58A32),background=Page,surface=Color.White,surfaceVariant=GreenSoft,onSurface=Color(0xFF17251E),onSurfaceVariant=Color(0xFF52645A))) {
    Surface(Modifier.fillMaxSize(),color=Page) { KioqApp() }
   }
  }
 } }
}
@Composable private fun BrandMark(size:Int=52) {
 Surface(shape=RoundedCornerShape((size/3).dp),color=Color.White,modifier=Modifier.size(size.dp)) {
  Box(contentAlignment=Alignment.Center) {
   Text("K",fontWeight=FontWeight.Black,color=GreenDark,style=MaterialTheme.typography.headlineLarge)
   Box(Modifier.align(Alignment.TopEnd).padding(7.dp).size(7.dp).background(Red,CircleShape))
  }
 }
}
private fun networkFor(number:String,selected:String):String {
 val d=number.filter(Char::isDigit)
 return when {
  d.startsWith("07")->"Djezzy"
  d.startsWith("05")->"Ooredoo"
  d.startsWith("06")->"Mobilis"
  d.startsWith("213")->"IDoom 4G"
  selected.startsWith("IDoom")->selected
  else->selected
 }
}
@Composable private fun NetworkMark(service:String,large:Boolean=false) {
 val color=when(service) { "Djezzy"->Color(0xFFD92732); "Ooredoo"->Color(0xFFE51D2A); "Mobilis"->Color(0xFF0876B9); "IDoom Fibre","IDoom 4G"->Green; else->Green }
 Surface(color=color,shape=RoundedCornerShape(12.dp)) {
  Text(when(service) { "Djezzy"->"D"; "Ooredoo"->"O"; "Mobilis"->"M"; "IDoom Fibre"->"F"; "IDoom 4G"->"4G"; else->"K" },
   Modifier.padding(horizontal=if(large)14.dp else 10.dp,vertical=if(large)9.dp else 6.dp),color=Color.White,fontWeight=FontWeight.Black)
 }
}
@Composable private fun KioqApp() {
 val context=LocalContext.current
 val db=remember { ContactDb(context.applicationContext) }
 var contacts by remember { mutableStateOf(db.all()) }
 var page by remember { mutableStateOf("الرئيسية") }
 var drawer by remember { mutableStateOf(false) }
 var service by remember { mutableStateOf("Djezzy") }
 var kind by remember { mutableStateOf("شحن عادي") }
 var number by remember { mutableStateOf("") }
 var amount by remember { mutableStateOf("") }
 var contactQuery by remember { mutableStateOf("") }
 var historyQuery by remember { mutableStateOf("") }
 var statusFilter by remember { mutableStateOf("الكل") }
 var expanded by remember { mutableStateOf<String?>(null) }
 var dialog by remember { mutableStateOf(false) }
 var editing by remember { mutableStateOf<Contact?>(null) }
 var message by remember { mutableStateOf("هذه واجهة تجريبية؛ لم يتم إرسال أي طلب أو خصم رصيد.") }
 val detected=networkFor(number,service)
 val recent=contacts.filter { it.service==service && (number.isBlank() || it.number.contains(number) || it.fullName.contains(number,true)) }.take(4)
 val contactList=contacts.filter { (it.fullName+" "+it.number+" "+it.service).contains(contactQuery.trim(),true) }
 val records=demoRecords.filter { (it.id+it.name+it.number+it.service).contains(historyQuery.trim(),true) && (statusFilter=="الكل" || it.status==statusFilter) }
 ModalNavigationDrawer(
  drawerState=rememberDrawerState(if(drawer) DrawerValue.Open else DrawerValue.Closed),
  gesturesEnabled=true,
  drawerContent={
   ModalDrawerSheet(drawerContainerColor=Color.White) {
    Column(Modifier.fillMaxWidth().background(GreenDark).padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
     BrandMark(60)
     Text("KIOQ.network",color=Color.White,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold)
     Text("منصة خدمات الهاتف والإنترنت",color=Color(0xFFD6E9DE),style=MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(12.dp))
    menuItems.forEachIndexed { index,item ->
     NavigationDrawerItem(
      label={Text(item,fontWeight=if(page==item) FontWeight.Bold else FontWeight.Normal)},
      selected=page==item,
      onClick={page=item;drawer=false},
      icon={Text(listOf("⌂","↗","◎","▤","♙","د","٪","ص","♢","✉","▥","⚙")[index],color=if(page==item) Green else Color.Gray)},
      modifier=Modifier.padding(horizontal=10.dp,vertical=2.dp),
      colors=NavigationDrawerItemDefaults.colors(selectedContainerColor=GreenSoft,selectedTextColor=Green,unselectedTextColor=Color(0xFF34483D))
     )
    }
    Spacer(Modifier.height(12.dp))
    Text("KIOQ.network · الجزائر",Modifier.padding(18.dp),style=MaterialTheme.typography.labelSmall,color=Color.Gray)
   }
  }
 ) {
  Scaffold(containerColor=Page) { padding ->
   Column(Modifier.fillMaxSize().padding(padding)) {
    Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal=16.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
     Surface(color=GreenSoft,shape=RoundedCornerShape(13.dp),modifier=Modifier.size(42.dp).clickable { drawer=true }) { Box(contentAlignment=Alignment.Center) { Text("☰",color=Green,fontWeight=FontWeight.Bold) } }
     Spacer(Modifier.width(12.dp))
     Column(Modifier.weight(1f)) { Text("KIOQ.network",fontWeight=FontWeight.ExtraBold,color=GreenDark); Text(page,style=MaterialTheme.typography.labelSmall,color=Color.Gray) }
     BrandMark(38)
    }
    when(page) {
     "الرئيسية" -> Dashboard(onNavigate={page=it},contacts=contacts.size)
     "تعبئة الهاتف","تعبئة IDoom" -> RechargePage(page,service,{service=it},kind,{kind=it},number,{number=it.filter(Char::isDigit)},amount,{amount=it.filter(Char::isDigit)},detected,recent,message,{message=it},onSave={editing=null;dialog=true},onConfirm={message="طلب تجريبي فقط: لم يتم إرسال طلب أو خصم رصيد."})
     "سجل التعبئات" -> HistoryPage(historyQuery,{historyQuery=it},statusFilter,{statusFilter=it},records,expanded,{expanded=it})
     "الزبائن والتجار" -> ContactsPage(contactQuery,{contactQuery=it},contactList,onAdd={editing=null;dialog=true},onUse={c->service=c.service;number=c.number;page=if(c.service.startsWith("IDoom")) "تعبئة IDoom" else "تعبئة الهاتف"},onEdit={editing=it;dialog=true},onDelete={db.delete(it.id);contacts=db.all()},onCall={context.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+it)))})
     "الديون والمدفوعات" -> InfoPage("الديون والمدفوعات","متابعة ديون التجار، الدفعات الجزئية، والمبالغ المتبقية.","سيتم احتساب الجزء المحصّل من الهامش بنسبة المبلغ المسدد إلى الدين المرتبط به.")
     "الأرباح والمداخيل" -> InfoPage("الأرباح والمداخيل","إجمالي الأرباح المسجلة، المحصّلة، وغير المحصّلة.","الأرباح تقارير محاسبية وليست محفظة رصيد مستقلة.")
     "الصندوق والسحوبات" -> InfoPage("الصندوق والسحوبات","متابعة المبالغ المحصّلة والسحوبات إلى الصندوق الفعلي.","السحب تحويل محاسبي للصندوق ولا يُسجّل كربح جديد.")
     "الإشعارات" -> InfoPage("الإشعارات","تنبيهات الطلبات وحالات التعبئة والمدفوعات.","لا توجد إشعارات فعلية في النسخة التجريبية.")
     "المحادثات" -> InfoPage("المحادثات","مساحة تواصل بين المالك والتجار.","المحادثات تحتاج إلى الخادم المركزي لتعمل.")
     "التقارير والإحصائيات" -> InfoPage("التقارير والإحصائيات","ملخص العمليات والمبيعات والديون والأرباح.","ستظهر البيانات الحقيقية بعد ربط الخادم.")
     else -> InfoPage("الإعدادات","إعدادات الحساب والأمان والإشعارات والنسخ الاحتياطي.","هذه الخيارات واجهة أولية، ولم يتم تفعيل الحسابات أو المزامنة بعد.")
    }
   }
  }
 }
 if(dialog) {
  var first by remember(editing?.id) { mutableStateOf(editing?.first.orEmpty()) }
  var last by remember(editing?.id) { mutableStateOf(editing?.last.orEmpty()) }
  var num by remember(editing?.id) { mutableStateOf(editing?.number ?: number) }
  var chosen by remember(editing?.id) { mutableStateOf(editing?.service ?: service) }
  AlertDialog(onDismissRequest={dialog=false},title={Text(if(editing==null)"إضافة جهة اتصال" else "تعديل جهة اتصال")},
   text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
    OutlinedTextField(first,{first=it},label={Text("الاسم")},singleLine=true)
    OutlinedTextField(last,{last=it},label={Text("اللقب")},singleLine=true)
    OutlinedTextField(num,{num=it.filter(Char::isDigit)},label={Text("الرقم دون بادئة مسبقة")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone))
    Text("الشبكة",fontWeight=FontWeight.SemiBold)
    services.chunked(2).forEach { group->Row(horizontalArrangement=Arrangement.spacedBy(3.dp)) { group.forEach { s->FilterChip(selected=chosen==s,onClick={chosen=s},label={Text(s)}) } } }
   }},
   confirmButton={TextButton(onClick={if(first.isNotBlank()&&last.isNotBlank()&&num.isNotBlank()){db.save(Contact(editing?.id?:0,first.trim(),last.trim(),num,chosen));contacts=db.all();dialog=false}}){Text("حفظ")}},
   dismissButton={TextButton(onClick={dialog=false}){Text("إلغاء")}}
  )
 }
}
@Composable private fun Dashboard(onNavigate:(String)->Unit,contacts:Int) {
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
  item {
   Card(colors=CardDefaults.cardColors(containerColor=GreenDark),shape=RoundedCornerShape(26.dp)) {
    Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
     Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(13.dp)) { BrandMark(58);Column { Text("KIOQ.network",color=Color.White,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);Text("منصة خدمات الهاتف والإنترنت",color=Color(0xFFD6E9DE)) } }
     HorizontalDivider(color=Color(0x5579A992))
     Text("أهلاً بك",color=Color(0xFFB9D8C7))
     Text("كل خدماتك في مكان واحد",color=Color.White,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
     Text("حلول تعبئة الهاتف والإنترنت في الجزائر",color=Color(0xFFD6E9DE),style=MaterialTheme.typography.bodySmall)
    }
   }
  }
  item { Text("الخدمات",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold) }
  item {
   Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
    services.chunked(2).forEach { group->Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
     group.forEach { s->Card(Modifier.weight(1f).clickable { onNavigate(if(s.startsWith("IDoom"))"تعبئة IDoom" else "تعبئة الهاتف") },shape=RoundedCornerShape(18.dp)) {
      Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) { NetworkMark(s);Text(s,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.bodyMedium) }
     } }
     if(group.size==1) Spacer(Modifier.weight(1f))
    } }
   }
  }
  item { Text("نظرة سريعة",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold) }
  item { Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) { StatCard("جهات الاتصال",contacts.toString(),Modifier.weight(1f));StatCard("عمليات تجريبية",demoRecords.size.toString(),Modifier.weight(1f)) } }
  item { Text("الوصول السريع",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold) }
  item { Column(verticalArrangement=Arrangement.spacedBy(7.dp)) { listOf("تعبئة الهاتف","تعبئة IDoom","سجل التعبئات","الزبائن والتجار").forEach { label->Card(Modifier.fillMaxWidth().clickable { onNavigate(label) }) { Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically) { Text(label,Modifier.weight(1f),fontWeight=FontWeight.SemiBold);Text("‹",color=Green,style=MaterialTheme.typography.titleLarge) } } } } }
  item { Text("النسخة الحالية تجريبية؛ لا توجد تعبئات فعلية أو أرصدة متصلة بخادم بعد.",style=MaterialTheme.typography.bodySmall,color=Color.Gray) }
 }
}
@Composable private fun StatCard(title:String,value:String,modifier:Modifier=Modifier) { Card(modifier) { Column(Modifier.padding(14.dp)) { Text(title,style=MaterialTheme.typography.bodySmall,color=Color.Gray);Text(value,style=MaterialTheme.typography.headlineMedium,color=Green,fontWeight=FontWeight.Bold) } } }
@Composable private fun RechargePage(page:String,service:String,onService:(String)->Unit,kind:String,onKind:(String)->Unit,number:String,onNumber:(String)->Unit,amount:String,onAmount:(String)->Unit,detected:String,recent:List<Contact>,message:String,onMessage:(String)->Unit,onSave:()->Unit,onConfirm:()->Unit) {
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item { Card { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Text(if(page=="تعبئة IDoom")"اختر خدمة الإنترنت" else "اختر شبكة الهاتف",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
   val choices=if(page=="تعبئة IDoom") services.filter { it.startsWith("IDoom") } else services.filter { !it.startsWith("IDoom") }
   choices.chunked(3).forEach { group->Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) { group.forEach { s->FilterChip(selected=service==s,onClick={onService(s);onNumber("")},label={Text(s)}) } } }
   Text("نوع التعبئة",fontWeight=FontWeight.Bold)
   Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) { listOf("شحن عادي","العروض المفعّلة").forEach { k->FilterChip(selected=kind==k,onClick={onKind(k)},label={Text(k)}) } }
   Text(if(service.startsWith("IDoom"))"رقم اشتراك IDoom" else "رقم الهاتف",fontWeight=FontWeight.Bold)
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
    OutlinedTextField(number,onNumber,Modifier.weight(1f),singleLine=true,label={Text("اكتب الرقم هنا")},placeholder={Text("أدخل الرقم كاملاً")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone))
    NetworkMark(detected,true)
   }
   if(number.isNotBlank()) Text("الشبكة المتعرّف عليها: $detected",style=MaterialTheme.typography.bodySmall,color=Green)
   if(recent.isNotEmpty()) { Text("أرقام سبق التعامل معها",fontWeight=FontWeight.SemiBold);recent.forEach { c->TextButton(onClick={onNumber(c.number)}) { Text(c.fullName+" · "+c.number) } } }
   if(kind=="شحن عادي") {
    Text("مبلغ الشحن بالدينار",fontWeight=FontWeight.Bold)
    OutlinedTextField(amount,onAmount,Modifier.fillMaxWidth(),singleLine=true,label={Text("أدخل المبلغ")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
    quickAmounts.chunked(4).forEach { group->Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) { group.forEach { a->FilterChip(selected=amount==a,onClick={onAmount(a)},label={Text(a)}) } } }
   } else Text("العروض المتاحة ستظهر عند تفعيلها ببيانات مؤكدة.")
   OutlinedButton(onClick=onSave,modifier=Modifier.fillMaxWidth()) { Text("حفظ الرقم كجهة اتصال") }
   Button(onClick=onConfirm,modifier=Modifier.fillMaxWidth()) { Text("متابعة") }
   Text(message,style=MaterialTheme.typography.bodySmall,color=Color.Gray)
  } } }
 }
}
@Composable private fun HistoryPage(query:String,onQuery:(String)->Unit,status:String,onStatus:(String)->Unit,records:List<RechargeRecord>,expanded:String?,onExpanded:(String?)->Unit) {
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
  item { Text("سجل التعبئة الكلي",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("بيانات توضيحية ثابتة وليست سجلاً حقيقياً.",style=MaterialTheme.typography.bodySmall,color=Color.Gray) }
  item { OutlinedTextField(query,onQuery,Modifier.fillMaxWidth(),singleLine=true,label={Text("بحث بالعملية أو الاسم أو الرقم أو الشبكة")}) }
  item { Row(horizontalArrangement=Arrangement.spacedBy(3.dp)) { listOf("الكل","ناجحة","قيد المعالجة","فاشلة").forEach { s->FilterChip(selected=status==s,onClick={onStatus(s)},label={Text(s)}) } } }
  item { Text("عدد النتائج: "+records.size,style=MaterialTheme.typography.labelMedium) }
  items(records,key={it.id}) { r->Card { Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(r.name,fontWeight=FontWeight.Bold);Text(r.status,color=if(r.status=="فاشلة")Red else Green) }
   Text(r.number+" · "+r.service);Text(r.kind+" · "+r.amount+" دج · "+r.date,style=MaterialTheme.typography.bodySmall)
   TextButton(onClick={onExpanded(if(expanded==r.id)null else r.id)}) { Text(if(expanded==r.id)"إخفاء التفاصيل" else "تفاصيل العملية") }
   if(expanded==r.id) { HorizontalDivider();Text("رقم العملية: "+r.id);Text("قيمة التعبئة: "+r.amount+" دج");Text("المبلغ المخصوم من رصيدك: "+r.deducted+" دج") }
  } } }
 }
}
@Composable private fun ContactsPage(query:String,onQuery:(String)->Unit,contacts:List<Contact>,onAdd:()->Unit,onUse:(Contact)->Unit,onEdit:(Contact)->Unit,onDelete:(Contact)->Unit,onCall:(String)->Unit) {
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
  item { Text("الزبائن والتجار",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("دفتر جهات الاتصال المحفوظ محلياً على هذا الجهاز.",style=MaterialTheme.typography.bodySmall,color=Color.Gray) }
  item { OutlinedTextField(query,onQuery,Modifier.fillMaxWidth(),singleLine=true,label={Text("بحث بالاسم أو الرقم أو الخدمة")});Button(onClick=onAdd,modifier=Modifier.fillMaxWidth()) { Text("إضافة جهة اتصال") } }
  if(contacts.isEmpty()) item { Text("لا توجد جهات اتصال مطابقة.") }
  items(contacts,key={it.id}) { c->Card { Column(Modifier.padding(13.dp)) {
   Text(c.fullName,fontWeight=FontWeight.Bold);Text(c.number+" · "+c.service,style=MaterialTheme.typography.bodySmall)
   Row(horizontalArrangement=Arrangement.spacedBy(2.dp)) { TextButton(onClick={onUse(c)}){Text("استخدام")};TextButton(onClick={onEdit(c)}){Text("تعديل")};TextButton(onClick={onDelete(c)}){Text("حذف")};TextButton(onClick={onCall(c.number)}){Text("اتصال")} }
  } } }
 }
}
@Composable private fun InfoPage(title:String,description:String,note:String) {
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(13.dp)) {
  item { Card(shape=RoundedCornerShape(22.dp)) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=GreenDark)
   Text(description,style=MaterialTheme.typography.bodyLarge)
   HorizontalDivider()
   Text(note,style=MaterialTheme.typography.bodyMedium,color=Color(0xFF52645A))
  } } }
  item { Card(colors=CardDefaults.cardColors(containerColor=GreenSoft)) { Text("سيتم تفعيل البيانات والعمليات الفعلية بعد تجهيز الخادم وربط الحسابات والصلاحيات.",Modifier.padding(15.dp),style=MaterialTheme.typography.bodySmall,color=GreenDark) } }
 }
}
