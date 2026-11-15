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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val KioqGreen=Color(0xFF075B3D)
private val KioqGreenDark=Color(0xFF03452F)
private val KioqRed=Color(0xFFD83A45)
private val KioqBackground=Color(0xFFF5F8F6)
private val services=listOf("Djezzy","Ooredoo","Mobilis","IDoom Fibre","IDoom 4G")
private val quickAmounts=listOf("50","100","200","500","1000","1500","2000")
private data class Contact(val id:Long=0,val first:String,val last:String,val number:String,val service:String) { val fullName:String get()="$first $last" }
private data class RechargeRecord(val id:String,val name:String,val number:String,val service:String,val kind:String,val amount:Int,val deducted:Int,val status:String,val date:String)
private val demoRecords=listOf(
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
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState);setContent {
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
   MaterialTheme(colorScheme=lightColorScheme(primary=KioqGreen,onPrimary=Color.White,secondary=KioqRed,onSecondary=Color.White,tertiary=Color(0xFFB58A32),background=KioqBackground,surface=Color.White,surfaceVariant=Color(0xFFEAF1ED),onSurface=Color(0xFF17251E),onSurfaceVariant=Color(0xFF52645A))) {
    Surface(Modifier.fillMaxSize(),color=KioqBackground) { RechargeScreen() }
   }
  }
 } }
}
@Composable private fun BrandMark() {
 Surface(shape=RoundedCornerShape(19.dp),color=Color.White,modifier=Modifier.size(58.dp)) {
  Box(contentAlignment=Alignment.Center) {
   Text("K",fontWeight=FontWeight.Black,color=KioqGreenDark,style=MaterialTheme.typography.headlineLarge)
   Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(8.dp).background(KioqRed,androidx.compose.foundation.shape.CircleShape))
  }
 }
}
private fun normalize(raw:String,service:String):String {
 val digits=raw.filter(Char::isDigit)
 val p=when(service) { "Djezzy"->"07";"Ooredoo"->"05";"Mobilis"->"06";"IDoom Fibre"->"0";else->"213" }
 if(digits.isEmpty()) return ""
 return if(digits.startsWith(p)) digits else p+digits
}
@Composable private fun RechargeScreen() {
 val context=LocalContext.current
 val db=remember { ContactDb(context.applicationContext) }
 var contacts by remember { mutableStateOf(db.all()) }
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
 var message by remember { mutableStateOf("نسخة تجريبية: لا يتم إرسال طلب أو خصم رصيد.") }
 val prefix=when(service) { "Djezzy"->"07";"Ooredoo"->"05";"Mobilis"->"06";"IDoom Fibre"->"0";else->"213" }
 val recent=contacts.filter { it.service==service && (number.isBlank() || it.number.contains(number) || it.fullName.contains(number,true)) }.take(4)
 val contactList=contacts.filter { (it.fullName+" "+it.number+" "+it.service).contains(contactQuery.trim(),true) }
 val records=demoRecords.filter { (it.id+it.name+it.number+it.service).contains(historyQuery.trim(),true) && (statusFilter=="الكل" || it.status==statusFilter) }
 Scaffold { pad ->
  LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(13.dp)) {
   item {
    Card(Modifier.fillMaxWidth().padding(top=14.dp),shape=RoundedCornerShape(28.dp),colors=CardDefaults.cardColors(containerColor=KioqGreenDark),elevation=CardDefaults.cardElevation(defaultElevation=4.dp)) {
     Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(17.dp)) {
      Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
       BrandMark()
       Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text("KIOQ.network",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold,color=Color.White)
        Text("منصة خدمات الهاتف والإنترنت",style=MaterialTheme.typography.bodyMedium,color=Color(0xFFE3F2E9))
        Text("حلول تعبئة موثوقة في الجزائر",style=MaterialTheme.typography.labelSmall,color=Color(0xFFB9D8C7))
       }
      }
      HorizontalDivider(color=Color(0x5579A992))
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
       Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
        Text("أهلاً بك",style=MaterialTheme.typography.labelMedium,color=Color(0xFFB9D8C7))
        Text("كل خدماتك في مكان واحد",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=Color.White)
       }
       Surface(shape=RoundedCornerShape(50.dp),color=Color(0xFF1B7654)) {
        Text("KIOQ",Modifier.padding(horizontal=15.dp,vertical=8.dp),color=Color.White,fontWeight=FontWeight.Bold)
       }
      }
     }
    }
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
     Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(11.dp)) {
      Surface(shape=RoundedCornerShape(13.dp),color=Color(0xFFFFEDEF),modifier=Modifier.size(42.dp)) {
       Box(contentAlignment=Alignment.Center) { Text("↗",color=KioqRed,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge) }
      }
      Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
       Text("خدمات التعبئة",fontWeight=FontWeight.Bold,color=KioqGreenDark)
       Text("اختر الشبكة وأدخل الرقم والمبلغ للمتابعة",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
      }
     }
    }
    Text("نسخة أولية · العمليات غير مفعّلة بعد",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
   }
   item {
    Card { Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
     Text("1. اختر الخدمة",fontWeight=FontWeight.Bold)
     services.chunked(3).forEach { group -> Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
      group.forEach { s -> FilterChip(selected=service==s,onClick={service=s;number="";amount=""},label={Text(s)}) }
     } }
     Text("2. نوع التعبئة",fontWeight=FontWeight.Bold)
     Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
      listOf("شحن عادي","العروض المفعّلة").forEach { k -> FilterChip(selected=kind==k,onClick={kind=k},label={Text(k)}) }
     }
     Text(if(service.startsWith("IDoom")) "3. رقم اشتراك IDoom" else "3. رقم الهاتف",fontWeight=FontWeight.Bold)
     OutlinedTextField(number,{number=normalize(it,service)},Modifier.fillMaxWidth(),singleLine=true,label={Text("يبدأ بـ $prefix")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone))
     if(recent.isNotEmpty()) {
      Text("أرقام سبق التعامل معها",fontWeight=FontWeight.SemiBold)
      recent.forEach { c -> Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
       Column(Modifier.weight(1f)) { Text(c.fullName);Text(c.number,style=MaterialTheme.typography.bodySmall) }
       TextButton(onClick={number=c.number}) { Text("اختيار") }
      } }
     }
     if(kind=="شحن عادي") {
      Text("4. مبلغ الشحن بالدينار",fontWeight=FontWeight.Bold)
      OutlinedTextField(amount,{amount=it.filter(Char::isDigit)},Modifier.fillMaxWidth(),singleLine=true,label={Text("أدخل المبلغ")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
      quickAmounts.chunked(4).forEach { group -> Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
       group.forEach { a -> FilterChip(selected=amount==a,onClick={amount=a},label={Text(a)}) }
      } }
     } else Text("العروض المتاحة ستظهر هنا عند تفعيل الخدمة.")
     OutlinedButton(onClick={editing=null;dialog=true},modifier=Modifier.fillMaxWidth()) { Text("حفظ الرقم كجهة اتصال") }
     Button(onClick={message="نموذج فقط: لم يُرسل طلب ولم يُخصم رصيد."},modifier=Modifier.fillMaxWidth()) { Text("تأكيد تجريبي") }
     Text(message,style=MaterialTheme.typography.bodySmall)
    } }
   }
   item {
    Text("دفتر جهات الاتصال",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
    Text("محفوظ محلياً على هذا الجهاز فقط في النسخة الحالية.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.secondary)
    OutlinedTextField(contactQuery,{contactQuery=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("بحث بالاسم أو اللقب أو الرقم أو الخدمة")})
    Button(onClick={editing=null;dialog=true},modifier=Modifier.fillMaxWidth()) { Text("إضافة جهة اتصال") }
   }
   if(contactList.isEmpty()) item { Text("لا توجد جهات اتصال مطابقة.") }
   items(contactList,key={it.id}) { c -> Card { Column(Modifier.fillMaxWidth().padding(12.dp)) {
    Text(c.fullName,fontWeight=FontWeight.Bold);Text(c.number+" · "+c.service)
    Row(horizontalArrangement=Arrangement.spacedBy(0.dp)) {
     TextButton(onClick={service=c.service;number=c.number;message="تم اختيار "+c.fullName+". راجع البيانات."}) { Text("استخدام") }
     TextButton(onClick={editing=c;dialog=true}) { Text("تعديل") }
     TextButton(onClick={db.delete(c.id);contacts=db.all()}) { Text("حذف") }
     if(!c.service.startsWith("IDoom")) {
      TextButton(onClick={context.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+c.number)))}) { Text("اتصال") }
      TextButton(onClick={val intl=if(c.number.startsWith("0")) "213"+c.number.drop(1) else c.number;context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/"+intl)))}) { Text("WhatsApp") }
     }
    }
   } } }
   item {
    Text("سجل التعبئة الكلي",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
    Text("بيانات توضيحية ثابتة وليست سجلاً حقيقياً.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.secondary)
    OutlinedTextField(historyQuery,{historyQuery=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("بحث بالعملية أو الاسم أو الرقم أو الشبكة")})
    Row(horizontalArrangement=Arrangement.spacedBy(3.dp)) {
     listOf("الكل","ناجحة","قيد المعالجة","فاشلة").forEach { st -> FilterChip(selected=statusFilter==st,onClick={statusFilter=st},label={Text(st)}) }
    }
    Text("عدد النتائج: "+records.size,style=MaterialTheme.typography.labelMedium)
   }
   items(records,key={it.id}) { r -> Card { Column(Modifier.fillMaxWidth().padding(13.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
     Text(r.name,fontWeight=FontWeight.Bold)
     Text(r.status,color=when(r.status){"فاشلة"->MaterialTheme.colorScheme.error;"قيد المعالجة"->MaterialTheme.colorScheme.tertiary;else->MaterialTheme.colorScheme.primary})
    }
    Text(r.number+" · "+r.service);Text(r.kind+" · "+r.amount+" دج · "+r.date,style=MaterialTheme.typography.bodySmall)
    TextButton(onClick={expanded=if(expanded==r.id)null else r.id}) { Text(if(expanded==r.id)"إخفاء التفاصيل" else "تفاصيل العملية") }
    if(expanded==r.id) {
     HorizontalDivider();Text("رقم العملية: "+r.id);Text("قيمة التعبئة: "+r.amount+" دج");Text("المبلغ المخصوم من رصيدك: "+r.deducted+" دج")
     
    }
   } } }
   item {
    Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)) {
     Text("ملاحظة: جهات الاتصال محفوظة محلياً. سجل العمليات والعروض والرصيد والتعبئات الحقيقية تحتاج إلى تفعيل الخادم المركزي. لا توجد مزامنة أو مصادقة بعد.",Modifier.padding(13.dp),style=MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(24.dp))
   }
  }
 }
 if(dialog) {
  var first by remember(editing?.id) { mutableStateOf(editing?.first.orEmpty()) }
  var last by remember(editing?.id) { mutableStateOf(editing?.last.orEmpty()) }
  var num by remember(editing?.id) { mutableStateOf(editing?.number ?: number) }
  var chosen by remember(editing?.id) { mutableStateOf(editing?.service ?: service) }
  AlertDialog(
   onDismissRequest={dialog=false},
   title={Text(if(editing==null)"إضافة جهة اتصال" else "تعديل جهة اتصال")},
   text={Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
    OutlinedTextField(first,{first=it},label={Text("الاسم")},singleLine=true)
    OutlinedTextField(last,{last=it},label={Text("اللقب")},singleLine=true)
    OutlinedTextField(num,{num=it.filter(Char::isDigit)},label={Text("الرقم")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone))
    Text("الخدمة",fontWeight=FontWeight.SemiBold)
    services.chunked(2).forEach { group -> Row(horizontalArrangement=Arrangement.spacedBy(2.dp)) {
     group.forEach { s -> FilterChip(selected=chosen==s,onClick={chosen=s},label={Text(s)}) }
    } }
   } },
   confirmButton={TextButton(onClick={
    if(first.isNotBlank()&&last.isNotBlank()&&num.isNotBlank()) {
     db.save(Contact(editing?.id ?: 0,first.trim(),last.trim(),normalize(num,chosen),chosen));contacts=db.all();dialog=false
    }
   }) { Text("حفظ") }},
   dismissButton={TextButton(onClick={dialog=false}) { Text("إلغاء") }}
  )
 }
}
