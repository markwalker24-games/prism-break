package app.prismbreak;

import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.getcapacitor.BridgeActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableImmersive();
        applyGestureExclusion();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Re-assert immersive whenever the window regains focus (e.g. after the
        // transient bars auto-hide, or returning from background).
        if (hasFocus) {
            enableImmersive();
            applyGestureExclusion();
        }
    }

    /**
     * Let the system inset our content below the status bar / camera cutout (so
     * the HUD never sits under the camera and there's no matching gap at the
     * bottom), while still going immersive at the bottom navigation bar. With
     * BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE an edge swipe just *reveals* the nav
     * bar transiently instead of firing the system back / home gesture — which is
     * what was hijacking the left/right hold controls in the game.
     */
    private void enableImmersive() {
        // true = system fits content within the bars' insets (content starts
        // below the status bar / cutout, fills down once the nav bar is hidden).
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        View decor = getWindow().getDecorView();
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), decor);
        // Hide only the navigation bar; keep the status bar so content sits below
        // the camera. Light icons so the clock/battery read on the dark theme.
        controller.hide(WindowInsetsCompat.Type.navigationBars());
        controller.setAppearanceLightStatusBars(false);
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    /**
     * Reinforce immersive by telling the system not to treat gestures on the
     * left/right screen edges as system gestures. Android caps gesture exclusion
     * at ~200dp per edge, so we cover the vertical band where thumbs rest.
     */
    private void applyGestureExclusion() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
        final View root = getWindow().getDecorView();
        root.post(new Runnable() {
            @Override
            public void run() {
                int w = root.getWidth();
                int h = root.getHeight();
                if (w == 0 || h == 0) return;
                float density = getResources().getDisplayMetrics().density;
                int band = (int) Math.min(h, 200 * density); // exclusion capped at 200dp/edge
                int top = (h - band) / 2;
                int edge = (int) (28 * density);              // ~28dp inward from each side
                List<Rect> rects = new ArrayList<>();
                rects.add(new Rect(0, top, edge, top + band));        // left edge
                rects.add(new Rect(w - edge, top, w, top + band));    // right edge
                root.setSystemGestureExclusionRects(rects);
            }
        });
    }
}
