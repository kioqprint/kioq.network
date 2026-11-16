package network.kioq.app

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.Crossfade
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay

private val Green = Color(0xFF087A50)
private val GreenDark = Color(0xFF06452F)
private val GreenSoft = Color(0xFFE6F5EC)
private val GreenBright = Color(0xFF19A66A)
private val Red = Color(0xFFD83A45)
private val Page = Color(0xFFF3F8F5)
private val services = listOf("Djezzy", "Ooredoo", "Mobilis", "IDoom Fibre", "IDoom 4G")
private val quickAmounts = listOf("50", "100", "200", "500", "1000", "1500", "2000")
private val menuItems = listOf("الرئيسية", "تعبئة الهاتف", "تعبئة IDoom", "سجل التعبئات", "الزبائن والتجار", "الديون والمدفوعات", "الأرباح والمداخيل", "الصندوق والسحوبات", "الإشعارات", "المحادثات", "التقارير والإحصائيات", "الإعدادات")
private data class Contact(val id:Long=0,val first:String,val last:String,val number:String,val service:String) { val fullName:String get()="$first $last" }
private data class RechargeRecord(val id:String,val name:String,val number:String,val service:String,val kind:String,val amount:Int,val deducted:Int,val status:String,val date:String)

private data class HelpNumber(val label:String,val number:String)
private val helpNumbers = mapOf(
 "Djezzy" to listOf(HelpNumber("خدمة الزبائن","777"),HelpNumber("خدمة الزبائن من شبكة أخرى","0770857777"),HelpNumber("دعم الموزعين ونقاط البيع","401"),HelpNumber("دعم الموزعين من شبكة أخرى","0770850401"),HelpNumber("القائمة العامة","700")),
 "Ooredoo" to listOf(HelpNumber("خدمة الزبائن","333"),HelpNumber("خدمة الزبائن من شبكة أخرى","0550000333"),HelpNumber("خدمة زبائن المؤسسات","330"),HelpNumber("خدمة المؤسسات من شبكة أخرى","0550000330")),
 "Mobilis" to listOf(HelpNumber("خدمة زبائن الدفع الآجل","666"),HelpNumber("خدمة زبائن الدفع المسبق","888"),HelpNumber("الدفع الآجل من شبكة أخرى","0660600666"),HelpNumber("مسبق الدفع من شبكة أخرى","0660600888")),
 "IDoom" to listOf(HelpNumber("خدمة الزبائن والدعم","12"))
)
private fun bannerPrefs(context:Context)=context.getSharedPreferences("kioq_banners",Context.MODE_PRIVATE)

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
   MaterialTheme(colorScheme=lightColorScheme(primary=Green,onPrimary=Color.White,primaryContainer=GreenSoft,onPrimaryContainer=GreenDark,secondary=Red,onSecondary=Color.White,tertiary=Color(0xFFB58A32),background=Page,surface=Color.White,surfaceVariant=GreenSoft,onSurface=Color(0xFF17251E),onSurfaceVariant=Color(0xFF52645A),outline=Color(0xFFB9CFC2)),shapes=Shapes(small=RoundedCornerShape(13.dp),medium=RoundedCornerShape(19.dp),large=RoundedCornerShape(26.dp))) {
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
@Composable private fun SplashScreen() {
 val transition=rememberInfiniteTransition(label="splash-pulse")
 val pulse by transition.animateFloat(initialValue=0.96f,targetValue=1.04f,animationSpec=infiniteRepeatable(tween(950),RepeatMode.Reverse),label="logo-pulse")
 Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF043D2A),Color(0xFF087A50),Color(0xFF18A66B)))),contentAlignment=Alignment.Center) {
  Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(15.dp)) {
   BrandMark(94)
   Text("KIOQ.network",Modifier.graphicsLayer { scaleX=pulse;scaleY=pulse },color=Color.White,fontSize=31.sp,fontWeight=FontWeight.Black)
   Text("منصة خدمات الهاتف والإنترنت",color=Color(0xFFD9F2E4),style=MaterialTheme.typography.bodyLarge)
   Spacer(Modifier.height(22.dp))
   Box(Modifier.width(42.dp).height(3.dp).background(Color(0x99FFFFFF),RoundedCornerShape(8.dp)))
  }
  Text("الجزائر",Modifier.align(Alignment.BottomCenter).padding(bottom=34.dp),color=Color(0xCCFFFFFF),style=MaterialTheme.typography.labelMedium)
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
 val color=when(service) { "Djezzy"->Color(0xFFD92732); "Ooredoo"->Color(0xFFE51D2A); "Mobilis"->Color(0xFF0876B9); "IDoom Fibre","IDoom 4G","IDoom"->Green; else->Green }
 Surface(color=Color.White,contentColor=color,shape=RoundedCornerShape(11.dp),border=androidx.compose.foundation.BorderStroke(1.dp,color.copy(alpha=0.24f))) {
  Text(service,Modifier.padding(horizontal=if(large)12.dp else 9.dp,vertical=if(large)8.dp else 6.dp),fontSize=if(large)12.sp else 10.sp,fontWeight=FontWeight.ExtraBold,maxLines=1)
 }
}
@Composable private fun KioqApp() {
 val context=LocalContext.current
 val db=remember { ContactDb(context.applicationContext) }
 var contacts by remember { mutableStateOf(db.all()) }
 var page by remember { mutableStateOf("الرئيسية") }
 var showSplash by remember { mutableStateOf(true) }
 LaunchedEffect(Unit) { delay(2000); showSplash=false }
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
 var message by remember { mutableStateOf("") }
 val prefs=remember { bannerPrefs(context) }
 var bannerText by remember { mutableStateOf(prefs.getString("text","مرحباً بكم في KIOQ.network · خدمات التعبئة والإنترنت في مكان واحد") ?: "") }
 var textSize by remember { mutableFloatStateOf(prefs.getFloat("textSize",16f)) }
 var textSpeed by remember { mutableFloatStateOf(prefs.getFloat("textSpeed",48f)) }
 var imageHeight by remember { mutableFloatStateOf(prefs.getFloat("imageHeight",150f)) }
 var imageSpeed by remember { mutableFloatStateOf(prefs.getFloat("imageSpeed",4f)) }
 var imageUris by remember { mutableStateOf(prefs.getString("images","")?.split("|")?.filter { it.isNotBlank() } ?: emptyList()) }
 var imageIndex by remember { mutableIntStateOf(0) }
 var helpNetwork by remember { mutableStateOf<String?>(null) }
 val imagePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
  if(uri!=null && imageUris.size<5) {
   runCatching { context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION) }
   imageUris=imageUris+uri.toString()
   prefs.edit().putString("images",imageUris.joinToString("|")).apply()
  }
 }
 LaunchedEffect(imageUris,imageSpeed) {
  if(imageUris.size>1) while(true) { delay((imageSpeed*1000).toLong().coerceAtLeast(1000L)); imageIndex=(imageIndex+1)%imageUris.size }
  else imageIndex=0
 }
 val detected=networkFor(number,service)
 val drawerState=rememberDrawerState(DrawerValue.Closed)
 LaunchedEffect(drawer) { if(drawer) drawerState.open() else drawerState.close() }
 val recent=contacts.filter { it.service==service && (number.isBlank() || it.number.contains(number) || it.fullName.contains(number,true)) }.take(4)
 val contactList=contacts.filter { (it.fullName+" "+it.number+" "+it.service).contains(contactQuery.trim(),true) }
 val records=demoRecords.filter { (it.id+it.name+it.number+it.service).contains(historyQuery.trim(),true) && (statusFilter=="الكل" || it.status==statusFilter) }
 if(showSplash) {
  SplashScreen()
 } else {
 ModalNavigationDrawer(
  drawerState=drawerState,
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
    if(bannerText.isNotBlank()) {
     Box(Modifier.fillMaxWidth().padding(horizontal=12.dp, vertical=6.dp).background(GreenDark,RoundedCornerShape(12.dp)).padding(horizontal=12.dp,vertical=9.dp)) {
      Text(bannerText,Modifier.fillMaxWidth().basicMarquee(iterations=Int.MAX_VALUE,velocity=textSpeed.dp),color=Color.White,fontSize=textSize.sp,fontWeight=FontWeight.SemiBold,maxLines=1)
     }
    }
    if(imageUris.isNotEmpty()) {
     Crossfade(targetState=imageUris[imageIndex.coerceIn(0,imageUris.lastIndex)],label="banner-image") { uri ->
      val bitmap=remember(uri) { runCatching { context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull() }
      if(bitmap!=null) Image(bitmap=bitmap,contentDescription="بانر KIOQ.network",modifier=Modifier.fillMaxWidth().padding(horizontal=12.dp).height(imageHeight.dp),contentScale=ContentScale.Crop)
     }
    }
    Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.TopCenter) {
   Box(Modifier.fillMaxWidth().widthIn(max=620.dp)) {
   when(page) {
     "الرئيسية" -> Dashboard(onNavigate={page=it},contacts=contacts.size,onHelp={helpNetwork=it})
     "تعبئة الهاتف","تعبئة IDoom" -> RechargePage(page,service,{service=it},kind,{kind=it},number,{number=it.filter(Char::isDigit)},amount,{amount=it.filter(Char::isDigit)},detected,recent,message,{message=it},onSave={editing=null;dialog=true},onConfirm={message="طلب تجريبي فقط: لم يتم إرسال طلب أو خصم رصيد."})
     "سجل التعبئات" -> HistoryPage(historyQuery,{historyQuery=it},statusFilter,{statusFilter=it},records,expanded,{expanded=it})
     "الزبائن والتجار" -> ContactsPage(contactQuery,{contactQuery=it},contactList,onAdd={editing=null;dialog=true},onUse={c->service=c.service;number=c.number;page=if(c.service.startsWith("IDoom")) "تعبئة IDoom" else "تعبئة الهاتف"},onEdit={editing=it;dialog=true},onDelete={db.delete(it.id);contacts=db.all()},onCall={context.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+it)))})
     "الديون والمدفوعات" -> InfoPage("الديون والمدفوعات","متابعة ديون التجار، الدفعات الجزئية، والمبالغ المتبقية.","سيتم احتساب الجزء المحصّل من الهامش بنسبة المبلغ المسدد إلى الدين المرتبط به.")
     "الأرباح والمداخيل" -> InfoPage("الأرباح والمداخيل","إجمالي الأرباح المسجلة، المحصّلة، وغير المحصّلة.","الأرباح تقارير محاسبية وليست محفظة رصيد مستقلة.")
     "الصندوق والسحوبات" -> InfoPage("الصندوق والسحوبات","متابعة المبالغ المحصّلة والسحوبات إلى الصندوق الفعلي.","السحب تحويل محاسبي للصندوق ولا يُسجّل كربح جديد.")
     "الإشعارات" -> InfoPage("الإشعارات","تنبيهات الطلبات وحالات التعبئة والمدفوعات.","لا توجد إشعارات فعلية في النسخة التجريبية.")
     "المحادثات" -> InfoPage("المحادثات","مساحة تواصل بين المالك والتجار.","المحادثات تحتاج إلى الخادم المركزي لتعمل.")
     "التقارير والإحصائيات" -> InfoPage("التقارير والإحصائيات","ملخص العمليات والمبيعات والديون والأرباح.","ستظهر البيانات الحقيقية بعد ربط الخادم.")
     else -> BannerSettingsPage(bannerText,{bannerText=it;prefs.edit().putString("text",it).apply()},textSize,{textSize=it;prefs.edit().putFloat("textSize",it).apply()},textSpeed,{textSpeed=it;prefs.edit().putFloat("textSpeed",it).apply()},imageHeight,{imageHeight=it;prefs.edit().putFloat("imageHeight",it).apply()},imageSpeed,{imageSpeed=it;prefs.edit().putFloat("imageSpeed",it).apply()},imageUris,onAddImage={if(imageUris.size<5) imagePicker.launch(arrayOf("image/*"))},onDeleteImage={uri->imageUris=imageUris.filterNot{it==uri};prefs.edit().putString("images",imageUris.joinToString("|")).apply()})
    }
    }
   }
  }
 }
 }
 }
 if(helpNetwork!=null) {
  val network=helpNetwork!!
  AlertDialog(onDismissRequest={helpNetwork=null},title={Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){NetworkMark(network,true);Text("أرقام $network الرسمية")}},
   text={Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
    helpNumbers[network].orEmpty().forEach { item ->
     OutlinedButton(onClick={context.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(item.number))))},modifier=Modifier.fillMaxWidth()) {
      Column(Modifier.fillMaxWidth()) { Text(item.label,style=MaterialTheme.typography.labelMedium);Text(item.number,fontWeight=FontWeight.Bold,color=GreenDark) }
     }
    }
   }},
   confirmButton={TextButton(onClick={helpNetwork=null}){Text("إغلاق")}}
  )
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
@Composable private fun Dashboard(onNavigate:(String)->Unit,contacts:Int,onHelp:(String)->Unit) {
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
  item {
   Card(colors=CardDefaults.cardColors(containerColor=Color.Transparent),shape=RoundedCornerShape(26.dp),modifier=Modifier.fillMaxWidth()) {
    Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(GreenDark,Green,GreenBright)),RoundedCornerShape(26.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
     Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(13.dp)) { BrandMark(58);Column { Text("KIOQ.network",color=Color.White,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);Text("منصة خدمات الهاتف والإنترنت",color=Color(0xFFD6E9DE)) } }
     HorizontalDivider(color=Color(0x5579A992))
     Text("أهلاً بك",color=Color(0xFFB9D8C7))
     Text("كل خدماتك في مكان واحد",color=Color.White,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
    }
   }
  }
  item { Text("النظرة السريعة",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold,color=GreenDark) }
  item { Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) { StatCard("الرصيد الرئيسي","— دج",Modifier.weight(1f));StatCard("الدين","— دج",Modifier.weight(1f)) } }
  item { Card(shape=RoundedCornerShape(18.dp)) { Column(Modifier.fillMaxWidth().padding(15.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text("العمليات المنجزة اليوم",style=MaterialTheme.typography.bodyMedium,color=Color.Gray)
   Text("0 عملية",style=MaterialTheme.typography.headlineMedium,color=Green,fontWeight=FontWeight.Bold)
   HorizontalDivider()
   Text("صافي الربح اليوم",style=MaterialTheme.typography.bodyMedium,color=Color.Gray)
   Text("— دج",style=MaterialTheme.typography.headlineSmall,color=Green,fontWeight=FontWeight.Bold)
  } } }
  item { Text("الخدمات",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold,color=GreenDark) }
  item { Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
   listOf("Djezzy","Ooredoo","Mobilis","IDoom").chunked(2).forEach { group->Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
    group.forEach { network->Card(Modifier.weight(1f).clickable { onHelp(network) },shape=RoundedCornerShape(18.dp)) {
     Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) { NetworkMark(network,true);Column { Text(network,fontWeight=FontWeight.Bold);Text("أرقام الخدمة والدعم",style=MaterialTheme.typography.labelSmall,color=Color.Gray) } }
    } }
   } }
  } }
  item { Text("خدمات التعبئة",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold,color=GreenDark) }
  item { Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
   services.chunked(2).forEach { group->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    group.forEach { item->Card(Modifier.weight(1f).clickable { onNavigate(if(item.startsWith("IDoom"))"تعبئة IDoom" else "تعبئة الهاتف") }) { Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) { NetworkMark(item);Text(item,fontWeight=FontWeight.SemiBold) } } }
    if(group.size==1) Spacer(Modifier.weight(1f))
   } }
  } }
 }
}
@Composable private fun StatCard(title:String,value:String,modifier:Modifier=Modifier) { Card(modifier) { Column(Modifier.padding(14.dp)) { Text(title,style=MaterialTheme.typography.bodySmall,color=Color.Gray);Text(value,style=MaterialTheme.typography.headlineMedium,color=Green,fontWeight=FontWeight.Bold) } } }
@Composable private fun RechargePage(page:String,service:String,onService:(String)->Unit,kind:String,onKind:(String)->Unit,number:String,onNumber:(String)->Unit,amount:String,onAmount:(String)->Unit,detected:String,recent:List<Contact>,message:String,onMessage:(String)->Unit,onSave:()->Unit,onConfirm:()->Unit) {
 val isInternet=page=="تعبئة IDoom"
 val choices=if(isInternet) services.filter { it.startsWith("IDoom") } else services.filter { !it.startsWith("IDoom") }
 val activeNetwork=if(number.isNotBlank()) detected else service
 LazyColumn(
  modifier=Modifier.fillMaxSize(),
  contentPadding=PaddingValues(horizontal=16.dp,vertical=18.dp),
  verticalArrangement=Arrangement.spacedBy(14.dp),
  horizontalAlignment=Alignment.CenterHorizontally
 ) {
  item {
   Column(Modifier.fillMaxWidth().widthIn(max=540.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)) {
    Text(if(isInternet)"تعبئة الإنترنت" else "تعبئة رصيد الهاتف",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold,color=GreenDark)
    Text(if(isInternet)"اختر الخدمة وأدخل رقم الاشتراك" else "اختر الشبكة وأدخل رقم الهاتف",style=MaterialTheme.typography.bodyMedium,color=Color.Gray)
   }
  }
  item {
   Card(
    modifier=Modifier.fillMaxWidth().widthIn(max=540.dp),
    shape=RoundedCornerShape(24.dp),
    colors=CardDefaults.cardColors(containerColor=Color.White),
    elevation=CardDefaults.cardElevation(defaultElevation=2.dp)
   ) {
    Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(15.dp)) {
     Text(if(isInternet)"نوع الاشتراك" else "اختر شبكة الهاتف",fontWeight=FontWeight.Bold,color=GreenDark)
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      choices.forEach { option ->
       val selected=service==option
       Card(
        modifier=Modifier.weight(1f).clickable { onService(option);onNumber("") },
        shape=RoundedCornerShape(15.dp),
        border=if(selected) androidx.compose.foundation.BorderStroke(1.5.dp,Green) else null,
        colors=CardDefaults.cardColors(containerColor=if(selected) GreenSoft else Page)
       ) {
        Column(Modifier.fillMaxWidth().padding(vertical=13.dp,horizontal=4.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
         NetworkMark(option,true)
         Text(option,fontWeight=if(selected) FontWeight.Bold else FontWeight.Medium,style=MaterialTheme.typography.labelMedium,color=if(selected) GreenDark else Color.DarkGray)
        }
       }
      }
     }
     HorizontalDivider(color=Page)
     Text("نوع العملية",fontWeight=FontWeight.Bold,color=GreenDark)
     Row(Modifier.fillMaxWidth().background(Page,RoundedCornerShape(14.dp)).padding(4.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
      listOf("شحن عادي","العروض المفعّلة").forEach { option ->
       val selected=kind==option
       Surface(
        modifier=Modifier.weight(1f).clickable { onKind(option) },
        color=if(selected) Color.White else Color.Transparent,
        shape=RoundedCornerShape(11.dp),
        shadowElevation=if(selected) 1.dp else 0.dp
       ) { Box(Modifier.padding(vertical=11.dp),contentAlignment=Alignment.Center) {
        Text(option,style=MaterialTheme.typography.labelLarge,fontWeight=if(selected) FontWeight.Bold else FontWeight.Normal,color=if(selected) GreenDark else Color.Gray)
       } }
      }
     }
     Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
      Text(if(isInternet)"رقم اشتراك IDoom" else "رقم الهاتف",fontWeight=FontWeight.Bold,color=GreenDark)
      OutlinedTextField(
       value=number,
       onValueChange=onNumber,
       modifier=Modifier.fillMaxWidth(),
       singleLine=true,
       placeholder={Text(if(isInternet)"أدخل رقم الاشتراك" else "أدخل رقم الهاتف دون بادئة مسبقة")},
       leadingIcon={if(number.isNotBlank()) NetworkMark(activeNetwork,true) else Text("☎",color=Green,fontSize=20.sp)},
       trailingIcon={if(number.isNotBlank()) IconButton(onClick={onNumber("")}) { Text("×",color=Green,fontSize=24.sp,fontWeight=FontWeight.Bold) }},
       keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),
       shape=RoundedCornerShape(16.dp)
      )
      if(number.isNotBlank()) {
       Row(Modifier.fillMaxWidth().background(GreenSoft,RoundedCornerShape(12.dp)).padding(11.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
        NetworkMark(activeNetwork,true)
        Column(Modifier.weight(1f)) {
         Text("تم التعرف على الشبكة",style=MaterialTheme.typography.labelSmall,color=Color.Gray)
         Text(activeNetwork,fontWeight=FontWeight.Bold,color=GreenDark)
        }
        Text("✓",color=Green,fontWeight=FontWeight.Black)
       }
      } else {
       Text("سيظهر شعار الشبكة تلقائياً بعد كتابة الرقم",style=MaterialTheme.typography.bodySmall,color=Color.Gray)
      }
     }
     if(recent.isNotEmpty()) {
      Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
       Text("الأرقام الأخيرة",fontWeight=FontWeight.Bold,color=GreenDark)
       recent.take(3).forEach { c ->
        Surface(Modifier.fillMaxWidth().clickable { onNumber(c.number);onService(c.service) },color=Page,shape=RoundedCornerShape(12.dp)) {
         Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
          NetworkMark(c.service)
          Column(Modifier.weight(1f)) { Text(c.fullName,fontWeight=FontWeight.SemiBold);Text(c.number,style=MaterialTheme.typography.bodySmall,color=Color.Gray) }
          Text("اختيار",color=Green,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelMedium)
         }
        }
       }
      }
     }
     if(kind=="شحن عادي") {
      Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
       Text("مبلغ التعبئة",fontWeight=FontWeight.Bold,color=GreenDark)
       OutlinedTextField(amount,onAmount,Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("أدخل المبلغ بالدينار الجزائري")},suffix={Text("دج")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),shape=RoundedCornerShape(16.dp))
       quickAmounts.chunked(4).forEach { group ->
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
         group.forEach { value ->
          Surface(modifier=Modifier.weight(1f).height(43.dp).clickable { onAmount(value) },shape=RoundedCornerShape(13.dp),color=if(amount==value) Green else GreenSoft,border=if(amount==value) null else androidx.compose.foundation.BorderStroke(1.dp,Color(0xFFD3E7DA))) { Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Text(value+" دج",fontWeight=FontWeight.Bold,fontSize=12.sp,color=if(amount==value) Color.White else GreenDark) } }
         }
         repeat(4-group.size) { Spacer(Modifier.weight(1f)) }
        }
       }
      }
     } else {
      Text("العروض المتاحة ستظهر عند تفعيلها ببيانات مؤكدة.",style=MaterialTheme.typography.bodySmall,color=Color.Gray)
     }
     OutlinedButton(onClick=onSave,modifier=Modifier.fillMaxWidth().height(50.dp),shape=RoundedCornerShape(15.dp),border=androidx.compose.foundation.BorderStroke(1.3.dp,Green)) { Text("حفظ الرقم كجهة اتصال",fontWeight=FontWeight.SemiBold) }
     Button(
      onClick=onConfirm,
      modifier=Modifier.fillMaxWidth().height(52.dp),
      shape=RoundedCornerShape(15.dp),
      colors=ButtonDefaults.buttonColors(containerColor=Green)
     ) { Text("متابعة",fontWeight=FontWeight.Bold) }
     if(message.isNotBlank()) Text(message,style=MaterialTheme.typography.bodySmall,color=Color.Gray)
    }
   }
  }
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
@Composable private fun BannerSettingsPage(
 text:String,onText:(String)->Unit,textSize:Float,onTextSize:(Float)->Unit,textSpeed:Float,onTextSpeed:(Float)->Unit,
 imageHeight:Float,onImageHeight:(Float)->Unit,imageSpeed:Float,onImageSpeed:(Float)->Unit,
 images:List<String>,onAddImage:()->Unit,onDeleteImage:(String)->Unit
) {
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item { Text("إدارة البانرات",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=GreenDark);Text("تظهر هذه البانرات أعلى محتوى جميع الصفحات.",style=MaterialTheme.typography.bodySmall,color=Color.Gray) }
  item { Card { Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
   Text("البانر الكتابي",fontWeight=FontWeight.Bold)
   OutlinedTextField(text,onText,Modifier.fillMaxWidth(),label={Text("النص المعروض")},minLines=2)
   Text("حجم الخط: ${textSize.toInt()} px")
   Slider(value=textSize,onValueChange=onTextSize,valueRange=12f..28f)
   Text("سرعة حركة النص")
   Slider(value=textSpeed,onValueChange=onTextSpeed,valueRange=20f..100f)
  } } }
  item { Card { Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
   Text("بانر الصور",fontWeight=FontWeight.Bold)
   Text("الصور المضافة: ${images.size} / 5",style=MaterialTheme.typography.bodySmall)
   images.forEachIndexed { index,uri -> Row(verticalAlignment=Alignment.CenterVertically) { Text("الصورة ${index+1}",Modifier.weight(1f));TextButton(onClick={onDeleteImage(uri)}) { Text("حذف",color=Red) } } }
   OutlinedButton(onClick=onAddImage,enabled=images.size<5,modifier=Modifier.fillMaxWidth()) { Text(if(images.size<5)"إضافة صورة" else "اكتمل الحد الأقصى للصور") }
   Text("ارتفاع الصور: ${imageHeight.toInt()} dp")
   Slider(value=imageHeight,onValueChange=onImageHeight,valueRange=90f..260f)
   Text("سرعة الانتقال بين الصور: ${imageSpeed.toInt()} ثوانٍ")
   Slider(value=imageSpeed,onValueChange=onImageSpeed,valueRange=2f..12f,steps=9)
  } } }
  item { Text("الإعدادات محفوظة على هذا الجهاز حالياً. مزامنتها بين حسابات التجار تتطلب ربط الخادم.",style=MaterialTheme.typography.bodySmall,color=Color.Gray) }
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
