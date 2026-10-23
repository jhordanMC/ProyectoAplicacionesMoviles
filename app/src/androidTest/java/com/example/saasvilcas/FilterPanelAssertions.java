package com.example.saasvilcas;

import android.view.View;
import android.widget.EditText;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import static org.junit.Assert.*;

public final class FilterPanelAssertions {
    private FilterPanelAssertions() { }
    public static void hideAndShow(ActivityScenario<MainActivity> scenario) {
        String[] search={null};
        scenario.onActivity(activity->{
            search[0]=((EditText)activity.findViewById(R.id.filterSearch)).getText().toString();
            activity.findViewById(R.id.filterPanelToggle).performClick();
        });
        settle();
        scenario.onActivity(activity->{
            assertEquals(View.GONE,activity.findViewById(R.id.filterPanelBody).getVisibility());
            assertTrue(activity.findViewById(R.id.companyMenu).isShown());
            activity.findViewById(R.id.filterPanelToggle).performClick();
        });
        settle();
        scenario.onActivity(activity->{
            View body=activity.findViewById(R.id.filterPanelBody);
            assertEquals(View.VISIBLE,body.getVisibility());
            assertTrue(body.getHeight()>0);
            assertEquals(search[0],((EditText)activity.findViewById(R.id.filterSearch)).getText().toString());
        });
    }
    private static void settle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        android.os.SystemClock.sleep(500);
    }
}
