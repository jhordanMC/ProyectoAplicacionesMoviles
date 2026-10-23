package com.example.medinajhordan_s07;
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
}