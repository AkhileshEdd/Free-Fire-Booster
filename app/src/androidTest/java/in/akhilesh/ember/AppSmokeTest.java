package in.akhilesh.ember;

import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AppSmokeTest {
    @Test public void allTabsAndRecreationRender() {
        Context context=ApplicationProvider.getApplicationContext();
        Store store=new Store(context);store.prefs.edit().clear().putBoolean("welcomed",true).commit();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            for(int i=0;i<5;i++){final int tab=i;scenario.onActivity(a->{a.navigate(tab);assertNotNull(a.findViewById(100+tab));});}
            scenario.recreate();scenario.onActivity(a->assertNotNull(a.findViewById(104)));
        }
    }
    @Test public void sessionSurvivesStoreRecreationAndExportsSafely() {
        Context context=ApplicationProvider.getApplicationContext();Store store=new Store(context);store.prefs.edit().clear().commit();
        DeviceSnapshot d=DeviceSnapshot.read(context);store.start("Free Fire","Balanced",d);
        Store restored=new Store(context);assertTrue(restored.active().has("start"));
        restored.finish(d,"Smooth","=HYPERLINK(\"bad\")");assertEquals(1,restored.sessions().length());assertFalse(restored.active().has("start"));
        assertTrue(restored.exportCsv().contains("'=HYPERLINK"));
        restored.finish(d,"Smooth","");assertEquals(1,restored.sessions().length());
    }
}
