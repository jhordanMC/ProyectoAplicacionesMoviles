from pathlib import Path
p=Path(r'D:\SaaSVilcas\semana7-preparado')
j=p/'app/src/main/java/com/example/medinajhordan_s07'
mapping=[('Estudiante','Saiyajin'),('estudiantes','saiyajins'),('estudiante','saiyajin'),('apellido','planeta'),('distrito','transformacion'),('BD_UPN','BD_SAIYAJIN')]
for f in j.glob('*.java'):
 s=f.read_text(encoding='utf-8')
 for a,b in mapping:s=s.replace(a,b)
 target=f.with_name(f.name.replace('Estudiante','Saiyajin'))
 target.write_text(s,encoding='utf-8')
 if target!=f:f.unlink()
s=p/'app/src/main/res/values/strings.xml'
s.write_text('''<?xml version="1.0" encoding="utf-8"?>
<resources>
 <string name="app_name">Medina SQLite · Saiyajin</string>
 <string name="registro">Registro de Saiyajin</string>
 <string name="editar">Editar Saiyajin</string>
 <string name="lista">Saiyajins registrados</string>
 <string name="nombre">Nombre</string>
 <string name="planeta">Planeta de origen</string>
 <string name="transformacion">Transformación</string>
 <string name="guardar">Registrar</string>
 <string name="ver_lista">Ver Saiyajins</string>
 <string name="actualizar">Actualizar</string>
 <string name="eliminar">Eliminar</string>
 <string name="volver">Volver</string>
 <string name="agregar">Agregar Saiyajin</string>
 <string name="vacio">Todavía no hay Saiyajins. Agrega el primero.</string>
 <string name="ayuda">Toca un Saiyajin para editar o eliminar su registro.</string>
 <string name="autor">Jhordan Diego Medina Casapayco · Semana 7</string>
 <string name="identificador">ID</string>
</resources>''',encoding='utf-8')
for f in (p/'app/src/main/res/layout').glob('*.xml'):
 s=f.read_text(encoding='utf-8').replace('@string/apellido','@string/planeta').replace('@string/distrito','@string/transformacion').replace('android:text="ID"','android:text="@string/identificador"')
 if f.name=='activity_main.xml':
  s=s.replace('</LinearLayout>','<TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="24dp" android:text="@string/autor" android:gravity="center"/></LinearLayout>')
 f.write_text(s,encoding='utf-8')
(p/'LEEME.md').write_text('''# Medina SQLite · Saiyajin

Alumno: Jhordan Diego Medina Casapayco. Laboratorio de aplicaciones móviles S07.

Aplicación Java con SQLite: registrar, listar, actualizar y eliminar Saiyajins.
Base: BD_SAIYAJIN. Tabla: Saiyajin. Campos: id INTEGER PRIMARY KEY AUTOINCREMENT, nombre VARCHAR, planeta VARCHAR, transformacion VARCHAR.
Los tres campos de texto son obligatorios. El ID se genera automáticamente y no se puede modificar.

## Ejecutar

Abre la carpeta en Android Studio, sincroniza Gradle y ejecuta app en un teléfono o emulador Android 7 o superior.

## Comprobar

1. Registra Goku / Tierra / Super Saiyajin.
2. Pulsa Ver Saiyajins y toca el registro.
3. Cambia la transformación, pulsa Actualizar y revisa la lista.
4. Cierra y abre la app para comprobar que persiste.
5. Prueba campos vacíos, cancelar la eliminación y confirmar la eliminación.

Para inspeccionar la base, usa App Inspection > Database Inspector con la aplicación en ejecución en Android 8 o superior y selecciona BD_SAIYAJIN.

Los nombres de base, tabla y atributos se eligieron para la clase asignada; si el docente proporcionó otros, deben ajustarse.
''',encoding='utf-8')
t=p/'app/src/androidTest/java/com/example/medinajhordan_s07/SaiyajinDatabaseTest.java'
t.write_text('''package com.example.medinajhordan_s07;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class SaiyajinDatabaseTest {
 @Test public void crudPersisteYAislaRegistros() {
  Context original=InstrumentationRegistry.getInstrumentation().getTargetContext();
  String testName="saiyajin_test_"+System.nanoTime();
  Context isolated=new ContextWrapper(original) {
   @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory,android.database.DatabaseErrorHandler handler) {
    return super.openOrCreateDatabase(testName,mode,factory,handler);
   }
   @Override public java.io.File getDatabasePath(String name) { return super.getDatabasePath(testName); }
  };
  BaseDatos db=new BaseDatos(isolated);
  try {
   assertTrue(db.listar().isEmpty());
   long first=db.registrar("Goku","Tierra","Super Saiyajin");
   long second=db.registrar("Vegeta","Vegeta","Super Saiyajin 2");
   assertTrue(first>0); assertTrue(second>first);
   db.close(); db=new BaseDatos(isolated);
   assertEquals(2,db.listar().size()); assertEquals("Goku",db.buscar(first).nombre);
   assertEquals(1,db.actualizar(first,"Goku","Tierra","Ultra Instinto"));
   assertEquals("Ultra Instinto",db.buscar(first).transformacion);
   assertEquals("Super Saiyajin 2",db.buscar(second).transformacion);
   assertEquals(1,db.eliminar(first)); assertNull(db.buscar(first));
   assertEquals(0,db.eliminar(first)); assertEquals(1,db.listar().size());
  } finally { db.close(); original.deleteDatabase(testName); }
 }
}''',encoding='utf-8')
print('Adaptado a Saiyajin')
